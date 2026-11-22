package org.millenaire.common.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.millenaire.common.item.IMetaBlockName;

public abstract class BlockOrientedSlab extends Block implements IMetaBlockName, LegacyVariantBlock {
    public static final EnumProperty<Direction.Axis> AXIS = EnumProperty.create("axis", Direction.Axis.class);
    public static final EnumProperty<Half> HALF = EnumProperty.create("half", Half.class);
    private static final VoxelShape BOTTOM_SHAPE = Block.box(0, 0, 0, 16, 8, 16);
    private static final VoxelShape TOP_SHAPE = Block.box(0, 8, 0, 16, 16, 16);

    public BlockOrientedSlab(String name) {
        super(Properties.of(Material.STONE, MaterialColor.COLOR_ORANGE).strength(2.0F, 6.0F).requiresCorrectToolForDrops());
        setRegistryName("millenaire", name);
        BlockState state = defaultBlockState().setValue(AXIS, Direction.Axis.X);
        registerDefaultState(func_176552_j() ? state : state.setValue(HALF, Half.BOTTOM));
    }
    public abstract boolean func_176552_j();
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
        if (!func_176552_j()) builder.add(HALF);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return func_176552_j() ? Shapes.block() : state.getValue(HALF) == Half.TOP ? TOP_SHAPE : BOTTOM_SHAPE;
    }
    @Override public boolean useShapeForLightOcclusion(BlockState state) { return !func_176552_j(); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        BlockState state = defaultBlockState().setValue(AXIS, face.getAxis() == Direction.Axis.Y ? Direction.Axis.X : face.getAxis());
        if (!func_176552_j()) state = state.setValue(HALF, face != Direction.DOWN && (face == Direction.UP
            || context.getClickLocation().y - context.getClickedPos().getY() <= 0.5) ? Half.BOTTOM : Half.TOP);
        return state;
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        Direction.Axis axis = state.getValue(AXIS);
        if ((rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90) && axis != Direction.Axis.Y)
            return state.setValue(AXIS, axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
        return state;
    }
    @Override public BlockState func_176203_a(int metadata) {
        int bits = metadata & 12;
        BlockState state = defaultBlockState().setValue(AXIS, bits == 4 ? Direction.Axis.X : bits == 8 ? Direction.Axis.Z : Direction.Axis.Y);
        return func_176552_j() ? state : state.setValue(HALF, (metadata & 2) == 0 ? Half.BOTTOM : Half.TOP);
    }
    @Override public int func_176201_c(BlockState state) {
        int axis = state.getValue(AXIS) == Direction.Axis.X ? 4 : state.getValue(AXIS) == Direction.Axis.Z ? 8 : 0;
        return axis | (!func_176552_j() && state.getValue(HALF) == Half.TOP ? 2 : 0);
    }
    @Override public int itemMetadata(BlockState state) { return 0; }
    @Override public boolean acceptsLegacyProperty(String name, String value) { return name.equals("variant") && value.equals("default"); }
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) { return new ItemStack(this); }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) { return List.of(new ItemStack(this)); }
    @Override public String getSpecialName(ItemStack stack) { return getDescriptionId(); }

    public static class BlockOrientedSlabSlab extends BlockOrientedSlab {
        public BlockOrientedSlabSlab(String name) { super(name); }
        @Override public boolean func_176552_j() { return false; }
    }
    public static class BlockOrientedSlabDouble extends BlockOrientedSlab {
        public BlockOrientedSlabDouble(String name) { super(name); }
        @Override public boolean func_176552_j() { return true; }
    }
}
