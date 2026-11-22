from pathlib import Path
r=Path(__file__).resolve().parents[1]
(r/'tools/LegacyBlockStatesCheck.java').write_text('''import org.millenaire.common.utilities.LegacyBlockStates;import net.minecraft.core.Registry;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.properties.*;import net.minecraft.core.Direction;import java.nio.file.*;
public class LegacyBlockStatesCheck {
 static void roundtrip(net.minecraft.world.level.block.state.BlockState state){String encoded=org.millenaire.common.utilities.BlockStateUtilities.getStringFromBlockState(state);String[] parts=encoded.split(";",-1);check(LegacyBlockStates.fromName(parts[0],parts[1]).equals(state),"Modern state roundtrip: "+encoded);}
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception {
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
  check(LegacyBlockStates.fromName("minecraft:planks",5).is(Blocks.DARK_OAK_PLANKS),"Dark oak planks");
  var log=LegacyBlockStates.fromName("minecraft:log",6);check(log.is(Blocks.BIRCH_LOG)&&log.getValue(RotatedPillarBlock.AXIS)==Direction.Axis.X,"Birch log axis");
  check(LegacyBlockStates.fromName("minecraft:log2",13).is(Blocks.DARK_OAK_WOOD),"Bark on every side");
  check(LegacyBlockStates.fromName("minecraft:stone_slab",8).getValue(SlabBlock.TYPE)==SlabType.TOP,"Top slab");
  check(LegacyBlockStates.fromName("minecraft:double_stone_slab",0).getValue(SlabBlock.TYPE)==SlabType.DOUBLE,"Double slab identity retained");
  check(LegacyBlockStates.fromName("minecraft:grass_path",0).is(Blocks.DIRT_PATH),"Later datafix rename");
  check(LegacyBlockStates.fromName("minecraft:stone",1).is(Blocks.GRANITE),"Stone variant");
  check(LegacyBlockStates.fromName("minecraft:flowing_water",3).getValue(LiquidBlock.LEVEL)==3,"Fluid level");
  check(LegacyBlockStates.fromName("minecraft:flower_pot",255).is(Blocks.FLOWER_POT),"Special empty pot");
  check(LegacyBlockStates.fromName("minecraft:planks","variant=dark_oak").is(Blocks.DARK_OAK_PLANKS),"Named wood variant");
  var door=LegacyBlockStates.fromName("minecraft:wooden_door","facing=north,half=lower,hinge=right,open=true");check(door.getValue(DoorBlock.HINGE)==DoorHingeSide.RIGHT&&door.getValue(DoorBlock.FACING)==Direction.NORTH&&door.getValue(DoorBlock.OPEN),"Door properties absent from lower-half metadata");
  for(String wood:new String[]{"oak","spruce","birch","jungle","acacia","dark_oak"})check(LegacyBlockStates.matchesName(Registry.BLOCK.get(new net.minecraft.resources.ResourceLocation(wood+"_planks")),"minecraft:planks"),"Configured plank family: "+wood);
  check(LegacyBlockStates.matchesName(Blocks.BRICKS,"minecraft:brick_block"),"Legacy brick exclusion");
  check(!LegacyBlockStates.matchesName(Blocks.OAK_LOG,"minecraft:planks"),"Logs are not planks");
  check(!LegacyBlockStates.matchesName(Blocks.AIR,"othermod:planks"),"No unknown namespace alias");
  int costs=0;
  int named=0;
  int checked=0;
  for(String line:Files.readAllLines(Path.of(args[0]))) {
   if(line.isBlank()||line.startsWith("//"))continue;String[] parts=line.split(";",-1);
   if(parts.length>=8&&parts[5].startsWith("minecraft:")&&!parts[7].equals("0")&&LegacyBlockStates.containsName(parts[5])){
    var cost=LegacyBlockStates.fromName(parts[5],parts[6]);check(cost.getBlock().asItem()!=net.minecraft.world.item.Items.AIR,"Vanilla block cost has no item: "+line);costs++;
   }
   if(parts.length<3||!parts[1].startsWith("minecraft:"))continue;
   if(!parts[2].matches("[0-9]+")){var state=LegacyBlockStates.fromName(parts[1],parts[2]);check(!state.isAir()||parts[1].equals("minecraft:air"),"Named entry became air: "+line);roundtrip(state);named++;continue;}
   var state=LegacyBlockStates.fromName(parts[1],Integer.parseInt(parts[2]));
   check(!state.isAir()||parts[1].equals("minecraft:air"),"Unexpected air for "+line);roundtrip(state);checked++;
  }
  try{LegacyBlockStates.fromName("minecraft:missing_block",0);throw new AssertionError("Unknown block silently became air");}catch(IllegalArgumentException expected){}
  System.out.println("PASS: "+checked+" numeric and "+named+" named vanilla palette entries, "+costs+" block costs; wood species/axis/bark, upper/double slab, fluid level, later renames and unknown-block guard. Live construction still untested; custom block helpers use a logging fixture.");
 }
}''',encoding='utf-8')
cp=(r/'build/migration-classpath.txt').read_text().strip();o=r/'build/legacy-block-check';q=lambda x:'"'+str(x).replace('\\','/')+'"'
a=['-encoding','UTF-8','-cp',cp,'-d',o,r/'tools/LegacyBlockStatesCheck.java']+[r/('src/main/java/org/millenaire/common/'+n+'.java') for n in ['utilities/LegacyBlockStates','utilities/LegacyFlowerPots','block/LegacyVariantBlock','block/WoodVariant','utilities/BlockStateUtilities']]
f=r/'tools/fixtures/legacy-block';f.mkdir(parents=True,exist_ok=True);fixture=f/'MillLog.java';fixture.write_text('package org.millenaire.common.utilities;public class MillLog{public static void error(Object o,String s){throw new AssertionError(s);}}',encoding='utf-8');a.append(fixture)
(r/'build/legacy-block-javac.args').write_text('\n'.join(map(q,a)),encoding='utf-8')
(r/'build/legacy-block-java.args').write_text('\n'.join(map(q,['-cp',str(o)+';'+str(r/'src/main/resources')+';'+cp,'LegacyBlockStatesCheck',r/'src/main/resources/todeploy/millenaire/blocklist.txt'])),encoding='utf-8')
