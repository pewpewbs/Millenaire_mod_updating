package org.millenaire.common.block;

import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.storage.loot.LootContext;
import org.millenaire.common.item.IMetaBlockName;
import org.millenaire.common.item.LegacyItemStacks;

public class BlockSod extends Block implements IMetaBlockName, LegacyVariantBlock {
    public static final EnumProperty<WoodVariant> VARIANT = EnumProperty.create("variant", WoodVariant.class);
    public BlockSod(String name) {
        super(Properties.of(Material.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD));
        setRegistryName("millenaire", name);
        registerDefaultState(defaultBlockState().setValue(VARIANT, WoodVariant.OAK));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(VARIANT); }
    @Override public BlockState func_176203_a(int metadata) { return defaultBlockState().setValue(VARIANT, WoodVariant.byMetadata(metadata)); }
    @Override public int func_176201_c(BlockState state) { return state.getValue(VARIANT).getMetadata(); }
    @Override public String getSpecialName(ItemStack stack) {
        return "block.millenaire." + getRegistryName().getPath() + "_" + WoodVariant.byMetadata(LegacyItemStacks.metadata(stack)).getSerializedName();
    }
    @Override public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items) {
        for (WoodVariant wood : WoodVariant.values()) items.add(LegacyItemStacks.stack(this, 1, wood.getMetadata()));
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder context) {
        return List.of(LegacyItemStacks.stack(this, 1, func_176201_c(state)));
    }
    @Override public ItemStack getCloneItemStack(net.minecraft.world.level.BlockGetter world, net.minecraft.core.BlockPos pos, BlockState state) {
        return LegacyItemStacks.stack(this, 1, func_176201_c(state));
    }
}
