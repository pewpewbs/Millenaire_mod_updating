package org.millenaire.common.world;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Keeps the original five tree shapes while using the current world and leaf update APIs. */
public abstract class MillTreeGenerator {
    private final int flags;
    protected MillTreeGenerator(boolean notify) { flags = notify ? 3 : 2; }
    public abstract boolean func_180709_b(Level level, Random random, BlockPos pos);
    protected boolean isReplaceable(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)
            || state.is(BlockTags.SAPLINGS) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)
            || state.is(Blocks.VINE);
    }
    protected void prepareSoil(Level level, BlockPos pos) {
        BlockState soil = level.getBlockState(pos);
        if (soil.is(Blocks.GRASS_BLOCK) || soil.is(Blocks.FARMLAND) || soil.is(Blocks.MYCELIUM))
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), flags);
    }
    protected void func_175903_a(Level level, BlockPos pos, BlockState state) {
        if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) return;
        level.setBlock(pos, state, flags);
        if (state.getBlock() instanceof LeavesBlock) level.scheduleTick(pos, state.getBlock(), 1);
    }
}
