from pathlib import Path
import subprocess

root = Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge')
fixture = root/'tools/fixtures/chest-conversion'
sources = {
'net/minecraft/core/BlockPos.java': 'package net.minecraft.core; public record BlockPos(int x,int y,int z) {}',
'net/minecraft/nbt/CompoundTag.java': 'package net.minecraft.nbt; public class CompoundTag extends java.util.HashMap<String,Object> {}',
'net/minecraft/world/level/block/Blocks.java': 'package net.minecraft.world.level.block; public class Blocks { public static final Object CHEST=new Object(); }',
'net/minecraft/world/level/block/ChestBlock.java': 'package net.minecraft.world.level.block; public class ChestBlock { public static final String FACING="facing"; }',
'net/minecraft/world/level/block/state/BlockState.java': '''package net.minecraft.world.level.block.state;
public class BlockState { public Object block; public Object facing; public BlockState(Object b){block=b;}
 public boolean is(Object b){return block==b;} public Object getValue(Object key){return facing;}
 public BlockState setValue(Object key,Object value){facing=value;return this;} }''',
'net/minecraft/world/level/block/entity/ChestBlockEntity.java': '''package net.minecraft.world.level.block.entity;
import net.minecraft.nbt.CompoundTag;
public class ChestBlockEntity { public CompoundTag saved=new CompoundTag(); public boolean changed,removed;
 public CompoundTag saveWithoutMetadata(){var copy=new CompoundTag();copy.putAll(saved);return copy;}
 public void clearRemoved(){removed=false;} public void setChanged(){changed=true;} }''',
'org/millenaire/common/block/BlockLockedChest.java': '''package org.millenaire.common.block;
public class BlockLockedChest { public static final String FACING="facing";
 public net.minecraft.world.level.block.state.BlockState defaultBlockState(){return new net.minecraft.world.level.block.state.BlockState(this);} }''',
'org/millenaire/common/block/MillBlocks.java': 'package org.millenaire.common.block; public class MillBlocks {public static final BlockLockedChest LOCKED_CHEST=new BlockLockedChest();}',
'org/millenaire/common/entity/TileEntityLockedChest.java': '''package org.millenaire.common.entity;
public class TileEntityLockedChest extends net.minecraft.world.level.block.entity.ChestBlockEntity {
 public boolean failLoad; public void load(net.minecraft.nbt.CompoundTag tag){if(failLoad)throw new IllegalStateException("injected load failure");saved=tag;} }''',
'net/minecraft/world/level/Level.java': '''package net.minecraft.world.level;
import net.minecraft.core.BlockPos; import net.minecraft.world.level.block.*; import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.entity.*; import org.millenaire.common.entity.*;
public class Level {
 public boolean isClientSide, unloaded, reject, missingEntity, failLoad, failSet; public int drops,mutations;
 public BlockState state=new BlockState(Blocks.CHEST).setValue(ChestBlock.FACING,"west");
 public ChestBlockEntity entity=new ChestBlockEntity();
 public boolean hasChunkAt(BlockPos p){return !unloaded;}
 public BlockState getBlockState(BlockPos p){return state;}
 public Object getBlockEntity(BlockPos p){return entity;}
 public void removeBlockEntity(BlockPos p){if(entity!=null)entity.removed=true;entity=null;mutations++;}
 public boolean setBlock(BlockPos p,BlockState next,int flags){
   boolean replacing=!next.is(Blocks.CHEST);
   if(replacing&&failSet)throw new IllegalStateException("injected set failure");
   if(replacing&&reject)return false;
   if(state.is(Blocks.CHEST)&&entity!=null)drops++;
   state=next; mutations++;
   if(replacing&&!missingEntity){var target=new TileEntityLockedChest();target.failLoad=failLoad;entity=target;}
   else entity=null;
   return true;
 }
 public void setBlockEntity(ChestBlockEntity original){entity=original;mutations++;}
}''',
'ChestConversionCheck.java': '''import net.minecraft.core.*; import net.minecraft.world.level.*; import net.minecraft.world.level.block.*;
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
}'''
}
for name, text in sources.items():
    path=fixture/name; path.parent.mkdir(parents=True, exist_ok=True); path.write_text(text,encoding='utf-8')
jdk=Path('C:/Users/sviat/Desktop/mods/.millenaire-tools/jdk-17.0.20.1+1/bin')
out=root/'build/chest-conversion-check';out.mkdir(exist_ok=True)
subprocess.run([str(jdk/'javac.exe'),'-encoding','UTF-8','-d',str(out),
    str(root/'src/main/java/org/millenaire/common/utilities/ChestConversion.java')]+[str(fixture/n) for n in sources],check=True)
subprocess.run([str(jdk/'java.exe'),'-cp',str(out),'ChestConversionCheck'],check=True)
