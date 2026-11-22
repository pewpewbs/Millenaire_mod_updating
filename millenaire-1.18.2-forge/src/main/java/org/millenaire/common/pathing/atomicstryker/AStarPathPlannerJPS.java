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
    public AStarConfig config;
    private net.minecraft.world.level.BlockGetter snapshot;
    private long nextSnapshotAttempt;
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
        if(this.world != null && this.world.getGameTime()<this.nextSnapshotAttempt)return;
        stopPathSearch(true);this.lastStart=copy(start);this.lastEnd=copy(end);this.config=copy(settings);
        this.snapshot=captureWorld(start,end);
        if(this.snapshot==null && this.world!=null) { this.nextSnapshotAttempt=this.world.getGameTime()+20+Math.floorMod(System.identityHashCode(this),17);return; }
        submit(false);
    }
    private void submit(boolean classic) {
        AStarWorker next=createWorker(classic);this.worker=next;this.busy=true;
        next.setup(this.snapshot,copy(this.lastStart),copy(this.lastEnd),copy(this.config));
        next.requestGeneration=this.generation;next.classicAttempt=classic;
        this.future=PathSearchExecutor.submit(next);
        if(this.future==null) { this.busy=false;this.worker=null;this.snapshot=null; } // Backpressure is not an unreachable destination.
    }
    public void getPath(int x,int y,int z,int dx,int dy,int dz,AStarConfig settings) throws ThreadSafeUtilities.ChunkAccessException {
        if(!AStarStatic.isViable(this.world,x,y,z,0,settings))--y;
        if(!AStarStatic.isViable(this.world,x,y,z,0,settings))y+=2;
        if(!AStarStatic.isViable(this.world,x,y,z,0,settings))--y;
        getPath(new AStarNode(x,y,z),new AStarNode(dx,dy,dz),settings);
    }
    public synchronized boolean isBusy() { return this.busy; }
    public void completed(AStarWorker source,ArrayList<AStarNode> result) {
        dispatch(()->deliver(source,result));
    }
    private synchronized void deliver(AStarWorker source,ArrayList<AStarNode> result) {
        if(source!=this.worker||source.requestGeneration!=this.generation)return;
        this.busy=false;this.future=null;this.worker=null;
        if(result==null&&this.isJPS&&!source.classicAttempt) { submit(true);return; }
        this.snapshot=null;
        if(this.pathedEntity!=null) {
            if(result==null)this.pathedEntity.onNoPathAvailable();
            else this.pathedEntity.onFoundPath(result);
        }
    }
    public synchronized void stopPathSearch(boolean interrupted) {
        ++this.generation;PathSearchExecutor.cancel(this.future);this.future=null;this.worker=null;this.busy=false;this.snapshot=null;
        if(!interrupted&&this.pathedEntity!=null)this.pathedEntity.onNoPathAvailable();
    }
}
