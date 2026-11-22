package org.millenaire.client.render;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import org.millenaire.common.entity.TileEntityLockedChest;

/** Render the custom chest with the same model as its world block. */
public final class MillChestItemRenderer extends BlockEntityWithoutLevelRenderer {
    public MillChestItemRenderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()); }
    @Override public void renderByItem(ItemStack stack,ItemTransforms.TransformType transform,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if (!(stack.getItem() instanceof BlockItem item)) return;
        var banner=new TileEntityLockedChest(BlockPos.ZERO,item.getBlock().defaultBlockState().setValue(org.millenaire.common.block.BlockLockedChest.FACING,net.minecraft.core.Direction.SOUTH));
        var renderer=Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(banner);
        if (renderer!=null) renderer.render(banner,0,pose,buffers,light,overlay);
    }
}
