package org.millenaire.common.pathing;

/**
 * Small deterministic safety decisions shared by snapshot path search and
 * in-world ladder movement. Geometry/world access stays with their callers.
 */
public final class LadderTransitionRules {
    private LadderTransitionRules() {}

    /** Only a continuous, head-clear ladder can become a vertical A* edge. */
    public static boolean canTraverse(boolean currentRung, boolean nextRung,
            boolean feetClear, boolean headClear) {
        return currentRung && nextRung && feetClear && headClear;
    }

    /**
     * Limit assisted approach to a rung immediately in reach. A path that is
     * heading elsewhere must not be dragged toward an unrelated ladder.
     */
    public static boolean shouldApproach(boolean pathTargetsLadder, double horizontalErrorSquared) {
        return pathTargetsLadder && horizontalErrorSquared <= 1.69D;
    }

    /** Bounded horizontal velocity toward the ladder's backing surface. */
    public static double approachSpeed(double error) {
        return Math.max(-0.085D, Math.min(0.085D, error * 0.25D));
    }

    /**
     * A rung can be slightly below the mob while the route is still ascending.
     * Look at the adjacent ladder waypoints, not at (rungY - mobFeetY).
     * The upcoming rung wins when a route reverses direction.
     */
    public static int routeDirection(int previousY, boolean previousRung,
                                     int currentY, int followingY,
                                     boolean followingRung) {
        if (followingRung && followingY != currentY) {
            return Integer.compare(followingY, currentY);
        }
        if (previousRung && previousY != currentY) {
            return Integer.compare(currentY, previousY);
        }
        return 0;
    }

    /**
     * Advance a ladder waypoint only when the body has actually crossed its
     * height and is centered in that shaft. Prevents a vanilla waypoint from
     * pulling the mob DOWN during an upward traversal.
     */
    public static boolean reachedRung(int direction, double feetY, int rungY,
                                      double horizontalErrorSquared) {
        if (direction == 0 || horizontalErrorSquared > 0.30D) return false;
        return direction > 0 ? feetY >= rungY + 0.04D
                : feetY <= rungY + 0.55D;
    }

    /** Command climb velocity from route intent, never from local Y error. */
    public static boolean shouldClimb(int direction, boolean aligned,
                                      boolean contact, boolean reached,
                                      double horizontalErrorSquared) {
        return direction != 0 && aligned && contact && !reached
                && horizontalErrorSquared <= 0.30D;
    }

    /**
     * Hold against the ladder and climb over the upper platform's lip.
     * Only applies when the immediately previous path node was a ladder,
     * the body still touches it and the exit is no lower than that rung.
     */
    public static boolean needsTopOut(boolean previousRung, boolean contact,
                                      int previousY, int exitY, double feetY,
                                      double horizontalErrorSquared) {
        return previousRung && contact && exitY >= previousY
                && feetY < exitY + 0.05D
                && horizontalErrorSquared <= 1.69D;
    }

    /** Never apply lift unless the next path node is on the same ladder column. */
    public static boolean shouldAssist(boolean sameColumn, boolean ladderContact,
            double verticalError, double horizontalErrorSquared) {
        return sameColumn && ladderContact && Math.abs(verticalError) >= 0.28D
                && horizontalErrorSquared <= 0.30D;
    }
}
