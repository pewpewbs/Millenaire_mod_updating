import org.millenaire.common.goal.GoalLumbermanChopTrees;import org.millenaire.common.entity.MillVillager;import org.millenaire.common.utilities.*;import org.millenaire.common.block.WoodVariant;import net.minecraft.world.level.block.*;import net.minecraft.core.*;import java.util.*;
public class LumberGoalCheck {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception {
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);var unsafe=(sun.misc.Unsafe)field.get(null);var v=(MillVillager)unsafe.allocateInstance(MillVillager.class);v.stocks=new HashMap<>();v.destination=new Point(0,-40,0);var goal=new GoalLumbermanChopTrees();
  for(var wood:WoodVariant.values()){
   Point.world.clear();v.stocks.clear();var p=v.destination.pos;Point.world.put(p,wood.log().defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X));check(goal.performAction(v),"Single log completes");check(Point.world.get(p).isAir()&&v.stocks.get(wood.log())==1,"Log species retained "+wood);
   Point.world.clear();v.stocks.clear();Point.world.put(p,wood.leaves().defaultBlockState());goal.performAction(v);check(Point.world.get(p).isAir()&&v.stocks.get(wood.sapling())==1,"Correct sapling "+wood);check(MillCommonUtilities.lastBound==(wood==WoodVariant.DARK_OAK?2:4),"Original drop chance "+wood);
   Point.world.clear();v.stocks.clear();var bark=Registry.BLOCK.get(new net.minecraft.resources.ResourceLocation(wood.getSerializedName()+"_wood"));Point.world.put(p,bark.defaultBlockState());goal.performAction(v);check(v.stocks.get(wood.log())==1,"Bark harvest normalizes to species log");
  }
  Point.world.clear();v.stocks.clear();var p=v.destination.pos;Point.world.put(p,Blocks.OAK_LOG.defaultBlockState());Point.world.put(p.above(),Blocks.SPRUCE_LOG.defaultBlockState());check(!goal.performAction(v)&&v.stocks.values().stream().mapToInt(Integer::intValue).sum()==1,"One log per action, more work remains");check(goal.performAction(v)&&v.stocks.values().stream().mapToInt(Integer::intValue).sum()==2,"Next action finishes");
  Point.world.clear();v.stocks.clear();Point.world.put(p,Blocks.OAK_PLANKS.defaultBlockState());Point.world.put(p.above(),Blocks.OAK_FENCE.defaultBlockState());check(goal.performAction(v)&&v.stocks.isEmpty()&&Point.world.get(p).is(Blocks.OAK_PLANKS)&&Point.world.get(p.above()).is(Blocks.OAK_FENCE),"Wooden buildings are not trees");
  var plant=new org.millenaire.common.goal.GoalLumbermanPlantSaplings();v.building=new org.millenaire.common.village.Building();String[] markers={"oakspawn","pinespawn","birchspawn","junglespawn","acaciaspawn","darkoakspawn"};
  for(var wood:WoodVariant.values()){
   Point.world.clear();v.stocks.clear();v.building.resources.type=markers[wood.ordinal()];v.stocks.put(wood.sapling(),1);
   check(plant.getHeldItemsTravelling(v)[0].is(wood.sapling().asItem()),"Held sapling type "+wood);plant.performAction(v);check(Point.world.get(p).is(wood.sapling())&&v.stocks.get(wood.sapling())==0,"Planted matching species and consumed sapling "+wood);
  }
  Point.world.put(p,Blocks.STONE.defaultBlockState());plant.performAction(v);check(Point.world.get(p).is(Blocks.STONE),"Solid block preserved");Point.world.put(p,Blocks.BIRCH_SAPLING.defaultBlockState());plant.performAction(v);check(Point.world.get(p).is(Blocks.BIRCH_SAPLING),"Existing sapling preserved");
  v.destination=null;check(plant.getHeldItemsTravelling(v)==null&&plant.performAction(v),"Planting missing destination guard");check(goal.performAction(v),"Missing destination guard");
  System.out.println("PASS: actual lumber goal harvest with fixtures: six log/sapling species, original drop bounds, bark normalization, negative world height, one-log batching, repeated actions, construction blocks excluded and missing destination; grove planting/held items for all six species, consumption, solid/existing-sapling protection. World/inventory/AI services are fixtures; live grove selection and tree cutting remain untested.");
 }
}