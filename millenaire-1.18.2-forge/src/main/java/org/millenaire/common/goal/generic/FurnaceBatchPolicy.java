package org.millenaire.common.goal.generic;

/** Batch thresholds shared by NPC cooking and tending; no world mutations. */
public final class FurnaceBatchPolicy {
    private FurnaceBatchPolicy() {}
    public static final int INPUT_LOW = 8;
    public static final int OUTPUT_BATCH = 32;
    public static final int FUEL_LOW = 4;
    public static int capacity(int slotLimit, int stackLimit) {
        return Math.max(0, Math.min(64, Math.min(slotLimit, stackLimit)));
    }
    public static int minimumBatch(int configured, int capacity) {
        return Math.max(1, Math.min(configured, capacity));
    }
    public static boolean inputDue(int current, int capacity) {
        return capacity > 0 && current < capacity && current <= Math.min(INPUT_LOW, capacity / 2);
    }
    public static boolean fuelDue(int current, int capacity) {
        // NPC fuel is made from whole logs: one log supplies four planks.
        return capacity - current >= 4 && current <= Math.min(FUEL_LOW, capacity / 2);
    }
    public static boolean outputDue(int count, int capacity, boolean canContinue) {
        return count > 0 && (count >= Math.max(1, Math.min(OUTPUT_BATCH, capacity)) || !canContinue);
    }
}
