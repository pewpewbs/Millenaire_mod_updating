package org.millenaire.common.block.mock;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.*;
import org.millenaire.common.block.LegacyVariantBlock;
import org.millenaire.common.entity.TileEntityMockBanner;
import org.millenaire.common.item.ItemMockBanner;

/** Deliberately avoids BannerBlock's global vanilla color-to-block registration. */
public class MockBlockBannerStanding extends AbstractBannerBlock implements LegacyVariantBlock {
    public static final IntegerProperty ROTATION = BannerBlock.ROTATION;
    public final int bannerType;
    public MockBlockBannerStanding(int type) {
        super(ItemMockBanner.BANNER_COLOURS[type], Properties.copy(Blocks.WHITE_BANNER).strength(1.0f));
        bannerType = type; registerDefaultState(stateDefinition.any().setValue(ROTATION, 0));
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileEntityMockBanner(pos, state); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(ROTATION); }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) { return level.getBlockState(pos.below()).getMaterial().isSolid(); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Block.box(4, 0, 4, 12, 16, 12); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(ROTATION, net.minecraft.util.Mth.floor((context.getRotation() + 180.0F) * 16.0F / 360.0F + 0.5F) & 15); }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), 16)); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION), 16)); }
    @Override public BlockState func_176203_a(int meta) { return defaultBlockState().setValue(ROTATION, meta & 15); }
    @Override public int func_176201_c(BlockState state) { return state.getValue(ROTATION); }
    @Override public int itemMetadata(BlockState state) { return 0; }
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootContext.Builder builder) { return TileEntityMockBanner.drops(state, builder); }
}
