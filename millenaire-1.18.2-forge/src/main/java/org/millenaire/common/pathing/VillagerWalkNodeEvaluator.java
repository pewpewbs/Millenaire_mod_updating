package org.millenaire.common.pathing;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public final class VillagerWalkNodeEvaluator extends WalkNodeEvaluator {
    @Override
    public BlockPathTypes getBlockPathType(BlockGetter world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        // Neither walk through a partial chest shape nor use its lid as a step.
        if (ChestPathRules.isChest(world.getBlockState(pos).getBlock())
                || y > world.getMinBuildHeight()
                && ChestPathRules.isChest(world.getBlockState(pos.below()).getBlock())) {
            return BlockPathTypes.BLOCKED;
        }
        if (BarrierPathRules.isBarrier(world.getBlockState(pos))
                || y > world.getMinBuildHeight() && BarrierPathRules.blocksStepAbove(world.getBlockState(pos.below())))
            return BlockPathTypes.BLOCKED;
        var state = world.getBlockState(pos);
        if (state.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock)
            return state.getValue(net.minecraft.world.level.block.FenceGateBlock.OPEN) ? BlockPathTypes.WALKABLE : BlockPathTypes.WALKABLE_DOOR;
        BlockPathTypes type = super.getBlockPathType(world, x, y, z);
        if ((type == BlockPathTypes.WALKABLE || type == BlockPathTypes.OPEN)
                && (state.getBlock() instanceof net.minecraft.world.level.block.CropBlock
                    || world.getBlockState(pos.below()).getBlock() instanceof net.minecraft.world.level.block.FarmBlock))
            return BlockPathTypes.DANGER_OTHER;
        return type;
    }
}
