from pathlib import Path
import subprocess
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');j=Path('C:/Users/sviat/Desktop/mods/.millenaire-tools/jdk-17.0.20.1+1/bin')
out=Path('work/goods-scan-check');out.mkdir(exist_ok=True)
source=out/'GoodsScanCheck.java'
source.write_text('''import java.util.*;import java.lang.reflect.*;
import net.minecraft.world.item.*;import net.minecraft.nbt.CompoundTag;
import org.millenaire.common.village.*;import org.millenaire.common.village.buildingmanagers.ResManager;
import org.millenaire.common.item.InvItem;import org.millenaire.common.utilities.Point;
public class GoodsScanCheck {
 static class Fixture extends Building {
  int stock,counts,records;
  Map<Long,VillagerRecord> residents=Map.of();
  Fixture(){super(null,(CompoundTag)null);}
  @Override public int countGoods(Item item,int meta){counts++;return stock;}
  @Override public Map<Long,VillagerRecord> getVillagerRecords(){records++;return residents;}
  @Override public Point getPos(){return new Point(0,64,0);}
 }
 static class Resident extends VillagerRecord {
  org.millenaire.common.culture.VillagerType type;int lookups;
  Resident(){super(null);}
  @Override public Point getHousePos(){return new Point(0,64,0);}
  @Override public org.millenaire.common.culture.VillagerType getType(){lookups++;return type;}
 }
 static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
  var uf=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);var u=(sun.misc.Unsafe)uf.get(null);
  var b=(Fixture)u.allocateInstance(Fixture.class);b.residents=Map.of();var res=new ResManager(b);
  var field=Building.class.getDeclaredField("resManager");field.setAccessible(true);field.set(b,res);
  var item=InvItem.createInvItem(Items.OAK_LOG,0);
  check(b.nbGoodAvailable(item,false,false,false)==0&&b.counts==0,"No chest: no inventory scan");
  res.chests.add(new Point(0,64,0));
  for(int flags=0;flags<8;flags++)check(b.nbGoodAvailable(item,(flags&1)!=0,(flags&2)!=0,(flags&4)!=0)==0,"Empty stock with each reservation mode");
  check(b.records==0,"Empty stock never scans residents or dereferences absent culture/location");
  b.location=(BuildingLocation)u.allocateInstance(BuildingLocation.class);b.stock=7;
  check(b.nbGoodAvailable(item,true,false,false)==7,"Nonempty construction stock retained");
  check(b.nbGoodAvailable(item,false,false,false)==7&&b.records==1,"Nonempty household stock still runs resident reservations");
  var resident=(Resident)u.allocateInstance(Resident.class);
  resident.type=(org.millenaire.common.culture.VillagerType)u.allocateInstance(org.millenaire.common.culture.VillagerType.class);
  resident.type.requiredFoodAndGoods=new HashMap<>();resident.type.requiredFoodAndGoods.put(item,3);b.residents=Map.of(1L,resident);
  check(b.nbGoodAvailable(item,false,false,false)==4,"Resident reserve remains unavailable");
  check(resident.lookups==1,"Resident type resolved once");
  check(b.nbGoodAvailable(item,true,false,false)==7,"Construction still bypasses resident reserve");
  b.stock=0;check(b.nbGoodAvailable(item,false,false,false)==0,"No stale availability cache after stock changes");
  System.out.println("PASS: actual Building availability: no chest, all 8 empty-stock modes, positive stock and stock changes.");
 }
}''',encoding='utf-8')
cp=str(r/'build/classes/java/main')+';'+(r/'build/migration-classpath.txt').read_text().strip()
stub=out/'stubs/org/millenaire/common/item/InvItem.java';stub.parent.mkdir(parents=True,exist_ok=True)
stub.write_text('''package org.millenaire.common.item;
public class InvItem { public net.minecraft.world.item.Item item; public int meta;
 public static InvItem createInvItem(net.minecraft.world.item.Item item,int meta){var result=new InvItem();result.item=item;result.meta=meta;return result;}
 public net.minecraft.world.item.Item getItem(){return item;}
 public boolean matches(InvItem other){return item==other.item && (meta==-1 || other.meta==-1 || meta==other.meta);}
}''',encoding='utf-8')
subprocess.run([str(j/'javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(out),str(stub),str(source)],check=True)
subprocess.run([str(j/'java.exe'),'-cp',str(out)+';'+cp,'GoodsScanCheck'],check=True)
