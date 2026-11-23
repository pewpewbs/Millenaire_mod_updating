"""Source-level regression checks for Millenaire A*/JPS-first routing.

Run from millenaire-1.18.2-forge:
    py -3 tools/tests/test_navigation_priority.py

No Minecraft, Forge or third-party Python packages required. These tests
check wiring/ordering only: they do not replace compileJava or playtesting.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/org/millenaire/common"
VILLAGER = (JAVA / "entity/MillVillager.java").read_text(encoding="utf-8")
PLANNER = (JAVA / "pathing/atomicstryker/AStarPathPlannerJPS.java").read_text(encoding="utf-8")
NAVIGATION = (JAVA / "entity/PathNavigateSimple.java").read_text(encoding="utf-8")


def section(source: str, beginning: str, after: str) -> str:
    start = source.index(beginning)
    end = source.index(after, start + len(beginning))
    return source[start:end]


class PrimaryRouteWiring(unittest.TestCase):
    def test_primary_path_never_begins_with_vanilla(self):
        primary = section(VILLAGER, "private List<Node> computeNewPath(Point dest)",
                          "public int countInv(Block block")
        self.assertIn("this.pathPlannerJPS.getPath(", primary)
        self.assertNotIn("this.tryGroundRoute(dest)", primary)

    def test_deferred_primary_schedules_fallback(self):
        primary = section(VILLAGER, "private List<Node> computeNewPath(Point dest)",
                          "public int countInv(Block block")
        self.assertIn("if (this.pathPlannerJPS.isBusy())", primary)
        self.assertIn("this.logRouteDecision(\"ASTAR-DEFERRED\", dest)", primary)
        self.assertIn("this.onNoPathAvailable();", primary)
        self.assertIn("catch (ThreadSafeUtilities.ChunkAccessException e)", primary)

    def test_primary_failure_uses_vanilla_and_retains_working_route(self):
        failed = section(VILLAGER, "private void pathFailedSinceLastTick()",
                         "public boolean performNightAction()")
        self.assertIn("if (this.tryGroundRoute(dest)) return;", failed)
        self.assertIn("this.pathEntity != null && !this.pathEntity.isDone()", failed)

    def test_physical_stall_can_try_vanilla(self):
        watchdog = section(VILLAGER, "private void recoverStationaryMovement()",
                           "private static boolean isStockTransferGoal")
        self.assertIn("Action.DETOUR)", watchdog)
        self.assertIn("this.tryGroundRoute(dest)", watchdog)
        self.assertIn("groundNavigation.requestRecoveryDetour()", watchdog)

    def test_stale_snapshot_path_schedules_fallback(self):
        result = section(VILLAGER, "private void applyPathCalculatedSinceLastTick()",
                         "public boolean attackEntity(Entity entity)")
        self.assertIn("NavigationMetrics.stale()", result)
        self.assertIn("this.pathFailedSincelastTick = true;", result)

    def test_ladder_comparison_scans_corridor_not_only_endpoints(self):
        method = section(PLANNER, "private boolean nearbyLadderOnVerticalRoute()",
                         "protected net.minecraft.world.level.BlockGetter captureWorld(")
        self.assertIn("step <= steps", method)
        self.assertIn("this.lastStart.x", method)
        self.assertIn("this.lastEnd.x", method)
        self.assertIn("this.snapshot.getBlockState(", method)

    def test_queue_rejection_does_not_discard_successful_jps_route(self):
        submit = section(PLANNER, "private void submit(boolean classic)",
                         "public void getPath(int x,int y,int z")
        self.assertIn("if (classic && this.jpsCandidate != null)", submit)
        self.assertIn("this.pathedEntity.onFoundPath(available)", submit)

    def test_executor_still_uses_minecraft_movement_controller(self):
        self.assertIn("extends GroundPathNavigation", NAVIGATION)
        self.assertIn("LadderTransitionRules.routeDirection", NAVIGATION)
        self.assertIn("LadderTransitionRules.reachedRung", NAVIGATION)
        self.assertIn("LadderTransitionRules.shouldClimb", NAVIGATION)
        self.assertIn("LadderTransitionRules.needsTopOut", NAVIGATION)
        self.assertIn("assistLeavingLadder", NAVIGATION)


if __name__ == "__main__":
    unittest.main(verbosity=2)
