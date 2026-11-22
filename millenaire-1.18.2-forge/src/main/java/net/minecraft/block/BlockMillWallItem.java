package net.minecraft.block;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.api.distmarker.*;
import org.millenaire.common.block.LegacyVariantBlock;

/** The original thin, climbable wall decoration. Retains its historic class name for saved extensions. */
public class BlockMillWallItem extends HorizontalDirectionalBlock implements LegacyVariantBlock {
    private static final VoxelShape EAST = Block.box(0, 0, 0, 3, 16, 16);
    private static final VoxelShape WEST = Block.box(13, 0, 0, 16, 16, 16);
    private static final VoxelShape SOUTH = Block.box(0, 0, 0, 16, 16, 3);
    private static final VoxelShape NORTH = Block.box(0, 0, 13, 16, 16, 16);

    public BlockMillWallItem(String name, Material material, SoundType sound) {
        super(Properties.of(material).sound(sound).noOcclusion());
        setRegistryName("millenaire", name);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> EAST;
        };
    }
    private boolean canAttachTo(BlockGetter level, BlockPos support, Direction facing) {
        BlockState state = level.getBlockState(support);
        return state.isFaceSturdy(level, support, facing) && !state.isSignalSource();
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return canAttachTo(level, pos.relative(facing.getOpposite()), facing);
    }
    @Nullable @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        BlockPos pos = context.getClickedPos();
        if (clicked.getAxis().isHorizontal() && canAttachTo(context.getLevel(), pos.relative(clicked.getOpposite()), clicked))
            return defaultBlockState().setValue(FACING, clicked);
        for (Direction facing : Direction.Plane.HORIZONTAL)
            if (canAttachTo(context.getLevel(), pos.relative(facing.getOpposite()), facing))
                return defaultBlockState().setValue(FACING, facing);
        return null;
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos))
            return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }
    @Override public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) { return true; }
    @Override public BlockState func_176203_a(int metadata) {
        Direction facing = Direction.from3DDataValue(metadata);
        return defaultBlockState().setValue(FACING, facing.getAxis().isHorizontal() ? facing : Direction.NORTH);
    }
    @Override public int func_176201_c(BlockState state) { return state.getValue(FACING).get3DDataValue(); }
    @Override public int itemMetadata(BlockState state) { return 0; }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder context) { return List.of(new ItemStack(this)); }
    @OnlyIn(Dist.CLIENT) public void initModel() {
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(this, net.minecraft.client.renderer.RenderType.cutout());
    }
}
