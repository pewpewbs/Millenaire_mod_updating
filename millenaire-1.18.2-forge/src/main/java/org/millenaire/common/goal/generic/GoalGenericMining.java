/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.init.Blocks
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 */
package org.millenaire.common.goal.generic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.millenaire.common.annotedparameters.AnnotedParameter;
import org.millenaire.common.annotedparameters.ConfigAnnotations;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.goal.generic.GoalGeneric;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.item.MillItems;
import org.millenaire.common.pathing.atomicstryker.AStarConfig;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;
import org.millenaire.common.village.Building;

public class GoalGenericMining
extends GoalGeneric {
    private static final ItemStack[] IS_ULU = new ItemStack[]{new ItemStack((Item)MillItems.ULU, 1)};
    public static final String GOAL_TYPE = "mining";
    @ConfigAnnotations.ConfigField(type=AnnotedParameter.ParameterType.BLOCKSTATE)
    @ConfigAnnotations.FieldDocumentation(explanation="Blockstate of the source, like stone (not necessarily the block being harvest - stone gives cobblestone for example).")
    public BlockState sourceBlockState = null;
    @ConfigAnnotations.ConfigField(type=AnnotedParameter.ParameterType.INVITEM_NUMBER_ADD, paramName="loot")
    @ConfigAnnotations.FieldDocumentation(explanation="Blocks or items  gained when mining.")
    public Map<InvItem, Integer> loots = new HashMap<InvItem, Integer>();

    @Override
    public int actionDuration(MillVillager villager) {
        // Generic goal durations are configured in milliseconds. The previous
        // 70-140 tick tool-speed override silently ignored duration=80000 in
        // Byzantine quarry goals, allowing much faster loot batches than the culture configuration.
        return Math.max(1, this.duration);
    }

    @Override
    public void applyDefaultSettings() {
        this.lookAtGoal = true;
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws Exception {
        List<Building> buildings = this.getBuildings(villager);
        ArrayList validSources = new ArrayList();
        ArrayList<Building> validDests = new ArrayList<Building>();
        for (Building possibleDest : buildings) {
            CopyOnWriteArrayList<CopyOnWriteArrayList<Point>> sources = possibleDest.getResManager().sources;
            for (int i = 0; i < sources.size(); ++i) {
                if (this.sourceBlockState != possibleDest.getResManager().sourceTypes.get(i)) continue;
                for (int j = 0; j < ((CopyOnWriteArrayList)sources.get(i)).size(); ++j) {
                    BlockState actualBlockState = WorldUtilities.getBlockState(villager.level, (Point)((CopyOnWriteArrayList)sources.get(i)).get(j));
                    if (actualBlockState != this.sourceBlockState) continue;
                    validSources.add(((CopyOnWriteArrayList)sources.get(i)).get(j));
                    validDests.add(possibleDest);
                }
            }
        }
        if (validSources.isEmpty()) {
            return null;
        }
        int randomTarget = MillCommonUtilities.randomInt(validSources.size());
        return this.packDest((Point)validSources.get(randomTarget), (Building)validDests.get(randomTarget));
    }

    @Override
    public ItemStack[] getHeldItemsTravelling(MillVillager villager) throws Exception {
        Block targetBlock = this.sourceBlockState.getBlock();
        if (targetBlock == Blocks.SAND || targetBlock == Blocks.CLAY || targetBlock == Blocks.GRAVEL) {
            return villager.getBestShovelStack();
        }
        if (targetBlock == Blocks.SNOW || targetBlock == Blocks.ICE) {
            return IS_ULU;
        }
        return villager.getBestPickaxeStack();
    }

    @Override
    public ItemStack getIcon() {
        if (this.icon != null) {
            return this.icon.getItemStack();
        }
        if (this.sourceBlockState != null) {
            return InvItem.createInvItem(this.sourceBlockState).getItemStack();
        }
        return null;
    }

    @Override
    public AStarConfig getPathingConfig(MillVillager villager) {
        if (!villager.canVillagerClearLeaves()) {
            return JPS_CONFIG_WIDE_NO_LEAVES;
        }
        return JPS_CONFIG_WIDE;
    }

    @Override
    public String getTypeLabel() {
        return GOAL_TYPE;
    }

    @Override
    public boolean isDestPossibleSpecific(MillVillager villager, Building b) {
        return true;
    }

    @Override
    public boolean isPossibleGenericGoal(MillVillager villager) throws Exception {
        return true;
    }

    @Override
    public boolean performAction(MillVillager villager) throws Exception {
        for (InvItem key : this.loots.keySet()) {
            villager.addToInv(key, org.millenaire.common.config.TestTuning.resourceAmount(villager.level, (int)this.loots.get(key)));
            if (MillConfigValues.LogMiner < 3 || !villager.extraLog) continue;
            MillLog.debug(this, "Gathered " + key + " at: " + villager.getGoalDestPoint());
        }
        WorldUtilities.playSoundBlockBreaking(villager.level, villager.getGoalDestPoint(), this.sourceBlockState.getBlock(), 1.0f);
        return true;
    }

    @Override
    public boolean stuckAction(MillVillager villager) throws Exception {
        return this.performAction(villager);
    }

    @Override
    public boolean swingArms() {
        return true;
    }

    @Override
    public boolean validateGoal() {
        return true;
    }
}

