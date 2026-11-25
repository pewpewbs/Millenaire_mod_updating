package org.millenaire.common.pathing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.common.entity.MillVillager;

/** Emergency escape only: no route failure, entity crowd or door is proof of entombment. */
public final class PhysicalTrapRecovery {
    private PhysicalTrapRecovery() {}

    private static boolean fullBlock(Level level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockState(pos).isCollisionShapeFullBlock(level, pos);
    }

    private static boolean surrounded(Level level, BlockPos feet) {
        if (!fullBlock(level, feet.below())) return false;
        boolean lowRoof = fullBlock(level, feet.above(2));
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos wall = feet.relative(side);
            if (!fullBlock(level, wall) || (!lowRoof && !fullBlock(level, wall.above()))) return false;
        }
        return true;
    }

    public static boolean isTrapped(MillVillager villager) {
        // Sleeping models intentionally lie across their standing collision box.
        // Do not relocate an NPC while it is in that pose; waking rechecks normally.
        if (villager.isVillagerSleeping()) return false;
        if (villager.isInWall()) return true;
        // A full two-block wall (or a one-block wall with a low solid roof)
        // is qualitatively different from a blocked route through a doorway.
        BlockPos feet = villager.blockPosition();
        return !villager.isVillagerSleeping()
                && Math.abs(villager.getY() - feet.getY()) < 0.1D
                && surrounded(villager.level, feet);
    }

    private static boolean hazardous(BlockState state) {
        return state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS)
                || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE)
                || state.is(Blocks.POWDER_SNOW);
    }

    private static boolean safe(MillVillager villager, BlockPos pos) {
        Level level = villager.level;
        if (pos.getY() <= level.getMinBuildHeight() || pos.getY() + 2 >= level.getMaxBuildHeight()
                || !level.hasChunkAt(pos)) return false;
        BlockPos below = pos.below();
        BlockState floor = level.getBlockState(below);
        if (!floor.isFaceSturdy(level, below, Direction.UP) || hazardous(floor)
                || hazardous(level.getBlockState(pos)) || hazardous(level.getBlockState(pos.above()))) return false;
        var box = villager.getBoundingBox().move(pos.getX() + 0.5D - villager.getX(),
                pos.getY() - villager.getY(), pos.getZ() + 0.5D - villager.getZ());
        // Do not place a villager in another entity, a liquid or another sealed cell.
        return level.noCollision(villager, box) && !level.containsAnyLiquid(box)
                && !surrounded(level, pos);
    }

    public static BlockPos findExit(MillVillager villager) {
        BlockPos origin = villager.blockPosition();
        BlockPos best = null;
        int bestDistance = Integer.MAX_VALUE;
        // Only search a local loaded region. No remote task teleport or forced chunk generation.
        for (int radius = 1; radius <= 6 && radius * radius < bestDistance; radius++) {
            for (int dx = -radius; dx <= radius; dx++)
                for (int dz = -radius; dz <= radius; dz++)
                    for (int dy = -radius; dy <= radius; dy++) {
                        if (Math.max(Math.max(Math.abs(dx), Math.abs(dz)), Math.abs(dy)) != radius) continue;
                        int distance = dx * dx + dy * dy + dz * dz;
                        if (distance >= bestDistance) continue;
                        BlockPos candidate = origin.offset(dx, dy, dz);
                        if (!safe(villager, candidate)) continue;
                        best = candidate;
                        bestDistance = distance;
                    }
        }
        return best;
    }
}
