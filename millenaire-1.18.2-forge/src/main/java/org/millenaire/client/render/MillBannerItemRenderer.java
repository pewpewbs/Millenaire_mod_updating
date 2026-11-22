package org.millenaire.client.render;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import org.millenaire.common.entity.TileEntityMockBanner;

/** Inventory rendering must honor the village's saved base color, not just the template color. */
public final class MillBannerItemRenderer extends BlockEntityWithoutLevelRenderer {
    public MillBannerItemRenderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()); }
    @Override public void renderByItem(ItemStack stack,ItemTransforms.TransformType transform,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if (!(stack.getItem() instanceof BlockItem item)) return;
        var banner=new TileEntityMockBanner(BlockPos.ZERO,item.getBlock().defaultBlockState()); banner.func_175112_a(stack,true);
        var renderer=Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(banner);
        if (renderer!=null) renderer.render(banner,0,pose,buffers,light,overlay);
    }
}
