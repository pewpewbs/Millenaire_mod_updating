from pathlib import Path
import subprocess
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');j=Path('C:/Users/sviat/Desktop/mods/.millenaire-tools/jdk-17.0.20.1+1/bin')
o=Path('work/audit19-check').resolve();o.mkdir(exist_ok=True)
s=o/'Audit19Check.java'
s.write_text(r'''import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.items.ItemStackHandler;
import org.millenaire.common.entity.*;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.village.Building;
import org.millenaire.common.goal.generic.GoalGenericCooking;
import org.millenaire.common.utilities.*;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.forge.*;
class Audit19Check {
 static sun.misc.Unsafe unsafe;
 static <T>T make(Class<T> c)throws Exception{return c.cast(unsafe.allocateInstance(c));}
 static void ok(boolean b,String m){if(!b)throw new AssertionError(m);}
 static class Resident extends MillVillager.EntityGenericMale {
  int stock;
  Resident(){super(null,null);}
  public int countInv(Item i,int meta){return stock;}
  public int countInv(InvItem i){return stock;}
  public void addToInv(Item i,int meta,int n){stock+=n;}
  public int takeFromInv(InvItem i,int n){int taken=Math.min(n,stock);stock-=taken;return taken;}
 }
 static class Drop extends ItemEntity {
  ItemStack stack;boolean removed;
  Drop(){super(null,null);}
  public ItemStack getItem(){return removed?ItemStack.EMPTY:stack;}
  public void remove(Entity.RemovalReason r){removed=true;}
 }
 static class Store extends Building {
  int stock,limit,accepted,reported=-1;
  Store(){super(null,null);}
  public int countGoods(InvItem i){return reported>=0?reported:stock;}
  public int takeGoods(InvItem i,int n){int k=Math.min(n,stock);stock-=k;return k;}
  public int storeGoods(Item i,int meta,int n){int k=Math.min(n,limit);limit-=k;accepted+=k;return k;}
 }
 public static void main(String[] args)throws Exception{
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
  var f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);unsafe=(sun.misc.Unsafe)f.get(null);
  var pit=make(TileEntityFirePit.class);var out=new ItemStackHandler(3);
  var outputs=TileEntityFirePit.class.getDeclaredField("outputs");outputs.setAccessible(true);outputs.set(pit,out);
  for(int request:new int[]{0,1,5,8,20,-1}){
   out.setStackInSlot(0,new ItemStack(Items.BREAD,3));out.setStackInSlot(1,new ItemStack(Items.BREAD,5));
   int got=WorldUtilities.getItemsFromFirePit(pit,Items.BREAD,request);
   ok(got==Math.min(8,Math.max(0,request)),"firepit requested count");
   ok(got+out.getStackInSlot(0).getCount()+out.getStackInSlot(1).getCount()==8,"firepit conservation");
  }
  var villages=new MillCommonUtilities.VillageList();
  for(int i=0;i<3;i++){villages.addVillage(new Point(i,64,0),"v"+i,"t","c","p");villages.buildingsTime.get(i).add((long)i);villages.villagersTime.get(i).add((long)i+10);}
  villages.removeVillage(new Point(1,64,0));
  ok(villages.pos.size()==2&&villages.buildingsTime.size()==2&&villages.villagersTime.size()==2,"parallel sizes");
  ok(villages.names.get(1).equals("v2")&&villages.buildingsTime.get(1).get(0)==2L&&villages.villagersTime.get(1).get(0)==12L&&villages.rankByPos.get(new Point(2,64,0))==1,"parallel identity");
  var registry=VillagerConfig.villagerConfigs;VillagerConfig.DEFAULT_CONFIG=new VillagerConfig("default");
  var copy=VillagerConfig.class.getDeclaredMethod("copyDefault",String.class);copy.setAccessible(true);
  for(int i=0;i<10;i++){var c=(VillagerConfig)copy.invoke(null,"c"+i);registry.put(c.key,c);ok(c.toolsAxe!=VillagerConfig.DEFAULT_CONFIG.toolsAxe,"independent map");}
  ok(VillagerConfig.villagerConfigs==registry&&registry.size()==10,"registry identity/content");
  ok(VillagerConfig.DEFAULT_CONFIG.getBestItemByCategoryName("absent",null)==null,"missing category");
  for(int count:new int[]{1,16,64}){var v=make(Resident.class);var d=make(Drop.class);d.stack=new ItemStack(Items.WHEAT,count);ok(v.pickUpGoods(d)&&v.stock==count&&d.removed,"whole drop");ok(!v.pickUpGoods(d)&&v.stock==count,"no repeat");}
  var v=make(Resident.class);var d=make(Drop.class);d.stack=new ItemStack(Items.WHEAT,16);d.stack.getOrCreateTag().putString("custom","keep");
  ok(!v.pickUpGoods(d)&&!d.removed&&v.stock==0,"tagged drop preserved");
  d.stack=new ItemStack(Items.WHEAT,16);v.stock=Integer.MAX_VALUE;ok(!v.pickUpGoods(d)&&!d.removed,"overflow guard");
  Mill.proxy=new CommonProxy();MillConfigValues.mainLanguage=new LanguageData("uk",false);
  MillConfigValues.mainLanguage.strings.put("fixture","Hello <0>");String literal="$5\\path=value";
  ok(LanguageUtilities.string("fixture",literal).equals("Hello "+literal),"literal substitution");
  ok(LanguageUtilities.string(new String[]{"fixture",literal}).equals("Hello "+literal),"array substitution");
  ((net.minecraftforge.registries.ForgeRegistry)net.minecraftforge.registries.ForgeRegistries.BLOCKS).unfreeze();
  var unfreeze=net.minecraft.core.Registry.BLOCK.getClass().getMethod("unfreeze");unfreeze.setAccessible(true);unfreeze.invoke(net.minecraft.core.Registry.BLOCK);
  org.millenaire.common.block.MillBlocks.EARTH_DECORATION=new org.millenaire.common.block.BlockDecorativeEarth("fixture_earth");
  var road=new org.millenaire.common.block.BlockPath("fixture_road",net.minecraft.world.level.material.MaterialColor.DIRT,net.minecraft.world.level.block.SoundType.GRAVEL);
  var slab=new org.millenaire.common.block.BlockPathSlab("fixture_slab",net.minecraft.world.level.material.MaterialColor.DIRT,net.minecraft.world.level.block.SoundType.GRAVEL);
  for(var field:org.millenaire.common.block.MillBlocks.class.getFields()){
   if(field.getType()==org.millenaire.common.block.BlockPath.class)field.set(null,road);
   if(field.getType()==org.millenaire.common.block.BlockPathSlab.class)field.set(null,slab);
  }
  var goal=new GoalGenericCooking();goal.itemToCook=InvItem.createInvItem(Items.BEEF,0);goal.minimumToCook=1;
  var fill=GoalGenericCooking.class.getDeclaredMethod("fillCookingInput",Building.class,MillVillager.class,ItemStack.class,int.class);fill.setAccessible(true);
  var store=GoalGenericCooking.class.getDeclaredMethod("storeCookingOutput",Building.class,ItemStack.class);store.setAccessible(true);
  for(int[] amounts:new int[][]{{0,10},{10,0},{3,7},{0,0},{64,64}}){
   var b=make(Store.class);b.stock=amounts[0];b.reported=-1;v=make(Resident.class);v.stock=amounts[1];
   var result=(ItemStack)fill.invoke(goal,b,v,ItemStack.EMPTY,64);
   ok(result.getCount()+b.stock+v.stock==amounts[0]+amounts[1],"cooking conservation");
   ok(result.getCount()==Math.min(64,amounts[0]+amounts[1]),"cooking real source amounts");
  }
  var b=make(Store.class);b.stock=2;b.reported=20;v=make(Resident.class);
  ok(((ItemStack)fill.invoke(goal,b,v,ItemStack.EMPTY,64)).getCount()==2,"stale count cannot create items");
  for(int limit:new int[]{0,3,8,64}){b=make(Store.class);b.limit=limit;var result=(ItemStack)store.invoke(goal,b,new ItemStack(Items.COOKED_BEEF,8));ok(result.getCount()+b.accepted==8&&b.accepted==Math.min(8,limit),"output remainder");}
  for(int size:new int[]{0,1,100,10000}){
   var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
   try{
    var booleans=new ArrayList<Boolean>();var points=new ArrayList<Point>();var strings=new ArrayList<String>();var stacks=new ArrayList<ItemStack>();
    for(int i=0;i<size;i++){booleans.add(i%2==0);points.add(i%3==0?null:new Point(i,-20,i));strings.add(i%3==0?null:"тест="+i);stacks.add(new ItemStack(Items.WHEAT,i%64+1));}
    org.millenaire.common.network.StreamReadWrite.writeBooleanList(booleans,buffer);
    org.millenaire.common.network.StreamReadWrite.writePointList(points,buffer);
    org.millenaire.common.network.StreamReadWrite.writeStringList(strings,buffer);
    org.millenaire.common.network.StreamReadWrite.writeItemStackList(stacks,buffer);
    ok(org.millenaire.common.network.StreamReadWrite.readBooleanList(buffer).equals(booleans),"boolean roundtrip");
    ok(org.millenaire.common.network.StreamReadWrite.readPointList(buffer).equals(points),"point roundtrip");
    ok(org.millenaire.common.network.StreamReadWrite.readStringList(buffer).equals(strings),"string roundtrip");
    var decoded=org.millenaire.common.network.StreamReadWrite.readItemStackList(buffer);ok(decoded.size()==size,"stack list length");
    for(int i=0;i<size;i++)ok(ItemStack.matches(decoded.get(i),stacks.get(i)),"stack roundtrip");
    ok(buffer.readableBytes()==0,"no leftover packet data");
   }finally{buffer.release();}
  }
  System.out.println("PASS firepit, village indices, config registry, item pickup, literal strings, cooking conservation, four network lists 0/1/100/10000 roundtrip");
 }
}''',encoding='utf-8')
cp=str(r/'build/classes/java/main')+';'+(r/'build/migration-classpath.txt').read_text().strip()
subprocess.run([str(j/'javac.exe'),'-cp',cp,'-d',str(o),str(s)],check=True)
subprocess.run([str(j/'java.exe'),'-cp',str(o)+';'+cp,'Audit19Check'],check=True)
