/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockSapling
 *  net.minecraft.entity.Entity
 *  net.minecraft.init.Blocks
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.EnumHand
 */
package org.millenaire.common.goal;

import java.util.ArrayList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import org.millenaire.common.config.DocumentedElement;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.pathing.atomicstryker.AStarConfig;
import org.millenaire.common.utilities.BlockItemUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;
import org.millenaire.common.village.Building;

@DocumentedElement.Documentation(value="Go plant saplings in a grove.")
public class GoalLumbermanPlantSaplings
extends Goal {
    public GoalLumbermanPlantSaplings() {
        this.maxSimultaneousInBuilding = 1;
        this.icon = InvItem.createInvItem(Blocks.field_150345_g);
    }

    @Override
    public int actionDuration(MillVillager villager) {
        return 20;
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) {
        ArrayList<Point> vp = new ArrayList<Point>();
        ArrayList<Point> buildingp = new ArrayList<Point>();
        for (Building grove : villager.getTownHall().getBuildingsWithTag("grove")) {
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
    public ItemStack[] getHeldItemsTravelling(MillVillager villager) {
        String saplingType = villager.getGoalBuildingDest().getResManager().getPlantingLocationType(villager.getGoalDestPoint());
        int meta = 0;
        if ("pinespawn".equals(saplingType)) {
            meta = 1;
        }
        if ("birchspawn".equals(saplingType)) {
            meta = 2;
        }
        if ("junglespawn".equals(saplingType)) {
            meta = 3;
        }
        if ("acaciaspawn".equals(saplingType)) {
            meta = 4;
        }
        if ("darkoakspawn".equals(saplingType)) {
            meta = 5;
        }
        return new ItemStack[]{org.millenaire.common.item.LegacyItemStacks.stack(Blocks.field_150345_g, 1, meta)};
    }

    @Override
    public AStarConfig getPathingConfig(MillVillager villager) {
        if (!villager.canVillagerClearLeaves()) {
            return JPS_CONFIG_WIDE_NO_LEAVES;
        }
        return JPS_CONFIG_WIDE;
    }

    @Override
    public boolean isPossibleSpecific(MillVillager villager) {
        for (Building grove : villager.getTownHall().getBuildingsWithTag("grove")) {
            Point p = grove.getResManager().getPlantingLocation();
            if (p == null) continue;
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
        Block block = WorldUtilities.getBlock(villager.level, villager.getGoalDestPoint());
        if (block == Blocks.AIR || block == Blocks.SNOW || BlockItemUtilities.isBlockDecorativePlant(block) && !(block instanceof SaplingBlock)) {
            String saplingType = villager.getGoalBuildingDest().getResManager().getPlantingLocationType(villager.getGoalDestPoint());
            int meta = 0;
            if ("pinespawn".equals(saplingType)) {
                meta = 1;
            }
            if ("birchspawn".equals(saplingType)) {
                meta = 2;
            }
            if ("junglespawn".equals(saplingType)) {
                meta = 3;
            }
            if ("acaciaspawn".equals(saplingType)) {
                meta = 4;
            }
            if ("darkoakspawn".equals(saplingType)) {
                meta = 5;
            }
            villager.takeFromInv(Blocks.field_150345_g, meta, 1);
            villager.setBlockAndMetadata(villager.getGoalDestPoint(), Blocks.field_150345_g, meta);
            villager.swing(InteractionHand.MAIN_HAND);
            if (MillConfigValues.LogLumberman >= 3 && villager.extraLog) {
                MillLog.debug(this, "Planted at: " + villager.getGoalDestPoint());
            }
        } else if (MillConfigValues.LogLumberman >= 3 && villager.extraLog) {
            MillLog.debug(this, "Failed to plant at: " + villager.getGoalDestPoint());
        }
        return true;
    }

    @Override
    public int priority(MillVillager villager) {
        return 120;
    }

    @Override
    public int range(MillVillager villager) {
        return 5;
    }
}

