from pathlib import Path
r=Path(__file__).resolve().parents[1];f=r/'tools/fixtures/trade';f.mkdir(parents=True,exist_ok=True)
fixtures={
'InvItem':'''package org.millenaire.common.item;import net.minecraft.world.item.*;public class InvItem {public final Item item;public final int meta=0;public InvItem(Item i){item=i;}public Item getItem(){return item;}}''',
'TradeGood':'''package org.millenaire.common.item;import net.minecraft.world.item.*;import net.minecraft.world.entity.player.Player;import org.millenaire.common.village.Building;import org.millenaire.common.entity.MillVillager;public class TradeGood {public InvItem item;public String name,requiredTag;public int minReputation;public boolean autoGenerate;public int sell=10,buy=3;public TradeGood(Item i){item=new InvItem(i);name=i.toString();}public String getName(){return name;}public int getCalculatedSellingPrice(Building b,Player p){return sell;}public int getCalculatedBuyingPrice(Building b,Player p){return buy;}public int getCalculatedSellingPrice(MillVillager v){return sell;}}''',
'LegacyItemStacks':'''package org.millenaire.common.item;import net.minecraft.world.item.*;public class LegacyItemStacks {public static ItemStack stack(Item item,int count,int meta){return new ItemStack(item,count);}}''',
'Culture':'''package org.millenaire.common.culture;import org.millenaire.common.item.*;public class Culture {public String key="test";public TradeGood getTradeGood(InvItem item){return new TradeGood(item.item);}public String getReputationLevelLabel(int rep){return "test";}}''',
'UserProfile':'''package org.millenaire.common.world;public class UserProfile {public boolean donationActivated;public void unlockTradeGoods(org.millenaire.common.culture.Culture c,java.util.List<org.millenaire.common.item.TradeGood> g){}public void adjustLanguage(String culture,int count){}}''',
'MillWorldData':'''package org.millenaire.common.world;public class MillWorldData {public final UserProfile profile=new UserProfile();public UserProfile getProfile(net.minecraft.world.entity.player.Player p){return profile;}}''',
'Mill':'''package org.millenaire.common.forge;public class Mill {public static final org.millenaire.common.world.MillWorldData world=new org.millenaire.common.world.MillWorldData();public static org.millenaire.common.world.MillWorldData getMillWorld(net.minecraft.world.level.Level level){return world;}}''',
'Point':'''package org.millenaire.common.utilities;public class Point {public double x,y,z;public net.minecraft.core.BlockPos getBlockPos(){return net.minecraft.core.BlockPos.ZERO;}}''',
'Building':'''package org.millenaire.common.village;
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
}''',
'MillVillager':'''package org.millenaire.common.entity;public class MillVillager extends net.minecraft.world.entity.monster.Zombie {
 public org.millenaire.common.village.Building house;public org.millenaire.common.world.MillWorldData mw=new org.millenaire.common.world.MillWorldData();public java.util.Map<org.millenaire.common.item.TradeGood,Integer> merchantSells=new java.util.LinkedHashMap<>();
 public MillVillager(){super(net.minecraft.world.entity.EntityType.ZOMBIE,null);}public org.millenaire.common.village.Building getHouse(){return house;}public org.millenaire.common.culture.Culture getCulture(){return house.culture;}public long getVillagerId(){return 1;}
}''',
'MillAdvancements':'''package org.millenaire.common.advancements;public class MillAdvancements {public static final MillAdvancements MP_NEIGHBOURTRADE=new MillAdvancements();public void grant(net.minecraft.world.entity.player.Player p){}}''',
'MillLog':'''package org.millenaire.common.utilities;public class MillLog {public static void error(Object a,String b){}}''',
'LanguageUtilities':'''package org.millenaire.common.utilities;public class LanguageUtilities {public static String string(String a,String...b){return a;}}''',
'MillMenus':'''package org.millenaire.common.ui;public class MillMenus {public static net.minecraft.world.inventory.MenuType<ContainerTrade> TRADE=null;}''',
'MillCommonUtilities':'''package org.millenaire.common.utilities;import net.minecraft.world.Container;import net.minecraft.world.item.*;import net.minecraft.world.entity.player.Player;public class MillCommonUtilities {
 public static int money=500;public static int countMoney(Container c){return money;}public static void changeMoney(Container c,int delta,Player p){money+=delta;}
 public static int countChestItems(Container c,Item item,int meta){int n=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))n+=c.getItem(i).getCount();return n;}
 public static int putItemsInChest(Container c,Item item,int meta,int count){int left=count;for(int i=0;i<c.getContainerSize()&&left>0;i++){var s=c.getItem(i);if(s.isEmpty()){int n=Math.min(left,item.getMaxStackSize());c.setItem(i,new ItemStack(item,n));left-=n;}else if(s.is(item)){int n=Math.min(left,s.getMaxStackSize()-s.getCount());s.grow(n);left-=n;}}return count-left;}
}''',
'WorldUtilities':'''package org.millenaire.common.utilities;public class WorldUtilities {public static int getItemsFromChest(net.minecraft.world.Container c,net.minecraft.world.item.Item item,int meta,int count){int left=count;for(int i=0;i<c.getContainerSize()&&left>0;i++){var s=c.getItem(i);if(s.is(item)){int n=Math.min(left,s.getCount());s.shrink(n);left-=n;}}return count-left;}}'''}
for name,code in fixtures.items():(f/(name+'.java')).write_text(code,encoding='utf-8')
(r/'tools/TradeMenuCheck.java').write_text('''import org.millenaire.common.ui.*;import org.millenaire.common.village.*;import org.millenaire.common.item.*;import org.millenaire.common.utilities.*;import net.minecraft.world.item.*;import net.minecraft.world.entity.player.*;import net.minecraft.world.inventory.*;import net.minecraft.server.level.ServerLevel;import com.mojang.authlib.GameProfile;import java.util.UUID;
public class TradeMenuCheck {
 static sun.misc.Unsafe unsafe;
 static class TestPlayer extends Player {Inventory inventory;TestPlayer(){super(null,net.minecraft.core.BlockPos.ZERO,0,new GameProfile(UUID.randomUUID(),"test"));}public boolean isSpectator(){return false;}public boolean isCreative(){return false;}public Inventory getInventory(){return inventory;}}
 static class Menu extends ContainerTrade {boolean valid=true;Menu(Player p,Building b){super(1,p,b);}@Override public boolean stillValid(Player p){return valid;}}
 static void check(boolean x,String why){if(!x)throw new AssertionError(why);}
 static TestPlayer player(ServerLevel world)throws Exception{var p=(TestPlayer)unsafe.allocateInstance(TestPlayer.class);p.level=world;p.inventory=new Inventory(p);return p;}
 static Building shop(ServerLevel world,TradeGood good){var b=new Building();b.world=world;b.selling.add(good);b.buying.add(good);b.stock.put(good.item.item,100);return b;}
 public static void main(String[] args)throws Exception {
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);unsafe=(sun.misc.Unsafe)field.get(null);var world=(ServerLevel)unsafe.allocateInstance(ServerLevel.class);
  var p=player(world);var good=new TradeGood(Items.APPLE);var b=shop(world,good);var menu=new Menu(p,b);
  check(menu.slots.size()==38&&menu.nbRowSelling==1&&menu.nbRowBuying==1,"Offer and inventory layout");
  check(!menu.slots.get(0).mayPlace(new ItemStack(Items.APPLE))&&!menu.slots.get(0).mayPickup(p)&&menu.slots.get(0).remove(1).isEmpty(),"Offer icons cannot become inventory stacks");
  menu.clicked(0,0,ClickType.PICKUP,p);check(MillCommonUtilities.money==490&&b.countGoods(Items.APPLE,0)==99&&MillCommonUtilities.countChestItems(p.inventory,Items.APPLE,0)==1,"Single purchase");
  menu.clicked(0,1,ClickType.PICKUP,p);check(MillCommonUtilities.money==410&&b.countGoods(Items.APPLE,0)==91,"Right click eight");
  menu.clicked(0,0,ClickType.QUICK_MOVE,p);check(MillCommonUtilities.money==0&&b.countGoods(Items.APPLE,0)==50,"Shift capped by funds");
  menu.clicked(1,1,ClickType.PICKUP,p);check(MillCommonUtilities.money==24&&b.countGoods(Items.APPLE,0)==58,"Sell eight");
  b.mw.profile.donationActivated=true;int rep=b.reputation;menu.clicked(1,0,ClickType.PICKUP,p);check(MillCommonUtilities.money==24&&b.reputation-rep==12,"Donation gives reputation and no currency");
  int stock=b.countGoods(Items.APPLE,0);menu.clicked(0,0,ClickType.CLONE,p);menu.clicked(0,2,ClickType.PICKUP,p);menu.clicked(-999,0,ClickType.PICKUP,p);menu.clicked(999,0,ClickType.PICKUP,p);check(b.countGoods(Items.APPLE,0)==stock,"Unsupported and invalid clicks rejected");menu.valid=false;menu.clicked(0,0,ClickType.PICKUP,p);check(b.countGoods(Items.APPLE,0)==stock,"Invalid menu rejected");
  var poor=player(world);b=shop(world,good);b.stock.put(Items.APPLE,2);menu=new Menu(poor,b);MillCommonUtilities.money=1000;menu.clicked(0,0,ClickType.QUICK_MOVE,poor);check(MillCommonUtilities.money==980&&b.countGoods(Items.APPLE,0)==0,"Stock cap");
  var full=player(world);for(int i=0;i<full.inventory.getContainerSize();i++)full.inventory.setItem(i,new ItemStack(Items.STONE,64));b=shop(world,good);menu=new Menu(full,b);MillCommonUtilities.money=1000;menu.clicked(0,0,ClickType.PICKUP,full);check(MillCommonUtilities.money==1000&&b.countGoods(Items.APPLE,0)==100,"Full inventory spends nothing");
  good.sell=Integer.MAX_VALUE;var costly=player(world);b=shop(world,good);menu=new Menu(costly,b);MillCommonUtilities.money=Integer.MAX_VALUE;menu.clicked(0,0,ClickType.QUICK_MOVE,costly);check(MillCommonUtilities.money==0&&b.countGoods(Items.APPLE,0)==99,"No multiplication overflow in purchase limit");
  System.out.println("PASS: actual trade menu dispatch, offer slots, purchase/sale/donation quantities, funds/stock/capacity limits and invalid clicks. World/player allocated as fixtures; storage, money, reputation and validation services are fixtures. No live network or GUI test.");
 }
}''',encoding='utf-8')
cp=(r/'build/migration-classpath.txt').read_text().strip();o=r/'build/trade-menu-check';o.mkdir(exist_ok=True);q=lambda x:'"'+str(x).replace('\\','/')+'"'
a=['-encoding','UTF-8','-cp',cp,'-d',o,r/'src/main/java/org/millenaire/common/ui/ContainerTrade.java',r/'tools/TradeMenuCheck.java']+list(f.glob('*.java'))
(r/'build/trade-menu-javac.args').write_text('\n'.join(map(q,a)),encoding='utf-8');(r/'build/trade-menu-java.args').write_text('\n'.join(map(q,['-cp',str(o)+';'+cp,'TradeMenuCheck'])),encoding='utf-8')

