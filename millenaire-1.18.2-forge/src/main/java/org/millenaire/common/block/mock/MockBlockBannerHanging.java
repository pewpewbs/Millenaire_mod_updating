package org.millenaire.common.block.mock;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.millenaire.common.block.LegacyVariantBlock;
import org.millenaire.common.entity.TileEntityMockBanner;
import org.millenaire.common.item.ItemMockBanner;
public class MockBlockBannerHanging extends WallBannerBlock implements LegacyVariantBlock {
    public final int bannerType;
    public MockBlockBannerHanging(int type) { super(ItemMockBanner.BANNER_COLOURS[type], Properties.copy(Blocks.WHITE_WALL_BANNER).strength(1.0f)); bannerType = type; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileEntityMockBanner(pos, state); }
    @Override public BlockState func_176203_a(int meta) { Direction facing = Direction.from3DDataValue(meta); return defaultBlockState().setValue(FACING, facing.getAxis().isHorizontal() ? facing : Direction.NORTH); }
    @Override public int func_176201_c(BlockState state) { return state.getValue(FACING).get3DDataValue(); }
    @Override public int itemMetadata(BlockState state) { return 0; }
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootContext.Builder builder) { return TileEntityMockBanner.drops(state, builder); }
}
