package org.millenaire.common.pathing;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.AtomicLong;
public final class NavigationMetrics {
    private static final LongAdder[] COUNT = {new LongAdder(),new LongAdder()};
    private static final LongAdder[] NANOS = {new LongAdder(),new LongAdder()};
    private static final AtomicLong[] MAX = {new AtomicLong(),new AtomicLong()};
    private static final LongAdder SECTIONS = new LongAdder(), DEFERRED = new LongAdder(), STALE = new LongAdder();
    private NavigationMetrics() {}
    public static void record(int kind,long nanos){COUNT[kind].increment();NANOS[kind].add(nanos);MAX[kind].accumulateAndGet(nanos,Math::max);}
    public static void sections(int count){SECTIONS.add(count);}
    public static void deferred(){DEFERRED.increment();}
    public static void stale(){STALE.increment();}
    public static String report(){
        StringBuilder text=new StringBuilder();
        for(int i=0;i<2;i++){long n=COUNT[i].sum();text.append(i==0?"native":"snapshot").append(String.format(java.util.Locale.ROOT," count=%d avg=%.3fms max=%.3fms; ",n,n==0?0:NANOS[i].sum()/1e6/n,MAX[i].get()/1e6));}
        long[] queue=org.millenaire.common.pathing.atomicstryker.PathSearchExecutor.metrics();
        return text+"copied_sections="+SECTIONS.sum()+" deferred="+DEFERRED.sum()+" stale_routes="+STALE.sum()+" active="+queue[2]+" queued="+queue[3]+" rejected="+queue[1];
    }
    public static void reset(){for(int i=0;i<2;i++){COUNT[i].reset();NANOS[i].reset();MAX[i].set(0);}SECTIONS.reset();DEFERRED.reset();STALE.reset();}
}
