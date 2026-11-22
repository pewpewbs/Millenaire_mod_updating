import org.millenaire.common.block.WoodVariant;import net.minecraft.world.level.block.Blocks;import net.minecraft.world.item.Items;
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
}