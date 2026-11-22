/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockDoublePlant
 *  net.minecraft.block.BlockDoublePlant$EnumBlockHalf
 *  net.minecraft.block.properties.IProperty
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.Entity
 *  net.minecraft.init.Blocks
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.EnumHand
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.world.World
 */
package org.millenaire.common.goal.generic;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.Registry;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.millenaire.common.annotedparameters.AnnotedParameter;
import org.millenaire.common.annotedparameters.ConfigAnnotations;
import org.millenaire.common.block.BlockGrapeVine;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.goal.generic.GoalGeneric;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.utilities.BlockItemUtilities;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;

public class GoalGenericPlantCrop
extends GoalGeneric {
    public static final String GOAL_TYPE = "planting";
    @ConfigAnnotations.ConfigField(type=AnnotedParameter.ParameterType.BLOCK_ID)
    @ConfigAnnotations.FieldDocumentation(explanation="Type of plant to plant.")
    public ResourceLocation cropType = null;
    @ConfigAnnotations.ConfigField(type=AnnotedParameter.ParameterType.BLOCKSTATE_ADD)
    @ConfigAnnotations.FieldDocumentation(explanation="Blockstate to plant. If not set, defaults to cropType. If more than one set, picks one at random.")
    public List<BlockState> plantBlockState = new ArrayList<BlockState>();
    @ConfigAnnotations.ConfigField(type=AnnotedParameter.ParameterType.INVITEM)
    @ConfigAnnotations.FieldDocumentation(explanation="Seed item that gets consumed when planting.")
    public InvItem seed = null;
    @ConfigAnnotations.ConfigField(type=AnnotedParameter.ParameterType.BLOCK_ID, defaultValue="minecraft:farmland")
    @ConfigAnnotations.FieldDocumentation(explanation="Block to set below the crop.")
    public ResourceLocation soilType = null;

    public static int getCropBlockMeta(ResourceLocation cropType2) {
        return 0;
    }

    @Override
    public void applyDefaultSettings() {
        this.duration = 2;
        this.lookAtGoal = true;
        this.tags.add("tag_agriculture");
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws MillLog.MillenaireException {
        Point dest = null;
        Building destBuilding = null;
        List<Building> buildings = this.getBuildings(villager);
        for (Building buildingDest : buildings) {
            List<Point> soils;
            if (!this.isDestPossible(villager, buildingDest) || (soils = buildingDest.getResManager().getSoilPoints(this.cropType)) == null) continue;
            for (Point p : soils) {
                if (!this.isValidPlantingLocation(villager.level, p) || dest != null && !(p.distanceTo((Entity)villager) < dest.distanceTo((Entity)villager))) continue;
                dest = p.getAbove();
                destBuilding = buildingDest;
            }
        }
        if (dest == null || villager.getGoalDestPoint() == null) {
            return null;
        }
        return this.packDest(dest, destBuilding);
    }

    @Override
    public ItemStack getIcon() {
        if (this.icon != null) {
            return this.icon.getItemStack();
        }
        if (this.seed != null) {
            return this.seed.getItemStack();
        }
        if (this.heldItems != null && this.heldItems.length > 0) {
            return this.heldItems[0];
        }
        return null;
    }

    @Override
    public String getTypeLabel() {
        return GOAL_TYPE;
    }

    @Override
    public boolean isDestPossibleSpecific(MillVillager villager, Building b) {
        return this.seed == null || (long)b.countGoods(this.seed) + villager.countInv(this.seed) > 0;
    }

    @Override
    public boolean isPossibleGenericGoal(MillVillager villager) throws Exception {
        return this.getDestination(villager) != null;
    }

    private static boolean clearable(Block block) {
        return block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR
            || block == Blocks.SNOW || block == Blocks.SNOW_BLOCK || block instanceof LeavesBlock;
    }

    private boolean isValidPlantingLocation(Level world, Point p) {
        if (p == null) return false;
        Block above = p.getAbove().getBlock(world);
        Block upper = p.getAbove().getAbove().getBlock(world);
        Block soil = p.getBlock(world);
        if (soil != Blocks.GRASS_BLOCK && soil != Blocks.DIRT && soil != Blocks.FARMLAND) return false;
        if (!clearable(upper) && !(above instanceof DoublePlantBlock && upper == above)) return false;
        if (clearable(above)) return true;
        if (!BlockItemUtilities.isBlockDecorativePlant(above)) return false;
        return !Mill.CROP_FLOWER.equals(this.cropType)
            || !(above instanceof FlowerBlock || above instanceof DoublePlantBlock);
    }

    @Override
    public boolean performAction(MillVillager villager) {
        Building dest = villager.getGoalBuildingDest();
        if (dest == null || villager.getGoalDestPoint() == null) {
            return true;
        }
        if (!this.isValidPlantingLocation(villager.level, villager.getGoalDestPoint().getBelow())) {
            return true;
        }
        Block soil = Registry.BLOCK.getOptional(this.soilType)
            .orElseThrow(() -> new IllegalArgumentException("Unknown planting soil: " + this.soilType));
        BlockState cropState = this.plantBlockState.isEmpty()
            ? Registry.BLOCK.getOptional(this.cropType)
                .orElseThrow(() -> new IllegalArgumentException("Unknown crop: " + this.cropType)).defaultBlockState()
            : this.plantBlockState.get(MillCommonUtilities.randomInt(this.plantBlockState.size()));
        boolean doublePlant = cropState.getBlock() instanceof DoublePlantBlock || cropState.getBlock() instanceof BlockGrapeVine;
        if (doublePlant) cropState = cropState.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER);
        if (this.seed != null) {
            int taken = villager.takeFromInv(this.seed, 1);
            if (taken == 0) taken = dest.takeGoods(this.seed, 1);
            if (taken == 0) return true;
        }
        if (villager.getGoalDestPoint().getBelow().getBlock(villager.level) != soil)
            villager.setBlockstate(villager.getGoalDestPoint().getBelow(), soil.defaultBlockState());
        villager.setBlockstate(villager.getGoalDestPoint(), cropState);
        if (doublePlant)
            villager.setBlockstate(villager.getGoalDestPoint().getAbove(), cropState.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));

        villager.swing(InteractionHand.MAIN_HAND);
        if (this.isDestPossibleSpecific(villager, villager.getGoalBuildingDest())) {
            try {
                villager.setGoalInformation(this.getDestination(villager));
            }
            catch (MillLog.MillenaireException e) {
                MillLog.printException(e);
            }
            return false;
        }
        return true;
    }

    @Override
    public int priority(MillVillager villager) throws MillLog.MillenaireException {
        Goal.GoalInformation info = this.getDestination(villager);
        if (info == null || info.getDest() == null) {
            return -1;
        }
        return (int)(100.0 - villager.getPos().distanceTo(info.getDest()));
    }

    @Override
    public boolean validateGoal() {
        if (this.cropType == null) {
            MillLog.error(this, "The croptype is mandatory in custom planting goals.");
            return false;
        }
        return true;
    }
}


