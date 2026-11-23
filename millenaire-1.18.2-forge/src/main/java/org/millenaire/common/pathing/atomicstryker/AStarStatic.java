/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockLeaves
 *  net.minecraft.block.BlockWall
 *  net.minecraft.block.properties.IProperty
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.EntityLiving
 *  net.minecraft.init.Blocks
 *  net.minecraft.pathfinding.PathPoint
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.World
 */
package org.millenaire.common.pathing.atomicstryker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.millenaire.common.block.BlockMillWall;
import org.millenaire.common.pathing.atomicstryker.AS_PathEntity;
import org.millenaire.common.pathing.atomicstryker.AS_PathPoint;
import org.millenaire.common.pathing.atomicstryker.AStarConfig;
import org.millenaire.common.pathing.atomicstryker.AStarNode;
import org.millenaire.common.utilities.BlockItemUtilities;
import org.millenaire.common.utilities.BlockStateUtilities;
import org.millenaire.common.utilities.ThreadSafeUtilities;

public class AStarStatic {
    static final int[][] candidates = new int[][]{{0, 0, -1, 1}, {0, 0, 1, 1}, {0, 1, 0, 1}, {1, 0, 0, 1}, {-1, 0, 0, 1}, {1, 1, 0, 2}, {-1, 1, 0, 2}, {0, 1, 1, 2}, {0, 1, -1, 2}, {1, -1, 0, 1}, {-1, -1, 0, 1}, {0, -1, 1, 1}, {0, -1, -1, 1}};
    static final int[][] candidates_allowdrops = new int[][]{{0, 0, -1, 1}, {0, 0, 1, 1}, {1, 0, 0, 1}, {-1, 0, 0, 1}, {1, 1, 0, 2}, {-1, 1, 0, 2}, {0, 1, 1, 2}, {0, 1, -1, 2}, {1, -1, 0, 1}, {-1, -1, 0, 1}, {0, -1, 1, 1}, {0, -1, -1, 1}, {1, -2, 0, 1}, {-1, -2, 0, 1}, {0, -2, 1, 1}, {0, -2, -1, 1}};

    public static AStarNode[] getAccessNodesSorted(BlockGetter world, int workerX, int workerY, int workerZ, int posX, int posY, int posZ, AStarConfig config) throws ThreadSafeUtilities.ChunkAccessException {
        AStarNode check;
        ArrayList<AStarNode> resultList = new ArrayList<AStarNode>();
        for (int xIter = -2; xIter <= 2; ++xIter) {
            for (int zIter = -2; zIter <= 2; ++zIter) {
                for (int yIter = -3; yIter <= 2; ++yIter) {
                    check = new AStarNode(posX + xIter, posY + yIter, posZ + zIter, Math.abs(xIter) + Math.abs(yIter) + Math.abs(zIter), null);
                    if (!AStarStatic.isViable(world, check, 1, config)) continue;
                    resultList.add(check);
                }
            }
        }
        Collections.sort(resultList);
        int count = 0;
        AStarNode[] returnVal = new AStarNode[resultList.size()];
        while (!resultList.isEmpty() && (check = (AStarNode)resultList.get(0)) != null) {
            returnVal[count] = check;
            resultList.remove(0);
            ++count;
        }
        return returnVal;
    }

    public static double getDistanceBetweenCoords(int x, int y, int z, int posX, int posY, int posZ) {
        return Math.sqrt(Math.pow(x - posX, 2.0) + Math.pow(y - posY, 2.0) + Math.pow(z - posZ, 2.0));
    }

    public static double getDistanceBetweenNodes(AStarNode a, AStarNode b) {
        return Math.sqrt(Math.pow(a.x - b.x, 2.0) + Math.pow(a.y - b.y, 2.0) + Math.pow(a.z - b.z, 2.0));
    }

    public static double getEntityLandSpeed(Mob entLiving) {
        return entLiving.getDeltaMovement().horizontalDistance();
    }

    public static int getIntCoordFromDoubleCoord(double input) {
        return Mth.floor((double)input);
    }

    public static boolean isLadder(BlockGetter world, Block b, int x, int y, int z) throws ThreadSafeUtilities.ChunkAccessException {
        if (b != null) {
            var state = ThreadSafeUtilities.getBlockState(world, x, y, z);
            if (world instanceof net.minecraft.world.level.LevelReader reader)
                return b.isLadder(state, reader, new BlockPos(x, y, z), null);
            return state.is(net.minecraft.tags.BlockTags.CLIMBABLE);
        }
        return false;
    }

