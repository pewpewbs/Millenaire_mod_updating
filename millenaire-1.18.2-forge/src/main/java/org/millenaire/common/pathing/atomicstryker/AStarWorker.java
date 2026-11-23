/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.World
 */
package org.millenaire.common.pathing.atomicstryker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.Level;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.pathing.atomicstryker.AStarConfig;
import org.millenaire.common.pathing.atomicstryker.AStarNode;
import org.millenaire.common.pathing.atomicstryker.AStarPathPlannerJPS;
import org.millenaire.common.pathing.atomicstryker.AStarStatic;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.ThreadSafeUtilities;

public class AStarWorker
implements Runnable {
    private final long SEARCH_TIME_LIMIT = 150L;
    public AStarPathPlannerJPS boss;
    AStarConfig config;
    public volatile boolean isRunning = false;
    public final ArrayList<AStarNode> closedNodes;
    private AStarNode startNode;
    protected AStarNode targetNode;
    protected net.minecraft.world.level.BlockGetter world;
    private long timeLimit;
    long requestGeneration;
    boolean classicAttempt;
    private final AStarOpenSet queue;
    private final Map<AStarOpenSet.Position, AStarNode> closedIndex = new HashMap<>();
    private volatile boolean isBusy = false;

    public AStarWorker() {
        this.boss = null;
        this.closedNodes = new ArrayList();
        this.queue = new AStarOpenSet();
    }

    public AStarWorker(AStarPathPlannerJPS creator) {
        this.boss = creator;
        this.closedNodes = new ArrayList();
        this.queue = new AStarOpenSet();
    }

    private void addToBinaryHeap(AStarNode input) {
        this.queue.offer(input);
    }

    /**
     * Classic A* vertical edges for a real contiguous ladder. JPS has no
     * vertical successor expansion, so its fallback runs this worker.
     * Every rung is validated against the detached pathfinding snapshot.
     */
    private void checkPossibleLadder(AStarNode node) throws ThreadSafeUtilities.ChunkAccessException {
        final int x = node.x, y = node.y, z = node.z;
        // Only vanilla ladder blocks are supported by the matching movement
        // controller. CLIMBABLE also contains vines, which this controller
        // must not accidentally treat as a vertical ladder route.
        if (ThreadSafeUtilities.getBlock(this.world, x, y, z)
                != net.minecraft.world.level.block.Blocks.LADDER) return;
        for (int direction : new int[] {1, -1}) {
            final int nextY = y + direction;
            if (nextY <= this.world.getMinBuildHeight()) continue;
            if (ThreadSafeUtilities.getBlock(this.world, x, nextY, z)
                    != net.minecraft.world.level.block.Blocks.LADDER) continue;
            // Both feet and head must be clear. No climbing through solid ceilings.
            boolean feetClear = AStarStatic.isPassableBlock(this.world, x, nextY, z, this.config);
            boolean headClear = AStarStatic.isPassableBlock(this.world, x, nextY + 1, z, this.config);
            if (!org.millenaire.common.pathing.LadderTransitionRules.canTraverse(
                    true, true, feetClear, headClear)) continue;
            AStarNode candidate = new AStarNode(x, nextY, z, node.getG() + 2, node, this.targetNode);
            AStarOpenSet.Position key = AStarOpenSet.Position.of(candidate);
            AStarNode closed = this.closedIndex.get(key);
            if (closed != null) {
                if (closed.updateDistance(candidate.getG(), node)) {
                    this.closedIndex.remove(key);
                    this.closedNodes.remove(closed);
                    this.queue.offer(closed);
                }
            } else if (!this.tryToUpdateExistingHeapNode(node, candidate)) {
                this.addToBinaryHeap(candidate);
            }
        }
    }

    private int getCostNodeToNode(AStarNode a, AStarNode b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y) + Math.abs(a.z - b.z);
    }

    public void getNextCandidates(AStarNode parent, boolean droppingAllowed) throws ThreadSafeUtilities.ChunkAccessException {
        int x = parent.x;
        int y = parent.y;
        int z = parent.z;
        int[][] c = droppingAllowed ? AStarStatic.candidates_allowdrops : AStarStatic.candidates;
        for (int i = 0; i < c.length; ++i) {
            AStarNode check = new AStarNode(x + c[i][0], y + c[i][1], z + c[i][2], parent.getG() + c[i][3], parent, this.targetNode);
            try {
                // Clearance must be checked BEFORE improving either an open
                // or a closed node. A blocked transition is not a cheaper path.
                if (!AStarStatic.isViable(this.world, check, c[i][1], this.config)
                        || !AStarStatic.isTransitionViable(this.world, parent, check, this.config)) continue;
                AStarNode closed = this.closedIndex.get(AStarOpenSet.Position.of(check));
                if (closed != null) {
                    // A better route to a previously expanded coordinate must
                    // be reopened; changing a closed node's parent alone never
                    // lets the priority queue explore its improved successors.
                    if (closed.updateDistance(check.getG(), parent)) {
                        this.closedIndex.remove(AStarOpenSet.Position.of(closed));
                        this.closedNodes.remove(closed);
                        this.queue.offer(closed);
                    }
                    continue;
                }
                if (this.tryToUpdateExistingHeapNode(parent, check)) continue;
                this.addToBinaryHeap(check);
                continue;
            }
            catch (Exception e) {
                if (MillConfigValues.LogChunkLoader < 2) continue;
                MillLog.minor(this, e.getLocalizedMessage());
            }
        }
        // The old implementation never invoked checkPossibleLadder().
        // This enables actual up/down edges only on consecutive ladder rungs.
        this.checkPossibleLadder(parent);
    }

    public ArrayList<AStarNode> getPath(AStarNode start, AStarNode end, boolean searchMode) throws ThreadSafeUtilities.ChunkAccessException {
        this.targetNode = end;
        this.queue.offer(start);
        while (!this.queue.isEmpty() && !this.shouldInterrupt()) {
            AStarNode current = this.queue.poll();
            if (this.isNodeEnd(current, end)) {
                ArrayList<AStarNode> foundPath = new ArrayList<>();
                AStarNode cursor = current;
                while (cursor != null) {
                    foundPath.add(cursor);
                    if (cursor == start) return foundPath;
                    cursor = cursor.parent;
                }
                return null; // Never publish a path with a broken parent chain.
            }
            this.closedNodes.add(current);
            this.closedIndex.put(AStarOpenSet.Position.of(current), current);
            this.getNextCandidates(current, searchMode);
        }
        return null;
    }

    public boolean isBusy() {
        return this.isBusy;
    }

    protected boolean isCoordsEnd(int x, int y, int z, AStarNode end) {
        if (!this.config.tolerance) {
            return x == end.x && y == end.y && z == end.z;
        }
        return Math.abs(x - end.x) <= this.config.toleranceHorizontal && Math.abs(z - end.z) <= this.config.toleranceHorizontal && Math.abs(y - end.y) <= this.config.toleranceVertical;
    }

    protected boolean isNodeEnd(AStarNode cn, AStarNode end) {
        return this.isCoordsEnd(cn.x, cn.y, cn.z, end);
    }

    /**
     * A path discovered in B->A may be a useful candidate for A->B, notably
     * at a top-of-ladder entrance. NEVER assume that reversing a route is
     * physically valid: a jump, one-way drop or top exit may be directional.
     * The candidate is validated in the requested A->B direction.
     *
     * Called only after classic A* failed or found a large detour, not for
     * every ordinary village walk. The reverse worker has its own 150 ms cap.
     */
    private ArrayList<AStarNode> tryReverseClassic(ArrayList<AStarNode> forward) {
        if (this instanceof AStarWorkerJPS || this.config.allowDropping
                || this.startNode == null || this.targetNode == null
                || this.world == null || this.shouldInterrupt()) return forward;
        int vertical = Math.abs(this.startNode.y - this.targetNode.y);
        int dx = this.startNode.x - this.targetNode.x;
        int dz = this.startNode.z - this.targetNode.z;
        if (vertical < 2 || vertical > 48 || (long) dx * dx + (long) dz * dz > 96L * 96L) {
            return forward;
        }
        double direct = Math.sqrt((double)dx * dx + (double)dz * dz + (double)vertical * vertical);
        if (forward != null && routeCost(forward) <= direct * 1.8D + 4.0D) {
            return forward; // Forward route is already reasonably direct.
        }
        org.millenaire.common.pathing.NavigationMetrics.reverseTried();
        long started = System.nanoTime();
        // Reverse search gets its own 150 ms budget; reserve a small additional
        // window for checking the candidate in the requested direction.
        long reverseDeadline = started + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(175L);
        AStarWorker opposite = new AStarWorker(this.boss);
        opposite.setup(this.world, new AStarNode(this.targetNode.x, this.targetNode.y, this.targetNode.z),
                new AStarNode(this.startNode.x, this.startNode.y, this.startNode.z), this.config);
        ArrayList<AStarNode> discovered = opposite.runSync();
        boolean valid = discovered != null && discovered.size() > 1;
        String invalidReason = valid ? "NONE" : "NO_REVERSE_PATH";
        ArrayList<AStarNode> candidate = null;
        if (valid) {
            candidate = new ArrayList<>(discovered);
            java.util.Collections.reverse(candidate);
            valid = candidate.get(0).equals(this.targetNode)
                    && candidate.get(candidate.size() - 1).equals(this.startNode);
            if (!valid) invalidReason = "ENDPOINT_MISMATCH";
        }
        if (valid) {
            try {
                for (int i = candidate.size() - 1; i > 0; i--) {
                    if (Thread.currentThread().isInterrupted()
                            || System.nanoTime() > reverseDeadline) {
                        invalidReason = "VALIDATION_TIMEOUT";
                        valid = false;
                        break;
                    }
                    AStarNode from = candidate.get(i), to = candidate.get(i - 1);
                    if (Math.abs(from.y - to.y) > 1
                            || Math.abs(from.x - to.x) > 1
                            || Math.abs(from.z - to.z) > 1
                            || !AStarStatic.isViable(this.world, to, 0, this.config)
                            || !AStarStatic.isTransitionViable(this.world, from, to, this.config)
                            || (!this.config.canTakeDiagonals && from.y == to.y
                                && from.x != to.x && from.z != to.z
                                && AStarStatic.findSafeOrthogonalCorner(
                                    this.world, from, to, this.config) == null)) {
                        invalidReason = "BLOCKED_EDGE " + from.x + "," + from.y + "," + from.z
                                + "->" + to.x + "," + to.y + "," + to.z;
                        valid = false;
                        break;
                    }
                }
            } catch (ThreadSafeUtilities.ChunkAccessException exception) {
                invalidReason = "UNLOADED_CHUNK " + exception.x + "," + exception.z;
                valid = false;
            }
        }
        ArrayList<AStarNode> chosen = forward;
        if (valid && (chosen == null || routeCost(candidate) < routeCost(chosen))) {
            chosen = candidate;
            org.millenaire.common.pathing.NavigationMetrics.reverseFound();
        }
        if (MillConfigValues.LogPathing >= 2) {
            MillLog.major(this, "NAV-OPT-REVERSE from=" + this.startNode
                    + " to=" + this.targetNode
                    + " candidateValid=" + valid
                    + " reason=" + invalidReason
                    + " forwardCost=" + String.format(java.util.Locale.ROOT, "%.2f", routeCost(forward))
                    + " reverseCost=" + String.format(java.util.Locale.ROOT, "%.2f",
                            valid ? routeCost(candidate) : Double.POSITIVE_INFINITY)
                    + " chosen=" + (chosen == candidate ? "REVERSED" : "FORWARD")
                    + " tookMs=" + String.format(java.util.Locale.ROOT, "%.2f",
                            (System.nanoTime() - started) / 1.0E6D));
        }
        return chosen;
    }

    private static double routeCost(ArrayList<AStarNode> nodes) {
        if (nodes == null || nodes.size() < 2) return Double.POSITIVE_INFINITY;
        double result = 0.0D;
        for (int i = 1; i < nodes.size(); i++) {
            AStarNode a = nodes.get(i - 1), b = nodes.get(i);
            int x = a.x - b.x, y = a.y - b.y, z = a.z - b.z;
            result += Math.sqrt(x * x + y * y + z * z);
            if (x == 0 && z == 0 && y != 0) result += 0.8D;
        }
        return result;
    }

    @Override
    public void run() {
        this.isRunning = true;
        this.isBusy = true;
        final long began = System.nanoTime();
        this.timeLimit = began + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(150L);
        try {
            ArrayList<AStarNode> result = null;
            try {
                if (this.world instanceof Level) throw new IllegalArgumentException("Background search requires a detached block snapshot");
                result = this.getPath(this.startNode, this.targetNode, this.config.allowDropping);
                if (!this.shouldInterrupt()) {
                    result = this.tryReverseClassic(result);
                }
            }
            catch (ThreadSafeUtilities.ChunkAccessException e) {
                MillLog.error(this, "Chunk access violation while calculating a path for " + this.boss);
            }
            catch (Throwable e) {
                MillLog.printException("Exception while calculating a path:", e);
            }
            if (!Thread.currentThread().isInterrupted()) this.boss.completed(this, result);
        } finally {
            org.millenaire.common.pathing.NavigationMetrics.search(
                    this instanceof AStarWorkerJPS ? 0 : 1,
                    System.nanoTime() - began, this.closedNodes.size());
            this.isBusy = false;
            this.isRunning = false;
        }
    }

    public ArrayList<AStarNode> runSync() {
        this.timeLimit = System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(150L);
        ArrayList<AStarNode> result = null;
        try {
            result = this.getPath(this.startNode, this.targetNode, this.config.allowDropping);
        }
        catch (ThreadSafeUtilities.ChunkAccessException e) {
            MillLog.error(this, "Chunk access violation while calculating a path for " + this.boss);
            return null;
        }
        catch (Throwable e) {
            MillLog.printException("Exception while calculating a path:", e);
            return null;
        }
        return result;
    }

    public void setup(net.minecraft.world.level.BlockGetter winput, AStarNode start, AStarNode end, AStarConfig config) {
        this.world = winput;
        this.startNode = start;
        this.targetNode = end;
        this.config = config;
    }

    protected boolean shouldInterrupt() {
        return Thread.currentThread().isInterrupted() || System.nanoTime() > this.timeLimit;
    }

    private boolean tryToUpdateExistingHeapNode(AStarNode parent, AStarNode checkedOne) {
        return this.queue.improve(checkedOne, parent);
    }
}

