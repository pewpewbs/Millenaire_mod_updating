package org.millenaire.common.buildingplan;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;
import org.millenaire.common.village.BuildingResManager;

/** Upgrade only the explicitly changed stock-plan resource points, never rebuild a village. */
public final class ProductionSourceRepair {
    private ProductionSourceRepair() {}
    // Coordinates are the same (floor, PNG x within floor, PNG y) as the plan regression tests.
    private record Marker(int floor, int k, int j, String type, Block block) {}
    private static Marker sand(int floor, int k, int j) {
        return new Marker(floor,k,j,"sandsource",Blocks.SAND);
    }
    private static Marker gravel(int floor, int k, int j) {
        return new Marker(floor,k,j,"gravelsource",Blocks.GRAVEL);
    }
    private static List<Marker> markers(BuildingPlan plan) {
        return switch (plan.culture.key + "/" + plan.buildingKey + "/" + plan.variation) {
            case "indian/quarry/0" -> List.of(sand(1,2,5),sand(1,6,6));
            case "indian/quarry/1" -> List.of(sand(2,3,3),sand(3,9,8));
            case "mayan/mayanmining/0" -> List.of(sand(2,4,5),sand(2,3,9));
            case "japanese/japanesequarry/0" -> List.of(sand(1,9,11),sand(1,11,16));
            case "japanese/lonemine/0" -> List.of(gravel(7,4,2),gravel(7,2,4),sand(7,4,4));
            default -> List.of();
        };
    }
    private static PointType at(BuildingPlan plan, int floor, int j, int k) {
        if (plan.plan == null || floor < 0 || floor >= plan.plan.length
                || j < 0 || j >= plan.plan[floor].length
                || k < 0 || k >= plan.plan[floor][j].length) return null;
        return plan.plan[floor][j][k];
    }
    private static boolean registered(BuildingResManager manager, BlockState type, Point point) {
        for (int i=0; i<Math.min(manager.sources.size(),manager.sourceTypes.size()); i++)
            if (manager.sourceTypes.get(i)==type && manager.sources.get(i).contains(point)) return true;
        return false;
    }
    private static void register(BuildingResManager manager, BlockState desired, Point point) {
        // Keep unrelated registrations and list indices intact.
        for (int i=0; i<Math.min(manager.sources.size(),manager.sourceTypes.size()); i++)
            if (manager.sourceTypes.get(i)==Blocks.STONE.defaultBlockState())
                manager.sources.get(i).remove(point);
        manager.addSourcePoint(desired,point);
    }

    /** Returns false only when at least one relevant chunk is not loaded yet. */
    public static boolean reconcile(Building building) {
        if (building.world.isClientSide || building.location == null
                || building.location.isCustomBuilding || building.location.level < 0) return true;
        BuildingPlan current = building.location.getPlan();
        if (current == null || current.culture == null) return true;
        List<Marker> candidates = markers(current);
        if (candidates.isEmpty()) return true;
        BuildingPlan base = current;
        while (base.level > 0) {
            base = base.getPreviousBuildingPlan();
            if (base == null) return true;
        }
        boolean complete = true;
        var manager = building.getResManager();
        for (Marker marker : candidates) {
            PointType original = at(base,marker.floor(),marker.j(),marker.k());
            if (original == null || !original.isType(marker.type())) continue;
            int localY = base.startLevel + marker.floor();
            int localX = marker.j() - base.lengthOffset;
            int localZ = marker.k() - base.widthOffset;
            boolean unchanged = true;
            // Higher-level plans may override a base marker. Do not touch those.
            for (BuildingPlan p=current; p!=null && p.level>0; p=p.getPreviousBuildingPlan()) {
                PointType later = at(p,localY-p.startLevel,localX+p.lengthOffset,localZ+p.widthOffset);
                if (later != null && !later.isType("empty") && !later.isType(marker.type())) {
                    unchanged=false; break;
                }
            }
            if (!unchanged) continue;
            Point origin=building.location.pos;
            Point point=BuildingPlan.adjustForOrientation(origin.getiX(),origin.getiY()+localY,
                    origin.getiZ(),localX,localZ,building.location.orientation);
            if (point == null) continue;
            var pos=point.getBlockPos();
            if (!building.world.hasChunkAt(pos)) { complete=false; continue; }
            BlockState old=Blocks.STONE.defaultBlockState();
            BlockState desired=marker.block().defaultBlockState();
            BlockState live=building.world.getBlockState(pos);
            boolean oldRegistered=registered(manager,old,point);
            boolean newRegistered=registered(manager,desired,point);
            boolean supported=building.world.getBlockState(pos.below())
                    .isFaceSturdy(building.world,pos.below(),Direction.UP);
            boolean clearAbove=building.world.getBlockState(pos.above()).isAir();
            var decision=ProductionSourceMigrationPolicy.decide(true,true,unchanged,
                    oldRegistered,newRegistered,live==old,live==desired,supported,clearAbove);
            if (decision==ProductionSourceMigrationPolicy.Decision.REPLACE) {
                if (!building.world.setBlockAndUpdate(pos,desired)
                        || building.world.getBlockState(pos)!=desired) continue;
            } else if (decision!=ProductionSourceMigrationPolicy.Decision.REGISTER) {
                if (oldRegistered || !newRegistered)
                    MillLog.major(building,"MILL-PRODUCTION-SOURCE-REVIEW " + point + " expected " + marker.type()
                            + "; existing block or support differs; preserved");
                continue;
            }
            register(manager,desired,point);
            building.requestSave("Stock quarry production source repair");
            MillLog.major(building,"MILL-PRODUCTION-SOURCE-REPAIRED " + point + " " + marker.type());
        }
        return complete;
    }
}
