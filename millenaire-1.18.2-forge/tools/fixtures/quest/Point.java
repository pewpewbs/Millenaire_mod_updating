package org.millenaire.common.utilities;
public class Point {
    public double x,y,z;
    public double distanceTo(net.minecraft.world.entity.Entity entity){return Math.sqrt(entity.distanceToSqr(x,y,z));}
    public static Point read(net.minecraft.nbt.CompoundTag tag,String key){return null;}
    public void write(net.minecraft.nbt.CompoundTag tag,String key){}
}
