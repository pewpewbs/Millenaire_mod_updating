from pathlib import Path
import subprocess
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');j=Path('C:/Users/sviat/Desktop/mods/.millenaire-tools/jdk-17.0.20.1+1/bin');o=Path('work/delivery16-check').resolve();o.mkdir(exist_ok=True)
s=o/'DeliveryCheck.java';s.write_text('''import java.util.*;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.millenaire.common.pathing.atomicstryker.AS_PathEntity;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.GoalBringBackResourcesHome;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.item.InvItem;
public class DeliveryCheck {
 static class Resident extends MillVillager.EntityGenericMale {
  Point destination;boolean inventoryRead;
  Resident(){super(null,null);}
  @Override public Point getGoalDestPoint(){return destination;}
  @Override public Set<InvItem> getInventoryKeys(){inventoryRead=true;return Set.of();}
 }
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception {
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
  var f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);var u=(sun.misc.Unsafe)f.get(null);
  var v=(Resident)u.allocateInstance(Resident.class);var position=net.minecraft.world.entity.Entity.class.getDeclaredField("position");position.setAccessible(true);
  var goal=new GoalBringBackResourcesHome();v.destination=new Point(100,64,0);
  position.set(v,new Vec3(16,64,0));check(!goal.performAction(v)&&!v.inventoryRead,"no deposit at halfway endpoint");
  position.set(v,new Vec3(100,70,0));check(!goal.isWithinActionRange(v,v.destination)&&!goal.performAction(v)&&!v.inventoryRead,"no deposit on another floor");
  position.set(v,new Vec3(98,64,0));check(goal.isWithinActionRange(v,v.destination)&&goal.performAction(v)&&v.inventoryRead,"near destination can access stock");
  var nodes=new Node[]{new Node(0,64,0),new Node(16,64,0)};var p=new AS_PathEntity(nodes,new BlockPos(100,64,0),false);
  check(!p.canReach()&&p.getTarget().getX()==100&&p.getEndNode().x==16,"partial route retains true target and reachability");
  p.advance();p.advance();check(p.isDone()&&!p.canReach(),"finishing segment is not reaching home");
  check(new AS_PathEntity(nodes,new BlockPos(16,64,0),true).canReach(),"full route retains reachability");
  System.out.println("PASS: actual delivery refuses halfway/other-floor actions before inventory access; nearby delivery allowed; partial/full target and completion semantics preserved.");
 }
}''')
cp=str(r/'build/classes/java/main')+';'+(r/'build/migration-classpath.txt').read_text().strip()
subprocess.run([str(j/'javac.exe'),'-cp',cp,'-d',str(o),str(s)],check=True)
subprocess.run([str(j/'java.exe'),'-cp',str(o)+';'+cp,'DeliveryCheck'],check=True)
