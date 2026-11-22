package net.minecraftforge.registries;
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
