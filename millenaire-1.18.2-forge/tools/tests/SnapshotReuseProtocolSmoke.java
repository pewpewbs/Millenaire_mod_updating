import org.millenaire.common.pathing.SnapshotCaptureIndex;
import org.millenaire.common.pathing.SnapshotSectionPool;

/**
 * Tests the protocol shared between NAV-OPT-11/13/14 without Minecraft.
 *
 * From millenaire-1.18.2-forge:
 * javac --release 17 -d build/nav-snapshot-protocol-smoke
 *   src/main/java/org/millenaire/common/pathing/SnapshotSectionPool.java
 *   src/main/java/org/millenaire/common/pathing/SnapshotCaptureIndex.java
 *   tools/tests/SnapshotReuseProtocolSmoke.java
 * java -cp build/nav-snapshot-protocol-smoke SnapshotReuseProtocolSmoke
 */
public final class SnapshotReuseProtocolSmoke {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        var pool = new SnapshotSectionPool<String>(4, 256, 6);
        var index = new SnapshotCaptureIndex<String>(4);
        Object source = new Object();
        long tick = 1200L;
        // Snapshot A in an overlapping region, then snapshot B elsewhere.
        check(pool.begin(tick, 16), "snapshot A admitted");
        pool.remember(tick, 1L, source, "A-section");
        index.remember(tick, 0, 7, 0, 7, "A");
        check(pool.begin(tick, 9), "snapshot B admitted");
        index.remember(tick, 20, 28, 20, 28, "B");
        check(index.find(tick, 1, 3, 1, 3).nonLatest(),
                "A remains reusable despite more recent B");
        check(pool.get(tick, 1L, source).equals("A-section"),
                "section copy can be shared before known mutation");

        // Forge PRE-break notification: remove stale snapshots immediately.
        long priorRevision = pool.revision(tick);
        pool.invalidateSection(tick, 1L);
        index.invalidate(tick);
        check(!pool.mayReuseSnapshots(tick), "PRE-change disables full reuse");
        check(index.find(tick, 1, 3, 1, 3) == null, "old A cannot be reused");
        check(index.find(tick, 21, 22, 21, 22) == null,
                "conservative invalidation removes unrelated B too");
        check(pool.get(tick, 1L, source) == null,
                "dirty section cannot return a stale palette");
        check(pool.revision(tick) != priorRevision,
                "an in-flight snapshot can reject observed mutation race");

        // A new search may still copy current state under normal budgets,
        // but dirty sections must never go back into the shared cache.
        check(pool.begin(tick, 4), "fresh search admitted under existing quota");
        pool.remember(tick, 1L, source, "copy-after-pre-event");
        check(pool.get(tick, 1L, source) == null,
                "possibly pre-mutation value is never re-shared");
        check(pool.usedCopies() == 2, "copy quota is not reset by event");
        check(pool.usedSnapshots() == 3, "admission quota is not reset by event");

        // Only the NEXT server tick makes a clean cache safe for reuse.
        tick++;
        check(pool.mayReuseSnapshots(tick), "new tick clears poison");
        check(index.find(tick, 1, 2, 1, 2) == null, "no cross-tick snapshot");
        check(pool.begin(tick, 4), "new tick resets admission budget");
        pool.remember(tick, 1L, source, "new-tick-copy");
        index.remember(tick, 0, 7, 0, 7, "new-tick-snapshot");
        check(pool.get(tick, 1L, source).equals("new-tick-copy"),
                "a fresh copy can be reused next tick");
        check(index.find(tick, 1, 2, 1, 2) != null,
                "fresh snapshot can be reused next tick");
        System.out.println("PASS: superset reuse, PRE-event eviction, poison, revision and next-tick isolation");
    }
}
