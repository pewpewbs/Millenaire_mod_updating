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
        var shared = new RouteMemory(1, 32, 10);
        var street = new java.util.ArrayList<RouteMemory.Cell>();
        for (int x = 24; x >= 8; x--) street.add(cell(x, 64, 0));
        shared.remember(100, 7, street);
        int hits = 0, exact = 0;
        for (int from = -2; from <= 2; from++) {
            for (int to = -2; to <= 2; to++) {
                var candidate = shared.lookup(101,7,cell(8,64,from),cell(24,64,to));
                if (candidate != null) {
                    hits++;
                    if (!candidate.corridor()) exact++;
                }
            }
        }
        test(hits==25 && exact==1, "24 nearby pairs plus the exact pair find the same street");
        System.out.println("Street fixture: "+exact+" exact, "+(hits-exact)+" nearby candidates in 25 trips");
        var origin = cell(7, 64, 1);
        var goal = cell(23, 64, 2);
        var joined = shared.lookup(101, 7, origin, goal);
        test(joined != null && joined.corridor() && !joined.reversed(), "offset grid across zone boundary");
        var walk = joined.goalToStart();
        test(walk.get(0).equals(goal) && walk.get(walk.size()-1).equals(origin)
                && walk.contains(cell(16,64,0)), "both new endpoints connected to original middle");
        for (int i=1; i<walk.size(); i++) {
            var p = walk.get(i-1); var q = walk.get(i);
            test(Math.abs(p.x()-q.x()) + Math.abs(p.z()-q.z()) == 1 && p.y()==q.y(), "unit edges");
        }
        test(shared.lookup(101,7,goal,origin).reversed(), "opposite nearby trip");
        test(shared.lookup(101,7,cell(7,65,1),goal)==null, "different floors are independent");
        test(shared.lookup(101,8,origin,goal)==null, "corridor respects movement settings");
        test(shared.lookup(101,7,cell(7,64,5),goal)==null, "distant room cannot borrow approach");
        test(shared.lookup(111,7,origin,goal)==null, "expired corridor cannot be replayed");
        shared.remember(112,7,street); shared.remember(113,7,List.of(b,rung,a));
        test(shared.lookup(113,7,origin,goal)==null, "eviction removes secondary index");
        test(shared.corridorBudget(113)==1_000_000L,"new world tick provides a corridor budget");
        shared.chargeCorridor(113,700_000L);
        test(shared.corridorBudget(113)==300_000L,"all residents draw on one budget");
        shared.chargeCorridor(113,500_000L);
        test(shared.corridorBudget(113)==0 && shared.corridorBudget(114)==1_000_000L,"exhaustion and rollover");
        System.out.println("PASS: forward/reverse lookup, independent directions, TTL, config, LRU, revision and isolation");
    }
}
