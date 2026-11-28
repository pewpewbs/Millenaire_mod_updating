/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.init.Blocks
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.EnumHand
 */
package org.millenaire.common.goal;

import java.util.ArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.config.DocumentedElement;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.item.MillItems;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;
import org.millenaire.common.village.Building;

@DocumentedElement.Documentation(value="Go and dry bricks in a building with the kiln tag.")
public class GoalIndianDryBrick
extends Goal {
    public GoalIndianDryBrick() {
        this.maxSimultaneousInBuilding = 1;
        this.tags.add("tag_construction");
        this.icon = InvItem.createInvItem(MillBlocks.BS_WET_BRICK);
    }

    @Override
    public int actionDuration(MillVillager villager) {
        return 20;
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws MillLog.MillenaireException {
        var demand=BrickProductionDemand.inspect(villager);
        var task=BrickWorkCycle.plan(villager,demand);
        if(task.work()==BrickWorkCycle.Work.REFILL)
            return this.validateDest(villager,task.kiln())?this.packDest(task.point(),task.kiln()):null;
        // While carrying a harvest, only its own refill is allowed.
        if(BrickWorkCycle.raw(villager)>0 || task.work()==BrickWorkCycle.Work.GATHER
                || villager.getTownHall()==null)return null;
        if(!demand.shouldForm(BrickProductionDemand.inDryingArea(villager)))return null;
        boolean minimumBricksNotRequired=demand.urgent() || this.key.equals(villager.goalKey)
                || !villager.lastGoalTime.containsKey(this)
                || villager.level.getGameTime()>villager.lastGoalTime.get(this)+2000L;
        Point best=null;
        Building bestBuilding=null;
        for(Building kiln:villager.getTownHall().getBuildingsWithTag("brickkiln")){
            if(!BrickWorkCycle.available(villager,kiln) || !this.validateDest(villager,kiln))continue;
            if(!minimumBricksNotRequired && kiln.getResManager().getNbEmptyBrickLocation()<=4)continue;
            Point p=BrickWorkCycle.nearestSpot(villager,kiln,false);
            if(p!=null && (best==null || p.horizontalDistanceToSquared(villager)<best.horizontalDistanceToSquared(villager))){
                best=p;bestBuilding=kiln;
            }
        }
        return best==null?null:this.packDest(best,bestBuilding);
    }

    @Override
    public ItemStack[] getHeldItemsTravelling(MillVillager villager) {
        return new ItemStack[]{org.millenaire.common.item.LegacyItemStacks.stack((Item)MillItems.BRICK_MOULD, 1, 0)};
    }

    @Override
    public boolean isPossibleSpecific(MillVillager villager) throws MillLog.MillenaireException {
        return this.getDestination(villager) != null;
    }

    @Override
    public boolean lookAtGoal() {
        return true;
    }

    @Override
    public boolean performAction(MillVillager villager) throws MillLog.MillenaireException {
        if (villager.level.isClientSide || villager.getGoalDestPoint() == null || villager.getGoalBuildingDest() == null
                || !villager.level.hasChunkAt(villager.getGoalDestPoint().getBlockPos())
                || !villager.getGoalBuildingDest().getResManager().brickspot.contains(villager.getGoalDestPoint())) return true;
        Building kiln=villager.getGoalBuildingDest();
        var demand=BrickProductionDemand.inspect(villager);
        if (!demand.shouldForm(BrickProductionDemand.inDryingArea(villager))) {
            BrickWorkCycle.finished(villager,kiln);
            return true;
        }
        if(BrickWorkCycle.raw(villager)>0){
            var task=BrickWorkCycle.plan(villager,demand);
            if(task.work()!=BrickWorkCycle.Work.REFILL || !kiln.getPos().equals(task.kiln().getPos())){
                if(task.work()!=BrickWorkCycle.Work.GATHER || !kiln.getPos().equals(task.kiln().getPos()))
                    BrickWorkCycle.finished(villager,kiln);
                return true;
            }
            BrickWorkCycle.beginRefill(villager,kiln);
        }
        if (villager.level.getBlockState(villager.getGoalDestPoint().getBlockPos()).isAir()) {
            villager.setBlockstate(villager.getGoalDestPoint(), MillBlocks.BS_WET_BRICK);
            if (villager.level.getBlockState(villager.getGoalDestPoint().getBlockPos()).getBlock() != MillBlocks.WET_BRICK) {
                BrickWorkCycle.failed(villager,kiln);
                return true;
            }
            villager.swing(InteractionHand.MAIN_HAND);
        }
        Goal.GoalInformation next=this.getDestination(villager);
        if (next == null || !kiln.getPos().equals(next.getDestBuildingPos())) {
            BrickWorkCycle.finished(villager,kiln);
            return true;
        }
        villager.setGoalInformation(next);
        return false;
    }

    @Override
    public int priority(MillVillager villager) {
        var demand = BrickProductionDemand.inspect(villager);
        if (!demand.shouldForm(BrickProductionDemand.inDryingArea(villager))) return 0;
        if (BrickWorkCycle.plan(villager,demand).work()==BrickWorkCycle.Work.REFILL)
            return BrickProductionPolicy.REFILL_BEFORE_DELIVERY_PRIORITY;
        int p = demand.priority(false);
        for (MillVillager v : villager.getTownHall().getKnownVillagers()) {
            if (v == villager || !this.key.equals(v.goalKey)) continue;
            p = Math.max(1, p / 2);
        }
        return p;
    }

    @Override
    public boolean unreachableDestination(MillVillager villager) {
        BrickWorkCycle.failed(villager,villager.getGoalBuildingDest());
        return false;
    }
}

