package org.millenaire.common.block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class BlockRosette extends BlockMillPane {
    public static final BooleanProperty ROSETTE_NORTH = BooleanProperty.create("ros_n");
    public static final BooleanProperty ROSETTE_EAST = BooleanProperty.create("ros_e");
    public static final BooleanProperty ROSETTE_SOUTH = BooleanProperty.create("ros_s");
    public static final BooleanProperty ROSETTE_WEST = BooleanProperty.create("ros_w");
    public static final BooleanProperty ROSETTE_UP = BooleanProperty.create("ros_u");
    public static final BooleanProperty ROSETTE_DOWN = BooleanProperty.create("ros_d");
    public BlockRosette(String name, Material material, SoundType sound) {
        super(name, material, sound);
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) state = state.setValue(property(direction), false);
        registerDefaultState(state);
    }
    private static BooleanProperty property(Direction direction) {
        return switch (direction) { case NORTH -> ROSETTE_NORTH; case EAST -> ROSETTE_EAST; case SOUTH -> ROSETTE_SOUTH;
            case WEST -> ROSETTE_WEST; case UP -> ROSETTE_UP; case DOWN -> ROSETTE_DOWN; };
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); builder.add(ROSETTE_NORTH, ROSETTE_EAST, ROSETTE_SOUTH, ROSETTE_WEST, ROSETTE_UP, ROSETTE_DOWN); }
    @Override public BlockState func_176221_a(BlockState state, BlockGetter level, BlockPos pos) {
        state = super.func_176221_a(state, level, pos);
        for (Direction direction : Direction.values()) state = state.setValue(property(direction), level.getBlockState(pos.relative(direction)).is(this));
        return state;
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = super.rotate(state, rotation);
        for (Direction direction : Direction.Plane.HORIZONTAL) rotated = rotated.setValue(property(rotation.rotate(direction)), state.getValue(property(direction)));
        return rotated;
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = super.mirror(state, mirror);
        for (Direction direction : Direction.Plane.HORIZONTAL) mirrored = mirrored.setValue(property(mirror.mirror(direction)), state.getValue(property(direction)));
        return mirrored;
    }
    @Override @OnlyIn(Dist.CLIENT) public void initModel() { net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(this, net.minecraft.client.renderer.RenderType.translucent()); }
}
