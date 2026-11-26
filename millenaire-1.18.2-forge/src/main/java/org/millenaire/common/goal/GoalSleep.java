/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockBed
 *  net.minecraft.world.World
 */
package org.millenaire.common.goal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.Level;
import org.millenaire.common.config.DocumentedElement;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;

@DocumentedElement.Documentation(value="Go to sleep at home at night. This goal is auto-added to all villagers.")
public class GoalSleep
extends Goal {
    public GoalSleep() {
        this.travelBookShow = false;
        this.sprint = false;
    }

    @Override
    public int actionDuration(MillVillager villager) throws Exception {
        return 10;
    }

    @Override
    public boolean allowRandomMoves() throws Exception {
        return false;
    }

    @Override
    public boolean canBeDoneAtNight() {
        return true;
    }

    @Override
    public boolean canBeDoneInDayTime() {
        return false;
    }

    private static boolean loaded(Level world,BlockPos pos){
        return !world.isOutsideBuildHeight(pos)
                &&(!(world instanceof ServerLevel server)
                    ||server.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null);
    }

    // Both bed halves describe the same physical reservation. For floor
    // sleeping use the existing orientation convention and its adjacent cell.
    private static Set<BlockPos> footprint(Level world,Point point){
        if(point==null)return Set.of();
        BlockPos pos=point.getBlockPos();
        if(!loaded(world,pos))return Set.of();
        var state=world.getBlockState(pos);
        if(state.getBlock() instanceof BedBlock){
            Direction facing=state.getValue(BedBlock.FACING);
            BlockPos other=pos.relative(state.getValue(BedBlock.PART)==BedPart.FOOT?facing:facing.getOpposite());
            if(!loaded(world,other))return Set.of();
            var pair=world.getBlockState(other);
            if(pair.getBlock()!=state.getBlock()||pair.getValue(BedBlock.FACING)!=facing
                    ||pair.getValue(BedBlock.PART)==state.getValue(BedBlock.PART))return Set.of();
            return Set.of(pos,other);
        }
        Direction dir=null;
        // Match MillVillager.getBedOrientationInDegrees() for floor spots.
        if(loaded(world,pos.south())&&world.getBlockState(pos.south()).isAir())dir=Direction.EAST;
        else if(loaded(world,pos.west())&&world.getBlockState(pos.west()).isAir())dir=Direction.SOUTH;
        else if(loaded(world,pos.north())&&world.getBlockState(pos.north()).isAir())dir=Direction.WEST;
        else if(loaded(world,pos.east())&&world.getBlockState(pos.east()).isAir())dir=Direction.NORTH;
        if(dir==null)return Set.of();
        BlockPos other=pos.relative(dir);
        if(!loaded(world,other))return Set.of();
        return Set.of(pos,other);
    }

    private static Set<BlockPos> reservations(MillVillager villager,Point center){
        Set<MillVillager> residents=new HashSet<>(villager.getHouse().getKnownVillagers());
        residents.addAll(villager.level.getEntitiesOfClass(MillVillager.class,
                new AABB(center.getBlockPos()).inflate(8,6,8)));
        Set<BlockPos> occupied=new HashSet<>();
        for(MillVillager other:residents){
            if(other==villager||!other.isAlive())continue;
            if(other.getCurrentGoal() instanceof GoalSleep||other.isVillagerSleeping()){
                Point destination=other.getGoalDestPoint();
                if(destination!=null)occupied.addAll(footprint(villager.level,destination));
                else if(other.isVillagerSleeping())occupied.addAll(footprint(villager.level,other.getPos()));
            }
        }
        return occupied;
    }

    private static boolean available(Level world,Point candidate,Set<BlockPos> occupied){
        Set<BlockPos> cells=footprint(world,candidate);
        if(cells.isEmpty())return false;
        for(BlockPos cell:cells){
            if(occupied.contains(cell))return false;
            var state=world.getBlockState(cell);
            if(state.getBlock() instanceof BedBlock&&state.getValue(BedBlock.OCCUPIED))return false;
        }
        return true;
    }

    private static boolean floorSpot(Level world,Point point){
        Set<BlockPos> cells=footprint(world,point);
        if(cells.isEmpty())return false;
        for(BlockPos cell:cells){
            if(!loaded(world,cell.below())||!loaded(world,cell.above()))return false;
            Point feet=new Point(cell.getX(),cell.getY(),cell.getZ());
            if(feet.getBelow().isBlockPassable(world)||!feet.isBlockPassable(world)||!feet.getAbove().isBlockPassable(world))return false;
            Point roof=WorldUtilities.findTopNonPassableBlock(world,cell.getX(),cell.getZ());
            if(roof==null||roof.y<=cell.getY())return false;
        }
        return true;
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws Exception {
        if(villager.getHouse()==null)return null;
        Level world=villager.level;
        Point sleepPos=villager.getHouse().getResManager().getSleepingPos();
        if(sleepPos==null)return null;
        Set<BlockPos> occupied=reservations(villager,sleepPos);
        // Each candidate once; preserve the existing preference for beds.
        Set<BlockPos> visited=new HashSet<>();
        ArrayList<Point> floors=new ArrayList<>();
        for(int xDelta=0;xDelta<6;xDelta++)for(int yDelta=0;yDelta<6;yDelta++)
            for(int zDelta=0;zDelta<6;zDelta++)for(int l=0;l<8;l++){
                Point p=sleepPos.getRelative(xDelta*(1-(l&1)*2),yDelta*(1-(l&2)),zDelta*(1-(l&4)/2));
                BlockPos pos=p.getBlockPos();
                if(!visited.add(pos)||!loaded(world,pos))continue;
                var state=world.getBlockState(pos);
                if(state.getBlock() instanceof BedBlock&&state.getValue(BedBlock.PART)==BedPart.FOOT){
                    if(available(world,p,occupied))return this.packDest(p,villager.getHouse());
                }else{
                    Point feet=p.getAbove();
                    if(loaded(world,feet.getBlockPos())&&!p.isBlockPassable(world)
                            &&!(state.getBlock() instanceof BedBlock)
                            &&available(world,feet,occupied))floors.add(feet);
                }
            }
        for(Point p:floors)if(floorSpot(world,p))return this.packDest(p,villager.getHouse());
        // No unconditional fallback to the occupied sleeping marker.
        return null;
    }

    @Override
    public String labelKeyWhileTravelling(MillVillager villager) {
        return villager != null && villager.isVillagerSleeping() ? this.key : this.key + "_travelling";
    }

    @Override
    public boolean performAction(MillVillager villager) throws Exception {
        Point destination=villager.getGoalDestPoint();
        if(villager.getHouse()==null||destination==null
                ||!available(villager.level,destination,reservations(villager,destination))){
            villager.shouldLieDown=false;
            return true;
        }
        var state=villager.level.getBlockState(destination.getBlockPos());
        if(!(state.getBlock() instanceof BedBlock)&&!floorSpot(villager.level,destination)){
            villager.shouldLieDown=false;
            return true;
        }
        if (!villager.nightActionPerformed) {
            villager.nightActionPerformed = villager.performNightAction();
        }
        villager.shouldLieDown = true;
        float angle = villager.getBedOrientationInDegrees();
        double dx = 0.5;
        double dz = 0.5;
        double fdx = 0.0;
        double fdz = 0.0;
        if (angle == 0.0f) {
            dx = 0.95;
            fdx = -10.0;
        } else if (angle == 90.0f) {
            dz = 0.95;
            fdz = -10.0;
        } else if (angle == 180.0f) {
            dx = 0.05;
            fdx = 10.0;
        } else if (angle == 270.0f) {
            dz = 0.05;
            fdz = 10.0;
        }
        float floatingHeight = villager.getBlock(villager.getGoalDestPoint()) instanceof BedBlock ? 0.7f : 0.2f;
        villager.setPos(villager.getGoalDestPoint().x + dx, villager.getGoalDestPoint().y + (double)floatingHeight, villager.getGoalDestPoint().z + dz);
        villager.facePoint(villager.getPos().getRelative(fdx, 1.0, fdz), 100.0f, 100.0f);
        return false;
    }

    @Override
    public int priority(MillVillager villager) throws Exception {
        return 50;
    }

    @Override
    public int range(MillVillager villager) {
        return 2;
    }

    @Override
    public boolean shouldVillagerLieDown() {
        // Set the lying flag only after the live occupancy check succeeds.
        return false;
    }
}

