from pathlib import Path
import subprocess
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');j=Path('C:/Users/sviat/Desktop/mods/.millenaire-tools/jdk-17.0.20.1+1/bin');o=Path('work/navigation29-check').resolve();o.mkdir(exist_ok=True)
s=o/'NavigationCheck.java'
s.write_text('''import java.util.*;import net.minecraft.world.entity.*;import net.minecraft.world.entity.ai.attributes.*;import net.minecraft.world.entity.ai.navigation.*;import net.minecraft.world.level.pathfinder.*;import org.millenaire.common.entity.*;
class NavigationCheck {
 public static void main(String[] args)throws Exception {
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
  var uf=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);var u=(sun.misc.Unsafe)uf.get(null);
  var mob=(MillVillager)u.allocateInstance(MillVillager.EntityGenericMale.class);
  var a=LivingEntity.class.getDeclaredField("attributes");a.setAccessible(true);a.set(mob,new AttributeMap(AttributeSupplier.builder().add(Attributes.FOLLOW_RANGE,16).build()));
  var costs=Mob.class.getDeclaredField("pathfindingMalus");costs.setAccessible(true);costs.set(mob,new EnumMap<BlockPathTypes,Float>(BlockPathTypes.class));
  var factory=MillVillager.class.getDeclaredMethod("createNavigation",net.minecraft.world.level.Level.class);factory.setAccessible(true);
  Object navigation=factory.invoke(mob,new Object[]{null});
  if(navigation.getClass()!=PathNavigateSimple.class)throw new AssertionError("wrong navigator");
  var pf=PathNavigation.class.getDeclaredField("pathFinder");pf.setAccessible(true);
  if(!(pf.get(navigation) instanceof org.millenaire.common.pathing.DetourPathFinder))throw new AssertionError("wrong finder");
  var ev=PathNavigation.class.getDeclaredField("nodeEvaluator");ev.setAccessible(true);
  if(!(ev.get(navigation) instanceof org.millenaire.common.pathing.VillagerWalkNodeEvaluator))throw new AssertionError("wrong evaluator");
  if(mob.getPathfindingMalus(BlockPathTypes.WATER)!=32)throw new AssertionError("water penalty missing");
  if(!((NodeEvaluator)ev.get(navigation)).canOpenDoors()||!((NodeEvaluator)ev.get(navigation)).canPassDoors())throw new AssertionError("doors disabled");
  System.out.println("PASS actual MillVillager factory installs PathNavigateSimple, DetourPathFinder, VillagerWalkNodeEvaluator, water costs and door flags");
 }
}''',encoding='utf-8')
cp=str(r/'build/classes/java/main')+';'+(r/'build/migration-classpath.txt').read_text().strip()
subprocess.run([str(j/'javac.exe'),'-cp',cp,'-d',str(o),str(s)],check=True)
subprocess.run([str(j/'java.exe'),'-cp',str(o)+';'+cp,'NavigationCheck'],check=True)
