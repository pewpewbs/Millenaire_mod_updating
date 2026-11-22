package org.millenaire.common.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class BlockMillWall extends Block implements LegacyVariantBlock {
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    private static final VoxelShape[] SHAPES = {Shapes.create(new net.minecraft.world.phys.AABB(0.25, 0.0, 0.25, 0.75, 1.0, 0.75)), Shapes.create(new net.minecraft.world.phys.AABB(0.25, 0.0, 0.25, 0.75, 1.0, 1.0)), Shapes.create(new net.minecraft.world.phys.AABB(0.0, 0.0, 0.25, 0.75, 1.0, 0.75)), Shapes.create(new net.minecraft.world.phys.AABB(0.0, 0.0, 0.25, 0.75, 1.0, 1.0)), Shapes.create(new net.minecraft.world.phys.AABB(0.25, 0.0, 0.0, 0.75, 1.0, 0.75)), Shapes.create(new net.minecraft.world.phys.AABB(0.3125, 0.0, 0.0, 0.6875, 0.875, 1.0)), Shapes.create(new net.minecraft.world.phys.AABB(0.0, 0.0, 0.0, 0.75, 1.0, 0.75)), Shapes.create(new net.minecraft.world.phys.AABB(0.0, 0.0, 0.0, 0.75, 1.0, 1.0)), Shapes.create(new net.minecraft.world.phys.AABB(0.25, 0.0, 0.25, 1.0, 1.0, 0.75)), Shapes.create(new net.minecraft.world.phys.AABB(0.25, 0.0, 0.25, 1.0, 1.0, 1.0)), Shapes.create(new net.minecraft.world.phys.AABB(0.0, 0.0, 0.3125, 1.0, 0.875, 0.6875)), Shapes.create(new net.minecraft.world.phys.AABB(0.0, 0.0, 0.25, 1.0, 1.0, 1.0)), Shapes.create(new net.minecraft.world.phys.AABB(0.25, 0.0, 0.0, 1.0, 1.0, 0.75)), Shapes.create(new net.minecraft.world.phys.AABB(0.25, 0.0, 0.0, 1.0, 1.0, 1.0)), Shapes.create(new net.minecraft.world.phys.AABB(0.0, 0.0, 0.0, 1.0, 1.0, 0.75)), Shapes.create(new net.minecraft.world.phys.AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0))};
    private static final VoxelShape[] COLLISION = new VoxelShape[16];
    static {
        for (int i = 0; i < 16; i++) {
            var box = SHAPES[i].bounds();
            COLLISION[i] = Shapes.create(box.minX, box.minY, box.minZ, box.maxX, 1.5, box.maxZ);
        }
    }
    private final Block baseBlock;
    public BlockMillWall(String name, Block baseBlock) {
        super(Properties.copy(baseBlock).noOcclusion());
        this.baseBlock = baseBlock;
        setRegistryName("millenaire", name);
        registerDefaultState(defaultBlockState().setValue(UP, false).setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(UP, NORTH, EAST, SOUTH, WEST); }
    private static int shapeIndex(BlockState state) {
        return (state.getValue(SOUTH) ? 1 : 0) | (state.getValue(WEST) ? 2 : 0) | (state.getValue(NORTH) ? 4 : 0) | (state.getValue(EAST) ? 8 : 0);
    }
    public boolean canBeConnectedTo(BlockGetter level, BlockPos pos, Direction facing) {
        Block block = level.getBlockState(pos.relative(facing)).getBlock();
        return block instanceof WallBlock || block instanceof FenceGateBlock || block instanceof BlockMillWall;
    }
    private boolean connects(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos other = pos.relative(direction);
        BlockState state = level.getBlockState(other);
        Block block = state.getBlock();
        if (block instanceof BlockMillWall || block instanceof WallBlock || block instanceof IronBarsBlock) return true;
        if (block instanceof FenceGateBlock) return FenceGateBlock.connectsToDirection(state, direction);
        return !Block.isExceptionForConnection(state) && state.isFaceSturdy(level, other, direction.getOpposite());
    }
    public BlockState func_176221_a(BlockState state, BlockGetter level, BlockPos pos) {
        boolean north = connects(level, pos, Direction.NORTH), east = connects(level, pos, Direction.EAST);
        boolean south = connects(level, pos, Direction.SOUTH), west = connects(level, pos, Direction.WEST);
        boolean straight = north && south && !east && !west || east && west && !north && !south;
        return state.setValue(UP, !straight || !level.getBlockState(pos.above()).isAir())
            .setValue(NORTH, north).setValue(EAST, east).setValue(SOUTH, south).setValue(WEST, west);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return func_176221_a(defaultBlockState(), context.getLevel(), context.getClickedPos()); }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) { return func_176221_a(state, level, pos); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPES[shapeIndex(state)]; }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return COLLISION[shapeIndex(state)]; }
    @Override public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) { return false; }
    @Override public BlockPathTypes getAiPathNodeType(BlockState state, BlockGetter level, BlockPos pos, Mob mob) { return BlockPathTypes.FENCE; }
    @Override public boolean useShapeForLightOcclusion(BlockState state) { return true; }
    public Block getBaseBlock() { return baseBlock; }
    @Override public int func_176201_c(BlockState state) { return 0; }
    @Override public BlockState func_176203_a(int metadata) { return defaultBlockState(); }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) { return List.of(new ItemStack(this)); }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) { return baseBlock.defaultBlockState().use(level, player, hand, hit); }
    @OnlyIn(Dist.CLIENT) public void initModel() {
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(this,
            net.minecraft.client.renderer.ItemBlockRenderTypes.getChunkRenderType(baseBlock.defaultBlockState()));
    }
}
