from pathlib import Path
r=Path(__file__).resolve().parents[1]
(r/'tools/WallDecorationBlockCheck.java').write_text('''package net.minecraftforge.registries;
import net.minecraft.block.BlockMillWallItem;import net.minecraft.core.*;import net.minecraft.world.item.*;import net.minecraft.world.level.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.world.level.material.Material;import net.minecraft.world.phys.shapes.*;import java.lang.reflect.Proxy;import java.util.*;
public class WallDecorationBlockCheck {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
  var blocks=(ForgeRegistry<Block>)ForgeRegistries.BLOCKS;var items=(ForgeRegistry<Item>)ForgeRegistries.ITEMS;blocks.unfreeze();items.unfreeze();((NamespacedDefaultedWrapper<Block>)Registry.BLOCK).unfreeze();((NamespacedDefaultedWrapper<Item>)Registry.ITEM).unfreeze();
  var block=new BlockMillWallItem("wall_decoration_check",Material.WOOD,SoundType.WOOD);blocks.register(block);var item=new BlockItem(block,new Item.Properties());item.setRegistryName(block.getRegistryName());items.register(item);item.registerBlocks(Item.BY_BLOCK,item);
  Map<BlockPos,BlockState> world=new HashMap<>();
  LevelAccessor view=(LevelAccessor)Proxy.newProxyInstance(LevelAccessor.class.getClassLoader(),new Class[]{LevelAccessor.class},(proxy,method,params)->{
   return switch(method.getName()){
    case "getBlockState" -> world.getOrDefault((BlockPos)params[0],Blocks.AIR.defaultBlockState());
    case "getBlockEntity" -> null;
    case "getFluidState" -> net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState();
    case "getHeight" -> 384;
    case "getMinBuildHeight" -> -64;
    default -> throw new AssertionError("Unexpected world read: "+method.getName());
   };
  });
  var pos=new BlockPos(0,-40,0);
  for(Direction facing:Direction.Plane.HORIZONTAL){
   var state=block.func_176203_a(facing.get3DDataValue());check(state.getValue(BlockMillWallItem.FACING)==facing&&block.func_176201_c(state)==facing.get3DDataValue(),"Facing metadata");
   var box=block.getShape(state,view,pos,CollisionContext.empty()).bounds();check(box.getYsize()==1&&Math.abs(box.getSize()-((1+1+0.1875)/3))<0.000001,"Original thin full-height shape");
   var support=pos.relative(facing.getOpposite());world.clear();world.put(support,Blocks.STONE.defaultBlockState());check(block.canSurvive(state,view,pos),"Solid support");
   check(block.updateShape(state,facing.getOpposite(),Blocks.STONE.defaultBlockState(),view,pos,support)==state,"Supported state retained");
   world.clear();check(!block.canSurvive(state,view,pos),"Missing support");check(block.updateShape(state,facing.getOpposite(),Blocks.AIR.defaultBlockState(),view,pos,support).isAir(),"Support removal removes decoration");
   world.put(support,Blocks.REDSTONE_BLOCK.defaultBlockState());check(!block.canSurvive(state,view,pos),"Signal sources cannot support original decoration");
   check(block.rotate(state,Rotation.CLOCKWISE_90).getValue(BlockMillWallItem.FACING)==facing.getClockWise(),"Rotation");check(block.mirror(block.mirror(state,Mirror.LEFT_RIGHT),Mirror.LEFT_RIGHT)==state,"Mirror roundtrip");
   check(block.getDrops(state,null).get(0).is(item)&&block.itemMetadata(state)==0,"Orientation-independent item drop");check(block.isLadder(state,view,pos,null),"Climbable");
  }
  check(block.func_176203_a(0).getValue(BlockMillWallItem.FACING)==Direction.NORTH,"Vertical metadata fallback");
  System.out.println("PASS: wall decoration block shape, all facings, metadata, rotation/mirroring, support/removal, redstone restriction, climbing flag and drops; world reads use a fixture. Placement in a live world and registration in the mod remain untested (no current production instances found).");
 }
}''',encoding='utf-8')
cp=(r/'build/migration-classpath.txt').read_text().strip();o=r/'build/wall-decoration-block-check';o.mkdir(exist_ok=True);q=lambda x:'"'+str(x).replace('\\','/')+'"'
a=['-encoding','UTF-8','-cp',cp,'-d',o,r/'tools/WallDecorationBlockCheck.java',r/'src/main/java/net/minecraft/block/BlockMillWallItem.java',r/'src/main/java/org/millenaire/common/block/LegacyVariantBlock.java']
(r/'build/wall-decoration-block-javac.args').write_text('\n'.join(map(q,a)),encoding='utf-8');(r/'build/wall-decoration-block-java.args').write_text('\n'.join(map(q,['-cp',str(o)+';'+cp,'net.minecraftforge.registries.WallDecorationBlockCheck'])),encoding='utf-8')
