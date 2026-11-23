package org.millenaire.common.pathing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.common.utilities.BlockItemUtilities;

/**
 * Prevents pathfinders from planning a crossing along a wooden door's leaf.
 *
 * A wooden door's FACING defines the axis through its CLOSED panel. When
 * opened, its collision shape swings onto the perpendicular edge of the
 * block. Treating that whole block as WALKABLE in every direction causes
 * mobs to repeatedly collide with the OPEN leaf on sideways routes.
 *
 * This test is deliberately per-EDGE, not per-node: the block remains
 * reachable from both correct sides, and double doors remain traversable.
 * Fence gates, ladders and non-door blocks are untouched.
 */
public final class DoorTransitionRules {
    private DoorTransitionRules() {}

    private static boolean allows(BlockState state, int dx, int dz) {
        if (!BlockItemUtilities.isWoodenDoor(state.getBlock())) return true;
        boolean eastWestFacing =
                state.getValue(DoorBlock.FACING).getAxis() == Direction.Axis.X;
        return DoorApproachRules.followsDoorNormal(dx, dz, eastWestFacing);
    }

    public static boolean allowsEdge(BlockGetter world,
            int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        int dx = toX - fromX;
        int dz = toZ - fromZ;
        if (dx == 0 && dz == 0) return true;
        // An edge is invalid if it enters OR exits a door sideways.
        // A door occupying the current node must also be exited normally.
        BlockState from = world.getBlockState(new BlockPos(fromX, fromY, fromZ));
        if (!allows(from, dx, dz)) return false;
        BlockState to = world.getBlockState(new BlockPos(toX, toY, toZ));
        return allows(to, dx, dz);
    }
}
