/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.init.Blocks
 *  net.minecraft.init.Items
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.EnumHand
 */
package org.millenaire.common.goal;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import org.millenaire.common.config.DocumentedElement;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.pathing.atomicstryker.AStarConfig;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.PathUtilities;
import org.millenaire.common.utilities.Point;

@DocumentedElement.Documentation(value="Clear an old path.")
public class GoalClearOldPath
extends Goal {
    public GoalClearOldPath() {
        this.maxSimultaneousTotal = 1;
        this.tags.add("tag_construction");
        this.icon = InvItem.createInvItem(Items.IRON_SHOVEL);
    }

    @Override
    public int actionDuration(MillVillager villager) {
        int toolEfficiency = (int)villager.getBestShovel().getDestroySpeed(new ItemStack((Item)villager.getBestShovel(), 1), Blocks.DIRT.defaultBlockState());
        return 10 - toolEfficiency;
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws Exception {
        Point p = villager.getTownHall().getCurrentClearPathPoint();
        if (p != null) {
            return this.packDest(p);
        }
        return null;
    }

    @Override
    public ItemStack[] getHeldItemsTravelling(MillVillager villager) {
        return villager.getBestShovelStack();
    }

    @Override
    public AStarConfig getPathingConfig(MillVillager villager) {
        if (!villager.canVillagerClearLeaves()) {
            return JPS_CONFIG_BUILDING_NO_LEAVES;
        }
        return JPS_CONFIG_BUILDING;
    }

    @Override
    public boolean isPossibleSpecific(MillVillager villager) {
        return MillConfigValues.BuildVillagePaths && villager.getTownHall().getCurrentClearPathPoint() != null;
    }

    @Override
    public boolean isStillValidSpecific(MillVillager villager) throws Exception {
        return villager.getTownHall().getCurrentClearPathPoint() != null;
    }

    @Override
    public boolean lookAtGoal() {
        return true;
    }

    @Override
    public boolean performAction(MillVillager villager) throws Exception {
        Point p = villager.getTownHall().getCurrentClearPathPoint();
        if (p == null) {
            return true;
        }
        if (MillConfigValues.LogVillagePaths >= 3) {
            MillLog.debug(villager, "Clearing old path block: " + p);
        }
        PathUtilities.clearPathBlock(p, villager.level);
        ++villager.getTownHall().oldPathPointsToClearIndex;
        p = villager.getTownHall().getCurrentClearPathPoint();
        villager.swing(InteractionHand.MAIN_HAND);
        if (p != null) {
            villager.setGoalDestPoint(p);
            return false;
        }
        return true;
    }

    @Override
    public int priority(MillVillager villager) throws Exception {
        return 40;
    }

    @Override
    public int range(MillVillager villager) {
        return 5;
    }

    @Override
    public boolean stopMovingWhileWorking() {
        return false;
    }

    @Override
    public boolean unreachableDestination(MillVillager villager) throws Exception {
        this.performAction(villager);
        return true;
    }
}

