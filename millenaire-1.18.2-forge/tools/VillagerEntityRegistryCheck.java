package net.minecraftforge.registries;
import java.util.HashMap;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import org.millenaire.common.entity.MillVillagerEntities;
/** Registry lifecycle check only; spawning, AI and rendering require a running Forge world. */
public class VillagerEntityRegistryCheck {
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
        ((ForgeRegistry<EntityType<?>>)ForgeRegistries.ENTITIES).unfreeze();
        ((NamespacedDefaultedWrapper<EntityType<?>>)Registry.ENTITY_TYPE).unfreeze();
        MillVillagerEntities.register(new RegistryEvent.Register<>(ForgeRegistries.ENTITIES.getRegistryName(),ForgeRegistries.ENTITIES));
        // Reproduce Forge's built-in attribute registration in this isolated bootstrap.
        ((ForgeRegistry<Attribute>)ForgeRegistries.ATTRIBUTES).unfreeze();
        ((NamespacedWrapper<Attribute>)Registry.ATTRIBUTE).unfreeze();
        var field=net.minecraftforge.common.ForgeMod.class.getDeclaredField("ATTRIBUTES");field.setAccessible(true);
        var deferred=(net.minecraftforge.registries.DeferredRegister<?>)field.get(null);
        // The standalone JVM has no event-bus transformer; invoke the real registration body.
        var register=net.minecraftforge.registries.DeferredRegister.class.getDeclaredMethod("addEntries",RegistryEvent.Register.class);register.setAccessible(true);
        register.invoke(deferred,new RegistryEvent.Register<>(ForgeRegistries.ATTRIBUTES.getRegistryName(),ForgeRegistries.ATTRIBUTES));
        var attributes=new HashMap<EntityType<? extends LivingEntity>,AttributeSupplier>();
        MillVillagerEntities.attributes(new EntityAttributeCreationEvent(attributes));
        for(String name:new String[]{"genericvillager","genericsimmfemale","genericasimmfemale"}) {
            var type=ForgeRegistries.ENTITIES.getValue(new ResourceLocation("millenaire",name));
            if(type==null || Registry.ENTITY_TYPE.getId(type)<0)throw new AssertionError("Missing registry entry: "+name);
            if(!attributes.containsKey(type) || attributes.get(type).getValue(Attributes.MAX_HEALTH)<=0)throw new AssertionError("Missing living attributes: "+name);
            if(type.clientTrackingRange()*16!=128 || type.updateInterval()!=3)throw new AssertionError("Legacy tracking settings changed");
        }
        for(var type:new EntityType<?>[]{MillVillagerEntities.MALE,MillVillagerEntities.FEMALE_SYM,MillVillagerEntities.FEMALE_ASYM}) {
            if(!type.fireImmune() || Math.abs(type.getWidth()-.6F)>.001 || Math.abs(type.getHeight()-1.8F)>.001)throw new AssertionError("Dimensions or immunity");
            if(attributes.get(type).getValue(Attributes.MAX_HEALTH)!=40 || attributes.get(type).getValue(Attributes.MOVEMENT_SPEED)!=.5)throw new AssertionError("Initial villager attributes");
        }
        System.out.println("PASS: three villager registry entries, copied production attribute builder, dimensions, fire immunity and 128-block tracking. Entity constructors, spawn data and gameplay use fixtures or remain untested.");
    }
}
