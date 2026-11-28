package org.millenaire.common.block;

import java.util.Random;

/** All kiln levels dry at half their previous rate, preserving their relative upgrades. */
public final class BrickDryingPolicy {
    private BrickDryingPolicy() {}
    public static final int NORMAL_LEVEL = 1;
    public static final int DRYING_TICK_DIVISOR = 2;

    public static int advance(int progress, int internalLevel, Random random) {
        if (random.nextInt(DRYING_TICK_DIVISOR) != 0) return progress;
        if (internalLevel == 0 && random.nextInt(10) == 0) return progress;
        int next = step(progress, random);
        if (internalLevel >= 2 && next < 3 && random.nextInt(5) == 0)
            next = step(next, random);
        return next;
    }

    private static int step(int progress, Random random) {
        int next = progress + 1;
        if (next < 2 && random.nextBoolean()) next++;
        return next;
    }

    /** Mean qualified random ticks: 2.5/0.9, 2.5, or 2.5-1.5*0.2+0.5*0.2^2. */
    public static int meanSeconds(int internalLevel) {
        double ticks = internalLevel == 0 ? 2.5 / 0.9 : internalLevel >= 2 ? 2.22 : 2.5;
        return (int)Math.round(ticks * DRYING_TICK_DIVISOR * 4096.0 / (3 * 20));
    }
}
