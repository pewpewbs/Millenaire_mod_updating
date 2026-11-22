from pathlib import Path
import subprocess,sys
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');j=Path('C:/Users/sviat/Desktop/mods/.millenaire-tools/jdk-17.0.20.1+1/bin')
out=Path('work/entity-identity-check').resolve();out.mkdir(exist_ok=True)
src=out/'EntityIdentityCheck.java'
src.write_text('''import org.millenaire.common.entity.MillVillager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.*;
public class EntityIdentityCheck {
 public static void main(String[] args)throws Exception {
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
  var f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);var u=(sun.misc.Unsafe)f.get(null);
  var first=(MillVillager)u.allocateInstance(MillVillager.EntityGenericMale.class);
  var second=(MillVillager)u.allocateInstance(MillVillager.EntityGenericMale.class);
  first.setId(101);second.setId(102);first.setVillagerId(12345L);second.setVillagerId(12345L);
  EntitySection<Entity> section=new EntitySection<>(Entity.class,Visibility.TRACKED);
  section.add(first);section.add(second);section.remove(second);
  boolean correct=section.size()==1 && section.getEntities().findFirst().orElse(null)==first;
  if(args.length>0 && args[0].equals("before")){
   if(correct)throw new AssertionError("Expected original defect");
   System.out.println("REPRODUCED: removing duplicate B removes original A from actual Minecraft EntitySection; B remains in its old section.");return;
  }
  if(!correct||first.equals(second))throw new AssertionError("Distinct entities confused");
  var set=new java.util.HashSet<MillVillager>();set.add(first);first.setVillagerId(67890L);
  if(!set.remove(first))throw new AssertionError("Logical ID change corrupted entity set");
  EntitySection<Entity> next=new EntitySection<>(Entity.class,Visibility.TRACKED);next.add(second);
  if(section.getEntities().anyMatch(e->e==second)||next.size()!=1)throw new AssertionError("Entity retained in two sections");
  System.out.println("PASS: actual entity-section removal keeps original, moved duplicate only in new section, entity hash stable across logical-ID changes.");
 }
}''')
cp=str(r/'build/classes/java/main')+';'+(r/'build/migration-classpath.txt').read_text().strip()
subprocess.run([str(j/'javac.exe'),'-cp',cp,'-d',str(out),str(src)],check=True)
subprocess.run([str(j/'java.exe'),'-cp',str(out)+';'+cp,'EntityIdentityCheck']+sys.argv[1:],check=True)
