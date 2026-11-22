/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.client.multiplayer.WorldClient
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.World
 *  net.minecraft.world.WorldServer
 */
package org.millenaire.common.utilities;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

public class ThreadSafeUtilities {
    public static Block getBlock(Level world, int x, int y, int z) throws ChunkAccessException {
        ThreadSafeUtilities.validateCoords(world, x, z);
        return world.getBlockState(new BlockPos(x, y, z)).getBlock();
    }

    public static BlockState getBlockState(Level world, int x, int y, int z) throws ChunkAccessException {
        ThreadSafeUtilities.validateCoords(world, x, z);
        return world.getBlockState(new BlockPos(x, y, z));
    }

    public static boolean isBlockPassable(Block block, Level world, int x, int y, int z) throws ChunkAccessException {
        ThreadSafeUtilities.validateCoords(world, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        return world.getBlockState(pos).isPathfindable(world, pos, net.minecraft.world.level.pathfinder.PathComputationType.LAND);
    }

    public static boolean isChunkAtGenerated(Level world, int x, int z) {
        return WorldUtilities.isChunkGenerated(world, x >> 4, z >> 4);
    }

    public static boolean isChunkAtLoaded(Level world, int x, int z) {
        if (world instanceof ServerLevel) {
            return ((ServerLevel)world).getChunkSource().hasChunk(x >> 4, z >> 4);
        }
        if (world instanceof ClientLevel) {
            return ((ClientLevel)world).getChunkSource().hasChunk(x >> 4, z >> 4);
        }
        return false;
    }

    private static void validateCoords(Level world, int x, int z) throws ChunkAccessException {
        if (world instanceof ServerLevel) {
            if (!((ServerLevel)world).getChunkSource().hasChunk(x >> 4, z >> 4)) {
                throw new ChunkAccessException("Attempting to access a coordinate in an unloaded chunk within a thread in a server world at " + x + "/" + z + ".", x, z);
            }
        } else if (world instanceof ClientLevel && !((ClientLevel)world).getChunkSource().hasChunk(x >> 4, z >> 4)) {
            throw new ChunkAccessException("Attempting to access a coordinate in an unloaded chunk within a thread in a client world at " + x + "/" + z + ".", x, z);
        }
    }

    public static class ChunkAccessException
    extends Exception {
        private static final long serialVersionUID = -7650231135028039490L;
        public final int x;
        public final int z;

        public ChunkAccessException(String message, int x, int z) {
            super(message);
            this.x = x;
            this.z = z;
        }
    }
}

