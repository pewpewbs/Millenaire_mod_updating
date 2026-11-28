package org.millenaire.common.pathing;

import java.util.Locale;
import java.util.Objects;

/**
 * Process-wide, wall-clock deltas for cumulative navigation counters.
 *
 * A game tick can exceed 50 ms during lag. Rates therefore use monotonic
 * elapsed real time, not an assumed 20 TPS. This is diagnostic code only:
 * it must not participate in decisions about paths or movement.
 */
public final class NavigationRateWindow {
    public record Counts(long jps, long astar, long snapshots, long copiedSections,
                         long cacheHits, long cacheMisses, long cacheInvalidated,
                         long failedRetryDeferred, long actualNoPathFailures, long cacheReplayDeferred,
                         long inflightJoined, long queueStarted, long queueWaitNanos,
                         long ladderScans, long ladderProbes,
                         long searchPreempted, long goalDriftRequests, long goalDriftBursts) {}

    private Counts previous;
    private long lastNanos;

    public synchronized String sample(long nowNanos, Counts totals) {
        Objects.requireNonNull(totals, "totals");
        if (previous == null || nowNanos <= lastNanos) {
            previous = totals;
            lastNanos = nowNanos;
            return "window_scope=process window_state=warmup";
        }
        long elapsed = nowNanos - lastNanos;
        // Two worlds may report in the same game tick. Keep the original
        // baseline instead of producing artificially huge per-second rates.
        if (elapsed < 1_000_000_000L) {
            return "window_scope=process window_state=pending";
        }
        double seconds = elapsed / 1.0E9D;
        long jps = delta(totals.jps, previous.jps);
        long astar = delta(totals.astar, previous.astar);
        long snapshot = delta(totals.snapshots, previous.snapshots);
        long sections = delta(totals.copiedSections, previous.copiedSections);
        long hits = delta(totals.cacheHits, previous.cacheHits);
        long misses = delta(totals.cacheMisses, previous.cacheMisses);
        long invalidated = delta(totals.cacheInvalidated, previous.cacheInvalidated);
        long failed = delta(totals.failedRetryDeferred, previous.failedRetryDeferred);
        long actualNoPath = delta(totals.actualNoPathFailures, previous.actualNoPathFailures);
        long cacheReplay = delta(totals.cacheReplayDeferred, previous.cacheReplayDeferred);
        long joined = delta(totals.inflightJoined, previous.inflightJoined);
        long queueStarted = delta(totals.queueStarted, previous.queueStarted);
        long queueNanos = delta(totals.queueWaitNanos, previous.queueWaitNanos);
        long ladderScans = delta(totals.ladderScans, previous.ladderScans);
        long ladderProbes = delta(totals.ladderProbes, previous.ladderProbes);
        long preempted = delta(totals.searchPreempted, previous.searchPreempted);
        long drift = delta(totals.goalDriftRequests, previous.goalDriftRequests);
        long bursts = delta(totals.goalDriftBursts, previous.goalDriftBursts);
        previous = totals;
        lastNanos = nowNanos;
        double hitPct = hits + misses == 0 ? 0.0 : hits * 100.0 / (hits + misses);
        double waitMs = queueStarted == 0 ? 0.0 : queueNanos / 1.0E6D / queueStarted;
        return String.format(Locale.ROOT,
                "window_scope=process window_wall_s=%.2f jps_s=%.2f astar_s=%.2f"
                + " snapshots_s=%.2f sections_s=%.2f cache_hit_pct=%.2f"
                + " cache_invalidated_s=%.2f failed_retry_deferred_s=%.2f no_path_failures_s=%.2f"
                + " cache_replay_deferred_s=%.2f inflight_joined_s=%.2f"
                + " queue_wait_window_avg_ms=%.3f ladder_scans_s=%.2f ladder_probes_s=%.2f"
                + " search_preempted_s=%.2f moving_goal_requests_s=%.2f moving_goal_bursts_s=%.2f",
                seconds, jps / seconds, astar / seconds, snapshot / seconds,
                sections / seconds, hitPct, invalidated / seconds, failed / seconds,
                actualNoPath / seconds, cacheReplay / seconds, joined / seconds, waitMs,
                ladderScans / seconds, ladderProbes / seconds,
                preempted / seconds, drift / seconds, bursts / seconds);
    }

    public synchronized void reset() {
        previous = null;
        lastNanos = 0L;
    }

    private static long delta(long current, long previous) {
        // Some tests reset the cumulative counters while the server is up.
        // Clamp rather than report a negative rate or overflow.
        return current >= previous ? current - previous : current;
    }
}
