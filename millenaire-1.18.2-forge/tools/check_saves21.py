from pathlib import Path
import subprocess
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');j=Path('C:/Users/sviat/Desktop/mods/.millenaire-tools/jdk-17.0.20.1+1/bin')
o=Path('work/saves21-check').resolve();o.mkdir(exist_ok=True)
s=o/'Saves21Check.java'
s.write_text(r'''import java.io.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.nbt.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.server.level.ServerLevel;
import org.millenaire.common.village.Building;
import org.millenaire.common.utilities.AtomicSaveFile;
class Saves21Check {
 static void ok(boolean b,String m){if(!b)throw new AssertionError(m);}
 static Field field(Class<?> c,String n)throws Exception{var f=c.getDeclaredField(n);f.setAccessible(true);return f;}
 static Thread worker(CompoundTag tag,Path p,long rev,AtomicSaveFile.Batch batch)throws Exception{
  var c=Class.forName("org.millenaire.common.village.Building$SaveWorker");
  var ctor=c.getDeclaredConstructor(CompoundTag.class,Path.class,long.class,String.class,AtomicSaveFile.Batch.class);ctor.setAccessible(true);
  return (Thread)ctor.newInstance(tag.copy(),p,rev,"test",batch);
 }
 public static void main(String[] args)throws Exception{
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
  field(org.millenaire.common.utilities.MillLog.class,"console").setBoolean(null,false);
  Path dir=Files.createTempDirectory(Path.of(args[0]),"cases-");Path file=dir.resolve("village.gz");
  Files.writeString(file,"old");
  try{AtomicSaveFile.write(file,out->{out.write("partial".getBytes());throw new IOException("injected disk write failure");});throw new AssertionError("must fail");}catch(IOException expected){}
  ok(Files.readString(file).equals("old"),"failed write preserves original");
  try(var list=Files.list(dir)){ok(list.count()==1,"failed write cleans temp");}
  AtomicSaveFile.write(file,out->{out.write("new".getBytes());out.close();});
  ok(Files.readString(file).equals("new")&&Files.readString(dir.resolve("village.gz.bak")).equals("old"),"replacement and backup, serializer close safe");
  byte[] live={1,2,3};Path aux=dir.resolve("paths.bin");var retry=new AtomicInteger();
  var batch=AtomicSaveFile.capture();try(batch){AtomicSaveFile.write(aux,out->out.write(live));AtomicSaveFile.retryIfFailed(retry::incrementAndGet);}
  live[0]=9;ok(!Files.exists(aux),"capture does not write live files");
  var tag=new CompoundTag();tag.putString("generation","snapshot");var w=worker(tag,file,1,batch);tag.putString("generation","mutated");
  w.start();w.join(10000);ok(!w.isAlive(),"worker completed");
  ok(field(w.getClass(),"succeeded").getBoolean(w),"worker success");
  ok(NbtIo.readCompressed(file.toFile()).getString("generation").equals("snapshot"),"immutable NBT snapshot");
  ok(Files.readAllBytes(aux)[0]==1&&retry.get()==0,"immutable auxiliary bytes, no retry on success");
  var u=(sun.misc.Unsafe)field(sun.misc.Unsafe.class,"theUnsafe").get(null);
  var world=(ServerLevel)u.allocateInstance(ServerLevel.class);
  var data=Proxy.newProxyInstance(Saves21Check.class.getClassLoader(),new Class[]{WritableLevelData.class},(p,m,a)->{if(m.getName().equals("getGameTime"))return 1000L;throw new UnsupportedOperationException(m.getName());});
  field(Level.class,"levelData").set(world,data);
  var b=(Building)u.allocateInstance(Building.class);field(Building.class,"world").set(b,world);field(Building.class,"isTownhall").setBoolean(b,true);
  field(Building.class,"saveNeeded").setBoolean(b,true);field(Building.class,"saveRevision").setLong(b,2);field(Building.class,"saveWorker").set(b,w);
  b.finishPendingSave();ok(field(Building.class,"saveNeeded").getBoolean(b),"newer changes remain dirty");
  field(Building.class,"saveRevision").setLong(b,1);field(Building.class,"saveWorker").set(b,w);b.finishPendingSave();
  ok(!field(Building.class,"saveNeeded").getBoolean(b),"matching generation clears dirty");
  b.markSaveNeeded();ok(field(Building.class,"saveNeeded").getBoolean(b)&&field(Building.class,"saveRevision").getLong(b)==2,"mutation increments revision");
  var construction=new org.millenaire.common.village.ConstructionIP(b,0,false);
  construction.startNewConstruction(null,new org.millenaire.common.buildingplan.BuildingBlock[1]);
  construction.incrementBblockPos();construction.clearBuildingLocation();construction.clearBblocks();
  ok(field(Building.class,"saveRevision").getLong(b)==6&&construction.isBblocksChanged(),"construction changes mark save revision");
  var bad=AtomicSaveFile.capture();try(bad){AtomicSaveFile.write(file.resolve("impossible"),out->out.write(1));AtomicSaveFile.retryIfFailed(retry::incrementAndGet);}
  var failed=worker(tag,file,2,bad);failed.start();failed.join(10000);
  ok(!failed.isAlive()&&!field(failed.getClass(),"succeeded").getBoolean(failed)&&field(failed.getClass(),"failure").get(failed)!=null,"worker reports IO failure");
  ok(NbtIo.readCompressed(file.toFile()).getString("generation").equals("snapshot"),"aux failure prevents main replacement");
  field(Building.class,"saveWorker").set(b,failed);b.finishPendingSave();
  ok(retry.get()==1&&field(Building.class,"saveNeeded").getBoolean(b)&&field(Building.class,"saveWorker").get(b)==null,"failed snapshot retried, dirty preserved");
  ok(field(Building.class,"nextSaveAttempt").getLong(b)==1100L,"bounded retry backoff");
  AtomicSaveFile.delete(aux);ok(!Files.exists(aux)&&Files.readAllBytes(dir.resolve("paths.bin.bak"))[0]==1,"deleted auxiliary backed up");
  System.out.println("PASS atomic failure/replacement/backup, immutable main and auxiliary snapshots, worker IO failure, dirty generation and retry backoff");
 }
}''',encoding='utf-8')
cp=str(r/'build/classes/java/main')+';'+(r/'build/migration-classpath.txt').read_text().strip()
subprocess.run([str(j/'javac.exe'),'-cp',cp,'-d',str(o),str(s)],check=True)
subprocess.run([str(j/'java.exe'),'-cp',str(o)+';'+cp,'Saves21Check',str(o)],check=True)
