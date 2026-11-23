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
SECTION_POOL = read("pathing/SnapshotSectionPool.java")
CAPTURE_INDEX = read("pathing/SnapshotCaptureIndex.java")
EVENTS = read("forge/MillEventController.java")
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
        reserve = capture.index("synchronized(SNAPSHOT_LOCK)")
        self.assertLess(area, reserve)
        self.assertIn("NavigationMetrics.snapshotAreaRejected()", capture)
        self.assertIn("NavigationMetrics.snapshotBudgetDeferred()", capture)
        self.assertIn("NavigationMetrics.snapshotSectionRejected()", capture)
        self.assertIn("snapshot_area_rejected=", METRICS)
        self.assertIn("snapshot_tick_budget_deferred=", METRICS)
        self.assertIn("snapshot_section_rejected=", METRICS)

    def test_same_tick_section_pool_has_strict_work_and_memory_budgets(self):
        self.assertIn("class SnapshotSectionPool<T>", SECTION_POOL)
        self.assertIn("if (tick != nowTick)", SECTION_POOL)
        self.assertIn("sections.clear();", SECTION_POOL)
        self.assertIn("snapshots >= maxSnapshots", SECTION_POOL)
        self.assertIn("chunkVisits + (long) requestedChunkVisits > maxChunkVisits", SECTION_POOL)
        self.assertIn("copies >= maxCopies", SECTION_POOL)
        self.assertIn("entry.source() != currentSource", SECTION_POOL)
        self.assertIn("new SnapshotSectionPool<>(", SNAPSHOT)
        self.assertIn("MAX_SNAPSHOTS_PER_TICK = 4", SNAPSHOT)
        self.assertIn("MAX_CHUNK_VISITS_PER_TICK = 256", SNAPSHOT)
        self.assertIn("MAX_SECTION_COPIES_PER_TICK = 1024", SNAPSHOT)

    def test_shared_section_palette_copied_only_once_and_snapshot_stays_detached(self):
        capture = SNAPSHOT.split("public static PathWorldSnapshot capture(", 1)[1]
        self.assertLess(capture.index("pool.begin(tick,chunkVisits)"),
                        capture.index("getChunkNow(x,z)"))
        self.assertIn("PalettedContainer<BlockState> copy = pool.get(tick,key,source)", capture)
        self.assertIn("copy = source.copy();", capture)
        self.assertIn("pool.remember(tick,key,source,copy);", capture)
        self.assertIn("detached.put(key,copy)", capture)
        self.assertIn("NavigationMetrics.snapshotSectionsReused(reusedSections)", capture)
        self.assertIn("NavigationMetrics.snapshotAdditionalCaptured()", capture)
        ctor = SNAPSHOT.split("PathWorldSnapshot(Map<Long, PalettedContainer<BlockState>> detached,",1)[1]
        ctor = ctor.split("public static PathWorldSnapshot capture(",1)[0]
        self.assertIn("this.sections = Map.copyOf(detached)", ctor)
        self.assertNotIn("palette.copy()", ctor)
        self.assertIn("snapshot_shared_sections=", METRICS)
        self.assertIn("snapshot_additional_captures=", METRICS)

    def test_known_world_changes_invalidate_same_tick_sections_without_refunding_quota(self):
        self.assertIn("public synchronized boolean invalidateSection(long nowTick, long sectionKey)", SECTION_POOL)
        self.assertIn("public synchronized int invalidateAll(long nowTick)", SECTION_POOL)
        self.assertIn("sections.remove(sectionKey)", SECTION_POOL)
        self.assertIn("sections.clear();", SECTION_POOL)
        invalid = SECTION_POOL.split("public synchronized int invalidateAll(", 1)[1]
        invalid = invalid.split("public synchronized int usedCopies()", 1)[0]
        self.assertNotIn("copies = 0;", invalid)
        self.assertNotIn("snapshots = 0;", invalid)
        self.assertIn("public static void onKnownBlockMutation(ServerLevel level, BlockPos pos)", SNAPSHOT)
        self.assertIn("public static void onKnownWorldMutation(ServerLevel level)", SNAPSHOT)
        self.assertIn("pool.invalidateSection(tick, key)", SNAPSHOT)
        self.assertIn("pool.invalidateAll(tick)", SNAPSHOT)
        self.assertIn("index.invalidate(tick)", SNAPSHOT)

    def test_forge_mutation_hooks_are_registered_and_diagnostic(self):
        for kind in ("BlockEvent.EntityPlaceEvent", "BlockEvent.BreakEvent",
                     "BlockEvent.FluidPlaceBlockEvent", "BlockEvent.CropGrowEvent.Post",
                     "PistonEvent.Post", "ChunkEvent.Load", "ChunkEvent.Unload",
                     "WorldEvent.Unload"):
            self.assertIn(kind, EVENTS)
        self.assertIn("PathWorldSnapshot.onKnownBlockMutation", EVENTS)
        self.assertIn("PathWorldSnapshot.onKnownWorldMutation", EVENTS)
        self.assertIn("PathWorldSnapshot.onWorldUnload", EVENTS)
        self.assertIn("snapshot_known_sections_invalidated=", METRICS)
        self.assertIn("snapshot_known_captures_invalidated=", METRICS)
        self.assertIn("onWorldUnload(ServerLevel level)", SNAPSHOT)
        # A finite set of Forge hooks must never enable cross-tick reuse.
        self.assertIn("if (tick != nowTick)", SECTION_POOL)

    def test_prechange_events_poison_sections_before_first_capture(self):
        self.assertIn("private final Set<Long> poisonedSections", SECTION_POOL)
        self.assertIn("private boolean poisonedWorld;", SECTION_POOL)
        self.assertIn("poisonedSections.add(sectionKey)", SECTION_POOL)
        self.assertIn("poisonedWorld = true;", SECTION_POOL)
        self.assertIn("if (poisonedWorld || poisonedSections.contains(sectionKey)) return null;", SECTION_POOL)
        self.assertIn("if (!poisonedWorld && !poisonedSections.contains(sectionKey))", SECTION_POOL)
        self.assertIn("poisonedSections.clear();", SECTION_POOL)
        self.assertIn("poisonedWorld = false;", SECTION_POOL)
        self.assertIn("public synchronized boolean mayReuseSnapshots(long nowTick)", SECTION_POOL)
        # Even if no snapshot was previously created, pre-change hints must
        # initialize a poison ledger for this server world.
        mutation = SNAPSHOT.split("public static void onKnownBlockMutation(", 1)[1]
        mutation = mutation.split("public static void onKnownWorldMutation(", 1)[0]
        self.assertIn("SECTION_POOLS.computeIfAbsent(", mutation)
        capture = SNAPSHOT.split("public static PathWorldSnapshot capture(", 1)[1]
        capture = capture.split("public static void onKnownBlockMutation(", 1)[0]
        self.assertLess(capture.index("pool.mayReuseSnapshots(tick)"),
                        capture.index("pool.begin(tick,chunkVisits)"))
        self.assertIn("pool.isPoisoned(tick,key)", capture)
        self.assertIn("snapshot_poisoned_bypasses=", METRICS)

    def test_snapshot_publication_rejects_observed_mutation_races(self):
        self.assertIn("public synchronized long revision(long nowTick)", SECTION_POOL)
        self.assertIn("revision++;", SECTION_POOL)
        capture = SNAPSHOT.split("public static PathWorldSnapshot capture(", 1)[1]
        capture = capture.split("public static void onKnownBlockMutation(", 1)[0]
        self.assertIn("captureRevision = pool.revision(tick);", capture)
        self.assertIn("if(pool.revision(tick)!=captureRevision)", capture)
        self.assertIn("NavigationMetrics.snapshotMutationRaceRejected()", capture)
        self.assertIn("snapshot_mutation_race_rejected=", METRICS)
        self.assertLess(capture.index("if(pool.revision(tick)!=captureRevision)"),
                        capture.index("index.remember(tick, minX, maxX, minZ, maxZ, result)"))
        # A conservative epoch check must not create a new no-limits cache.
        self.assertIn("MAX_SECTION_COPIES_PER_TICK = 1024", SNAPSHOT)

    def test_bounded_snapshot_index_can_reuse_nonlatest_same_tick_supersets(self):
        self.assertIn("class SnapshotCaptureIndex<T>", CAPTURE_INDEX)
        self.assertIn("private final int capacity;", CAPTURE_INDEX)
        self.assertIn("if (entries.size() >= capacity) entries.remove(0)", CAPTURE_INDEX)
        self.assertIn("new WeakReference<>(snapshot)", CAPTURE_INDEX)
        self.assertIn("if (tick != nowTick)", CAPTURE_INDEX)
        self.assertIn("entries.clear();", CAPTURE_INDEX)
        self.assertIn("minX < entry.minX() || maxX > entry.maxX()", CAPTURE_INDEX)
        self.assertIn("minZ < entry.minZ() || maxZ > entry.maxZ()", CAPTURE_INDEX)
        self.assertIn("long bestArea = Long.MAX_VALUE;", CAPTURE_INDEX)
        self.assertIn("nonLatest = i < newest;", CAPTURE_INDEX)
        self.assertIn("public synchronized int invalidate(long nowTick)", CAPTURE_INDEX)

    def test_snapshot_index_honors_poison_and_capture_budgets(self):
        self.assertIn("new SnapshotCaptureIndex<>(MAX_SNAPSHOTS_PER_TICK)", SNAPSHOT)
        self.assertIn("CAPTURE_INDICES", SNAPSHOT)
        self.assertIn("if (pool.mayReuseSnapshots(tick))", SNAPSHOT)
        self.assertIn("index.find(tick, minX, maxX, minZ, maxZ)", SNAPSHOT)
        self.assertIn("NavigationMetrics.snapshotNonLatestReused()", SNAPSHOT)
        self.assertIn("index.remember(tick, minX, maxX, minZ, maxZ, result)", SNAPSHOT)
        self.assertIn("index.invalidate(tick)", SNAPSHOT)
        self.assertIn("pool.usedSnapshots() > 1", SNAPSHOT)
        self.assertIn("snapshot_non_latest_reused=", METRICS)
        self.assertIn("CAPTURE_INDICES.remove(level)", SNAPSHOT)
        capture = SNAPSHOT.split("public static PathWorldSnapshot capture(", 1)[1]
        capture = capture.split("public static void onKnownBlockMutation(", 1)[0]
        self.assertLess(capture.index("index.find(tick, minX, maxX, minZ, maxZ)"),
                        capture.index("pool.begin(tick,chunkVisits)"))
        self.assertLess(capture.index("if(pool.revision(tick)!=captureRevision)"),
                        capture.index("index.remember(tick, minX, maxX, minZ, maxZ, result)"))

    def test_snapshot_reuse_only_same_tick(self):
        self.assertIn("index.find(tick, minX, maxX, minZ, maxZ)", SNAPSHOT)
        self.assertIn("index.remember(tick, minX, maxX, minZ, maxZ, result)", SNAPSHOT)
        self.assertIn("if (tick != nowTick)", CAPTURE_INDEX)
        self.assertIn("entries.clear();", CAPTURE_INDEX)
        self.assertIn("new WeakReference<>(snapshot)", CAPTURE_INDEX)

    def test_diagnostic_counter_names(self):
        self.assertIn('"NAV-OPT-STATS world="', PLANNER)
        for name in ("cache_reverse=", "reverse_tried=", "reverse_found=",
                     "snapshot_reused=", "jps", "astar"):
            self.assertIn(name, METRICS)

if __name__ == "__main__":
    unittest.main(verbosity=2)
