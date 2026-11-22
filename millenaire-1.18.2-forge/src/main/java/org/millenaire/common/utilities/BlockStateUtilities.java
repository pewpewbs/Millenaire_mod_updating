package org.millenaire.common.utilities;

import javax.annotation.Nullable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.millenaire.common.block.WoodVariant;

public class BlockStateUtilities {
    public static BlockState getBlockStateWithValues(BlockState state, String values) {
        if ("default".equals(values)) return state.getBlock().defaultBlockState();
        BlockState result = state;
        for (String entry : values.split(",")) {
            String[] pair = entry.split("=", 2);
            Property<?> property = pair.length == 2 ? state.getBlock().getStateDefinition().getProperty(pair[0]) : null;
            if (pair.length == 2 && result.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock) {
                if (pair[0].equals("decayable") && (pair[1].equals("true") || pair[1].equals("false"))) {
                    result = result.setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, !Boolean.parseBoolean(pair[1])); continue;
                }
                if (pair[0].equals("check_decay") && (pair[1].equals("true") || pair[1].equals("false"))) {
                    result = result.setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE, Boolean.parseBoolean(pair[1]) ? 7 : 1); continue;
                }
            }
            // Old decorative slab plans use half=top/bottom; vanilla slabs now call this property type.
            if (property == null && pair.length == 2 && pair[0].equals("half")
                    && state.getBlock() instanceof net.minecraft.world.level.block.SlabBlock
                    && (pair[1].equals("top") || pair[1].equals("bottom")))
                property = net.minecraft.world.level.block.SlabBlock.TYPE;
            if (property == null && pair.length == 2 && state.getBlock() instanceof org.millenaire.common.block.LegacyVariantBlock legacy
                    && legacy.acceptsLegacyProperty(pair[0], pair[1])) continue;
            if (property == null || (result = parseValue(result, property, pair[1])) == null) {
                MillLog.error(null, "Could not parse values line of " + values + " for block " + state.getBlock());
                return state;
            }
        }
        return result;
    }
    @Nullable
    private static <T extends Comparable<T>> BlockState parseValue(BlockState state, Property<T> property, String value) {
        return property.getValue(value).map(parsed -> state.setValue(property, parsed)).orElse(null);
    }
    public static WoodVariant getPlankVariant(BlockState state) { return WoodVariant.fromState(state); }
    @Nullable
    public static Comparable<?> getPropertyValueByName(BlockState state, String name) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        return property == null ? null : state.getValue(property);
    }
    public static String getStringFromBlockState(BlockState state) {
        StringBuilder result = new StringBuilder("v2975/").append(state.getBlock().getRegistryName().toString()).append(';');
        boolean first = true;
        for (Property<?> property : state.getProperties()) {
            if (!first) result.append(',');
            first = false;
            result.append(property.getName()).append('=').append(valueName(state, property));
        }
        return result.toString();
    }
    private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }
    public static boolean hasPropertyByName(BlockState state, String name) {
        return state.getBlock().getStateDefinition().getProperty(name) != null;
    }
    @Nullable
    public static BlockState setPropertyValueByName(BlockState state, String name, Comparable<?> value) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        return property == null ? null : setValue(state, property, value);
    }
    @Nullable
    private static <T extends Comparable<T>> BlockState setValue(BlockState state, Property<T> property, Object value) {
        if (!property.getValueClass().isInstance(value)) return null;
        T typed = property.getValueClass().cast(value);
        return property.getPossibleValues().contains(typed) ? state.setValue(property, typed) : null;
    }
}
