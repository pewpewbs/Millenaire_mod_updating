import org.millenaire.common.pathing.DoorApproachRules;

/**
 * Dependency-free Java 17 smoke test, from millenaire-1.18.2-forge:
 * javac --release 17 -d build/door-smoke src/main/java/org/millenaire/common/pathing/DoorApproachRules.java tools/tests/DoorApproachRulesSmoke.java
 * java -cp build/door-smoke DoorApproachRulesSmoke
 *
 * Visibility/door lease behavior must still be checked in Forge/Minecraft.
 */
public final class DoorApproachRulesSmoke {
    private static void test(boolean okay, String label) {
        if (!okay) throw new AssertionError(label);
    }

    public static void main(String[] args) {
        test(Double.isFinite(DoorApproachRules.score(1, 0, 1.2, 0, 0)),
                "closed doorway ahead");
        test(!Double.isFinite(DoorApproachRules.score(1, 0, -1.2, 0, 0)),
                "must not open a door behind resident");
        test(!Double.isFinite(DoorApproachRules.score(1, 0, 1.0, 1.4, 0)),
                "must not open unrelated side doors");
        test(!Double.isFinite(DoorApproachRules.score(1, 0, 2.8, 0, 0)),
                "must not reach distant doors");
        test(!Double.isFinite(DoorApproachRules.score(1, 0, 1, 0, 2)),
                "must not reach different floor");
        test(DoorApproachRules.score(1, 0, 0.8, 0.05, 0)
                < DoorApproachRules.score(1, 0, 1.6, 0.3, 0),
                "prefer closer, aligned doorway");
        test(DoorApproachRules.clearSight(false, false),
                "thin closed door missed by center ray must still be reachable");
        test(DoorApproachRules.clearSight(true, true),
                "ray actually touching door is valid");
        test(!DoorApproachRules.clearSight(true, false),
                "solid wall before doorway must block opening");
        double offsetDoorWalking = DoorApproachRules.recoveryScore(
                1, 0, 0.7, 1.1, 0.0, false);
        double offsetDoorStalled = DoorApproachRules.recoveryScore(
                1, 0, 0.7, 1.1, 0.0, true);
        test(!Double.isFinite(offsetDoorWalking),
                "offset door must stay closed while resident moves normally");
        test(Double.isFinite(offsetDoorStalled),
                "physically stalled villager may recover an offset nearby door");
        test(!Double.isFinite(DoorApproachRules.recoveryScore(
                1, 0, -1.2, 0, 0, true)),
                "even a stalled resident must not open a door behind it");
        test(!Double.isFinite(DoorApproachRules.recoveryScore(
                1, 0, 1.2, 1.7, 0, true)),
                "recovery must not open a distant sideways door");
        test(!Double.isFinite(DoorApproachRules.recoveryScore(
                1, 0, 0.8, 1, 2.0, true)),
                "recovery must not reach a different floor");
        test(Double.isFinite(DoorApproachRules.recoveryScore(
                1, 0, 1.2, 0, 0, false)),
                "ordinary forward door still opens without stalling");
        // Closed oak doors occupy a thin collision plane on one of four
        // edges of their BLOCK. Test BOTH approach directions along X and Z.
        // Near-face placement: the door panel is at world X = 10.0..10.1875.
        double panelFromWest = DoorApproachRules.closestPanelCoordinate(9.6, 10.0, 10.1875);
        double panelFromEast = DoorApproachRules.closestPanelCoordinate(10.4, 10.0, 10.1875);
        test(Double.isFinite(DoorApproachRules.score(
                1, 0, panelFromWest - 9.6, 0, 0)),
                "wooden door must open from west side");
        test(Double.isFinite(DoorApproachRules.score(
                -1, 0, panelFromEast - 10.4, 0, 0)),
                "same wooden door must open from east side");
        // Far-face placement: the panel is at the other edge of the block.
        double farFromWest = DoorApproachRules.closestPanelCoordinate(10.6, 10.8125, 11);
        double farFromEast = DoorApproachRules.closestPanelCoordinate(11.4, 10.8125, 11);
        test(Double.isFinite(DoorApproachRules.score(
                1, 0, farFromWest - 10.6, 0, 0)),
                "far-edge panel must open from west side");
        test(Double.isFinite(DoorApproachRules.score(
                -1, 0, farFromEast - 11.4, 0, 0)),
                "far-edge panel must open from east side");
        // A block-center approach would reject these even though the thin
        // panel is directly AHEAD of the approaching resident.
        test(!Double.isFinite(DoorApproachRules.score(
                -1, 0, 10.5 - 10.4, 0, 0)),
                "old block-center logic incorrectly rejects reverse approach");
        double panelFromNorth = DoorApproachRules.closestPanelCoordinate(9.6, 10.0, 10.1875);
        double panelFromSouth = DoorApproachRules.closestPanelCoordinate(10.4, 10.0, 10.1875);
        test(Double.isFinite(DoorApproachRules.score(
                0, 1, 0, panelFromNorth - 9.6, 0)),
                "north approach to Z-facing panel");
        test(Double.isFinite(DoorApproachRules.score(
                0, -1, 0, panelFromSouth - 10.4, 0)),
                "south approach to Z-facing panel");

        // After crossing, the original panel must be BEHIND the direction
        // of travel so a path node cannot renew its auto-close lease.
        test(DoorApproachRules.passedPanel(1, 0, 10.1875 - 10.5, 0),
                "stop lease after crossing eastward");
        test(DoorApproachRules.passedPanel(-1, 0, 10.8125 - 10.5, 0),
                "stop lease after crossing westward");
        test(DoorApproachRules.passedPanel(0, 1, 0, 10.1875 - 10.5),
                "stop lease after crossing southward");
        test(DoorApproachRules.passedPanel(0, -1, 0, 10.8125 - 10.5),
                "stop lease after crossing northward");
        test(!DoorApproachRules.passedPanel(1, 0, 10.8125 - 10.5, 0),
                "renew lease while approaching panel");
        // Reproduces Giannis at (30,66,554): repeatedly reopening
        // door (31,66,553) via SCANNER without moving along route 3/11.
        test(DoorApproachRules.suppressStationaryScannerReopen(
                true, true, 0.0D, 15L),
                "same stuck resident must not reopen door every 15 ticks");
        test(DoorApproachRules.suppressStationaryScannerReopen(
                true, true, 0.01D, 119L),
                "bounded retry cooldown holds even with tiny shuffling");
        test(!DoorApproachRules.suppressStationaryScannerReopen(
                true, true, 0.0D, 120L),
                "genuinely stuck resident can try again after cooldown");
        test(!DoorApproachRules.suppressStationaryScannerReopen(
                true, true, 0.26D, 15L),
                "actual movement resets scanner retry guard");
        test(!DoorApproachRules.suppressStationaryScannerReopen(
                true, false, 0.0D, 15L),
                "new goal or destination resets scanner retry guard");
        test(!DoorApproachRules.suppressStationaryScannerReopen(
                false, true, 0.0D, 15L),
                "another door can still be opened");
        test(!DoorApproachRules.suppressStationaryScannerReopen(
                true, true, 0.0D, -1L),
                "negative elapsed ticks after reload do not suppress opening");
        // Reported Minecraft collision: door faces WEST/EAST, but planner
        // routes from z=547 to z=548 (parallel to the swung OPEN leaf).
        test(!DoorApproachRules.followsDoorNormal(0, 1, true),
                "west-facing door cannot be traversed along Z");
        test(!DoorApproachRules.followsDoorNormal(0, -1, true),
                "east-facing door cannot be traversed along Z");
        test(DoorApproachRules.followsDoorNormal(1, 0, true),
                "west/east door can be crossed normally in +X");
        test(DoorApproachRules.followsDoorNormal(-1, 0, true),
                "west/east door can be crossed normally in -X");
        test(!DoorApproachRules.followsDoorNormal(1, 0, false),
                "north-facing door cannot be traversed along X");
        test(!DoorApproachRules.followsDoorNormal(-1, 0, false),
                "south-facing door cannot be traversed along X");
        test(DoorApproachRules.followsDoorNormal(0, 1, false),
                "north/south door can be crossed normally in +Z");
        test(DoorApproachRules.followsDoorNormal(0, -1, false),
                "north/south door can be crossed normally in -Z");
        test(!DoorApproachRules.followsDoorNormal(1, 1, true),
                "diagonal entry cannot graze an open door leaf");
        test(!DoorApproachRules.followsDoorNormal(1, 1, false),
                "diagonal entry cannot graze either hinge");
        test(DoorApproachRules.followsDoorNormal(0, 0, true),
                "vertical-only ladder movement is unaffected");
        System.out.println("PASS: emergency front-door geometry");
    }
}
