package org.millenaire.common.pathing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bounded, immutable route-coordinate memory. Owns no Minecraft objects, so it
 * is safe to keep per-world without retaining chunks, entities or snapshots.
 *
 * IMPORTANT: hits are only candidates. The caller MUST validate every node
 * and every directed edge against the current world before using them.
 */
public final class RouteMemory {
    public record Cell(int x, int y, int z) {}
    public record Lookup(List<Cell> goalToStart, boolean reversed, boolean corridor) {}
    /** Atomic result and revision: another villager may store a route at any time. */
    public record CandidatesSnapshot(List<Lookup> matches, long revision) {}
    private record Key(Cell start, Cell goal, int options) {}
    private record CacheEntry(List<Cell> path, long tick) {}
    // Two staggered grids avoid most boundary misses. Buckets hold only the
    // latest exact key; the exact-entry LRU owns lifetime and eviction.
    private record ZonePair(Cell start, Cell goal, int options, int offset) {
        static ZonePair of(Key key, int offset) {
            return new ZonePair(zone(key.start, offset), zone(key.goal, offset), key.options, offset);
        }
        private static Cell zone(Cell p, int offset) {
            return new Cell((p.x + offset) >> 3, p.y, (p.z + offset) >> 3);
        }
    }

    private final int maxEntries;
    private final int maxNodes;
    private final long lifetimeTicks;
    private final Map<Key, CacheEntry> entries;
    private final Map<ZonePair, Key> corridors = new java.util.HashMap<>();
    private long revision;
    private long budgetTick = Long.MIN_VALUE;
    private long corridorNanos;

    // Optional corridor replay runs on the server thread. All villagers in
    // this world share 1 ms per game tick; exact hits keep their existing budget.
    public synchronized long corridorBudget(long tick) {
        if (budgetTick != tick) {
            budgetTick = tick;
            corridorNanos = 1_000_000L;
        }
        return corridorNanos;
    }

    public synchronized void chargeCorridor(long tick, long elapsed) {
        if (budgetTick == tick) corridorNanos = Math.max(0L, corridorNanos - elapsed);
    }

