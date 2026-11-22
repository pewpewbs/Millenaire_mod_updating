package org.millenaire.common.block;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.millenaire.common.item.IMetaBlockName;
import org.millenaire.common.item.LegacyItemStacks;

public class BlockMillStainedGlass extends IronBarsBlock implements IMetaBlockName, LegacyVariantBlock {
    public static final EnumProperty<EnumType> VARIANT = EnumProperty.create("variant", EnumType.class);
    private final String blockName;
    public BlockMillStainedGlass(String name) {
        super(Properties.of(Material.GLASS).strength(0.3F).sound(SoundType.GLASS).noOcclusion());
        blockName = name; setRegistryName("millenaire", name);
        registerDefaultState(defaultBlockState().setValue(VARIANT, EnumType.WHITE));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); builder.add(VARIANT); }
    @Override public int func_176201_c(BlockState state) { return state.getValue(VARIANT).getMetadata(); }
    @Override public BlockState func_176203_a(int metadata) { return defaultBlockState().setValue(VARIANT, EnumType.byMetadata(metadata)); }
    @Override public String getSpecialName(ItemStack stack) { return "block.millenaire." + blockName + "." + EnumType.byMetadata(LegacyItemStacks.metadata(stack)).getSerializedName(); }
    @Override public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items) { for (EnumType variant : EnumType.values()) items.add(LegacyItemStacks.stack(this, 1, variant.getMetadata())); }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) { return List.of(LegacyItemStacks.stack(this, 1, func_176201_c(state))); }
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) { return LegacyItemStacks.stack(this, 1, func_176201_c(state)); }
    @OnlyIn(Dist.CLIENT) public void initModel() { net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(this, net.minecraft.client.renderer.RenderType.translucent()); }
    public static enum EnumType implements StringRepresentable {
      WHITE(0, "white"),
      YELLOW(1, "yellow"),
      YELLOW_RED(2, "yellow_red"),
      RED_BLUE(3, "red_blue"),
      GREEN_BLUE(4, "green_blue");

      private static final BlockMillStainedGlass.EnumType[] META_LOOKUP = new BlockMillStainedGlass.EnumType[values().length];
      private final int meta;
      private final String name;

      public static BlockMillStainedGlass.EnumType byMetadata(int meta) {
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
         BlockMillStainedGlass.EnumType[] var0 = values();

         for (BlockMillStainedGlass.EnumType var3 : var0) {
            META_LOOKUP[var3.getMetadata()] = var3;
         }
      }
   }
}

