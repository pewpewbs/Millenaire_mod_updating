package org.millenaire.client.render;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.millenaire.common.block.BlockPanel;
import org.millenaire.common.entity.TileEntityPanel;

public class TESRPanel implements BlockEntityRenderer<TileEntityPanel> {
    private static final ResourceLocation DEFAULT = new ResourceLocation("millenaire","textures/entity/panels/default.png");
    private final ModelPanel model = new ModelPanel();
    private final Font font;
    public TESRPanel(BlockEntityRendererProvider.Context context) { font=context.getFont(); }
    @Override public void render(TileEntityPanel panel,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose(); pose.translate(0.5,0.5,0.5);
        pose.mulPose(Vector3f.YP.rotationDegrees(-panel.getBlockState().getValue(BlockPanel.FACING).toYRot())); pose.translate(0,0,-0.4375);
        pose.pushPose(); pose.scale(2.0F/3,-2.0F/3,-2.0F/3);
        model.renderSign(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(panel.texture==null?DEFAULT:panel.texture)),light,overlay);
        panel.translateLines(font); pose.translate(0,0.24,0);
        for (int index=0;index<panel.displayLines.size();index++) {
            var line=panel.displayLines.get(index); drawIcon(panel,index,line.leftIcon,-0.54F,pose,buffers,light,overlay);
            drawIcon(panel,index,line.middleIcon,0.08F,pose,buffers,light,overlay); drawIcon(panel,index,line.rightIcon,0.54F,pose,buffers,light,overlay);
        }
        pose.popPose(); pose.translate(0,0.25,0.0466666669); pose.scale(1.0F/96,-1.0F/96,1.0F/96);
        for (int index=0;index<panel.displayLines.size();index++) {
            var line=panel.displayLines.get(index); int y=index*10-15;
            text(line.fullLine,line.centerLine?-font.width(line.fullLine)/2.0F:-29,y,pose,buffers,light);
            text(line.leftColumn,-29,y,pose,buffers,light); text(line.rightColumn,11,y,pose,buffers,light);
        }
        pose.popPose();
    }
    private void text(String text,float x,int y,PoseStack pose,MultiBufferSource buffers,int light) { font.drawInBatch(text,x,y,0,false,pose.last().pose(),buffers,false,0,light); }
    private void drawIcon(TileEntityPanel panel,int index,ItemStack stack,float x,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if (stack.isEmpty()) return;
        pose.pushPose(); pose.translate(x,-0.74+index*0.15,-0.09); pose.scale(0.15F,-0.15F,-0.001F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack,ItemTransforms.TransformType.GUI,light,overlay,pose,buffers,index);
        pose.popPose();
    }
}
