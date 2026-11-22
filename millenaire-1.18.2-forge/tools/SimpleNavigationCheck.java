import org.millenaire.common.entity.PathNavigateSimple;import net.minecraft.world.entity.*;import net.minecraft.world.entity.animal.Pig;import net.minecraft.world.entity.ai.navigation.PathNavigation;import net.minecraft.world.level.pathfinder.*;import net.minecraft.world.phys.*;import net.minecraft.core.BlockPos;import java.util.*;
public class SimpleNavigationCheck {
 static class Nav extends PathNavigateSimple {
  boolean shortcut;int probes,stuckChecks;Vec3 lastTarget;
  protected void doStuckDetection(Vec3 position){stuckChecks++;}
  Nav(){super(null,null);}
  protected Vec3 getTempMobPos(){return new Vec3(mob.getX(),Math.floor(mob.getY()),mob.getZ());}
  protected boolean canMoveDirectly(Vec3 a,Vec3 b,int x,int y,int z){probes++;lastTarget=b;return shortcut;}
  void route(Path p){path=p;}
  void step(){followThePath();}
  PathFinder finder(){return createPathFinder(100);}
  void timer(int n){lastStuckCheck=n;} int timer(){return lastStuckCheck;} double speed(){return speedModifier;}
 }
 static void field(Class<?> c,Object target,String name,Object value)throws Exception{var f=c.getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
 static void pos(Mob mob,double x,double y,double z)throws Exception{field(Entity.class,mob,"position",new Vec3(x,y,z));}
 static Path path(Node... nodes){return new Path(new ArrayList<>(List.of(nodes)),new BlockPos(20,0,20),true);}
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();var f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);var unsafe=(sun.misc.Unsafe)f.get(null);var mob=(Pig)unsafe.allocateInstance(Pig.class);field(Entity.class,mob,"dimensions",EntityDimensions.fixed(.6f,1.8f));var nav=(Nav)unsafe.allocateInstance(Nav.class);field(PathNavigation.class,nav,"mob",mob);
  check(nav.finder()!=null,"Modern factory returns a usable path finder");nav.step();
  var p=path(new Node(-3,-40,-7),new Node(-2,-40,-7));nav.route(p);pos(mob,-2.5,-40,-6.5);nav.step();check(p.getNextNodeIndex()==1,"Negative-coordinate waypoint reached");check(nav.stuckChecks==1,"Stuck detection invoked after path following");pos(mob,-1.5,-40,-6.5);nav.step();check(p.isDone(),"Final waypoint completed");nav.step();check(p.isDone(),"Completed path safe");
  p=path(new Node(1,10,1));nav.route(p);pos(mob,1.5,12,1.5);nav.step();check(p.getNextNodeIndex()==0,"Vertical gap does not skip waypoint");pos(mob,4,10,1.5);nav.step();check(p.getNextNodeIndex()==0,"Distant waypoint not reached");
  p=path(new Node(1,0,0),new Node(2,0,0),new Node(3,0,0),new Node(4,1,0));nav.route(p);pos(mob,0,0,0);nav.shortcut=true;nav.probes=0;nav.step();check(p.getNextNodeIndex()==2&&nav.probes==1,"Shortcut limited to same-height run");
  p=path(new Node(5,0,0));nav.route(p);nav.timer(37);check(nav.moveTo(p,.8)&&nav.timer()==37&&nav.speed()==.8,"Reusing active route preserves stuck timer and updates speed");nav.shortcut=false;p=path();nav.route(p);nav.step();check(p.isDone(),"Empty path safe");
  System.out.println("PASS: actual navigation follow logic, real Minecraft Path/Pig fields: factory, missing/empty/completed routes, negative waypoints, distance/height gates and same-height shortcuts. Entity construction and direct-path/world checks are fixtures; live movement and swimming untested.");
 }
}