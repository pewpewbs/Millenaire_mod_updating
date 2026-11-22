package net.minecraft.world.level.block.state;
public class BlockState { public Object block; public Object facing; public BlockState(Object b){block=b;}
 public boolean is(Object b){return block==b;} public Object getValue(Object key){return facing;}
 public BlockState setValue(Object key,Object value){facing=value;return this;} }