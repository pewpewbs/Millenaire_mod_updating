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
    public final PujaSacrifice shrine;
    ToolSlot slotTool;

    public ContainerPuja(Player player, Building temple) { this(0, player.getInventory(), temple); }

    public ContainerPuja(int id, net.minecraft.world.entity.player.Inventory inventory, Building temple) {
        super(MillMenus.PUJA, id);
        this.shrine = java.util.Objects.requireNonNull(temple.pujas, "Missing shrine");
        this.slotTool = new ToolSlot(shrine, 4, 86, 37);
        addSlot(new OfferingSlot(shrine, 0, 26, 19));
        addSlot(new MoneySlot(shrine, 1, 8, 55));
        addSlot(new MoneySlot(shrine, 2, 26, 55));
        addSlot(new MoneySlot(shrine, 3, 44, 55));
        addSlot(slotTool);
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 106 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 164));
        addDataSlots(new net.minecraft.world.inventory.ContainerData() {
            public int getCount() { return 6; }
            public int get(int index) {
                return switch(index) {
                    case 0 -> shrine.pujaProgress;
                    case 1 -> shrine.offeringProgress & 65535;
                    case 2 -> shrine.offeringProgress >>> 16;
                    case 3 -> shrine.offeringNeeded & 65535;
                    case 4 -> shrine.offeringNeeded >>> 16;
                    case 5 -> java.util.Arrays.asList(targets()).indexOf(shrine.currentTarget);
                    default -> 0;
                };
            }
            public void set(int index, int value) {
                switch(index) {
                    case 0 -> shrine.pujaProgress = (short)value;
                    case 1 -> shrine.offeringProgress = (shrine.offeringProgress & 0xffff0000) | (value & 65535);
                    case 2 -> shrine.offeringProgress = (shrine.offeringProgress & 65535) | (value << 16);
                    case 3 -> shrine.offeringNeeded = (shrine.offeringNeeded & 0xffff0000) | (value & 65535);
                    case 4 -> shrine.offeringNeeded = (shrine.offeringNeeded & 65535) | (value << 16);
                    case 5 -> shrine.currentTarget = value >= 0 && value < targets().length ? targets()[value] : null;
                }
            }
        });
    }

    private PujaSacrifice.PrayerTarget[] targets() { return shrine.type == 0 ? PujaSacrifice.PUJA_TARGETS : PujaSacrifice.MAYAN_TARGETS; }

    public static void open(Player player, Building temple) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) || temple == null || temple.pujas == null) return;
        temple.sendBuildingPacket(player, true);
        net.minecraftforge.network.NetworkHooks.openGui(serverPlayer, new net.minecraft.world.SimpleMenuProvider(
                (id, inventory, owner) -> new ContainerPuja(id, inventory, temple), temple.pujas.getDisplayName()),
                buffer -> buffer.writeBlockPos(temple.getPos().getBlockPos()));
    }

    @Override public boolean stillValid(Player player) {
        if (player.level != shrine.temple.world || shrine.temple.pujas != shrine) return false;
        var anchor = shrine.temple.getResManager().getCraftingPos();
        if (anchor == null) anchor = shrine.temple.getPos();
        return player.distanceToSqr(anchor.x, anchor.y, anchor.z) <= 256
                && (player.level.isClientSide || shrine.temple.getTownHall() != null
                    && shrine.temple.getTownHall().getReputation(player) >= -1024);
    }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (!stillValid(player) || id < 0 || id >= shrine.getTargets().size()) return false;
        GuiActions.pujasChangeEnchantment(player, shrine.temple, id);
        broadcastChanges();
        return true;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public ItemStack quickMoveStack(Player par1EntityPlayer, int stackID) {
        if (!stillValid(par1EntityPlayer) || stackID < 0 || stackID >= slots.size()) return ItemStack.EMPTY;
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
            super.setChanged();
        }
    }
}

