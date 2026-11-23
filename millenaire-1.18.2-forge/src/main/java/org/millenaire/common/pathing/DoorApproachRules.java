package org.millenaire.common.pathing;

/** Pure geometry checks for an emergency interaction with a blocked doorway. */
public final class DoorApproachRules {
    private DoorApproachRules() {}

    /**
     * A door panel can be thinner than a ray's centerline. A MISS is therefore
     * unobstructed; only a hit on a different solid block blocks access.
     */
    public static boolean clearSight(boolean hitBlock, boolean hitDoor) {
        return !hitBlock || hitDoor;
    }

    /**
     * Doors are not guaranteed to be explicit path nodes: JPS can omit a door,
     * and a partial route may aim at an offset access node. Do not loosen the
     * approach rule for a walking resident. After prolonged PHYSICAL stalling,
     * allow one nearby door in the forward half-plane, with a wider side margin.
     * The caller additionally checks block visibility and player priority.
     */
    public static double recoveryScore(double dx, double dz, double ox, double oz,
            double verticalDifference, boolean physicallyStalled) {
        double standard = score(dx, dz, ox, oz, verticalDifference);
        if (Double.isFinite(standard)) return standard;
        if (!physicallyStalled) return Double.POSITIVE_INFINITY;

        double forward = ox * dx + oz * dz;
        double side = Math.abs(ox * dz - oz * dx);
        double distanceSquared = ox * ox + oz * oz;
        if (forward < -0.10D || forward > 2.2D || side > 1.45D
                || distanceSquared > 4.84D || verticalDifference > 1.5D)
            return Double.POSITIVE_INFINITY;

        // Always prefer the tightly aligned normal candidate to a rescue one.
        return 3.0D + distanceSquared + side;
    }

    /**
     * @param dx normalized intended horizontal movement on X
     * @param dz normalized intended horizontal movement on Z
     * @param ox horizontal door offset on X
     * @param oz horizontal door offset on Z
     * @param verticalDifference absolute feet-to-door elevation difference
     * @return the door's approach cost, or infinity when out of safe range
     */
    public static double score(double dx, double dz, double ox, double oz,
            double verticalDifference) {
        double forward = ox * dx + oz * dz;
        double side = Math.abs(ox * dz - oz * dx);
        if (forward < 0.10D || forward > 2.2D
                || side > 0.85D || ox * ox + oz * oz > 5.0D
                || verticalDifference > 1.5D) return Double.POSITIVE_INFINITY;
        return forward + side;
    }
}