from pathlib import Path
r=Path(__file__).resolve().parents[1];p=r/'tools/TradeMenuCheck.java';s=p.read_text(encoding='utf-8').replace('super(1,p,b);}@Override','super(1,p,b);}Menu(Player p,org.millenaire.common.entity.MillVillager v){super(1,p,v);}@Override')
s=s.replace('  System.out.println(','''  var trader=(org.millenaire.common.entity.MillVillager)unsafe.allocateInstance(org.millenaire.common.entity.MillVillager.class);trader.level=world;trader.mw=new org.millenaire.common.world.MillWorldData();trader.merchantSells=new java.util.LinkedHashMap<>();good.sell=2;trader.house=shop(world,good);trader.merchantSells.put(good,2);var customer=player(world);menu=new Menu(customer,trader);MillCommonUtilities.money=1000;menu.clicked(0,1,ClickType.PICKUP,customer);check(MillCommonUtilities.money==872&&trader.house.countGoods(Items.APPLE,0)==36,"Merchant right-click batch");
  var goods=net.minecraft.core.Registry.ITEM.stream().filter(item->item!=Items.AIR).limit(26).map(TradeGood::new).toList();var left=new Building();left.world=world;left.selling.addAll(goods);var right=new Building();right.world=world;for(int i=goods.size()-1;i>=0;i--)right.selling.add(goods.get(i));var lmenu=new Menu(player(world),left);var rmenu=new Menu(player(world),right);check(lmenu.nbRowSelling==2&&lmenu.nbRowBuying==0,"Exact 26-offer row count");for(int i=0;i<26;i++)check(((ContainerTrade.TradeSlot)lmenu.slots.get(i)).good==((ContainerTrade.TradeSlot)rmenu.slots.get(i)).good,"Stable ordering independent of incoming set order");
  System.out.println(''').replace('funds/stock/capacity limits and invalid clicks','funds/stock/capacity limits, merchant batches, stable offer order, row boundaries and invalid clicks')
p.write_text(s,encoding='utf-8')
