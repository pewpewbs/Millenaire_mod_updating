import org.millenaire.common.utilities.*;import net.minecraft.core.Registry;import net.minecraft.nbt.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.properties.*;import java.nio.file.*;
public class ResourceSourceStateCheck {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static void reject(CompoundTag tag){try{ResourceSourceStateIO.read(tag);throw new AssertionError("Invalid source silently accepted: "+tag);}catch(IllegalArgumentException expected){}}
 public static void main(String[] args)throws Exception{
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();int states=0,ids=0;
  for(var block:Registry.BLOCK)for(var state:block.getStateDefinition().getPossibleStates()){
   var tag=new CompoundTag();tag.putString("block_rl","minecraft:stone");tag.putInt("block_meta",1);tag.putInt("type",1);var points=new ListTag();var point=new CompoundTag();point.putInt("x",-123);points.add(point);tag.put("points",points);
   ResourceSourceStateIO.write(tag,state);check(ResourceSourceStateIO.read(tag).equals(state),"Full state roundtrip: "+state);check(!tag.contains("block_rl")&&!tag.contains("block_meta")&&!tag.contains("type")&&tag.getList("points",10).equals(points),"Old type keys removed; source positions retained");states++;
  }
  for(String line:Files.readAllLines(Path.of(args[0]))){if(line.isBlank()||line.startsWith("#"))continue;var pair=line.split("=");var tag=new CompoundTag();tag.putInt("type",Integer.parseInt(pair[1]));if(pair[0].equals("minecraft:skull")){reject(tag);continue;}check(ResourceSourceStateIO.read(tag).equals(LegacyBlockStates.fromName(pair[0],0)),"Old numeric ID "+line);ids++;}
  var tag=new CompoundTag();tag.putString("block_rl","minecraft:stone");tag.putInt("block_meta",1);check(ResourceSourceStateIO.read(tag).is(Blocks.GRANITE),"Old stone subtype retained");tag.putString("block_rl","minecraft:log");tag.putInt("block_meta",7);var log=ResourceSourceStateIO.read(tag);check(log.is(Blocks.JUNGLE_LOG)&&log.getValue(RotatedPillarBlock.AXIS)==net.minecraft.core.Direction.Axis.X,"Old jungle species and orientation retained");
  tag=new CompoundTag();tag.putInt("type",4095);reject(tag);reject(new CompoundTag());tag=new CompoundTag();var missing=new CompoundTag();missing.putString("Name","missing:resource");tag.put("block_state",missing);reject(tag);
  System.out.println("PASS: resource source persistence: "+states+" vanilla block states round-trip; "+ids+" old vanilla IDs; old named metadata, positions preserved, stale keys removed, missing/unknown IDs rejected. Legacy skull numeric-only records remain unsupported and fail explicitly. Actual codec and Minecraft NBT; no world save loaded.");
 }
}