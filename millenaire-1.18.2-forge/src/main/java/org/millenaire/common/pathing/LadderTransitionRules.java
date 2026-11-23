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

    /** Never apply lift unless the next path node is on the same ladder column. */
    public static boolean shouldAssist(boolean sameColumn, boolean ladderContact,
            double verticalError, double horizontalErrorSquared) {
        return sameColumn && ladderContact && Math.abs(verticalError) >= 0.28D
                && horizontalErrorSquared <= 0.30D;
    }
}
