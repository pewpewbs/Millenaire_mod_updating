"""Source-level wiring smoke checks for NAV-OPT.

Run from millenaire-1.18.2-forge:
    python3 tools/tests/test_navigation_optimization_wiring.py

This tests architecture invariants, NOT Minecraft physics or Forge compilation.
"""
from pathlib import Path
import unittest

JAVA = Path(__file__).resolve().parents[2] / "src/main/java/org/millenaire/common"
def read(path):
    return (JAVA / path).read_text(encoding="utf-8")

PLANNER = read("pathing/atomicstryker/AStarPathPlannerJPS.java")
WORKER = read("pathing/atomicstryker/AStarWorker.java")
VILLAGER = read("entity/MillVillager.java")
SNAPSHOT = read("pathing/PathWorldSnapshot.java")
METRICS = read("pathing/NavigationMetrics.java")
MEMORY = read("pathing/RouteMemory.java")

class OptimizationWiring(unittest.TestCase):
    def test_route_cache_always_checks_current_world(self):
        cache = PLANNER.split("private ArrayList<AStarNode> reusePath(", 1)[1]
        cache = cache.split("private void rememberPath(", 1)[0]
        self.assertIn("AStarStatic.isViable(this.world, to", cache)
        self.assertIn("AStarStatic.isTransitionViable(this.world, from, to", cache)
        self.assertIn("findSafeOrthogonalCorner", cache)
        self.assertIn("memory.forget(", cache)
        self.assertIn("cacheStale()", cache)

    def test_cache_world_isolated_and_bounded(self):
        self.assertIn("new java.util.WeakHashMap<>()", PLANNER)
        self.assertIn("new org.millenaire.common.pathing.RouteMemory(96, 96, 200L)", PLANNER)
        self.assertIn("lifetimeTicks", MEMORY)
        self.assertIn("removeEldestEntry", MEMORY)

    def test_deferred_is_not_mistaken_for_missing_path(self):
        start = VILLAGER.index('this.pathPlannerJPS.getPath(this.doubleToInt(')
        section = VILLAGER[start:start+1800]
        self.assertIn("hadImmediatePath()", section)
        self.assertIn("wasDeferred()", section)
        self.assertIn('logRouteDecision("ASTAR-NO-WORKER"', section)
        self.assertNotIn("onNoPathAvailable();", section.split("wasDeferred()", 1)[1].split("} else {", 1)[0])

    def test_reverse_search_is_bounded_and_validated_directionally(self):
        reverse = WORKER.split("private ArrayList<AStarNode> tryReverseClassic(", 1)[1]
        reverse = reverse.split("private static double routeCost(", 1)[0]
        self.assertIn("opposite.runSync()", reverse)
        self.assertIn("reverseDeadline", reverse)
        self.assertIn("AStarStatic.isTransitionViable(this.world, from, to", reverse)
        self.assertIn("NavigationMetrics.reverseFound()", reverse)
        self.assertIn('"NAV-OPT-REVERSE from="', reverse)

    def test_snapshot_reuse_only_same_tick(self):
        self.assertIn("previous.tick()==tick", SNAPSHOT)
        self.assertIn("minX>=previous.minX()", SNAPSHOT)
        self.assertIn("maxZ<=previous.maxZ()", SNAPSHOT)
        self.assertIn("new WeakReference<>(result)", SNAPSHOT)

    def test_diagnostic_counter_names(self):
        self.assertIn('"NAV-OPT-STATS world="', PLANNER)
        for name in ("cache_reverse=", "reverse_tried=", "reverse_found=",
                     "snapshot_reused=", "jps", "astar"):
            self.assertIn(name, METRICS)

if __name__ == "__main__":
    unittest.main(verbosity=2)
