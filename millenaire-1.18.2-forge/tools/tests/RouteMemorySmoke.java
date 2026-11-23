import java.util.List;
import org.millenaire.common.pathing.RouteMemory;

/**
 * Standalone Java 17 regression suite:
 * javac --release 17 -d build/route-cache-smoke \
 *   src/main/java/org/millenaire/common/pathing/RouteMemory.java \
 *   tools/tests/RouteMemorySmoke.java
 * java -cp build/route-cache-smoke RouteMemorySmoke
 *
 * Cached coordinates are NOT a licence to use them without real-world,
 * directed transition validation (done by AStarPathPlannerJPS).
 */
public final class RouteMemorySmoke {
    private static RouteMemory.Cell cell(int x, int y, int z) {
        return new RouteMemory.Cell(x, y, z);
    }
    private static void test(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        RouteMemory memory = new RouteMemory(2, 4, 10);
        var a = cell(0, 64, 0);
        var rung = cell(1, 64, 0);
        var b = cell(1, 65, 0);
        var noCandidates = memory.candidatesWithRevision(99, 7, a, b);
        test(noCandidates.matches().isEmpty() && noCandidates.revision() == 0L,
                "atomic empty-candidate revision begins at zero");
        // Route format follows Millenaire: target -> start.
        memory.remember(100, 7, List.of(b, rung, a));
        test(memory.revision() == 1L, "new stored route increments revision");
        test(!memory.candidatesWithRevision(101, 7, a, b).matches().isEmpty(),
                "route remembered after a miss must become visible");
        var direct = memory.lookup(101, 7, a, b);
        test(direct != null && !direct.reversed(), "forward cache hit");
        test(direct.goalToStart().equals(List.of(b, rung, a)), "correct forward order");
        var reverse = memory.lookup(102, 7, b, a);
        test(reverse != null && reverse.reversed(), "reverse cache hit");
        test(reverse.goalToStart().equals(List.of(a, rung, b)), "reverse order must swap endpoints");
        test(memory.lookup(102, 8, a, b) == null, "different path configuration is not reusable");
        test(memory.lookup(111, 7, a, b) == null, "TTL expires old route");
        test(memory.revision() >= 2L,
                "expired route invalidates the remembered empty-result revision");

        memory.remember(112, 7, List.of(b, rung, a));
        // Both directions can exist with independent, possibly different costs.
        memory.remember(112, 7, List.of(a, cell(0, 65, 0), b));
        test(memory.candidates(112, 7, a, b).size() == 2,
                "allow cost comparison of validated forward and reverse candidates");
        long beforeForget = memory.revision();
        memory.forget(112, 7, a, b, true);
        test(memory.revision() == beforeForget + 1L,
                "forgotten path invalidates old empty-result revision");
        memory.forget(112, 7, a, b, true);
        test(memory.revision() == beforeForget + 1L,
                "forgetting an already-absent route must not claim a cache mutation");
        test(memory.candidates(112, 7, a, b).size() == 1,
                "invalidated reverse entry must not poison forward candidate");

        RouteMemory limited = new RouteMemory(2, 4, 10);
        limited.remember(1, 1, List.of(cell(1, 64, 0), cell(0, 64, 0)));
        limited.remember(1, 1, List.of(cell(2, 64, 0), cell(0, 64, 0)));
        limited.remember(1, 1, List.of(cell(3, 64, 0), cell(0, 64, 0)));
        test(limited.size() == 2 && limited.lookup(1, 1, cell(0, 64, 0), cell(1, 64, 0)) == null,
                "bounded LRU eviction");
        test(new RouteMemory(2, 4, 10).size() == 0,
                "world-specific cache instances never share entries");
        System.out.println("PASS: forward/reverse lookup, independent directions, TTL, config, LRU, revision and isolation");
    }
}
