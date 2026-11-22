package org.millenaire.common.utilities;

import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** Stored quarry/resource states: retain full modern properties, but read both old save formats. */
public final class ResourceSourceStateIO {
    private ResourceSourceStateIO() {}
    public static BlockState read(CompoundTag source) {
        if (source.contains("block_state", Tag.TAG_COMPOUND)) {
            CompoundTag state = source.getCompound("block_state");
            ResourceLocation id = ResourceLocation.tryParse(state.getString("Name"));
            if (id == null || !Registry.BLOCK.containsKey(id))
                throw new IllegalArgumentException("Unknown saved resource block: " + state.getString("Name"));
            return NbtUtils.readBlockState(state);
        }
        if (source.contains("block_rl", Tag.TAG_STRING))
            return LegacyBlockStates.fromName(source.getString("block_rl"), source.getInt("block_meta"));
        if (source.contains("type", Tag.TAG_ANY_NUMERIC))
            return LegacyBlockStates.fromLegacyId(source.getInt("type"), 0);
        throw new IllegalArgumentException("Resource source has no saved block state");
    }
    public static void write(CompoundTag source, BlockState state) {
        source.put("block_state", NbtUtils.writeBlockState(state));
        source.remove("block_rl");
        source.remove("block_meta");
        source.remove("type");
    }
}
