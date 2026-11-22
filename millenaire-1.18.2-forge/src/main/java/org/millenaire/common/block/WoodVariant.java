package org.millenaire.common.block;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** The six wood families used by Millenaire's existing plans and sod textures. */
public enum WoodVariant implements StringRepresentable {
    OAK("oak"), SPRUCE("spruce"), BIRCH("birch"), JUNGLE("jungle"), ACACIA("acacia"), DARK_OAK("dark_oak");

    private final String name;
    WoodVariant(String name) { this.name = name; }
    @Override public String getSerializedName() { return name; }
    @Override public String toString() { return name; }
    public int getMetadata() { return ordinal(); }
    public static WoodVariant byMetadata(int metadata) {
        return metadata < 0 || metadata >= values().length ? OAK : values()[metadata];
    }
    private Block block(String suffix) {
        ResourceLocation id = new ResourceLocation("minecraft", name + "_" + suffix);
        return Registry.BLOCK.getOptional(id).orElseThrow(() -> new IllegalStateException("Missing wood block " + id));
    }
    public Block planks() { return block("planks"); }
    public Block log() { return block("log"); }
    public Block leaves() { return block("leaves"); }
    public Block sapling() { return block("sapling"); }

    public static WoodVariant fromState(BlockState state) {
        Property<?> variant = state.getBlock().getStateDefinition().getProperty("variant");
        if (variant != null && state.getValue(variant) instanceof WoodVariant wood) return wood;
        ResourceLocation id = Registry.BLOCK.getKey(state.getBlock());
        if (!id.getNamespace().equals("minecraft")) return null;
        String path = id.getPath();
        if (path.startsWith("stripped_")) path = path.substring(9);
        for (WoodVariant wood : values()) if (path.startsWith(wood.name + "_")) return wood;
        return null;
    }

    /** Swap the family, then copy shared states such as axis, facing, half and waterlogging. */
    public BlockState replace(BlockState original) {
        Property<?> variant = original.getBlock().getStateDefinition().getProperty("variant");
        if (variant != null && original.getValue(variant) instanceof WoodVariant)
            return copyProperty(original, variant, this);
        WoodVariant previous = fromState(original);
        if (previous == null) return original;
        String path = Registry.BLOCK.getKey(original.getBlock()).getPath();
        String prefix = path.startsWith("stripped_") ? "stripped_" : "";
        String suffix = path.substring(prefix.length() + previous.name.length());
        ResourceLocation target = new ResourceLocation("minecraft", prefix + name + suffix);
        Block replacement = Registry.BLOCK.getOptional(target).orElse(null);
        if (replacement == null) return original;
        BlockState result = replacement.defaultBlockState();
        for (Property<?> property : original.getProperties()) {
            Property<?> destination = replacement.getStateDefinition().getProperty(property.getName());
            if (destination != null) result = copyProperty(result, destination, original.getValue(property));
        }
        return result;
    }
    private static <T extends Comparable<T>> BlockState copyProperty(BlockState state, Property<T> property, Object value) {
        if (property.getValueClass().isInstance(value)) {
            T typed = property.getValueClass().cast(value);
            if (property.getPossibleValues().contains(typed)) return state.setValue(property, typed);
        }
        return state;
    }
}
