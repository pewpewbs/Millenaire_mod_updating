package org.millenaire.client.render;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.millenaire.common.entity.EntityWallDecoration;

public class RenderWallDecoration extends EntityRenderer<EntityWallDecoration> {
    public static final ResourceLocation textureTapestries=new ResourceLocation("millenaire","textures/painting/tapestry.png");
    public static final ResourceLocation textureSculptures=new ResourceLocation("millenaire","textures/painting/sculptures.png");
    public RenderWallDecoration(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(EntityWallDecoration entity) {
        return entity.type==1 ? textureTapestries : textureSculptures;
    }
    @Override public void render(EntityWallDecoration entity,float yaw,float partialTicks,PoseStack pose,MultiBufferSource buffers,int light) {
        pose.pushPose();
        pose.mulPose(Vector3f.YP.rotationDegrees(180-yaw));
        pose.scale(.0625f,.0625f,.0625f);
        var art=entity.millArt;
        renderPainting(entity,art.sizeX,art.sizeY,art.offsetX,art.offsetY,pose,buffers.getBuffer(RenderType.entityCutout(getTextureLocation(entity))));
        pose.popPose();
        super.render(entity,yaw,partialTicks,pose,buffers,light);
    }
    private static void vertex(PoseStack pose,VertexConsumer out,float x,float y,float z,double u,double v,int light,float nx,float ny,float nz) {
        out.vertex(pose.last().pose(),x,y,z).color(255,255,255,255).uv((float)u,(float)v)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),nx,ny,nz).endVertex();
    }
    private void renderPainting(EntityWallDecoration painting, int width, int height, int textureU, int textureV, PoseStack pose, VertexConsumer bufferbuilder) {
        float f = (float)(-width) / 2.0f;
        float f1 = (float)(-height) / 2.0f;
        for (int i = 0; i < width / 16; ++i) {
            for (int j = 0; j < height / 16; ++j) {
                float f15 = f + (float)((i + 1) * 16);
                float f16 = f + (float)(i * 16);
                float f17 = f1 + (float)((j + 1) * 16);
                float f18 = f1 + (float)(j * 16);
                int light = this.getLight(painting, (f15 + f16) / 2.0f, (f17 + f18) / 2.0f);
                float f19 = (float)(textureU + width - i * 16) / 256.0f;
                float f20 = (float)(textureU + width - (i + 1) * 16) / 256.0f;
                float f21 = (float)(textureV + height - j * 16) / 256.0f;
                float f22 = (float)(textureV + height - (j + 1) * 16) / 256.0f;
                vertex(pose, bufferbuilder, f15, f18, -0.5f, f20, f21, light, 0.0f, 0.0f, -1.0f);
                vertex(pose, bufferbuilder, f16, f18, -0.5f, f19, f21, light, 0.0f, 0.0f, -1.0f);
                vertex(pose, bufferbuilder, f16, f17, -0.5f, f19, f22, light, 0.0f, 0.0f, -1.0f);
                vertex(pose, bufferbuilder, f15, f17, -0.5f, f20, f22, light, 0.0f, 0.0f, -1.0f);
                vertex(pose, bufferbuilder, f15, f17, 0.5f, 0.75, 0.0, light, 0.0f, 0.0f, 1.0f);
                vertex(pose, bufferbuilder, f16, f17, 0.5f, 0.8125, 0.0, light, 0.0f, 0.0f, 1.0f);
                vertex(pose, bufferbuilder, f16, f18, 0.5f, 0.8125, 0.0625, light, 0.0f, 0.0f, 1.0f);
                vertex(pose, bufferbuilder, f15, f18, 0.5f, 0.75, 0.0625, light, 0.0f, 0.0f, 1.0f);
                vertex(pose, bufferbuilder, f15, f17, -0.5f, 0.75, 0.001953125, light, 0.0f, 1.0f, 0.0f);
                vertex(pose, bufferbuilder, f16, f17, -0.5f, 0.8125, 0.001953125, light, 0.0f, 1.0f, 0.0f);
                vertex(pose, bufferbuilder, f16, f17, 0.5f, 0.8125, 0.001953125, light, 0.0f, 1.0f, 0.0f);
                vertex(pose, bufferbuilder, f15, f17, 0.5f, 0.75, 0.001953125, light, 0.0f, 1.0f, 0.0f);
                vertex(pose, bufferbuilder, f15, f18, 0.5f, 0.75, 0.001953125, light, 0.0f, -1.0f, 0.0f);
                vertex(pose, bufferbuilder, f16, f18, 0.5f, 0.8125, 0.001953125, light, 0.0f, -1.0f, 0.0f);
                vertex(pose, bufferbuilder, f16, f18, -0.5f, 0.8125, 0.001953125, light, 0.0f, -1.0f, 0.0f);
                vertex(pose, bufferbuilder, f15, f18, -0.5f, 0.75, 0.001953125, light, 0.0f, -1.0f, 0.0f);
                vertex(pose, bufferbuilder, f15, f17, 0.5f, 0.751953125, 0.0, light, -1.0f, 0.0f, 0.0f);
                vertex(pose, bufferbuilder, f15, f18, 0.5f, 0.751953125, 0.0625, light, -1.0f, 0.0f, 0.0f);
                vertex(pose, bufferbuilder, f15, f18, -0.5f, 0.751953125, 0.0625, light, -1.0f, 0.0f, 0.0f);
                vertex(pose, bufferbuilder, f15, f17, -0.5f, 0.751953125, 0.0, light, -1.0f, 0.0f, 0.0f);
                vertex(pose, bufferbuilder, f16, f17, -0.5f, 0.751953125, 0.0, light, 1.0f, 0.0f, 0.0f);
                vertex(pose, bufferbuilder, f16, f18, -0.5f, 0.751953125, 0.0625, light, 1.0f, 0.0f, 0.0f);
                vertex(pose, bufferbuilder, f16, f18, 0.5f, 0.751953125, 0.0625, light, 1.0f, 0.0f, 0.0f);
                vertex(pose, bufferbuilder, f16, f17, 0.5f, 0.751953125, 0.0, light, 1.0f, 0.0f, 0.0f);
            }
        }
    }

    private int getLight(EntityWallDecoration painting, float p_77008_2_, float p_77008_3_) {
        int i = Mth.floor((double)painting.getX());
        int j = Mth.floor((double)(painting.getY() + (double)(p_77008_3_ / 16.0f)));
        int k = Mth.floor((double)painting.getZ());
        Direction enumfacing = painting.getDirection();
        if (enumfacing == Direction.NORTH) {
            i = Mth.floor((double)(painting.getX() + (double)(p_77008_2_ / 16.0f)));
        }
        if (enumfacing == Direction.WEST) {
            k = Mth.floor((double)(painting.getZ() - (double)(p_77008_2_ / 16.0f)));
        }
        if (enumfacing == Direction.SOUTH) {
            i = Mth.floor((double)(painting.getX() - (double)(p_77008_2_ / 16.0f)));
        }
        if (enumfacing == Direction.EAST) {
            k = Mth.floor((double)(painting.getZ() + (double)(p_77008_2_ / 16.0f)));
        }
        return LevelRenderer.getLightColor(painting.level, new BlockPos(i, j, k));
    }
}
