import org.millenaire.common.utilities.LegacyBannerItems;import net.minecraft.world.item.*;import net.minecraft.nbt.*;
public class CultureBannerCheck {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();int checked=0;
  for(int base=0;base<16;base++)for(int color=0;color<16;color++){
   var stack=LegacyBannerItems.fromConfig("{blockentitytag:{base:"+base+",patterns:[{pattern:by1,color:"+color+"},{pattern:sc,color:15}]},marker:\"base pattern color\"}");
   check(stack.getCount()==1&&((BannerItem)stack.getItem()).getColor()==DyeColor.byId(15-base),"Base dye inversion "+base);
   var banner=stack.getTag().getCompound("BlockEntityTag");var patterns=banner.getList("Patterns",10);check(patterns.size()==2&&patterns.getCompound(0).getString("Pattern").equals("by1")&&patterns.getCompound(1).getString("Pattern").equals("sc"),"Custom patterns and order retained");check(patterns.getCompound(0).getInt("Color")==15-color&&patterns.getCompound(1).getInt("Color")==0,"Pattern dye inversion "+color);check(stack.getTag().getString("marker").equals("base pattern color"),"Unrelated values unchanged");checked++;
  }
  for(var color:DyeColor.values()){
   var patterns=new ListTag();var pattern=new CompoundTag();pattern.putString("Pattern","by1");pattern.putInt("Color",color.getId());patterns.add(pattern);var stack=LegacyBannerItems.create(color,patterns);patterns.clear();check(((BannerItem)stack.getItem()).getColor()==color&&stack.getTag().getCompound("BlockEntityTag").getList("Patterns",10).getCompound(0).getInt("Color")==color.getId(),"Modern creation keeps colors and copies patterns");
  }
  int configs=0;for(String line:java.nio.file.Files.readAllLines(java.nio.file.Path.of(args[0]))){check(LegacyBannerItems.fromConfig(line).getItem() instanceof BannerItem,"Original banner configuration");configs++;}
  System.out.println("Original culture/village banner configurations converted: "+configs);
  check(LegacyBannerItems.fromConfig("{}").is(Items.BLACK_BANNER),"Legacy damage-zero default remains black");
  for(int invalid:new int[]{-1,16,38,99}){var stack=LegacyBannerItems.fromConfig("{BlockEntityTag:{Base:"+invalid+",Patterns:[{Pattern:iu1,Color:"+invalid+"}]}}");check(stack.is(Items.BLACK_BANNER)&&stack.getTag().getCompound("BlockEntityTag").getList("Patterns",10).getCompound(0).getInt("Color")==15,"Legacy invalid-color black fallback");}
  try{LegacyBannerItems.fromConfig("{broken");throw new AssertionError("Malformed NBT accepted");}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){}
  System.out.println("PASS: "+checked+" base/pattern color combinations, custom pattern IDs/order, lowercase legacy keys, unrelated values, old black default and invalid inputs. Actual Minecraft item data fixer; in-game banner rendering untested.");
 }
}