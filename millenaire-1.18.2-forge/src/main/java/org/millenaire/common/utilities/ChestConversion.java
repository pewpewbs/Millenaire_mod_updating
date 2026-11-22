package org.millenaire.common.utilities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.common.block.BlockLockedChest;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.entity.TileEntityLockedChest;

/** Converts one half at a time, retaining inventory, names and unopened loot tables. */
public final class ChestConversion {
    private ChestConversion() {}

    public static boolean convert(Level world, BlockPos pos) {
        if (world.isClientSide || !world.hasChunkAt(pos)) return false;
        BlockState previous = world.getBlockState(pos);
        if (!previous.is(Blocks.CHEST) || !(world.getBlockEntity(pos) instanceof ChestBlockEntity original)) return false;
        CompoundTag saved = original.saveWithoutMetadata();
        BlockState replacement = MillBlocks.LOCKED_CHEST.defaultBlockState()
            .setValue(BlockLockedChest.FACING, previous.getValue(ChestBlock.FACING));
        // ChestBlock.onRemove drops the old inventory. Detach it first so the items
        // can be transferred exactly once, including the untouched loot-table seed.
        world.removeBlockEntity(pos);
        boolean converted = false;
        try {
            if (world.setBlock(pos, replacement, 3)
                && world.getBlockEntity(pos) instanceof TileEntityLockedChest target) {
                target.load(saved);
                target.setChanged();
                converted = true;
            }
            return converted;
        } finally {
            if (!converted) {
                // Restore the original object even if replacement failed before a
                // new block entity was created. No items were cleared from it.
                world.removeBlockEntity(pos);
                world.setBlock(pos, previous, 3);
                original.clearRemoved();
                world.setBlockEntity(original);
                original.setChanged();
            }
        }
    }
}
