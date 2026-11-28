import org.millenaire.common.pathing.SnapshotCaptureIndex;

/**
 * Dependency-free Java 17 regression for multiple snapshots per game tick.
 *
 * From millenaire-1.18.2-forge:
 * javac --release 17 -d build/nav-capture-index-smoke
 *   src/main/java/org/millenaire/common/pathing/SnapshotCaptureIndex.java
 *   tools/tests/SnapshotCaptureIndexSmoke.java
 * java -cp build/nav-capture-index-smoke SnapshotCaptureIndexSmoke
 */
public final class SnapshotCaptureIndexSmoke {
    private static void check(boolean yes, String message) {
        if (!yes) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        var index = new SnapshotCaptureIndex<String>(4);
        check(index.find(100, 2, 3, 2, 3) == null, "index initially empty");
        index.remember(100, 0, 10, 0, 10, "first");
        index.remember(100, 20, 30, 20, 30, "second");
        var old = index.find(100, 2, 3, 2, 3);
        check(old != null && old.snapshot().equals("first") && old.nonLatest(),
                "an older superset remains discoverable after a disjoint later capture");
        var newest = index.find(100, 21, 22, 21, 22);
        check(newest != null && newest.snapshot().equals("second") && !newest.nonLatest(),
                "latest containing capture is also reusable");
        check(index.find(100, 9, 21, 9, 21) == null,
                "two disjoint snapshots cannot be stitched together as one");
        check(index.find(100, 9, 11, 1, 3) == null,
                "partial overlap is not a superset");

        // Prefer the smallest live containing snapshot, not the newest copy.
        index.remember(100, -10, 40, -10, 40, "larger");
        var smallest = index.find(100, 2, 3, 2, 3);
        check(smallest != null && smallest.snapshot().equals("first") && smallest.nonLatest(),
                "smallest valid containing rectangle wins");
        check(index.invalidate(100) == 3, "known event evicts all three live captures");
        check(index.find(100, 2, 3, 2, 3) == null, "invalidated cache not reused");
        index.remember(100, 0, 4, 0, 4, "afterMutation");
        check(index.find(100, 1, 2, 1, 2) != null,
                "index supports new independent copies, if caller allows it");

        // Caller-side mutation poison prevents that new copy from being added
        // at all; this index intentionally implements no mutation heuristic.
        check(index.find(101, 1, 2, 1, 2) == null,
                "tick boundary drops all full snapshots");
        index.remember(101, 1, 2, 3, 4, "nextTick");
        check(index.find(100, 1, 2, 3, 4) == null,
                "backward tick also drops later copies");

        var bounded = new SnapshotCaptureIndex<String>(2);
        bounded.remember(500, 0, 2, 0, 2, "old");
        bounded.remember(500, 10, 12, 10, 12, "middle");
        bounded.remember(500, 20, 22, 20, 22, "new");
        check(bounded.find(500, 1, 1, 1, 1) == null,
                "fixed-size index evicts oldest entry after saturation");
        check(bounded.find(500, 11, 11, 11, 11) != null,
                "older live entry retained while within capacity");
        check(bounded.invalidate(500) == 2, "eviction count honors capacity");
        check(bounded.invalidate(500) == 0, "repeat invalidation is idempotent");

        boolean invalidBounds = false;
        try { bounded.remember(500, 5, 4, 0, 1, "invalid"); }
        catch (IllegalArgumentException expected) { invalidBounds = true; }
        check(invalidBounds, "malformed region must be rejected");
        check(bounded.find(500, 5, 4, 0, 1) == null,
                "malformed query never returns a snapshot");

        boolean invalidCapacity = false;
        try { new SnapshotCaptureIndex<String>(0); }
        catch (IllegalArgumentException expected) { invalidCapacity = true; }
        check(invalidCapacity, "zero-capacity catalogs cannot be created");
        System.out.println("PASS: non-latest reuse, smallest superset, mutation eviction, tick isolation and bounds");
    }
}
