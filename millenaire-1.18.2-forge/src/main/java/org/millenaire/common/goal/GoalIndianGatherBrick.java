/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.init.Blocks
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.EnumHand
 */
package org.millenaire.common.goal;

import java.util.ArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.config.DocumentedElement;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.utilities.BlockItemUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;
import org.millenaire.common.village.Building;

@DocumentedElement.Documentation(value="Pick up dried bricks from a kiln.")
public class GoalIndianGatherBrick
extends Goal {
    private static ItemStack[] MUD_BRICK = new ItemStack[]{BlockItemUtilities.getItemStackFromBlockState(MillBlocks.BS_MUD_BRICK, 1)};

    public GoalIndianGatherBrick() {
        this.maxSimultaneousInBuilding = 1;
        this.townhallLimit.put(InvItem.createInvItem(MillBlocks.BS_MUD_BRICK), 4096);
        this.tags.add("tag_construction");
        this.icon = InvItem.createInvItem(MillBlocks.BS_MUD_BRICK);
    }

    @Override
    public int actionDuration(MillVillager villager) {
        return 20;
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws MillLog.MillenaireException {
        var demand=BrickProductionDemand.inspect(villager);
        var task=BrickWorkCycle.plan(villager,demand);
        if(task.work()!=BrickWorkCycle.Work.GATHER || !this.validateDest(villager,task.kiln()))return null;
        return this.packDest(task.point(),task.kiln());
    }

    @Override
    public ItemStack[] getHeldItemsOffHandDestination(MillVillager villager) {
        return MUD_BRICK;
    }

    @Override
    public ItemStack[] getHeldItemsTravelling(MillVillager villager) {
        return villager.getBestPickaxeStack();
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
                || !villager.getGoalBuildingDest().getResManager().canWorkBrickSpot(villager.getGoalDestPoint(), true)) return true;
        Building kiln=villager.getGoalBuildingDest();
        var task=BrickWorkCycle.plan(villager,BrickProductionDemand.inspect(villager));
        if(task.work()!=BrickWorkCycle.Work.GATHER || !kiln.getPos().equals(task.kiln().getPos())){
            if(task.work()!=BrickWorkCycle.Work.REFILL || !kiln.getPos().equals(task.kiln().getPos()))
                BrickWorkCycle.finished(villager,kiln);
            return true;
        }
        if (WorldUtilities.getBlockState(villager.level, villager.getGoalDestPoint()) == MillBlocks.BS_MUD_BRICK) {
            boolean newCargo=BrickWorkCycle.raw(villager)==0;
            if (!villager.setBlockAndMetadata(villager.getGoalDestPoint(), Blocks.AIR, 0)) {
                BrickWorkCycle.failed(villager,kiln);
                return true;
            }
            villager.addToInv(MillBlocks.BS_MUD_BRICK, 1);
            BrickWorkCycle.gathered(villager,kiln,newCargo);
            villager.swing(InteractionHand.MAIN_HAND);
        }
        var follow=BrickWorkCycle.plan(villager,BrickProductionDemand.inspect(villager));
        if(follow.work()==BrickWorkCycle.Work.REFILL && kiln.getPos().equals(follow.kiln().getPos()))
            return true; // hand off to forming, retaining this visit
        if(follow.work()!=BrickWorkCycle.Work.GATHER || !kiln.getPos().equals(follow.kiln().getPos())){
            BrickWorkCycle.finished(villager,kiln);
            return true;
        }
        villager.setGoalInformation(this.packDest(follow.point(),kiln));
        return false;
    }

    @Override
    public int priority(MillVillager villager) {
        var demand = BrickProductionDemand.inspect(villager);
        var task=BrickWorkCycle.plan(villager,demand);
        if(task.work()!=BrickWorkCycle.Work.GATHER)return 0;
        if(BrickWorkCycle.raw(villager)>0)return BrickProductionPolicy.REFILL_BEFORE_DELIVERY_PRIORITY;
        int p = demand.priority(true);
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

