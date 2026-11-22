package org.millenaire.common.utilities;

import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.millenaire.common.item.LegacyItemStacks;

/** Item identity in stored stocks. The version distinguishes reused old and modern names. */
public record SavedInventoryItem(Item item, int metadata) {
    public static SavedInventoryItem read(CompoundTag tag) {
        String namespace = tag.getString("itemmod");
        ResourceLocation id = new ResourceLocation(namespace.isEmpty() ? "minecraft" : namespace, tag.getString("item"));
        int meta = tag.getInt("meta");
        if (tag.contains("item_data_version")) {
            if (tag.getInt("item_data_version") != 2975)
                throw new IllegalArgumentException("Unsupported inventory item version: " + tag.getInt("item_data_version"));
            Item item = Registry.ITEM.getOptional(id).orElseThrow(() -> new IllegalArgumentException("Unknown inventory item: " + id));
            return new SavedInventoryItem(item, meta);
        }
        var stack = LegacyItemStacks.fromLegacyId(id.toString(), 1, meta);
        return new SavedInventoryItem(stack.getItem(), meta == -1 ? -1 : LegacyItemStacks.metadata(stack));
    }
    public static void write(CompoundTag tag, Item item, int metadata) {
        ResourceLocation id = Registry.ITEM.getKey(item);
        if (id == null || Registry.ITEM.getOptional(id).orElse(null) != item) throw new IllegalArgumentException("Cannot save unregistered inventory item");
        tag.putString("item", id.getPath());
        tag.putString("itemmod", id.getNamespace());
        tag.putInt("meta", metadata);
        tag.putInt("item_data_version", 2975);
    }
}
