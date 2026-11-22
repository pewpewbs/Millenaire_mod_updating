/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLiving
 *  net.minecraft.init.Blocks
 *  net.minecraft.pathfinding.PathFinder
 *  net.minecraft.pathfinding.PathNavigate
 *  net.minecraft.pathfinding.PathNodeType
 *  net.minecraft.pathfinding.WalkNodeProcessor
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.World
 */
package org.millenaire.common.entity;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

public class PathNavigateSimple
extends PathNavigation {
    public PathNavigateSimple(Mob entityIn, Level worldIn) {
        super(entityIn, worldIn);
    }

    protected boolean canUpdatePath() {
        return true;
    }

    private boolean getCanSwim() {
        return true;
    }

    protected Vec3 getTempMobPos() {
        return new Vec3(this.mob.getX(), (double)this.getPathablePosY(), this.mob.getZ());
    }

    private int getPathablePosY() {
        if (this.mob.isInWater() && this.getCanSwim()) {
            int i = (int)this.mob.getBoundingBox().minY;
            Block block = this.level.getBlockState(new BlockPos(Mth.floor((double)this.mob.getX()), i, Mth.floor((double)this.mob.getZ()))).getBlock();
            int j = 0;
            while (block == Blocks.field_150358_i || block == Blocks.WATER) {
                block = this.level.getBlockState(new BlockPos(Mth.floor((double)this.mob.getX()), ++i, Mth.floor((double)this.mob.getZ()))).getBlock();
                if (++j <= 16) continue;
                return (int)this.mob.getBoundingBox().minY;
            }
            return i;
        }
        return (int)(this.mob.getBoundingBox().minY + 0.5);
    }

    protected PathFinder createPathFinder() {
        this.nodeEvaluator = new WalkNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return null;
    }

    protected boolean canMoveDirectly(Vec3 posVec31, Vec3 posVec32, int sizeX, int sizeY, int sizeZ) {
        int i = Mth.floor((double)posVec31.x);
        int j = Mth.floor((double)posVec31.z);
        double d0 = posVec32.x - posVec31.x;
        double d1 = posVec32.z - posVec31.z;
        double d2 = d0 * d0 + d1 * d1;
        if (d2 < 1.0E-8) {
            return false;
        }
        double d3 = 1.0 / Math.sqrt(d2);
        if (!this.isSafeToStandAt(i, (int)posVec31.y, j, sizeX += 2, sizeY, sizeZ += 2, posVec31, d0 *= d3, d1 *= d3)) {
            return false;
        }
        sizeX -= 2;
        sizeZ -= 2;
        double d4 = 1.0 / Math.abs(d0);
        double d5 = 1.0 / Math.abs(d1);
        double d6 = (double)i - posVec31.x;
        double d7 = (double)j - posVec31.z;
        if (d0 >= 0.0) {
            d6 += 1.0;
        }
        if (d1 >= 0.0) {
            d7 += 1.0;
        }
        d6 /= d0;
        d7 /= d1;
        int k = d0 < 0.0 ? -1 : 1;
        int l = d1 < 0.0 ? -1 : 1;
        int i1 = Mth.floor((double)posVec32.x);
        int j1 = Mth.floor((double)posVec32.z);
        int k1 = i1 - i;
        int l1 = j1 - j;
        while (k1 * k > 0 || l1 * l > 0) {
            if (d6 < d7) {
                d6 += d4;
                k1 = i1 - (i += k);
            } else {
                d7 += d5;
                l1 = j1 - (j += l);
            }
            if (this.isSafeToStandAt(i, (int)posVec31.y, j, sizeX, sizeY, sizeZ, posVec31, d0, d1)) continue;
            return false;
        }
        return true;
    }

    private boolean isPositionClear(int x, int y, int z, int sizeX, int sizeY, int sizeZ, Vec3 p_179692_7_, double p_179692_8_, double p_179692_10_) {
        for (BlockPos blockpos : BlockPos.func_177980_a((BlockPos)new BlockPos(x, y, z), (BlockPos)new BlockPos(x + sizeX - 1, y + sizeY - 1, z + sizeZ - 1))) {
            Block block;
            double d1;
            double d0 = (double)blockpos.getX() + 0.5 - p_179692_7_.x;
            if (!(d0 * p_179692_8_ + (d1 = (double)blockpos.getZ() + 0.5 - p_179692_7_.z) * p_179692_10_ >= 0.0) || (block = this.level.getBlockState(blockpos).getBlock()).func_176205_b((BlockGetter)this.level, blockpos)) continue;
            return false;
        }
        return true;
    }

    private boolean isSafeToStandAt(int x, int y, int z, int sizeX, int sizeY, int sizeZ, Vec3 vec31, double p_179683_8_, double p_179683_10_) {
        int i = x - sizeX / 2;
        int j = z - sizeZ / 2;
        if (!this.isPositionClear(i, y, j, sizeX, sizeY, sizeZ, vec31, p_179683_8_, p_179683_10_)) {
            return false;
        }
        for (int k = i; k < i + sizeX; ++k) {
            for (int l = j; l < j + sizeZ; ++l) {
                double d0 = (double)k + 0.5 - vec31.x;
                double d1 = (double)l + 0.5 - vec31.z;
                if (!(d0 * p_179683_8_ + d1 * p_179683_10_ >= 0.0)) continue;
                BlockPathTypes pathnodetype = this.nodeEvaluator.getBlockPathType((BlockGetter)this.level, k, y - 1, l, this.mob, sizeX, sizeY, sizeZ, true, true);
                if (pathnodetype == BlockPathTypes.WATER) {
                    return false;
                }
                if (pathnodetype == BlockPathTypes.LAVA) {
                    return false;
                }
                if (pathnodetype == BlockPathTypes.OPEN) {
                    return false;
                }
                pathnodetype = this.nodeEvaluator.getBlockPathType((BlockGetter)this.level, k, y, l, this.mob, sizeX, sizeY, sizeZ, true, true);
                float f = this.mob.getPathfindingMalus(pathnodetype);
                if (f < 0.0f || f >= 8.0f) {
                    return false;
                }
                if (pathnodetype != BlockPathTypes.DAMAGE_FIRE && pathnodetype != BlockPathTypes.DANGER_FIRE && pathnodetype != BlockPathTypes.DAMAGE_OTHER) continue;
                return false;
            }
        }
        return true;
    }

    protected void followThePath() {
        Vec3 vec3d = this.getTempMobPos();
        int i = this.path.getNodeCount();
        for (int j = this.path.getNextNodeIndex(); j < this.path.getNodeCount(); ++j) {
            if ((double)this.path.getNode((int)j).y == Math.floor(vec3d.y)) continue;
            i = j;
            break;
        }
        this.maxDistanceToWaypoint = this.mob.field_70130_N > 0.75f ? this.mob.field_70130_N / 2.0f : 0.75f - this.mob.field_70130_N / 2.0f;
        Vec3 vec3d1 = this.path.func_186310_f();
        if (Mth.abs((float)((float)(this.mob.getX() - (vec3d1.x + 0.5)))) < this.maxDistanceToWaypoint && Mth.abs((float)((float)(this.mob.getZ() - (vec3d1.z + 0.5)))) < this.maxDistanceToWaypoint && Math.abs(this.mob.getY() - vec3d1.y) < 1.0) {
            this.path.setNextNodeIndex(this.path.getNextNodeIndex() + 1);
        }
        int k = Mth.ceil((float)this.mob.field_70130_N);
        int l = Mth.ceil((float)this.mob.field_70131_O);
        int i1 = k;
        for (int j1 = i - 1; j1 >= this.path.getNextNodeIndex(); --j1) {
            if (!this.canMoveDirectly(vec3d, this.path.getEntityPosAtNode((Entity)this.mob, j1), k, l, i1)) continue;
            this.path.setNextNodeIndex(j1);
            break;
        }
    }
}

