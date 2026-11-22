package org.millenaire.common.pathing.atomicstryker;
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
