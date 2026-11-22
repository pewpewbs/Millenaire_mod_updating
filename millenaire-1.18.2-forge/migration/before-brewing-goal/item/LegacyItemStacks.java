package org.millenaire.common.item;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.util.datafix.fixes.ItemStackTheFlatteningFix;

/** Converts the old item/damage pair at the boundary with modern item stacks. */
public final class LegacyItemStacks {
    public static final String VARIANT_TAG = "MillenaireVariant";
    private static final Map<String, String> LEGACY_FAMILIES = families();
    private LegacyItemStacks() {}

    private static Map<String, String> families() {
        Map<String, String> result = new HashMap<>();
        for (String name : new String[]{"stone", "dirt", "planks", "sapling", "sand", "log", "leaves", "sponge",
                "sandstone", "wool", "red_flower", "stone_slab", "wooden_slab", "stained_hardened_clay",
                "stained_glass", "stained_glass_pane", "leaves2", "log2", "prismarine", "carpet", "double_plant",
                "red_sandstone", "stone_slab2", "concrete", "concrete_powder", "coal", "golden_apple", "fish",
                "cooked_fish", "dye", "bed", "skull", "banner", "monster_egg", "stonebrick", "quartz_block"}) {
            String legacy = "minecraft:" + name;
            String base = ItemStackTheFlatteningFix.updateItem(legacy, 0);
            if (base != null) result.put(rename(base), legacy);
        }
        return Map.copyOf(result);
    }
    private static String rename(String id) {
        return switch (id) {
            case "minecraft:rose_red" -> "minecraft:red_dye";
            case "minecraft:cactus_green" -> "minecraft:green_dye";
            case "minecraft:dandelion_yellow" -> "minecraft:yellow_dye";
            case "minecraft:sign" -> "minecraft:oak_sign";
            default -> id;
        };
    }
    public static ItemStack stack(ItemLike source, int count, int metadata) {
        Item item = source.asItem();
        ItemStack stack = new ItemStack(item, count);
        if (metadata <= 0) return stack; // -1 is the old inventory wildcard, not a stack variant.
        if (stack.getMaxDamage() > 0) {
            stack.setDamageValue(metadata);
            return stack;
        }
        ResourceLocation id = Registry.ITEM.getKey(item);
        if (id.toString().equals("millenaire:bannerpattern")) {
            if (metadata >= 28) throw new IllegalArgumentException("Unknown legacy banner pattern: " + metadata);
            Item pattern = Registry.ITEM.getOptional(new ResourceLocation("millenaire", "bannerpattern_" + metadata))
                .orElseThrow(() -> new IllegalStateException("Banner pattern items are not registered yet"));
            return new ItemStack(pattern, count);
        }
        if (id.getNamespace().equals("minecraft")) {
            String legacy = LEGACY_FAMILIES.get(id.toString());
            String flattened = legacy == null ? null : ItemStackTheFlatteningFix.updateItem(legacy, metadata);
            if (flattened == null) throw new IllegalArgumentException("Unsupported legacy item variant " + id + ":" + metadata);
            ResourceLocation converted = new ResourceLocation(rename(flattened));
            Item modern = Registry.ITEM.getOptional(converted).orElseThrow(() -> new IllegalArgumentException("Unknown converted item " + converted));
            return new ItemStack(modern, count);
        }
        stack.getOrCreateTag().putInt(VARIANT_TAG, metadata);
        return stack;
    }
    public static int metadata(ItemStack stack) {
        if (stack.getTag() != null && stack.getTag().contains(VARIANT_TAG, 3))
            return stack.getTag().getInt(VARIANT_TAG);
        return stack.getMaxDamage() > 0 ? stack.getDamageValue() : 0;
    }
    public static int metadata(net.minecraft.world.level.block.state.BlockState state) {
        return state.getBlock() instanceof org.millenaire.common.block.LegacyVariantBlock variants
            ? variants.itemMetadata(state) : 0;
    }
    public static ItemStack fromLegacyId(String name, int count, int metadata) {
        ResourceLocation id = new ResourceLocation(name);
        if (id.getNamespace().equals("minecraft")) {
            String flattened = ItemStackTheFlatteningFix.updateItem(id.toString(), Math.max(0, metadata));
            if (flattened != null) {
                ResourceLocation converted = new ResourceLocation(rename(flattened));
                Item item = Registry.ITEM.getOptional(converted).orElseThrow(() -> new IllegalArgumentException("Unknown legacy item " + name));
                return new ItemStack(item, count);
            }
        }
        Item item = Registry.ITEM.getOptional(id).orElseThrow(() -> new IllegalArgumentException("Unknown item " + name));
        return stack(item, count, metadata);
    }
    public static boolean matches(ItemStack candidate, Item wanted, int metadata) {
        if (candidate == null || candidate.isEmpty()) return false;
        if (metadata != -1) {
            ItemStack expected = stack(wanted, 1, metadata);
            return candidate.is(expected.getItem()) && metadata(candidate) == metadata(expected);
        }
        if (candidate.is(wanted)) return true;
        ResourceLocation wantedId = Registry.ITEM.getKey(wanted);
        ResourceLocation candidateId = Registry.ITEM.getKey(candidate.getItem());
        if (wantedId.toString().equals("millenaire:bannerpattern") && candidateId.getNamespace().equals("millenaire")) {
            String path = candidateId.getPath();
            if (path.startsWith("bannerpattern_")) {
                try { int variant = Integer.parseInt(path.substring("bannerpattern_".length())); return variant > 0 && variant < 28; }
                catch (NumberFormatException ignored) { return false; }
            }
        }
        // Millenaire's "any log" requests include both old log families.
        if (wantedId.toString().equals("minecraft:oak_log") && candidateId.getNamespace().equals("minecraft")
                && (candidateId.getPath().endsWith("_log") || candidateId.getPath().endsWith("_wood"))) return true;
        String family = LEGACY_FAMILIES.get(wantedId.toString());
        if (family == null) return false;
        for (int value = 0; value < 16; value++) {
            String flattened = ItemStackTheFlatteningFix.updateItem(family, value);
            if (flattened != null && rename(flattened).equals(candidateId.toString())) return true;
        }
        return false;
    }
}
