package org.millenaire.common.buildingplan;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

/** Place both halves before allowing neighbour shape updates to inspect the pair. */
public final class DoorPlacement {
    private DoorPlacement() {}
    public static BlockState half(BlockState state, boolean inverted, DoubleBlockHalf half) {
        return (inverted ? state.setValue(DoorBlock.HINGE, DoorHingeSide.RIGHT) : state)
            .setValue(DoorBlock.HALF, half);
    }
    public static boolean place(Level world, BlockPos pos, BlockState state, boolean inverted) {
        BlockPos bottom = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        BlockPos top = bottom.above();
        if (world.isOutsideBuildHeight(bottom) || world.isOutsideBuildHeight(top)) return false;
        BlockState lower = half(state, inverted, DoubleBlockHalf.LOWER);
        BlockState upper = half(state, inverted, DoubleBlockHalf.UPPER);
        if (!lower.canSurvive(world,bottom)) return false;
        // UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE: an incomplete pair must not remove itself.
        world.setBlock(bottom, lower, 18);
        world.setBlock(top, upper, 18);
        ConstructionPairPlacement.finish(world,bottom);
        ConstructionPairPlacement.finish(world,top);
        return world.getBlockState(bottom).equals(lower) && world.getBlockState(top).equals(upper);
    }
}
