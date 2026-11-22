package org.millenaire.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.millenaire.common.entity.MillVillager;

public class LayerVillagerClothes extends RenderLayer<MillVillager, ModelMillVillager> {
    protected final ModelMillVillager modelCloth;
    private final int layer;

    public LayerVillagerClothes(RenderLayerParent<MillVillager, ModelMillVillager> renderer,
            ModelMillVillager model, int layer) {
        super(renderer);
        this.layer = layer;
        float inflation = 0.1F * (layer + 1);
        modelCloth = model instanceof ModelFemaleAsymmetrical ? new ModelFemaleAsymmetrical(inflation)
                : model instanceof ModelFemaleSymmetrical ? new ModelFemaleSymmetrical(inflation)
                : new ModelMillVillager(inflation);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, MillVillager villager,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
            float netHeadYaw, float headPitch) {
        var texture = villager.getClothTexturePath(layer);
        if (texture == null || villager.isInvisible()) return;
        getParentModel().copyPropertiesTo(modelCloth);
        modelCloth.prepareMobModel(villager, limbSwing, limbSwingAmount, partialTicks);
        modelCloth.setupAnim(villager, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        modelCloth.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)),
                light, LivingEntityRenderer.getOverlayCoords(villager, 0.0F), 1, 1, 1, 1);
    }
}
