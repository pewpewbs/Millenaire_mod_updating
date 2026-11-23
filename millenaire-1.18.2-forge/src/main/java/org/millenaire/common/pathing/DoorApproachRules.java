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
     * Stop an emergency SCANNER from reopening the same wooden door every
     * 10-15 ticks while the same resident is stuck at the same coordinates.
     * This does NOT affect path-node opening, gates, other residents, or a
     * resident that has actually advanced through the doorway.
     *
     * The bounded 120-tick cooldown allows a later retry rather than
     * permanently blocking a resident whose navigation is still stuck.
     */
    public static boolean suppressStationaryScannerReopen(
            boolean sameDoor, boolean sameGoalAndDestination,
            double movedSquared, long ticksSinceOpen) {
        return sameDoor && sameGoalAndDestination && ticksSinceOpen >= 0L
                && ticksSinceOpen < 120L && movedSquared < 0.25D;
    }

    /**
     * A door that FACES east/west has its closed panel in the Z plane,
     * so crossing its doorway is along X; north/south doors cross along Z.
     * When opened, the leaf swings onto a perpendicular edge of the same
     * block. Traversing the door ALONG that leaf leads to real collisions.
     * Ignore vertical-only steps here; the caller separately validates Y.
     */
    public static boolean followsDoorNormal(int stepX, int stepZ, boolean eastWestFacing) {
        if (stepX == 0 && stepZ == 0) return true;
        return eastWestFacing ? stepX != 0 && stepZ == 0
                              : stepZ != 0 && stepX == 0;
    }

    /**
     * The physical closed panel sits along one edge of the door BLOCK.
     * Use the closest point on its horizontal collision bounds instead of
     * the block center: both sides of the same door must work symmetrically.
     *
     * min and max are world-space coordinates (block origin already added).
     */
    public static double closestPanelCoordinate(double resident, double min, double max) {
        return Math.max(min, Math.min(max, resident));
    }

    /**
     * A pending path node can still name the DOOR BLOCK after its thin panel
     * has already been crossed. Stop renewing its open lease when the original
     * closed panel is behind the path's horizontal direction; the world-tick
     * closer separately waits for bodies actually occupying the doorway.
     */
    public static boolean passedPanel(double pathDx, double pathDz,
            double panelOffsetX, double panelOffsetZ) {
        double length = Math.sqrt(pathDx * pathDx + pathDz * pathDz);
        return length > 0.01D
                && (panelOffsetX * pathDx + panelOffsetZ * pathDz) / length < -0.10D;
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
