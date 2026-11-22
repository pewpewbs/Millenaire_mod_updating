package net.minecraft.world.level;
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
}