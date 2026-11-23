/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.World
 */
package org.millenaire.common.pathing.atomicstryker;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.world.level.Level;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.pathing.atomicstryker.AStarConfig;
import org.millenaire.common.pathing.atomicstryker.AStarNode;
import org.millenaire.common.pathing.atomicstryker.AStarStatic;
import org.millenaire.common.pathing.atomicstryker.AStarWorker;
import org.millenaire.common.pathing.atomicstryker.AStarWorkerJPS;
import org.millenaire.common.pathing.atomicstryker.IAStarPathedEntity;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.ThreadSafeUtilities;

public class AStarPathPlannerJPS {
    private final Level world;
    private final IAStarPathedEntity pathedEntity;
    private final boolean isJPS;
    private AStarWorker worker;
    private java.util.concurrent.FutureTask<Void> future;
    private long generation;
    private boolean busy;
    private AStarNode lastStart,lastEnd;
    private org.millenaire.common.pathing.NavigationRequestKey activeRequest;
    private boolean joinedSearch;
    public AStarConfig config;
    private net.minecraft.world.level.BlockGetter snapshot;
    private long nextSnapshotAttempt;
    // NAV-OPT-16: exact request identity, capped 20/40/80 tick backoff and
    // immediate wake-up on known world edits. Different goals retry normally.
    private final org.millenaire.common.pathing.FailedRouteBackoff failedRouteBackoff =
            new org.millenaire.common.pathing.FailedRouteBackoff(
                    org.millenaire.common.pathing.NavigationMetrics::noPathBackoffEvent);
    // JPS never expands vertical ladder edges. Keep its candidate while a
    // bounded classic A* attempt checks for a shorter ladder alternative.
    private ArrayList<AStarNode> jpsCandidate;
    private boolean immediatePath;
    private boolean deferred;
    // A physically blocked villager may request the SAME cached route every
    // tick. Rate-limit only this exact start/goal/config after a successful
    // cache delivery; a moved villager or changed task is never delayed.
    private AStarNode recentCacheStart, recentCacheEnd;
    private int recentCacheOptions;
    private long recentCacheTick = -1L;
    private static final java.util.Map<Level, org.millenaire.common.pathing.RouteMemory> ROUTES =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private static final java.util.Map<Level, Long> REPORT_TICKS =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private static org.millenaire.common.pathing.RouteMemory routes(Level world) {
        synchronized (ROUTES) {
            return ROUTES.computeIfAbsent(world,
                    ignored -> new org.millenaire.common.pathing.RouteMemory(96, 96, 200L));
        }
    }

    private static org.millenaire.common.pathing.NavigationRequestKey requestKey(
            AStarNode start, AStarNode goal, AStarConfig cfg) {
        return new org.millenaire.common.pathing.NavigationRequestKey(
                new org.millenaire.common.pathing.NavigationRequestKey.Endpoint(start.x, start.y, start.z),
                new org.millenaire.common.pathing.NavigationRequestKey.Endpoint(goal.x, goal.y, goal.z),
                new org.millenaire.common.pathing.NavigationRequestKey.Movement(
                        cfg.canUseDoors, cfg.canTakeDiagonals, cfg.allowDropping,
                        cfg.canSwim, cfg.canClearLeaves, cfg.tolerance,
                        cfg.toleranceHorizontal, cfg.toleranceVertical));
    }

    private static int routeOptions(AStarConfig cfg) {
        return java.util.Objects.hash(cfg.canUseDoors, cfg.canTakeDiagonals,
                cfg.allowDropping, cfg.canSwim, cfg.canClearLeaves,
                cfg.tolerance, cfg.toleranceHorizontal, cfg.toleranceVertical);
    }

    private static org.millenaire.common.pathing.RouteMemory.Cell cell(AStarNode node) {
        return new org.millenaire.common.pathing.RouteMemory.Cell(node.x, node.y, node.z);
    }

    private long knownWorldMutationEpoch() {
        return this.world instanceof net.minecraft.server.level.ServerLevel server
                ? org.millenaire.common.pathing.PathWorldSnapshot.knownMutationEpoch(server)
                : 0L;
    }

