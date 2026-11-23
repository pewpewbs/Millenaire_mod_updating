package org.millenaire.common.pathing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.common.utilities.BlockItemUtilities;

/**
 * Matches wooden-door transitions to the PHYSICAL block state.
 *
 * Normal crossings require opening the door. Sideways travel across the
 * middle of the SAME block may be passable while the leaf is CLOSED, but
 * becomes blocked when it swings across that sideways lane. DOOR-13
 * mistakenly rejected such traversals regardless of state, making NPCs
 * walk around entrances rather than using otherwise passable corridors.
 *
 * The interaction controller must not automatically OPEN a door when the
 * route specifically relies on the CLOSED leaf for a sideways passage.
 * Fence gates, ladders and non-door blocks are untouched.
 */
public final class DoorTransitionRules {
    private DoorTransitionRules() {}

    private static boolean allows(BlockState state, int dx, int dz) {
        if (!BlockItemUtilities.isWoodenDoor(state.getBlock())) return true;
        boolean eastWestFacing =
                state.getValue(DoorBlock.FACING).getAxis() == Direction.Axis.X;
        return DoorApproachRules.canTraverseDoorEdge(dx, dz, eastWestFacing,
                state.getValue(DoorBlock.OPEN));
    }

    /**
     * Path-node lookahead must only OPEN a wooden door when it intends
     * to cross the closed panel. Opening a closed sideways-passable door
     * would swing the leaf directly into the resident's planned lane.
     */
    public static boolean needsOpeningForEdge(BlockState state, int stepX, int stepZ) {
        if (!BlockItemUtilities.isWoodenDoor(state.getBlock())) return true;
        if (stepX == 0 && stepZ == 0) return false;
        boolean eastWestFacing =
                state.getValue(DoorBlock.FACING).getAxis() == Direction.Axis.X;
        return DoorApproachRules.followsDoorNormal(stepX, stepZ, eastWestFacing);
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
