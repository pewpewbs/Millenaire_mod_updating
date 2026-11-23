import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.millenaire.common.pathing.atomicstryker.PathSearchExecutor;

/**
 * Plain Java 17, no Forge dependencies.
 *
 * javac --release 17 -d build/nav-executor-smoke
 *   src/main/java/org/millenaire/common/pathing/atomicstryker/PathSearchExecutor.java
 *   tools/tests/PathSearchExecutorSmoke.java
 * java -cp build/nav-executor-smoke PathSearchExecutorSmoke
 */
public final class PathSearchExecutorSmoke {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        var entered = new CountDownLatch(2);
        var release = new CountDownLatch(1);
        var executed = new AtomicInteger();
        long[] before = PathSearchExecutor.metrics();
        Runnable blocker = () -> {
            entered.countDown();
            try { release.await(4, TimeUnit.SECONDS); }
            catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        };
        FutureTask<Void> a = null, b = null;
        try {
            a = PathSearchExecutor.submit(blocker);
            b = PathSearchExecutor.submit(blocker);
            check(a != null && b != null, "two worker slots must be available");
            check(entered.await(2, TimeUnit.SECONDS), "both workers must start");

            var third = PathSearchExecutor.submit(executed::incrementAndGet);
            var canceled = PathSearchExecutor.submit(() -> executed.addAndGet(100));
            check(third != null && canceled != null, "tasks must enter bounded queue");
            Thread.sleep(40);
            PathSearchExecutor.cancel(canceled);
            check(canceled.isCancelled(), "canceled task stays canceled");

            release.countDown();
            a.get(2, TimeUnit.SECONDS);
            b.get(2, TimeUnit.SECONDS);
            third.get(2, TimeUnit.SECONDS);
            long[] after = PathSearchExecutor.metrics();
            check(executed.get() == 1, "canceled queued task must never execute");
            check(after[0] - before[0] == 4, "submitted includes accepted tasks");
            check(after[5] - before[5] == 3, "only executed tasks count as started");
            check(after[6] > before[6], "queue time must be captured");
            check(after[7] >= TimeUnit.MILLISECONDS.toNanos(15),
                    "queued task must show observable wait");
            check(after[3] == 0, "canceled task must be removed from queue");
            System.out.println("PASS: executor FIFO, queue wait, cancellation, metrics and task counting");
        } finally {
            release.countDown();
            PathSearchExecutor.cancel(a);
            PathSearchExecutor.cancel(b);
        }
    }
}