    private void reportPeriodically() {
        if (this.world == null || MillConfigValues.LogPathing < 1) return;
        long tick = this.world.getGameTime();
        synchronized (REPORT_TICKS) {
            long previous = REPORT_TICKS.getOrDefault(this.world, tick - 200L);
            if (tick < previous || tick - previous >= 200L) {
                REPORT_TICKS.put(this.world, tick);
                MillLog.major(null, "NAV-OPT-STATS world=" + this.world.dimension().location()
                        + " " + org.millenaire.common.pathing.NavigationMetrics.report());
            }
        }
    }

    /**
     * Verify cached coordinates against the LIVE world. In particular,
     * reversibility is not assumed for drops or uneven ladder entrances.
     * This runs on the server thread, so only short paths may be cached.
     */
    private ArrayList<AStarNode> reusePath(AStarNode start, AStarNode end) {
        if (this.world == null || this.world.isClientSide) return null;
        var memory = routes(this.world);
        var alternatives = memory.candidates(this.world.getGameTime(),
                routeOptions(this.config), cell(start), cell(end));
        ArrayList<AStarNode> best = null;
        boolean bestReversed = false;
        double bestCost = Double.POSITIVE_INFINITY;
        // Cache hits are validated on the main thread. Hard-limit that extra
        // work; a cache miss may be cheaper than a huge shape revalidation.
        final long validationDeadline = System.nanoTime()
                + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(2L);
        for (var alternative : alternatives) {
            var coordinates = alternative.goalToStart();
            if (coordinates.size() < 2 || coordinates.size() > 96) continue;
            ArrayList<AStarNode> path = new ArrayList<>(coordinates.size());
            for (var position : coordinates) {
                path.add(new AStarNode(position.x(), position.y(), position.z()));
            }
            boolean valid = path.get(0).equals(end)
                    && path.get(path.size() - 1).equals(start);
            boolean timedOut = false;
            String rejection = valid ? null : "ENDPOINT_MISMATCH";
            AStarNode rejectedFrom = null, rejectedTo = null;
            if (valid) {
                try {
                    for (int i = path.size() - 1; i > 0; i--) {
                        if (System.nanoTime() > validationDeadline) {
                            timedOut = true;
                            valid = false;
                            break;
                        }
                        AStarNode from = path.get(i), to = path.get(i - 1);
                        // Keep the validation ordered and single-pass so
                        // diagnostics never repeat expensive collision queries.
                        if (Math.abs(to.y - from.y) > 1
                                || Math.abs(to.x - from.x) > 1
                                || Math.abs(to.z - from.z) > 1) {
                            rejection = "NON_ADJACENT_EDGE";
                        } else if (!AStarStatic.isViable(this.world, to, 0, this.config)) {
                            rejection = "BLOCKED_NODE";
                        } else if (!AStarStatic.isTransitionViable(this.world, from, to, this.config)) {
                            rejection = "BLOCKED_TRANSITION";
                        } else if (!this.config.canTakeDiagonals && from.y == to.y
                                && from.x != to.x && from.z != to.z
                                && AStarStatic.findSafeOrthogonalCorner(
                                        this.world, from, to, this.config) == null) {
                            rejection = "NO_ORTHOGONAL_CORNER";
                        }
                        if (rejection != null) {
                            valid = false;
                            rejectedFrom = from;
                            rejectedTo = to;
                            break;
                        }
                    }
                } catch (ThreadSafeUtilities.ChunkAccessException problem) {
                    rejection = "CHUNK_UNAVAILABLE";
                    valid = false;
                }
            }
            if (!valid) {
                // A time-budget rejection does not mean the stored path is
                // stale. Keep it for future requests with more free CPU.
                if (!timedOut) {
                    memory.forget(this.world.getGameTime(), routeOptions(this.config),
                            cell(start), cell(end), alternative.reversed());
                    org.millenaire.common.pathing.NavigationMetrics.cacheStale();
                    if (MillConfigValues.LogPathing >= 2) {
                        MillLog.major(null, "NAV-OPT-CACHE action=INVALIDATE reason=" + rejection
                                + " reverse=" + alternative.reversed()
                                + " from=" + start + " to=" + end
                                + " rejectedEdge=" + (rejectedFrom == null ? "-" : rejectedFrom
                                        + "->" + rejectedTo));
                    }
                }
                continue;
            }
            double cost = pathCost(path);
            if (cost < bestCost) {
                best = path;
                bestCost = cost;
                bestReversed = alternative.reversed();
            }
        }
        if (best == null) {
            org.millenaire.common.pathing.NavigationMetrics.cacheMiss();
            return null;
        }
        org.millenaire.common.pathing.NavigationMetrics.cacheHit(bestReversed);
        if (MillConfigValues.LogPathing >= 2) {
            MillLog.major(null, "NAV-OPT-CACHE action=HIT reverse=" + bestReversed
                    + " from=" + start + " to=" + end + " nodes=" + best.size()
                    + " cost=" + String.format(java.util.Locale.ROOT, "%.2f", bestCost));
        }
        return best;
    }

