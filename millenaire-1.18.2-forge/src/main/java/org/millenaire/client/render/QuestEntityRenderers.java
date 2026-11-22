package org.millenaire.client.render;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.renderer.entity.*;
import org.millenaire.common.entity.MillQuestEntities;
@Mod.EventBusSubscriber(modid="millenaire",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class QuestEntityRenderers {
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(org.millenaire.common.entity.MillVillagerEntities.MALE,RenderMillVillager.FACTORY_MALE);
        event.registerEntityRenderer(org.millenaire.common.entity.MillVillagerEntities.FEMALE_SYM,RenderMillVillager.FACTORY_FEMALE_SYM);
        event.registerEntityRenderer(org.millenaire.common.entity.MillVillagerEntities.FEMALE_ASYM,RenderMillVillager.FACTORY_FEMALE_ASYM);
        event.registerEntityRenderer(org.millenaire.common.entity.MillDecorationEntities.WALL_DECORATION,RenderWallDecoration::new);
        event.registerEntityRenderer(MillQuestEntities.BLAZE,BlazeRenderer::new);
        event.registerEntityRenderer(MillQuestEntities.GHAST,GhastRenderer::new);
        event.registerEntityRenderer(MillQuestEntities.WITHER_SKELETON,SkeletonRenderer::new);
    }
}
