import org.millenaire.common.pathing.NavigationRateWindow;

/**
 * Standalone Java 17 test (no Forge dependencies).
 * javac --release 17 -d build/nav-window-smoke
 *   src/main/java/org/millenaire/common/pathing/NavigationRateWindow.java
 *   tools/tests/NavigationRateWindowSmoke.java
 * java -cp build/nav-window-smoke NavigationRateWindowSmoke
 */
public final class NavigationRateWindowSmoke {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static NavigationRateWindow.Counts counts(long jps, long astar,
            long snapshots, long sections, long hits, long misses,
            long invalidations, long failed, long cachedRetries, long joined,
            long queueStarted, long queueWaitNanos, long ladderScans, long ladderProbes) {
        return new NavigationRateWindow.Counts(jps, astar, snapshots, sections,
                hits, misses, invalidations, failed, cachedRetries, joined,
                queueStarted, queueWaitNanos, ladderScans, ladderProbes);
    }

    public static void main(String[] args) {
        NavigationRateWindow window = new NavigationRateWindow();
        NavigationRateWindow.Counts zero =
                counts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        check(window.sample(1_000L, zero).contains("window_state=warmup"),
                "first report warms up");
        var baseline = counts(10, 6, 4, 100, 5, 15, 2, 4, 3, 8,
                2, 8_000_000L, 3, 100);
        check(window.sample(2_000_001_000L, baseline).contains("jps_s=5.00"),
                "rates must divide count deltas by elapsed wall seconds");
        window.reset();
        check(window.sample(100L, baseline).contains("window_state=warmup"),
                "reset starts a new window");
        check(window.sample(500_000_100L, baseline).contains("window_state=pending"),
                "short reporting interval retains old baseline");
        var next = counts(20, 12, 6, 120, 10, 30, 4, 5, 4, 10,
                4, 16_000_000L, 4, 160);
        String report = window.sample(2_000_000_100L, next);
        check(report.contains("jps_s=5.00"), "JPS rate since baseline");
        check(report.contains("astar_s=3.00"), "classic A* rate since baseline");
        check(report.contains("cache_hit_pct=25.00"), "window cache hit percentage");
        check(report.contains("queue_wait_window_avg_ms=4.000"),
                "queue wait must use interval samples, not lifetime average");
        check(report.contains("ladder_probes_s=30.00"), "ladder probe rate");
        check(report.contains("window_scope=process"), "global counters labeled accurately");

        String resetCounter = window.sample(4_000_000_100L,
                counts(1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        check(resetCounter.contains("jps_s=0.50"), "counter reset must not make negative rate");
        check(!resetCounter.contains("NaN") && !resetCounter.contains("Infinity"),
                "zero samples must be finite");
        System.out.println("PASS: wall-clock windows, deltas, cache hit rate, queue wait and reset");
    }
}
