/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.EnumDyeColor
 */
package org.millenaire.common.item;

import net.minecraft.world.item.DyeColor;
import org.millenaire.common.block.BlockPaintedBricks;
import org.millenaire.common.item.ItemMill;

public class ItemPaintBucket
extends ItemMill {
    private final DyeColor colour;

    public ItemPaintBucket(String baseName, DyeColor colour) {
        super(baseName + "_" + BlockPaintedBricks.getColorName(colour), new net.minecraft.world.item.Item.Properties().durability(2048));
        this.colour = colour;
    }

    public DyeColor getColour() {
        return this.colour;
    }
}

