package net.minecraftforge.registries;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.advancements.*;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.world.level.storage.loot.PredicateManager;
import org.millenaire.common.advancements.MillAdvancements;
/** Parse production JSON with Minecraft's loader; only icons and config/network services use fixtures. */
public class AdvancementResourceCheck {
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
        ((ForgeRegistry<Item>)ForgeRegistries.ITEMS).unfreeze();((NamespacedDefaultedWrapper<Item>)Registry.ITEM).unfreeze();
        Path folder=Path.of(args[0]);Map<ResourceLocation,JsonObject> jsons=new LinkedHashMap<>();
        try(var paths=Files.list(folder)) { for(Path file:paths.filter(p->p.toString().endsWith(".json")).sorted().toList()) {
            JsonObject json=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            ResourceLocation icon=new ResourceLocation(json.getAsJsonObject("display").getAsJsonObject("icon").get("item").getAsString());
            if(icon.getNamespace().equals("millenaire") && !ForgeRegistries.ITEMS.containsKey(icon))ForgeRegistries.ITEMS.register(new Item(new Item.Properties()).setRegistryName(icon));
            if(!ForgeRegistries.ITEMS.containsKey(icon))throw new AssertionError("Invalid vanilla icon: "+icon);
            jsons.put(new ResourceLocation("millenaire",file.getFileName().toString().replace(".json","")),json);
        }}
        MillAdvancements.registerTriggers();MillAdvancements.registerTriggers();
        var builders=new LinkedHashMap<ResourceLocation,Advancement.Builder>();var predicates=new PredicateManager();
        for(var entry:jsons.entrySet())builders.put(entry.getKey(),Advancement.Builder.fromJson(entry.getValue(),new DeserializationContext(entry.getKey(),predicates)));
        AdvancementList list=new AdvancementList();list.add(builders);
        if(list.getAllAdvancements().size()!=jsons.size())throw new AssertionError("Unresolved advancement parents");
        for(var advancement:list.getAllAdvancements()) {
            if(advancement.getDisplay().getIcon().isEmpty())throw new AssertionError("Missing icon");
            JsonObject expected=jsons.get(advancement.getId()).getAsJsonObject("criteria");
            if(!advancement.getCriteria().keySet().equals(expected.keySet()))throw new AssertionError("Criterion keys changed");
            for(var criterion:advancement.getCriteria().values())if(!criterion.getTrigger().getCriterion().getNamespace().equals("millenaire"))throw new AssertionError("Trigger namespace lost");
        }
        for(var entry:Map.of("complete_norman",1,"complete_mayan",2,"complete_seljuk",1).entrySet()) {
            var icon=list.get(new ResourceLocation("millenaire",entry.getKey())).getDisplay().getIcon();
            if(icon.getTag()==null || icon.getTag().getInt("MillenaireVariant")!=entry.getValue())throw new AssertionError("Legacy icon variant lost");
        }
        System.out.println("PASS: "+jsons.size()+" advancement JSON files parsed by Minecraft, all parents and triggers resolved, icon NBT retained");
    }
}
