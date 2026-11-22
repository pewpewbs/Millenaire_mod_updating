/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nonnull
 *  net.minecraft.inventory.SlotFurnaceFuel
 *  net.minecraft.item.ItemStack
 *  net.minecraft.tileentity.TileEntityFurnace
 *  net.minecraftforge.items.IItemHandler
 *  net.minecraftforge.items.SlotItemHandler
 */
package org.millenaire.common.ui.firepit;

import javax.annotation.Nonnull;
import net.minecraft.world.inventory.FurnaceFuelSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class SlotFirePitFuel
extends SlotItemHandler {
    public SlotFirePitFuel(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    public int getMaxStackSize(@Nonnull ItemStack stack) {
        return FurnaceFuelSlot.isBucket((ItemStack)stack) ? 1 : super.getMaxStackSize(stack);
    }

    public boolean mayPlace(@Nonnull ItemStack stack) {
        return org.millenaire.common.entity.TileEntityFirePit.fuelTime(stack) > 0 || FurnaceFuelSlot.isBucket((ItemStack)stack);
    }
}

