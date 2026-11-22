from pathlib import Path
import subprocess
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');j=Path('C:/Users/sviat/Desktop/mods/.millenaire-tools/jdk-17.0.20.1+1/bin');o=Path('work/plans24-check').resolve();o.mkdir(exist_ok=True)
s=o/'Plans24Check.java';s.write_text(r'''import java.io.*;import java.nio.file.*;import java.util.*;
import net.minecraft.world.level.block.Blocks;import org.millenaire.common.buildingplan.*;import org.millenaire.common.village.*;
class Plans24Check {
 interface IO {void run() throws Exception;}
 static void ok(boolean b,String m){if(!b)throw new AssertionError(m);}
 static void rejects(IO task)throws Exception{try{task.run();throw new AssertionError("Malformed input accepted");}catch(IOException expected){}}
 static void block(DataOutputStream out)throws Exception{out.writeInt(12);out.writeShort(-40);out.writeInt(-8);BuildingBlockStateIO.write(out,Blocks.STONE.defaultBlockState(),(byte)0,(byte)0);}
 public static void main(String[]args)throws Exception{
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
  Path dir=Files.createTempDirectory(Path.of(args[0]),"cases-");Path file=dir.resolve("blocks.bin");
  try(var out=new DataOutputStream(Files.newOutputStream(file))){out.writeInt(2);block(out);block(out);}
  byte[] valid=Files.readAllBytes(file);
  for(int i=0;i<=2;i++){var blocks=SavedPlanReader.blocks(file,i);ok(blocks.length==2&&blocks[0].p.getiY()==-40&&blocks[0].block==Blocks.STONE,"roundtrip and negative height");}
  rejects(()->SavedPlanReader.blocks(file,-1));rejects(()->SavedPlanReader.blocks(file,3));
  for(int cut=0;cut<valid.length;cut++){Files.write(file,Arrays.copyOf(valid,cut));rejects(()->SavedPlanReader.blocks(file,0));}
  for(int count:new int[]{-1,Integer.MAX_VALUE,1000001,500000}){try(var out=new DataOutputStream(Files.newOutputStream(file))){out.writeInt(count);}rejects(()->SavedPlanReader.blocks(file,0));}
  Files.write(file,Arrays.copyOf(valid,valid.length+1));rejects(()->SavedPlanReader.blocks(file,0));
  Path paths=dir.resolve("paths.bin");try(var out=new DataOutputStream(Files.newOutputStream(paths))){out.writeInt(2);out.writeInt(1);block(out);out.writeInt(1);block(out);}
  ok(SavedPlanReader.paths(paths,1,1).size()==2,"nested paths valid");
  rejects(()->SavedPlanReader.paths(paths,0,2));rejects(()->SavedPlanReader.paths(paths,3,0));rejects(()->SavedPlanReader.paths(paths,2,1));
  ok(SavedPlanReader.paths(paths,2,0).size()==2,"finished paths valid");
  byte[] nested=Files.readAllBytes(paths);Files.write(paths,Arrays.copyOf(nested,nested.length-1));rejects(()->SavedPlanReader.paths(paths,0,0));
  try(var out=new DataOutputStream(Files.newOutputStream(paths))){out.writeInt(1);out.writeInt(Integer.MAX_VALUE);}rejects(()->SavedPlanReader.paths(paths,0,0));
  Path points=dir.resolve("clear.bin");try(var out=new DataOutputStream(Files.newOutputStream(points))){out.writeInt(1);out.writeInt(1);out.writeShort(-64);out.writeInt(2);}
  ok(SavedPlanReader.points(points,1).get(0).getiY()==-64,"clear points roundtrip");rejects(()->SavedPlanReader.points(points,2));
  try(var out=new DataOutputStream(Files.newOutputStream(points))){out.writeInt(1);out.writeInt(Integer.MAX_VALUE);out.writeShort(0);out.writeInt(0);}rejects(()->SavedPlanReader.points(points,0));
  Path oversized=dir.resolve("large.bin");try(var out=new RandomAccessFile(oversized.toFile(),"rw")){out.setLength(SavedPlanReader.MAX_BYTES+1);}rejects(()->SavedPlanReader.blocks(oversized,0));Files.delete(oversized);
  var uf=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);var u=(sun.misc.Unsafe)uf.get(null);
  org.millenaire.common.utilities.MillLog.console=false;
  var b=(Building)u.allocateInstance(Building.class);b.requirePlanFilesWritable();b.protectPlanFiles(file.toFile(),new IOException("injected"));
  try{b.writeToNBT(new net.minecraft.nbt.CompoundTag());throw new AssertionError("Unsafe main snapshot allowed");}catch(IllegalStateException expected){ok(expected.getMessage().contains("repair"),"main snapshot blocked before serialization");}
  var construction=new ConstructionIP(b,0,false);
  try{construction.writeBblocks();throw new AssertionError("Unsafe auxiliary write allowed");}catch(IllegalStateException expected){}
  System.out.println("PASS bounded counts/bytes, every truncated block prefix, nested paths, trailing bytes, progress bounds, negative heights, coordinates, blocked main/auxiliary writes");
 }
}''',encoding='utf-8')
cp=str(r/'build/classes/java/main')+';'+(r/'build/migration-classpath.txt').read_text().strip()
subprocess.run([str(j/'javac.exe'),'-cp',cp,'-d',str(o),str(s)],check=True)
raise SystemExit(subprocess.run([str(j/'java.exe'),'-cp',str(o)+';'+cp,'Plans24Check',str(o)]).returncode)
