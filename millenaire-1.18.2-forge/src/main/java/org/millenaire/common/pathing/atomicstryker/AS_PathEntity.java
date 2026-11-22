package org.millenaire.common.pathing.atomicstryker;
import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.phys.Vec3;

public class AS_PathEntity extends Path {
    private long timeLastPathIncrement = System.currentTimeMillis();
    /** Original route for diagnostic display; navigation reads the current Path nodes. */
    public final Node[] pointsCopy;
    public AS_PathEntity(Node[] points) {
        this(points, points.length == 0 ? BlockPos.ZERO : points[points.length - 1].asBlockPos(), points.length > 0);
    }
    public AS_PathEntity(Node[] points, BlockPos target, boolean reachesTarget) {
        super(new ArrayList<>(Arrays.asList(points.clone())), target, reachesTarget && points.length > 0);
        pointsCopy = points.clone();
    }
    public void advancePathIndex() { advance(); }
    @Override public void advance() { if (!isDone()) setNextNodeIndex(getNextNodeIndex() + 1); }
    public Node getCurrentTargetPathPoint() { return isDone() ? null : getNode(getNextNodeIndex()); }
    public Node getFuturePathPoint(int jump) {
        int index = getNextNodeIndex() + jump;
        return jump < 0 || index < 0 || index >= getNodeCount() ? null : getNode(index);
    }
    public Node getNextTargetPathPoint() { return getFuturePathPoint(1); }
    public Node getPastTargetPathPoint(int jump) {
        int index = getNextNodeIndex() - jump;
        return jump < 0 || index < 0 || index >= getNodeCount() ? null : getNode(index);
    }
    public Node getPreviousTargetPathPoint() { return getPastTargetPathPoint(1); }
    @Override public Vec3 getNextEntityPos(Entity entity) { return isDone() ? null : super.getNextEntityPos(entity); }
    public long getTimeSinceLastPathIncrement() { return System.currentTimeMillis() - timeLastPathIncrement; }
    @Override public void setNextNodeIndex(int index) {
        timeLastPathIncrement = System.currentTimeMillis();
        super.setNextNodeIndex(Math.max(0, Math.min(index, getNodeCount())));
    }
}
