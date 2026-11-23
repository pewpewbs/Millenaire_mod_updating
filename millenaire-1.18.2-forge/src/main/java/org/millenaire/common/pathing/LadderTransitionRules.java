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

    /** Never apply lift unless the next path node is on the same ladder column. */
    public static boolean shouldAssist(boolean sameColumn, boolean ladderContact,
            double verticalError, double horizontalErrorSquared) {
        return sameColumn && ladderContact && Math.abs(verticalError) >= 0.28D
                && horizontalErrorSquared <= 0.30D;
    }
}
