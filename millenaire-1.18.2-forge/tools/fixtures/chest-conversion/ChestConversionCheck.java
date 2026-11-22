import net.minecraft.core.*; import net.minecraft.world.level.*; import net.minecraft.world.level.block.*;
import org.millenaire.common.utilities.ChestConversion;
public class ChestConversionCheck {
 static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 static Level world(){var w=new Level(); w.entity.saved.put("Items", "27 slots with damaged, named and tagged stacks");
  w.entity.saved.put("CustomName","Stored goods");w.entity.saved.put("LootTable","minecraft:chests/test");w.entity.saved.put("LootTableSeed",12345L);return w;}
 public static void main(String[] args){var pos=new BlockPos(1,67,2); var w=world();var original=w.entity;var nbt=original.saveWithoutMetadata();
  check(ChestConversion.convert(w,pos),"success");check(w.drops==0,"no duplicate drops");
  check(w.entity.saved.equals(nbt),"all NBT fields transferred");check(w.entity.changed,"new entity marked dirty");
  check(w.state.facing.equals("west"),"facing preserved");
  check(!ChestConversion.convert(w,pos),"already converted is not duplicated");
  for(int mode=0;mode<4;mode++){w=world();original=w.entity;var state=w.state;
   w.reject=mode==0;w.missingEntity=mode==1;w.failLoad=mode==2;w.failSet=mode==3;
   try {check(!ChestConversion.convert(w,pos),"failure not reported as success");check(mode<2,"exception expected");}
   catch(IllegalStateException e){check(mode>=2,"unexpected exception");}
   check(w.entity==original&&!original.removed&&original.changed,"original entity restored");
   check(w.state==state&&w.entity.saved.equals(nbt),"original state and NBT restored");check(w.drops==0,"rollback has no drops");
  }
  for(int mode=0;mode<4;mode++){w=world();w.isClientSide=mode==0;w.unloaded=mode==1;
   if(mode==2)w.state=new net.minecraft.world.level.block.state.BlockState(new Object());if(mode==3)w.entity=null;
   check(!ChestConversion.convert(w,pos)&&w.mutations==0,"invalid input untouched");}
  System.out.println("PASS: actual ChestConversion with world/container fixtures: success, complete NBT transfer, facing, no drops, repeated conversion, four rollback paths and four input guards. Actual in-world chest conversion remains untested.");
 }
}