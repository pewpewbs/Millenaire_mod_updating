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

public class BlockDecorativeEarth extends Block implements IMetaBlockName, LegacyVariantBlock {
    public static final EnumProperty<EnumType> VARIANT = EnumProperty.create("variant", EnumType.class);
    public BlockDecorativeEarth(String name) {
        super(Properties.of(Material.DIRT).strength(0.8F).sound(SoundType.GRAVEL));
        setRegistryName("millenaire", name);
        registerDefaultState(defaultBlockState());
    }
    @Override public boolean acceptsLegacyProperty(String name, String value) { return name.equals("variant") && value.equals("dirtwall"); }
    @Override public BlockState func_176203_a(int metadata) { return defaultBlockState(); }
    @Override public int func_176201_c(BlockState state) { return 0; }
    @Override public String getSpecialName(ItemStack stack) { return func_150002_b(LegacyItemStacks.metadata(stack)); }
    public String func_150002_b(int metadata) { return "block.millenaire." + EnumType.byMetadata(metadata).getSerializedName(); }
    public Property<?> func_176551_l() { return VARIANT; }
    public Comparable<?> func_185674_a(ItemStack stack) { return EnumType.byMetadata(LegacyItemStacks.metadata(stack) & 7); }
    public boolean func_176552_j() { return true; }
    @Override public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items) {
        for (EnumType variant : EnumType.values()) {  items.add(LegacyItemStacks.stack(this, 1, variant.getMetadata())); }
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder context) { return List.of(LegacyItemStacks.stack(this, 1, func_176201_c(state))); }
    @Override public ItemStack getCloneItemStack(net.minecraft.world.level.BlockGetter world, net.minecraft.core.BlockPos pos, BlockState state) { return LegacyItemStacks.stack(this, 1, func_176201_c(state)); }

    public static enum EnumType implements StringRepresentable {
      DIRTWALL(0, "dirtwall");

      private static final BlockDecorativeEarth.EnumType[] META_LOOKUP = new BlockDecorativeEarth.EnumType[values().length];
      private final int meta;
      private final String name;

      public static BlockDecorativeEarth.EnumType byMetadata(int meta) {
         if (meta < 0 || meta >= META_LOOKUP.length) {
            meta = 0;
         }

         return META_LOOKUP[meta];
      }

      private EnumType(int meta, String name) {
         this.meta = meta;
         this.name = name;
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

      @Override
      public String toString() {
         return this.name;
      }

      static {
         BlockDecorativeEarth.EnumType[] var0 = values();

         for (BlockDecorativeEarth.EnumType var3 : var0) {
            META_LOOKUP[var3.getMetadata()] = var3;
         }
      }
   }
}

