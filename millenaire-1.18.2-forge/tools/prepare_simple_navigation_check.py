from pathlib import Path
r=Path(__file__).resolve().parents[1];q=lambda x:'"'+str(x).replace('\\','/')+'"'
(r/'tools/SimpleNavigationCheck.java').write_text('''import org.millenaire.common.entity.PathNavigateSimple;import net.minecraft.world.entity.*;import net.minecraft.world.entity.animal.Pig;import net.minecraft.world.entity.ai.navigation.PathNavigation;import net.minecraft.world.level.pathfinder.*;import net.minecraft.world.phys.*;import net.minecraft.core.BlockPos;import java.util.*;
public class SimpleNavigationCheck {
 static class Nav extends PathNavigateSimple {
  boolean shortcut;int probes;Vec3 lastTarget;
  Nav(){super(null,null);}
  protected Vec3 getTempMobPos(){return new Vec3(mob.getX(),Math.floor(mob.getY()),mob.getZ());}
  protected boolean canMoveDirectly(Vec3 a,Vec3 b,int x,int y,int z){probes++;lastTarget=b;return shortcut;}
  void route(Path p){path=p;}
  void step(){followThePath();}
  PathFinder finder(){return createPathFinder(100);}
 }
 static void field(Class<?> c,Object target,String name,Object value)throws Exception{var f=c.getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
 static void pos(Mob mob,double x,double y,double z)throws Exception{field(Entity.class,mob,"position",new Vec3(x,y,z));}
 static Path path(Node... nodes){return new Path(new ArrayList<>(List.of(nodes)),new BlockPos(20,0,20),true);}
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();var f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);var unsafe=(sun.misc.Unsafe)f.get(null);var mob=(Pig)unsafe.allocateInstance(Pig.class);field(Entity.class,mob,"dimensions",EntityDimensions.fixed(.6f,1.8f));var nav=(Nav)unsafe.allocateInstance(Nav.class);field(PathNavigation.class,nav,"mob",mob);
  check(nav.finder()!=null,"Modern factory returns a usable path finder");nav.step();
  var p=path(new Node(-3,-40,-7),new Node(-2,-40,-7));nav.route(p);pos(mob,-2.5,-40,-6.5);nav.step();check(p.getNextNodeIndex()==1,"Negative-coordinate waypoint reached");pos(mob,-1.5,-40,-6.5);nav.step();check(p.isDone(),"Final waypoint completed");nav.step();check(p.isDone(),"Completed path safe");
  p=path(new Node(1,10,1));nav.route(p);pos(mob,1.5,12,1.5);nav.step();check(p.getNextNodeIndex()==0,"Vertical gap does not skip waypoint");pos(mob,4,10,1.5);nav.step();check(p.getNextNodeIndex()==0,"Distant waypoint not reached");
  p=path(new Node(1,0,0),new Node(2,0,0),new Node(3,0,0),new Node(4,1,0));nav.route(p);pos(mob,0,0,0);nav.shortcut=true;nav.probes=0;nav.step();check(p.getNextNodeIndex()==2&&nav.probes==1,"Shortcut limited to same-height run");
  nav.shortcut=false;p=path();nav.route(p);nav.step();check(p.isDone(),"Empty path safe");
  System.out.println("PASS: actual navigation follow logic, real Minecraft Path/Pig fields: factory, missing/empty/completed routes, negative waypoints, distance/height gates and same-height shortcuts. Entity construction and direct-path/world checks are fixtures; live movement and swimming untested.");
 }
}''',encoding='utf-8')
cp=(r/'build/migration-classpath.txt').read_text().strip();o=r/'build/simple-navigation-check';o.mkdir(exist_ok=True)
a=['-encoding','UTF-8','-cp',cp,'-d',o,r/'tools/SimpleNavigationCheck.java',r/'src/main/java/org/millenaire/common/entity/PathNavigateSimple.java']
(r/'build/simple-navigation-javac.args').write_text('\n'.join(map(q,a)),encoding='utf-8');(r/'build/simple-navigation-java.args').write_text('\n'.join(map(q,['-cp',str(o)+';'+cp,'SimpleNavigationCheck'])),encoding='utf-8')
