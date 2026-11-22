package org.millenaire.common.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public abstract class BlockHalfSlab extends Block implements LegacyVariantBlock {
    public static final EnumProperty<Half> HALF = EnumProperty.create("half", Half.class);
    private static final VoxelShape BOTTOM_SHAPE = Block.box(0, 0, 0, 16, 8, 16);
    private static final VoxelShape TOP_SHAPE = Block.box(0, 8, 0, 16, 16, 16);
    private final Block baseBlock;
    public BlockHalfSlab(Block fullBlock) {
        super(Properties.copy(fullBlock).color(fullBlock.defaultBlockState().getMapColor(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO)).strength(1.5F, 6.0F).noOcclusion());
        baseBlock = fullBlock;
        registerDefaultState(defaultBlockState().setValue(HALF, Half.BOTTOM));
    }
    public Block getBaseBlock() { return baseBlock; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(HALF); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == Half.TOP ? TOP_SHAPE : BOTTOM_SHAPE;
    }
    @Override public boolean useShapeForLightOcclusion(BlockState state) { return true; }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        double height = context.getClickLocation().y - context.getClickedPos().getY();
        Half half = face == Direction.DOWN || face != Direction.UP && height > 0.5 ? Half.TOP : Half.BOTTOM;
        return defaultBlockState().setValue(HALF, half);
    }
    @Override public int func_176201_c(BlockState state) { return state.getValue(HALF) == Half.TOP ? 8 : 0; }
    @Override public BlockState func_176203_a(int metadata) { return defaultBlockState().setValue(HALF, (metadata & 8) == 0 ? Half.BOTTOM : Half.TOP); }
    public String getUnlocalizedName(int metadata) { return getDescriptionId(); }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder context) { return List.of(new ItemStack(this)); }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return baseBlock.defaultBlockState().use(level, player, hand, hit);
    }
    @OnlyIn(Dist.CLIENT)
    public void initModel() {
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(this,
            net.minecraft.client.renderer.ItemBlockRenderTypes.getChunkRenderType(baseBlock.defaultBlockState()));
    }
    @Override public int itemMetadata(BlockState state) { return 0; }
}
