from pathlib import Path
r=Path(__file__).resolve().parents[1];d=r/'tools/fixtures/villager-inventory';d.mkdir(parents=True,exist_ok=True);s=(r/'src/main/java/org/millenaire/common/entity/MillVillager.java').read_text(encoding='utf-8')
def method(sig):
 a=s.index('    public '+sig);start=s.index('{',a);depth=1;i=start+1
 while depth:
  depth += (s[i]=='{')-(s[i]=='}');i+=1
 return s[a:i]
methods='\n'.join(method(x) for x in ['int countInv(InvItem key)','int takeFromInv(Item item, int meta, int nb)','int takeFromBuilding(Building building, Item item, int meta, int nb)'])
(d/'InventoryHarness.java').write_text('''import net.minecraft.world.item.*;import net.minecraft.world.level.block.*;import java.util.*;
public class InventoryHarness {
 final Map<InvItem,Integer> inventory=new HashMap<>();
 void updateVillagerRecord(){} void updateClothTexturePath(){}
 void addToInv(Item item,int meta,int count){inventory.merge(InvItem.createInvItem(item,meta),count,Integer::sum);}
'''+methods+'''
}
class InvItem {
 final Item item;final Block block;final int meta;
 InvItem(Item item,int meta){this.item=item;this.meta=meta;this.block=Block.byItem(item);}
 Item getItem(){return item;}
 static InvItem createInvItem(Item item,int meta){return new InvItem(item,meta);}
 static InvItem createInvItem(Block block){return new InvItem(block.asItem(),0);}
 public int hashCode(){return item.hashCode()*31+meta;}
 public boolean equals(Object obj){return obj instanceof InvItem i&&i.item==item&&i.meta==meta;}
}
class Building {
 final Map<Item,Integer> goods=new HashMap<>();
 int takeGoods(Item item,int meta,int nb){int take=Math.min(nb,goods.getOrDefault(item,0));goods.put(item,goods.getOrDefault(item,0)-take);return take;}
}
''',encoding='utf-8')
(r/'tools/VillagerInventoryCheck.java').write_text('''import org.millenaire.common.block.WoodVariant;import net.minecraft.world.level.block.Blocks;import net.minecraft.world.item.Items;
public class VillagerInventoryCheck {
 static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
 public static void main(String[] args){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
  var any=InvItem.createInvItem(Blocks.OAK_LOG.asItem(),-1);
  for(int requested:new int[]{0,1,3,4,17,18,19,100}){
   var inv=new InventoryHarness();for(var wood:WoodVariant.values())inv.addToInv(wood.log().asItem(),0,3);
   check(inv.countInv(any)==18,"Count every species once");
   int taken=inv.takeFromInv(Blocks.OAK_LOG.asItem(),-1,requested);
   check(taken==Math.min(requested,18)&&inv.countInv(any)==18-taken,"Bounded wildcard removal "+requested);
   check(inv.inventory.values().stream().allMatch(n->n>=0),"No negative stock");
  }
  var inv=new InventoryHarness();var building=new Building();building.goods.put(Blocks.SPRUCE_LOG.asItem(),3);building.goods.put(Blocks.ACACIA_LOG.asItem(),4);
  check(inv.takeFromBuilding(building,Blocks.OAK_LOG.asItem(),-1,5)==5,"Transfer amount");
  check(inv.countInv(InvItem.createInvItem(Blocks.SPRUCE_LOG))==3&&inv.countInv(InvItem.createInvItem(Blocks.ACACIA_LOG))==2&&inv.countInv(InvItem.createInvItem(Blocks.OAK_LOG))==0,"Keep species");
  check(building.goods.get(Blocks.ACACIA_LOG.asItem())==2,"Source remainder");
  inv.addToInv(Items.STICK,0,7);check(inv.countInv(InvItem.createInvItem(Items.STICK,-1))==7,"Wildcard non-variant item counted once");
  check(inv.takeFromInv(Items.STICK,0,-3)==0&&inv.countInv(InvItem.createInvItem(Items.STICK,0))==7,"Negative request");
  System.out.println("PASS: extracted production inventory methods preserve six log species, cap removals, conserve transfers and handle wildcard non-variant items. Building, inventory keys and record updates are fixtures; gameplay not tested.");
 }
}''',encoding='utf-8')
cp=(r/'build/migration-classpath.txt').read_text().strip();o=r/'build/villager-inventory-check';o.mkdir(exist_ok=True);q=lambda x:'"'+str(x).replace('\\','/')+'"'
a=['-encoding','UTF-8','-cp',cp,'-d',o,d/'InventoryHarness.java',r/'tools/VillagerInventoryCheck.java']+[r/('src/main/java/org/millenaire/'+p+'.java') for p in ['common/block/WoodVariant','common/block/LegacyVariantBlock','common/item/LegacyItemStacks']]
(r/'build/villager-inventory-javac.args').write_text('\n'.join(map(q,a)),encoding='utf-8');(r/'build/villager-inventory-java.args').write_text('\n'.join(map(q,['-cp',str(o)+';'+cp,'VillagerInventoryCheck'])),encoding='utf-8')
