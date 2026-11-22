/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.Entity
 *  net.minecraft.init.Blocks
 *  net.minecraft.init.Items
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.EnumHand
 *  net.minecraft.world.World
 */
package org.millenaire.common.goal;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import org.millenaire.common.buildingplan.BuildingBlock;
import org.millenaire.common.buildingplan.BuildingPlan;
import org.millenaire.common.config.DocumentedElement;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.pathing.atomicstryker.AStarConfig;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.WorldUtilities;
import org.millenaire.common.village.Building;
import org.millenaire.common.village.ConstructionIP;

@DocumentedElement.Documentation(value="Build a building")
public class GoalConstructionStepByStep
extends Goal {
    public GoalConstructionStepByStep() {
        this.tags.add("tag_construction");
        this.icon = InvItem.createInvItem(Items.IRON_SHOVEL);
    }

    @Override
    public int actionDuration(MillVillager villager) {
        ConstructionIP cip = villager.getCurrentConstruction();
        if (cip == null) {
            return 0;
        }
        BuildingBlock bblock = cip.getCurrentBlock();
        if (bblock == null) {
            return 0;
        }
        int toolEfficiency = (int)villager.getBestShovel().getDestroySpeed(new ItemStack((Item)villager.getBestShovel(), 1), Blocks.DIRT.defaultBlockState());
        int duration = 14;
        duration = toolEfficiency > 8 ? 7 : (toolEfficiency == 8 ? 8 : (toolEfficiency >= 6 ? 10 : (toolEfficiency >= 4 ? 12 : (toolEfficiency >= 2 ? 14 : 16))));
        if (bblock.block == Blocks.AIR || bblock.block == Blocks.DIRT || bblock.block == Blocks.GRASS_BLOCK || bblock.block == Blocks.SAND) {
            return (int)((float)duration / 4.0f);
        }
        return duration;
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) {
        ConstructionIP cip = villager.getCurrentConstruction();
        if (cip == null) {
            return null;
        }
        BuildingBlock bblock = cip.getCurrentBlock();
        if (bblock == null) {
            return null;
        }
        return this.packDest(bblock.p);
    }

    private ConstructionIP getDoableConstructionIP(MillVillager villager) {
        for (ConstructionIP cip : villager.getTownHall().getConstructionsInProgress()) {
            boolean possible = true;
            if (cip.getBuilder() != null && cip.getBuilder() != villager || cip.getBuildingLocation() == null || cip.getBblocks() == null) {
                possible = false;
            }
            if (possible) {
                if (villager.getTownHall().getBuildingPlanForConstruction(cip) == null) {
                    return null;
                }
                for (MillVillager v : villager.getTownHall().getKnownVillagers()) {
                    if (!Goal.getResourcesForBuild.key.equals(v.goalKey) && !Goal.construction.key.equals(v.goalKey) || v.constructionJobId != cip.getId()) continue;
                    possible = false;
                }
                for (InvItem key : villager.getTownHall().getBuildingPlanForConstruction((ConstructionIP)cip).resCost.keySet()) {
                    if (villager.countInv(key) >= villager.getTownHall().getBuildingPlanForConstruction((ConstructionIP)cip).resCost.get(key)) continue;
                    possible = false;
                }
            }
            if (!possible) continue;
            return cip;
        }
        return null;
    }

    @Override
    public ItemStack[] getHeldItemsOffHandTravelling(MillVillager villager) {
        ConstructionIP cip = villager.getCurrentConstruction();
        if (cip == null) {
            return null;
        }
        BuildingBlock bblock = cip.getCurrentBlock();
        if (bblock != null && !bblock.getBlockstate().isAir()) {
            ItemStack stack = org.millenaire.common.item.InvItem.createInvItem(bblock.getBlockstate()).getItemStack();
            if (!stack.isEmpty()) return new ItemStack[]{stack};
        }
        return null;
    }

    @Override
    public ItemStack[] getHeldItemsTravelling(MillVillager villager) {
        return villager.getBestShovelStack();
    }

    @Override
    public AStarConfig getPathingConfig(MillVillager villager) {
        ConstructionIP cip = villager.getCurrentConstruction();
        if (cip != null && cip.getBuildingLocation() != null && cip.getBuildingLocation().containsPlanTag("scaffoldings")) {
            return JPS_CONFIG_BUILDING_SCAFFOLDINGS;
        }
        if (!villager.canVillagerClearLeaves()) {
            return JPS_CONFIG_BUILDING_NO_LEAVES;
        }
        return JPS_CONFIG_BUILDING;
    }

    @Override
    public boolean isPossibleSpecific(MillVillager villager) {
        return this.getDoableConstructionIP(villager) != null;
    }

    @Override
    public boolean isStillValidSpecific(MillVillager villager) throws Exception {
        ConstructionIP cip = villager.getCurrentConstruction();
        return cip != null;
    }

    @Override
    public boolean lookAtGoal() {
        return true;
    }

    @Override
    public void onAccept(MillVillager villager) {
        ConstructionIP cip = this.getDoableConstructionIP(villager);
        if (cip == null) {
            return;
        }
        cip.setBuilder(villager);
        villager.constructionJobId = cip.getId();
    }

    @Override
    public boolean performAction(MillVillager villager) throws MillLog.MillenaireException {
        ConstructionIP cip = villager.getCurrentConstruction();
        if (cip == null) {
            return true;
        }
        BuildingBlock bblock = cip.getCurrentBlock();
        if (bblock == null) {
            return true;
        }
        if (MillConfigValues.LogWifeAI >= 2) {
            MillLog.minor(villager, "Setting block at " + bblock.p + " type: " + bblock.block + " replacing: " + villager.getBlock(bblock.p) + " distance: " + bblock.p.distanceTo((Entity)villager));
        }
        if (bblock.p.horizontalDistanceTo((Entity)villager) < 1.0 && (double)bblock.p.getiY() > villager.getY() && (double)bblock.p.getiY() < villager.getY() + 2.0) {
            boolean jumped = false;
            Level world = villager.level;
            if (!WorldUtilities.isBlockFullCube(world, villager.getPos().getiX() + 1, villager.getPos().getiY() + 1, villager.getPos().getiZ()) && !WorldUtilities.isBlockFullCube(world, villager.getPos().getiX() + 1, villager.getPos().getiY() + 2, villager.getPos().getiZ())) {
                villager.setPos(villager.getPos().getiX() + 1, villager.getPos().getiY() + 1, villager.getPos().getiZ());
                jumped = true;
            }
            if (!(jumped || WorldUtilities.isBlockFullCube(world, villager.getPos().getiX() - 1, villager.getPos().getiY() + 1, villager.getPos().getiZ()) || WorldUtilities.isBlockFullCube(world, villager.getPos().getiX() - 1, villager.getPos().getiY() + 2, villager.getPos().getiZ()))) {
                villager.setPos(villager.getPos().getiX() - 1, villager.getPos().getiY() + 1, villager.getPos().getiZ());
                jumped = true;
            }
            if (!(jumped || WorldUtilities.isBlockFullCube(world, villager.getPos().getiX(), villager.getPos().getiY(), villager.getPos().getiZ() + 1) || WorldUtilities.isBlockFullCube(world, villager.getPos().getiX(), villager.getPos().getiY() + 2, villager.getPos().getiZ() + 1))) {
                villager.setPos(villager.getPos().getiX(), villager.getPos().getiY() + 1, villager.getPos().getiZ() + 1);
                jumped = true;
            }
            if (!(jumped || WorldUtilities.isBlockFullCube(world, villager.getPos().getiX(), villager.getPos().getiY() + 1, villager.getPos().getiZ() - 1) || WorldUtilities.isBlockFullCube(world, villager.getPos().getiX(), villager.getPos().getiY() + 2, villager.getPos().getiZ() - 1))) {
                villager.setPos(villager.getPos().getiX(), villager.getPos().getiY() + 1, villager.getPos().getiZ() - 1);
                jumped = true;
            }
            if (!jumped && MillConfigValues.LogWifeAI >= 1) {
                MillLog.major(villager, "Tried jumping in construction but couldn't");
            }
        }
        boolean blockSet = bblock.build(villager.level, villager.getTownHall(), false, false);
        while (!blockSet && cip.areBlocksLeft()) {
            cip.incrementBblockPos();
            BuildingBlock bb = cip.getCurrentBlock();
            if (bb == null || bb.alreadyDone(villager.level)) continue;
            blockSet = bb.build(villager.level, villager.getTownHall(), false, false);
        }
        villager.swing(InteractionHand.MAIN_HAND);
        villager.actionStart = 0L;
        boolean foundNextBlock = false;
        while (!foundNextBlock && cip.areBlocksLeft()) {
            cip.incrementBblockPos();
            BuildingBlock bb = cip.getCurrentBlock();
            if (bb == null || bb.alreadyDone(villager.level)) continue;
            villager.setGoalDestPoint(bb.p);
            foundNextBlock = true;
        }
        if (!cip.areBlocksLeft()) {
            if (MillConfigValues.LogBuildingPlan >= 1) {
                MillLog.major(this, "Villager " + villager + " laid last block in " + cip.getBuildingLocation().planKey + " at " + bblock.p);
            }
            cip.clearBblocks();
            BuildingPlan plan = villager.getTownHall().getBuildingPlanForConstruction(cip);
            for (InvItem key : plan.resCost.keySet()) {
                villager.takeFromInv(key.getItem(), key.meta, (int)plan.resCost.get(key));
            }
            if (cip.getBuildingLocation() != null) {
                if (cip.getBuildingLocation().level == 0) {
                    villager.getTownHall().initialiseConstruction(cip, cip.getBuildingLocation().chestPos);
                } else {
                    Building building = cip.getBuildingLocation().getBuilding(villager.level);
                    if (building != null) {
                        plan.updateBuildingForPlan(building);
                    }
                }
            }
        }
        if (!foundNextBlock) {
            villager.setGoalDestPoint(null);
        }
        if (MillConfigValues.LogWifeAI >= 2 && villager.extraLog) {
            MillLog.minor(villager, "Reseting actionStart after " + (villager.level.getGameTime() - villager.actionStart));
        }
        return !cip.areBlocksLeft();
    }

    @Override
    public int priority(MillVillager villager) {
        return 1500;
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
    public boolean stuckAction(MillVillager villager) throws MillLog.MillenaireException {
        if (villager.getGoalDestPoint().horizontalDistanceTo((Entity)villager) < 30.0) {
            if (MillConfigValues.LogWifeAI >= 2) {
                MillLog.major(villager, "Putting block at a distance: " + villager.getGoalDestPoint().distanceTo((Entity)villager));
            }
            this.performAction(villager);
            return true;
        }
        return false;
    }

    @Override
    public long stuckDelay(MillVillager villager) {
        return 100L;
    }

    @Override
    public boolean unreachableDestination(MillVillager villager) throws Exception {
        this.performAction(villager);
        return true;
    }
}

