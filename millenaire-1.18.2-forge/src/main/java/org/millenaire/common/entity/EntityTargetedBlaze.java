package org.millenaire.common.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import org.millenaire.common.utilities.Point;

public class EntityTargetedBlaze
extends Blaze {
    public Point target = null;

    public EntityTargetedBlaze(Level world) { this(MillQuestEntities.BLAZE,world); }
    public EntityTargetedBlaze(EntityType<? extends Blaze> type,Level world) { super(type,world); }
    @Override public net.minecraft.network.protocol.Packet<?> getAddEntityPacket() { return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this); }

    @Override public boolean removeWhenFarAway(double distance) {
        return false;
    }

    private boolean isCourseTraversable(double par1, double par3, double par5, double par7) {
        double d4 = (this.target.x - this.getX()) / par7;
        double d5 = (this.target.y - this.getY()) / par7;
        double d6 = (this.target.z - this.getZ()) / par7;
        AABB axisalignedbb = this.getBoundingBox();
        int i = 1;
        while ((double)i < par7) {
            axisalignedbb = axisalignedbb.move(d4, d5, d6);
            if (!this.level.noCollision(this, axisalignedbb)) {
                return false;
            }
            ++i;
        }
        return true;
    }

    @Override public boolean isSensitiveToWater() { return false; }

    public boolean isInWaterOrRain() {
        return false;
    }

    @Override public void readAdditionalSaveData(CompoundTag par1nbtTagCompound) {
        super.readAdditionalSaveData(par1nbtTagCompound);
        this.target = Point.read(par1nbtTagCompound, "targetPoint");
    }

    @Override public void addAdditionalSaveData(CompoundTag par1nbtTagCompound) {
        super.addAdditionalSaveData(par1nbtTagCompound);
        if (this.target != null) {
            this.target.write(par1nbtTagCompound, "targetPoint");
        }
    }
}

