/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockStone
 *  net.minecraft.block.BlockStone$EnumType
 *  net.minecraft.block.properties.IProperty
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.init.Blocks
 *  net.minecraft.init.Items
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 */
package org.millenaire.common.goal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.level.block.Block;
import org.millenaire.common.utilities.LegacyBlockStates;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.config.DocumentedElement;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.item.MillItems;
import org.millenaire.common.pathing.atomicstryker.AStarConfig;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;
import org.millenaire.common.village.Building;

@DocumentedElement.Documentation(value="Go and mine rocks etc at the villager's house.")
public class GoalMinerMineResource
extends Goal {
    private static final ItemStack[] IS_ULU = new ItemStack[]{new ItemStack((Item)MillItems.ULU, 1)};
    public String buildingTag = null;

    public GoalMinerMineResource() {
        this.icon = InvItem.createInvItem(Items.IRON_PICKAXE);
    }

    @Override
    public int actionDuration(MillVillager villager) {
        Block block = villager.getBlock(villager.getGoalDestPoint());
        if (LegacyBlockStates.matchesName(block, "minecraft:stone") || LegacyBlockStates.matchesName(block, "minecraft:sandstone")) {
            int toolEfficiency = (int)villager.getBestPickaxe().getDestroySpeed(new ItemStack((Item)villager.getBestPickaxe(), 1), Blocks.SANDSTONE.defaultBlockState());
            return 140 - 4 * toolEfficiency;
        }
        if (LegacyBlockStates.matchesName(block, "minecraft:sand") || block == Blocks.CLAY || block == Blocks.GRAVEL) {
            int toolEfficiency = (int)villager.getBestShovel().getDestroySpeed(new ItemStack((Item)villager.getBestShovel(), 1), Blocks.SAND.defaultBlockState());
            return 140 - 4 * toolEfficiency;
        }
        return 70;
    }

    public List<Building> getBuildings(MillVillager villager) {
        ArrayList<Building> buildings = new ArrayList<Building>();
        if (this.buildingTag == null) {
            if (villager.getHouse() != null) buildings.add(villager.getHouse());
        } else if (villager.getTownHall() != null) {
            for (Building b : villager.getTownHall().getBuildingsWithTag(this.buildingTag)) {
                buildings.add(b);
            }
        }
        return buildings;
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws Exception {
        List<Building> buildings = this.getBuildings(villager);
        ArrayList validSources = new ArrayList();
        ArrayList<Building> validDests = new ArrayList<Building>();
        for (Building possibleDest : buildings) {
            CopyOnWriteArrayList<CopyOnWriteArrayList<Point>> sources = possibleDest.getResManager().sources;
            for (int i = 0; i < Math.min(sources.size(), possibleDest.getResManager().sourceTypes.size()); ++i) {
                BlockState sourceBlockState = possibleDest.getResManager().sourceTypes.get(i);
                for (int j = 0; j < ((CopyOnWriteArrayList)sources.get(i)).size(); ++j) {
                    Point point = sources.get(i).get(j);
                    if (point == null || !villager.level.hasChunkAt(point.getBlockPos())) continue;
                    BlockState actualBlockState = WorldUtilities.getBlockState(villager.level, point);
                    if (actualBlockState != sourceBlockState || !supported(actualBlockState.getBlock())) continue;
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
        Block targetBlock = villager.getBlock(villager.getGoalDestPoint());
        if (targetBlock == Blocks.SAND || targetBlock == Blocks.CLAY || targetBlock == Blocks.GRAVEL) {
            return villager.getBestShovelStack();
        }
        if (targetBlock == Blocks.SNOW || targetBlock == Blocks.ICE) {
            return IS_ULU;
        }
        return villager.getBestPickaxeStack();
    }

    @Override
    public AStarConfig getPathingConfig(MillVillager villager) {
        if (!villager.canVillagerClearLeaves()) {
            return JPS_CONFIG_WIDE_NO_LEAVES;
        }
        return JPS_CONFIG_WIDE;
    }

    @Override
    public boolean isPossibleSpecific(MillVillager villager) throws Exception {
        return this.getDestination(villager) != null;
    }

    @Override
    public boolean lookAtGoal() {
        return true;
    }

    private boolean supported(Block block) {
        return LegacyBlockStates.matchesName(block, "minecraft:sand")
                || LegacyBlockStates.matchesName(block, "minecraft:stone")
                || LegacyBlockStates.matchesName(block, "minecraft:sandstone")
                || block == Blocks.CLAY || block == Blocks.GRAVEL || block == Blocks.SNOW || block == Blocks.ICE;
    }

    private boolean validTarget(MillVillager villager, Building dest, Point point) {
        if (dest == null || point == null || !getBuildings(villager).contains(dest)
                || !villager.level.hasChunkAt(point.getBlockPos())) return false;
        BlockState actual = WorldUtilities.getBlockState(villager.level,point);
        if (!supported(actual.getBlock())) return false;
        var manager = dest.getResManager();
        for (int i=0; i<Math.min(manager.sources.size(),manager.sourceTypes.size()); i++)
            if (manager.sourceTypes.get(i)==actual && manager.sources.get(i).contains(point)) return true;
        return false;
    }

    @Override
    public boolean isWithinActionRange(MillVillager villager, Point ignored) {
        return WorkAccess.canAccessResource(villager, villager.getGoalDestPoint(), this.range(villager));
    }

    @Override
    protected boolean isStillValidSpecific(MillVillager villager) throws Exception {
        return validTarget(villager, villager.getGoalBuildingDest(), villager.getGoalDestPoint());
    }

    @Override
    public boolean performAction(MillVillager villager) throws Exception {
        if (villager.level.isClientSide || !validTarget(villager,villager.getGoalBuildingDest(),villager.getGoalDestPoint())) return true;
        if (!this.isWithinActionRange(villager,villager.getGoalDestPoint())) return false;
        BlockState blockState = WorldUtilities.getBlockState(villager.level, villager.getGoalDestPoint());
        Block block = blockState.getBlock();
        if (LegacyBlockStates.matchesName(block, "minecraft:sand")) {
            villager.addToInv((Block)Blocks.SAND, org.millenaire.common.config.TestTuning.resourceAmount(villager.level, 1));
            WorldUtilities.playSoundBlockBreaking(villager.level, villager.getGoalDestPoint(), (Block)Blocks.SAND, 1.0f);
            if (MillConfigValues.LogMiner >= 3 && villager.extraLog) {
                MillLog.debug(this, "Gathered sand at: " + villager.getGoalDestPoint());
            }
        } else if (LegacyBlockStates.matchesName(block, "minecraft:stone")) {
            if (block == Blocks.STONE) {
                villager.addToInv(Blocks.COBBLESTONE, org.millenaire.common.config.TestTuning.resourceAmount(villager.level, 1));
            } else {
                villager.addToInv(blockState, org.millenaire.common.config.TestTuning.resourceAmount(villager.level, 1));
            }
            WorldUtilities.playSoundBlockBreaking(villager.level, villager.getGoalDestPoint(), Blocks.STONE, 1.0f);
            if (MillConfigValues.LogMiner >= 3 && villager.extraLog) {
                MillLog.debug(this, "Gather cobblestone at: " + villager.getGoalDestPoint());
            }
        } else if (LegacyBlockStates.matchesName(block, "minecraft:sandstone")) {
            villager.addToInv(Blocks.SANDSTONE, org.millenaire.common.config.TestTuning.resourceAmount(villager.level, 1));
            WorldUtilities.playSoundBlockBreaking(villager.level, villager.getGoalDestPoint(), Blocks.SANDSTONE, 1.0f);
            if (MillConfigValues.LogMiner >= 3 && villager.extraLog) {
                MillLog.debug(this, "Gather sand stone at: " + villager.getGoalDestPoint());
            }
        } else if (block == Blocks.CLAY) {
            villager.addToInv(Items.CLAY_BALL, org.millenaire.common.config.TestTuning.resourceAmount(villager.level, 1));
            WorldUtilities.playSoundBlockBreaking(villager.level, villager.getGoalDestPoint(), Blocks.CLAY, 1.0f);
            if (MillConfigValues.LogMiner >= 3 && villager.extraLog) {
                MillLog.debug(this, "Gather clay at: " + villager.getGoalDestPoint());
            }
        } else if (block == Blocks.GRAVEL) {
            villager.addToInv(Blocks.GRAVEL, org.millenaire.common.config.TestTuning.resourceAmount(villager.level, 1));
            WorldUtilities.playSoundBlockBreaking(villager.level, villager.getGoalDestPoint(), Blocks.GRAVEL, 1.0f);
            if (MillConfigValues.LogMiner >= 3 && villager.extraLog) {
                MillLog.debug(this, "Gather gravel at: " + villager.getGoalDestPoint());
            }
        } else if (block == Blocks.SNOW) {
            villager.addToInv((Block)MillBlocks.SNOW_BRICK, org.millenaire.common.config.TestTuning.resourceAmount(villager.level, 1));
            WorldUtilities.playSoundBlockBreaking(villager.level, villager.getGoalDestPoint(), Blocks.SNOW_BLOCK, 1.0f);
            if (MillConfigValues.LogMiner >= 3) {
                MillLog.debug(this, "Gather snow at: " + villager.getGoalDestPoint());
            }
        } else if (block == Blocks.ICE) {
            villager.addToInv(MillBlocks.ICE_BRICK, org.millenaire.common.config.TestTuning.resourceAmount(villager.level, 1));
            WorldUtilities.playSoundBlockBreaking(villager.level, villager.getGoalDestPoint(), Blocks.ICE, 1.0f);
            if (MillConfigValues.LogMiner >= 3) {
                MillLog.debug(this, "Gather ice at: " + villager.getGoalDestPoint());
            }
        }
        return true;
    }

    @Override
    public int priority(MillVillager villager) throws Exception {
        return 30;
    }

    @Override
    public int range(MillVillager villager) {
        return 5;
    }

    @Override
    public boolean stuckAction(MillVillager villager) throws Exception {
        return false;
    }

    @Override
    public boolean swingArms() {
        return true;
    }
}

