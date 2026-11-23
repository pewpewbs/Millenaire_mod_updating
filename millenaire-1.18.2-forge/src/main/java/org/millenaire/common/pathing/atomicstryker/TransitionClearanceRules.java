package org.millenaire.common.pathing.atomicstryker;

/**
 * Small, dependency-free movement-envelope sampler for one neighbouring path edge.
 *
 * This is intentionally conservative where the path follower must jump: a route
 * requiring a one-block rise needs space for the villager at the raised height
 * throughout the transition. Flat edges may step across half-height geometry,
 * but must also have headroom for the controller's jump recovery. The collision
 * callback uses real block voxel shapes, not block-level passability flags.
 */
public final class TransitionClearanceRules {
    public static final double VILLAGER_WIDTH = 0.60D;
    public static final double VILLAGER_HEIGHT = 1.95D;
    private static final double STEP_UP = 0.625D;
    private static final int SAMPLES = 8;

    @FunctionalInterface
    public interface Clearance {
        boolean isClear(double centerX, double feetY, double centerZ);
    }

    private TransitionClearanceRules() {}

    public static boolean canTraverse(int fromX, int fromY, int fromZ,
                                      int toX, int toY, int toZ,
                                      Clearance clearance) {
        final int dx = toX - fromX;
        final int dy = toY - fromY;
        final int dz = toZ - fromZ;
        if (Math.abs(dx) > 1 || Math.abs(dz) > 1 || Math.abs(dy) > 2) return false;
        if (dx == 0 && dz == 0) {
            // Only the dedicated ladder expansion is allowed to create this
            // edge. Its consecutive-rung and head checks are performed there.
            return Math.abs(dy) == 1;
        }

        for (int sample = 0; sample <= SAMPLES; sample++) {
            double t = (double) sample / SAMPLES;
            double x = fromX + 0.5D + dx * t;
            double z = fromZ + 0.5D + dz * t;
            double feetY;
            if (dy > 0) {
                // A full-block upward edge involves a jump. Check the
                // raised body along the entire horizontal approach.
                feetY = toY;
            } else if (dy < 0) {
                // Remain at the high level until the body has cleared the
                // departure cell, then test the landing at the low level.
                feetY = t < 0.875D ? fromY : toY;
            } else {
                feetY = fromY;
            }

            if (clearance.isClear(x, feetY, z)) continue;

            // Stair/slab faces can project into the nominal (integer Y)
            // corridor. A 0.5-block step may avoid that floor collision,
            // but requires full jump headroom: the movement controller may
            // initiate a jump instead of stepping smoothly. A full block
            // cannot be bypassed by this small upward adjustment.
            if (dy == 0
                    && clearance.isClear(x, feetY + STEP_UP, z)
                    && clearance.isClear(x, feetY + 1.0D, z)) {
                continue;
            }
            return false;
        }
        return true;
    }
}
