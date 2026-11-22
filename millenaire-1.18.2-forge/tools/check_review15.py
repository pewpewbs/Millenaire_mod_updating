from pathlib import Path
import subprocess
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');j=Path('C:/Users/sviat/Desktop/mods/.millenaire-tools/jdk-17.0.20.1+1/bin');o=Path('work/review15-check').resolve();o.mkdir(exist_ok=True)
s=o/'Review15Check.java'
s.write_text('''import org.millenaire.common.block.*;
import org.millenaire.common.entity.*;
import org.millenaire.common.goal.GoalSleep;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.pathfinder.PathComputationType;
public class Review15Check {
 static void check(boolean x,String m){if(!x)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception {
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
  ((net.minecraftforge.registries.ForgeRegistry)net.minecraftforge.registries.ForgeRegistries.BLOCKS).unfreeze();
  var unfreeze=net.minecraft.core.Registry.BLOCK.getClass().getMethod("unfreeze");unfreeze.setAccessible(true);unfreeze.invoke(net.minecraft.core.Registry.BLOCK);
  var road=new BlockPath("fixture_road",MaterialColor.DIRT,SoundType.GRAVEL);
  var slab=new BlockPathSlab("fixture_road",MaterialColor.DIRT,SoundType.GRAVEL);
  var world=EmptyBlockGetter.INSTANCE;var pos=BlockPos.ZERO;
  for(boolean stable:new boolean[]{false,true}){
   var rs=road.defaultBlockState().setValue(IBlockPath.STABLE,stable);
   check(!rs.isPathfindable(world,pos,PathComputationType.LAND),"road cannot be traversed inside");
   check(Math.abs(rs.getCollisionShape(world,pos).max(Direction.Axis.Y)-15.0/16)<.0001,"road height retained");
   for(Half h:Half.values()){
    var ss=slab.defaultBlockState().setValue(IBlockPath.STABLE,stable).setValue(BlockPathSlab.HALF,h);
    check(!ss.isPathfindable(world,pos,PathComputationType.LAND),"slab cannot be traversed inside");
    check(Math.abs(ss.getCollisionShape(world,pos).max(Direction.Axis.Y)-(h==Half.TOP?15.0:7.0)/16)<.0001,"slab height retained");
   }
  }
  var panel=new BlockPanel("fixture_panel");check(panel.defaultBlockState().getDestroySpeed(world,pos)>=20,"panel hardness");
  check(net.minecraft.world.entity.ai.navigation.GroundPathNavigation.class.isAssignableFrom(PathNavigateSimple.class),"native ground navigation");
  var f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);var u=(sun.misc.Unsafe)f.get(null);
  var v=(MillVillager)u.allocateInstance(MillVillager.EntityGenericMale.class);var sleep=new GoalSleep();sleep.key="sleep";
  v.shouldLieDown=true;check(sleep.labelKeyWhileTravelling(v).equals("sleep"),"sleeping label despite remote target");
  v.shouldLieDown=false;check(sleep.labelKeyWhileTravelling(v).equals("sleep_travelling"),"travelling label");
  System.out.println("PASS: 6 road states block inside-pathing while retaining surface heights; panel hardness; native navigation inheritance; sleeping/travelling labels.");
 }
}''')
cp=str(r/'build/classes/java/main')+';'+(r/'build/migration-classpath.txt').read_text().strip()
subprocess.run([str(j/'javac.exe'),'-cp',cp,'-d',str(o),str(s)],check=True)
subprocess.run([str(j/'java.exe'),'-cp',str(o)+';'+cp,'Review15Check'],check=True)
