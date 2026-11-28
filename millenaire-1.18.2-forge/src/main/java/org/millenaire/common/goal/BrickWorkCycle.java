package org.millenaire.common.goal;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;

/** One gather/refill visit per kiln per delivery trip. Server-thread work state. */
final class BrickWorkCycle {
    enum Work { GATHER, REFILL, DELIVER, NONE }
    record Task(Work work, Building kiln, Point point) {}
    private static final Task NONE=new Task(Work.NONE,null,null);
    private static final Task DELIVER=new Task(Work.DELIVER,null,null);
    private static final class Visit {
        Point current;
        boolean refilling;
        long lastWorked;
        final Set<Point> visited=new HashSet<>();
    }
    private static final WeakHashMap<MillVillager,Visit> VISITS=new WeakHashMap<>();
    private static final WeakHashMap<MillVillager,Map<Point,Long>> FAILURES=new WeakHashMap<>();
    private BrickWorkCycle() {}

    static int raw(MillVillager v){return v.countInv(MillBlocks.BS_MUD_BRICK);}

    static void gathered(MillVillager v,Building kiln,boolean newCargo){
        if(newCargo)VISITS.remove(v);
        Visit visit=VISITS.computeIfAbsent(v,k->new Visit());
        if(!kiln.getPos().equals(visit.current)){
            visit.current=kiln.getPos();
            visit.refilling=false;
        }
        visit.visited.add(kiln.getPos());
        visit.lastWorked=v.level.getGameTime();
    }

    static void beginRefill(MillVillager v,Building kiln){
        Visit visit=VISITS.get(v);
        if(visit!=null && kiln.getPos().equals(visit.current)){
            visit.refilling=true;
            visit.lastWorked=v.level.getGameTime();
        }
    }

    static void finished(MillVillager v,Building kiln){
        Visit visit=VISITS.get(v);
        if(visit!=null && kiln!=null && kiln.getPos().equals(visit.current)){
            visit.current=null;
            visit.refilling=false;
        }
    }

    static void delivered(MillVillager v){
        if(raw(v)==0)VISITS.remove(v);
    }

    static void failed(MillVillager v,Building kiln){
        if(kiln==null)return;
        long now=v.level.getGameTime();
        Map<Point,Long> blocked=FAILURES.computeIfAbsent(v,k->new HashMap<>());
        blocked.entrySet().removeIf(entry->entry.getValue()<=now);
        blocked.put(kiln.getPos(),now+200L);
        Visit visit=VISITS.get(v);
        if(visit!=null && raw(v)>0)visit.visited.add(kiln.getPos());
        finished(v,kiln);
    }

    static boolean available(MillVillager v,Building kiln){
        if(kiln==null || kiln.location==null || kiln.location.level<0
                || kiln.getPos()==null || !v.level.hasChunkAt(kiln.getPos().getBlockPos()))return false;
        long now=v.level.getGameTime();
        Map<Point,Long> blocked=FAILURES.get(v);
        if(blocked!=null && now<blocked.getOrDefault(kiln.getPos(),0L))return false;
        for(MillVillager other:v.getTownHall().getKnownVillagers()){
            if(other==v)continue;
            // Keep the harvest-to-refill handoff local to the same worker.
            // Expire the short lease if work was interrupted.
            Visit otherVisit=VISITS.get(other);
            if(otherVisit!=null && kiln.getPos().equals(otherVisit.current)
                    && now-otherVisit.lastWorked<200L && raw(other)>0)return false;
            if(other.goalKey==null || !kiln.getPos().equals(other.getGoalBuildingDestPoint()))continue;
            Goal goal=Goal.goals.get(other.goalKey);
            if(goal instanceof GoalIndianGatherBrick || goal instanceof GoalIndianDryBrick)return false;
        }
        return true;
    }

    static Point nearestSpot(MillVillager v,Building kiln,boolean ready){
        Point best=null;
        for(Point p:kiln.getResManager().getBrickWorkSpots()){
            if(p==null || !v.level.hasChunkAt(p.getBlockPos()))continue;
            if(!kiln.getResManager().canWorkBrickSpot(p,ready))continue;
            if(best==null || p.horizontalDistanceToSquared(v)<best.horizontalDistanceToSquared(v))best=p;
        }
        return best;
    }

    private static Task completed(int cargo){
        return BrickProductionPolicy.shouldDeliverRaw(cargo,false)?DELIVER:NONE;
    }

    /** Read-only planning; state changes only after successful work or a route failure. */
    static Task plan(MillVillager v,BrickProductionPolicy.Demand demand){
        Building th=v.getTownHall();
        if(th==null || !BrickProductionDemand.brickWorker(v))return NONE;
        int cargo=raw(v);
        Visit visit=cargo>0?VISITS.get(v):null;
        var kilns=th.getBuildingsWithTag("brickkiln");
        InvItem item=InvItem.createInvItem(MillBlocks.BS_MUD_BRICK);
        boolean stockAllowed=th.storageRoom(item)>0
                && th.nbGoodAvailable(MillBlocks.BS_MUD_BRICK,false,false,false)<=4096;
        // Finish a started visit even when the harvested cargo covers demand.
        // Stock limits still gate gathering; an already-started refill does not
        // require space in the town hall because the wet bricks stay in the kiln.
        if(visit!=null && visit.current!=null){
            for(Building kiln:kilns){
                if(!visit.current.equals(kiln.getPos()) || !available(v,kiln))continue;
                Point ready=visit.refilling?null:nearestSpot(v,kiln,true);
                if(stockAllowed && ready!=null)return new Task(Work.GATHER,kiln,ready);
                Point empty=nearestSpot(v,kiln,false);
                if(empty!=null)
                    return new Task(Work.REFILL,kiln,empty);
                break;
            }
        }
        // Count distinct completed kilns in this settlement, not just ones currently
        // loaded or ready. If no further ready kiln is reachable, deliver the remainder.
        int completedKilns=(int)kilns.stream()
                .filter(kiln->kiln.location!=null && kiln.location.level>=0 && kiln.getPos()!=null)
                .map(Building::getPos).distinct().count();
        if(!stockAllowed || !BrickProductionPolicy.mayStartAnotherKiln(cargo,demand.shouldGather(),completedKilns))
            return completed(cargo);
        Task best=NONE;
        for(Building kiln:kilns){
            if((visit!=null && visit.visited.contains(kiln.getPos())) || !available(v,kiln))continue;
            Point ready=nearestSpot(v,kiln,true);
            if(ready==null)continue;
            if(best.point()==null || ready.horizontalDistanceToSquared(v)<best.point().horizontalDistanceToSquared(v))
                best=new Task(Work.GATHER,kiln,ready);
        }
        // Never wait for new drying ticks while carrying a completed batch.
        return best.work()==Work.GATHER?best:(completed(cargo));
    }
}
