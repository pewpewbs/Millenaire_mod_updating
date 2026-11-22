package org.millenaire.common.utilities;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.*;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;

/** Culture configuration banners use the 1.12 dye order and Base tag. */
public final class LegacyBannerItems {
    private LegacyBannerItems() {}
    public static ItemStack fromConfig(String snbt) throws CommandSyntaxException {
        CompoundTag tag = TagParser.parseTag(snbt);
        rename(tag, "BlockEntityTag");
        CompoundTag banner = tag.getCompound("BlockEntityTag");
        rename(banner, "Base");
        rename(banner, "Patterns");
        if (banner.contains("Base", Tag.TAG_ANY_NUMERIC)) banner.putInt("Base", normalizeColor(banner.getInt("Base")));
        for (Tag entry : banner.getList("Patterns", Tag.TAG_COMPOUND)) {
            CompoundTag pattern = (CompoundTag)entry;
            rename(pattern, "Pattern");
            rename(pattern, "Color");
            if (pattern.contains("Color", Tag.TAG_ANY_NUMERIC)) pattern.putInt("Color", normalizeColor(pattern.getInt("Color")));
        }
        CompoundTag oldItem = new CompoundTag();
        oldItem.putString("id", "minecraft:banner");
        oldItem.putByte("Count", (byte)1);
        oldItem.putShort("Damage", (short)banner.getInt("Base"));
        banner.remove("Base");
        oldItem.put("tag", tag);
        Dynamic<?> fixed = DataFixers.getDataFixer().update(References.ITEM_STACK,
                new Dynamic<>(NbtOps.INSTANCE, oldItem), 1343, 2975);
        ItemStack stack = ItemStack.of((CompoundTag)fixed.convert(NbtOps.INSTANCE).getValue());
        if (!(stack.getItem() instanceof BannerItem)) throw new IllegalArgumentException("Could not convert culture banner");
        return stack;
    }
    /** Build an already-modern banner; do not invert these colours a second time. */
    public static ItemStack create(net.minecraft.world.item.DyeColor base, ListTag patterns) {
        ItemStack stack = new ItemStack(net.minecraft.world.level.block.BannerBlock.byColor(base));
        if (patterns != null && !patterns.isEmpty())
            stack.getOrCreateTagElement("BlockEntityTag").put("Patterns", patterns.copy());
        return stack;
    }
    private static int normalizeColor(int value) {
        // EnumDyeColor.byDyeDamage used black for invalid values. Two bundled Inuit banners use 38.
        return value < 0 || value > 15 ? 0 : value;
    }
    private static void rename(CompoundTag tag, String canonical) {
        for (String key : java.util.List.copyOf(tag.getAllKeys())) {
            if (!key.equals(canonical) && key.equalsIgnoreCase(canonical)) {
                if (!tag.contains(canonical)) tag.put(canonical, tag.get(key));
                tag.remove(key);
            }
        }
    }
}

