from pathlib import Path
root=Path(__file__).resolve().parents[1];src=root/'src/main/java/org/millenaire';fixtures=root/'tools/fixtures/parchment';fixtures.mkdir(parents=True,exist_ok=True)
files={
'common/block/MillBlocks':'public class MillBlocks { public static final net.minecraft.world.item.CreativeModeTab tabMillenaire=net.minecraft.world.item.CreativeModeTab.TAB_MISC; }',
'common/item/MillItems':'public class MillItems { public static net.minecraft.world.item.Item PARCHMENT_VILLAGE_SCROLL; }',
'common/forge/Mill':'public class Mill { public static org.millenaire.common.world.MillWorldData clientWorld; public static org.millenaire.common.world.MillWorldData getMillWorld(net.minecraft.world.level.Level level){return clientWorld;} }',
'common/world/MillWorldData':'public class MillWorldData { public org.millenaire.common.village.Building building; public org.millenaire.common.village.Building getBuilding(org.millenaire.common.utilities.Point pos){return building;} }',
'common/village/Building':'public class Building { public boolean isActive=true; public org.millenaire.common.utilities.Point getPos(){return new org.millenaire.common.utilities.Point(37,-42,99);} public String getVillageQualifiedName(){return "Village test";} }',
'common/utilities/Point':'''public class Point { public final double x,y,z; public Point(double x,double y,double z){this.x=x;this.y=y;this.z=z;} public void write(net.minecraft.nbt.CompoundTag tag,String key){tag.putDouble(key+"_xCoord",x);tag.putDouble(key+"_yCoord",y);tag.putDouble(key+"_zCoord",z);} public static Point read(net.minecraft.nbt.CompoundTag tag,String key){return tag.contains(key+"_xCoord")?new Point(tag.getDouble(key+"_xCoord"),tag.getDouble(key+"_yCoord"),tag.getDouble(key+"_zCoord")):null;} public int getChunkX(){return ((int)x)>>4;} public int getChunkZ(){return ((int)z)>>4;} }''',
'common/network/ServerSender':'public class ServerSender { public static void sendTranslatedSentence(net.minecraft.world.entity.player.Player player,char colour,String key,String... args){} public static void displayVillageBookGUI(net.minecraft.world.entity.player.Player player,org.millenaire.common.utilities.Point pos){} }',
'client/book/ParchmentClient':'public class ParchmentClient { public static void open(net.minecraft.world.entity.player.Player player,int[] ids){throw new UnsupportedOperationException("Screen not tested here");} }'
}
for path,body in files.items():
 f=fixtures/(path+'.java');f.parent.mkdir(parents=True,exist_ok=True);f.write_text('package org.millenaire.'+path.rsplit('/',1)[0].replace('/','.')+';\n'+body,encoding='utf-8')
(root/'tools/ParchmentItemCheck.java').write_text('''package net.minecraftforge.registries;
import net.minecraft.SharedConstants;import net.minecraft.server.Bootstrap;import net.minecraft.core.*;import net.minecraft.world.item.*;import net.minecraft.nbt.CompoundTag;import org.millenaire.common.item.*;
public class ParchmentItemCheck {
 public static void main(String[] args){
  SharedConstants.tryDetectVersion();Bootstrap.bootStrap();((ForgeRegistry<Item>)ForgeRegistries.ITEMS).unfreeze();((NamespacedDefaultedWrapper<Item>)Registry.ITEM).unfreeze();
  var book=new ItemParchment("parchment_test",ItemParchment.VILLAGE_BOOK,false);ForgeRegistries.ITEMS.register(book);MillItems.PARCHMENT_VILLAGE_SCROLL=book;
  var empty=new ItemStack(book);if(empty.getMaxStackSize()!=1 || empty.getHoverName()==null)throw new AssertionError("Unbound scroll must be named and nonstacking");
  var stack=ItemParchment.createParchmentForVillage(new org.millenaire.common.village.Building());var restored=ItemStack.of(stack.save(new CompoundTag()));
  if(!restored.is(book) || restored.getTag().getDouble("village_pos__yCoord")!=-42)throw new AssertionError("Village scroll coordinates lost on save/load");
  org.millenaire.common.forge.Mill.clientWorld=new org.millenaire.common.world.MillWorldData();org.millenaire.common.forge.Mill.clientWorld.building=new org.millenaire.common.village.Building();
  if(!restored.getHoverName().getString().contains("Village test"))throw new AssertionError("Village name missing");
  var hidden=new ItemParchment("obsolete_scroll_test",1,true);var items=NonNullList.<ItemStack>create();hidden.fillItemCategory(CreativeModeTab.TAB_MISC,items);if(!items.isEmpty())throw new AssertionError("Obsolete scroll visible");
  book.fillItemCategory(CreativeModeTab.TAB_MISC,items);if(items.size()!=1)throw new AssertionError("Village scroll missing from creative tab");
  System.out.println("PASS: parchment stack size, unbound name, village NBT and title, obsolete creative visibility; world use and screens not tested");
 }
}
''',encoding='utf-8')
cp=(root/'build/migration-classpath.txt').read_text(encoding='utf-8').strip();out=root/'build/parchment-check';out.mkdir(exist_ok=True);q=lambda s:'"'+str(s).replace('\\','/')+'"'
args=['-encoding','UTF-8','-classpath',cp,'-d',out,src/'common/item/ItemMill.java',src/'common/item/ItemParchment.java']+list(fixtures.rglob('*.java'))+[root/'tools/ParchmentItemCheck.java']
(root/'build/parchment-check-javac.args').write_text('\n'.join(q(v) for v in args),encoding='utf-8');(root/'build/parchment-check-java.args').write_text('\n'.join(q(v) for v in ['-cp',str(out)+';'+cp,'net.minecraftforge.registries.ParchmentItemCheck']),encoding='utf-8')
