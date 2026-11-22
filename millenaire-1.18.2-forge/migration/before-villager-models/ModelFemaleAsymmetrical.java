/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.ModelBase
 *  net.minecraft.client.model.ModelRenderer
 *  net.minecraft.entity.Entity
 */
package org.millenaire.client.render;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import org.millenaire.client.render.ModelMillVillager;

public class ModelFemaleAsymmetrical
extends ModelMillVillager {
    private final ModelPart bipedBreast;

    public ModelFemaleAsymmetrical() {
        this(0.0f);
    }

    public ModelFemaleAsymmetrical(float f) {
        this(f, 0.0f);
    }

    public ModelFemaleAsymmetrical(float f, float f1) {
        this.field_78117_n = false;
        this.head = new ModelPart((Model)this, 0, 0);
        this.head.func_78790_a(-4.0f, -8.0f, -4.0f, 8, 8, 8, f);
        this.head.setPos(0.0f, 0.0f + f1, 0.0f);
        this.hat = new ModelPart((Model)this, 32, 0);
        this.hat.func_78790_a(-4.0f, -8.0f, -4.0f, 8, 8, 8, f + 0.5f);
        this.hat.setPos(0.0f, 0.0f + f1, 0.0f);
        this.body = new ModelPart((Model)this, 16, 17);
        this.body.func_78790_a(-3.5f, 0.0f, -1.5f, 7, 12, 3, f);
        this.body.setPos(0.0f, 0.0f + f1, 0.0f);
        this.bipedBreast = new ModelPart((Model)this, 17, 18);
        this.bipedBreast.func_78790_a(-3.5f, 0.75f, -3.0f, 7, 4, 2, f);
        this.bipedBreast.setPos(0.0f, 0.0f + f1, 0.0f);
        this.body.func_78792_a(this.bipedBreast);
        this.rightArm = new ModelPart((Model)this, 36, 17);
        this.rightArm.func_78790_a(-1.5f, -2.0f, -1.5f, 3, 12, 3, f);
        this.rightArm.setPos(-5.0f, 2.0f + f1, 0.0f);
        this.leftArm = new ModelPart((Model)this, 36, 17);
        this.leftArm.field_78809_i = true;
        this.leftArm.func_78790_a(-1.5f, -2.0f, -1.5f, 3, 12, 3, f);
        this.leftArm.setPos(5.0f, 2.0f + f1, 0.0f);
        this.rightLeg = new ModelPart((Model)this, 0, 16);
        this.rightLeg.func_78790_a(-2.0f, 0.0f, -2.0f, 4, 12, 4, f);
        this.rightLeg.setPos(-2.0f, 12.0f + f1, 0.0f);
        this.leftLeg = new ModelPart((Model)this, 48, 16);
        this.leftLeg.func_78790_a(-2.0f, 0.0f, -2.0f, 4, 12, 4, f);
        this.leftLeg.setPos(2.0f, 12.0f + f1, 0.0f);
    }

    public void func_78088_a(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        super.func_78088_a(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
    }
}

