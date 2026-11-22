package org.millenaire.common.entity;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="millenaire",bus=Mod.EventBusSubscriber.Bus.MOD)
public final class MillQuestEntities {
    public static EntityType<EntityTargetedBlaze> BLAZE;
    public static EntityType<EntityTargetedGhast> GHAST;
    public static EntityType<EntityTargetedWitherSkeleton> WITHER_SKELETON;
    @SubscribeEvent public static void register(RegistryEvent.Register<EntityType<?>> event) {
        BLAZE=EntityType.Builder.<EntityTargetedBlaze>of(EntityTargetedBlaze::new,MobCategory.MONSTER).sized(.6f,1.8f).fireImmune().clientTrackingRange(4).updateInterval(3).build("millenaire:millblaze");
        GHAST=EntityType.Builder.<EntityTargetedGhast>of(EntityTargetedGhast::new,MobCategory.MONSTER).sized(4,4).fireImmune().clientTrackingRange(4).updateInterval(3).build("millenaire:millghast");
        WITHER_SKELETON=EntityType.Builder.<EntityTargetedWitherSkeleton>of(EntityTargetedWitherSkeleton::new,MobCategory.MONSTER).sized(.6f,1.99f).clientTrackingRange(4).updateInterval(3).build("millenaire:millwitherskeleton");
        BLAZE.setRegistryName("millenaire","millblaze");GHAST.setRegistryName("millenaire","millghast");WITHER_SKELETON.setRegistryName("millenaire","millwitherskeleton");
        event.getRegistry().registerAll(BLAZE,GHAST,WITHER_SKELETON);
    }
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event) {
        event.put(BLAZE,Blaze.createAttributes().build());event.put(GHAST,Ghast.createAttributes().build());event.put(WITHER_SKELETON,AbstractSkeleton.createAttributes().build());
    }
}
