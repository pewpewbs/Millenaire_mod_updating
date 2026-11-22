from pathlib import Path
import subprocess
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');j=Path('C:/Users/sviat/Desktop/mods/.millenaire-tools/jdk-17.0.20.1+1/bin');o=Path('work/transactions26-check').resolve();o.mkdir(exist_ok=True)
s=o/'Transactions26Check.java';s.write_text(r'''import java.util.*;import net.minecraft.world.item.*;import net.minecraft.world.SimpleContainer;import net.minecraftforge.items.ItemStackHandler;
import org.millenaire.common.entity.*;import org.millenaire.common.item.InvItem;import org.millenaire.common.village.Building;import org.millenaire.common.utilities.GoodsTransaction;import org.millenaire.common.goal.generic.GoalGenericCrafting;
class Transactions26Check {
 static sun.misc.Unsafe u;static <T>T make(Class<T> c)throws Exception{return c.cast(u.allocateInstance(c));}
 static void ok(boolean b,String m){if(!b)throw new AssertionError(m);}
 static class Resident extends MillVillager.EntityGenericMale {
  Store home;Resident(){super(null,null);}
  public Building getGoalBuildingDest(){return home;}
  public int countInv(InvItem i){return inventory.getOrDefault(i,0);}
  public int takeFromInv(InvItem i,int n){int k=Math.min(n,countInv(i));inventory.put(i,countInv(i)-k);return k;}
  public void restoreGoodsInventory(Map<InvItem,Integer> snapshot){inventory.clear();inventory.putAll(snapshot);}
 }
 static class Store extends Building {
  SimpleContainer chest;ItemStackHandler pit;boolean stale,failOutput,throwOutput;int outputCalls;
  Store(){super(null,null);}
  public int countGoods(InvItem i){if(stale)return 100;int n=0;for(int s=0;s<chest.getContainerSize();s++)if(chest.getItem(s).is(i.getItem()))n+=chest.getItem(s).getCount();for(int s=0;s<pit.getSlots();s++)if(pit.getStackInSlot(s).is(i.getItem()))n+=pit.getStackInSlot(s).getCount();return n;}
  public int takeGoods(InvItem i,int n){int k=0;for(int s=0;s<chest.getContainerSize()&&k<n;s++)if(chest.getItem(s).is(i.getItem()))k+=chest.removeItem(s,n-k).getCount();for(int s=0;s<pit.getSlots()&&k<n;s++)if(pit.getStackInSlot(s).is(i.getItem()))k+=pit.extractItem(s,n-k,false).getCount();return k;}
  public int storeGoods(InvItem i,int n){outputCalls++;if(throwOutput)throw new IllegalStateException("injected output failure");int accepted=failOutput?Math.max(0,n-1):n;return org.millenaire.common.utilities.MillCommonUtilities.putItemsInChest(chest,i.getItem(),i.meta,accepted);}
  public GoodsTransaction beginGoodsTransaction(MillVillager v){return new GoodsTransaction(List.of(chest),List.of(pit),v,()->{});}
 }
 public static void main(String[]args)throws Exception{
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();var f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);u=(sun.misc.Unsafe)f.get(null);
  org.millenaire.common.utilities.MillLog.console=false;
  ((net.minecraftforge.registries.ForgeRegistry)net.minecraftforge.registries.ForgeRegistries.BLOCKS).unfreeze();var unfreeze=net.minecraft.core.Registry.BLOCK.getClass().getMethod("unfreeze");unfreeze.setAccessible(true);unfreeze.invoke(net.minecraft.core.Registry.BLOCK);
  org.millenaire.common.block.MillBlocks.EARTH_DECORATION=new org.millenaire.common.block.BlockDecorativeEarth("fixture_earth");
  var road=new org.millenaire.common.block.BlockPath("fixture_road",net.minecraft.world.level.material.MaterialColor.DIRT,net.minecraft.world.level.block.SoundType.GRAVEL);var slab=new org.millenaire.common.block.BlockPathSlab("fixture_slab",net.minecraft.world.level.material.MaterialColor.DIRT,net.minecraft.world.level.block.SoundType.GRAVEL);
  for(var field:org.millenaire.common.block.MillBlocks.class.getFields()){if(field.getType()==org.millenaire.common.block.BlockPath.class)field.set(null,road);if(field.getType()==org.millenaire.common.block.BlockPathSlab.class)field.set(null,slab);}
  var wheat=InvItem.createInvItem(Items.WHEAT,0);var egg=InvItem.createInvItem(Items.EGG,0);var bread=InvItem.createInvItem(Items.BREAD,0);
  for(int scenario=0;scenario<5;scenario++){
   var b=make(Store.class);b.chest=new SimpleContainer(3);b.pit=new ItemStackHandler(3);b.stale=scenario==1;b.failOutput=scenario==2;b.throwOutput=scenario==3;
   var v=make(Resident.class);v.home=b;v.inventory=new HashMap<>();v.inventory.put(wheat,1);v.level=make(net.minecraft.server.level.ServerLevel.class);
   b.chest.setItem(0,new ItemStack(Items.WHEAT,2));b.pit.setStackInSlot(0,new ItemStack(Items.EGG,scenario==1?1:2));
   b.pit.getStackInSlot(0).getOrCreateTag().putString("marker","preserve-exactly");
   if(scenario==4){b.chest.setItem(1,new ItemStack(Items.STONE,64));b.chest.setItem(2,new ItemStack(Items.STONE,64));}
   var goal=new GoalGenericCrafting();goal.input.put(wheat,3);goal.input.put(egg,2);goal.output.put(bread,scenario==4?128:2);
   var before=new ArrayList<ItemStack>();for(int i=0;i<3;i++)before.add(b.chest.getItem(i).copy());var beforeEgg=b.pit.getStackInSlot(0).copy();var beforeInv=new HashMap<>(v.inventory);
   try{goal.performAction(v);ok(scenario!=3,"exception expected");}catch(IllegalStateException expected){ok(scenario==3,"only injected exception");}
   if(scenario==0){ok(v.countInv(wheat)==0&&b.countGoods(egg)==0&&b.countGoods(wheat)==0&&b.countGoods(bread)==2,"exact successful recipe");}
   else{for(int i=0;i<3;i++)ok(ItemStack.matches(before.get(i),b.chest.getItem(i)),"slot rollback scenario "+scenario+" slot "+i);ok(ItemStack.matches(beforeEgg,b.pit.getStackInSlot(0)),"output handler/tag rollback");ok(beforeInv.equals(v.inventory),"resident rollback");}
  }
  var invalid=new GoalGenericCrafting();invalid.output.put(bread,1);invalid.input.put(wheat,-1);ok(!invalid.validateGoal(),"negative input rejected");invalid.input.put(wheat,1);invalid.output.put(bread,0);ok(!invalid.validateGoal(),"zero output rejected");

  ((net.minecraftforge.registries.ForgeRegistry)net.minecraftforge.registries.ForgeRegistries.ITEMS).unfreeze();
  var iu=net.minecraft.core.Registry.ITEM.getClass().getMethod("unfreeze");iu.setAccessible(true);iu.invoke(net.minecraft.core.Registry.ITEM);
  org.millenaire.common.item.MillItems.DENIER=new org.millenaire.common.item.ItemMill("fixture_copper");
  org.millenaire.common.item.MillItems.DENIER_ARGENT=new org.millenaire.common.item.ItemMill("fixture_silver");
  org.millenaire.common.item.MillItems.DENIER_OR=new org.millenaire.common.item.ItemMill("fixture_gold");
  org.millenaire.common.item.MillItems.PURSE=new org.millenaire.common.item.ItemPurse("fixture_purse");
  for(var item:List.of(org.millenaire.common.item.MillItems.DENIER,org.millenaire.common.item.MillItems.DENIER_ARGENT,org.millenaire.common.item.MillItems.DENIER_OR,org.millenaire.common.item.MillItems.PURSE))net.minecraftforge.registries.ForgeRegistries.ITEMS.register(item);
  var copper=org.millenaire.common.item.MillItems.DENIER;var gold=org.millenaire.common.item.MillItems.DENIER_OR;
  var wallet=new SimpleContainer(4);wallet.setItem(0,new ItemStack(copper,64));
  ok(org.millenaire.common.utilities.MoneyTransactions.change(wallet,-1)&&org.millenaire.common.utilities.MoneyTransactions.count(wallet)==63,"exact debit");
  var beforeMoney=wallet.getItem(0).copy();ok(!org.millenaire.common.utilities.MoneyTransactions.change(wallet,-64)&&ItemStack.matches(beforeMoney,wallet.getItem(0)),"insufficient balance unchanged");
  var tight=new SimpleContainer(1);tight.setItem(0,new ItemStack(gold,1));ok(!org.millenaire.common.utilities.MoneyTransactions.change(tight,-1)&&tight.getItem(0).getItem()==gold,"no room for change unchanged");
  var purse=new ItemStack(org.millenaire.common.item.MillItems.PURSE);purse.getOrCreateTag().putString("custom","keep");wallet.clearContent();wallet.setItem(0,purse);wallet.setItem(1,new ItemStack(gold,1));
  ok(org.millenaire.common.utilities.MoneyTransactions.change(wallet,-1)&&org.millenaire.common.utilities.MoneyTransactions.count(wallet)==4095,"purse and loose coins debit");
  ok(wallet.getItem(0).getTag().getString("custom").equals("keep"),"purse custom NBT retained");
  wallet.getItem(0).getTag().putInt("ml_Purse_DENIER",-1);ok(!org.millenaire.common.utilities.MoneyTransactions.change(wallet,100),"invalid purse rejected");
  wallet.clearContent();wallet.setItem(0,new ItemStack(org.millenaire.common.item.MillItems.PURSE));wallet.setItem(1,new ItemStack(org.millenaire.common.item.MillItems.PURSE));
  ok(org.millenaire.common.utilities.MoneyTransactions.change(wallet,(long)Integer.MAX_VALUE+123)&&org.millenaire.common.utilities.MoneyTransactions.count(wallet)==(long)Integer.MAX_VALUE+123,"multiple purses long balance");
  ok(org.millenaire.common.utilities.MillCommonUtilities.countMoney(wallet)==Integer.MAX_VALUE,"legacy displayed count saturates");
  var store=make(Store.class);store.world=make(net.minecraft.server.level.ServerLevel.class);store.chest=new SimpleContainer(3);store.pit=new ItemStackHandler(3);store.chest.setItem(0,new ItemStack(Items.WHEAT,5));
  wallet.clearContent();wallet.setItem(0,new ItemStack(copper,64));
  int traded=org.millenaire.common.utilities.TradeTransactions.trade(store,wallet,wheat,3,2,true,false,false);
  ok(traded==3&&store.countGoods(wheat)==2&&org.millenaire.common.utilities.MoneyTransactions.count(wallet)==58,"buy exact goods/payment");
  traded=org.millenaire.common.utilities.TradeTransactions.trade(store,wallet,wheat,3,1,false,false,false);
  ok(traded==3&&store.countGoods(wheat)==5&&org.millenaire.common.utilities.MoneyTransactions.count(wallet)==61,"sell exact goods/payment");
  tight.setItem(0,new ItemStack(gold,1));int beforeStock=store.countGoods(wheat);
  ok(org.millenaire.common.utilities.TradeTransactions.trade(store,tight,wheat,1,1,true,false,false)==0&&store.countGoods(wheat)==beforeStock&&tight.getItem(0).is(gold),"failed buy restores both sides");
  wallet.clearContent();wallet.setItem(0,new ItemStack(Items.WHEAT,2));
  for(int i=0;i<3;i++)store.chest.setItem(i,new ItemStack(Items.STONE,64));
  ok(org.millenaire.common.utilities.TradeTransactions.trade(store,wallet,wheat,2,1,false,false,false)==0&&wallet.getItem(0).getCount()==2,"full store restores seller goods");
  store.chest.clearContent();store.throwOutput=true;
  try{org.millenaire.common.utilities.TradeTransactions.trade(store,wallet,wheat,2,1,false,false,false);throw new AssertionError("missing injected failure");}catch(IllegalStateException expected){}
  ok(wallet.getItem(0).getCount()==2&&store.countGoods(wheat)==0,"trade exception rollback");
  System.out.println("PASS money debit/credit/insufficient/capacity/purses/long balances, trade buy/sell/full storage/exception rollback");
  System.out.println("PASS crafting exact success, partial ingredient withdrawal, partial output, full storage, exception rollback, exact slots/NBT/firepit/resident preservation, invalid recipe counts");
 }
}''',encoding='utf-8')
cp=str(r/'build/classes/java/main')+';'+(r/'build/migration-classpath.txt').read_text().strip()
subprocess.run([str(j/'javac.exe'),'-cp',cp,'-d',str(o),str(s)],check=True)
raise SystemExit(subprocess.run([str(j/'java.exe'),'-cp',str(o)+';'+cp,'Transactions26Check'],cwd=o).returncode)

