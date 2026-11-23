import org.millenaire.common.pathing.LadderTransitionRules;

/**
 * Dependency-free Java 17 smoke test. From millenaire-1.18.2-forge:
 * javac --release 17 -d build/ladder-smoke src/main/java/org/millenaire/common/pathing/LadderTransitionRules.java tools/tests/LadderTransitionRulesSmoke.java
 * java -cp build/ladder-smoke LadderTransitionRulesSmoke
 *
 * Forge integration tests are still required for pathfinding, ladder physics
 * and safely stepping out beside the quarry gate.
 */
public final class LadderTransitionRulesSmoke {
    private static void test(boolean passed, String label) {
        if (!passed) throw new AssertionError(label);
    }

    public static void main(String[] args) {
        test(LadderTransitionRules.canTraverse(true, true, true, true),
                "continuous clear ladder should be traversable");
        test(!LadderTransitionRules.canTraverse(true, false, true, true),
                "cannot climb through missing rung");
        test(!LadderTransitionRules.canTraverse(true, true, false, true),
                "blocked feet reject edge");
        test(!LadderTransitionRules.canTraverse(true, true, true, false),
                "blocked head rejects edge");
        test(!LadderTransitionRules.canTraverse(false, true, true, true),
                "must already be on a ladder");
        test(LadderTransitionRules.shouldAssist(true, true, 0.8, 0.05),
                "ascending inside ladder column");
        test(LadderTransitionRules.shouldAssist(true, true, -0.8, 0.05),
                "descending inside ladder column");
        test(!LadderTransitionRules.shouldAssist(false, true, 0.8, 0.05),
                "no vertical help to off-column route node");
        test(!LadderTransitionRules.shouldAssist(true, false, 0.8, 0.05),
                "no airborne lift outside ladder");
        test(!LadderTransitionRules.shouldAssist(true, true, 0.8, 0.8),
                "no snapping from far outside ladder shaft");
        test(!LadderTransitionRules.shouldAssist(true, true, 0.1, 0.05),
                "no vertical boost near the current rung");
        System.out.println("PASS: ladder edges and vertical climb assistance");
    }
}
