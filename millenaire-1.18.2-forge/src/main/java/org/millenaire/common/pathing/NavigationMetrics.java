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
    private static final LongAdder CACHE_REPLAN_DEFERRED = new LongAdder();
    private NavigationMetrics() {}
    public static void record(int kind,long nanos){COUNT[kind].increment();NANOS[kind].add(nanos);MAX[kind].accumulateAndGet(nanos,Math::max);}
    public static void sections(int count){SECTIONS.add(count);}
    public static void deferred(){DEFERRED.increment();}
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
    public static void cacheReplanDeferred() { CACHE_REPLAN_DEFERRED.increment(); }
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
            +" no_path_cooldown="+NO_PATH_COOLDOWN.sum()
            +" cache_replan_deferred="+CACHE_REPLAN_DEFERRED.sum()
            +" cache_hit="+CACHE_HIT.sum()
            +" cache_reverse="+CACHE_REVERSE.sum()+" cache_miss="+CACHE_MISS.sum()
            +" cache_invalidated="+CACHE_STALE.sum()+" routes_stored="+ROUTES_STORED.sum()
            +" reverse_tried="+REVERSE_TRIED.sum()+" reverse_found="+REVERSE_FOUND.sum()
            +" active="+queue[2]+" queued="+queue[3]+" rejected="+queue[1];
    }
    public static void reset(){
        for(int i=0;i<2;i++){
            COUNT[i].reset();NANOS[i].reset();MAX[i].set(0);
            SEARCH_COUNT[i].reset();SEARCH_NANOS[i].reset();
            SEARCH_MAX[i].set(0);EXPANDED[i].reset();
        }
        SECTIONS.reset();DEFERRED.reset();STALE.reset();
        CACHE_HIT.reset();CACHE_REVERSE.reset();CACHE_MISS.reset();CACHE_STALE.reset();
        ROUTES_STORED.reset();REVERSE_TRIED.reset();REVERSE_FOUND.reset();SNAPSHOT_REUSED.reset();NO_PATH_COOLDOWN.reset();CACHE_REPLAN_DEFERRED.reset();
    }
}
