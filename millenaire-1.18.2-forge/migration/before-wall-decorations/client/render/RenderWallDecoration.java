/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.renderer.BufferBuilder
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.OpenGlHelper
 *  net.minecraft.client.renderer.Tessellator
 *  net.minecraft.client.renderer.entity.Render
 *  net.minecraft.client.renderer.entity.RenderManager
 *  net.minecraft.client.renderer.vertex.DefaultVertexFormats
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.MathHelper
 *  net.minecraftforge.fml.client.registry.IRenderFactory
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package org.millenaire.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.millenaire.common.entity.EntityWallDecoration;

@OnlyIn(value=Dist.CLIENT)
public class RenderWallDecoration
extends EntityRenderer<EntityWallDecoration> {
    public static final FactoryRenderWallDecoration FACTORY_WALL_DECORATION = new FactoryRenderWallDecoration();
    public static final ResourceLocation textureTapestries = new ResourceLocation("millenaire", "textures/painting/tapestry.png");
    public static final ResourceLocation textureSculptures = new ResourceLocation("millenaire", "textures/painting/sculptures.png");

    protected RenderWallDecoration(EntityRenderDispatcher renderManager) {
        super(renderManager);
    }

    public void doRender(EntityWallDecoration entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.func_179094_E();
        GlStateManager.func_179137_b((double)x, (double)y, (double)z);
        GlStateManager.func_179114_b((float)(180.0f - entityYaw), (float)0.0f, (float)1.0f, (float)0.0f);
        GlStateManager.func_179091_B();
        this.func_180548_c((Entity)entity);
        EntityWallDecoration.EnumWallDecoration enumart = entity.millArt;
        GlStateManager.func_179152_a((float)0.0625f, (float)0.0625f, (float)0.0625f);
        if (this.field_188301_f) {
            GlStateManager.func_179142_g();
            GlStateManager.func_187431_e((int)this.func_188298_c((Entity)entity));
        }
        this.renderPainting(entity, enumart.sizeX, enumart.sizeY, enumart.offsetX, enumart.offsetY);
        if (this.field_188301_f) {
            GlStateManager.func_187417_n();
            GlStateManager.func_179119_h();
        }
        GlStateManager.func_179101_C();
        GlStateManager.func_179121_F();
        super.func_76986_a((Entity)entity, x, y, z, entityYaw, partialTicks);
    }

    protected ResourceLocation getEntityTexture(EntityWallDecoration entity) {
        if (entity.type == 1) {
            return textureTapestries;
        }
        return textureSculptures;
    }

    private void renderPainting(EntityWallDecoration painting, int width, int height, int textureU, int textureV) {
        float f = (float)(-width) / 2.0f;
        float f1 = (float)(-height) / 2.0f;
        for (int i = 0; i < width / 16; ++i) {
            for (int j = 0; j < height / 16; ++j) {
                float f15 = f + (float)((i + 1) * 16);
                float f16 = f + (float)(i * 16);
                float f17 = f1 + (float)((j + 1) * 16);
                float f18 = f1 + (float)(j * 16);
                this.setLightmap(painting, (f15 + f16) / 2.0f, (f17 + f18) / 2.0f);
                float f19 = (float)(textureU + width - i * 16) / 256.0f;
                float f20 = (float)(textureU + width - (i + 1) * 16) / 256.0f;
                float f21 = (float)(textureV + height - j * 16) / 256.0f;
                float f22 = (float)(textureV + height - (j + 1) * 16) / 256.0f;
                Tesselator tessellator = Tesselator.getInstance();
                BufferBuilder bufferbuilder = tessellator.getBuilder();
                bufferbuilder.begin(7, DefaultVertexFormat.field_181710_j);
                bufferbuilder.vertex((double)f15, (double)f18, -0.5).uv((double)f20, (double)f21).func_181663_c(0.0f, 0.0f, -1.0f).endVertex();
                bufferbuilder.vertex((double)f16, (double)f18, -0.5).uv((double)f19, (double)f21).func_181663_c(0.0f, 0.0f, -1.0f).endVertex();
                bufferbuilder.vertex((double)f16, (double)f17, -0.5).uv((double)f19, (double)f22).func_181663_c(0.0f, 0.0f, -1.0f).endVertex();
                bufferbuilder.vertex((double)f15, (double)f17, -0.5).uv((double)f20, (double)f22).func_181663_c(0.0f, 0.0f, -1.0f).endVertex();
                bufferbuilder.vertex((double)f15, (double)f17, 0.5).uv(0.75, 0.0).func_181663_c(0.0f, 0.0f, 1.0f).endVertex();
                bufferbuilder.vertex((double)f16, (double)f17, 0.5).uv(0.8125, 0.0).func_181663_c(0.0f, 0.0f, 1.0f).endVertex();
                bufferbuilder.vertex((double)f16, (double)f18, 0.5).uv(0.8125, 0.0625).func_181663_c(0.0f, 0.0f, 1.0f).endVertex();
                bufferbuilder.vertex((double)f15, (double)f18, 0.5).uv(0.75, 0.0625).func_181663_c(0.0f, 0.0f, 1.0f).endVertex();
                bufferbuilder.vertex((double)f15, (double)f17, -0.5).uv(0.75, 0.001953125).func_181663_c(0.0f, 1.0f, 0.0f).endVertex();
                bufferbuilder.vertex((double)f16, (double)f17, -0.5).uv(0.8125, 0.001953125).func_181663_c(0.0f, 1.0f, 0.0f).endVertex();
                bufferbuilder.vertex((double)f16, (double)f17, 0.5).uv(0.8125, 0.001953125).func_181663_c(0.0f, 1.0f, 0.0f).endVertex();
                bufferbuilder.vertex((double)f15, (double)f17, 0.5).uv(0.75, 0.001953125).func_181663_c(0.0f, 1.0f, 0.0f).endVertex();
                bufferbuilder.vertex((double)f15, (double)f18, 0.5).uv(0.75, 0.001953125).func_181663_c(0.0f, -1.0f, 0.0f).endVertex();
                bufferbuilder.vertex((double)f16, (double)f18, 0.5).uv(0.8125, 0.001953125).func_181663_c(0.0f, -1.0f, 0.0f).endVertex();
                bufferbuilder.vertex((double)f16, (double)f18, -0.5).uv(0.8125, 0.001953125).func_181663_c(0.0f, -1.0f, 0.0f).endVertex();
                bufferbuilder.vertex((double)f15, (double)f18, -0.5).uv(0.75, 0.001953125).func_181663_c(0.0f, -1.0f, 0.0f).endVertex();
                bufferbuilder.vertex((double)f15, (double)f17, 0.5).uv(0.751953125, 0.0).func_181663_c(-1.0f, 0.0f, 0.0f).endVertex();
                bufferbuilder.vertex((double)f15, (double)f18, 0.5).uv(0.751953125, 0.0625).func_181663_c(-1.0f, 0.0f, 0.0f).endVertex();
                bufferbuilder.vertex((double)f15, (double)f18, -0.5).uv(0.751953125, 0.0625).func_181663_c(-1.0f, 0.0f, 0.0f).endVertex();
                bufferbuilder.vertex((double)f15, (double)f17, -0.5).uv(0.751953125, 0.0).func_181663_c(-1.0f, 0.0f, 0.0f).endVertex();
                bufferbuilder.vertex((double)f16, (double)f17, -0.5).uv(0.751953125, 0.0).func_181663_c(1.0f, 0.0f, 0.0f).endVertex();
                bufferbuilder.vertex((double)f16, (double)f18, -0.5).uv(0.751953125, 0.0625).func_181663_c(1.0f, 0.0f, 0.0f).endVertex();
                bufferbuilder.vertex((double)f16, (double)f18, 0.5).uv(0.751953125, 0.0625).func_181663_c(1.0f, 0.0f, 0.0f).endVertex();
                bufferbuilder.vertex((double)f16, (double)f17, 0.5).uv(0.751953125, 0.0).func_181663_c(1.0f, 0.0f, 0.0f).endVertex();
                tessellator.end();
            }
        }
    }

    private void setLightmap(EntityWallDecoration painting, float p_77008_2_, float p_77008_3_) {
        int i = Mth.floor((double)painting.getX());
        int j = Mth.floor((double)(painting.getY() + (double)(p_77008_3_ / 16.0f)));
        int k = Mth.floor((double)painting.getZ());
        Direction enumfacing = painting.direction;
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
        int l = this.entityRenderDispatcher.level.func_175626_b(new BlockPos(i, j, k), 0);
        int i1 = l % 65536;
        int j1 = l / 65536;
        OpenGlHelper.func_77475_a((int)OpenGlHelper.field_77476_b, (float)i1, (float)j1);
        GlStateManager.func_179124_c((float)1.0f, (float)1.0f, (float)1.0f);
    }

    public static class FactoryRenderWallDecoration
    implements IRenderFactory<EntityWallDecoration> {
        public EntityRenderer<? super EntityWallDecoration> createRenderFor(EntityRenderDispatcher manager) {
            return new RenderWallDecoration(manager);
        }
    }
}

