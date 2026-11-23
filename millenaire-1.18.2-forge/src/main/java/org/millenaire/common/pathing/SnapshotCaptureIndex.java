package org.millenaire.common.pathing;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Bounded, per-world catalog of detached path snapshots created in one game
 * tick. It holds WEAK references only: search workers own snapshot lifetimes.
 *
 * A later capture may fit an older snapshot even if it does not fit the most
 * recently created one. We prefer the smallest enclosing rectangle to limit
 * unnecessary palette access, but never return a snapshot from another tick.
 *
 * This class does NOT establish that the world is unchanged. The caller must
 * check its per-tick mutation ledger before using any match and invalidate
 * the catalog when a known Forge mutation arrives.
 */
public final class SnapshotCaptureIndex<T> {
    private record Entry<T>(int minX, int maxX, int minZ, int maxZ,
                            WeakReference<T> value) {}

    public record Match<T>(T snapshot, boolean nonLatest) {
        public Match {
            Objects.requireNonNull(snapshot, "snapshot");
        }
    }

    private final int capacity;
    private final ArrayList<Entry<T>> entries;
    private long tick = Long.MIN_VALUE;

    public SnapshotCaptureIndex(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
        this.entries = new ArrayList<>(capacity);
    }

    /**
     * Finds the smallest live containing snapshot (not simply the last one).
     * A bounded array scan needs <= capacity weak-reference dereferences.
     */
    public synchronized Match<T> find(long nowTick, int minX, int maxX, int minZ, int maxZ) {
        resetIfNewTick(nowTick);
        if (minX > maxX || minZ > maxZ) return null;
        T best = null;
        int bestArea = Integer.MAX_VALUE;
        boolean nonLatest = false;
        int newest = entries.size() - 1;
        for (int i = 0; i < entries.size(); i++) {
            Entry<T> entry = entries.get(i);
            T value = entry.value().get();
            if (value == null || minX < entry.minX() || maxX > entry.maxX()
                    || minZ < entry.minZ() || maxZ > entry.maxZ()) continue;
            long area = ((long) entry.maxX() - entry.minX() + 1)
                    * ((long) entry.maxZ() - entry.minZ() + 1);
            if (area < bestArea) {
                best = value;
                bestArea = (int) area;
                nonLatest = i < newest;
            }
        }
        return best == null ? null : new Match<>(best, nonLatest);
    }

    /**
     * Existing entries remain weak. Expired references are discarded so they
     * do not evict still-live snapshots under the 4-entry bound.
     */
    public synchronized void remember(long nowTick, int minX, int maxX,
                                      int minZ, int maxZ, T snapshot) {
        resetIfNewTick(nowTick);
        Objects.requireNonNull(snapshot, "snapshot");
        if (minX > maxX || minZ > maxZ) throw new IllegalArgumentException("invalid bounds");
        entries.removeIf(entry -> entry.value().get() == null);
        if (entries.size() >= capacity) entries.remove(0);
        entries.add(new Entry<>(minX, maxX, minZ, maxZ, new WeakReference<>(snapshot)));
    }

    /** Returns the number of live copies evicted by a known mutation. */
    public synchronized int invalidate(long nowTick) {
        resetIfNewTick(nowTick);
        int live = 0;
        for (Entry<T> entry : entries) {
            if (entry.value().get() != null) live++;
        }
        entries.clear();
        return live;
    }

    private void resetIfNewTick(long nowTick) {
        if (tick != nowTick) {
            entries.clear();
            tick = nowTick;
        }
    }
}
