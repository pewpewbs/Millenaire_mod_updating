package org.millenaire.common.pathing;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.AtomicLong;
public final class NavigationMetrics {
    private static final LongAdder[] COUNT = {new LongAdder(),new LongAdder()};
    private static final LongAdder[] NANOS = {new LongAdder(),new LongAdder()};
    private static final AtomicLong[] MAX = {new AtomicLong(),new AtomicLong()};
    private static final LongAdder SECTIONS = new LongAdder(), DEFERRED = new LongAdder(), STALE = new LongAdder();
    private static final LongAdder[] SEARCH_COUNT = {new LongAdder(), new LongAdder()};
    private static final LongAdder[] SEARCH_NANOS = {new LongAdder(), new LongAdder()};
    private static final AtomicLong[] SEARCH_MAX = {new AtomicLong(), new AtomicLong()};
    private static final LongAdder[] EXPANDED = {new LongAdder(), new LongAdder()};
    private static final LongAdder CACHE_HIT = new LongAdder(), CACHE_REVERSE = new LongAdder();
    private static final LongAdder CACHE_MISS = new LongAdder(), CACHE_STALE = new LongAdder();
    private static final LongAdder REVERSE_TRIED = new LongAdder(), REVERSE_FOUND = new LongAdder();
    private static final LongAdder ROUTES_STORED = new LongAdder();
    private static final LongAdder SNAPSHOT_REUSED = new LongAdder();
    private static final LongAdder NO_PATH_COOLDOWN = new LongAdder();
    private static final LongAdder NO_PATH_FAILURES = new LongAdder();
    private static final LongAdder NO_PATH_BACKOFF_LEVEL2 = new LongAdder();
    private static final LongAdder NO_PATH_BACKOFF_LEVEL3 = new LongAdder();
    // Seven diagnostic-only transitions. Enum ordinals are private to this
    // process, never persisted as part of a save or network packet.
    private static final LongAdder[] NO_PATH_BACKOFF_EVENTS =
            new LongAdder[FailedRouteBackoff.Event.values().length];
    static {
        for (int i = 0; i < NO_PATH_BACKOFF_EVENTS.length; i++)
            NO_PATH_BACKOFF_EVENTS[i] = new LongAdder();
    }
    private static final LongAdder CACHE_REPLAN_DEFERRED = new LongAdder();
    private static final LongAdder INFLIGHT_JOINED = new LongAdder();
    private static final LongAdder LADDER_SCANS = new LongAdder();
    private static final LongAdder LADDER_PROBES = new LongAdder();
    private static final LongAdder LADDER_HITS = new LongAdder();
    private static final LongAdder LADDER_NANOS = new LongAdder();
    private static final LongAdder SNAPSHOT_AREA_REJECTED = new LongAdder();
    private static final LongAdder SNAPSHOT_TICK_BUDGET = new LongAdder();
    private static final LongAdder SNAPSHOT_SECTION_LIMIT = new LongAdder();
    private static final LongAdder SNAPSHOT_SHARED_SECTIONS = new LongAdder();
    private static final LongAdder SNAPSHOT_ADDITIONAL_CAPTURES = new LongAdder();
    private static final LongAdder SNAPSHOT_NON_LATEST_REUSED = new LongAdder();
    private static final LongAdder SNAPSHOT_KNOWN_SECTIONS_INVALIDATED = new LongAdder();
    private static final LongAdder SNAPSHOT_KNOWN_CAPTURES_INVALIDATED = new LongAdder();
    private static final LongAdder SNAPSHOT_POISONED_BYPASSES = new LongAdder();
    private static final LongAdder SNAPSHOT_MUTATION_RACE_REJECTED = new LongAdder();
    private static final LongAdder FLAT_COLUMN_MEMO_HIT = new LongAdder();
    private static final LongAdder FLAT_COLUMN_MEMO_MISS = new LongAdder();
    private static final LongAdder SWEPT_TRANSITION_COUNT = new LongAdder();
    private static final LongAdder SWEPT_TRANSITION_BLOCKED = new LongAdder();
    private static final LongAdder SWEPT_TRANSITION_NANOS = new LongAdder();
    private static final AtomicLong SWEPT_TRANSITION_MAX = new AtomicLong();
    private static final NavigationRateWindow RATE_WINDOW = new NavigationRateWindow();
    private NavigationMetrics() {}
    public static void record(int kind,long nanos){COUNT[kind].increment();NANOS[kind].add(nanos);MAX[kind].accumulateAndGet(nanos,Math::max);}
    public static void sections(int count){SECTIONS.add(count);}
    public static void deferred(){DEFERRED.increment();}
    /** Too large requests must not claim the single-tick snapshot budget. */
    public static void snapshotAreaRejected(){SNAPSHOT_AREA_REJECTED.increment();}
    /** A snapshot was already captured, or reserved, for this world and tick. */
    public static void snapshotBudgetDeferred(){
        SNAPSHOT_TICK_BUDGET.increment();
        DEFERRED.increment();
    }
    public static void snapshotSectionRejected(){SNAPSHOT_SECTION_LIMIT.increment();}
    public static void snapshotSectionsReused(int amount) { SNAPSHOT_SHARED_SECTIONS.add(Math.max(0, amount)); }
    public static void snapshotAdditionalCaptured() { SNAPSHOT_ADDITIONAL_CAPTURES.increment(); }
    public static void snapshotNonLatestReused() { SNAPSHOT_NON_LATEST_REUSED.increment(); }
    public static void snapshotKnownSectionInvalidated() { SNAPSHOT_KNOWN_SECTIONS_INVALIDATED.increment(); }
    public static void snapshotKnownSectionsInvalidated(int count) {
        SNAPSHOT_KNOWN_SECTIONS_INVALIDATED.add(Math.max(0, count));
    }
    public static void snapshotKnownCaptureInvalidated() { SNAPSHOT_KNOWN_CAPTURES_INVALIDATED.increment(); }
    public static void snapshotKnownCapturesInvalidated(int count) {
        SNAPSHOT_KNOWN_CAPTURES_INVALIDATED.add(Math.max(0, count));
    }
    public static void snapshotPoisonedBypass() { SNAPSHOT_POISONED_BYPASSES.increment(); }
    public static void snapshotMutationRaceRejected() { SNAPSHOT_MUTATION_RACE_REJECTED.increment(); }
    public static void flatColumnMemoHit() { FLAT_COLUMN_MEMO_HIT.increment(); }
    public static void flatColumnMemoMiss() { FLAT_COLUMN_MEMO_MISS.increment(); }
    public static void sweptTransition(long nanos, boolean traversable) {
        long duration = Math.max(0L, nanos);
        SWEPT_TRANSITION_COUNT.increment();
        if (!traversable) SWEPT_TRANSITION_BLOCKED.increment();
        SWEPT_TRANSITION_NANOS.add(duration);
        SWEPT_TRANSITION_MAX.accumulateAndGet(duration, Math::max);
    }
    public static void stale(){STALE.increment();}
    /** kind: 0=JPS, 1=classic A*; includes search and validation time. */
    public static void search(int kind, long nanos, int expanded) {
        SEARCH_COUNT[kind].increment();
        SEARCH_NANOS[kind].add(Math.max(0L, nanos));
        SEARCH_MAX[kind].accumulateAndGet(Math.max(0L, nanos), Math::max);
        EXPANDED[kind].add(Math.max(0, expanded));
    }
    public static void cacheHit(boolean reverse) {
        CACHE_HIT.increment();
        if (reverse) CACHE_REVERSE.increment();
    }
    public static void cacheMiss() { CACHE_MISS.increment(); }
    public static void cacheStale() { CACHE_STALE.increment(); }
    public static void routeStored() { ROUTES_STORED.increment(); }
    public static void snapshotReused() { SNAPSHOT_REUSED.increment(); }
    public static void noPathCooldown() { NO_PATH_COOLDOWN.increment(); }
    public static void noPathFailure(int level) {
        NO_PATH_FAILURES.increment();
        if (level == 2) NO_PATH_BACKOFF_LEVEL2.increment();
        if (level >= 3) NO_PATH_BACKOFF_LEVEL3.increment();
    }
    public static void noPathBackoffEvent(FailedRouteBackoff.Event event) {
        NO_PATH_BACKOFF_EVENTS[event.ordinal()].increment();
    }
    private static long noPathBackoffEventCount(FailedRouteBackoff.Event event) {
        return NO_PATH_BACKOFF_EVENTS[event.ordinal()].sum();
    }
    public static void cacheReplanDeferred() { CACHE_REPLAN_DEFERRED.increment(); }
    public static void inflightJoined() { INFLIGHT_JOINED.increment(); }
    public static void ladderScan(int probes, boolean found, long nanos) {
        LADDER_SCANS.increment();
        LADDER_PROBES.add(Math.max(0, probes));
        if (found) LADDER_HITS.increment();
        LADDER_NANOS.add(Math.max(0L, nanos));
    }
    public static void reverseTried() { REVERSE_TRIED.increment(); }
    public static void reverseFound() { REVERSE_FOUND.increment(); }
    public static String report(){
        StringBuilder text=new StringBuilder();
        for(int i=0;i<2;i++){long n=COUNT[i].sum();text.append(i==0?"native":"snapshot").append(String.format(java.util.Locale.ROOT," count=%d avg=%.3fms max=%.3fms; ",n,n==0?0:NANOS[i].sum()/1e6/n,MAX[i].get()/1e6));}
        long[] queue=org.millenaire.common.pathing.atomicstryker.PathSearchExecutor.metrics();
        for(int i=0;i<2;i++){
            long n=SEARCH_COUNT[i].sum();
            text.append(i==0?"jps":"astar")
                .append(String.format(java.util.Locale.ROOT,
                    " searches=%d avg=%.3fms max=%.3fms expanded=%d; ",
                    n,n==0?0:SEARCH_NANOS[i].sum()/1e6/n,SEARCH_MAX[i].get()/1e6,
                    EXPANDED[i].sum()));
        }
        return text+"copied_sections="+SECTIONS.sum()+" deferred="+DEFERRED.sum()
            +" stale_routes="+STALE.sum()+" snapshot_reused="+SNAPSHOT_REUSED.sum()
            +" snapshot_area_rejected="+SNAPSHOT_AREA_REJECTED.sum()
            +" snapshot_tick_budget_deferred="+SNAPSHOT_TICK_BUDGET.sum()
            +" snapshot_section_rejected="+SNAPSHOT_SECTION_LIMIT.sum()
            +" snapshot_shared_sections="+SNAPSHOT_SHARED_SECTIONS.sum()
            +" snapshot_additional_captures="+SNAPSHOT_ADDITIONAL_CAPTURES.sum()
            +" snapshot_non_latest_reused="+SNAPSHOT_NON_LATEST_REUSED.sum()
            +" snapshot_known_sections_invalidated="+SNAPSHOT_KNOWN_SECTIONS_INVALIDATED.sum()
            +" snapshot_known_captures_invalidated="+SNAPSHOT_KNOWN_CAPTURES_INVALIDATED.sum()
            +" snapshot_poisoned_bypasses="+SNAPSHOT_POISONED_BYPASSES.sum()
            +" snapshot_mutation_race_rejected="+SNAPSHOT_MUTATION_RACE_REJECTED.sum()
            +" flat_column_memo_hit="+FLAT_COLUMN_MEMO_HIT.sum()
            +" flat_column_memo_miss="+FLAT_COLUMN_MEMO_MISS.sum()
            +" swept_checks="+SWEPT_TRANSITION_COUNT.sum()
            +" swept_blocked="+SWEPT_TRANSITION_BLOCKED.sum()
            +String.format(java.util.Locale.ROOT,
                    " swept_avg_ms=%.3f swept_max_ms=%.3f",
                    SWEPT_TRANSITION_COUNT.sum()==0 ? 0.0D
                            : SWEPT_TRANSITION_NANOS.sum()/1.0E6D/SWEPT_TRANSITION_COUNT.sum(),
                    SWEPT_TRANSITION_MAX.get()/1.0E6D)
            +String.format(java.util.Locale.ROOT, " snapshot_section_reuse_pct=%.1f",
                    SECTIONS.sum()+SNAPSHOT_SHARED_SECTIONS.sum()==0 ? 0.0D
                    : SNAPSHOT_SHARED_SECTIONS.sum()*100.0D
                        /(SECTIONS.sum()+SNAPSHOT_SHARED_SECTIONS.sum()))
            +" no_path_cooldown="+NO_PATH_COOLDOWN.sum()
            +" no_path_failures="+NO_PATH_FAILURES.sum()
            +" no_path_backoff_level2="+NO_PATH_BACKOFF_LEVEL2.sum()
            +" no_path_backoff_level3="+NO_PATH_BACKOFF_LEVEL3.sum()
            +" no_path_reset_start="+noPathBackoffEventCount(FailedRouteBackoff.Event.RESET_START_CHANGED)
            +" no_path_reset_goal="+noPathBackoffEventCount(FailedRouteBackoff.Event.RESET_GOAL_CHANGED)
            +" no_path_reset_movement="+noPathBackoffEventCount(FailedRouteBackoff.Event.RESET_MOVEMENT_CHANGED)
            +" no_path_reset_world="+noPathBackoffEventCount(FailedRouteBackoff.Event.RESET_WORLD_MUTATED)
            +" no_path_reset_rewind="+noPathBackoffEventCount(FailedRouteBackoff.Event.RESET_TICK_REWIND)
            +" no_path_reset_success="+noPathBackoffEventCount(FailedRouteBackoff.Event.RESET_SUCCESS)
            +" no_path_retry_expired="+noPathBackoffEventCount(FailedRouteBackoff.Event.RETRY_EXPIRED)
            +" cache_replan_deferred="+CACHE_REPLAN_DEFERRED.sum()
            +" inflight_joined="+INFLIGHT_JOINED.sum()
            +" cache_hit="+CACHE_HIT.sum()
            +" cache_reverse="+CACHE_REVERSE.sum()+" cache_miss="+CACHE_MISS.sum()
            +" cache_invalidated="+CACHE_STALE.sum()+" routes_stored="+ROUTES_STORED.sum()
            +" reverse_tried="+REVERSE_TRIED.sum()+" reverse_found="+REVERSE_FOUND.sum()
            +" active="+queue[2]+" queued="+queue[3]+" rejected="+queue[1]
            +" queue_started="+queue[5]
            +String.format(java.util.Locale.ROOT, " queue_wait_avg_ms=%.3f queue_wait_max_ms=%.3f",
                    queue[5]==0 ? 0.0D : queue[6]/1.0E6D/queue[5],
                    queue[7]/1.0E6D)
            +" ladder_scans="+LADDER_SCANS.sum()
            +" ladder_probes="+LADDER_PROBES.sum()
            +" ladder_hits="+LADDER_HITS.sum()
            +String.format(java.util.Locale.ROOT, " ladder_scan_avg_ms=%.3f",
                    LADDER_SCANS.sum()==0 ? 0.0D : LADDER_NANOS.sum()/1.0E6D/LADDER_SCANS.sum())
            +" "+RATE_WINDOW.sample(System.nanoTime(), new NavigationRateWindow.Counts(
                    SEARCH_COUNT[0].sum(), SEARCH_COUNT[1].sum(), COUNT[1].sum(),
                    SECTIONS.sum(), CACHE_HIT.sum(), CACHE_MISS.sum(),
                    CACHE_STALE.sum(), NO_PATH_COOLDOWN.sum(), NO_PATH_FAILURES.sum(),
                    CACHE_REPLAN_DEFERRED.sum(), INFLIGHT_JOINED.sum(),
                    queue[5], queue[6], LADDER_SCANS.sum(), LADDER_PROBES.sum()));
    }
    public static void reset(){
        for(int i=0;i<2;i++){
            COUNT[i].reset();NANOS[i].reset();MAX[i].set(0);
            SEARCH_COUNT[i].reset();SEARCH_NANOS[i].reset();
            SEARCH_MAX[i].set(0);EXPANDED[i].reset();
        }
        SECTIONS.reset();DEFERRED.reset();STALE.reset();
        CACHE_HIT.reset();CACHE_REVERSE.reset();CACHE_MISS.reset();CACHE_STALE.reset();
        ROUTES_STORED.reset();REVERSE_TRIED.reset();REVERSE_FOUND.reset();SNAPSHOT_REUSED.reset();NO_PATH_COOLDOWN.reset();NO_PATH_FAILURES.reset();NO_PATH_BACKOFF_LEVEL2.reset();NO_PATH_BACKOFF_LEVEL3.reset();CACHE_REPLAN_DEFERRED.reset();INFLIGHT_JOINED.reset();
        for (LongAdder transition : NO_PATH_BACKOFF_EVENTS) transition.reset();
        LADDER_SCANS.reset();LADDER_PROBES.reset();LADDER_HITS.reset();LADDER_NANOS.reset();
        SNAPSHOT_AREA_REJECTED.reset();SNAPSHOT_TICK_BUDGET.reset();SNAPSHOT_SECTION_LIMIT.reset();
        SNAPSHOT_SHARED_SECTIONS.reset();SNAPSHOT_ADDITIONAL_CAPTURES.reset();SNAPSHOT_NON_LATEST_REUSED.reset();
        SNAPSHOT_KNOWN_SECTIONS_INVALIDATED.reset();SNAPSHOT_KNOWN_CAPTURES_INVALIDATED.reset();
        SNAPSHOT_POISONED_BYPASSES.reset();SNAPSHOT_MUTATION_RACE_REJECTED.reset();
        FLAT_COLUMN_MEMO_HIT.reset();FLAT_COLUMN_MEMO_MISS.reset();
        SWEPT_TRANSITION_COUNT.reset();SWEPT_TRANSITION_BLOCKED.reset();
        SWEPT_TRANSITION_NANOS.reset();SWEPT_TRANSITION_MAX.set(0L);
        RATE_WINDOW.reset();
    }
}