    public static boolean isPassableBlock(BlockGetter world, int ix, int iy, int iz, AStarConfig config) throws ThreadSafeUtilities.ChunkAccessException {
        Block blockBelow;
        BlockState blockState = ThreadSafeUtilities.getBlockState(world, ix, iy, iz);
        Block block = blockState.getBlock();
        if (org.millenaire.common.pathing.BarrierPathRules.isBarrier(blockState)
                || iy > world.getMinBuildHeight() && org.millenaire.common.pathing.BarrierPathRules.blocksStepAbove(
                    ThreadSafeUtilities.getBlockState(world, ix, iy - 1, iz))) return false;
        if (org.millenaire.common.pathing.ChestPathRules.isChest(block)
                || iy > world.getMinBuildHeight()
                && org.millenaire.common.pathing.ChestPathRules.isChest(
                    ThreadSafeUtilities.getBlock(world, ix, iy - 1, iz))) return false;
        if (iy > world.getMinBuildHeight() && (BlockItemUtilities.isFence(blockBelow = ThreadSafeUtilities.getBlock(world, ix, iy - 1, iz)) || blockBelow == Blocks.IRON_BARS || blockBelow == Blocks.NETHER_BRICK_FENCE || blockBelow instanceof WallBlock || blockBelow instanceof BlockMillWall)) {
            return false;
        }
        if (block != null) {
            if (!(config.canSwim || block != Blocks.WATER)) {
                return false;
            }
            if (config.canUseDoors && (BlockItemUtilities.isWoodenDoor(block) || BlockItemUtilities.isFenceGate(block))) {
                return true;
            }
            if (config.canClearLeaves && block instanceof LeavesBlock) {
                return !blockState.getValue(LeavesBlock.PERSISTENT);
            }
            return ThreadSafeUtilities.isBlockPassable(block, world, ix, iy, iz);
        }
        return true;
    }

    public static boolean isViable(BlockGetter world, AStarNode target, int yoffset, AStarConfig config) throws ThreadSafeUtilities.ChunkAccessException {
        return AStarStatic.isViable(world, target.x, target.y, target.z, yoffset, config);
    }

    public static boolean isViable(BlockGetter world, int x, int y, int z, int yoffset, AStarConfig config) throws ThreadSafeUtilities.ChunkAccessException {
        Block block = ThreadSafeUtilities.getBlock(world, x, y, z);
        Block blockBelow = ThreadSafeUtilities.getBlock(world, x, y - 1, z);
        if (block == Blocks.LADDER && AStarStatic.isPassableBlock(world, x, y + 1, z, config)) {
            return true;
        }
        if (!AStarStatic.isPassableBlock(world, x, y, z, config) || !AStarStatic.isPassableBlock(world, x, y + 1, z, config)) {
            return false;
        }
        if (blockBelow == Blocks.WATER) {
            return false;
        }
        if (AStarStatic.isPassableBlock(world, x, y - 1, z, config)) {
            if (!config.canSwim) {
                return false;
            }
            if (block != Blocks.WATER) {
                return false;
            }
        }
        if (yoffset < 0) {
            yoffset *= -1;
        }
        for (int ycheckhigher = 1; ycheckhigher <= yoffset; ++ycheckhigher) {
            if (AStarStatic.isPassableBlock(world, x, y + ycheckhigher, z, config)) continue;
            return false;
        }
        return true;
    }

    /**
     * Validates the whole A->B motion rather than merely checking if B is a
     * viable integer node. JPS also runs this on every expanded unit edge
     * before publishing its path. Shape queries stay on detached snapshots
     * during worker searches; they never touch a live Level off-thread.
     */
    public static boolean isTransitionViable(BlockGetter world, AStarNode from,
                                             AStarNode to, AStarConfig config)
            throws ThreadSafeUtilities.ChunkAccessException {
        int dx = Math.abs(to.x - from.x);
        int dz = Math.abs(to.z - from.z);
        if (dx == 0 && dz == 0) {
            // Vertical movement is checked by the consecutive-rung ladder
            // expansion, not by horizontal swept collision.
            return Math.abs(to.y - from.y) == 1;
        }
        if (dx > 1 || dz > 1) return false;
        // Most flat, axial moves are between two columns of air on full
        // blocks. Their body stays between those columns; avoid voxel-shape
        // sampling on every ordinary A* expansion.
        if (to.y == from.y && dx + dz == 1
                && isSimpleFlatColumn(world, from)
                && isSimpleFlatColumn(world, to)) {
            return true;
        }
        return TransitionClearanceRules.canTraverse(
                from.x, from.y, from.z, to.x, to.y, to.z,
                (centerX, feetY, centerZ) -> isBodyCollisionFree(
                        world, centerX, feetY, centerZ, config));
    }

    private static boolean isSimpleFlatColumn(BlockGetter world, AStarNode node) {
        BlockPos feet = new BlockPos(node.x, node.y, node.z);
        BlockPos head = feet.above();
        BlockPos floor = feet.below();
        // No fractional supports, doors, stair faces or overhangs involved.
        return world.getBlockState(feet).isAir()
                && world.getBlockState(head).isAir()
                && Block.isShapeFullBlock(world.getBlockState(floor)
                        .getCollisionShape(world, floor));
    }

