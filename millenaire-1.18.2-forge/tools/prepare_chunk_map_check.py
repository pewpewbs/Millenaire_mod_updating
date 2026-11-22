from pathlib import Path
root=Path(__file__).resolve().parents[1];src=root/'src/main/java/org/millenaire';fixtures=root/'tools/fixtures/chunk-map';fixtures.mkdir(parents=True,exist_ok=True)
files={
'common/forge/Mill':'''public class Mill { public static org.millenaire.common.world.MillWorldData getMillWorld(net.minecraft.world.level.Level level){throw new UnsupportedOperationException("Server capture not tested here");} }''',
'common/world/MillWorldData':'''public class MillWorldData { public java.util.List<org.millenaire.common.village.Building> allBuildings(){return java.util.List.of();} }''',
'common/village/Building':'''public class Building { public boolean isTownhall; public Info winfo; public Type villageType; public String getVillageQualifiedName(){return "village";} public static class Info { public int mapStartX,mapStartZ,length,width; } public static class Type { public boolean lonebuilding; } }'''
}
for path,body in files.items():
 f=fixtures/(path+'.java');f.parent.mkdir(parents=True,exist_ok=True);f.write_text('package org.millenaire.'+path.rsplit('/',1)[0].replace('/','.')+';\n'+body,encoding='utf-8')
(root/'tools/ChunkMapCheck.java').write_text('''import java.nio.file.*;import java.nio.*;import java.util.*;import net.minecraft.world.level.*;import net.minecraft.core.BlockPos;import org.millenaire.common.utilities.RegionChunkIndex;import org.millenaire.client.gui.ChunkMapSnapshot;
public class ChunkMapCheck {
 static void region(Path dir,int rx,int rz,int[][] entries)throws Exception {
  ByteBuffer bytes=ByteBuffer.allocate(4*4096);for(int[] entry:entries)bytes.putInt(4*((entry[0]&31)+(entry[1]&31)*32),entry[2]);Files.write(dir.resolve("r."+rx+"."+rz+".mca"),bytes.array());
 }
 static void add(Object tracker,Object owner,long chunk,boolean ticking)throws Exception {
  Class<?> ownerClass=Class.forName("net.minecraftforge.common.world.ForgeChunkManager$TicketOwner");var ctor=ownerClass.getDeclaredConstructor(String.class,Comparable.class);ctor.setAccessible(true);Object ticket=ctor.newInstance("millenaire",owner);
  var add=tracker.getClass().getDeclaredMethod("add",ownerClass,long.class,boolean.class);add.setAccessible(true);add.invoke(tracker,ticket,chunk,ticking);
 }
 public static void main(String[] args)throws Exception {
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
  Path dir=Path.of(args[0]);Files.createDirectories(dir);
  region(dir,-1,-1,new int[][]{{-1,-1,513},{-32,-32,769},{-2,-2,257},{-3,-3,1281}});
  region(dir,0,0,new int[][]{{0,0,513},{31,31,769}});
  Set<Long> saved=RegionChunkIndex.read(dir,-32,-32,64);
  if(!saved.equals(Set.of(ChunkPos.asLong(-1,-1),ChunkPos.asLong(-32,-32),ChunkPos.asLong(0,0),ChunkPos.asLong(31,31))))throw new AssertionError("Region index boundaries/invalid offsets "+saved);
  if(!RegionChunkIndex.read(dir,-1,-1,2).equals(Set.of(ChunkPos.asLong(-1,-1),ChunkPos.asLong(0,0))))throw new AssertionError("Window clipping");
  Path missing=dir.resolve("absent");if(!RegionChunkIndex.read(missing,0,0,80).isEmpty() || Files.exists(missing))throw new AssertionError("Read generated files");
  Files.write(dir.resolve("r.2.2.mca"),new byte[12]);boolean rejected=false;try{RegionChunkIndex.read(dir,64,64,1);}catch(java.io.IOException expected){rejected=true;}if(!rejected)throw new AssertionError("Truncated header accepted");
  var data=new ForcedChunksSavedData();data.getChunks().add(1L);add(data.getBlockForcedChunks(),BlockPos.ZERO,2L,false);add(data.getBlockForcedChunks(),BlockPos.ZERO,3L,true);add(data.getEntityForcedChunks(),UUID.randomUUID(),4L,false);add(data.getEntityForcedChunks(),UUID.randomUUID(),5L,true);
  Set<Long> forced=ChunkMapSnapshot.collectForced(data);if(!forced.equals(Set.of(1L,2L,3L,4L,5L)))throw new AssertionError("Missing ticket owner/category");
  var live=new HashSet<Long>();live.add(11L);var areas=new ArrayList<ChunkMapSnapshot.VillageArea>();areas.add(new ChunkMapSnapshot.VillageArea(-2,-2,1,1,"test",false));
  var snapshot=new ChunkMapSnapshot(Level.NETHER,-40,-40,live,live,forced,areas);live.clear();areas.clear();
  if(!snapshot.loaded().contains(11L) || snapshot.villages().size()!=1 || !snapshot.villages().get(0).contains(-2,-2) || snapshot.villages().get(0).contains(2,0))throw new AssertionError("Mutable capture or bounds");
  boolean immutable=false;try{snapshot.loaded().clear();}catch(UnsupportedOperationException expected){immutable=true;}if(!immutable)throw new AssertionError("Mutable published collection");
  System.out.println("PASS: negative/positive region boundaries, window clipping, invalid/truncated entries, absent files without creation, all five forced-ticket categories, immutable snapshots and village bounds. Server capture/scheduling and game display not tested.");
 }
}
''',encoding='utf-8')
cp=(root/'build/migration-classpath.txt').read_text(encoding='utf-8').strip();out=root/'build/chunk-map-check';out.mkdir(exist_ok=True);q=lambda v:'"'+str(v).replace('\\','/')+'"'
args=['-encoding','UTF-8','-classpath',cp,'-d',out]+[src/p for p in ['common/utilities/RegionChunkIndex.java','client/gui/ChunkMapSnapshot.java']]+list(fixtures.rglob('*.java'))+[root/'tools/ChunkMapCheck.java']
(root/'build/chunk-map-check-javac.args').write_text('\n'.join(q(v) for v in args),encoding='utf-8');(root/'build/chunk-map-check-java.args').write_text('\n'.join(q(v) for v in ['-cp',str(out)+';'+cp,'ChunkMapCheck',root/'build/chunk-map-regions']),encoding='utf-8')
