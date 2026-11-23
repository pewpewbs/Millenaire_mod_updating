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
        System.out.println("PASS: emergency front-door geometry");
    }
}
