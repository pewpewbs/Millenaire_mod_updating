/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.item.EnumDyeColor
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.World
 */
package org.millenaire.common.block;

import java.util.Random;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.millenaire.common.block.BlockHalfSlab;
import org.millenaire.common.block.IPaintedBlock;
import org.millenaire.common.block.MillBlocks;

public class BlockPaintedSlab
extends BlockHalfSlab
implements IPaintedBlock {
    private final DyeColor colour;
    private final String blockType;

    public BlockPaintedSlab(String blockType, Block baseBlock, DyeColor colour) {
        super(baseBlock);
        this.blockType = blockType;
        this.colour = colour;
        this.setRegistryName(blockType + "_" + BlockPaintedBricks.getColorName(colour));
    }

    @Override
    public String getBlockType() {
        return this.blockType;
    }

    @Override
    public DyeColor getDyeColour() {
        return this.colour;
    }

    public ItemStack getCloneItemStack(net.minecraft.world.level.BlockGetter worldIn, BlockPos pos, BlockState state) {
        return new ItemStack(MillBlocks.PAINTED_BRICK_MAP.get(this.getBlockType()).get(this.colour));
    }

    public Item func_180660_a(BlockState state, Random rand, int fortune) {
        return Item.byBlock((Block)MillBlocks.PAINTED_BRICK_MAP.get(this.getBlockType()).get(this.colour));
    }
}

