import org.millenaire.common.pathing.SnapshotSectionPool;

/**
 * Dependency-free Java 17 regression:
 * javac --release 17 -d build/nav-snapshot-pool-smoke
 *   src/main/java/org/millenaire/common/pathing/SnapshotSectionPool.java
 *   tools/tests/SnapshotSectionPoolSmoke.java
 * java -cp build/nav-snapshot-pool-smoke SnapshotSectionPoolSmoke
 */
public final class SnapshotSectionPoolSmoke {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        var pool = new SnapshotSectionPool<String>(3, 8, 3);
        Object source1 = new Object(), source2 = new Object();
        check(pool.begin(100, 3), "first region reserves 3 chunk checks");
        check(pool.get(100, 42L, source1) == null, "first section is not cached");
        pool.remember(100, 42L, source1, "copy1");
        check(pool.get(100, 42L, source1).equals("copy1"), "same-tick copy reused");
        check(pool.usedCopies() == 1 && pool.reusedCopies() == 1, "copy/hit accounting");

        check(pool.begin(100, 2), "partially overlapping region may also capture");
        check(pool.get(100, 42L, source1).equals("copy1"), "overlap shares copy");
        pool.remember(100, 43L, source2, "copy2");
        check(pool.usedChunkVisits() == 5, "total region visits bounded");
        check(pool.begin(100, 2), "third region may capture");
        pool.remember(100, 44L, source1, "copy3");
        check(!pool.canCopy(100), "hard upper limit for copies");
        check(!pool.begin(100, 1), "max snapshot count enforced");
        check(pool.usedCopies() == 3, "failed admission never copies a section");

        // Object identity matters even for the exact same SectionPos.
        check(pool.get(100, 43L, new Object()) == null, "replaced palette is invalidated");
        check(!pool.begin(100, 2), "budget exhausted before walking chunks");

        // New server tick must discard old live source identities + copies.
        check(pool.begin(101, 3), "new tick resets admission limits");
        check(pool.get(101, 42L, source1) == null, "never reuse sections across ticks");
        check(pool.usedCopies() == 0 && pool.reusedCopies() == 0,
                "no stale copy references or counters after tick boundary");
        check(pool.canCopy(101), "copy allowance reset on new tick");
        pool.remember(101, 42L, source1, "freshCopy");
        check(pool.get(101, 42L, source1).equals("freshCopy"), "fresh copy available");

        var otherWorld = new SnapshotSectionPool<String>(3, 8, 3);
        check(otherWorld.begin(101, 1), "second world is independent");
        check(otherWorld.get(101, 42L, source1) == null, "no cross-world copy reuse");
        check(!otherWorld.begin(101, 9), "area beyond chunk budget rejected");
        check(otherWorld.usedChunkVisits() == 1, "failed admission does not spend quota");

        var visitPool = new SnapshotSectionPool<String>(4, 4, 10);
        check(visitPool.begin(5, 3), "first capture accepted");
        check(!visitPool.begin(5, 2), "aggregate chunk check budget enforced");
        check(visitPool.begin(5, 1), "failed admission did not consume allowance");

        boolean exhausted = false;
        var oneCopy = new SnapshotSectionPool<String>(2, 8, 1);
        oneCopy.begin(1, 1);
        oneCopy.remember(1, 1L, source1, "first");
        try { oneCopy.remember(1, 2L, source1, "second"); }
        catch (IllegalStateException expected) { exhausted = true; }
        check(exhausted, "copy budget fail-closed");
        // NAV-OPT-12: known-mutation hints must evict stale copies without
        // resetting per-tick CPU limits (otherwise edits could bypass caps).
        var edits = new SnapshotSectionPool<String>(4, 8, 2);
        check(edits.begin(200, 2), "mutation test snapshot admitted");
        edits.remember(200, 10L, source1, "before");
        edits.remember(200, 20L, source2, "other");
        check(edits.invalidateSection(200, 10L), "specific section evicted");
        check(edits.get(200, 10L, source1) == null, "dirty section must not be reused");
        check(edits.get(200, 20L, source2).equals("other"), "untouched section kept");
        check(edits.usedCopies() == 2 && !edits.canCopy(200),
                "invalidation must not refund section-copy quota");
        check(edits.invalidateAll(200) == 1, "whole pool evicts remaining copies");
        check(edits.usedChunkVisits() == 2 && edits.usedCopies() == 2,
                "whole-pool invalidation must preserve current-tick work budgets");
        check(edits.begin(201, 2), "new tick resets budgets after mutation");
        check(edits.canCopy(201) && edits.get(201, 20L, source2) == null,
                "old copies are never used across tick boundaries");

        System.out.println("PASS: snapshot same-tick overlap, quotas, mutations, source identity and tick isolation");
    }
}
