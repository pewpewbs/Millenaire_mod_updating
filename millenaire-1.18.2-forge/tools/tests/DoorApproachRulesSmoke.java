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
        System.out.println("PASS: emergency front-door geometry");
    }
}