    private static boolean isBodyCollisionFree(BlockGetter world, double x,
                                               double feetY, double z,
                                               AStarConfig config) {
        double halfWidth = TransitionClearanceRules.VILLAGER_WIDTH * 0.5D;
        AABB body = new AABB(
                x - halfWidth, feetY, z - halfWidth,
                x + halfWidth, feetY + TransitionClearanceRules.VILLAGER_HEIGHT,
                z + halfWidth);
        // The strict intersection test treats touching a supporting surface
        // as safe. The 0.625-block step test adds a margin above half slabs.
        int minX = net.minecraft.util.Mth.floor(body.minX);
        int maxX = net.minecraft.util.Mth.floor(body.maxX - 1.0E-7D);
        int minY = net.minecraft.util.Mth.floor(body.minY);
        int maxY = net.minecraft.util.Mth.floor(body.maxY - 1.0E-7D);
        int minZ = net.minecraft.util.Mth.floor(body.minZ);
        int maxZ = net.minecraft.util.Mth.floor(body.maxZ - 1.0E-7D);
        for (int bx = minX; bx <= maxX; bx++) {
            for (int bz = minZ; bz <= maxZ; bz++) {
                for (int by = minY; by <= maxY; by++) {
                    BlockPos pos = new BlockPos(bx, by, bz);
                    // The main-thread path translator can receive a stale
                    // route. Do not synchronously load chunks to validate it.
                    if (world instanceof Level level && !level.hasChunkAt(pos)) return false;
                    BlockState state = world.getBlockState(pos);
                    if (state.isAir()) continue;
                    Block block = state.getBlock();
                    // The villager opens doors/gates as a separate action.
                    if (config.canUseDoors
                            && (BlockItemUtilities.isWoodenDoor(block)
                                || BlockItemUtilities.isFenceGate(block))) continue;
                    if (config.canClearLeaves && block instanceof LeavesBlock
                            && !state.getValue(LeavesBlock.PERSISTENT)) continue;
                    var shape = state.getCollisionShape(world, pos);
                    if (shape.isEmpty()) continue;
                    for (AABB part : shape.toAabbs()) {
                        if (body.intersects(part.move(bx, by, bz))) return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * JPS may use diagonal jump points even when the final path must be
     * orthogonal. The inserted corner has to be an actual two-edge route.
     */
    public static AStarNode findSafeOrthogonalCorner(BlockGetter world,
            AStarNode from, AStarNode to, AStarConfig config)
            throws ThreadSafeUtilities.ChunkAccessException {
        if (from.y != to.y || Math.abs(from.x - to.x) != 1
                || Math.abs(from.z - to.z) != 1) return null;
        AStarNode first = new AStarNode(from.x, from.y, to.z, 0, null);
        if (isViable(world, first, 0, config)
                && isTransitionViable(world, from, first, config)
                && isTransitionViable(world, first, to, config)) return first;
        AStarNode second = new AStarNode(to.x, from.y, from.z, 0, null);
        if (isViable(world, second, 0, config)
                && isTransitionViable(world, from, second, config)
                && isTransitionViable(world, second, to, config)) return second;
        return null;
    }

    public static AS_PathEntity translateAStarPathtoPathEntity(BlockGetter world, List<AStarNode> input, AStarConfig config) throws ThreadSafeUtilities.ChunkAccessException {
        input = new ArrayList<>(input);
        if (!config.canTakeDiagonals) {
            List<AStarNode> oldInput = input;
            input = new ArrayList<AStarNode>();
            for (int i = 0; i < oldInput.size() - 1; ++i) {
                AStarNode newNode;
                input.add(oldInput.get(i));
                if (oldInput.get((int)i).x == oldInput.get((int)(i + 1)).x || oldInput.get((int)i).z == oldInput.get((int)(i + 1)).z || oldInput.get((int)i).y != oldInput.get((int)(i + 1)).y) continue;
                // The source list is reversed (goal to start). Validate the
                // inserted elbow in the actual traversal direction.
                newNode = findSafeOrthogonalCorner(
                        world, oldInput.get(i + 1), oldInput.get(i), config);
                if (newNode != null) input.add(newNode);
            }
            if (!oldInput.isEmpty()) input.add(oldInput.get(oldInput.size() - 1));
        }
        AS_PathPoint[] points = new AS_PathPoint[input.size()];
        int i = 0;
        int size = input.size();
        while (size > 0) {
            AStarNode reading = input.get(size - 1);
            points[i] = new AS_PathPoint(reading.x, reading.y, reading.z);
            points[i].setIndex(i);
            points[i].setTotalPathDistance(i);
            points[i].setDistanceToNext(1.0f);
            points[i].setDistanceToTarget(size);
            if (i > 0) {
                points[i].setPrevious(points[i - 1]);
            }
            input.remove(size - 1);
            --size;
            ++i;
        }
        return new AS_PathEntity(points);
    }
}

