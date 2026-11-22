/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nonnull
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.InventoryPlayer
 *  net.minecraft.inventory.Container
 *  net.minecraft.inventory.IContainerListener
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.TileEntityFurnace
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 *  net.minecraftforge.items.IItemHandler
 */
package org.millenaire.common.ui.firepit;

import javax.annotation.Nonnull;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.IItemHandler;
import org.millenaire.common.entity.TileEntityFirePit;
import org.millenaire.common.ui.firepit.SlotFirePitFuel;
import org.millenaire.common.ui.firepit.SlotFirePitInput;
import org.millenaire.common.ui.firepit.SlotFirePitOutput;

public class ContainerFirePit
extends AbstractContainerMenu {
    private static final int[][] INPUT_POSITIONS = new int[][]{{56, 8}, {44, 28}, {56, 48}};
    private static final int[][] OUTPUT_POSITIONS = new int[][]{{104, 8}, {116, 28}, {104, 48}};
    private static final int[] FUEL_POSITION = new int[]{80, 70};
    private static final int[] INV_POSITION = new int[]{8, 93};
    private final int inputStart;
    private final int inputEnd;
    private final int fuelStart;
    private final int fuelEnd;
    private final int outputStart;
    private final int outputEnd;
    private final int inventoryStart;
    private final int inventoryEnd;
    private final int hotbarStart;
    private final int hotbarEnd;
    private final TileEntityFirePit firePit;

    private static boolean inRange(int index, int start, int end) {
        return start <= index && index < end;
    }

    public ContainerFirePit(Player player, TileEntityFirePit firePit) { this(0, player.getInventory(), firePit); }
    public TileEntityFirePit getFirePit() { return firePit; }
    public ContainerFirePit(int id, Inventory playerInventory, TileEntityFirePit firePit) {
        super(org.millenaire.common.ui.MillMenus.FIRE_PIT, id);
        Player player = playerInventory.player;
        int i;
        this.firePit = firePit;
        this.inputStart = this.slots.size();
        for (i = 0; i < 3; ++i) {
            this.addSlot((Slot)new SlotFirePitInput((IItemHandler)firePit.inputs, i, INPUT_POSITIONS[i][0], INPUT_POSITIONS[i][1], player.level));
        }
        this.inputEnd = this.slots.size();
        this.fuelStart = this.slots.size();
        this.addSlot((Slot)new SlotFirePitFuel((IItemHandler)firePit.fuel, 0, FUEL_POSITION[0], FUEL_POSITION[1]));
        this.fuelEnd = this.slots.size();
        this.outputStart = this.slots.size();
        for (i = 0; i < 3; ++i) {
            this.addSlot((Slot)new SlotFirePitOutput(player, (IItemHandler)firePit.outputs, i, OUTPUT_POSITIONS[i][0], OUTPUT_POSITIONS[i][1]));
        }
        this.outputEnd = this.slots.size();
        this.inventoryStart = this.slots.size();
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot((Container)playerInventory, column + row * 9 + 9, INV_POSITION[0] + column * 18, INV_POSITION[1] + row * 18));
            }
        }
        this.inventoryEnd = this.slots.size();
        this.hotbarStart = this.slots.size();
        for (int hotbarIndex = 0; hotbarIndex < 9; ++hotbarIndex) {
            this.addSlot(new Slot((Container)playerInventory, hotbarIndex, INV_POSITION[0] + hotbarIndex * 18, INV_POSITION[1] + 54 + 4));
        }
        this.hotbarEnd = this.slots.size();
        addDataSlots(new net.minecraft.world.inventory.ContainerData() {
            public int getCount() { return 5; }
            public int get(int index) { return index < 3 ? firePit.getCookTime(index) : index == 3 ? firePit.getBurnTime() : firePit.getTotalBurnTime(); }
            public void set(int index, int value) { if (index < 3) firePit.setCookTime(index, value); else if (index == 3) firePit.setBurnTime(value); else firePit.setTotalBurnTime(value); }
        });
    }

    public boolean stillValid(@Nonnull Player playerIn) {
        return !firePit.isRemoved() && playerIn.level.getBlockEntity(firePit.getBlockPos()) == firePit && playerIn.distanceToSqr(firePit.getBlockPos().getX() + 0.5, firePit.getBlockPos().getY() + 0.5, firePit.getBlockPos().getZ() + 0.5) <= 64;
    }

    @Nonnull
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            original = stackInSlot.copy();
            if (ContainerFirePit.inRange(index, this.outputStart, this.outputEnd)) {
                if (!this.moveItemStackTo(stackInSlot, this.inventoryStart, this.hotbarEnd, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, original);
            } else if (ContainerFirePit.inRange(index, this.inventoryStart, this.hotbarEnd) ? (TileEntityFirePit.isFirePitBurnable(playerIn.level, stackInSlot) ? !this.moveItemStackTo(stackInSlot, this.inputStart, this.inputEnd, false) : (TileEntityFirePit.fuelTime(stackInSlot) > 0 ? !this.moveItemStackTo(stackInSlot, this.fuelStart, this.fuelEnd, false) : (ContainerFirePit.inRange(index, this.inventoryStart, this.inventoryEnd) ? !this.moveItemStackTo(stackInSlot, this.hotbarStart, this.hotbarEnd, false) : !this.moveItemStackTo(stackInSlot, this.inventoryStart, this.inventoryEnd, false)))) : !this.moveItemStackTo(stackInSlot, this.inventoryStart, this.hotbarEnd, false)) {
                return ItemStack.EMPTY;
            }
            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stackInSlot.getCount() == original.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(playerIn, stackInSlot);
        }
        return original;
    }

}
