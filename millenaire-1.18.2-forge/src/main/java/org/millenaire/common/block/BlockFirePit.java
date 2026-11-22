package org.millenaire.common.block;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkHooks;
import org.millenaire.common.entity.MillBlockEntities;
import org.millenaire.common.entity.TileEntityFirePit;

public class BlockFirePit extends BaseEntityBlock implements LegacyVariantBlock {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public static final EnumProperty<EnumAlignment> ALIGNMENT = EnumProperty.create("alignment", EnumAlignment.class);
    public static final AABB FIRE_PIT_BOX = new AABB(0.1875, 0, 0.1875, 0.8125, 0.5, 0.8125);
    private static final VoxelShape SHAPE = Shapes.create(FIRE_PIT_BOX);
    public BlockFirePit(String name) {
        super(Properties.of(Material.WOOD).strength(0.2F).sound(SoundType.WOOD).noOcclusion().lightLevel(state -> state.getValue(LIT) ? 15 : 0));
        setRegistryName("millenaire", name);
        registerDefaultState(defaultBlockState().setValue(LIT, false).setValue(ALIGNMENT, EnumAlignment.X));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(LIT, ALIGNMENT); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileEntityFirePit(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, MillBlockEntities.FIRE_PIT, (world, pos, blockState, entity) -> entity.tick());
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof TileEntityFirePit pit) pit.dropAll();
        super.onRemove(state, level, pos, next, moving);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(ALIGNMENT, EnumAlignment.fromAxis(context.getHorizontalDirection().getAxis())); }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90
            ? state.setValue(ALIGNMENT, state.getValue(ALIGNMENT) == EnumAlignment.X ? EnumAlignment.Z : EnumAlignment.X) : state;
    }
    @Override public BlockState func_176203_a(int metadata) { return defaultBlockState().setValue(LIT, (metadata & 1) != 0).setValue(ALIGNMENT, EnumAlignment.fromMeta(metadata & 2)); }
    @Override public int func_176201_c(BlockState state) { return (state.getValue(LIT) ? 1 : 0) | state.getValue(ALIGNMENT).getMeta() << 1; }
    @Override public int itemMetadata(BlockState state) { return 0; }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder context) { return List.of(new ItemStack(this)); }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityFirePit pit)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) NetworkHooks.openGui(serverPlayer, pit, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, Random random) {
        if (!state.getValue(LIT)) return;
        level.playLocalSound(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1 + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F, false);
        if (random.nextInt(24) == 0) for (int i = 0; i < 3; i++) level.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX() + random.nextDouble(), pos.getY() + 0.5 + random.nextDouble() * 0.5, pos.getZ() + random.nextDouble(), 0, 0, 0);
    }
    @OnlyIn(Dist.CLIENT) public void initModel() { net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(this, net.minecraft.client.renderer.RenderType.cutout()); }
    public enum EnumAlignment implements StringRepresentable {
        X("x", 0, 90), Z("z", 1, 0);
        private final String name; private final int meta; public final double angle;
        EnumAlignment(String name, int meta, double angle) { this.name = name; this.meta = meta; this.angle = angle; }
        public int getMeta() { return meta; }
        public String getSerializedName() { return name; }
        public static EnumAlignment fromAxis(Direction.Axis axis) { return axis == Direction.Axis.X ? Z : X; }
        public static EnumAlignment fromMeta(int flag) { return flag == 0 ? X : Z; }
    }
}
