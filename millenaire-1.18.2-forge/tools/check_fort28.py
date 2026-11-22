from pathlib import Path
import subprocess
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge'); j=Path('C:/Users/sviat/Desktop/mods/.millenaire-tools/jdk-17.0.20.1+1/bin')
o=Path('work/fort28-check').resolve();o.mkdir(exist_ok=True)
s=o/'FortCheck.java'
s.write_text('''import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.Mob;
import net.minecraft.util.profiling.*;
class FortCheck {
 static class Grid extends NodeEvaluator {
  int expanded; boolean moat;
  Grid(boolean m){moat=m;}
  boolean wall(int x,int z){return (Math.abs(x)==12&&Math.abs(z)<=12 || Math.abs(z)==12&&Math.abs(x)<=12) && !(x==0&&z==-12);}
  boolean water(int x,int z){return moat && Math.max(Math.abs(x),Math.abs(z))>=14 && Math.max(Math.abs(x),Math.abs(z))<=18 && !(x==0&&z<0);}
  public Node getStart(){return getNode(0,0,10);}
  public Target getGoal(double x,double y,double z){return new Target(getNode((int)x,(int)y,(int)z));}
  public int getNeighbors(Node[] out,Node n){expanded++;int count=0;for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int x=n.x+d[0],z=n.z+d[1];if(Math.abs(x)>32||Math.abs(z)>32||wall(x,z))continue;Node v=getNode(x,0,z);if(v.closed)continue;v.costMalus=water(x,z)?32:0;out[count++]=v;}return count;}
  public BlockPathTypes getBlockPathType(BlockGetter b,int x,int y,int z){return BlockPathTypes.WALKABLE;}
  public BlockPathTypes getBlockPathType(BlockGetter b,int x,int y,int z,Mob m,int w,int h,int d,boolean a,boolean c){return getBlockPathType(b,x,y,z);}
 }
 static Path run(boolean moat,boolean reverse,float limit)throws Exception {
  Grid g=new Grid(moat);Node start=reverse?g.getGoal(0,0,22):g.getStart();
  BlockPos dest=reverse?new BlockPos(0,0,10):new BlockPos(0,0,22);
  Target target=g.getGoal(dest.getX(),0,dest.getZ());
  var method=PathFinder.class.getDeclaredMethod("findPath",ProfilerFiller.class,Node.class,Map.class,float.class,int.class,float.class);method.setAccessible(true);
  PathFinder finder=new PathFinder(g,256); float multiplier=4f; if(limit>64){var bounded=new org.millenaire.common.pathing.DetourPathFinder(g,256);var field=bounded.getClass().getDeclaredField("detours");field.setAccessible(true);finder=(PathFinder)field.get(bounded);limit=org.millenaire.common.pathing.DetourPathFinder.DETOUR_WALK_LIMIT;multiplier=1f;} long t=System.nanoTime();Path p=(Path)method.invoke(finder,InactiveProfiler.INSTANCE,start,Map.of(target,dest),limit,0,multiplier);
  System.out.println("moat="+moat+" reverse="+reverse+" limit="+limit+" reached="+p.canReach()+" expanded="+g.expanded+" nodes="+p.getNodeCount()+" ms="+(System.nanoTime()-t)/1e6);
  if(limit>64){if(!p.canReach())throw new AssertionError("detour incomplete");boolean exit=false;for(int i=0;i<p.getNodeCount();i++){Node n=p.getNode(i);if(g.wall(n.x,n.z)||g.water(n.x,n.z))throw new AssertionError("wall or moat shortcut");if(n.x==0&&n.z==-12)exit=true;}if(!exit)throw new AssertionError("exit missed");if(g.expanded>4096)throw new AssertionError("budget");}
  return p;
 }
 public static void main(String[] args)throws Exception{net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();for(boolean moat:new boolean[]{false,true})for(boolean reverse:new boolean[]{false,true}){if(run(moat,reverse,64).canReach())throw new AssertionError("fixture must reproduce old limit");run(moat,reverse,192);}System.out.println("PASS bounded fort detours both directions, bridge retained");}
}''',encoding='utf-8')
cp=str(r/'build/classes/java/main')+';'+(r/'build/migration-classpath.txt').read_text().strip()
subprocess.run([str(j/'javac.exe'),'-cp',cp,'-d',str(o),str(s)],check=True)
subprocess.run([str(j/'java.exe'),'-cp',str(o)+';'+cp,'FortCheck'],check=True)



