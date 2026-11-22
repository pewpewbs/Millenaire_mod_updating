package org.millenaire.common.item;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.millenaire.common.block.LegacyVariantBlock;
import org.millenaire.common.block.MillBlocks;

public class ItemBlockMeta extends BlockItem {
    public ItemBlockMeta(Block block) {
        super(block, new Item.Properties().tab(MillBlocks.tabMillenaire));
        if (!(block instanceof IMetaBlockName)) throw new IllegalArgumentException("Block has no variant names: " + block);
    }
    @Override
    public String getDescriptionId(ItemStack stack) { return ((IMetaBlockName)getBlock()).getSpecialName(stack).replace("tile.millenaire.", "block.millenaire."); }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState placed = super.getPlacementState(context);
        if (placed == null || !(getBlock() instanceof LegacyVariantBlock variants)) return placed;
        BlockState encoded = variants.func_176203_a(LegacyItemStacks.metadata(context.getItemInHand()));
        for (Property<?> property : encoded.getProperties()) {
            // Placement geometry is determined by the clicked face and player, not inventory metadata.
            if (java.util.Set.of("facing", "axis", "half", "waterlogged", "shape").contains(property.getName())) continue;
            if (property == net.minecraft.world.level.block.SlabBlock.TYPE) continue;
            if (placed.hasProperty(property)) placed = copy(placed, encoded, property);
        }
        return placed;
    }
    private static <T extends Comparable<T>> BlockState copy(BlockState target, BlockState source, Property<T> property) {
        return target.setValue(property, source.getValue(property));
    }
}
