package org.millenaire.common.block;
import java.util.List;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class BlockMillPane extends IronBarsBlock {
    public BlockMillPane(String name, Material material, SoundType sound) { this(name, Properties.of(material).strength(0.3F).sound(sound).noOcclusion()); }
    protected BlockMillPane(String name, Properties properties) { super(properties); setRegistryName("millenaire", name); }
    public boolean attachesTo(BlockState state, boolean sturdy) { return state.getBlock() instanceof BlockMillWall || super.attachsTo(state, sturdy); }
    public BlockState func_176221_a(BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            var other = pos.relative(direction); var neighbor = level.getBlockState(other);
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), attachesTo(neighbor, neighbor.isFaceSturdy(level, other, direction.getOpposite())));
        }
        return state;
    }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) { return func_176221_a(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos()); }
    @Override public BlockState updateShape(BlockState state, net.minecraft.core.Direction direction, BlockState neighbor, net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos pos, net.minecraft.core.BlockPos neighborPos) {
        return func_176221_a(super.updateShape(state, direction, neighbor, level, pos, neighborPos), level, pos);
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) { return List.of(new ItemStack(this)); }
    @OnlyIn(Dist.CLIENT) public void initModel() { net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(this, net.minecraft.client.renderer.RenderType.cutoutMipped()); }
}
