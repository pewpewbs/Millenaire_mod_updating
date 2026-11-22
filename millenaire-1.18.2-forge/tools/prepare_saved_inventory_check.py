from pathlib import Path
r=Path(__file__).resolve().parents[1];q=lambda x:'"'+str(x).replace('\\','/')+'"'
(r/'tools/SavedInventoryCheck.java').write_text('''import org.millenaire.common.utilities.SavedInventoryItem;import net.minecraft.core.Registry;import net.minecraft.nbt.CompoundTag;import net.minecraft.world.item.*;
public class SavedInventoryCheck{
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static CompoundTag old(String item,int meta){var tag=new CompoundTag();tag.putString("itemmod","minecraft");tag.putString("item",item);tag.putInt("meta",meta);tag.putInt("amount",12345);return tag;}
 static void expect(String name,int meta,Item item,int newMeta){var result=SavedInventoryItem.read(old(name,meta));check(result.item()==item&&result.metadata()==newMeta,"Legacy "+name+":"+meta);}
 public static void main(String[] args){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();int count=0;
  for(Item item:Registry.ITEM){for(int meta:new int[]{0,-1}){var tag=old("stone",9);SavedInventoryItem.write(tag,item,meta);var read=SavedInventoryItem.read(tag);check(read.item()==item&&read.metadata()==meta&&tag.getInt("amount")==12345,"Modern identity, wildcard and quantity preserved: "+item);count++;}}
  expect("planks",5,Items.DARK_OAK_PLANKS,0);expect("log",2,Items.BIRCH_LOG,0);expect("log2",1,Items.DARK_OAK_LOG,0);expect("dye",5,Items.PURPLE_DYE,0);expect("dye",1,Items.RED_DYE,0);expect("grass",0,Items.GRASS_BLOCK,0);expect("fish",1,Items.SALMON,0);expect("stone",1,Items.GRANITE,0);expect("iron_sword",37,Items.IRON_SWORD,37);expect("log",-1,Items.OAK_LOG,-1);
  var tag=old("grass",0);SavedInventoryItem.write(tag,Items.GRASS,0);check(SavedInventoryItem.read(tag).item()==Items.GRASS,"Reused modern grass name is not reinterpreted as old grass block");
  tag=old("stone",0);tag.putInt("item_data_version",9999);try{SavedInventoryItem.read(tag);throw new AssertionError("Future version silently accepted");}catch(IllegalArgumentException expected){}
  try{SavedInventoryItem.read(old("not_an_item",0));throw new AssertionError("Unknown item became air");}catch(IllegalArgumentException expected){}
  System.out.println("PASS: "+count+" modern item/wildcard roundtrips; legacy wood, dyes, fish, stone, damage and wildcard; ambiguous grass identity, amount retention, future/unknown guards. Actual item codec; complete world inventories not loaded.");
 }
}''',encoding='utf-8')
cp=(r/'build/migration-classpath.txt').read_text().strip();o=r/'build/saved-inventory-check';o.mkdir(exist_ok=True)
a=['-encoding','UTF-8','-cp',cp,'-d',o,r/'tools/SavedInventoryCheck.java']+[r/'src/main/java/org/millenaire/common'/name for name in ['utilities/SavedInventoryItem.java','item/LegacyItemStacks.java','block/LegacyVariantBlock.java']]
(r/'build/saved-inventory-javac.args').write_text('\n'.join(map(q,a)),encoding='utf-8');(r/'build/saved-inventory-java.args').write_text('\n'.join(map(q,['-cp',str(o)+';'+cp,'SavedInventoryCheck'])),encoding='utf-8')
