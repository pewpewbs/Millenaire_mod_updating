from pathlib import Path
import shutil
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');base=r/'src/main/java/org/millenaire/common';bk=r/'migration/before-scheduler27';bk.mkdir(exist_ok=False)
def edit(n,fn):
 p=base/n;q=bk/n;q.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,q);p.write_text(fn(p.read_text()),encoding='utf-8')
(base/'pathing/atomicstryker/PathSearchExecutor.java').write_text('''package org.millenaire.common.pathing.atomicstryker;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public final class PathSearchExecutor {
    private static final AtomicLong sequence=new AtomicLong(),rejected=new AtomicLong(),submitted=new AtomicLong();
    private static final ThreadPoolExecutor POOL=new ThreadPoolExecutor(2,2,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(64),r->{
        Thread t=new Thread(r,"Millenaire-path-"+sequence.incrementAndGet());t.setDaemon(true);return t;
    },new ThreadPoolExecutor.AbortPolicy());
    static { POOL.allowCoreThreadTimeOut(true); }
    private PathSearchExecutor() {}
    public static FutureTask<Void> submit(Runnable task) {
        FutureTask<Void> future=new FutureTask<>(task,null);
        try { POOL.execute(future);submitted.incrementAndGet();return future; }
        catch(RejectedExecutionException full) { rejected.incrementAndGet();return null; }
    }
    public static void cancel(FutureTask<Void> future) {
        if(future!=null) { future.cancel(true);POOL.remove(future); }
    }
    public static long[] metrics() { return new long[]{submitted.get(),rejected.get(),POOL.getActiveCount(),POOL.getQueue().size(),POOL.getCompletedTaskCount()}; }
}
''',encoding='utf-8')
def planner(s):
 a=s.index('public class AStarPathPlannerJPS');s=s[:a]+'''public class AStarPathPlannerJPS {
    private final Level world;
    private final IAStarPathedEntity pathedEntity;
    private final boolean isJPS;
    private AStarWorker worker;
    private java.util.concurrent.FutureTask<Void> future;
    private long generation;
    private boolean busy;
    private AStarNode lastStart,lastEnd;
    public AStarConfig config;

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
        stopPathSearch(true);this.lastStart=copy(start);this.lastEnd=copy(end);this.config=copy(settings);submit(false);
    }
    private void submit(boolean classic) {
        AStarWorker next=createWorker(classic);this.worker=next;this.busy=true;
        next.setup(this.world,copy(this.lastStart),copy(this.lastEnd),copy(this.config));
        next.requestGeneration=this.generation;next.classicAttempt=classic;
        this.future=PathSearchExecutor.submit(next);
        if(this.future==null) { this.busy=false;this.worker=null; } // Backpressure is not an unreachable destination.
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
        if(this.pathedEntity!=null) {
            if(result==null)this.pathedEntity.onNoPathAvailable();
            else this.pathedEntity.onFoundPath(result);
        }
    }
    public synchronized void stopPathSearch(boolean interrupted) {
        ++this.generation;PathSearchExecutor.cancel(this.future);this.future=null;this.worker=null;this.busy=false;
        if(!interrupted&&this.pathedEntity!=null)this.pathedEntity.onNoPathAvailable();
    }
}
''';return s
edit('pathing/atomicstryker/AStarPathPlannerJPS.java',planner)
def worker(s):
 s=s.replace('    private long timeLimit;','    private long timeLimit;\n    long requestGeneration;\n    boolean classicAttempt;')
 s=s.replace('System.currentTimeMillis() + 150L','System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(150L)')
 a=s.index('            // A failed search must notify');b=s.index('        } finally {',a)
 s=s[:a]+'''            if (!Thread.currentThread().isInterrupted()) this.boss.completed(this, result);
'''+s[b:]
 s=s.replace('return System.currentTimeMillis() > this.timeLimit;','return Thread.currentThread().isInterrupted() || System.nanoTime() > this.timeLimit;')
 return s
edit('pathing/atomicstryker/AStarWorker.java',worker)
edit('pathing/atomicstryker/AStarWorkerJPS.java',lambda s:s.replace('        int nzY;','        if (this.shouldInterrupt()) return null;\n        int nzY;'))
def resident(s):
 s=s.replace('    private List<Node> computeNewPath(Point dest) {','''    private List<Node> computeNewPath(Point dest) {
        this.pathCalculatedSinceLastTick = null;
        this.pathFailedSincelastTick = false;
        if (this.pathPlannerJPS != null) this.pathPlannerJPS.stopPathSearch(true);''')
 s=s.replace('    public void remove(Entity.RemovalReason reason) {','''    public void remove(Entity.RemovalReason reason) {
        if (this.pathPlannerJPS != null) this.pathPlannerJPS.stopPathSearch(true);
        this.pathCalculatedSinceLastTick = null;''')
 return s
edit('entity/MillVillager.java',resident)
print('Bounded executor, real cancellation, generation checks and server-thread delivery')
