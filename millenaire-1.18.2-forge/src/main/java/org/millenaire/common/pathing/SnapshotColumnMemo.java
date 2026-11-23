package org.millenaire.common.pathing;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Bounded, thread-safe, direct-mapped memo for one immutable path snapshot.
 *
 * A cache key is the packed BlockPos.asLong(x,y,z) (provided by the caller).
 * A slot collision replaces the previous entry; it can only lose a cache hit,
 * never return the result for a different column. Both true and false are
 * cached. This is intentionally NOT shared across snapshots or game ticks.
 *
 * Fixed capacity avoids unbounded allocations by broad A*/JPS searches.
 */
public final class SnapshotColumnMemo {
    private record Entry(long position, boolean simple) {}

    private final AtomicReferenceArray<Entry> slots;
    private final int mask;

    public SnapshotColumnMemo(int capacity) {
        if (capacity <= 0 || (capacity & (capacity - 1)) != 0)
            throw new IllegalArgumentException("capacity must be a power of two");
        slots = new AtomicReferenceArray<>(capacity);
        mask = capacity - 1;
    }

    private int index(long key) {
        // Mix high and low position bits: nearby columns should distribute
        // across the small direct-mapped table.
        long mixed = key ^ (key >>> 32);
        mixed ^= mixed >>> 16;
        mixed *= 0x7feb352dL;
        mixed ^= mixed >>> 15;
        return ((int) mixed) & mask;
    }

    /** Null means not present; FALSE is a valid, cached negative result. */
    public Boolean get(long key) {
        Entry hit = slots.get(index(key));
        if (hit == null || hit.position() != key) return null;
        return Boolean.valueOf(hit.simple());
    }

    public void remember(long key, boolean simple) {
        slots.set(index(key), new Entry(key, simple));
    }

    public int capacity() { return slots.length(); }
}
