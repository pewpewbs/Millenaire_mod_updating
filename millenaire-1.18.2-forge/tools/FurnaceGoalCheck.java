import org.millenaire.common.goal.generic.GoalGenericTendFurnace;import org.millenaire.common.block.WoodVariant;import org.millenaire.common.entity.*;import org.millenaire.common.utilities.Point;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.entity.*;import net.minecraft.world.item.*;import net.minecraft.core.BlockPos;
public class FurnaceGoalCheck {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static FurnaceBlockEntity furnace(){return new FurnaceBlockEntity(BlockPos.ZERO,Blocks.FURNACE.defaultBlockState());}
 public static void main(String[] args)throws Exception{
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();var goal=new GoalGenericTendFurnace();goal.minimumFuel=4;
  for(var wood:WoodVariant.values()){
   var v=new MillVillager();var f=furnace();v.point=new Point(f);v.destination.stocks.put(wood.log(),1);v.inventory.stocks.put(wood.log(),2);v.house.stocks.put(wood.log(),3);
   goal.performAction(v);check(f.getItem(1).is(wood.planks().asItem())&&f.getItem(1).getCount()==24,"Species and conversion "+wood);check(v.destination.countGoods(wood.log().defaultBlockState())+v.inventory.countGoods(wood.log().defaultBlockState())+v.house.countGoods(wood.log().defaultBlockState())==0,"Consumed every source");
  }
  var v=new MillVillager();var f=furnace();v.point=new Point(f);v.house=v.destination;v.destination.stocks.put(Blocks.OAK_LOG,4);v.destination.resources.furnaces.add(v.point);check(goal.getDestination(v)!=null,"Minimum fuel applies at exact threshold");goal.performAction(v);check(f.getItem(1).getCount()==16,"Shared house not double-counted");
  f.setItem(1,new ItemStack(Blocks.OAK_PLANKS,61));v.destination.stocks.put(Blocks.OAK_LOG,10);goal.performAction(v);check(f.getItem(1).getCount()==61&&v.destination.stocks.get(Blocks.OAK_LOG)==10,"No fractional/free planks when fewer than four fit");
  f.setItem(1,new ItemStack(Blocks.OAK_PLANKS,60));f.getItem(1).getOrCreateTag().putString("marker","keep");goal.performAction(v);check(f.getItem(1).getCount()==64&&v.destination.stocks.get(Blocks.OAK_LOG)==9&&f.getItem(1).getTag().getString("marker").equals("keep"),"Bounded top-up preserves NBT");
  f.setItem(1,new ItemStack(Items.COAL,2));goal.performAction(v);check(f.getItem(1).is(Items.COAL)&&v.destination.stocks.get(Blocks.OAK_LOG)==9,"Other fuel untouched");
  v=new MillVillager();f=furnace();v.point=new Point(f);v.destination.reportedExtra=100;goal.performAction(v);check(f.getItem(1).isEmpty(),"Stale availability never creates fuel");
  var firepit=new TileEntityFirePit();v.point=new Point(firepit);v.destination.reportedExtra=0;v.destination.stocks.put(Blocks.BIRCH_LOG,2);firepit.fuel.setStackInSlot(0,new ItemStack(Blocks.BIRCH_PLANKS,52));goal.performAction(v);check(firepit.fuel.getStackInSlot(0).getCount()==60&&v.destination.stocks.get(Blocks.BIRCH_LOG)==0,"Firepit top-up uses actual consumed logs and valid slot zero");
  v.point=null;check(goal.performAction(v)&&goal.getHeldItemsTravelling(v)==null,"Missing destination guard");
  System.out.println("PASS: actual furnace goal with vanilla furnace, six wood species, bounded conversion, storage depletion, shared house, fuel threshold, stale stocks, NBT, other fuel and firepit slot zero. Villager/building storage and firepit are fixtures; live AI untested.");
 }
}