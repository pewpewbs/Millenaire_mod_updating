package org.millenaire.common.block;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

public class BlockSnailSoil extends Block implements IMetaBlockName, LegacyVariantBlock {
    public static final EnumProperty<EnumType> PROGRESS = EnumProperty.create("progress", EnumType.class);
    public BlockSnailSoil(String name) {
        super(Properties.of(Material.DIRT).strength(0.0F).sound(SoundType.STONE).randomTicks());
        setRegistryName("millenaire", name);
        registerDefaultState(defaultBlockState().setValue(PROGRESS, EnumType.SNAIL_SOIL_EMPTY));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PROGRESS); }
    @Override public BlockState func_176203_a(int metadata) { return defaultBlockState().setValue(PROGRESS, EnumType.byMetadata(metadata)); }
    @Override public int func_176201_c(BlockState state) { return state.getValue(PROGRESS).getMetadata(); }
    @Override public int itemMetadata(BlockState state) { return 0; }
    @Override public String getSpecialName(ItemStack stack) { return "block.millenaire." + EnumType.byMetadata(LegacyItemStacks.metadata(stack)).getSerializedName(); }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) { return List.of(new ItemStack(this)); }
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) { return new ItemStack(this); }
    @Override public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items) {
        items.add(LegacyItemStacks.stack(this, 1, 0));
        items.add(LegacyItemStacks.stack(this, 1, 3));
    }
    @OnlyIn(Dist.CLIENT) public void initModel() { net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(this, net.minecraft.client.renderer.RenderType.solid()); }
    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, Random random) {
        int current = state.getValue(PROGRESS).getMetadata();
        BlockPos surface = pos.above(2);
        if (current < 3 && level.getBlockState(pos.above()).is(Blocks.WATER)
                && !level.getBlockState(surface).isSuffocating(level, surface) && random.nextInt(2) == 0)
            level.setBlockAndUpdate(pos, state.setValue(PROGRESS, EnumType.byMetadata(current + 1)));
    }
    public static enum EnumType implements StringRepresentable {
      SNAIL_SOIL_EMPTY(0, "snail_soil_empty"),
      SNAIL_SOIL_IP1(1, "snail_soil_ip1"),
      SNAIL_SOIL_IP2(2, "snail_soil_ip2"),
      SNAIL_SOIL_FULL(3, "snail_soil_full");

      private static final BlockSnailSoil.EnumType[] META_LOOKUP = new BlockSnailSoil.EnumType[values().length];
      private final int meta;
      private final String name;

      public static BlockSnailSoil.EnumType byMetadata(int meta) {
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
         BlockSnailSoil.EnumType[] var0 = values();

         for (BlockSnailSoil.EnumType var3 : var0) {
            META_LOOKUP[var3.getMetadata()] = var3;
         }
      }
   }
}

