package org.millenaire.common.pathing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

/**
 * Bounded copies of chunk sections shared by path snapshots during ONE server
 * game tick only. A new tick always drops both the source identities and all
 * copies: ordinary Forge block events do not cover every block-state mutation.
 *
 * The caller is responsible for creating detached immutable copies on the
 * server thread. This class owns only the admission policy and identity map.
 */
public final class SnapshotSectionPool<T> {
    private record Entry<T>(Object source, T copy) {}

    private final int maxSnapshots;
    private final int maxChunkVisits;
    private final int maxCopies;
    private final Map<Long, Entry<T>> sections = new HashMap<>();
    // A Forge event can fire BEFORE a block is actually modified. Eviction
    // alone is insufficient: a subsequent capture might cache the OLD value.
    // Suppress re-sharing of such sections until the next server tick.
    private final Set<Long> poisonedSections = new HashSet<>();
    private boolean poisonedWorld;
    private long revision;
    private long tick = Long.MIN_VALUE;
    private int snapshots, chunkVisits, copies, hits;

    public SnapshotSectionPool(int maxSnapshots, int maxChunkVisits, int maxCopies) {
        if (maxSnapshots < 1 || maxChunkVisits < 1 || maxCopies < 1)
            throw new IllegalArgumentException("Budget limits must be positive");
        this.maxSnapshots = maxSnapshots;
        this.maxChunkVisits = maxChunkVisits;
        this.maxCopies = maxCopies;
    }

    /** Reserve work before any chunk walks; failed admissions spend nothing. */
    public synchronized boolean begin(long nowTick, int requestedChunkVisits) {
        resetIfNewTick(nowTick);
        if (requestedChunkVisits < 1 || requestedChunkVisits > maxChunkVisits
                || snapshots >= maxSnapshots
                || chunkVisits + (long) requestedChunkVisits > maxChunkVisits) return false;
        snapshots++;
        chunkVisits += requestedChunkVisits;
        return true;
    }

    /**
     * Return a previously detached copy only when its source palette object
     * still has the same identity. This protects palette replacement, NOT
     * in-place mutation; therefore this pool MUST NOT outlive the game tick.
     */
    public synchronized T get(long nowTick, long sectionKey, Object currentSource) {
        resetIfNewTick(nowTick);
        Objects.requireNonNull(currentSource, "source");
        if (poisonedWorld || poisonedSections.contains(sectionKey)) return null;
        Entry<T> entry = sections.get(sectionKey);
        if (entry == null) return null;
        if (entry.source() != currentSource) {
            sections.remove(sectionKey);
            return null;
        }
        hits++;
        return entry.copy();
    }

    /**
     * The caller must copy the palette BEFORE inserting it. False means the
     * shared copy budget is exhausted and no further copy may be taken.
     */
    public synchronized boolean canCopy(long nowTick) {
        resetIfNewTick(nowTick);
        return copies < maxCopies;
    }

    public synchronized void remember(long nowTick, long sectionKey, Object currentSource, T detached) {
        resetIfNewTick(nowTick);
        Objects.requireNonNull(currentSource, "source");
        Objects.requireNonNull(detached, "detached copy");
        if (copies >= maxCopies) throw new IllegalStateException("Section copy budget exhausted");
        // Always charge actual allocations to the budget; never cache copies
        // of a section with an outstanding pre-mutation notification.
        if (!poisonedWorld && !poisonedSections.contains(sectionKey)) {
            sections.put(sectionKey, new Entry<>(currentSource, detached));
        }
        copies++;
    }

    /**
     * A known block-change notification invalidates exactly one section in
     * this tick. Already-admitted copy/scan work remains charged to the budget
     * so repeated edits cannot evade the CPU / allocation ceiling.
     */
    public synchronized boolean invalidateSection(long nowTick, long sectionKey) {
        resetIfNewTick(nowTick);
        boolean evicted = sections.remove(sectionKey) != null;
        revision++;
        if (!poisonedWorld) {
            // Bounded memory even when very many blocks change in a tick.
            if (poisonedSections.size() >= maxCopies) {
                poisonedWorld = true;
                poisonedSections.clear();
                sections.clear();
            } else {
                poisonedSections.add(sectionKey);
            }
        }
        return evicted;
    }

    /**
     * Conservative reaction to an entire chunk reload, piston movement or
     * world lifecycle event. Does NOT reset the admission quotas.
     */
    public synchronized int invalidateAll(long nowTick) {
        resetIfNewTick(nowTick);
        int discarded = sections.size();
        revision++;
        poisonedWorld = true;
        poisonedSections.clear();
        sections.clear();
        return discarded;
    }

    /** Do not reuse a completed superset snapshot after ANY known mutation. */
    public synchronized boolean mayReuseSnapshots(long nowTick) {
        resetIfNewTick(nowTick);
        return !poisonedWorld && poisonedSections.isEmpty();
    }

    public synchronized boolean isPoisoned(long nowTick, long sectionKey) {
        resetIfNewTick(nowTick);
        return poisonedWorld || poisonedSections.contains(sectionKey);
    }

    /** Detect known mutations that raced with a snapshot's palette copying. */
    public synchronized long revision(long nowTick) {
        resetIfNewTick(nowTick);
        return revision;
    }

    public synchronized int usedSnapshots() { return snapshots; }
    public synchronized int usedCopies() { return copies; }
    public synchronized int reusedCopies() { return hits; }
    public synchronized int usedChunkVisits() { return chunkVisits; }

    private void resetIfNewTick(long nowTick) {
        if (tick != nowTick) {
            // Keep no references to live palette containers across ticks.
            sections.clear();
            poisonedSections.clear();
            poisonedWorld = false;
            revision = 0L;
            snapshots = 0;
            chunkVisits = 0;
            copies = 0;
            hits = 0;
            tick = nowTick;
        }
    }
}
