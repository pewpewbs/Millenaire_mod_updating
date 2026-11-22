package org.millenaire.client.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

public class ModelFemaleAsymmetrical extends ModelMillVillager {
    public final ModelPart bipedBreast;

    public ModelFemaleAsymmetrical() { this(0.0F); }
    public ModelFemaleAsymmetrical(float inflation) { this(inflation, 0.0F); }
    public ModelFemaleAsymmetrical(float inflation, float offsetY) {
        super(createBodyLayer(inflation, offsetY).bakeRoot());
        bipedBreast = body.getChild("breast");
    }

    public static LayerDefinition createBodyLayer(float inflation, float offsetY) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation size = new CubeDeformation(inflation);
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, -8, -4, 8, 8, 8, size), PartPose.offset(0, offsetY, 0));
        root.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0)
                .addBox(-4, -8, -4, 8, 8, 8, new CubeDeformation(inflation + 0.5F)), PartPose.offset(0, offsetY, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 17)
                .addBox(-3.5F, 0, -1.5F, 7, 12, 3, size), PartPose.offset(0, offsetY, 0));
        body.addOrReplaceChild("breast", CubeListBuilder.create().texOffs(17, 18)
                .addBox(-3.5F, 0.75F, -3, 7, 4, 2, size), PartPose.offset(0, offsetY, 0));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(36, 17)
                .addBox(-1.5F, -2, -1.5F, 3, 12, 3, size), PartPose.offset(-5, 2 + offsetY, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(36, 17).mirror()
                .addBox(-1.5F, -2, -1.5F, 3, 12, 3, size), PartPose.offset(5, 2 + offsetY, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-2, 0, -2, 4, 12, 4, size), PartPose.offset(-2, 12 + offsetY, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(48, 16)
                .addBox(-2, 0, -2, 4, 12, 4, size), PartPose.offset(2, 12 + offsetY, 0));
        return LayerDefinition.create(mesh, 64, 32);
    }
}
