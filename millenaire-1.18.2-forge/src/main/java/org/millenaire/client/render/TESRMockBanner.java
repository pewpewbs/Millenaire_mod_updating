package org.millenaire.client.render;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.world.level.block.WallBannerBlock;
import org.millenaire.common.block.mock.MockBlockBannerStanding;
import org.millenaire.common.entity.TileEntityMockBanner;

public class TESRMockBanner implements BlockEntityRenderer<TileEntityMockBanner> {
    private final ModelPart flag, pole, bar;
    public TESRMockBanner(BlockEntityRendererProvider.Context context) {
        ModelPart model = context.bakeLayer(ModelLayers.BANNER); flag = model.getChild("flag"); pole = model.getChild("pole"); bar = model.getChild("bar");
    }
    @Override public void render(TileEntityMockBanner banner, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var state = banner.getBlockState(); pose.pushPose();
        boolean standing = state.getBlock() instanceof MockBlockBannerStanding;
        if (standing) { pose.translate(0.5,0.5,0.5); pose.mulPose(Vector3f.YP.rotationDegrees(-state.getValue(MockBlockBannerStanding.ROTATION) * 360.0F / 16)); }
        else { pose.translate(0.5,-1.0/6,0.5); pose.mulPose(Vector3f.YP.rotationDegrees(-state.getValue(WallBannerBlock.FACING).toYRot())); pose.translate(0,-0.3125,-0.4375); }
        pole.visible = standing; pose.scale(2.0F/3,-2.0F/3,-2.0F/3);
        var consumer = ModelBakery.BANNER_BASE.buffer(buffers, RenderType::entitySolid);
        pole.render(pose,consumer,light,overlay); bar.render(pose,consumer,light,overlay);
        var pos = banner.getBlockPos(); long time = banner.getLevel() == null ? 0 : banner.getLevel().getGameTime();
        float phase = (Math.floorMod(pos.getX()*7L + pos.getY()*9L + pos.getZ()*13L + time, 100) + partial) / 100;
        flag.xRot = (-0.0125F + 0.01F * net.minecraft.util.Mth.cos((float)Math.PI * 2 * phase)) * (float)Math.PI; flag.y = -32;
        BannerRenderer.renderPatterns(pose,buffers,light,overlay,flag,ModelBakery.BANNER_BASE,true,banner.getPatterns());
        pose.popPose();
    }
}
