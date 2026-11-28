import java.util.ArrayList;
import java.util.List;
import org.millenaire.common.pathing.atomicstryker.TransitionClearanceRules;

/**
 * Dependency-free regression checks for STAIR-01.
 *
 * From millenaire-1.18.2-forge:
 *   javac --release 17 -d build/transition-smoke src/main/java/org/millenaire/common/pathing/atomicstryker/TransitionClearanceRules.java tools/tests/TransitionClearanceRulesSmoke.java
 *   java -cp build/transition-smoke TransitionClearanceRulesSmoke
 *
 * The boxes emulate the actual collision shapes used by AStarStatic.
 * A Forge compile and in-game test are separate requirements.
 */
public final class TransitionClearanceRulesSmoke {
    private record Box(double x1, double y1, double z1, double x2, double y2, double z2) {}
    private static final double HALF_WIDTH = TransitionClearanceRules.VILLAGER_WIDTH / 2.0D;

    private static boolean clear(List<Box> obstacles, double x, double y, double z) {
        for (Box b : obstacles) {
            if (x + HALF_WIDTH > b.x1 && x - HALF_WIDTH < b.x2
                    && y + TransitionClearanceRules.VILLAGER_HEIGHT > b.y1 && y < b.y2
                    && z + HALF_WIDTH > b.z1 && z - HALF_WIDTH < b.z2) {
                return false;
            }
        }
        return true;
    }

    private static boolean route(int fromY, int toY, Box... obstacles) {
        List<Box> blocks = List.of(obstacles);
        return TransitionClearanceRules.canTraverse(
                0, fromY, 0, 1, toY, 0, (x, y, z) -> clear(blocks, x, y, z));
    }

    private static void expect(boolean actual, boolean expected, String scenario) {
        if (actual != expected) {
            throw new AssertionError(scenario + ": expected " + expected + ", got " + actual);
        }
    }

    public static void main(String[] args) {
        Box stairHalf = new Box(1, 64, 0, 2, 64.5, 1);
        Box topSlab = new Box(1, 66.5, 0, 2, 67, 1);
        Box solid = new Box(1, 64, 0, 2, 65, 1);

        expect(route(64, 64), true, "ordinary flat passage");
        expect(route(64, 64, stairHalf), true, "stair face with open headroom");
        expect(route(64, 64, stairHalf, topSlab), false,
                "stair step below 2.5-block ceiling must not be selected");
        boolean reverseUnderSlab = TransitionClearanceRules.canTraverse(
                1, 64, 0, 0, 64, 0,
                (x, y, z) -> clear(List.of(stairHalf, topSlab), x, y, z));
        expect(reverseUnderSlab, false, "reverse passage past the same covered stair");
        boolean reverseInOpenAir = TransitionClearanceRules.canTraverse(
                1, 64, 0, 0, 64, 0,
                (x, y, z) -> clear(List.of(stairHalf), x, y, z));
        expect(reverseInOpenAir, true, "reverse stair crossing in open headroom");
        expect(route(64, 64, solid), false,
                "full solid block cannot masquerade as a half-height step");
        expect(route(64, 65, solid), true, "one-block jump with open headroom");
        expect(route(64, 65, solid, topSlab), false,
                "full jump blocked by slab on approach");

        // A diagonal must not squeeze between two full-width obstructing cells.
        List<Box> corners = new ArrayList<>();
        corners.add(new Box(1, 64, 0, 2, 66, 1));
        corners.add(new Box(0, 64, 1, 1, 66, 2));
        boolean diagonal = TransitionClearanceRules.canTraverse(
                0, 64, 0, 1, 64, 1, (x, y, z) -> clear(corners, x, y, z));
        expect(diagonal, false, "diagonal corner clipping");

        System.out.println("PASS: flat passage, stair clearance, slab ceiling, jump and diagonal");
    }
}
