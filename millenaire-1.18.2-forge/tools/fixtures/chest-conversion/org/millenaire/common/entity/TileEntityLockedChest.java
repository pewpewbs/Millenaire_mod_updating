package org.millenaire.common.entity;
public class TileEntityLockedChest extends net.minecraft.world.level.block.entity.ChestBlockEntity {
 public boolean failLoad; public void load(net.minecraft.nbt.CompoundTag tag){if(failLoad)throw new IllegalStateException("injected load failure");saved=tag;} }