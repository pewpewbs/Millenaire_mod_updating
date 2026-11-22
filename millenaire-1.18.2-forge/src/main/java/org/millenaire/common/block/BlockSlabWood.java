package org.millenaire.common.block;

import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.storage.loot.LootContext;
import org.millenaire.common.item.IMetaBlockName;
import org.millenaire.common.item.LegacyItemStacks;

public class BlockSlabWood extends SlabBlock implements IMetaBlockName, LegacyVariantBlock {
    public static final EnumProperty<BlockDecorativeWood.EnumType> VARIANT = EnumProperty.create("variant", BlockDecorativeWood.EnumType.class);
    public BlockSlabWood(String name) {
        super(Properties.of(Material.WOOD, state -> state.getValue(VARIANT).getMapColor()).strength(2.0F, 6.0F).sound(SoundType.WOOD));
        setRegistryName("millenaire", name);
        registerDefaultState(defaultBlockState().setValue(VARIANT, BlockDecorativeWood.EnumType.TIMBERFRAMEPLAIN));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); builder.add(VARIANT); }
    @Override public BlockState func_176203_a(int metadata) { return defaultBlockState().setValue(VARIANT, BlockDecorativeWood.EnumType.byMetadata(metadata & 7)).setValue(TYPE, (metadata & 8) == 0 ? SlabType.BOTTOM : SlabType.TOP); }
    @Override public int func_176201_c(BlockState state) { return state.getValue(VARIANT).getMetadata() | (state.getValue(TYPE) == SlabType.TOP ? 8 : 0); }
    @Override public String getSpecialName(ItemStack stack) { return func_150002_b(LegacyItemStacks.metadata(stack)); }
    public String func_150002_b(int metadata) { return "block.millenaire.slabs_" + BlockDecorativeWood.EnumType.byMetadata(metadata & 7).getSerializedName(); }
    public Property<?> func_176551_l() { return VARIANT; }
    public Comparable<?> func_185674_a(ItemStack stack) { return BlockDecorativeWood.EnumType.byMetadata(LegacyItemStacks.metadata(stack) & 7); }
    public boolean func_176552_j() { return false; }
    @Override public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return state.getValue(VARIANT) == BlockDecorativeWood.EnumType.byMetadata(LegacyItemStacks.metadata(context.getItemInHand()) & 7) && super.canBeReplaced(state, context);
    }
    @Override public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items) {
        for (BlockDecorativeWood.EnumType variant : BlockDecorativeWood.EnumType.values()) if (variant.hasSlab()) items.add(LegacyItemStacks.stack(this, 1, variant.getMetadata()));
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder context) { return List.of(LegacyItemStacks.stack(this, state.getValue(TYPE) == SlabType.DOUBLE ? 2 : 1, state.getValue(VARIANT).getMetadata())); }
    @Override public ItemStack getCloneItemStack(net.minecraft.world.level.BlockGetter world, net.minecraft.core.BlockPos pos, BlockState state) { return LegacyItemStacks.stack(this, 1, state.getValue(VARIANT).getMetadata()); }
    @Override public int itemMetadata(BlockState state) { return func_176201_c(state) & 7; }
}
