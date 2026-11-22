package org.millenaire.common.utilities;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import org.millenaire.common.block.LegacyVariantBlock;
import org.millenaire.common.buildingplan.BuildingBlockStateIO;

/** Narrow compatibility for remaining age, bed, chest and sign callers.
 * Building storage/export use full states; unsupported conversions fail explicitly. */
public final class LegacyWorldMetadata {
    private LegacyWorldMetadata() {}
    public static int read(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof LegacyVariantBlock legacy) return legacy.func_176201_c(state);
        if (block instanceof CropBlock) {
            for (var property : state.getProperties())
                if (property instanceof net.minecraft.world.level.block.state.properties.IntegerProperty age && age.getName().equals("age"))
                    return state.getValue(age);
        }
        if (block instanceof NetherWartBlock) return state.getValue(NetherWartBlock.AGE);
        if (block instanceof BedBlock) return state.getValue(BedBlock.FACING).get2DDataValue()
            | (state.getValue(BedBlock.OCCUPIED) ? 4 : 0) | (state.getValue(BedBlock.PART) == BedPart.HEAD ? 8 : 0);
        if (block instanceof ChestBlock) return state.getValue(ChestBlock.FACING).get3DDataValue();
        if (block instanceof WallSignBlock) return state.getValue(WallSignBlock.FACING).get3DDataValue();
        if (block instanceof StandingSignBlock) return state.getValue(StandingSignBlock.ROTATION);
        if (block instanceof StairBlock || block instanceof LadderBlock) return BuildingBlockStateIO.markerMetadata(state);
        if (state.getValues().isEmpty()) return 0;
        throw new IllegalArgumentException("No legacy metadata representation for " + state);
    }
    public static BlockState write(Block block, int metadata) {
        if (block instanceof WallSignBlock) {
            var facing = net.minecraft.core.Direction.from3DDataValue(metadata);
            if (facing.getAxis().isVertical()) facing = net.minecraft.core.Direction.NORTH;
            return block.defaultBlockState().setValue(WallSignBlock.FACING, facing);
        }
        if (block instanceof StandingSignBlock) return block.defaultBlockState().setValue(StandingSignBlock.ROTATION, metadata & 15);
        return BuildingBlockStateIO.fromMarker(block, metadata);
    }
}
