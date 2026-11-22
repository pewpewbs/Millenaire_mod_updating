package org.millenaire.common.pathing.atomicstryker;
import net.minecraft.world.level.pathfinder.Node;
/** Named access replaces reflection against the old PathPoint field order. */
public class AS_PathPoint extends Node {
    public AS_PathPoint(int x, int y, int z) { super(x, y, z); }
    public void setIndex(int index) { heapIdx = index; }
    public void setTotalPathDistance(float distance) { g = distance; }
    public void setDistanceToNext(float distance) { h = distance; }
    public void setDistanceToTarget(float distance) { f = distance; }
    public void setPrevious(Node previous) { cameFrom = previous; }
}
