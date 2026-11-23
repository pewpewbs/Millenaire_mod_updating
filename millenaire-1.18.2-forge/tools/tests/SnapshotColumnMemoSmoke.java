import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.millenaire.common.pathing.SnapshotColumnMemo;

/**
 * No-Forge Java 17 regression for per-snapshot collision-column memo.
 *
 * From millenaire-1.18.2-forge:
 * javac --release 17 -d build/nav-column-smoke
 *   src/main/java/org/millenaire/common/pathing/SnapshotColumnMemo.java
 *   tools/tests/SnapshotColumnMemoSmoke.java
 * java -cp build/nav-column-smoke SnapshotColumnMemoSmoke
 */
public final class SnapshotColumnMemoSmoke {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) throws InterruptedException {
        SnapshotColumnMemo memo = new SnapshotColumnMemo(32);
        long airColumn = 0x0000_0001_0000_0002L;
        long stairColumn = 0x0000_0001_0000_0022L;
        check(memo.get(airColumn) == null, "unknown column returns null");
        memo.remember(airColumn, true);
        check(Boolean.TRUE.equals(memo.get(airColumn)), "true result is cached");
        memo.remember(airColumn, false);
        check(Boolean.FALSE.equals(memo.get(airColumn)), "false result is cached");
        check(memo.capacity() == 32, "fixed bounded table size");

        // One slot intentionally forces a collision. A stale answer must
        // never be returned for the displaced coordinate.
        SnapshotColumnMemo collision = new SnapshotColumnMemo(1);
        collision.remember(airColumn, true);
        collision.remember(stairColumn, false);
        check(collision.get(airColumn) == null, "slot collision is a MISS, not a wrong hit");
        check(Boolean.FALSE.equals(collision.get(stairColumn)), "latest key matches exactly");
        collision.remember(airColumn, true);
        check(collision.get(stairColumn) == null, "replaced key cannot return stale value");

        SnapshotColumnMemo snapshotA = new SnapshotColumnMemo(16);
        SnapshotColumnMemo snapshotB = new SnapshotColumnMemo(16);
        snapshotA.remember(airColumn, true);
        check(snapshotB.get(airColumn) == null, "another snapshot has its own cache");
        snapshotB.remember(airColumn, false);
        check(Boolean.TRUE.equals(snapshotA.get(airColumn)), "no cross-snapshot contamination");
        check(Boolean.FALSE.equals(snapshotB.get(airColumn)), "independent geometry is allowed");

        // Two path workers can read/update the same detached snapshot.
        SnapshotColumnMemo concurrent = new SnapshotColumnMemo(256);
        CountDownLatch start = new CountDownLatch(1), finished = new CountDownLatch(2);
        for (int worker = 0; worker < 2; worker++) {
            final int id = worker;
            new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 10_000; i++) {
                        long key = (long) id * 100_000L + i;
                        concurrent.remember(key, (i & 1) == 0);
                        Boolean observed = concurrent.get(key);
                        // Other writes can collide and evict the slot, but
                        // must never publish the opposite value for THIS key.
                        if (observed != null && observed != ((i & 1) == 0)) {
                            throw new AssertionError("wrong value for matching column key");
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finished.countDown();
                }
            }, "column-memo-test-" + id).start();
        }
        start.countDown();
        check(finished.await(5, TimeUnit.SECONDS), "concurrent workers must finish");
        boolean badCapacity = false;
        try { new SnapshotColumnMemo(3); }
        catch (IllegalArgumentException expected) { badCapacity = true; }
        check(badCapacity, "power-of-two requirement enforced");
        System.out.println("PASS: bounded snapshot column memo, negative results, collision safety, isolation");
    }
}
