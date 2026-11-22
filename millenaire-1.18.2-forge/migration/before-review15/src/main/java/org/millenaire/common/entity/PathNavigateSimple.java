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
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.pathfinder.PathComputationType;

public class PathNavigateSimple
extends PathNavigation {
    public PathNavigateSimple(Mob entityIn, Level worldIn) {
        super(entityIn, worldIn);
    }

    @Override
    public boolean moveTo(net.minecraft.world.level.pathfinder.Path requested, double speed) {
        if (requested != null && requested == this.path && !this.isDone()) {
            // Reusing an active route must not restart vanilla's stuck timer each tick.
            this.speedModifier = speed;
            return true;
        }
        return super.moveTo(requested, speed);
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
            int i = Mth.floor(this.mob.getBoundingBox().minY);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Mth.floor(this.mob.getX()), i, Mth.floor(this.mob.getZ()));
            int scanned = 0;
            while (i < this.level.getMaxBuildHeight() && this.level.getFluidState(pos).is(FluidTags.WATER)) {
                if (++scanned > 16) return Mth.floor(this.mob.getBoundingBox().minY);
                pos.setY(++i);
            }
            return i;
        }
        return Mth.floor(this.mob.getBoundingBox().minY + 0.5);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new WalkNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        this.nodeEvaluator.setCanOpenDoors(true);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
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
        if (!this.isSafeToStandAt(i, Mth.floor(posVec31.y), j, sizeX += 2, sizeY, sizeZ += 2, posVec31, d0 *= d3, d1 *= d3)) {
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
        // An axis-aligned segment must not produce 0 / 0 and stall traversal.
        d6 = d0 == 0.0 ? Double.POSITIVE_INFINITY : d6 / d0;
        d7 = d1 == 0.0 ? Double.POSITIVE_INFINITY : d7 / d1;
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
            if (this.isSafeToStandAt(i, Mth.floor(posVec31.y), j, sizeX, sizeY, sizeZ, posVec31, d0, d1)) continue;
            return false;
        }
        return true;
    }

    private boolean isPositionClear(int x, int y, int z, int sizeX, int sizeY, int sizeZ, Vec3 p_179692_7_, double p_179692_8_, double p_179692_10_) {
        for (BlockPos blockpos : BlockPos.betweenClosed((BlockPos)new BlockPos(x, y, z), (BlockPos)new BlockPos(x + sizeX - 1, y + sizeY - 1, z + sizeZ - 1))) {
            Block block;
            double d1;
            double d0 = (double)blockpos.getX() + 0.5 - p_179692_7_.x;
            if (!(d0 * p_179692_8_ + (d1 = (double)blockpos.getZ() + 0.5 - p_179692_7_.z) * p_179692_10_ >= 0.0) || this.level.getBlockState(blockpos).isPathfindable(this.level, blockpos, PathComputationType.LAND)) continue;
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
        if (this.path == null || this.path.isDone()) return;
        Vec3 vec3d = this.getTempMobPos();
        int i = this.path.getNodeCount();
        for (int j = this.path.getNextNodeIndex(); j < this.path.getNodeCount(); ++j) {
            if ((double)this.path.getNode((int)j).y == Math.floor(vec3d.y)) continue;
            i = j;
            break;
        }
        this.maxDistanceToWaypoint = this.mob.getBbWidth() > 0.75f ? this.mob.getBbWidth() / 2.0f : 0.75f - this.mob.getBbWidth() / 2.0f;
        BlockPos nextNode = this.path.getNextNodePos();
        Vec3 vec3d1 = Vec3.atLowerCornerOf(nextNode);
        if (Mth.abs((float)((float)(this.mob.getX() - (vec3d1.x + 0.5)))) < this.maxDistanceToWaypoint && Mth.abs((float)((float)(this.mob.getZ() - (vec3d1.z + 0.5)))) < this.maxDistanceToWaypoint && Math.abs(this.mob.getY() - vec3d1.y) < 1.0) {
            this.path.setNextNodeIndex(this.path.getNextNodeIndex() + 1);
        }
        int k = Mth.ceil((float)this.mob.getBbWidth());
        int l = Mth.ceil((float)this.mob.getBbHeight());
        int i1 = k;
        for (int j1 = i - 1; j1 >= this.path.getNextNodeIndex(); --j1) {
            if (!this.canMoveDirectly(vec3d, this.path.getEntityPosAtNode((Entity)this.mob, j1), k, l, i1)) continue;
            this.path.setNextNodeIndex(j1);
            break;
        }
        this.doStuckDetection(vec3d);
    }
}

