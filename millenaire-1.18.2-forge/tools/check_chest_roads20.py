from pathlib import Path
import subprocess
r=Path('C:/Users/sviat/Desktop/mods/millenaire-1.18.2-forge');j=Path('C:/Users/sviat/Desktop/mods/.millenaire-tools/jdk-17.0.20.1+1/bin');o=Path('work/chest-roads20-check').resolve();o.mkdir(exist_ok=True)
s=o/'ChestRoadCheck.java';s.write_text('''import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.pathfinder.*;
import org.millenaire.common.block.*;
import org.millenaire.common.pathing.*;
class ChestRoadCheck {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static class World implements BlockGetter {
  Map<BlockPos,BlockState> blocks=new HashMap<>();
  public BlockEntity getBlockEntity(BlockPos p){return null;}
  public BlockState getBlockState(BlockPos p){return blocks.getOrDefault(p,p.getY()<0?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());}
  public FluidState getFluidState(BlockPos p){return getBlockState(p).getFluidState();}
  public int getHeight(){return 384;}
  public int getMinBuildHeight(){return -64;}
 }
 public static void main(String[] args)throws Exception {
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
  ((net.minecraftforge.registries.ForgeRegistry)net.minecraftforge.registries.ForgeRegistries.BLOCKS).unfreeze();
  var u=Registry.BLOCK.getClass().getMethod("unfreeze");u.setAccessible(true);u.invoke(Registry.BLOCK);
  var locked=new BlockLockedChest("fixture_locked");
  var world=new World();var evaluator=new VillagerWalkNodeEvaluator();var origin=BlockPos.ZERO;
  var chestStates=List.of(Blocks.CHEST.defaultBlockState(),Blocks.TRAPPED_CHEST.defaultBlockState(),Blocks.ENDER_CHEST.defaultBlockState(),locked.defaultBlockState(),Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE,ChestType.LEFT),Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE,ChestType.RIGHT));
  for(var chest:chestStates){
   world.blocks.put(origin,chest);
   check(ChestPathRules.isChest(chest.getBlock()),"shared chest classifier");
   check(evaluator.getBlockPathType(world,0,0,0)==BlockPathTypes.BLOCKED,"cannot enter chest");
   check(evaluator.getBlockPathType(world,0,1,0)==BlockPathTypes.BLOCKED,"cannot step onto lid");
   check(evaluator.getBlockPathType(world,1,0,0)==BlockPathTypes.WALKABLE,"adjacent floor accessible for delivery");
  }
  check(!ChestPathRules.isChest(Blocks.STONE),"ordinary support retained");
  world.blocks.clear();
  for(String name:List.of("dirt","gravel","slabs","sandstone","gravelslabs","ochreslabs")){
   var road=new BlockPath("fixture_"+name,MaterialColor.DIRT,SoundType.GRAVEL);
   var slab=new BlockPathSlab("fixture_"+name,MaterialColor.DIRT,SoundType.GRAVEL);
   for(boolean stable:new boolean[]{false,true}){
    var full=road.defaultBlockState().setValue(IBlockPath.STABLE,stable);
    check(full.getLightBlock(world,origin)==Blocks.DIRT_PATH.defaultBlockState().getLightBlock(world,origin),"road light matches vanilla dirt path");
    check(full.getLightBlock(world,origin)<15&&full.useShapeForLightOcclusion(),"road not opaque cube");
    check(full.getCollisionShape(world,origin).max(Direction.Axis.Y)==15.0/16,"road physical height unchanged");
    for(Half half:Half.values()){
     var state=slab.defaultBlockState().setValue(IBlockPath.STABLE,stable).setValue(BlockPathSlab.HALF,half);
     var vanilla=Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,half==Half.TOP?SlabType.TOP:SlabType.BOTTOM);
     check(state.getLightBlock(world,origin)==vanilla.getLightBlock(world,origin),"slab light matches vanilla slab");
     check(state.getLightBlock(world,origin)<15&&state.useShapeForLightOcclusion(),"slab not opaque cube");
     check(state.getCollisionShape(world,origin).max(Direction.Axis.Y)==(half==Half.TOP?15.0:7.0)/16,"slab height unchanged");
    }
   }
  }
  System.out.println("PASS six chest states blocked at body/lid, adjacent floor walkable; 36 road states vanilla light attenuation and unchanged collision heights");
 }
}''',encoding='utf-8')
cp=str(r/'build/classes/java/main')+';'+(r/'build/migration-classpath.txt').read_text().strip()
subprocess.run([str(j/'javac.exe'),'-cp',cp,'-d',str(o),str(s)],check=True)
subprocess.run([str(j/'java.exe'),'-cp',str(o)+';'+cp,'ChestRoadCheck'],check=True)
