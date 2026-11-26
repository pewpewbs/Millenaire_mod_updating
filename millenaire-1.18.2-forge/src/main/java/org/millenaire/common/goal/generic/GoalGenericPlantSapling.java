/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockPlanks$EnumType
 *  net.minecraft.block.BlockSapling
 *  net.minecraft.block.properties.IProperty
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.Entity
 *  net.minecraft.init.Blocks
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.EnumHand
 */
package org.millenaire.common.goal.generic;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.Block;
import org.millenaire.common.block.WoodVariant;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.goal.generic.GoalGeneric;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.utilities.BlockItemUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;
import org.millenaire.common.village.Building;

public class GoalGenericPlantSapling
extends GoalGeneric {
    public static final String GOAL_TYPE = "plantsapling";

    @Override
    public void applyDefaultSettings() {
        this.duration = 2;
        this.lookAtGoal = true;
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws MillLog.MillenaireException {
        List<Building> buildings = this.getBuildings(villager);
        ArrayList<Point> vp = new ArrayList<Point>();
        ArrayList<Point> buildingp = new ArrayList<Point>();
        for (Building grove : buildings) {
            Point p = grove.getResManager().getPlantingLocation();
            if (p == null) continue;
            vp.add(p);
            buildingp.add(grove.getPos());
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
    public ItemStack getIcon() {
        if (this.icon != null) {
            return this.icon.getItemStack();
        }
        return InvItem.createInvItem(Blocks.OAK_SAPLING).getItemStack();
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
        return this.getDestination(villager) != null;
    }

    @Override
    public boolean performAction(MillVillager villager) {
        if (villager.getGoalBuildingDest() == null || villager.getGoalDestPoint() == null) return true;
        Point target=villager.getGoalDestPoint();
        if(villager.level instanceof net.minecraft.server.level.ServerLevel server
                &&server.getChunkSource().getChunkNow(target.getChunkX(),target.getChunkZ())==null)return true;
        Block block = WorldUtilities.getBlock(villager.level, target);
        if (block == Blocks.AIR || block == Blocks.SNOW || BlockItemUtilities.isBlockDecorativePlant(block) && !(block instanceof SaplingBlock)) {
            String saplingType = villager.getGoalBuildingDest().getResManager().getPlantingLocationType(villager.getGoalDestPoint());
            BlockState saplingBS = Blocks.OAK_SAPLING.defaultBlockState();
            if ("pinespawn".equals(saplingType)) {
                saplingBS = WoodVariant.SPRUCE.sapling().defaultBlockState();
            } else if ("birchspawn".equals(saplingType)) {
                saplingBS = WoodVariant.BIRCH.sapling().defaultBlockState();
            } else if ("junglespawn".equals(saplingType)) {
                saplingBS = WoodVariant.JUNGLE.sapling().defaultBlockState();
            } else if ("acaciaspawn".equals(saplingType)) {
                if(!villager.getGoalBuildingDest().getResManager().canPlantAcaciaAt(target))return true;
                saplingBS = WoodVariant.ACACIA.sapling().defaultBlockState();
            } else if ("darkoakspawn".equals(saplingType)) {
                saplingBS = WoodVariant.DARK_OAK.sapling().defaultBlockState();
            } else if ("appletreespawn".equals(saplingType)) {
                saplingBS = MillBlocks.SAPLING_APPLETREE.defaultBlockState();
            } else if ("olivetreespawn".equals(saplingType)) {
                saplingBS = MillBlocks.SAPLING_OLIVETREE.defaultBlockState();
            } else if ("pistachiotreespawn".equals(saplingType)) {
                saplingBS = MillBlocks.SAPLING_PISTACHIO.defaultBlockState();
            } else if ("cherrytreespawn".equals(saplingType)) {
                saplingBS = MillBlocks.SAPLING_CHERRY.defaultBlockState();
            } else if ("sakuratreespawn".equals(saplingType)) {
                saplingBS = MillBlocks.SAPLING_SAKURA.defaultBlockState();
            }
            // Preserve the existing planting supply policy (saplings can be
            // planted without inventory); do not consume one if placement failed.
            if(!villager.setBlockstate(target,saplingBS))return true;
            villager.takeFromInv(saplingBS, 1);
            villager.swing(InteractionHand.MAIN_HAND);
            if("acaciaspawn".equals(saplingType)&&MillConfigValues.LogLumberman>=1)
                MillLog.major(this,"GROVE-ACACIA planted pos="+target);
            if (MillConfigValues.LogLumberman >= 3 && villager.extraLog) {
                MillLog.debug(this, "Planted at: " + villager.getGoalDestPoint());
            }
        } else if (MillConfigValues.LogLumberman >= 3 && villager.extraLog) {
            MillLog.debug(this, "Failed to plant at: " + villager.getGoalDestPoint());
        }
        return true;
    }

    @Override
    public boolean validateGoal() {
        return true;
    }
}

