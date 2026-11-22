/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.init.Items
 *  net.minecraft.item.EnumDyeColor
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.EnumHand
 */
package org.millenaire.common.goal;

import java.util.ArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import org.millenaire.common.block.BlockSnailSoil;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.config.DocumentedElement;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;
import org.millenaire.common.village.Building;

@DocumentedElement.Documentation(value="Gather snails from a snail soil block to make purple dye.")
public class GoalByzantineGatherSnails
extends Goal {

    public GoalByzantineGatherSnails() {
        this.maxSimultaneousInBuilding = 2;
        this.buildingLimit.put(InvItem.createInvItem(Items.PURPLE_DYE, 0), 128);
        this.townhallLimit.put(InvItem.createInvItem(Items.PURPLE_DYE, 0), 128);
        this.icon = InvItem.createInvItem(Items.PURPLE_DYE, 0);
    }

    @Override
    public int actionDuration(MillVillager villager) {
        return 20;
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) {
        ArrayList<Point> vp = new ArrayList<Point>();
        ArrayList<Point> buildingp = new ArrayList<Point>();
        for (Building snailFamr : villager.getTownHall().getBuildingsWithTag("snailsfarm")) {
            Point p = snailFamr.getResManager().getSnailSoilHarvestLocation();
            if (p == null) continue;
            vp.add(p);
            buildingp.add(snailFamr.getPos());
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
    public ItemStack[] getHeldItemsOffHandDestination(MillVillager villager) {
        return new ItemStack[]{new ItemStack(Items.PURPLE_DYE)};
    }

    @Override
    public ItemStack[] getHeldItemsTravelling(MillVillager villager) {
        return villager.getBestShovelStack();
    }

    @Override
    public boolean isPossibleSpecific(MillVillager villager) {
        boolean delayOver = !villager.lastGoalTime.containsKey(this) ? true : villager.level.getGameTime() > villager.lastGoalTime.get(this) + 2000L;
        for (Building kiln : villager.getTownHall().getBuildingsWithTag("snailsfarm")) {
            int nb = kiln.getResManager().getNbSnailSoilHarvestLocation();
            if (nb > 0 && delayOver) {
                return true;
            }
            if (nb <= 4) continue;
            return true;
        }
        return false;
    }

    @Override
    public boolean lookAtGoal() {
        return true;
    }

    @Override
    public boolean performAction(MillVillager villager) {
        Point destination = villager.getGoalDestPoint();
        if (destination == null) return true;
        BlockState soil = WorldUtilities.getBlockState(villager.level, destination);
        if (soil.is(MillBlocks.SNAIL_SOIL)
                && soil.getValue(BlockSnailSoil.PROGRESS) == BlockSnailSoil.EnumType.SNAIL_SOIL_FULL) {
            // Award dye only after the harvest has actually changed the soil.
            if (villager.setBlockstate(destination, soil.setValue(BlockSnailSoil.PROGRESS,
                    BlockSnailSoil.EnumType.SNAIL_SOIL_EMPTY))) {
                villager.addToInv(Items.PURPLE_DYE, 1);
                villager.swing(InteractionHand.MAIN_HAND);
            }
            return false;
        }
        return true;
    }

    @Override
    public int priority(MillVillager villager) {
        int p = 100 - villager.getTownHall().nbGoodAvailable(InvItem.createInvItem(Items.PURPLE_DYE, 0), false, false, false) * 2;
        for (MillVillager v : villager.getTownHall().getKnownVillagers()) {
            if (!this.key.equals(v.goalKey)) continue;
            p /= 2;
        }
        return p;
    }

    @Override
    public boolean unreachableDestination(MillVillager villager) {
        return false;
    }
}

