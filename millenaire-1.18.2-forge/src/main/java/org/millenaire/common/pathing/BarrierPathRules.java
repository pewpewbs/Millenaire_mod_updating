package org.millenaire.common.pathing;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.common.block.BlockMillWall;

/** Solid barriers must not be treated as a walk-through node or a one-block step. */
public final class BarrierPathRules {
    private BarrierPathRules() {}
    public static boolean isBarrier(BlockState state) {
        Block block = state.getBlock();
        return block instanceof FenceBlock || block instanceof WallBlock
            || block instanceof BlockMillWall || block instanceof IronBarsBlock;
    }
    public static boolean blocksStepAbove(BlockState state) {
        return isBarrier(state) || state.getBlock() instanceof FenceGateBlock && !state.getValue(FenceGateBlock.OPEN);
    }
}
