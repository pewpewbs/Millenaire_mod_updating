package org.millenaire.common.goal.generic;

/** Pure stock policy. Only an explicitly designated secondary planting item
 * can use this rule; the primary crop is never gated. */
public final class SeedSurplusPolicy {
    private SeedSurplusPolicy() {}

    public static long plantingReserve(int plots) {
        return plots <= 0 ? 0 : Math.max(16L, 2L * plots);
    }

    public static long chickenFeedReserve(int markers) {
        return 2L * Math.max(0, markers);
    }

    public static int allowedDrop(int requested, long localStock, long localTarget,
                                  long villageStock, long villageTarget) {
        long localNeed = Math.max(0L, localTarget - Math.max(0L, localStock));
        long villageNeed = Math.max(0L, villageTarget - Math.max(0L, villageStock));
        return (int)Math.min(Math.max(0, requested), Math.max(localNeed, villageNeed));
    }
}
