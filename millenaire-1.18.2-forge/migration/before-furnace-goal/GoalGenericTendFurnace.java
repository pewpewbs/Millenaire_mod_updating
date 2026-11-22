/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.init.Blocks
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.tileentity.TileEntityFurnace
 */
package org.millenaire.common.goal.generic;

import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import org.millenaire.common.annotedparameters.AnnotedParameter;
import org.millenaire.common.annotedparameters.ConfigAnnotations;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.entity.TileEntityFirePit;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.goal.generic.GoalGeneric;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.utilities.BlockItemUtilities;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;

public class GoalGenericTendFurnace
extends GoalGeneric {
    public static final String GOAL_TYPE = "tendfurnace";
    private static ItemStack[][] PLANKS = new ItemStack[][]{{org.millenaire.common.item.LegacyItemStacks.stack(Blocks.field_150344_f, 1, 0)}, {org.millenaire.common.item.LegacyItemStacks.stack(Blocks.field_150344_f, 1, 1)}, {org.millenaire.common.item.LegacyItemStacks.stack(Blocks.field_150344_f, 1, 2)}, {org.millenaire.common.item.LegacyItemStacks.stack(Blocks.field_150344_f, 1, 3)}, {org.millenaire.common.item.LegacyItemStacks.stack(Blocks.field_150344_f, 1, 4)}, {org.millenaire.common.item.LegacyItemStacks.stack(Blocks.field_150344_f, 1, 5)}};
    @ConfigAnnotations.ConfigField(type=AnnotedParameter.ParameterType.INTEGER, defaultValue="4")
    @ConfigAnnotations.FieldDocumentation(explanation="Minimum number of wood to put back in one go.")
    public int minimumFuel;

    @Override
    public void applyDefaultSettings() {
        this.lookAtGoal = true;
        this.icon = InvItem.createInvItem(Blocks.FURNACE);
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws Exception {
        List<Building> buildings = this.getBuildings(villager);
        for (Building dest : buildings) {
            if (!this.isDestPossible(villager, dest)) continue;
            int anyWoodAvailable = dest.countGoods(Blocks.field_150364_r, -1) + villager.countInv(Blocks.field_150364_r, -1) + villager.getHouse().countGoods(Blocks.field_150364_r, -1);
            for (Point p : dest.getResManager().furnaces) {
                int woodMeta;
                int woodAvailable;
                FurnaceBlockEntity furnace = p.getFurnace(villager.level);
                if (furnace == null) continue;
                if (furnace.getItem(1) == ItemStack.EMPTY && anyWoodAvailable > 4) {
                    return this.packDest(p, dest);
                }
                if (furnace.getItem(1).getCount() >= 32 || furnace.getItem(1).getItem() != Item.byBlock((Block)Blocks.field_150344_f) || (woodAvailable = this.getWoodCountByMeta(villager, dest, woodMeta = org.millenaire.common.item.LegacyItemStacks.metadata(furnace.getItem(1)))) < this.minimumFuel) continue;
                return this.packDest(p, dest);
            }
            for (Point p : dest.getResManager().firepits) {
                int woodMeta;
                int woodAvailable;
                TileEntityFirePit firepit = p.getFirePit(villager.level);
                if (firepit == null) continue;
                ItemStack stack = firepit.fuel.getStackInSlot(0);
                if (stack.isEmpty() && anyWoodAvailable > 4) {
                    return this.packDest(p, dest);
                }
                if (stack.getCount() >= 32 || stack.getItem() != Item.byBlock((Block)Blocks.field_150344_f) || (woodAvailable = this.getWoodCountByMeta(villager, dest, woodMeta = org.millenaire.common.item.LegacyItemStacks.metadata(stack))) <= 4) continue;
                return this.packDest(p, dest);
            }
        }
        return null;
    }

    @Override
    public ItemStack[] getHeldItemsTravelling(MillVillager villager) {
        Building dest = villager.getGoalBuildingDest();
        BlockEntity tileEntity = villager.getGoalDestPoint().getTileEntity(villager.level);
        if (dest != null && tileEntity != null) {
            if (tileEntity instanceof FurnaceBlockEntity) {
                FurnaceBlockEntity furnace = (FurnaceBlockEntity)tileEntity;
                if (furnace.getItem(1) == ItemStack.EMPTY) {
                    int mostWoodAvailable = 0;
                    int mostWoodAvailableMeta = -1;
                    for (int woodMeta = 0; woodMeta < 6; ++woodMeta) {
                        int woodAvailable = this.getWoodCountByMeta(villager, dest, woodMeta);
                        if (woodAvailable <= mostWoodAvailable) continue;
                        mostWoodAvailable = woodAvailable;
                        mostWoodAvailableMeta = woodMeta;
                    }
                    if (mostWoodAvailableMeta > -1) {
                        return PLANKS[mostWoodAvailableMeta];
                    }
                } else if (furnace.getItem(1).getCount() < 64 && furnace.getItem(1).getItem() == Item.byBlock((Block)Blocks.field_150344_f)) {
                    int woodMeta = org.millenaire.common.item.LegacyItemStacks.metadata(furnace.getItem(1));
                    return PLANKS[woodMeta];
                }
            } else if (tileEntity instanceof TileEntityFirePit) {
                TileEntityFirePit firepit = (TileEntityFirePit)tileEntity;
                if (firepit.fuel.getStackInSlot(0) == ItemStack.EMPTY) {
                    int mostWoodAvailable = 0;
                    int mostWoodAvailableMeta = 0;
                    for (int woodMeta = 0; woodMeta < 6; ++woodMeta) {
                        int woodAvailable = this.getWoodCountByMeta(villager, dest, woodMeta);
                        if (woodAvailable <= mostWoodAvailable) continue;
                        mostWoodAvailable = woodAvailable;
                        mostWoodAvailableMeta = woodMeta;
                    }
                    return PLANKS[mostWoodAvailableMeta];
                }
                if (firepit.fuel.getStackInSlot(0).getCount() < 64 && firepit.fuel.getStackInSlot(0).getItem() == Item.byBlock((Block)Blocks.field_150344_f)) {
                    int woodMeta = org.millenaire.common.item.LegacyItemStacks.metadata(firepit.fuel.getStackInSlot(0));
                    return PLANKS[woodMeta];
                }
            }
        }
        return null;
    }

    @Override
    public String getTypeLabel() {
        return GOAL_TYPE;
    }

    private int getWoodCountByMeta(MillVillager villager, Building dest, int woodMeta) {
        BlockState logsToTake = BlockItemUtilities.getLogBlockstateFromPlankMeta(woodMeta);
        return dest.countGoods(logsToTake) + villager.countInv(logsToTake) + villager.getHouse().countGoods(logsToTake);
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
    public boolean performAction(MillVillager villager) throws Exception {
        Building dest = villager.getGoalBuildingDest();
        BlockEntity tileEntity = villager.getGoalDestPoint().getTileEntity(villager.level);
        if (dest != null && tileEntity != null) {
            if (tileEntity instanceof FurnaceBlockEntity) {
                this.performAction_furnace(villager, (FurnaceBlockEntity)tileEntity, dest);
            } else if (tileEntity instanceof TileEntityFirePit) {
                this.performAction_firepit(villager, (TileEntityFirePit)tileEntity, dest);
            }
        }
        return true;
    }

    private void performAction_firepit(MillVillager villager, TileEntityFirePit firepit, Building dest) {
        if (firepit.fuel.getStackInSlot(0).isEmpty()) {
            int mostWoodAvailable = 0;
            int mostWoodAvailableMeta = -1;
            for (int woodMeta = 0; woodMeta < 6; ++woodMeta) {
                int woodAvailable = this.getWoodCountByMeta(villager, dest, woodMeta);
                if (woodAvailable <= mostWoodAvailable) continue;
                mostWoodAvailable = woodAvailable;
                mostWoodAvailableMeta = woodMeta;
            }
            int nbplanks = Math.min(64, mostWoodAvailable * 4);
            firepit.fuel.setStackInSlot(0, org.millenaire.common.item.LegacyItemStacks.stack(Blocks.field_150344_f, nbplanks, mostWoodAvailableMeta));
            BlockState logsToTake = BlockItemUtilities.getLogBlockstateFromPlankMeta(mostWoodAvailableMeta);
            int nbTaken = dest.takeGoods(logsToTake, nbplanks / 4);
            if (nbTaken < nbplanks / 4) {
                nbTaken += villager.takeFromInv(logsToTake, nbplanks / 4 - nbTaken);
            }
            if (nbTaken < nbplanks / 4) {
                nbTaken += villager.getHouse().takeGoods(logsToTake, nbplanks / 4 - nbTaken);
            }
        } else if (firepit.fuel.getStackInSlot(0).getCount() < 64 && firepit.fuel.getStackInSlot(0).getItem() == Item.byBlock((Block)Blocks.field_150344_f)) {
            int woodMeta = org.millenaire.common.item.LegacyItemStacks.metadata(firepit.fuel.getStackInSlot(0));
            BlockState logsToTake = BlockItemUtilities.getLogBlockstateFromPlankMeta(woodMeta);
            int woodAvailable = this.getWoodCountByMeta(villager, dest, woodMeta);
            int nbplanks = Math.min(64 - firepit.fuel.getStackInSlot(0).getCount(), woodAvailable * 4);
            firepit.fuel.setStackInSlot(1, org.millenaire.common.item.LegacyItemStacks.stack(Blocks.field_150344_f, firepit.fuel.getStackInSlot(0).getCount() + nbplanks, woodMeta));
            int nbTaken = dest.takeGoods(logsToTake, nbplanks / 4);
            if (nbTaken < nbplanks / 4) {
                nbTaken += villager.takeFromInv(logsToTake, nbplanks / 4 - nbTaken);
            }
            if (nbTaken < nbplanks / 4) {
                nbTaken += villager.getHouse().takeGoods(logsToTake, nbplanks / 4 - nbTaken);
            }
        }
    }

    private void performAction_furnace(MillVillager villager, FurnaceBlockEntity furnace, Building dest) {
        if (furnace.getItem(1) == ItemStack.EMPTY) {
            int mostWoodAvailable = 0;
            int mostWoodAvailableMeta = -1;
            for (int woodMeta = 0; woodMeta < 6; ++woodMeta) {
                int woodAvailable = this.getWoodCountByMeta(villager, dest, woodMeta);
                if (woodAvailable <= mostWoodAvailable) continue;
                mostWoodAvailable = woodAvailable;
                mostWoodAvailableMeta = woodMeta;
            }
            int nbplanks = Math.min(64, mostWoodAvailable * 4);
            furnace.setItem(1, org.millenaire.common.item.LegacyItemStacks.stack(Blocks.field_150344_f, nbplanks, mostWoodAvailableMeta));
            BlockState logsToTake = BlockItemUtilities.getLogBlockstateFromPlankMeta(mostWoodAvailableMeta);
            int nbTaken = dest.takeGoods(logsToTake, nbplanks / 4);
            if (nbTaken < nbplanks / 4) {
                nbTaken += villager.takeFromInv(logsToTake, nbplanks / 4 - nbTaken);
            }
            if (nbTaken < nbplanks / 4) {
                nbTaken += villager.getHouse().takeGoods(logsToTake, nbplanks / 4 - nbTaken);
            }
        } else if (furnace.getItem(1).getCount() < 64 && furnace.getItem(1).getItem() == Item.byBlock((Block)Blocks.field_150344_f)) {
            int woodMeta = org.millenaire.common.item.LegacyItemStacks.metadata(furnace.getItem(1));
            BlockState logsToTake = BlockItemUtilities.getLogBlockstateFromPlankMeta(woodMeta);
            int woodAvailable = this.getWoodCountByMeta(villager, dest, woodMeta);
            int nbplanks = Math.min(64 - furnace.getItem(1).getCount(), woodAvailable * 4);
            furnace.setItem(1, org.millenaire.common.item.LegacyItemStacks.stack(Blocks.field_150344_f, furnace.getItem(1).getCount() + nbplanks, woodMeta));
            int nbTaken = dest.takeGoods(logsToTake, nbplanks / 4);
            if (nbTaken < nbplanks / 4) {
                nbTaken += villager.takeFromInv(logsToTake, nbplanks / 4 - nbTaken);
            }
            if (nbTaken < nbplanks / 4) {
                nbTaken += villager.getHouse().takeGoods(logsToTake, nbplanks / 4 - nbTaken);
            }
        }
    }

    @Override
    public boolean validateGoal() {
        return true;
    }
}

