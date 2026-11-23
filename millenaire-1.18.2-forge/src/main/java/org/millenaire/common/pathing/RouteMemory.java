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
    public record Lookup(List<Cell> goalToStart, boolean reversed) {}
    /** Atomic result and revision: another villager may store a route at any time. */
    public record CandidatesSnapshot(List<Lookup> matches, long revision) {}
    private record Key(Cell start, Cell goal, int options) {}
    private record CacheEntry(List<Cell> path, long tick) {}

    private final int maxEntries;
    private final int maxNodes;
    private final long lifetimeTicks;
    private final Map<Key, CacheEntry> entries;
    private long revision;

    public RouteMemory(int maxEntries, int maxNodes, long lifetimeTicks) {
        if (maxEntries < 1 || maxNodes < 2 || lifetimeTicks < 1) {
            throw new IllegalArgumentException("Invalid route-cache limits");
        }
        this.maxEntries = maxEntries;
        this.maxNodes = maxNodes;
        this.lifetimeTicks = lifetimeTicks;
        this.entries = new LinkedHashMap<>(16, 0.75F, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<Key, CacheEntry> eldest) {
                return size() > RouteMemory.this.maxEntries;
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
        entries.put(new Key(start, goal, options), new CacheEntry(path, tick));
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
        return result;
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
        if (entries.remove(reversed ? new Key(goal, start, options)
                : new Key(start, goal, options)) != null) ++revision;
    }

    private Lookup find(long tick, Key key, boolean reverse) {
        CacheEntry entry = entries.get(key);
        if (entry == null) return null;
        if (tick < entry.tick || tick - entry.tick > lifetimeTicks) {
            entries.remove(key);
            ++revision;
            return null;
        }
        if (!reverse) return new Lookup(entry.path, false);
        ArrayList<Cell> inverted = new ArrayList<>(entry.path);
        Collections.reverse(inverted);
        return new Lookup(List.copyOf(inverted), true);
    }

    public synchronized int size() { return entries.size(); }
}
