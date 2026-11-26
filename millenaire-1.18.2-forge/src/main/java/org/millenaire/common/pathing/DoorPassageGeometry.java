package org.millenaire.common.pathing;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A door panel never disappears. Select the state that lies ALONG the passage. */
public final class DoorPassageGeometry {
    private DoorPassageGeometry() {}

    public static boolean openForDirection(BlockState state, double dx, double dz) {
        Direction.Axis crossing = Math.abs(dx) > Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z;
        return state.getValue(DoorBlock.FACING).getAxis() == crossing;
    }

    public static BlockState passageState(BlockState state, double dx, double dz) {
        if (state.getValue(DoorBlock.POWERED)) return state;
        return state.setValue(DoorBlock.OPEN, openForDirection(state, dx, dz));
    }
}
