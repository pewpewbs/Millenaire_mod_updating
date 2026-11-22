import java.util.*;import net.minecraft.world.item.*;import net.minecraft.world.level.block.*;import net.minecraft.resources.ResourceLocation;import net.minecraft.core.Registry;import org.millenaire.common.item.LegacyItemStacks;import org.millenaire.common.block.WoodVariant;
public class BuildingInventoryCheck{
 record InvItem(Item item,int meta){ItemStack getItemStack(){return LegacyItemStacks.stack(item,1,meta);}}Map<InvItem,Integer> inventoryCache=new HashMap<>();void calculateInventoryCache(){throw new AssertionError("Unexpected cache reload");}
   private int getInventoryCountFromCache(InvItem wanted) {
      if (this.inventoryCache == null) this.calculateInventoryCache();
      if (wanted.meta != -1) return this.inventoryCache.getOrDefault(wanted, 0);
      long count = 0;
      for (var entry : this.inventoryCache.entrySet()) {
         if (LegacyItemStacks.matches(entry.getKey().getItemStack(), wanted.item, -1)) count += entry.getValue();
      }
      return (int)Math.min(Integer.MAX_VALUE, count);
   }


 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();var b=new BuildingInventoryCheck();int expected=0;for(WoodVariant w:WoodVariant.values()){int n=w.ordinal()+1;expected+=n;b.inventoryCache.put(new InvItem(w.log().asItem(),0),n);}check(b.getInventoryCountFromCache(new InvItem(Items.OAK_LOG,-1))==expected,"Six woods counted once");check(b.getInventoryCountFromCache(new InvItem(Items.SPRUCE_LOG,0))==2,"Exact species request");b.inventoryCache.put(new InvItem(Items.OAK_PLANKS,0),900);check(b.getInventoryCountFromCache(new InvItem(Items.OAK_LOG,-1))==expected,"Planks excluded from log request");b.inventoryCache.put(new InvItem(Items.STRIPPED_BIRCH_WOOD,0),7);check(b.getInventoryCountFromCache(new InvItem(Items.OAK_LOG,-1))==expected+7,"Existing wildcard wood behavior preserved");
 b.inventoryCache.clear();expected=0;for(DyeColor c:DyeColor.values()){int n=c.getId()+1;expected+=n;Item item=Registry.ITEM.get(new ResourceLocation(c.getName()+"_wool"));b.inventoryCache.put(new InvItem(item,0),n);}check(b.getInventoryCountFromCache(new InvItem(Items.WHITE_WOOL,-1))==expected,"All sixteen wool variants including black");check(b.getInventoryCountFromCache(new InvItem(Items.WHITE_WOOL,0))==1,"Exact wool request");check(b.getInventoryCountFromCache(new InvItem(Items.COD,0))==0,"Missing goods zero");b.inventoryCache.clear();b.inventoryCache.put(new InvItem(Items.OAK_LOG,0),Integer.MAX_VALUE);b.inventoryCache.put(new InvItem(Items.SPRUCE_LOG,0),5);check(b.getInventoryCountFromCache(new InvItem(Items.OAK_LOG,-1))==Integer.MAX_VALUE,"Wildcard sum does not overflow");
 System.out.println("PASS: extracted production building cache method with InvItem/cache fixtures and actual LegacyItemStacks: six woods without duplicate counts, exact species, plank exclusion, stripped wood, all sixteen wool colors, absent goods and overflow saturation. Real storage mutation, grove growth and live animal respawn untested.");}}
