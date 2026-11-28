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

public class BlockWetBrick extends Block implements IMetaBlockName, LegacyVariantBlock {
    public static final EnumProperty<EnumType> PROGRESS = EnumProperty.create("progress", EnumType.class);
    public BlockWetBrick(String name) {
        super(Properties.of(Material.DIRT).strength(0.8F).sound(SoundType.GRAVEL).randomTicks());
        setRegistryName("millenaire", name);
        registerDefaultState(defaultBlockState().setValue(PROGRESS, EnumType.WETBRICK0));
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
    }
    @OnlyIn(Dist.CLIENT) public void initModel() { net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(this, net.minecraft.client.renderer.RenderType.solid()); }
    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, Random random) {
        int current = state.getValue(PROGRESS).getMetadata();
        if (level.getMaxLocalRawBrightness(pos.above()) <= 14) return;
        var data = org.millenaire.common.forge.Mill.getMillWorld(level);
        int kilnLevel = data != null && data.world == level
                ? data.getBrickDryingLevel(pos) : BrickDryingPolicy.NORMAL_LEVEL;
        int next = BrickDryingPolicy.advance(current, kilnLevel, random);
        if (next == current) return;
        if (next < 3) level.setBlockAndUpdate(pos, state.setValue(PROGRESS, EnumType.byMetadata(next)));
        else level.setBlockAndUpdate(pos, MillBlocks.STONE_DECORATION.defaultBlockState().setValue(BlockDecorativeStone.VARIANT, BlockDecorativeStone.EnumType.MUDBRICK));
    }
    public static enum EnumType implements StringRepresentable {
      WETBRICK0(0, "wetbrick0"),
      WETBRICK1(1, "wetbrick1"),
      WETBRICK2(2, "wetbrick2");

      private static final BlockWetBrick.EnumType[] META_LOOKUP = new BlockWetBrick.EnumType[values().length];
      private final int meta;
      private final String name;

      public static BlockWetBrick.EnumType byMetadata(int meta) {
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
         BlockWetBrick.EnumType[] var0 = values();

         for (BlockWetBrick.EnumType var3 : var0) {
            META_LOOKUP[var3.getMetadata()] = var3;
         }
      }
   }
}

