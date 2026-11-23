/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.entity.Entity
 *  net.minecraft.init.Blocks
 *  net.minecraft.init.Items
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.EnumHand
 */
package org.millenaire.common.goal;

import java.util.ArrayList;
import org.millenaire.common.block.WoodVariant;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Registry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
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
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;
import org.millenaire.common.village.Building;

@DocumentedElement.Documentation(value="Go chop trees in a grove.")
public class GoalLumbermanChopTrees
extends Goal {
    public GoalLumbermanChopTrees() {
        this.maxSimultaneousInBuilding = 1;
        this.townhallLimit.put(InvItem.createInvItem(Blocks.OAK_LOG, -1), 4096);
        this.icon = InvItem.createInvItem(Items.IRON_AXE);
    }

    @Override
    public int actionDuration(MillVillager villager) {
        int toolEfficiency = (int)villager.getBestAxe().getDestroySpeed(new ItemStack((Item)villager.getBestAxe(), 1), Blocks.OAK_LOG.defaultBlockState());
        return Math.max(1, 20 - toolEfficiency * 2);
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) {
        ArrayList<Point> woodPos = new ArrayList<Point>();
        ArrayList<Point> buildingp = new ArrayList<Point>();
        for (Building grove : villager.getTownHall().getBuildingsWithTag("grove")) {
            Point p;
            // A damaged or partially harvested acacia may have fewer than five
            // remaining logs. Leaving it unselected forever prevents the grove
            // from recovering, even though a valid log is still present.
            if ((p = grove.getWoodLocation()) == null) continue;
            woodPos.add(p);
            buildingp.add(grove.getPos());
            if (MillConfigValues.LogLumberman < 3) continue;
            MillLog.debug(this, "Found location in grove: " + p + ". Targeted block: " + p.getBlock(villager.level));
        }
        if (woodPos.isEmpty()) {
            return null;
        }
        Point p = (Point)woodPos.get(0);
        Point buildingP = (Point)buildingp.get(0);
        for (int i = 1; i < woodPos.size(); ++i) {
            if (!(((Point)woodPos.get(i)).horizontalDistanceToSquared((Entity)villager) < p.horizontalDistanceToSquared((Entity)villager))) continue;
            p = (Point)woodPos.get(i);
            buildingP = (Point)buildingp.get(i);
        }
        if (MillConfigValues.LogLumberman >= 3) {
            MillLog.debug(this, "Going to gather wood around: " + p + ". Targeted block: " + p.getBlock(villager.level));
        }
        return this.packDest(p, buildingP);
    }

    @Override
    public ItemStack[] getHeldItemsTravelling(MillVillager villager) {
        return villager.getBestAxeStack();
    }

    @Override
    public AStarConfig getPathingConfig(MillVillager villager) {
        if (!villager.canVillagerClearLeaves()) {
            return JPS_CONFIG_CHOPLUMBER_NO_LEAVES;
        }
        return JPS_CONFIG_CHOPLUMBER;
    }

    @Override
    public boolean isPossibleSpecific(MillVillager villager) {
        if (villager.countInv(Blocks.OAK_LOG, -1) > 64) {
            return false;
        }
        return this.getDestination(villager) != null;
    }

    @Override
    public boolean lookAtGoal() {
        return true;
    }

    @Override
    public boolean performAction(MillVillager villager) throws Exception {
        if (villager.getGoalDestPoint() == null) return true;
        boolean woodFound = false;
        if (MillConfigValues.LogLumberman >= 3) {
            MillLog.debug(this, "Attempting to gather wood around: " + villager.getGoalDestPoint() + ", central block: " + villager.getGoalDestPoint().getBlock(villager.level));
        }
        for (int deltaY = 12; deltaY > -12; --deltaY) {
            for (int deltaX = -3; deltaX < 4; ++deltaX) {
                for (int deltaZ = -3; deltaZ < 4; ++deltaZ) {
                    Point p = villager.getGoalDestPoint().getRelative(deltaX, deltaY, deltaZ);
                    BlockState state = p.getBlockActualState(villager.level);
                    Block block = state.getBlock();
                    WoodVariant wood = WoodVariant.fromState(state);
                    if (wood == null) continue;
                    boolean isLog = block == wood.log() || Registry.BLOCK.getKey(block).equals(new net.minecraft.resources.ResourceLocation("minecraft", wood.getSerializedName() + "_wood"));
                    boolean isLeaves = block == wood.leaves();
                    if (!isLog && !isLeaves) continue;
                    if (!woodFound) {
                        if (isLog) {
                            villager.setBlock(p, Blocks.AIR);
                            villager.swing(InteractionHand.MAIN_HAND);
                            // Axis/bark is placement state; harvested stock remains the species' ordinary log.
                            villager.addToInv(wood.log(), 0, 1);
                            woodFound = true;
                            if (MillConfigValues.LogLumberman >= 3) MillLog.debug(this, "Gathered wood at: " + p);
                            continue;
                        }
                        // Preserve the original grove sapling chances: dark oak 1/2, other species 1/4.
                        if (MillCommonUtilities.randomInt(wood == WoodVariant.DARK_OAK ? 2 : 4) == 0)
                            villager.addToInv(wood.sapling(), 0, 1);
                        villager.setBlock(p, Blocks.AIR);
                        villager.swing(InteractionHand.MAIN_HAND);
                        if (MillConfigValues.LogLumberman >= 3) MillLog.debug(this, "Destroyed leaves at: " + p);
                        continue;
                    }
                    if (MillConfigValues.LogLumberman >= 3) {
                        MillLog.debug(this, "More wood found.");
                    }
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int priority(MillVillager villager) {
        return Math.max(10, 125 - villager.countInv(Blocks.OAK_LOG, -1));
    }

    @Override
    public int range(MillVillager villager) {
        return 8;
    }
}

