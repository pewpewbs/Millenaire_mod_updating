package org.millenaire.common.pathing.atomicstryker;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Shared bounded CPU executor for detached-snapshot path searches.
 * Search priority is deliberately unchanged: FIFO prevents starvation and
 * ensures DOOR/LADDER route semantics are unaffected by NAV-OPT-09.
 */
public final class PathSearchExecutor {
    private static final AtomicLong sequence = new AtomicLong();
    private static final AtomicLong rejected = new AtomicLong();
    private static final AtomicLong submitted = new AtomicLong();
    private static final AtomicLong started = new AtomicLong();
    private static final AtomicLong queueWaitNanos = new AtomicLong();
    private static final AtomicLong maxQueueWaitNanos = new AtomicLong();

    private static final ThreadPoolExecutor POOL = new ThreadPoolExecutor(
            2, 2, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(64), runnable -> {
                Thread thread = new Thread(runnable, "Millenaire-path-" + sequence.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());

    static { POOL.allowCoreThreadTimeOut(true); }

    private PathSearchExecutor() {}

    private static final class MeasuredTask extends FutureTask<Void> {
        private final long enqueuedAtNanos = System.nanoTime();

        private MeasuredTask(Runnable runnable) {
            super(runnable, null);
        }

        @Override
        public void run() {
            // A canceled queued task must not contribute bogus waiting time.
            if (!isCancelled()) {
                long waitNanos = Math.max(0L, System.nanoTime() - enqueuedAtNanos);
                started.incrementAndGet();
                queueWaitNanos.addAndGet(waitNanos);
                maxQueueWaitNanos.accumulateAndGet(waitNanos, Math::max);
            }
            super.run();
        }
    }

    public static FutureTask<Void> submit(Runnable task) {
        FutureTask<Void> future = new MeasuredTask(task);
        try {
            POOL.execute(future);
            submitted.incrementAndGet();
            return future;
        } catch (RejectedExecutionException full) {
            rejected.incrementAndGet();
            return null;
        }
    }

    public static void cancel(FutureTask<Void> future) {
        if (future != null) {
            future.cancel(true);
            POOL.remove(future);
        }
    }

    /**
     * Backward-compatible first five slots:
     * [submitted, rejected, active, queued, completed].
     * New slots: [started, totalQueueWaitNanos, maxQueueWaitNanos].
     */
    public static long[] metrics() {
        return new long[] {
                submitted.get(), rejected.get(),
                POOL.getActiveCount(), POOL.getQueue().size(),
                POOL.getCompletedTaskCount(),
                started.get(), queueWaitNanos.get(), maxQueueWaitNanos.get()
        };
    }
}
