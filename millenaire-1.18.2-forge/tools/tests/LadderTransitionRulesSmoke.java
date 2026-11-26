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
        test(LadderTransitionRules.reachedRung(0, 64.0, 64, 0.02),
                "horizontal single-rung route can complete on contact");
        test(!LadderTransitionRules.reachedRung(0, 65.0, 64, 0.02),
                "single rung cannot complete one block above its height");
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
        test(LadderTransitionRules.shouldApproach(true, 1.0D),
                "approach the next ladder rung from adjacent ground");
        test(!LadderTransitionRules.shouldApproach(false, 0.1D),
                "do not attract a resident toward an unrelated ladder");
        test(!LadderTransitionRules.shouldApproach(true, 3.0D),
                "do not drag the resident from a distant path");
        test(LadderTransitionRules.approachSpeed(5.0D) <= 0.085D,
                "positive approach bounded");
        test(LadderTransitionRules.approachSpeed(-5.0D) >= -0.085D,
                "negative approach bounded");
        test(Math.abs(LadderTransitionRules.approachSpeed(0.0D)) < 0.0001D,
                "no unwanted horizontal drift at ladder");
        // LADDER-03 reproduction: the current rung lies below the villager
        // after a small overshoot, but the next rung is still ABOVE it.
        test(LadderTransitionRules.routeDirection(61, true, 62, 63, true) == 1,
                "ascending despite negative local dy to rung 62");
        test(LadderTransitionRules.routeDirection(63, true, 62, 61, true) == -1,
                "descending through consecutive rungs");
        test(LadderTransitionRules.routeDirection(61, true, 62, 62, false) == 1,
                "last ascent rung before the platform");
        test(LadderTransitionRules.routeDirection(63, true, 62, 62, false) == -1,
                "last descent rung");
        test(LadderTransitionRules.routeDirection(62, false, 62, 62, false) == 0,
                "unconnected ladder node must not invent a climb direction");
        test(LadderTransitionRules.routeDirection(61, true, 62, 61, true) == -1,
                "upcoming rung takes precedence at a route reversal");

        test(LadderTransitionRules.reachedRung(1, 62.31D, 62, 0.04D),
                "upward rung reached after slight overshoot");
        test(!LadderTransitionRules.reachedRung(1, 61.90D, 62, 0.04D),
                "do not advance upward before actual height reached");
        test(LadderTransitionRules.reachedRung(-1, 62.50D, 62, 0.04D),
                "descending rung reached at its safe midpoint");
        test(!LadderTransitionRules.reachedRung(-1, 62.70D, 62, 0.04D),
                "do not advance descending rung too early");
        test(!LadderTransitionRules.reachedRung(1, 62.31D, 62, 0.40D),
                "off-center body may not skip rung");

        test(LadderTransitionRules.shouldClimb(1, true, true, false, 0.03D),
                "upward movement comes from positive route direction");
        test(LadderTransitionRules.shouldClimb(-1, true, true, false, 0.03D),
                "downward movement only on descending route");
        test(!LadderTransitionRules.shouldClimb(1, true, true, true, 0.03D),
                "do not over-climb an already reached rung");
        test(!LadderTransitionRules.shouldClimb(1, false, true, false, 0.03D),
                "do not lift from off-shaft position");
        test(!LadderTransitionRules.shouldClimb(1, true, false, false, 0.03D),
                "do not lift without actual ladder contact");

        test(LadderTransitionRules.needsTopOut(true, true, 67, 68, 67.30D, 0.05D),
                "climb the last block to upper landing");
        test(!LadderTransitionRules.needsTopOut(true, true, 67, 68, 68.10D, 0.05D),
                "hand back control after clearing the upper lip");
        test(!LadderTransitionRules.needsTopOut(true, false, 67, 68, 67.30D, 0.05D),
                "do not float above a ladder after contact is lost");
        test(!LadderTransitionRules.needsTopOut(true, true, 67, 66, 66.30D, 0.05D),
                "do not top-out a downward exit");
        test(!LadderTransitionRules.needsTopOut(false, true, 67, 68, 67.30D, 0.05D),
                "ordinary path must not trigger ladder exit assist");

        System.out.println("PASS: ladder route direction, rung progression, exit and contact");
    }
}
