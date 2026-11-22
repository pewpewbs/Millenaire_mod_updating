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
        // This method is also used to check a villager's HEAD position.
        // A closed gate is an OPENABLE doorway, not an impenetrable 2-block
        // wall: marking the air above it BLOCKED makes both vanilla A* and
        // legacy JPS reject the very gate they are configured to use.
        // Actual fences/walls remain non-walkable and non-steppable.
        return isBarrier(state);
    }
}
