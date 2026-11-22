package net.minecraft.world.level.block.entity;
import net.minecraft.nbt.CompoundTag;
public class ChestBlockEntity { public CompoundTag saved=new CompoundTag(); public boolean changed,removed;
 public CompoundTag saveWithoutMetadata(){var copy=new CompoundTag();copy.putAll(saved);return copy;}
 public void clearRemoved(){removed=false;} public void setChanged(){changed=true;} }