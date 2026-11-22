package org.millenaire.common.buildingplan;
import java.io.*;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import org.millenaire.common.block.LegacyVariantBlock;
import org.millenaire.common.utilities.LegacyBlockStates;
import org.millenaire.common.utilities.LegacyFlowerPots;

/** Versioned construction records preserve complete states; old numeric IDs are vanilla only. */
public final class BuildingBlockStateIO {
    private static final int FORMAT = -2975;
    public record Entry(BlockState state, byte markerMetadata, byte special) {}
    private BuildingBlockStateIO() {}
    public static Entry read(DataInput input) throws IOException {
        int format = input.readInt();
        try {
            if (format == FORMAT) {
                CompoundTag tag = NbtIo.read(input, new NbtAccounter(1048576L));
                ResourceLocation name = tag == null ? null : ResourceLocation.tryParse(tag.getString("Name"));
                if (name == null || !Registry.BLOCK.containsKey(name)) throw new IOException("Unknown saved construction block: " + name);
                BlockState state = NbtUtils.readBlockState(tag);
                if (!NbtUtils.writeBlockState(state).equals(tag)) throw new IOException("Invalid or unsupported construction properties: " + tag);
                return new Entry(state, input.readByte(), input.readByte());
            }
            if (format < 0) throw new IOException("Unknown construction state format: " + format);
            byte metadata = input.readByte();
            byte special = input.readByte();
            return new Entry(LegacyBlockStates.fromLegacyId(format, metadata), metadata, special);
        } catch (IllegalArgumentException error) { throw new IOException("Cannot decode construction block " + format, error); }
    }
    public static void write(DataOutput output, BlockState state, byte metadata, byte special) throws IOException {
        output.writeInt(FORMAT);
        NbtIo.write(NbtUtils.writeBlockState(state), output);
        output.writeByte(metadata);
        output.writeByte(special);
    }
    public static BlockState fromMarker(Block block, int metadata) {
        if (block instanceof FlowerPotBlock) return LegacyFlowerPots.fromPlan(metadata);
        if (block instanceof LegacyVariantBlock legacy) return legacy.func_176203_a(metadata);
        if (block instanceof LadderBlock) {
            Direction facing = Direction.from3DDataValue(metadata);
            return block.defaultBlockState().setValue(LadderBlock.FACING, facing.getAxis().isHorizontal() ? facing : Direction.NORTH);
        }
        if (block instanceof StairBlock) {
            Direction facing = switch (metadata & 3) {case 0 -> Direction.EAST;case 1 -> Direction.WEST;case 2 -> Direction.SOUTH;default -> Direction.NORTH;};
            return block.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, (metadata & 4) == 0 ? Half.BOTTOM : Half.TOP);
        }
        if (metadata == 0) return block.defaultBlockState();
        return LegacyBlockStates.fromName(Registry.BLOCK.getKey(block).toString(), metadata);
    }
    /** Compatibility byte for auto-guess markers only; full state is stored separately. */
    public static byte markerMetadata(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof FlowerPotBlock) return 0; // Pot contents are part of the full state.
        if (block instanceof LegacyVariantBlock legacy) return (byte)legacy.func_176201_c(state);
        if (block instanceof LadderBlock) return (byte)state.getValue(LadderBlock.FACING).get3DDataValue();
        if (block instanceof StairBlock) {
            int facing = switch(state.getValue(StairBlock.FACING)) {case EAST -> 0;case WEST -> 1;case SOUTH -> 2;default -> 3;};
            return (byte)(facing | (state.getValue(StairBlock.HALF) == Half.TOP ? 4 : 0));
        }
        return 0;
    }
}

