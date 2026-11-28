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
        var demand = BrickProductionDemand.inspect(villager);
        long drying = BrickProductionDemand.inDryingArea(villager);
        if (!demand.shouldForm(drying)) {
            GoalDiagnostics.note(villager,this,"BRICK_TARGET_COVERED shortage="+demand.shortage()+" drying="+drying);
            return null;
        }
        boolean minimumBricksNotRequired = demand.urgent() || (villager.goalKey != null && villager.goalKey.equals(this.key) ? true : (!villager.lastGoalTime.containsKey(this) ? true : villager.level.getGameTime() > villager.lastGoalTime.get(this) + 2000L));
        ArrayList<Point> vp = new ArrayList<Point>();
        ArrayList<Point> buildingp = new ArrayList<Point>();
        for (Building kiln : villager.getTownHall().getBuildingsWithTag("brickkiln")) {
            Point p;
            if (!this.validateDest(villager, kiln)) continue;
            int nb = kiln.getResManager().getNbEmptyBrickLocation();
            boolean validTarget = false;
            if (nb > 0 && minimumBricksNotRequired) {
                validTarget = true;
            }
            if (nb > 4) {
                validTarget = true;
            }
            if (!validTarget || (p = kiln.getResManager().getEmptyBrickLocation()) == null) continue;
            vp.add(p);
            buildingp.add(kiln.getPos());
        }
        if (vp.isEmpty()) {
            return null;
        }
        Point p = (Point)vp.get(0);
        Point buildingP = (Point)buildingp.get(0);
        for (int i = 1; i < vp.size(); ++i) {
            if (!(((Point)vp.get(i)).horizontalDistanceToSquared((Entity)villager) < p.horizontalDistanceToSquared((Entity)villager))) continue;
            p = (Point)vp.get(i);
            buildingP = (Point)buildingp.get(i);
        }
        return this.packDest(p, buildingP);
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
        if (!BrickProductionDemand.inspect(villager).shouldForm(BrickProductionDemand.inDryingArea(villager))) return true;
        if (WorldUtilities.getBlock(villager.level, villager.getGoalDestPoint()) == Blocks.AIR) {
            villager.setBlockstate(villager.getGoalDestPoint(), MillBlocks.BS_WET_BRICK);
            villager.swing(InteractionHand.MAIN_HAND);
        }
        if (villager.getGoalBuildingDest().getResManager().getNbEmptyBrickLocation() > 0) {
            Goal.GoalInformation next = this.getDestination(villager);
            if (next == null) return true;
            villager.setGoalInformation(next);
            return false;
        }
        return true;
    }

    @Override
    public int priority(MillVillager villager) {
        var demand = BrickProductionDemand.inspect(villager);
        if (!demand.shouldForm(BrickProductionDemand.inDryingArea(villager))) return 0;
        int p = demand.priority(false);
        for (MillVillager v : villager.getTownHall().getKnownVillagers()) {
            if (v == villager || !this.key.equals(v.goalKey)) continue;
            p = Math.max(1, p / 2);
        }
        return p;
    }

    @Override
    public boolean unreachableDestination(MillVillager villager) {
        return false;
    }
}

