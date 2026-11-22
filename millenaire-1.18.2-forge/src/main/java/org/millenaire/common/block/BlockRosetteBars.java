package org.millenaire.common.block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class BlockRosetteBars extends BlockBars implements LegacyVariantBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Half> TOP_BOTTOM = EnumProperty.create("topbottom", Half.class);
    public BlockRosetteBars(String name, Material material, SoundType sound) {
        super(name); registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH).setValue(TOP_BOTTOM, Half.TOP));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); builder.add(FACING, TOP_BOTTOM); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level world = context.getLevel(); BlockPos pos = context.getClickedPos();
        BlockState placed = super.getStateForPlacement(context);
        BlockState iBlockStateAbove = world.getBlockState(pos.offset(0, 1, 0));
        if (iBlockStateAbove.getBlock() == this && iBlockStateAbove.getValue(TOP_BOTTOM) == net.minecraft.world.level.block.state.properties.Half.TOP) {
            return placed.setValue(TOP_BOTTOM, net.minecraft.world.level.block.state.properties.Half.BOTTOM).setValue(FACING, iBlockStateAbove.getValue(FACING));
        }
        BlockState iBlockStateWest = world.getBlockState(pos.offset(-1, 0, 0));
        if (iBlockStateWest.getBlock() == this && iBlockStateWest.getValue(FACING) == Direction.WEST) {
            return placed.setValue(FACING, Direction.EAST).setValue(TOP_BOTTOM, iBlockStateWest.getValue(TOP_BOTTOM));
        }
        BlockState iBlockStateSouth = world.getBlockState(pos.offset(0, 0, 1));
        if (iBlockStateSouth.getBlock() == this && iBlockStateSouth.getValue(FACING) == Direction.SOUTH) {
            return placed.setValue(FACING, Direction.NORTH).setValue(TOP_BOTTOM, iBlockStateSouth.getValue(TOP_BOTTOM));
        }
        BlockState iBlockStateBelow = world.getBlockState(pos.offset(0, -1, 0));
        if (iBlockStateBelow.getBlock() == this && iBlockStateBelow.getValue(TOP_BOTTOM) == net.minecraft.world.level.block.state.properties.Half.BOTTOM) {
            return placed.setValue(TOP_BOTTOM, net.minecraft.world.level.block.state.properties.Half.TOP).setValue(FACING, iBlockStateBelow.getValue(FACING));
        }
        BlockState iBlockStateEast = world.getBlockState(pos.offset(1, 0, 0));
        if (iBlockStateEast.getBlock() == this && iBlockStateEast.getValue(FACING) == Direction.EAST) {
            return placed.setValue(FACING, Direction.WEST).setValue(TOP_BOTTOM, iBlockStateEast.getValue(TOP_BOTTOM));
        }
        BlockState iBlockStateNorth = world.getBlockState(pos.offset(0, 0, -1));
        if (iBlockStateNorth.getBlock() == this && iBlockStateNorth.getValue(FACING) == Direction.NORTH) {
            return placed.setValue(FACING, Direction.SOUTH).setValue(TOP_BOTTOM, iBlockStateNorth.getValue(TOP_BOTTOM));
        }
        BlockState basicState = placed;
        if (!iBlockStateAbove.isCollisionShapeFullBlock(world, pos.above()) && iBlockStateBelow.isCollisionShapeFullBlock(world, pos.below())) {
            basicState = basicState.setValue(TOP_BOTTOM, net.minecraft.world.level.block.state.properties.Half.BOTTOM);
        }
        if (!iBlockStateWest.isCollisionShapeFullBlock(world, pos.west()) && iBlockStateEast.isCollisionShapeFullBlock(world, pos.east())) {
            basicState = basicState.setValue(FACING, Direction.EAST);
        } else if (!iBlockStateSouth.isCollisionShapeFullBlock(world, pos.south()) && iBlockStateNorth.isCollisionShapeFullBlock(world, pos.north())) {
            basicState = basicState.setValue(FACING, Direction.NORTH);
        } else if (iBlockStateSouth.isCollisionShapeFullBlock(world, pos.south()) && !iBlockStateNorth.isCollisionShapeFullBlock(world, pos.north())) {
            basicState = basicState.setValue(FACING, Direction.SOUTH);
        } else if (iBlockStateWest.isCollisionShapeFullBlock(world, pos.west()) && !iBlockStateEast.isCollisionShapeFullBlock(world, pos.east())) {
            basicState = basicState.setValue(FACING, Direction.WEST);
        }
        return basicState;
    }
    @Override public BlockState func_176203_a(int metadata) { return defaultBlockState().setValue(FACING, Direction.from2DDataValue(metadata & 3)).setValue(TOP_BOTTOM, (metadata & 4) == 0 ? Half.TOP : Half.BOTTOM); }
    @Override public int func_176201_c(BlockState state) { return state.getValue(FACING).get2DDataValue() | (state.getValue(TOP_BOTTOM) == Half.BOTTOM ? 4 : 0); }
    @Override public int itemMetadata(BlockState state) { return 0; }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return super.rotate(state, rotation).setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return super.mirror(state, mirror).setValue(FACING, mirror.mirror(state.getValue(FACING))); }
    @Override @OnlyIn(Dist.CLIENT) public void initModel() { net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(this, net.minecraft.client.renderer.RenderType.translucent()); }
}
