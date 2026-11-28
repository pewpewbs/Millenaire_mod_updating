package org.millenaire.common.pathing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.millenaire.common.pathing.atomicstryker.AS_PathEntity;

/** A short escape leg for an already stalled swimmer. Called on the server thread. */
public final class ShoreRecovery {
    private static final int MAX_DISTANCE = 12;

    private ShoreRecovery() { }

    public static AS_PathEntity findPath(BlockGetter world, BlockPos feet, BlockPos destination) {
        if (!loaded(world, feet)) return null;
        BlockPos surface = feet;
        if (!sourceWater(world, surface)) {
            if (!world.getBlockState(surface).isAir()) return null;
            surface = surface.below();
        }
        if (!sourceWater(world, surface)) return null;
        // Feet can be submerged or bob just above the uppermost water block.
        for (int i = 0; i < 2 && sourceWater(world, surface.above()); i++) surface = surface.above();
        if (!clearAboveWater(world, surface)) return null;

        Direction bestDirection = null;
        BlockPos bestBank = null;
        BlockPos bestInland = null;
        int bestLength = 0;
        double bestCost = Double.POSITIVE_INFINITY;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int step = 1; step <= MAX_DISTANCE; step++) {
                BlockPos next = surface.relative(direction, step);
                if (!loaded(world, next)) break;
                if (sourceWater(world, next)) {
                    if (!clearAboveWater(world, next)) break;
                    continue;
                }
                BlockPos bank = landing(world, next.above());
                if (bank != null && (bank.getY() == surface.getY() + 1
                        || world.getBlockState(next.relative(direction.getOpposite()).above(3)).isAir())) {
                    BlockPos inland = landing(world, bank.relative(direction));
                    if (inland != null && (inland.getY() == bank.getY() || world.getBlockState(bank.above(2)).isAir())) {
                        double cost = step + Math.sqrt(inland.distSqr(destination));
                        if (cost < bestCost) {
                            bestCost = cost;
                            bestDirection = direction;
                            bestLength = step;
                            bestBank = bank;
                            bestInland = inland;
                        }
                    }
                }
                break; // A ray never continues through a bank or an obstruction.
            }
        }
        if (bestBank == null) return null;
        Node[] nodes = new Node[bestLength + 2];
        for (int i = 0; i < bestLength; i++) nodes[i] = node(surface.relative(bestDirection, i));
        nodes[bestLength] = node(bestBank);
        nodes[bestLength + 1] = node(bestInland);
        return new AS_PathEntity(nodes);
    }

    private static boolean loaded(BlockGetter world, BlockPos pos) {
        return !(world instanceof Level level) || level.hasChunkAt(pos);
    }

    private static boolean sourceWater(BlockGetter world, BlockPos pos) {
        var state = world.getBlockState(pos);
        return state.is(Blocks.WATER) && state.getFluidState().isSource();
    }

    private static boolean clearAboveWater(BlockGetter world, BlockPos pos) {
        return world.getBlockState(pos.above()).isAir() && world.getBlockState(pos.above(2)).isAir();
    }

    private static BlockPos landing(BlockGetter world, BlockPos lowest) {
        // Vanilla's hazard classification also samples the neighbouring columns.
        if (world instanceof Level level && !level.hasChunksAt(lowest.offset(-1, -1, -1), lowest.offset(1, 2, 1))) return null;
        for (int rise = 0; rise <= 1; rise++) {
            BlockPos stand = lowest.above(rise);
            BlockPos floor = stand.below();
            if (!world.getBlockState(floor).isCollisionShapeFullBlock(world, floor)
                    || !world.getBlockState(stand).getFluidState().isEmpty()
                    || !world.getBlockState(stand).getCollisionShape(world, stand).isEmpty()
                    || !world.getBlockState(stand.above()).isAir()) continue;
            var type = WalkNodeEvaluator.getBlockPathTypeStatic(world, stand.mutable());
            if (type == BlockPathTypes.WALKABLE || type == BlockPathTypes.WATER_BORDER) return stand;
        }
        return null;
    }

    private static Node node(BlockPos pos) {
        return new Node(pos.getX(), pos.getY(), pos.getZ());
    }
}
