from pathlib import Path
root=Path(__file__).resolve().parents[1];src=root/'src/main/java/org/millenaire';fixtures=root/'tools/fixtures/decoration';fixtures.mkdir(parents=True,exist_ok=True)
files={
'common/block/MillBlocks':'public class MillBlocks { public static final net.minecraft.world.item.CreativeModeTab tabMillenaire=net.minecraft.world.item.CreativeModeTab.TAB_MISC; }',
'common/item/MillItems':'public class MillItems { public static final net.minecraft.world.item.Item TAPESTRY=net.minecraft.world.item.Items.PAINTING,INDIAN_STATUE=TAPESTRY,MAYAN_STATUE=TAPESTRY,BYZANTINE_ICON_SMALL=TAPESTRY,BYZANTINE_ICON_MEDIUM=TAPESTRY,BYZANTINE_ICON_LARGE=TAPESTRY,HIDEHANGING=TAPESTRY,WALLCARPETSMALL=TAPESTRY,WALLCARPETMEDIUM=TAPESTRY,WALLCARPETLARGE=TAPESTRY; }',
'common/config/MillConfigValues':'public class MillConfigValues { public static int LogBuildingPlan=0; }',
'common/utilities/MillCommonUtilities':'public class MillCommonUtilities { public interface WeightedChoice { int getChoiceWeight(net.minecraft.world.entity.player.Player player); } public static <T extends WeightedChoice> T getWeightedChoice(java.util.List<T> list,net.minecraft.world.entity.player.Player player){throw new UnsupportedOperationException("Placement not tested");} }',
'common/utilities/MillLog':'public class MillLog { public static void major(Object o,String text){} public static void error(Object o,String text){throw new AssertionError(text);} }',
'common/utilities/Point':'public class Point { public net.minecraft.core.BlockPos getBlockPos(){return net.minecraft.core.BlockPos.ZERO;} public Point getNorth(){return this;} public Point getSouth(){return this;} public Point getEast(){return this;} public Point getWest(){return this;} }',
'common/utilities/WorldUtilities':'public class WorldUtilities { public static net.minecraft.world.level.block.Block getBlock(net.minecraft.world.level.Level level,Point p){throw new UnsupportedOperationException("World not tested");} }',
'common/utilities/BlockItemUtilities':'public class BlockItemUtilities { public static boolean isBlockSolid(net.minecraft.world.level.block.Block b){return true;} }',
'common/network/StreamReadWrite':'public class StreamReadWrite {}'
}
for path,body in files.items():
 f=fixtures/(path+'.java');f.parent.mkdir(parents=True,exist_ok=True);f.write_text('package org.millenaire.'+path.rsplit('/',1)[0].replace('/','.')+';\n'+body,encoding='utf-8')
(root/'tools/DecorationStateCheck.java').write_text('''package net.minecraftforge.registries;
import net.minecraft.SharedConstants;import net.minecraft.server.Bootstrap;import net.minecraft.core.*;import net.minecraft.world.entity.*;import net.minecraft.nbt.CompoundTag;import net.minecraft.network.FriendlyByteBuf;import io.netty.buffer.Unpooled;import org.millenaire.common.entity.*;
public class DecorationStateCheck {
 public static void main(String[] args){
  SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
  ((ForgeRegistry<EntityType<?>>)ForgeRegistries.ENTITIES).unfreeze();((NamespacedDefaultedWrapper<EntityType<?>>)Registry.ENTITY_TYPE).unfreeze();
  MillDecorationEntities.register(new net.minecraftforge.event.RegistryEvent.Register<>(ForgeRegistries.ENTITIES.getRegistryName(),ForgeRegistries.ENTITIES));
  int checks=0;
  for(var art:EntityWallDecoration.EnumWallDecoration.values())for(Direction facing:Direction.Plane.HORIZONTAL){
   var entity=new EntityWallDecoration(MillDecorationEntities.WALL_DECORATION,null);
   var tag=new CompoundTag();tag.putString("Motive",art.title);tag.putInt("Type",art.type);tag.putInt("TileX",-125);tag.putInt("TileY",-48);tag.putInt("TileZ",300);tag.putByte("Facing",(byte)facing.get2DDataValue());
   entity.readAdditionalSaveData(tag);
   var saved=new CompoundTag();entity.addAdditionalSaveData(saved);
   if(!saved.equals(tag))throw new AssertionError("NBT roundtrip "+art.title+" "+saved);
   var client=new EntityWallDecoration(MillDecorationEntities.WALL_DECORATION,null);var data=new FriendlyByteBuf(Unpooled.buffer());
   try {entity.writeSpawnData(data);client.readSpawnData(data);if(data.isReadable())throw new AssertionError("Unread bytes");}finally{data.release();}
   if(client.millArt!=art || client.type!=art.type || !client.getPos().equals(entity.getPos()) || client.getDirection()!=facing || !client.getBoundingBox().equals(entity.getBoundingBox()))throw new AssertionError("Spawn state "+art.title);
   if(client.getWidth()!=art.sizeX || client.getHeight()!=art.sizeY || client.getPickResult().isEmpty())throw new AssertionError("Dimensions/drop");
   if(art.sizeX%16!=0 || art.sizeY%16!=0 || art.offsetX+art.sizeX>256 || art.offsetY+art.sizeY>256 || art.getChoiceWeight(null)<=0)throw new AssertionError("Art atlas/weight");
   checks++;
  }
  for(int type=1;type<=10;type++)if(EntityWallDecoration.findArt("missing",type).type!=type)throw new AssertionError("Fallback type");
  if(EntityWallDecoration.findArt("missing",999)!=EntityWallDecoration.EnumWallDecoration.Griffon)throw new AssertionError("Unknown fallback");
  System.out.println("PASS: "+checks+" art/facing NBT and spawn roundtrips, negative-height anchors, bounding boxes, dimensions, atlas bounds, weights and fallback. World placement/rendering not tested; item drops are fixtures.");
 }
}
''',encoding='utf-8')
cp=(root/'build/migration-classpath.txt').read_text(encoding='utf-8').strip();out=root/'build/decoration-check';out.mkdir(exist_ok=True);q=lambda s:'"'+str(s).replace('\\','/')+'"'
args=['-encoding','UTF-8','-classpath',cp,'-d',out]+[src/p for p in ['common/entity/EntityWallDecoration.java','common/entity/MillDecorationEntities.java','common/item/ItemMill.java','common/item/ItemWallDecoration.java','client/render/RenderWallDecoration.java']]+list(fixtures.rglob('*.java'))+[root/'tools/DecorationStateCheck.java']
(root/'build/decoration-check-javac.args').write_text('\n'.join(q(v) for v in args),encoding='utf-8');(root/'build/decoration-check-java.args').write_text('\n'.join(q(v) for v in ['-cp',str(out)+';'+cp,'net.minecraftforge.registries.DecorationStateCheck']),encoding='utf-8')
