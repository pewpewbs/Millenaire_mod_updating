package org.millenaire.common.block;

import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.core.NonNullList;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.storage.loot.LootContext;
import org.millenaire.common.item.IMetaBlockName;
import org.millenaire.common.item.LegacyItemStacks;

public class BlockDecorativeStone extends Block implements IMetaBlockName, LegacyVariantBlock {
    public static final EnumProperty<EnumType> VARIANT = EnumProperty.create("variant", EnumType.class);
    public BlockDecorativeStone(String name) {
        super(Properties.of(Material.STONE, state -> state.getValue(VARIANT).getMapColor()).strength(1.5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());
        setRegistryName("millenaire", name);
        registerDefaultState(defaultBlockState().setValue(VARIANT, EnumType.MUDBRICK));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(VARIANT); }
    @Override public BlockState func_176203_a(int metadata) { return defaultBlockState().setValue(VARIANT, EnumType.byMetadata(metadata)); }
    @Override public int func_176201_c(BlockState state) { return state.getValue(VARIANT).getMetadata(); }
    @Override public String getSpecialName(ItemStack stack) { return func_150002_b(LegacyItemStacks.metadata(stack)); }
    public String func_150002_b(int metadata) { return "block.millenaire." + EnumType.byMetadata(metadata).getSerializedName(); }
    public Property<?> func_176551_l() { return VARIANT; }
    public Comparable<?> func_185674_a(ItemStack stack) { return EnumType.byMetadata(LegacyItemStacks.metadata(stack) & 7); }
    public boolean func_176552_j() { return true; }
    @Override public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items) {
        for (EnumType variant : EnumType.values()) { if (variant == EnumType.COOKEDBRICK) continue; items.add(LegacyItemStacks.stack(this, 1, variant.getMetadata())); }
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder context) { return List.of(LegacyItemStacks.stack(this, 1, func_176201_c(state))); }
    @Override public ItemStack getCloneItemStack(net.minecraft.world.level.BlockGetter world, net.minecraft.core.BlockPos pos, BlockState state) { return LegacyItemStacks.stack(this, 1, func_176201_c(state)); }

    public static enum EnumType implements StringRepresentable {
      MUDBRICK(0, "mudbrick", MaterialColor.COLOR_BROWN, true),
      COOKEDBRICK(1, "cookedbrick", MaterialColor.TERRACOTTA_WHITE, true),
      MAYANGOLDBLOCK(2, "mayangoldblock", MaterialColor.GOLD, false),
      BYZANTINEMOSAICRED(3, "byzantine_mosaic_red", MaterialColor.COLOR_RED, false),
      BYZANTINEMOSAICBLUE(4, "byzantine_mosaic_blue", MaterialColor.COLOR_BLUE, false),
      LIGHTBLUEBRICK_BLOCK(5, "lightbluebrick_block", MaterialColor.COLOR_BLUE, false),
      LIGHTBLUECHISELED_BLOCK(6, "lightbluechiseled_block", MaterialColor.COLOR_BLUE, false);

      private static final BlockDecorativeStone.EnumType[] META_LOOKUP = new BlockDecorativeStone.EnumType[values().length];
      private final int meta;
      private final String name;
      private final MaterialColor mapColor;
      private final boolean hasSlab;

      public static BlockDecorativeStone.EnumType byMetadata(int meta) {
         if (meta < 0 || meta >= META_LOOKUP.length) {
            meta = 0;
         }

         return META_LOOKUP[meta];
      }

      private EnumType(int meta, String name, MaterialColor mapColor, boolean hasSlab) {
         this.meta = meta;
         this.name = name;
         this.mapColor = mapColor;
         this.hasSlab = hasSlab;
      }

      public MaterialColor getMapColor() {
         return this.mapColor;
      }

      public int getMetadata() {
         return this.meta;
      }

      public String getSerializedName() {
         return this.name;
      }

      public String getUnlocalizedName() {
         return this.name;
      }

      public boolean hasSlab() {
         return this.hasSlab;
      }

      @Override
      public String toString() {
         return this.name;
      }

      static {
         BlockDecorativeStone.EnumType[] var0 = values();

         for (BlockDecorativeStone.EnumType var3 : var0) {
            META_LOOKUP[var3.getMetadata()] = var3;
         }
      }
   }
}