    private void rememberPath(ArrayList<AStarNode> path) {
        if (this.world == null || path == null || path.size() < 2
                || path.size() > 96 || this.lastStart == null || this.lastEnd == null
                || !path.get(path.size() - 1).equals(this.lastStart)
                || !path.get(0).equals(this.lastEnd)) return;
        ArrayList<org.millenaire.common.pathing.RouteMemory.Cell> cells =
                new ArrayList<>(path.size());
        for (AStarNode node : path) cells.add(cell(node));
        routes(this.world).remember(this.world.getGameTime(), routeOptions(this.config), cells);
        org.millenaire.common.pathing.NavigationMetrics.routeStored();
    }


    private static double pathCost(ArrayList<AStarNode> nodes) {
        if (nodes == null || nodes.size() < 2) return Double.POSITIVE_INFINITY;
        double cost = 0.0D;
        for (int i = 1; i < nodes.size(); i++) {
            AStarNode a = nodes.get(i - 1), b = nodes.get(i);
            int dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
            cost += Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dy != 0 && dx == 0 && dz == 0) cost += 0.8D;
        }
        return cost;
    }

    /**
     * Detect ladders near a plausible vertical route, not only around start
     * and end. The earlier endpoint-only test missed quarry shafts midway
     * between a villager and a job target.
     *
     * The number of samples is strictly bounded; we read only detached
     * snapshot palettes and never ask the live Level to load a chunk.
     */
    private boolean nearbyLadderOnVerticalRoute() {
        if (this.snapshot == null || this.lastStart == null || this.lastEnd == null) return false;
        int deltaY = Math.abs(this.lastStart.y - this.lastEnd.y);
        if (deltaY < 3 || deltaY > 48) return false;
        int dx = this.lastEnd.x - this.lastStart.x;
        int dz = this.lastEnd.z - this.lastStart.z;
        if (dx * dx + dz * dz > 96 * 96) return false;

        int floor = Math.max(this.snapshot.getMinBuildHeight(),
                Math.min(this.lastStart.y, this.lastEnd.y) - 2);
        int ceiling = Math.min(this.snapshot.getMaxBuildHeight() - 1,
                Math.max(this.lastStart.y, this.lastEnd.y) + 2);
        if (ceiling <= floor) return false;

        // A sample every ~4 horizontal blocks, always including endpoints.
        // Within each sample only a 9x9 column neighborhood is considered.
        int steps = Math.max(1, Math.min(24,
                (int) Math.ceil(Math.sqrt((double)dx * dx + (double)dz * dz) / 4.0D)));
        int ySamples = Math.min(12, ceiling - floor + 1);
        // Same sample coordinates and scan order as before NAV-OPT-10.
        // Precompute heights once and reuse one mutable position to avoid
        // allocating up to ~24,300 short-lived BlockPos objects per scan.
        int[] sampledHeights = new int[ySamples];
        for (int k = 0; k < ySamples; k++) {
            sampledHeights[k] = floor + (int) Math.round((double) (ceiling - floor) * k
                    / Math.max(1, ySamples - 1));
        }
        net.minecraft.core.BlockPos.MutableBlockPos probe = new net.minecraft.core.BlockPos.MutableBlockPos();
        long scanStarted = System.nanoTime();
        int probes = 0;
        for (int step = 0; step <= steps; step++) {
            int midX = this.lastStart.x + (int) Math.round((double) dx * step / steps);
            int midZ = this.lastStart.z + (int) Math.round((double) dz * step / steps);
            for (int x = midX - 4; x <= midX + 4; x++) {
                for (int z = midZ - 4; z <= midZ + 4; z++) {
                    for (int y : sampledHeights) {
                        probe.set(x, y, z);
                        probes++;
                        if (this.snapshot.getBlockState(probe)
                                .is(net.minecraft.world.level.block.Blocks.LADDER)) {
                            org.millenaire.common.pathing.NavigationMetrics.ladderScan(
                                    probes, true, System.nanoTime() - scanStarted);
                            return true;
                        }
                    }
                }
            }
        }
        org.millenaire.common.pathing.NavigationMetrics.ladderScan(
                probes, false, System.nanoTime() - scanStarted);
        return false;
    }

    protected net.minecraft.world.level.BlockGetter captureWorld(AStarNode start, AStarNode end) {
        if (!(this.world instanceof net.minecraft.server.level.ServerLevel server)) return null;
        return org.millenaire.common.pathing.PathWorldSnapshot.capture(server, start, end);
    }

    public AStarPathPlannerJPS(Level world, IAStarPathedEntity entity, boolean jps) {
        this.world=world;this.pathedEntity=entity;this.isJPS=jps;
    }
    protected AStarWorker createWorker(boolean classic) {
        return this.isJPS&&!classic?new AStarWorkerJPS(this):new AStarWorker(this);
    }
    protected void dispatch(Runnable action) {
        if(this.world.getServer()!=null)this.world.getServer().execute(action);
    }
    private static AStarNode copy(AStarNode node) { return new AStarNode(node.x,node.y,node.z); }
    private static AStarConfig copy(AStarConfig source) {
        AStarConfig result=new AStarConfig(source.canUseDoors,source.canTakeDiagonals,source.allowDropping,source.canSwim,source.canClearLeaves);
        result.tolerance=source.tolerance;result.toleranceHorizontal=source.toleranceHorizontal;result.toleranceVertical=source.toleranceVertical;return result;
    }
    public synchronized void getPath(AStarNode start,AStarNode end,AStarConfig settings) {
        var requested = requestKey(start, end, settings);
        this.failedRouteBackoff.onRequest(requested);
        if (this.busy && requested.equals(this.activeRequest)) {
            // The same NPC asked for an already running or queued A*/JPS
            // search. Do NOT cancel/restart that expensive computation.
            this.joinedSearch = true;
            org.millenaire.common.pathing.NavigationMetrics.inflightJoined();
            return;
        }
        // A new destination, starting block, or movement policy really does
        // supersede the old search. Generation gating drops old callbacks.
        stopPathSearch(true);
        this.activeRequest = requested;
        this.immediatePath = false;
        this.deferred = false;
        this.joinedSearch = false;
        this.jpsCandidate = null;
        this.lastStart = copy(start);
        this.lastEnd = copy(end);
        this.config = copy(settings);
        reportPeriodically();
        long tick = this.world == null ? 0L : this.world.getGameTime();
        if (this.world != null && this.recentCacheStart != null
                && this.lastStart.equals(this.recentCacheStart)
                && this.lastEnd.equals(this.recentCacheEnd)
                && routeOptions(this.config) == this.recentCacheOptions
                && tick >= this.recentCacheTick && tick - this.recentCacheTick < 10L) {
            // Keep the current movement controller active. Do not clear an
            // existing route or redo an expensive live-world shape sweep.
            this.deferred = true;
            org.millenaire.common.pathing.NavigationMetrics.cacheReplanDeferred();
            return;
        }
        ArrayList<AStarNode> cached = reusePath(this.lastStart, this.lastEnd);
        if (cached != null) {
            this.recentCacheStart = copy(this.lastStart);
            this.recentCacheEnd = copy(this.lastEnd);
            this.recentCacheOptions = routeOptions(this.config);
            this.recentCacheTick = tick;
            this.immediatePath = true;
            if (this.pathedEntity != null) this.pathedEntity.onFoundPath(cached);
            return;
        }
        if (this.world != null && this.failedRouteBackoff.shouldDefer(
                requested, tick, knownWorldMutationEpoch())) {
            // Suppressing a repeated failure is NOT a definitive NO_PATH.
            // A different start / target / movement policy, a known world edit
            // or the expiry of this bounded backoff starts a fresh search.
            this.deferred = true;
            org.millenaire.common.pathing.NavigationMetrics.noPathCooldown();
            return;
        }
        if (this.world != null && this.world.getGameTime() < this.nextSnapshotAttempt) {
            this.deferred = true;
            return;
        }
        this.snapshot = captureWorld(start,end);
        if (this.snapshot == null && this.world != null) {
            this.deferred = true;
            this.nextSnapshotAttempt = this.world.getGameTime() + 5
                    + Math.floorMod(System.identityHashCode(this), 7);
            return;
        }
        submit(false);
    }
    private void submit(boolean classic) {
        AStarWorker next=createWorker(classic);this.worker=next;this.busy=true;
        next.setup(this.snapshot,copy(this.lastStart),copy(this.lastEnd),copy(this.config));
        next.requestGeneration=this.generation;next.classicAttempt=classic;
        this.future=PathSearchExecutor.submit(next);
        if (this.future == null) {
            this.busy = false;
            this.activeRequest = null;
            this.worker = null;
            this.snapshot = null;
            this.deferred = true;
            // If the optional ladder-capable comparison could not be queued,
            // retain JPS's already successful route. Never discard a real
            // path simply because the executor is temporarily saturated.
            if (classic && this.jpsCandidate != null) {
                ArrayList<AStarNode> available = this.jpsCandidate;
                this.jpsCandidate = null;
                if (this.pathedEntity != null) this.pathedEntity.onFoundPath(available);
            }
            // For an initial rejection no task exists to call completed().
            // The villager observes !isBusy() and schedules Vanilla fallback.
        }
    }
    public void getPath(int x,int y,int z,int dx,int dy,int dz,AStarConfig settings) throws ThreadSafeUtilities.ChunkAccessException {
        if(!AStarStatic.isViable(this.world,x,y,z,0,settings))--y;
        if(!AStarStatic.isViable(this.world,x,y,z,0,settings))y+=2;
        if(!AStarStatic.isViable(this.world,x,y,z,0,settings))--y;
        getPath(new AStarNode(x,y,z),new AStarNode(dx,dy,dz),settings);
    }
    public synchronized boolean isBusy() { return this.busy; }
    public synchronized boolean hadImmediatePath() { return this.immediatePath; }
    public synchronized boolean hadJoinedSearch() { return this.joinedSearch; }
    public synchronized boolean wasDeferred() { return this.deferred; }
    public void completed(AStarWorker source,ArrayList<AStarNode> result) {
        dispatch(()->deliver(source,result));
    }
    private synchronized void deliver(AStarWorker source,ArrayList<AStarNode> result) {
        if(source!=this.worker||source.requestGeneration!=this.generation)return;
        this.busy=false;this.future=null;this.worker=null;
        if (this.isJPS && !source.classicAttempt) {
            if (result == null) { submit(true); return; }
            if (nearbyLadderOnVerticalRoute()) {
                // Compare the JPS solution to a ladder-capable classical route.
                this.jpsCandidate = result;
                submit(true);
                return;
            }
        }
        if (source.classicAttempt && this.jpsCandidate != null) {
            if (result == null || pathCost(this.jpsCandidate) <= pathCost(result)) {
                result = this.jpsCandidate;
            }
        }
        this.jpsCandidate=null;
        this.snapshot=null;
        this.activeRequest = null;
        this.joinedSearch = false;
        if (result != null) {
            this.failedRouteBackoff.onSuccess();
            rememberPath(result);
        } else if (this.world != null && this.lastStart != null && this.lastEnd != null) {
            var failed = requestKey(this.lastStart, this.lastEnd, this.config);
            int level = this.failedRouteBackoff.onFailed(failed,
                    this.world.getGameTime(), knownWorldMutationEpoch(),
                    Math.floorMod(System.identityHashCode(this), 7));
            org.millenaire.common.pathing.NavigationMetrics.noPathFailure(level);
            if (MillConfigValues.LogPathing >= 2) {
                // Report once per actual failed search, not on suppressed
                // attempts. The increasing level pinpoints persistent stalls.
                MillLog.major(null, "NAV-OPT-COOLDOWN action=NO_PATH"
                        + " from=" + this.lastStart + " to=" + this.lastEnd
                        + " retryTick=" + this.failedRouteBackoff.retryTick()
                        + " level=" + level);
            }
        }
        if(this.pathedEntity!=null) {
            if(result==null)this.pathedEntity.onNoPathAvailable();
            else this.pathedEntity.onFoundPath(result);
        }
    }
    public synchronized void stopPathSearch(boolean interrupted) {
        ++this.generation;PathSearchExecutor.cancel(this.future);this.future=null;this.worker=null;this.busy=false;this.snapshot=null;this.jpsCandidate=null;this.activeRequest=null;this.joinedSearch=false;
        if(!interrupted&&this.pathedEntity!=null)this.pathedEntity.onNoPathAvailable();
    }
}
