package org.millenaire.common.village;

/** Pure scoring for a bounded population; does not change herd limits. */
public final class PenAllocationRules {
    private PenAllocationRules() {}
    public static int score(int count,int capacity,int markerCount) {
        return (count==1?100000:count==0?50000:0)
                -1000*count/Math.max(1,capacity)-markerCount;
    }
}
