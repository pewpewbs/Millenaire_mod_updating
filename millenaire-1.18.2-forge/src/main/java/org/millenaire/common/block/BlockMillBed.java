package org.millenaire.common.block;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.shapes.*;
import org.millenaire.common.entity.TileEntityMillBed;
public class BlockMillBed extends BedBlock implements LegacyVariantBlock {
    private final int bedHeight;
    private final VoxelShape shape;
    public BlockMillBed(String name,int height) { super(DyeColor.RED,Properties.copy(Blocks.RED_BED)); setRegistryName(name); bedHeight=height; shape=Block.box(0,0,0,16,height,16); }
    public int getBedHeight() { return bedHeight; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new TileEntityMillBed(pos,state); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return shape; }
    @Override public ItemStack getCloneItemStack(BlockGetter level,BlockPos pos,BlockState state) { return new ItemStack(this); }
    @Override public List<ItemStack> getDrops(BlockState state,LootContext.Builder builder) { return state.getValue(PART)==BedPart.HEAD ? List.of(new ItemStack(this)) : List.of(); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getClickedFace()!=Direction.UP) return null;
        BlockState state=super.getStateForPlacement(context); if (state==null) return null;
        BlockPos foot=context.getClickedPos(),head=foot.relative(state.getValue(FACING));
        Level level=context.getLevel();
        if (!level.getBlockState(foot.below()).isFaceSturdy(level,foot.below(),Direction.UP) || !level.getBlockState(head.below()).isFaceSturdy(level,head.below(),Direction.UP)) return null;
        if (context.getPlayer()!=null && !context.getPlayer().mayUseItemAt(head,Direction.UP,context.getItemInHand())) return null;
        return state;
    }
    @Override public BlockState func_176203_a(int meta) { return defaultBlockState().setValue(FACING,Direction.from2DDataValue(meta & 3)).setValue(PART,(meta & 8)!=0?BedPart.HEAD:BedPart.FOOT).setValue(OCCUPIED,(meta & 4)!=0); }
    @Override public int func_176201_c(BlockState state) { return state.getValue(FACING).get2DDataValue() | (state.getValue(PART)==BedPart.HEAD?8:0) | (state.getValue(OCCUPIED)?4:0); }
    @Override public int itemMetadata(BlockState state) { return 0; }
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public void initModel() { net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(this,net.minecraft.client.renderer.RenderType.cutoutMipped()); }
}
