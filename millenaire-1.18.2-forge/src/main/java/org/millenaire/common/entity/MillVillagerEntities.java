package org.millenaire.common.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "millenaire", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MillVillagerEntities {
    public static EntityType<MillVillager.EntityGenericMale> MALE;
    public static EntityType<MillVillager.EntityGenericSymmFemale> FEMALE_SYM;
    public static EntityType<MillVillager.EntityGenericAsymmFemale> FEMALE_ASYM;

    private static <T extends MillVillager> EntityType<T> type(ResourceLocation id, EntityType.EntityFactory<T> factory) {
        EntityType<T> type = EntityType.Builder.of(factory, MobCategory.CREATURE)
                .sized(0.6F, 1.8F).fireImmune().clientTrackingRange(8).updateInterval(3).build(id.toString());
        type.setRegistryName(id);
        return type;
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<EntityType<?>> event) {
        MALE = type(MillVillager.GENERIC_VILLAGER, MillVillager.EntityGenericMale::new);
        FEMALE_SYM = type(MillVillager.GENERIC_SYMM_FEMALE, MillVillager.EntityGenericSymmFemale::new);
        FEMALE_ASYM = type(MillVillager.GENERIC_ASYMM_FEMALE, MillVillager.EntityGenericAsymmFemale::new);
        event.getRegistry().registerAll(MALE, FEMALE_SYM, FEMALE_ASYM);
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(MALE, MillVillager.createVillagerAttributes().build());
        event.put(FEMALE_SYM, MillVillager.createVillagerAttributes().build());
        event.put(FEMALE_ASYM, MillVillager.createVillagerAttributes().build());
    }

    public static MillVillager create(ResourceLocation id, Level world) {
        if (id.equals(MillVillager.GENERIC_VILLAGER)) return MALE.create(world);
        if (id.equals(MillVillager.GENERIC_SYMM_FEMALE)) return FEMALE_SYM.create(world);
        if (id.equals(MillVillager.GENERIC_ASYMM_FEMALE)) return FEMALE_ASYM.create(world);
        throw new IllegalArgumentException("Unknown Millenaire villager type: " + id);
    }
}
