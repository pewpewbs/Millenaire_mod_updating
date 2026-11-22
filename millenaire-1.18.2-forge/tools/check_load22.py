from pathlib import Path
import subprocess
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');j=Path('C:/Users/sviat/Desktop/mods/.millenaire-tools/jdk-17.0.20.1+1/bin')
o=Path('work/load22-check').resolve();o.mkdir(exist_ok=True)
s=o/'Load22Check.java'
s.write_text(r'''import java.nio.file.*;import java.util.*;import java.lang.reflect.*;
import net.minecraft.nbt.*;import net.minecraft.server.level.ServerLevel;
import org.millenaire.common.world.*;import org.millenaire.common.village.*;import org.millenaire.common.culture.*;
class Load22Check {
 static sun.misc.Unsafe u;
 static Field f(Class<?> c,String n)throws Exception{var f=c.getDeclaredField(n);f.setAccessible(true);return f;}
 static void call(Object o,String n)throws Exception{var m=o.getClass().getDeclaredMethod(n);m.setAccessible(true);m.invoke(o);}
 static void ok(boolean b,String s){if(!b)throw new AssertionError(s);}
 static MillWorldData world(Path dir)throws Exception{
  var m=(MillWorldData)u.allocateInstance(MillWorldData.class);
  f(MillWorldData.class,"world").set(m,u.allocateInstance(ServerLevel.class));
  f(MillWorldData.class,"millenaireDir").set(m,dir.toFile());
  f(MillWorldData.class,"profiles").set(m,new HashMap<>());
  f(MillWorldData.class,"buildings").set(m,new HashMap<>());
  f(MillWorldData.class,"villagerRecords").set(m,new HashMap<>());return m;
 }
 static CompoundTag record(long id){var t=new CompoundTag();t.putLong("vr_lid",id);t.putString("vr_culture","test");t.putString("vr_type","worker");t.putString("vr_texture","millenaire:test");t.putFloat("vr_scale",0.9f);return t;}
 static void records(Path file,CompoundTag...tags)throws Exception{var root=new CompoundTag();var list=new ListTag();for(var t:tags)list.add(t);root.put("villagersrecords",list);NbtIo.writeCompressed(root,file.toFile());}
 public static void main(String[]args)throws Exception{
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
  u=(sun.misc.Unsafe)f(sun.misc.Unsafe.class,"theUnsafe").get(null);f(org.millenaire.common.utilities.MillLog.class,"console").setBoolean(null,false);
  Path dir=Files.createTempDirectory(Path.of(args[0]),"cases-");
  var culture=new Culture("test");culture.villagerTypes.put("worker",new VillagerType(culture,"worker"));
  ((Map<String,Culture>)f(Culture.class,"cultures").get(null)).put("test",culture);
  var mw=world(dir);var legacy=record(42);legacy.remove("vr_lid");legacy.putInt("vr_id",42);
  ok(VillagerRecord.read(mw,legacy,"vr").getVillagerId()==42,"legacy int ID");
  legacy.putLong("vr_id",5000000000L);ok(VillagerRecord.read(mw,legacy,"vr").getVillagerId()==5000000000L,"legacy long ID");
  for(long id:new long[]{0,Long.MIN_VALUE})ok(VillagerRecord.read(mw,record(id),"vr")==null,"invalid ID");
  var badType=record(1);badType.putString("vr_lid","1");ok(VillagerRecord.read(mw,badType,"vr")==null,"wrong ID type");
  Path file=dir.resolve("villagerRecords.gz");
  for(int scenario=0;scenario<4;scenario++){
   mw=world(dir);
   if(scenario==0)records(file,record(1),record(2));
   if(scenario==1)records(file,record(1),new CompoundTag());
   if(scenario==2)records(file,record(1),record(1));
   if(scenario==3)Files.writeString(file,"truncated gzip");
   byte[] before=Files.readAllBytes(file);call(mw,"loadVillagerRecords");
   ok(Arrays.equals(before,Files.readAllBytes(file)),"reading never rewrites records");
   ok(f(MillWorldData.class,"villagerRecordsLoadFailed").getBoolean(mw)==(scenario!=0),"failure protection flag");
   call(mw,"saveVillagerRecords");
   if(scenario!=0)ok(Arrays.equals(before,Files.readAllBytes(file)),"later save cannot overwrite incomplete load");
   else ok(NbtIo.readCompressed(file.toFile()).getList("villagersrecords",10).size()==2,"valid records save roundtrip");
  }
  ok(UserProfile.parseProfileUUID("1-1-1-1-1")==null&&UserProfile.parseProfileUUID("bad-name")==null,"strict UUID");
  Path profiles=Files.createDirectories(dir.resolve("profiles"));UUID good=UUID.randomUUID(),broken=UUID.randomUUID();
  Path gd=Files.createDirectory(profiles.resolve(good.toString())),bd=Files.createDirectory(profiles.resolve(broken.toString()));
  Files.writeString(gd.resolve("config.txt"),"player_name=Test\nculture_reputation=test,123\n");
  Files.writeString(gd.resolve("actiondata.txt"),"key:value:with:colons\n");
  Files.writeString(bd.resolve("config.txt"),"culture_reputation=test,not-a-number\n");
  Files.createDirectory(profiles.resolve("unresolvedLegacyName"));
  mw=world(dir);call(mw,"loadProfiles");ok(mw.profiles.containsKey(good)&&mw.profiles.containsKey(broken),"bad profile does not stop others");
  var valid=mw.profiles.get(good);ok(valid.getActionData("key").equals("value:with:colons"),"action value preserved");
  byte[] before=Files.readAllBytes(bd.resolve("config.txt"));mw.profiles.get(broken).saveProfile();
  ok(Arrays.equals(before,Files.readAllBytes(bd.resolve("config.txt"))),"broken profile protected");
  valid.saveProfile();ok(Files.exists(gd.resolve("config.txt.bak")),"valid profile atomic backup");
  ok(Files.isDirectory(profiles.resolve("unresolvedLegacyName")),"legacy source retained");
  System.out.println("PASS legacy IDs, invalid IDs, valid/partial/duplicate/corrupt records, no overwrite after failure, strict UUID, profile isolation, preserved action values, atomic profile backup");
 }
}''',encoding='utf-8')
cp=str(r/'build/classes/java/main')+';'+(r/'build/migration-classpath.txt').read_text().strip()
subprocess.run([str(j/'javac.exe'),'-cp',cp,'-d',str(o),str(s)],check=True)
result=subprocess.run([str(j/'java.exe'),'-cp',str(o)+';'+cp,'Load22Check',str(o)])
raise SystemExit(result.returncode)
