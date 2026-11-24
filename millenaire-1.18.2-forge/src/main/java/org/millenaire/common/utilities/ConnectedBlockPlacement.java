package org.millenaire.common.utilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.common.block.BlockMillWall;

/**
 * Programmatic placement has no BlockPlaceContext. The geometry of a
 * connected block must be derived from actual neighbouring states, rather
 * than the disconnected state stored in a legacy building plan.
 *
 * Only passive structural shapes are normalised here. Never invoke
 * placement handlers for containers, doors, redstone, plants or rails:
 * they have their own lifecycle and may create, drop or remove blocks.
 */
public final class ConnectedBlockPlacement {
    private ConnectedBlockPlacement() {}
    private static final Direction[] DIRECTIONS = Direction.values();

    public static boolean isConnectedStructure(BlockState state) {
        Block block = state.getBlock();
        // CrossCollisionBlock includes FenceBlock and IronBarsBlock, hence
        // vanilla panes AND Millenaire stained glass, bars and rosettes.
        // Subclasses of stairs and the painted custom walls are covered too.
        return block instanceof CrossCollisionBlock
                || block instanceof WallBlock
                || block instanceof BlockMillWall
                || block instanceof FenceGateBlock
                || block instanceof StairBlock;
    }

    public static BlockState resolve(Level world, BlockPos pos, BlockState state) {
        if (!isConnectedStructure(state) || world.isClientSide
                || world.isOutsideBuildHeight(pos) || !world.isAreaLoaded(pos, 1)) return state;
        // Use each block's actual updateShape implementation, including
        // Millenaire-specific connection and rosette rules. Do not invent
        // connections between blocks that vanilla refuses to connect.
        BlockState shaped = Block.updateFromNeighbourShapes(state, world, pos);
        return shaped.getBlock() == state.getBlock() ? shaped : state;
    }

    public static boolean matchesPlan(Level world, BlockPos pos, BlockState actual, BlockState planned) {
        if (actual.getBlock() != planned.getBlock()) return false;
        return actual.equals(resolve(world, pos, planned));
    }

    public static boolean place(Level world, BlockPos pos, BlockState planned, boolean notify) {
        if (planned == null || world.isOutsideBuildHeight(pos) || !Level.isInSpawnableBounds(pos)) return false;
        BlockState old = world.getBlockState(pos);
        BlockState target = resolve(world, pos, planned);
        // Flags 2/3 retain Minecraft's usual shape propagation. Never use
        // UPDATE_KNOWN_SHAPE (16), which would suppress that propagation.
        boolean changed = world.setBlock(pos, target, notify ? 3 : 2);
        if (!world.isClientSide && (changed || isConnectedStructure(old) || isConnectedStructure(target))) {
            refresh(world, pos);
            // A replacement/removal can disconnect an existing neighbour.
            // Even a no-op placement of a connector can heal a stale neighbour.
            for (Direction direction : DIRECTIONS) refresh(world, pos.relative(direction));
        }
        return changed;
    }

    private static void refresh(Level world, BlockPos pos) {
        // No forcing adjacent chunks to load for cosmetic maintenance.
        if (world.isOutsideBuildHeight(pos) || !world.hasChunkAt(pos)) return;
        BlockState actual = world.getBlockState(pos);
        if (!isConnectedStructure(actual)) return;
        BlockState shaped = resolve(world, pos, actual);
        if (!shaped.equals(actual)) {
            // Geometry only; preserve block identity, orientation, material,
            // NBT-bearing neighbours and ordinary update semantics.
            world.setBlock(pos, shaped, 2);
        }
    }
}
