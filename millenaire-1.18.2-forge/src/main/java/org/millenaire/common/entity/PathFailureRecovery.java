package org.millenaire.common.entity;
/** Short-lived route failures should be retried before using emergency relocation. */
public final class PathFailureRecovery {
    private long firstFailure = -1;
    private int failures;
    public boolean failed(long tick) {
        if (firstFailure < 0 || tick < firstFailure) { firstFailure = tick; failures = 0; }
        failures++;
        if (failures >= 3 && tick - firstFailure >= 200) { reset(); return true; }
        return false;
    }
    public void reset() { firstFailure = -1; failures = 0; }
}
