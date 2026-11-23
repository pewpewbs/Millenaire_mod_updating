package org.millenaire.common.pathing;

/** Pure geometry checks for an emergency interaction with a blocked doorway. */
public final class DoorApproachRules {
    private DoorApproachRules() {}

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
