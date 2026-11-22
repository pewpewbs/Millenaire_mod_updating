package org.millenaire.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import org.millenaire.common.entity.MillVillager;

public class ModelMillVillager extends HumanoidModel<MillVillager> {
    public ModelMillVillager() { this(0.0F); }
    public ModelMillVillager(float inflation) {
        this(LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(inflation), 0.0F), 64, 32).bakeRoot());
    }
    protected ModelMillVillager(ModelPart root) { super(root); }

    @Override
    public void setupAnim(MillVillager villager, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(villager, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (villager.travelBookMockVillager && villager.heldItem != null) {
            leftArm.xRot = -0.6F;
            leftArm.zRot = -0.2F;
        }
        if (villager.travelBookMockVillager && villager.heldItemOffHand != null) {
            rightArm.xRot = -0.5F;
            rightArm.zRot = 0.1F;
        }
    }
}
