package org.millenaire.common.pathing;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A bounded per-villager retry policy for consecutively unsuccessful routes.
 *
 * Exact route identity (both endpoints AND movement policy) is mandatory.
 * Known world edits reset a failure immediately. Unobserved world changes can
 * postpone the retry only up to MAX_DELAY_TICKS + per-villager jitter.
 *
 * Reset and retry-expiry events are diagnostic ONLY: the observer does not
 * influence whether a request is allowed or the length of its backoff.
 *
 * Independent of Minecraft so the policy can be regression-tested with Java 17.
 */
public final class FailedRouteBackoff {
    private static final long BASE_DELAY_TICKS = 20L;
    private static final long MAX_DELAY_TICKS = 80L;
    private static final int MAX_LEVEL = 3;

    public enum Event {
        RESET_START_CHANGED,
        RESET_GOAL_CHANGED,
        RESET_MOVEMENT_CHANGED,
        RESET_WORLD_MUTATED,
        RESET_TICK_REWIND,
        RESET_SUCCESS,
        RETRY_EXPIRED
    }

    private final Consumer<Event> observer;
    private NavigationRequestKey lastFailure;
    private long failureTick = Long.MIN_VALUE;
    private long retryTick;
    private long mutationEpoch;
    private int level;
    private boolean expiryReported;

    public FailedRouteBackoff() {
        this(event -> {});
    }

    public FailedRouteBackoff(Consumer<Event> observer) {
        this.observer = Objects.requireNonNull(observer, "observer");
    }

    private void reset(Event event) {
        // Never count resets for a planner that has no recorded failure.
        if (lastFailure != null) observer.accept(event);
        clear();
    }

    private void resetForChangedRequest(NavigationRequestKey requested) {
        if (lastFailure == null || lastFailure.equals(requested)) return;
        Event reason = !lastFailure.start().equals(requested.start())
                ? Event.RESET_START_CHANGED
                : !lastFailure.goal().equals(requested.goal())
                        ? Event.RESET_GOAL_CHANGED
                        : Event.RESET_MOVEMENT_CHANGED;
        reset(reason);
    }

    public void onRequest(NavigationRequestKey requested) {
        Objects.requireNonNull(requested, "requested");
        resetForChangedRequest(requested);
    }

    /**
     * Should a retry be skipped? A changed known-world mutation epoch bypasses
     * the cooldown immediately, as does a different request or tick rewind.
     *
     * An expired timer is NOT a reset: if the subsequent retry fails again,
     * the streak may still escalate to level 2 or 3.
     */
    public boolean shouldDefer(NavigationRequestKey requested, long tick, long currentEpoch) {
        Objects.requireNonNull(requested, "requested");
        if (lastFailure == null || !lastFailure.equals(requested)) return false;
        if (tick < failureTick) {
            reset(Event.RESET_TICK_REWIND);
            return false;
        }
        if (currentEpoch != mutationEpoch) {
            reset(Event.RESET_WORLD_MUTATED);
            return false;
        }
        if (tick >= retryTick) {
            if (!expiryReported) {
                observer.accept(Event.RETRY_EXPIRED);
                expiryReported = true;
            }
            return false;
        }
        return true;
    }

    /**
     * Schedule another failure of this EXACT route. First failed search waits
     * ~1 second, second ~2 seconds, third and later <=4.3 seconds at 20 TPS.
     * Jitter prevents many villagers retrying together on one tick.
     *
     * @return 1..3, the backoff level applied to this failure
     */
    public int onFailed(NavigationRequestKey requested, long tick, long currentEpoch,
                        int jitter) {
        Objects.requireNonNull(requested, "requested");
        if (jitter < 0 || jitter > 6)
            throw new IllegalArgumentException("jitter must be in [0..6]");

        // A search may have been in flight while the world was modified,
        // or this may be the first callback for a different request. Count
        // the reason even if shouldDefer() was not called in the meantime.
        resetForChangedRequest(requested);
        if (lastFailure != null && tick < failureTick) {
            reset(Event.RESET_TICK_REWIND);
        } else if (lastFailure != null && currentEpoch != mutationEpoch) {
            reset(Event.RESET_WORLD_MUTATED);
        }
        if (lastFailure != null && lastFailure.equals(requested)) {
            level = Math.min(MAX_LEVEL, level + 1);
        } else {
            level = 1;
        }
        lastFailure = requested;
        failureTick = tick;
        mutationEpoch = currentEpoch;
        long delay = Math.min(MAX_DELAY_TICKS, BASE_DELAY_TICKS << (level - 1)) + jitter;
        retryTick = tick > Long.MAX_VALUE - delay ? Long.MAX_VALUE : tick + delay;
        expiryReported = false;
        return level;
    }

    /** Called only when a real path has been found. */
    public void onSuccess() {
        reset(Event.RESET_SUCCESS);
    }

    /** Silent manual clear, suitable for initialization and tests. */
    public void clear() {
        lastFailure = null;
        failureTick = Long.MIN_VALUE;
        retryTick = 0L;
        mutationEpoch = 0L;
        level = 0;
        expiryReported = false;
    }

    public int level() { return level; }
    public long retryTick() { return retryTick; }
}
