package org.millenaire.common.pathing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.millenaire.common.block.IBlockPath;

/** A short, flat rectangle enclosing both the original bend and the swept body. */
public final class OpenGroundPassage {
    private OpenGroundPassage() {}

    public static int lookAhead(BlockGetter world, Path path, Vec3 feet, float width, float height) {
        int first = path.getNextNodeIndex();
        Node next = path.getNextNode();
        int floor = Mth.floor(feet.y - 0.01D);
        if (next.y != floor + 1 || Math.abs(next.x + 0.5 - feet.x) > 1.6
                || Math.abs(next.z + 0.5 - feet.z) > 1.6) return first;
        double minX = feet.x, maxX = feet.x, minZ = feet.z, maxZ = feet.z;
        int last = first;
        for (int i = first; i < Math.min(first + 4, path.getNodeCount()); i++) {
            Node node = path.getNode(i);
            if (node.y != next.y) break;
            if (i > first) {
                Node previous = path.getNode(i - 1);
                if (Math.abs(node.x - previous.x) > 1 || Math.abs(node.z - previous.z) > 1) break;
            }
            double x = node.x + 0.5, z = node.z + 0.5;
            if (Math.abs(x - feet.x) > 3.25 || Math.abs(z - feet.z) > 3.25) break;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
            last = i;
        }
        if (last == first) return first;

        double radius = width / 2.0D + 0.02D;
        int head = Mth.floor(feet.y + height - 1.0E-6D);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = Mth.floor(minX - radius); x <= Mth.floor(maxX + radius); x++) {
            for (int z = Mth.floor(minZ - radius); z <= Mth.floor(maxZ + radius); z++) {
                pos.set(x, floor, z);
                if (world instanceof Level level && !level.hasChunkAt(pos)) return first;
                BlockState support = world.getBlockState(pos);
                if (!ordinaryFloor(support) || !support.getFluidState().isEmpty()) return first;
                var shape = support.getCollisionShape(world, pos);
                if (!flatSupportAtHeight(shape, feet.y - floor))
                    return first;
                for (int y = floor + 1; y <= head; y++) {
                    if (!world.getBlockState(pos.set(x, y, z)).isAir()) return first;
                }
            }
        }
        return last;
    }

    private static boolean flatSupportAtHeight(VoxelShape shape, double height) {
        if (shape.isEmpty() || Math.abs(shape.max(Direction.Axis.Y) - height) > 0.025D) return false;
        if (shape == Shapes.block()) return true;
        // A modded slab or planks tag entry may have holes even at the correct height.
        var boxes = shape.toAabbs();
        if (boxes.size() != 1) return false;
        var box = boxes.get(0);
        return box.minX <= 0 && box.minZ <= 0 && box.maxX >= 1 && box.maxZ >= 1;
    }

    // Unknown floor materials retain their planned nodes.
    private static boolean ordinaryFloor(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.DIRT_PATH) || state.is(Blocks.GRAVEL) || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.TERRACOTTA)
                || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.PLANKS)
                || state.is(BlockTags.STONE_BRICKS) || state.getBlock() instanceof IBlockPath
                || state.getBlock() instanceof SlabBlock;
    }
}
