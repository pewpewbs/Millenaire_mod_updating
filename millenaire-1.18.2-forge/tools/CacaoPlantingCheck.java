import net.minecraft.core.*;import net.minecraft.world.level.Level;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.tags.BlockTags;import net.minecraft.world.InteractionHand;import org.millenaire.common.utilities.Point;import org.millenaire.common.entity.MillVillager;import java.util.*;
public class CacaoPlantingCheck{
    private BlockState getCocoaState(Level world, Point p) {
        BlockState result = null;
        // Keep the original tie-breaking order, but only use supports valid for cocoa.
        for (Direction direction : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            BlockState support = p.getRelative(direction.getStepX(), 0, direction.getStepZ()).getBlockActualState(world);
            if (support.is(BlockTags.JUNGLE_LOGS))
                result = Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.FACING, direction).setValue(CocoaBlock.AGE, 0);
        }
        return result;
    }

    public boolean performAction(MillVillager villager) {
        Point cropPoint = villager.getGoalDestPoint();
        if (cropPoint == null || !cropPoint.getBlockActualState(villager.level).isAir()) return true;
        BlockState state = this.getCocoaState(villager.level, cropPoint);
        if (state != null && villager.setBlockstate(cropPoint, state))
            villager.swing(InteractionHand.MAIN_HAND);
        return true;
    }


 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();List<Holder<Block>> logs=new ArrayList<>();for(Block b:new Block[]{Blocks.JUNGLE_LOG,Blocks.JUNGLE_WOOD,Blocks.STRIPPED_JUNGLE_LOG,Blocks.STRIPPED_JUNGLE_WOOD})logs.add(Registry.BLOCK.getHolderOrThrow(Registry.BLOCK.getResourceKey(b).orElseThrow()));Registry.BLOCK.bindTags(Map.of(BlockTags.JUNGLE_LOGS,logs));var goal=new CacaoPlantingCheck();var v=new MillVillager();v.dest=new Point(-8,-30,4);
  for(var log:logs)for(var facing:Direction.Plane.HORIZONTAL){Point.cells.clear();v.swings=0;Point.cells.put(v.dest.pos.relative(facing),log.value().defaultBlockState());goal.performAction(v);var state=Point.cells.get(v.dest.pos);check(state.is(Blocks.COCOA)&&state.getValue(CocoaBlock.FACING)==facing&&state.getValue(CocoaBlock.AGE)==0&&v.swings==1,"Cocoa support direction "+facing);goal.performAction(v);check(v.swings==1,"Existing cocoa retained");}
  Point.cells.clear();v.swings=0;Point.cells.put(v.dest.pos.north(),Blocks.OAK_LOG.defaultBlockState());goal.performAction(v);check(v.swings==0&&!Point.cells.containsKey(v.dest.pos),"Non-jungle support rejected");
  Point.cells.clear();for(var d:Direction.Plane.HORIZONTAL)Point.cells.put(v.dest.pos.relative(d),Blocks.JUNGLE_LOG.defaultBlockState());goal.performAction(v);check(Point.cells.get(v.dest.pos).getValue(CocoaBlock.FACING)==Direction.EAST,"Original tie order retained");
  Point.cells.remove(v.dest.pos);v.reject=true;v.swings=0;goal.performAction(v);check(v.swings==0&&!Point.cells.containsKey(v.dest.pos),"Failed write does not report planting");v.dest=null;check(goal.performAction(v),"Missing destination safe");
  System.out.println("PASS: extracted production cocoa state/action methods: four directions and four jungle support types, age zero, existing crop protection, wrong support rejection, original tie order, rejected write and null destination. Tags/world/villager are fixtures; in-game cocoa growth untested.");
 }
}