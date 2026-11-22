package net.minecraftforge.registries;import org.millenaire.common.goal.GoalByzantineGatherSnails;import org.millenaire.common.entity.MillVillager;import org.millenaire.common.utilities.Point;import org.millenaire.common.village.Building;import org.millenaire.common.block.*;import net.minecraft.world.item.*;import net.minecraft.world.level.block.Blocks;
public class SnailGoalCheck {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();((net.minecraftforge.registries.ForgeRegistry<?>)net.minecraftforge.registries.ForgeRegistries.BLOCKS).unfreeze();((net.minecraftforge.registries.NamespacedDefaultedWrapper<?>)net.minecraft.core.Registry.BLOCK).unfreeze();
  var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);var unsafe=(sun.misc.Unsafe)field.get(null);var v=(MillVillager)unsafe.allocateInstance(MillVillager.class);v.destination=new Point();v.hall=new Building();v.lastGoalTime=new java.util.HashMap<>();var goal=new GoalByzantineGatherSnails();var soil=MillBlocks.SNAIL_SOIL;
  check(goal.icon.item()==Items.PURPLE_DYE&&goal.icon.meta()==0&&goal.buildingLimit.get(goal.icon)==128&&goal.townhallLimit.get(goal.icon)==128,"Flattened dye and original limits");
  for(var stage:BlockSnailSoil.EnumType.values()){
   v.dyes=0;v.swings=0;v.destination.state=soil.defaultBlockState().setValue(BlockSnailSoil.PROGRESS,stage);boolean done=goal.performAction(v);boolean full=stage==BlockSnailSoil.EnumType.SNAIL_SOIL_FULL;
   check(v.dyes==(full?1:0)&&v.swings==(full?1:0)&&done!=full,"Only mature soil harvested: "+stage);check(v.destination.state.getValue(BlockSnailSoil.PROGRESS)==(full?BlockSnailSoil.EnumType.SNAIL_SOIL_EMPTY:stage),"Growth stage preserved/reset");
   if(full){check(goal.performAction(v)&&v.dyes==1,"Repeated action cannot duplicate dye");}
  }
  v.dyes=0;v.rejectWrite=true;v.destination.state=soil.defaultBlockState().setValue(BlockSnailSoil.PROGRESS,BlockSnailSoil.EnumType.SNAIL_SOIL_FULL);goal.performAction(v);check(v.dyes==0&&v.destination.state.getValue(BlockSnailSoil.PROGRESS)==BlockSnailSoil.EnumType.SNAIL_SOIL_FULL,"Rejected world write cannot create dye");v.rejectWrite=false;
  v.destination.state=Blocks.DIRT.defaultBlockState();check(goal.performAction(v)&&v.dyes==0,"Unrelated blocks unchanged");v.destination=null;check(goal.performAction(v),"Missing destination handled");
  var held=goal.getHeldItemsOffHandDestination(v);check(held[0].is(Items.PURPLE_DYE)&&held[0].getCount()==1,"Display single purple dye");held[0].shrink(1);check(goal.getHeldItemsOffHandDestination(v)[0].getCount()==1,"Held stacks not shared");
  check(goal.getDestination(v)==null,"No harvestable farms");for(int distance:new int[]{20,3,10}){var farm=new Building();farm.resources.harvest=new Point();farm.resources.harvest.distance=distance;v.hall.farms.add(farm);}check(goal.getDestination(v).destination.distance==3,"Nearest farm selected");
  v.hall.dyeStock=12;check(goal.priority(v)==76,"Dye stock lowers priority");v.goalKey=goal.key;v.hall.villagers.add(v);check(goal.priority(v)==38,"Other goal assignments lower priority");
  System.out.println("PASS: actual snail goal and soil block; four maturity stages, one dye per harvest, no duplicate/failed-write rewards, null/foreign blocks, independent held stacks, nearest farm, limits and stock priorities. World, villager inventory and building services are fixtures; live AI untested.");
 }
}