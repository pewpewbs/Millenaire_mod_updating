import org.millenaire.common.village.*;import org.millenaire.common.forge.Mill;import org.millenaire.common.utilities.Point;
public class VillageMapSnapshotCheck {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static VillageMapInfo map(){var m=new VillageMapInfo();m.length=32;m.width=32;m.mapStartX=-16;m.mapStartZ=-16;m.frequency=1000;m.topGround=new short[32][32];m.spaceAbove=new short[32][32];m.danger=new boolean[32][32];m.buildingLocRef=new BuildingLocation[32][32];m.canBuild=new boolean[32][32];m.buildingForbidden=new boolean[32][32];m.water=new boolean[32][32];m.tree=new boolean[32][32];m.path=new boolean[32][32];m.topAdjusted=new boolean[32][32];m.buildTested=new boolean[32][32];return m;}
 public static void main(String[] args)throws Exception{
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();var m=map();var clone=m.clone();clone.topGround[0][0]=100;clone.spaceAbove[0][0]=2;clone.danger[0][0]=true;clone.topAdjusted[0][0]=true;clone.buildTested[0][0]=true;clone.buildingLocRef[0][0]=new BuildingLocation();
  check(m.topGround[0][0]==0&&m.spaceAbove[0][0]==0&&!m.danger[0][0]&&!m.topAdjusted[0][0]&&!m.buildTested[0][0]&&m.buildingLocRef[0][0]==null,"Independent snapshot arrays, including formerly shared flags");
  var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);var unsafe=(sun.misc.Unsafe)field.get(null);m.world=(net.minecraft.server.level.ServerLevel)unsafe.allocateInstance(net.minecraft.server.level.ServerLevel.class);
  var old=new BuildingLocation();m.buildingLocRef[0][0]=old;var first=new BuildingLocation();first.minxMargin=-2;first.minzMargin=-2;first.maxxMargin=2;first.maxzMargin=2;
  check(!m.update(m.world,java.util.List.of(first),new Point(0,0,0),0),"Unchanged extents retain terrain arrays");check(m.buildingLocRef[0][0]==null&&m.buildingLocRef[16][16]==first,"Stale building occupancy cleared");
  var second=new BuildingLocation();second.minxMargin=4;second.minzMargin=4;second.maxxMargin=6;second.maxzMargin=6;m.update(m.world,java.util.List.of(second),new Point(0,0,0),0);check(m.buildingLocRef[16][16]==null&&m.buildingLocRef[20][20]==second,"Same count, different locations refresh occupancy");
  m.frequency=0;m.updateNextChunk();check(Mill.proxy.queued.size()==1,"Zero frequency guarded and update routed through world scheduler");m.topGround=new short[32][32];Mill.proxy.queued.remove(0).run();check(m.topGround[0][0]==0,"Queued result for a replaced map is discarded before any world read");
  m.updateNextChunk();m.world=null;Mill.proxy.queued.remove(0).run();check(m.topGround[0][0]==0,"Queued result for another world is discarded");
  System.out.println("PASS: actual VillageMapInfo snapshots, occupancy refresh with equal location counts, scheduler routing, frequency guard and stale map/world cancellation. Scheduler/world/classification are fixtures; live chunk reads and timing untested.");
 }
}