    public RouteMemory(int maxEntries, int maxNodes, long lifetimeTicks) {
        if (maxEntries < 1 || maxNodes < 2 || lifetimeTicks < 1) {
            throw new IllegalArgumentException("Invalid route-cache limits");
        }
        this.maxEntries = maxEntries;
        this.maxNodes = maxNodes;
        this.lifetimeTicks = lifetimeTicks;
        this.entries = new LinkedHashMap<>(16, 0.75F, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<Key, CacheEntry> eldest) {
                if (size() <= RouteMemory.this.maxEntries) return false;
                unindex(eldest.getKey());
                return true;
            }
        };
    }

    public synchronized void remember(long tick, int options, List<Cell> goalToStart) {
        if (goalToStart == null || goalToStart.size() < 2
                || goalToStart.size() > maxNodes) return;
        List<Cell> path = List.copyOf(goalToStart);
        Cell start = path.get(path.size() - 1);
        Cell goal = path.get(0);
        if (start.equals(goal)) return;
        Key key = new Key(start, goal, options);
        entries.put(key, new CacheEntry(path, tick));
        if (path.size() >= 8) {
            corridors.put(ZonePair.of(key, 0), key);
            corridors.put(ZonePair.of(key, 4), key);
        } else unindex(key);
        ++revision;
    }

    public synchronized Lookup lookup(long tick, int options, Cell start, Cell goal) {
        List<Lookup> hits = candidates(tick, options, start, goal);
        return hits.isEmpty() ? null : hits.get(0);
    }

    /** Return both alternatives so the caller can compare VALIDATED costs. */
    public synchronized List<Lookup> candidates(long tick, int options, Cell start, Cell goal) {
        ArrayList<Lookup> result = new ArrayList<>(2);
        Lookup exact = find(tick, new Key(start, goal, options), false);
        Lookup opposite = find(tick, new Key(goal, start, options), true);
        if (exact != null) result.add(exact);
        if (opposite != null) result.add(opposite);
        if (result.isEmpty() && !corridors.isEmpty() && !start.equals(goal)) {
            Lookup corridor = nearby(tick, new Key(start, goal, options), false);
            if (corridor == null) corridor = nearby(tick, new Key(goal, start, options), true);
            if (corridor != null) result.add(corridor);
        }
        return result;
    }

    private void unindex(Key key) {
        corridors.remove(ZonePair.of(key, 0), key);
        corridors.remove(ZonePair.of(key, 4), key);
    }

    private Lookup nearby(long tick, Key request, boolean reverse) {
        Key previous = null;
        for (int offset = 0; offset <= 4; offset += 4) {
            Key source = corridors.get(ZonePair.of(request, offset));
            if (source == null || source.equals(previous)) continue;
            previous = source;
            Lookup stored = find(tick, source, false);
            if (stored == null) continue;
            List<Cell> path = stored.goalToStart;
            int end = anchor(path, request.goal, false);
            int start = anchor(path, request.start, true);
            if (end < 0 || start - end < 3) continue;
            ArrayList<Cell> joined = new ArrayList<>();
            joined.add(request.goal);
            connect(joined, path.get(end));
            joined.addAll(path.subList(end + 1, start + 1));
            connect(joined, request.start);
            if (joined.size() > maxNodes || new java.util.HashSet<>(joined).size() != joined.size()) continue;
            if (reverse) Collections.reverse(joined);
            return new Lookup(List.copyOf(joined), reverse, true);
        }
        return null;
    }

    // Keep both local approaches short, on the same floor, and in unit steps.
    // A door, gap or wall blocking any join causes ordinary search at replay.
    private static int anchor(List<Cell> path, Cell point, boolean fromStart) {
        int best = -1, bestScore = Integer.MAX_VALUE;
        for (int i = 0; i < 5; i++) {
            int index = fromStart ? path.size() - 1 - i : i;
            Cell p = path.get(index);
            int distance = Math.abs(p.x - point.x) + Math.abs(p.z - point.z);
            int score = distance * 2 - i;
            if (p.y == point.y && distance <= 3 && score < bestScore) {
                best = index;
                bestScore = score;
            }
        }
        return best;
    }

    private static void connect(List<Cell> path, Cell to) {
        Cell from = path.get(path.size() - 1);
        int x = from.x, z = from.z;
        while (x != to.x || z != to.z) {
            if (x != to.x) x += Integer.compare(to.x, x);
            else z += Integer.compare(to.z, z);
            path.add(new Cell(x, to.y, z));
        }
    }

    /**
     * Return candidates and the revision under the SAME monitor. A miss can
     * safely be remembered only for this revision; concurrent additions must
     * force another candidate lookup.
     */
    public synchronized CandidatesSnapshot candidatesWithRevision(
            long tick, int options, Cell start, Cell goal) {
        return new CandidatesSnapshot(candidates(tick, options, start, goal), revision);
    }

    public synchronized long revision() { return revision; }

    public synchronized void forget(long tick, int options, Cell start, Cell goal, boolean reversed) {
        Key key = reversed ? new Key(goal, start, options) : new Key(start, goal, options);
        if (entries.remove(key) != null) {
            unindex(key);
            ++revision;
        }
    }

    private Lookup find(long tick, Key key, boolean reverse) {
        CacheEntry entry = entries.get(key);
        if (entry == null) return null;
        if (tick < entry.tick || tick - entry.tick > lifetimeTicks) {
            entries.remove(key);
            unindex(key);
            ++revision;
            return null;
        }
        if (!reverse) return new Lookup(entry.path, false, false);
        ArrayList<Cell> inverted = new ArrayList<>(entry.path);
        Collections.reverse(inverted);
        return new Lookup(List.copyOf(inverted), true, false);
    }

    public synchronized int size() { return entries.size(); }
}
