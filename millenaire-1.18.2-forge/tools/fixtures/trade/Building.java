package org.millenaire.common.village;
import java.util.*;import net.minecraft.world.item.*;import net.minecraft.world.entity.player.Player;import org.millenaire.common.item.*;
public class Building {
 public net.minecraft.world.level.Level world;public java.util.UUID controlledBy;public org.millenaire.common.culture.Culture culture=new org.millenaire.common.culture.Culture();public org.millenaire.common.world.MillWorldData mw=new org.millenaire.common.world.MillWorldData();
 public Set<TradeGood> selling=new LinkedHashSet<>(),buying=new LinkedHashSet<>();public Map<Item,Integer> stock=new HashMap<>();public int reputation,language,capacity=1000;
 public Set<TradeGood> getSellingGoods(Player p){return selling;}public Set<TradeGood> getBuyingGoods(Player p){return buying;}public Building getTownHall(){return this;}
 public int countGoods(Item i,int meta){return stock.getOrDefault(i,0);}public int countGoods(InvItem i){return countGoods(i.item,0);}
 public int takeGoods(Item i,int meta,int count){int n=Math.min(count,countGoods(i,meta));stock.put(i,countGoods(i,meta)-n);return n;}public int takeGoods(InvItem i,int n){return takeGoods(i.item,0,n);}
 public int storeGoods(Item i,int meta,int count){int n=Math.min(count,Math.max(0,capacity-countGoods(i,meta)));stock.put(i,countGoods(i,meta)+n);return n;}
 public void adjustReputation(Player p,int n){reputation+=n;}public void adjustLanguage(Player p,int n){language+=n;}public int getReputation(Player p){return 10000;}public boolean containsTags(String tag){return true;}
 public void broadcastChanges(){}public void invalidateInventoryCache(){}public void sendChestPackets(Player p){}public String getNativeBuildingName(){return "test";}
 public org.millenaire.common.utilities.Point getPos(){return new org.millenaire.common.utilities.Point();}public Building getResManager(){return this;}public org.millenaire.common.utilities.Point getSellingPos(){return getPos();}
}