/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.pathfinding.Path
 *  net.minecraft.pathfinding.PathPoint
 *  net.minecraft.util.math.Vec3d
 */
package org.millenaire.common.pathing.atomicstryker;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.phys.Vec3;

public class AS_PathEntity
extends Path {
    private long timeLastPathIncrement = System.currentTimeMillis();
    public final Node[] pointsCopy;
    private int pathIndexCopy;

    public AS_PathEntity(Node[] points) {
        super(points);
        this.pointsCopy = points;
        this.pathIndexCopy = 0;
    }

    public void advancePathIndex() {
        this.timeLastPathIncrement = System.currentTimeMillis();
        ++this.pathIndexCopy;
        this.setNextNodeIndex(this.pathIndexCopy);
    }

    public Node getCurrentTargetPathPoint() {
        if (this.isDone()) {
            return null;
        }
        return this.pointsCopy[this.getNextNodeIndex()];
    }

    public Node getFuturePathPoint(int jump) {
        if (this.getNextNodeIndex() >= this.pointsCopy.length - jump) {
            return null;
        }
        return this.pointsCopy[this.getNextNodeIndex() + jump];
    }

    public Node getNextTargetPathPoint() {
        if (this.getNextNodeIndex() >= this.pointsCopy.length - 1) {
            return null;
        }
        return this.pointsCopy[this.getNextNodeIndex() + 1];
    }

    public Node getPastTargetPathPoint(int jump) {
        if (this.getNextNodeIndex() < jump || this.pointsCopy.length == 0) {
            return null;
        }
        return this.pointsCopy[this.getNextNodeIndex() - jump];
    }

    public Vec3 getNextEntityPos(Entity var1) {
        if (super.isDone()) {
            return null;
        }
        return super.getNextEntityPos(var1);
    }

    public Node getPreviousTargetPathPoint() {
        if (this.getNextNodeIndex() < 1 || this.pointsCopy.length == 0) {
            return null;
        }
        return this.pointsCopy[this.getNextNodeIndex() - 1];
    }

    public long getTimeSinceLastPathIncrement() {
        return System.currentTimeMillis() - this.timeLastPathIncrement;
    }

    public void setNextNodeIndex(int par1) {
        this.timeLastPathIncrement = System.currentTimeMillis();
        this.pathIndexCopy = par1;
        super.setNextNodeIndex(par1);
    }
}

