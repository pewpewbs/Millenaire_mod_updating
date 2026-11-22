package org.millenaire.common.block;

import net.minecraft.world.level.block.state.BlockState;

/** Explicit codec for Millenaire's pre-flattening block variants. */
public interface LegacyVariantBlock {
    BlockState func_176203_a(int metadata);
    int func_176201_c(BlockState state);
    /** Inventory identity excludes placement-only state such as upper half or road stability. */
    default int itemMetadata(BlockState state) { return func_176201_c(state); }
    /** Accept obsolete, single-valued properties when reading old building plans. */
    default boolean acceptsLegacyProperty(String name, String value) { return false; }
}
