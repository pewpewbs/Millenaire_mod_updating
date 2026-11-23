package org.millenaire.common.pathing;

import java.util.Objects;

/**
 * A bounded per-villager retry policy for consecutively unsuccessful routes.
 *
 * Exact route identity (both endpoints AND movement policy) is mandatory.
 * Known world edits reset a failure immediately. Unobserved world changes can
 * postpone the retry only up to MAX_DELAY_TICKS + per-villager jitter.
 *
 * Deliberately independent of Minecraft so the scheduling policy can be
 * regression-tested with plain Java 17.
 */
public final class FailedRouteBackoff {
    private static final long BASE_DELAY_TICKS = 20L;
    private static final long MAX_DELAY_TICKS = 80L;
    private static final int MAX_LEVEL = 3;

    private NavigationRequestKey lastFailure;
    private long failureTick = Long.MIN_VALUE;
    private long retryTick;
    private long mutationEpoch;
    private int level;

    public void onRequest(NavigationRequestKey requested) {
        Objects.requireNonNull(requested, "requested");
        if (lastFailure != null && !lastFailure.equals(requested)) clear();
    }

    /**
     * Should a retry be skipped? A changed known-world mutation epoch bypasses
     * the cooldown immediately, as does a different request or tick rewind.
     */
    public boolean shouldDefer(NavigationRequestKey requested, long tick, long currentEpoch) {
        Objects.requireNonNull(requested, "requested");
        if (lastFailure == null || !lastFailure.equals(requested)) return false;
        if (tick < failureTick || currentEpoch != mutationEpoch) {
            clear();
            return false;
        }
        return tick < retryTick;
    }

    /**
     * Schedule another failure of this EXACT route. The first failure waits
     * ~1 second, the second ~2 seconds, the third and later <=4.3 seconds.
     * Jitter prevents numerous villagers synchronously retrying on one tick.
     *
     * @return 1..3, the backoff level applied to this failure
     */
    public int onFailed(NavigationRequestKey requested, long tick, long currentEpoch,
                        int jitter) {
        Objects.requireNonNull(requested, "requested");
        if (jitter < 0 || jitter > 6)
            throw new IllegalArgumentException("jitter must be in [0..6]");
        if (lastFailure != null && lastFailure.equals(requested)
                && currentEpoch == mutationEpoch && tick >= failureTick) {
            level = Math.min(MAX_LEVEL, level + 1);
        } else {
            level = 1;
        }
        lastFailure = requested;
        failureTick = tick;
        mutationEpoch = currentEpoch;
        long delay = Math.min(MAX_DELAY_TICKS, BASE_DELAY_TICKS << (level - 1)) + jitter;
        retryTick = tick > Long.MAX_VALUE - delay ? Long.MAX_VALUE : tick + delay;
        return level;
    }

    public void clear() {
        lastFailure = null;
        failureTick = Long.MIN_VALUE;
        retryTick = 0L;
        mutationEpoch = 0L;
        level = 0;
    }

    public int level() { return level; }
    public long retryTick() { return retryTick; }
}
