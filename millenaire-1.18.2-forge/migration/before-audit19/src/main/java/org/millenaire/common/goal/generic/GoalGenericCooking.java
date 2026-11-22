/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.init.Items
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.tileentity.TileEntityFurnace
 */
package org.millenaire.common.goal.generic;

import java.util.List;
import net.minecraft.world.item.Items;
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
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;

public class GoalGenericCooking
extends GoalGeneric {
    public static final String GOAL_TYPE = "cooking";
    @ConfigAnnotations.ConfigField(type=AnnotedParameter.ParameterType.INVITEM)
    @ConfigAnnotations.FieldDocumentation(explanation="The item to be cooked.")
    public InvItem itemToCook = null;
    @ConfigAnnotations.ConfigField(type=AnnotedParameter.ParameterType.INTEGER, defaultValue="16")
    @ConfigAnnotations.FieldDocumentation(explanation="Minimum number of items that can be added to a cooking.")
    public int minimumToCook;

    @Override
    public void applyDefaultSettings() {
        this.lookAtGoal = true;
    }

    @Override
    public Goal.GoalInformation getDestination(MillVillager villager) throws Exception {
        List<Building> buildings = this.getBuildings(villager);
        for (Building dest : buildings) {
            if (!this.isDestPossible(villager, dest)) continue;
            int countGoods = dest.countGoods(this.itemToCook) + villager.countInv(this.itemToCook);
            for (Point p : dest.getResManager().furnaces) {
                FurnaceBlockEntity furnace = p.getFurnace(villager.level);
                if (furnace == null) continue;
                if (countGoods >= this.minimumToCook && (furnace.getItem(0) == ItemStack.EMPTY || furnace.getItem(0).getItem() == Items.AIR || furnace.getItem(0).getItem() == this.itemToCook.getItem() && furnace.getItem(0).getDamageValue() == this.itemToCook.meta && furnace.getItem(0).getCount() < 32)) {
                    return this.packDest(p, dest);
                }
                if (furnace.getItem(2) == null || furnace.getItem(2).getCount() < this.minimumToCook) continue;
                return this.packDest(p, dest);
            }
            boolean firepitBurnable = TileEntityFirePit.isFirePitBurnable(villager.level, this.itemToCook.staticStack);
            if (!firepitBurnable) continue;
            for (Point p : dest.getResManager().firepits) {
                ItemStack stack;
                int slotNb;
                TileEntityFirePit firepit = p.getFirePit(villager.level);
                if (firepit == null) continue;
                for (slotNb = 0; slotNb < 3; ++slotNb) {
                    stack = firepit.inputs.getStackInSlot(slotNb);
                    if (countGoods < this.minimumToCook || !stack.isEmpty() && (stack.getItem() != this.itemToCook.getItem() || stack.getDamageValue() != this.itemToCook.meta || stack.getCount() >= 32)) continue;
                    return this.packDest(p, dest);
                }
                for (slotNb = 0; slotNb < 3; ++slotNb) {
                    stack = firepit.outputs.getStackInSlot(slotNb);
                    if (stack == null || stack.getCount() < this.minimumToCook) continue;
                    return this.packDest(p, dest);
                }
            }
        }
        return null;
    }

    @Override
    public ItemStack getIcon() {
        if (this.icon != null) {
            return this.icon.getItemStack();
        }
        if (this.itemToCook != null) {
            return this.itemToCook.getItemStack();
        }
        return null;
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
    public boolean performAction(MillVillager villager) throws Exception {
        BlockEntity tileEntity = villager.getGoalDestPoint().getTileEntity(villager.level);
        Building dest = villager.getGoalBuildingDest();
        boolean firepitBurnable = TileEntityFirePit.isFirePitBurnable(villager.level, this.itemToCook.staticStack);
        if (tileEntity != null && dest != null) {
            if (tileEntity instanceof FurnaceBlockEntity) {
                FurnaceBlockEntity furnace = (FurnaceBlockEntity)tileEntity;
                this.performAction_furnace(dest, furnace, villager);
            } else if (firepitBurnable && tileEntity instanceof TileEntityFirePit) {
                TileEntityFirePit firepit = (TileEntityFirePit)tileEntity;
                this.performAction_firepit(dest, firepit, villager);
            }
        }
        return true;
    }

    private void performAction_firepit(Building dest, TileEntityFirePit firepit, MillVillager villager) {
        ItemStack stack;
        int slotNb;
        for (slotNb = 0; slotNb < 3; ++slotNb) {
            int nb;
            stack = firepit.inputs.getStackInSlot(slotNb);
            int countGoods = dest.countGoods(this.itemToCook) + villager.countInv(this.itemToCook);
            if ((!stack.isEmpty() || countGoods < this.minimumToCook) && (stack.isEmpty() || stack.getItem() != this.itemToCook.getItem() || stack.getDamageValue() != this.itemToCook.meta || stack.getCount() >= 64 || countGoods <= 0)) continue;
            if (stack.isEmpty()) {
                nb = Math.min(64, countGoods);
                firepit.inputs.setStackInSlot(slotNb, org.millenaire.common.item.LegacyItemStacks.stack(this.itemToCook.getItem(), nb, this.itemToCook.meta));
                dest.takeGoods(this.itemToCook, nb);
                continue;
            }
            nb = Math.min(64 - stack.getCount(), countGoods);
            ItemStack newStack = stack.copy();
            newStack.setCount(stack.getCount() + nb);
            firepit.inputs.setStackInSlot(slotNb, newStack);
            dest.takeGoods(this.itemToCook, nb);
        }
        for (slotNb = 0; slotNb < 3; ++slotNb) {
            stack = firepit.outputs.getStackInSlot(slotNb);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            int meta = stack.getDamageValue();
            dest.storeGoods(item, meta, stack.getCount());
            firepit.outputs.setStackInSlot(slotNb, ItemStack.EMPTY);
        }
    }

    private void performAction_furnace(Building dest, FurnaceBlockEntity furnace, MillVillager villager) {
        int countGoods = dest.countGoods(this.itemToCook) + villager.countInv(this.itemToCook);
        if (furnace.getItem(0).isEmpty() && countGoods >= this.minimumToCook || !furnace.getItem(0).isEmpty() && furnace.getItem(0).getItem() == this.itemToCook.getItem() && furnace.getItem(0).getDamageValue() == this.itemToCook.meta && furnace.getItem(0).getCount() < 64 && countGoods > 0) {
            int nb;
            if (furnace.getItem(0).isEmpty()) {
                nb = Math.min(64, countGoods);
                furnace.setItem(0, org.millenaire.common.item.LegacyItemStacks.stack(this.itemToCook.getItem(), nb, this.itemToCook.meta));
                dest.takeGoods(this.itemToCook, nb);
            } else {
                nb = Math.min(64 - furnace.getItem(0).getCount(), countGoods);
                ItemStack stack = furnace.getItem(0);
                stack.setCount(furnace.getItem(0).getCount() + nb);
                furnace.setItem(0, stack);
                dest.takeGoods(this.itemToCook, nb);
            }
        }
        if (!furnace.getItem(2).isEmpty()) {
            Item item = furnace.getItem(2).getItem();
            int meta = furnace.getItem(2).getDamageValue();
            dest.storeGoods(item, meta, furnace.getItem(2).getCount());
            furnace.setItem(2, ItemStack.EMPTY);
        }
    }

    @Override
    public boolean validateGoal() {
        if (this.itemToCook == null) {
            MillLog.error(this, "The itemtocook id is mandatory in custom cooking goals.");
            return false;
        }
        return true;
    }
}

