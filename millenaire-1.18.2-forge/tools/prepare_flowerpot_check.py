from pathlib import Path
r=Path(__file__).resolve().parents[1]
(r/'tools/FlowerPotCheck.java').write_text('''import org.millenaire.common.utilities.LegacyFlowerPots;
import net.minecraft.core.Registry;
import net.minecraft.world.level.block.*;
import java.nio.file.*;
public class FlowerPotCheck {
 public static void main(String[] args)throws Exception {
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
  String[] plants="poppy blue_orchid allium azure_bluet red_tulip orange_tulip white_tulip pink_tulip oxeye_daisy dandelion oak_sapling spruce_sapling birch_sapling jungle_sapling acacia_sapling dark_oak_sapling red_mushroom brown_mushroom dead_bush fern cactus".split(" ");
  int found=0;
  for(String line:Files.readAllLines(Path.of(args[0]))) {
   if(!line.startsWith("flowerpot"))continue;
   String[] fields=line.split(";");int ordinal=Integer.parseInt(fields[2]);
   Block block=LegacyFlowerPots.fromPlan(ordinal).getBlock();
   if(LegacyFlowerPots.toPlan(block)!=ordinal)throw new AssertionError("Palette roundtrip: "+line);
   if(ordinal!=255) {
    String expected=plants[ordinal-1];String actual=Registry.BLOCK.getKey(((FlowerPotBlock)block).getContent()).getPath();
    if(!actual.equals(expected))throw new AssertionError(expected+" != "+actual);
   }
   found++;
  }
  if(found!=22)throw new AssertionError("Expected 22 palette entries: "+found);
  for(int empty:new int[]{0,-1,255})if(LegacyFlowerPots.fromPlan(empty).getBlock()!=Blocks.FLOWER_POT)throw new AssertionError("Empty alias");
  for(int bad:new int[]{-2,22,256}){try{LegacyFlowerPots.fromPlan(bad);throw new AssertionError("Invalid ordinal accepted");}catch(IllegalArgumentException expected){}}
  try{LegacyFlowerPots.toPlan(Blocks.POTTED_BAMBOO);throw new AssertionError("Unsupported plant silently lost");}catch(IllegalArgumentException expected){}
  System.out.println("PASS: all 22 original flowerpot palette entries, plant identity, round trips, empty aliases and invalid/unsupported guards. Live construction/export remains untested.");
 }
}''',encoding='utf-8')
cp=(r/'build/migration-classpath.txt').read_text().strip();o=r/'build/flowerpot-check';o.mkdir(exist_ok=True);q=lambda x:'"'+str(x).replace('\\','/')+'"'
(r/'build/flowerpot-javac.args').write_text('\n'.join(map(q,['-encoding','UTF-8','-cp',cp,'-d',o,r/'tools/FlowerPotCheck.java',r/'src/main/java/org/millenaire/common/utilities/LegacyFlowerPots.java'])),encoding='utf-8')
(r/'build/flowerpot-java.args').write_text('\n'.join(map(q,['-cp',str(o)+';'+cp,'FlowerPotCheck',r/'src/main/resources/todeploy/millenaire/blocklist.txt'])),encoding='utf-8')

