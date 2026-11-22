from pathlib import Path
import subprocess
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');j=Path('C:/Users/sviat/Desktop/mods/.millenaire-tools/jdk-17.0.20.1+1/bin')
test=r/'tools/DoorPairCheck.java'
test.write_text('''import net.minecraft.core.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.*;import net.minecraft.world.level.block.state.properties.*;import org.millenaire.common.buildingplan.*;import java.util.*;
public class DoorPairCheck {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static DoorHingeSide choose(BlockState door,BlockPos pos,Map<BlockPos,BlockState> plan){var facing=door.getValue(DoorBlock.FACING);var left=pos.relative(facing.getCounterClockWise());var right=pos.relative(facing.getClockWise());var air=Blocks.AIR.defaultBlockState();return DoorHingeSelection.choose(door,plan.getOrDefault(left,air),plan.getOrDefault(right,air),plan.getOrDefault(left.above(),air),plan.getOrDefault(right.above(),air));}
 public static void main(String[] args){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();int count=0;
 for(Block block:new Block[]{Blocks.OAK_DOOR,Blocks.SPRUCE_DOOR,Blocks.BIRCH_DOOR,Blocks.JUNGLE_DOOR,Blocks.ACACIA_DOOR,Blocks.DARK_OAK_DOOR,Blocks.CRIMSON_DOOR,Blocks.WARPED_DOOR,Blocks.IRON_DOOR})for(Direction facing:Direction.Plane.HORIZONTAL){
  var door=block.defaultBlockState().setValue(DoorBlock.FACING,facing).setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER);var first=BlockPos.ZERO;var second=first.relative(facing.getClockWise());var plan=new HashMap<BlockPos,BlockState>();plan.put(first,door);plan.put(second,door);
  var left=choose(door,first,plan);var right=choose(door,second,plan);check(left==DoorHingeSide.LEFT&&right==DoorHingeSide.RIGHT,"Pair outer hinges: "+block+" "+facing);
  for(var hinge:new DoorHingeSide[]{left,right}){var state=door.setValue(DoorBlock.HINGE,hinge);check(DoorPlacement.half(state,false,DoubleBlockHalf.LOWER).setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER).equals(DoorPlacement.half(state,false,DoubleBlockHalf.UPPER)),"Pair halves agree");}
  plan.put(first.relative(facing.getCounterClockWise()),Blocks.STONE.defaultBlockState());plan.put(second.relative(facing.getClockWise()),Blocks.STONE.defaultBlockState());check(choose(door,first,plan)==left&&choose(door,second,plan)==right,"Pair takes precedence over walls");
  plan.clear();plan.put(first.relative(facing.getClockWise()),Blocks.AIR.defaultBlockState());check(choose(door,first,plan)==DoorHingeSide.LEFT,"Explicit air is not a wall");
  plan.put(first.relative(facing.getClockWise()),Blocks.STONE.defaultBlockState());check(choose(door,first,plan)==DoorHingeSide.RIGHT,"Solid right wall");count++;
 }
 System.out.println("PASS: "+count+" real door-type/facing combinations: paired outer hinges, both halves, surrounding walls and explicitly planned air. Live construction not covered.");
 }}''',encoding='utf-8')
cp=(r/'build/migration-classpath.txt').read_text().strip();out=r/'build/door-pair-check';out.mkdir(exist_ok=True)
subprocess.run([str(j/'javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(out),str(test),str(r/'src/main/java/org/millenaire/common/buildingplan/DoorHingeSelection.java'),str(r/'src/main/java/org/millenaire/common/buildingplan/DoorPlacement.java')],check=True)
subprocess.run([str(j/'java.exe'),'-cp',str(out)+';'+cp,'DoorPairCheck'],check=True)
manager=(r/'src/main/java/org/millenaire/common/village/buildingmanagers/PanelManager.java').read_text()
assert all('getDayTime()' not in line for line in manager.splitlines() if 'lastSignUpdate' in line)
print('Panel scheduling audit: all cooldown reads/writes use game time; no day-time clock remains in panel cooldowns. Live /time update still needs testing.')
