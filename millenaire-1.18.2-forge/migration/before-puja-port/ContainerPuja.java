/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemArmor
 *  net.minecraft.item.ItemAxe
 *  net.minecraft.item.ItemBow
 *  net.minecraft.item.ItemPickaxe
 *  net.minecraft.item.ItemSpade
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.ItemSword
 */
package org.millenaire.common.ui;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import org.millenaire.common.item.MillItems;
import org.millenaire.common.ui.PujaSacrifice;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.village.Building;

public class ContainerPuja
extends AbstractContainerMenu {
    PujaSacrifice shrine;
    ToolSlot slotTool;

    public ContainerPuja(Player player, Building temple) {
        try {
            this.shrine = temple.pujas;
            this.slotTool = new ToolSlot(temple.pujas, 4, 86, 37);
            this.addSlot(new OfferingSlot(temple.pujas, 0, 26, 19));
            this.addSlot(new MoneySlot(temple.pujas, 1, 8, 55));
            this.addSlot(new MoneySlot(temple.pujas, 2, 26, 55));
            this.addSlot(new MoneySlot(temple.pujas, 3, 44, 55));
            this.addSlot(this.slotTool);
            for (int i = 0; i < 3; ++i) {
                for (int k = 0; k < 9; ++k) {
                    this.addSlot(new Slot((Container)player.getInventory(), k + i * 9 + 9, 8 + k * 18, 106 + i * 18));
                }
            }
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot((Container)player.getInventory(), j, 8 + j * 18, 164));
            }
        }
        catch (Exception e) {
            MillLog.printException("Exception in ContainerPuja(): ", e);
        }
    }

    public boolean stillValid(Player entityplayer) {
        return true;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public ItemStack quickMoveStack(Player par1EntityPlayer, int stackID) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(stackID);
        if (slot == null || !slot.hasItem()) return itemstack;
        ItemStack itemstack1 = slot.getItem();
        itemstack = itemstack1.copy();
        if (stackID == 4) {
            if (!this.moveItemStackTo(itemstack1, 5, 41, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(itemstack1, itemstack);
        } else if (stackID > 4) {
            if (itemstack1.getItem() == MillItems.DENIER || itemstack1.getItem() == MillItems.DENIER_ARGENT || itemstack1.getItem() == MillItems.DENIER_OR) {
                if (!this.moveItemStackTo(itemstack1, 1, 4, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.shrine.getOfferingValue(itemstack1) > 0) {
                if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.slotTool.mayPlace(itemstack1)) return ItemStack.EMPTY;
                if (!this.moveItemStackTo(itemstack1, 4, 5, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else if (!this.moveItemStackTo(itemstack1, 5, 41, false)) {
            return ItemStack.EMPTY;
        }
        if (itemstack1.getCount() == 0) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (itemstack1.getCount() == itemstack.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(par1EntityPlayer, itemstack1);
        return itemstack;
    }

    public static class ToolSlot
    extends Slot {
        PujaSacrifice shrine;

        public ToolSlot(PujaSacrifice shrine, int par2, int par3, int par4) {
            super((Container)shrine, par2, par3, par4);
            this.shrine = shrine;
        }

        public boolean mayPlace(ItemStack is) {
            Item item = is.getItem();
            if (this.shrine.type == 1) {
                return item instanceof SwordItem || item instanceof ArmorItem || item instanceof BowItem || item instanceof AxeItem;
            }
            return item instanceof ShovelItem || item instanceof AxeItem || item instanceof PickaxeItem;
        }

        public void setChanged() {
            this.shrine.calculateOfferingsNeeded();
            if (!this.shrine.temple.world.isClientSide) {
                this.shrine.temple.getTownHall().requestSave("Puja tool slot changed");
            }
            super.setChanged();
        }
    }

    public static class OfferingSlot
    extends Slot {
        PujaSacrifice shrine;

        public OfferingSlot(PujaSacrifice shrine, int par2, int par3, int par4) {
            super((Container)shrine, par2, par3, par4);
            this.shrine = shrine;
        }

        public boolean mayPlace(ItemStack par1ItemStack) {
            return this.shrine.getOfferingValue(par1ItemStack) > 0;
        }

        public void setChanged() {
            if (!this.shrine.temple.world.isClientSide) {
                this.shrine.temple.getTownHall().requestSave("Puja offering slot changed");
            }
            super.setChanged();
        }
    }

    public static class MoneySlot
    extends Slot {
        PujaSacrifice shrine;

        public MoneySlot(PujaSacrifice shrine, int par2, int par3, int par4) {
            super((Container)shrine, par2, par3, par4);
            this.shrine = shrine;
        }

        public boolean mayPlace(ItemStack is) {
            return is.getItem() == MillItems.DENIER || is.getItem() == MillItems.DENIER_OR || is.getItem() == MillItems.DENIER_ARGENT;
        }

        public void setChanged() {
            if (!this.shrine.temple.world.isClientSide) {
                this.shrine.temple.getTownHall().requestSave("Puja money slot changed");
            }
            super.setChanged();
        }
    }
}

