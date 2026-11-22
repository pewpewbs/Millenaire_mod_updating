package org.millenaire.common.entity;
import net.minecraft.world.entity.*;import net.minecraft.world.entity.ai.attributes.*;import net.minecraft.resources.ResourceLocation;import net.minecraft.world.level.Level;
public abstract class MillVillager extends PathfinderMob {
    public static final ResourceLocation GENERIC_VILLAGER = new ResourceLocation("millenaire", "genericvillager");
    public static final ResourceLocation GENERIC_ASYMM_FEMALE = new ResourceLocation("millenaire", "genericasimmfemale");
    public static final ResourceLocation GENERIC_SYMM_FEMALE = new ResourceLocation("millenaire", "genericsimmfemale");
    public static final ResourceLocation GENERIC_ZOMBIE = new ResourceLocation("millenaire", "genericzombie");    public static AttributeSupplier.Builder createVillagerAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.MAX_HEALTH, 40.0);
    }

protected MillVillager(EntityType<? extends MillVillager> type,Level world){super(type,world);}
public static class EntityGenericMale extends MillVillager { public EntityGenericMale(EntityType<? extends MillVillager> type,Level world){super(type,world);} }
public static class EntityGenericSymmFemale extends MillVillager { public EntityGenericSymmFemale(EntityType<? extends MillVillager> type,Level world){super(type,world);} }
public static class EntityGenericAsymmFemale extends MillVillager { public EntityGenericAsymmFemale(EntityType<? extends MillVillager> type,Level world){super(type,world);} }
}
