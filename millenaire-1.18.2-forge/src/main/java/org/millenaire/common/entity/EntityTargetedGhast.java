package org.millenaire.common.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.millenaire.common.utilities.Point;

public class EntityTargetedGhast
extends Ghast {
    public Point target = null;

    public EntityTargetedGhast(Level world) { this(MillQuestEntities.GHAST,world); }
    public EntityTargetedGhast(EntityType<? extends Ghast> type,Level world) { super(type,world); }
    @Override public net.minecraft.network.protocol.Packet<?> getAddEntityPacket() { return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this); }

    @Override public boolean removeWhenFarAway(double distance) {
        return false;
    }

    public void tick() {
        if (!this.level.isClientSide && this.target != null) {
            if (this.target.distanceTo((Entity)this) > 20.0) {
                this.getMoveControl().setWantedPosition(this.target.x, this.target.y, this.target.z, (double)this.getSpeed());
            } else if (this.target.distanceTo((Entity)this) < 10.0) {
                this.getMoveControl().setWantedPosition(this.target.x + (double)((this.random.nextFloat() * 2.0f - 1.0f) * 16.0f), this.target.y + (double)((this.random.nextFloat() * 2.0f - 1.0f) * 16.0f), this.target.z + (double)((this.random.nextFloat() * 2.0f - 1.0f) * 16.0f), (double)this.getSpeed());
            }
        }
        super.tick();
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

