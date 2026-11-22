package org.millenaire.common.utilities;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.*;

/** Names in the original culture files, compared with modern biome registry identifiers. */
public final class LegacyBiomeNames {
    private static final Map<String,String> ALIASES=aliases();
    private static final Map<String,ResourceLocation> CONVERTED=new ConcurrentHashMap<>();
    private LegacyBiomeNames() {}
    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT).replace("+","plus").replaceAll("[^a-z0-9]","");
    }
    private static Map<String,String> aliases() {
        Map<String,String> names=new HashMap<>();
        for(String id:BiomeFix.BIOMES.keySet()) names.put(normalize(id.substring(id.indexOf(':')+1)),id);
        for(String id:List.of("ocean","plains","desert","forest","taiga","river","frozen_ocean","frozen_river","desert_hills","taiga_hills","jungle","jungle_hills","jungle_edge","deep_ocean","birch_forest","birch_forest_hills","savanna")) names.put(normalize(id),"minecraft:"+id);
        String[][] displayNames={
            {"Extreme Hills","extreme_hills"},{"Extreme Hills Edge","smaller_extreme_hills"},{"Extreme Hills+","extreme_hills_with_trees"},
            {"Ice Plains","ice_flats"},{"MushroomIsland","mushroom_island"},{"MushroomIslandShore","mushroom_island_shore"},
            {"Beach","beaches"},{"ForestHills","forest_hills"},{"Cold Taiga","taiga_cold"},{"Cold Taiga Hills","taiga_cold_hills"},
            {"Mega Taiga","redwood_taiga"},{"Mega Taiga Hills","redwood_taiga_hills"},{"Savanna Plateau","savanna_rock"},
            {"Mesa Plateau F","mesa_rock"},{"Mesa Plateau","mesa_clear_rock"},
            {"Sunflower Plains","mutated_plains"},{"Flower Forest","mutated_forest"},{"Ice Plains Spikes","mutated_ice_flats"},
            {"Mega Spruce Taiga","mutated_redwood_taiga"},{"Redwood Taiga Hills M","mutated_redwood_taiga_hills"},
            {"The End","sky"},{"The Void","void"}
        };
        for(String[] entry:displayNames)names.put(normalize(entry[0]),"minecraft:"+entry[1]);
        Map<String,String> mutations=new HashMap<>();
        names.forEach((name,id)->{
            String mutated="minecraft:mutated_"+id.substring("minecraft:".length());
            if(BiomeFix.BIOMES.containsKey(mutated))mutations.put(name+"m",mutated);
        });
        names.putAll(mutations);
        // Misspelling present in the original culture data.
        names.put(normalize("Mesa Platau F M"),"minecraft:mutated_mesa_rock");
        return Map.copyOf(names);
    }
    public static ResourceLocation upgrade(String id) {
        return CONVERTED.computeIfAbsent(id,key->{
            if(!key.startsWith("minecraft:"))return new ResourceLocation(key);
            var fixed=DataFixers.getDataFixer().update(References.BIOME,new Dynamic<>(JsonOps.INSTANCE,new JsonPrimitive(key)),1343,2975);
            return new ResourceLocation(fixed.asString(key));
        });
    }
    public static boolean matches(String configured,ResourceLocation actual) {
        if(configured==null || actual==null)return false;
        if(configured.contains(":")) {
            ResourceLocation id=ResourceLocation.tryParse(configured.toLowerCase(Locale.ROOT));
            return id!=null && upgrade(id.toString()).equals(actual);
        }
        String legacy=ALIASES.get(normalize(configured));
        if(legacy!=null)return upgrade(legacy).equals(actual);
        return normalize(configured).equals(normalize(actual.getPath()));
    }
    public static boolean matchesAny(Collection<String> configured,String actualId) {
        ResourceLocation actual=ResourceLocation.tryParse(actualId);
        return actual!=null && configured.stream().anyMatch(name->matches(name,actual));
    }
}
