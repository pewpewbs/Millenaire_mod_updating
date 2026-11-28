package org.millenaire.common.pathing;

/**
 * Detects failure to make actual ground-level progress toward a goal.
 *
 * Vertical jumping against stairs or a wall must not reset this watchdog.
 * Intentional climbing on a ladder/vine is the exception: it is genuine travel.
 * The clock is independent of path object refreshes, so a loop of identical
 * path recalculations cannot make a stationary villager appear to progress.
 */
public final class MovementWatchdog {
    public enum Action { NONE, REPATH, DETOUR, RECOVER }

    private static final double MIN_HORIZONTAL_PROGRESS_SQUARED = 0.25D;
    private static final double MIN_CLIMB_PROGRESS = 0.5D;
    private static final long REPATH_TICKS = 80L;
    private static final long DETOUR_TICKS = 160L;
    private static final long RECOVER_TICKS = 240L;

    private Object target;
    private double x;
    private double y;
    private double z;
    private long since;
    private long nextRetry;

    public void reset() {
        target = null;
    }

    /**
     * @param climbing whether the actor is genuinely using a climbable block,
     *                 rather than simply jumping or bobbing up and down
     */
    public Action sample(long now, Object destination, double px, double py,
            double pz, boolean climbing) {
        if (destination == null) {
            reset();
            return Action.NONE;
        }
        if (target == null || !target.equals(destination) || now < since) {
            startTracking(now, destination, px, py, pz);
            return Action.NONE;
        }

        double dx = px - x;
        double dz = pz - z;
        boolean horizontalProgress = dx * dx + dz * dz >= MIN_HORIZONTAL_PROGRESS_SQUARED;
        boolean climbingProgress = climbing && Math.abs(py - y) >= MIN_CLIMB_PROGRESS;
        if (horizontalProgress || climbingProgress) {
            startTracking(now, destination, px, py, pz);
            return Action.NONE;
        }

        long stalled = now - since;
        if (stalled >= RECOVER_TICKS) {
            // Limit expensive recovery attempts if safe teleport is disabled
            // or the destination is temporarily unavailable.
            nextRetry = now + REPATH_TICKS;
            since = now;
            return Action.RECOVER;
        }
        if (now < nextRetry) return Action.NONE;
        nextRetry = now + REPATH_TICKS;
        return stalled >= DETOUR_TICKS ? Action.DETOUR : Action.REPATH;
    }

    private void startTracking(long now, Object destination, double px,
            double py, double pz) {
        target = destination;
        x = px;
        y = py;
        z = pz;
        since = now;
        nextRetry = now + REPATH_TICKS;
    }
}
