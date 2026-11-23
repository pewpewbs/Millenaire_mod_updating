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
REQUEST = read("pathing/NavigationRequestKey.java")
WINDOW = read("pathing/NavigationRateWindow.java")
UTILITIES = read("utilities/ThreadSafeUtilities.java")
EXECUTOR = read("pathing/atomicstryker/PathSearchExecutor.java")

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

    def test_failed_search_cooldown_preserves_new_goals(self):
        self.assertIn("this.lastStart.equals(this.failedStart)", PLANNER)
        self.assertIn("this.lastEnd.equals(this.failedEnd)", PLANNER)
        self.assertIn("routeOptions(this.config) == this.failedOptions", PLANNER)
        self.assertIn("this.world.getGameTime() < this.failedRetryTick", PLANNER)
        self.assertIn("this.deferred = true;", PLANNER)
        self.assertIn("NavigationMetrics.noPathCooldown()", PLANNER)
        self.assertIn("this.failedRetryTick = this.world.getGameTime() + 20L", PLANNER)
        self.assertIn("this.failedStart = null;", PLANNER)

    def test_identical_cached_routes_are_throttled_without_blocking_new_goals(self):
        self.assertIn("this.lastStart.equals(this.recentCacheStart)", PLANNER)
        self.assertIn("this.lastEnd.equals(this.recentCacheEnd)", PLANNER)
        self.assertIn("routeOptions(this.config) == this.recentCacheOptions", PLANNER)
        self.assertIn("tick - this.recentCacheTick < 10L", PLANNER)
        self.assertIn("this.deferred = true;", PLANNER)
        self.assertIn("NavigationMetrics.cacheReplanDeferred()", PLANNER)
        self.assertIn("this.recentCacheTick = tick;", PLANNER)
        self.assertIn("cache_replan_deferred=", METRICS)

    def test_reverse_diagnostics_distinguish_no_route_from_reversed(self):
        self.assertIn('chosen == null ? "NONE"', WORKER)
        self.assertIn('"ENDPOINT_MISMATCH actualStart="', WORKER)
        self.assertIn('" actualGoal="', WORKER)

    def test_cache_invalidation_and_failure_cooldown_have_actionable_logs(self):
        cache = PLANNER.split("private ArrayList<AStarNode> reusePath(", 1)[1]
        cache = cache.split("private void rememberPath(", 1)[0]
        self.assertIn('"NAV-OPT-CACHE action=INVALIDATE reason="', cache)
        self.assertIn('rejection = "BLOCKED_NODE"', cache)
        self.assertIn('rejection = "BLOCKED_TRANSITION"', cache)
        self.assertIn('rejection = "NO_ORTHOGONAL_CORNER"', cache)
        self.assertIn('rejection = "CHUNK_UNAVAILABLE"', cache)
        self.assertIn("rejectedFrom = from;", cache)
        self.assertIn("if (!timedOut)", cache)
        self.assertIn('"NAV-OPT-COOLDOWN action=NO_PATH"', PLANNER)

    def test_inflight_identical_work_is_not_canceled(self):
        method = PLANNER.split("public synchronized void getPath(AStarNode start,AStarNode end,AStarConfig settings)", 1)[1]
        method = method.split("private void submit(boolean classic)", 1)[0]
        same = method.index("requested.equals(this.activeRequest)")
        cancel = method.index("stopPathSearch(true);")
        self.assertLess(same, cancel)
        self.assertIn("this.busy && requested.equals(this.activeRequest)", method)
        self.assertIn("NavigationMetrics.inflightJoined()", method)
        self.assertIn("this.joinedSearch = true", method)
        self.assertIn("this.activeRequest = requested", method)
        self.assertIn("this.activeRequest=null", PLANNER)
        self.assertIn("this.activeRequest = null;", PLANNER)
        self.assertIn("inflight_joined=", METRICS)

    def test_villager_does_not_preempt_coalescing_but_cancels_no_goal(self):
        start = VILLAGER.index("private List<Node> computeNewPath(Point dest)")
        body = VILLAGER[start:VILLAGER.index("public int countInv(Block block", start)]
        self.assertIn('this.pathPlannerJPS.hadJoinedSearch()', body)
        self.assertIn('"ASTAR-JOIN"', body)
        # No unconditional cancellation before the planner compares requests.
        prelude = body.split("block9: {",1)[0]
        self.assertNotIn("stopPathSearch(true)", prelude)
        # A destination already reached, goal action range, or no-access
        # still cancels a pending, now-obsolete worker.
        self.assertGreaterEqual(body.count("stopPathSearch(true)"), 4)

    def test_background_queue_metrics_and_bounded_fifo_unchanged(self):
        self.assertIn("new ArrayBlockingQueue<>(64)", EXECUTOR)
        self.assertIn("new ThreadPoolExecutor(", EXECUTOR)
        self.assertIn("2, 2, 30, TimeUnit.SECONDS", EXECUTOR)
        self.assertIn("queueWaitNanos", EXECUTOR)
        self.assertIn("maxQueueWaitNanos", EXECUTOR)
        self.assertIn("POOL.remove(future)", EXECUTOR)
        self.assertIn("queue_wait_avg_ms=", METRICS)
        self.assertIn("queue_wait_max_ms=", METRICS)
        self.assertIn("record NavigationRequestKey(", REQUEST)
        self.assertIn("record Movement(", REQUEST)

    def test_hot_snapshot_reads_are_allocation_free(self):
        self.assertIn("public BlockState getBlockState(int x, int y, int z)", SNAPSHOT)
        self.assertIn("return getBlockState(pos.getX(), pos.getY(), pos.getZ());", SNAPSHOT)
        self.assertIn("!chunks.contains(ChunkPos.asLong(x >> 4, z >> 4))", SNAPSHOT)
        self.assertIn("palette.get(x & 15, y & 15, z & 15)", SNAPSHOT)
        self.assertIn("world instanceof org.millenaire.common.pathing.PathWorldSnapshot snapshot", UTILITIES)
        self.assertIn("return snapshot.getBlockState(x, y, z)", UTILITIES)
        # Non-snapshot live worlds still validate unloaded chunk access.
        self.assertIn("ThreadSafeUtilities.validateCoords(world, x, z);", UTILITIES)

    def test_ladder_scan_reuses_mutable_position_and_preserves_geometry(self):
        part = PLANNER.split("private boolean nearbyLadderOnVerticalRoute()", 1)[1]
        part = part.split("protected net.minecraft.world.level.BlockGetter captureWorld(", 1)[0]
        self.assertIn("int[] sampledHeights = new int[ySamples];", part)
        self.assertIn("net.minecraft.core.BlockPos.MutableBlockPos probe =", part)
        self.assertIn("probe.set(x, y, z);", part)
        self.assertIn("for (int y : sampledHeights)", part)
        self.assertIn("NavigationMetrics.ladderScan(", part)
        self.assertNotIn("new net.minecraft.core.BlockPos(x, y, z)", part)

    def test_window_metrics_use_monotonic_elapsed_time_and_reset(self):
        self.assertIn("public synchronized String sample(long nowNanos, Counts totals)", WINDOW)
        self.assertIn("nowNanos - lastNanos", WINDOW)
        self.assertIn("window_scope=process", WINDOW)
        self.assertIn("cache_hit_pct=", WINDOW)
        self.assertIn("queue_wait_window_avg_ms=", WINDOW)
        self.assertIn("private static final NavigationRateWindow RATE_WINDOW", METRICS)
        self.assertIn("RATE_WINDOW.sample(System.nanoTime()", METRICS)
        self.assertIn("RATE_WINDOW.reset()", METRICS)
        for name in ("ladder_scans=", "ladder_probes=", "ladder_hits=",
                     "ladder_scan_avg_ms=", "ladderScan(int probes"):
            self.assertIn(name, METRICS)

    def test_oversized_snapshot_does_not_consume_per_tick_capture_budget(self):
        capture = SNAPSHOT.split("public static PathWorldSnapshot capture(", 1)[1]
        area = capture.index("if ((long) (maxX - minX + 1)")
        reserve = capture.index("synchronized(LAST_CAPTURE)")
        self.assertLess(area, reserve)
        self.assertIn("NavigationMetrics.snapshotAreaRejected()", capture)
        self.assertIn("NavigationMetrics.snapshotBudgetDeferred()", capture)
        self.assertIn("NavigationMetrics.snapshotSectionRejected()", capture)
        self.assertIn("snapshot_area_rejected=", METRICS)
        self.assertIn("snapshot_tick_budget_deferred=", METRICS)
        self.assertIn("snapshot_section_rejected=", METRICS)

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
