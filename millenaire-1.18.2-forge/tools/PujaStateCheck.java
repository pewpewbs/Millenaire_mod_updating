import org.millenaire.common.ui.PujaSacrifice;import org.millenaire.common.village.Building;import org.millenaire.common.item.MillItems;import org.millenaire.common.utilities.MillCommonUtilities;import net.minecraft.world.item.*;import net.minecraft.world.item.enchantment.*;import net.minecraft.nbt.*;
public class PujaStateCheck {
 static void check(boolean x,String why){if(!x)throw new AssertionError(why);}
 static PujaSacrifice shrine(){var p=new PujaSacrifice(new Building(),(short)0);p.setItem(4,new ItemStack(Items.DIAMOND_PICKAXE));p.changeEnchantment(0);p.setItem(0,new ItemStack(Items.DIAMOND,5));return p;}
 static void complete(PujaSacrifice p){check(p.performPuja(null),"Start");for(int i=0;i<PujaSacrifice.PUJA_DURATION;i++)p.performPuja(null);check(p.pujaProgress==0,"Completion duration");}
 public static void main(String[] args){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
  var p=shrine();p.setItem(1,new ItemStack(MillItems.DENIER,7));check(!p.performPuja(null)&&MillCommonUtilities.countMoney(p)==7,"Insufficient payment must not be spent");
  p.setItem(1,new ItemStack(MillItems.DENIER,24));complete(p);check(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY,p.getItem(4))==1,"First enchantment");check(MillCommonUtilities.countMoney(p)==16&&p.getItem(0).getCount()==4,"Exact fee and offering");
  complete(p);check(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY,p.getItem(4))==2,"Upgrade existing string-id enchantment");check(MillCommonUtilities.countMoney(p)==8,"Second fee");
  var saved=new CompoundTag();p.offeringProgress=80000;p.writeToNBT(saved);var restored=new PujaSacrifice(new Building(),saved);check(restored.currentTarget.enchantment==Enchantments.BLOCK_EFFICIENCY&&restored.offeringProgress==80000,"Named target and full int progress");for(int i=0;i<5;i++)check(restored.getItem(i)!=null,"No null inventory slots");
  var empty=new PujaSacrifice(new Building(),new CompoundTag());check(empty.isEmpty()&&empty.getDisplayName()!=null,"Empty saved inventory");
  saved.putShort("enchantmentTarget",(short)32);restored=new PujaSacrifice(new Building(),saved);check(restored.currentTarget.enchantment==Enchantments.BLOCK_EFFICIENCY,"Legacy target id");restored.changeEnchantment(-1);restored.changeEnchantment(999);
  var mayan=new PujaSacrifice(new Building(),(short)1);for(var item:new Item[]{Items.SKELETON_SKULL,Items.WITHER_SKELETON_SKULL,Items.ZOMBIE_HEAD,Items.PLAYER_HEAD,Items.CREEPER_HEAD,Items.DRAGON_HEAD})check(mayan.getOfferingValue(new ItemStack(item))==4096,"Skull variants");for(var item:new Item[]{Items.COD,Items.SALMON,Items.TROPICAL_FISH,Items.PUFFERFISH})check(mayan.getOfferingValue(new ItemStack(item))==1,"Fish variants");check(mayan.getOfferingValue(new ItemStack(Items.INK_SAC))==1,"Ink offering");
  restored.clearContent();restored.writeToNBT(saved);check(restored.isEmpty()&&!saved.contains("enchantmentTarget"),"Clear removes stale target");
  var packet=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
  try {
   p.offeringNeeded=90000;p.offeringProgress=80000;PujaPacketHarness.writeNullablePuja(p,packet);
   var remote=PujaPacketHarness.readOrUpdateNullablePuja(packet,new Building(),null);
   check(remote.offeringNeeded==90000&&remote.offeringProgress==80000&&remote.currentTarget.enchantment==Enchantments.BLOCK_EFFICIENCY&&!packet.isReadable(),"Full progress and target before inventory sync");
   p.currentTarget=null;PujaPacketHarness.writeNullablePuja(p,packet);PujaPacketHarness.readOrUpdateNullablePuja(packet,remote.temple,remote);check(remote.currentTarget==null,"Explicit no-target sentinel clears old target");
   PujaPacketHarness.writeNullablePuja(null,packet);check(PujaPacketHarness.readOrUpdateNullablePuja(packet,new Building(),remote)==null&&!packet.isReadable(),"Null shrine roundtrip");
  } finally {packet.release();}
  System.out.println("PASS: actual ritual class: insufficient/exact fees, two enchantment levels, offering consumption, sparse NBT, named/legacy targets, 32-bit progress and packet roundtrips, invalid selections and flattened fish/skull variants. Currency items, building saves and services are fixtures; menu/gameplay not tested.");
 }
}