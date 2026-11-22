package org.millenaire.client.render;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

public class ModelPanel {
    public final ModelPart signBoard;
    public ModelPanel() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("board",CubeListBuilder.create().texOffs(0,0).addBox(-12,-12,-1,24,24,2),PartPose.ZERO);
        signBoard=LayerDefinition.create(mesh,64,32).bakeRoot().getChild("board");
    }
    public void renderSign(PoseStack pose,VertexConsumer consumer,int light,int overlay) { signBoard.render(pose,consumer,light,overlay); }
}
