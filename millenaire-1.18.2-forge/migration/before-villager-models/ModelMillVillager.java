/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBiped
 *  net.minecraft.entity.Entity
 */
package org.millenaire.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import org.millenaire.common.entity.MillVillager;

public class ModelMillVillager
extends HumanoidModel {
    public ModelMillVillager() {
        this(0.0f);
    }

    public ModelMillVillager(float f) {
        super(f);
    }

    public void func_78087_a(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        super.func_78087_a(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
        MillVillager villager = (MillVillager)entityIn;
        if (villager.travelBookMockVillager && villager.heldItem != null) {
            this.leftArm.xRot = -0.6f;
            this.leftArm.zRot = -0.2f;
        }
        if (villager.travelBookMockVillager && villager.heldItemOffHand != null) {
            this.rightArm.xRot = -0.5f;
            this.rightArm.zRot = 0.1f;
        }
    }
}

