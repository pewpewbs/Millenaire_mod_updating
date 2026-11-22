package org.millenaire.common.entity;
import net.minecraft.world.entity.*;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="millenaire", bus=Mod.EventBusSubscriber.Bus.MOD)
public final class MillDecorationEntities {
    public static EntityType<EntityWallDecoration> WALL_DECORATION;
    @SubscribeEvent public static void register(RegistryEvent.Register<EntityType<?>> event) {
        WALL_DECORATION = EntityType.Builder.<EntityWallDecoration>of(EntityWallDecoration::new, MobCategory.MISC)
            .sized(.5f, .5f).clientTrackingRange(4).updateInterval(Integer.MAX_VALUE)
            .build(EntityWallDecoration.WALL_DECORATION.toString());
        WALL_DECORATION.setRegistryName(EntityWallDecoration.WALL_DECORATION);
        event.getRegistry().register(WALL_DECORATION);
    }
}
