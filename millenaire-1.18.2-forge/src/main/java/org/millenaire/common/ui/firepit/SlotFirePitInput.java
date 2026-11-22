/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nonnull
 *  net.minecraft.item.ItemStack
 *  net.minecraftforge.items.IItemHandler
 *  net.minecraftforge.items.SlotItemHandler
 */
package org.millenaire.common.ui.firepit;

import javax.annotation.Nonnull;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.millenaire.common.entity.TileEntityFirePit;

public class SlotFirePitInput
extends SlotItemHandler {
    private final net.minecraft.world.level.Level level;
    public SlotFirePitInput(IItemHandler itemHandler, int index, int xPosition, int yPosition, net.minecraft.world.level.Level level) {
        super(itemHandler, index, xPosition, yPosition);
        this.level = level;
    }

    public boolean mayPlace(@Nonnull ItemStack stack) {
        return TileEntityFirePit.isFirePitBurnable(level, stack);
    }
}

