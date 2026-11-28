package org.millenaire.common.goal;

/** Pure scheduling policy: no world reads or mutations. */
public final class BrickProductionPolicy {
    public static final int RESERVE_PER_LEVEL = 128;
    public static final int MAX_RESERVE = 512;
    public static final int DELIVERY_BATCH = 32;
    public static final int REFILL_BEFORE_DELIVERY_PRIORITY = 950;
    private BrickProductionPolicy() {}

    /** Completed internal levels 0..3 correspond to displayed levels 1..4. */
    public static int reserveForCentreLevel(int internalLevel) {
        return RESERVE_PER_LEVEL * (1 + Math.max(0, Math.min(3, internalLevel)));
    }

    public static boolean deliveryDue(int carried, boolean fired) {
        return carried > 0 && (carried >= DELIVERY_BATCH || fired);
    }

    /** 32 bricks per completed kiln; zero kilns means no new visit. */
    public static long rawDeliveryTarget(int completedKilns) {
        return (long) DELIVERY_BATCH * Math.max(0, completedKilns);
    }

    /** The threshold gates another kiln visit, never the active harvest. */
    public static boolean mayStartAnotherKiln(int raw, boolean demandRemaining) {
        return mayStartAnotherKiln(raw, demandRemaining, 1);
    }

    public static boolean mayStartAnotherKiln(int raw, boolean demandRemaining, int completedKilns) {
        return raw < rawDeliveryTarget(completedKilns) && demandRemaining;
    }

    public static boolean shouldDeliverRaw(int raw, boolean workRemaining) {
        return raw > 0 && !workRemaining;
    }

    public record Demand(long shortage, long constructionShortage) {
        public boolean shouldGather() { return shortage > 0; }
        public boolean shouldForm(long inDryingArea) { return shortage > Math.max(0, inDryingArea); }
        public boolean urgent() { return constructionShortage > 0; }
        public int priority(boolean gathering) {
            int reserveBonus = (int)Math.min(20, shortage);
            int constructionBonus = (int)Math.min(500, constructionShortage * 4);
            return (gathering ? 260 : 220) + reserveBonus + constructionBonus;
        }
    }

    public static Demand demand(long rawRequired, long centralRaw, long otherRaw,
                                long firedRequired, long centralFired, long otherFired) {
        return demand(rawRequired,centralRaw,otherRaw,firedRequired,centralFired,otherFired,reserveForCentreLevel(0));
    }

    public static Demand demand(long rawRequired, long centralRaw, long otherRaw,
                                long firedRequired, long centralFired, long otherFired, long reserve) {
        long rawMissing = Math.max(0, rawRequired - centralRaw);
        long firedMissing = Math.max(0, firedRequired - centralFired - otherFired);
        long uncommitted = Math.max(0, centralRaw - rawRequired) + otherRaw;
        long constructionShortage = Math.max(0, rawMissing + firedMissing - uncommitted);
        return new Demand(Math.max(0, rawMissing + firedMissing + Math.max(0, reserve) - uncommitted), constructionShortage);
    }
}
