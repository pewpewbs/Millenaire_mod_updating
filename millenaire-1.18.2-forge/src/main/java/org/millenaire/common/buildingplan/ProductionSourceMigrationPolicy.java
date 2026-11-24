package org.millenaire.common.buildingplan;

/** Pure decision for the narrowly scoped stock quarry upgrade. */
public final class ProductionSourceMigrationPolicy {
    private ProductionSourceMigrationPolicy() {}

    public enum Decision { WAIT, KEEP, REGISTER, REPLACE }

    public static Decision decide(boolean loaded, boolean expectedMarker, boolean unchanged,
            boolean oldRegistered, boolean newRegistered, boolean oldBlock, boolean newBlock,
            boolean supported, boolean clearAbove) {
        if (!expectedMarker || !unchanged) return Decision.KEEP;
        if (!loaded) return Decision.WAIT;
        // Recover from a world save succeeding before the building metadata save.
        if (newBlock) return oldRegistered || !newRegistered ? Decision.REGISTER : Decision.KEEP;
        // Never recreate a missing, replaced, or unregistered old resource.
        if (oldRegistered && oldBlock && supported && clearAbove) return Decision.REPLACE;
        return Decision.KEEP;
    }
}
