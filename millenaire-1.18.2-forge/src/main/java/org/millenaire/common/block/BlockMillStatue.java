package org.millenaire.common.block;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
public class BlockMillStatue extends DirectionalBlock implements LegacyVariantBlock {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);
    public BlockMillStatue(String name, SoundType sound, Material material) {
        super(Properties.of(material).strength(0.5F, 1.2F).sound(sound).noOcclusion().requiresCorrectToolForDrops());
        setRegistryName("millenaire", name); registerDefaultState(defaultBlockState().setValue(FACING, Direction.DOWN));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) { return 0; }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        var player = context.getPlayer(); BlockPos pos = context.getClickedPos();
        if (player != null && Math.abs(player.getX() - pos.getX()) < 2 && Math.abs(player.getZ() - pos.getZ()) < 2
            && (player.getEyeY() - pos.getY() > 2 || pos.getY() - player.getEyeY() > 0)) facing = Direction.SOUTH;
        return defaultBlockState().setValue(FACING, facing);
    }
    public static Direction getFacing(int metadata) { return (metadata & 7) <= 5 ? Direction.from3DDataValue(metadata & 7) : Direction.DOWN; }
    @Override public BlockState func_176203_a(int metadata) { return defaultBlockState().setValue(FACING, getFacing(metadata)); }
    @Override public int func_176201_c(BlockState state) { return state.getValue(FACING).get3DDataValue(); }
    @Override public int itemMetadata(BlockState state) { return 0; }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return state.setValue(FACING, mirror.mirror(state.getValue(FACING))); }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) { return List.of(new ItemStack(this)); }
    @OnlyIn(Dist.CLIENT) public void initModel() { net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(this, net.minecraft.client.renderer.RenderType.solid()); }
}
