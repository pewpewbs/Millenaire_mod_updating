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

    private void checkPossibleLadder(AStarNode node) throws ThreadSafeUtilities.ChunkAccessException {
        int x = node.x;
        int y = node.y;
        int z = node.z;
        if (AStarStatic.isLadder(this.world, ThreadSafeUtilities.getBlock(this.world, x, y, z), x, y, z)) {
            AStarNode ladder = null;
            if (AStarStatic.isLadder(this.world, ThreadSafeUtilities.getBlock(this.world, x, y + 1, z), x, y + 1, z)) {
                // empty if block
            }
            if (!this.tryToUpdateExistingHeapNode(node, ladder = new AStarNode(x, y + 1, z, node.getG() + 2, node, this.targetNode))) {
                this.addToBinaryHeap(ladder);
            }
            if (AStarStatic.isLadder(this.world, ThreadSafeUtilities.getBlock(this.world, x, y - 1, z), x, y - 1, z)) {
                // empty if block
            }
            if (!this.tryToUpdateExistingHeapNode(node, ladder = new AStarNode(x, y - 1, z, node.getG() + 2, node, this.targetNode))) {
                this.addToBinaryHeap(ladder);
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
                if (this.tryToUpdateExistingHeapNode(parent, check) || !AStarStatic.isViable(this.world, check, c[i][1], this.config)) continue;
                this.addToBinaryHeap(check);
                continue;
            }
            catch (Exception e) {
                if (MillConfigValues.LogChunkLoader < 2) continue;
                MillLog.minor(this, e.getLocalizedMessage());
            }
        }
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

    @Override
    public void run() {
        this.isRunning = true;
        this.isBusy = true;
        this.timeLimit = System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(150L);
        try {
            ArrayList<AStarNode> result = null;
            try {
                if (this.world instanceof Level) throw new IllegalArgumentException("Background search requires a detached block snapshot");
                result = this.getPath(this.startNode, this.targetNode, this.config.allowDropping);
            }
            catch (ThreadSafeUtilities.ChunkAccessException e) {
                MillLog.error(this, "Chunk access violation while calculating a path for " + this.boss);
            }
            catch (Throwable e) {
                MillLog.printException("Exception while calculating a path:", e);
            }
            if (!Thread.currentThread().isInterrupted()) this.boss.completed(this, result);
        } finally {
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

