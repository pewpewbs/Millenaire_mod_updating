package net.minecraftforge.registries;
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
