import org.millenaire.common.utilities.LegacyBiomeNames;import net.minecraft.resources.ResourceLocation;import net.minecraft.data.BuiltinRegistries;import java.util.*;
public class BiomeNamesCheck {
 public static void main(String[] args){
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
  String[][] cases={{"Extreme Hills","windswept_hills"},{"ExtremeHills+","windswept_forest"},{"Extreme Hills M","windswept_gravelly_hills"},{"Ice plains","snowy_plains"},{"Ice Plains Spikes","ice_spikes"},{"Cold Taiga","snowy_taiga"},{"Cold Taiga M","snowy_taiga"},{"Roofed Forest","dark_forest"},{"ForestHills","forest"},{"Birch Forest Hills M","old_growth_birch_forest"},{"Mega Taiga","old_growth_pine_taiga"},{"Mega Spruce Taiga","old_growth_spruce_taiga"},{"Mesa Plateau F","wooded_badlands"},{"Mesa Platau F M","wooded_badlands"},{"JungleEdge M","sparse_jungle"},{"Savanna M","windswept_savanna"},{"Sunflower Plains","sunflower_plains"},{"Stone beach","stony_shore"},{"Cold Beach","snowy_beach"},{"DesertHills","desert"},{"minecraft:ice_flats","snowy_plains"},{"minecraft:snowy_plains","snowy_plains"}};
  for(var pair:cases){var id=new ResourceLocation("minecraft",pair[1]);if(!BuiltinRegistries.BIOME.containsKey(id) || !LegacyBiomeNames.matches(pair[0],id))throw new AssertionError(Arrays.toString(pair));}
  if(!LegacyBiomeNames.matches("Cherry Blossom Grove",new ResourceLocation("example","cherry_blossom_grove")))throw new AssertionError("Mod display name");
  if(!LegacyBiomeNames.matches("example:grove",new ResourceLocation("example","grove")) || LegacyBiomeNames.matches("example:grove",new ResourceLocation("minecraft","grove")))throw new AssertionError("Namespace collision");
  if(LegacyBiomeNames.matches("Plains",new ResourceLocation("example","plains")) || LegacyBiomeNames.matches("Ocean",new ResourceLocation("minecraft","desert")))throw new AssertionError("False match");
  if(!LegacyBiomeNames.matchesAny(List.of("Forest","Extreme Hills"),"minecraft:windswept_hills"))throw new AssertionError("Village biome list");
  System.out.println("PASS: 22 legacy/modern biome aliases against Minecraft registry; mod biome names, namespace isolation, negative matches and village-list matching. Generation placement not game-tested.");
 }
}
