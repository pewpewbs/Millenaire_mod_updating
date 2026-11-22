exec(open('work/check_fort28.py',encoding='utf-8-sig').read().split("s.write_text(")[0])
o=Path('work/fort29-replay').resolve();o.mkdir(exist_ok=True);s=o/'Replay.java'
s.write_text('''import java.util.*;import java.nio.file.*;import com.google.gson.*;
import net.minecraft.core.*;import net.minecraft.resources.*;import net.minecraft.world.level.*;import net.minecraft.world.level.border.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.*;import net.minecraft.world.level.block.state.properties.*;import net.minecraft.world.level.block.entity.*;import net.minecraft.world.level.material.*;import net.minecraft.world.level.pathfinder.*;import net.minecraft.world.entity.*;import net.minecraft.world.phys.*;import net.minecraft.world.phys.shapes.*;import net.minecraft.util.profiling.*;
class Replay {
 static class Region extends PathNavigationRegion {
  Map<BlockPos,BlockState> blocks;
  Region(){super(null,BlockPos.ZERO,BlockPos.ZERO);}
  public BlockState getBlockState(BlockPos p){return blocks.getOrDefault(p,Blocks.BEDROCK.defaultBlockState());}
  public FluidState getFluidState(BlockPos p){return getBlockState(p).getFluidState();}
  public BlockEntity getBlockEntity(BlockPos p){return null;}
  public BlockGetter getChunkForCollisions(int x,int z){return this;}
  public List<VoxelShape> getEntityCollisions(Entity e,AABB b){return List.of();}
  public WorldBorder getWorldBorder(){return new WorldBorder();}
  public int getMinBuildHeight(){return -64;}public int getHeight(){return 384;}
  public ProfilerFiller getProfiler(){return InactiveProfiler.INSTANCE;}
 }
 static class Walker extends PathfinderMob {
  Walker(){super(EntityType.VILLAGER,null);} public float getPathfindingMalus(BlockPathTypes t){return t==BlockPathTypes.WATER?32:t==BlockPathTypes.DANGER_OTHER?8:t.getMalus();} public void setPathfindingMalus(BlockPathTypes t,float v){} public int getMaxFallDistance(){return 3;} public float getStepHeight(){return 0.6f;} public boolean isDescending(){return false;} public net.minecraft.world.item.ItemStack getItemBySlot(EquipmentSlot slot){return net.minecraft.world.item.ItemStack.EMPTY;}
 }
 static BlockState prop(BlockState b,Property p,String value){Optional v=p.getValue(value);return v.isPresent()?b.setValue(p,(Comparable)v.get()):b;}
 public static void main(String[] args)throws Exception {
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
  var f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);var u=(sun.misc.Unsafe)f.get(null);
  Region region=(Region)u.allocateInstance(Region.class);region.blocks=new HashMap<>();
  var json=JsonParser.parseString(Files.readString(java.nio.file.Path.of("work/fort29-blocks.json"))).getAsJsonObject();
  for(var e:json.entrySet()){String[] xyz=e.getKey().split(",");var v=e.getValue().getAsJsonObject();String name=v.get("Name").getAsString();Block block=Registry.BLOCK.get(new ResourceLocation(name));
   if(name.equals("millenaire:locked_chest"))block=Blocks.CHEST;
   else if(name.startsWith("millenaire:")){if(name.contains("path"))block=Blocks.DIRT_PATH;else block=Blocks.STONE;}
   BlockState state=block.defaultBlockState();if(v.has("Properties"))for(var pr:v.getAsJsonObject("Properties").entrySet()){Property p=block.getStateDefinition().getProperty(pr.getKey());if(p!=null)state=prop(state,p,pr.getValue().getAsString());}
   region.blocks.put(new BlockPos(Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2])),state);
  }
  Walker mob=(Walker)u.allocateInstance(Walker.class);var dim=Entity.class.getDeclaredField("dimensions");dim.setAccessible(true);dim.set(mob,EntityDimensions.scalable(0.6f,1.8f));for(var entry:Map.of("position",(Object)new Vec3(11.5,67,591.5),"blockPosition",new BlockPos(11,67,591),"bb",new AABB(11.2,67,591.2,11.8,68.8,591.8)).entrySet()){var field=Entity.class.getDeclaredField(entry.getKey());field.setAccessible(true);field.set(mob,entry.getValue());}mob.level=(Level)u.allocateInstance(net.minecraft.server.level.ServerLevel.class);var dt=net.minecraft.world.level.dimension.DimensionType.class.getDeclaredField("DEFAULT_OVERWORLD");dt.setAccessible(true);var lf=Level.class.getDeclaredField("dimensionType");lf.setAccessible(true);lf.set(mob.level,dt.get(null));mob.setOnGround(true);mob.maxUpStep=0.6f;
  mob.setPathfindingMalus(BlockPathTypes.WATER,32);mob.setPathfindingMalus(BlockPathTypes.DANGER_OTHER,8);
  var evaluator=new org.millenaire.common.pathing.VillagerWalkNodeEvaluator();evaluator.setCanOpenDoors(true);evaluator.setCanPassDoors(true);evaluator.setCanFloat(true);
  for(BlockPos target:List.of(new BlockPos(2,64,562),new BlockPos(10,67,599))){
   var path=new PathFinder(evaluator,4096).findPath(region,mob,Set.of(target),192,2,1);
   if(path==null||!path.canReach())throw new AssertionError("actual fort route unreachable: "+target);System.out.println("TARGET "+target+" reached="+(path!=null&&path.canReach())+" nodes="+(path==null?0:path.getNodeCount()));if(path!=null)for(int i=0;i<path.getNodeCount();i++)System.out.println(path.getNode(i));
  }
 }
}''',encoding='utf-8')
cp=str(r/'build/classes/java/main')+';'+(r/'build/migration-classpath.txt').read_text().strip()
subprocess.run([str(j/'javac.exe'),'-cp',cp,'-d',str(o),str(s)],check=True)
subprocess.run([str(j/'java.exe'),'-cp',str(o)+';'+cp,'Replay'],check=True)






