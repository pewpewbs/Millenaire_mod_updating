package org.millenaire.common.pathing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** One debug observation. No chunk reads, entity references, movement or door control. */
public final class DoorCrossing {
    public final BlockPos door;
    public final Direction direction;
    public final long started;
    public final String goal;
    public final long job;
    public final int routeId, nodeIndex;
    public boolean manual, powered;
    public String entryState;
    public int stationaryTicks;
    private final double halfWidth;
    private long lastTick;
    private double lastX, lastY, lastZ;
    private boolean throughCentre;

    public enum Result { CROSSED, RETREATED, DIVERTED, DISCONTINUITY, GOAL_CHANGED, TIMEOUT, REMOVED }

    private DoorCrossing(BlockPos door, Direction direction, long tick, double x, double y, double z,
            double width, String goal, long job, int routeId, int nodeIndex) {
        this.door = door.immutable();
        this.direction = direction;
        this.started = this.lastTick = tick;
        this.lastX = x; this.lastY = y; this.lastZ = z;
        this.halfWidth = width / 2;
        this.goal = goal;
        this.job = job;
        this.routeId = routeId;
        this.nodeIndex = nodeIndex;
    }

    public static DoorCrossing approach(BlockPos door, Direction dir, long tick, double x, double y,
            double z, double width, String goal, long job, int routeId, int nodeIndex) {
        if (dir == null || !dir.getAxis().isHorizontal() || y < door.getY()-0.6 || y > door.getY()+1) return null;
        double along = (x-door.getX()-0.5)*dir.getStepX() + (z-door.getZ()-0.5)*dir.getStepZ();
        double across = (x-door.getX()-0.5)*dir.getStepZ() - (z-door.getZ()-0.5)*dir.getStepX();
        if (along > -width/2 || along < -2.8 || Math.abs(across) > 1.25) return null;
        return new DoorCrossing(door,dir,tick,x,y,z,width,goal,job,routeId,nodeIndex);
    }

    public Result sample(long tick, double x, double y, double z, String currentGoal, long currentJob) {
        if (tick == lastTick) return null;
        double dx = x-lastX, dy = y-lastY, dz = z-lastZ;
        if (tick != lastTick+1 || dx*dx + dz*dz > 1.0 || Math.abs(dy)>1.0) return Result.DISCONTINUITY;
        int sx = direction.getStepX(), sz = direction.getStepZ();
        double along = (x-door.getX()-0.5)*sx + (z-door.getZ()-0.5)*sz;
        double across = (x-door.getX()-0.5)*sz - (z-door.getZ()-0.5)*sx;
        double step = dx*sx + dz*sz;
        if (step > 0 && along >= 0 && along-step < 0) {
            // Interpolate the actual crossing point. Passing on a parallel
            // lane or through the other leaf is not a crossing of this cell.
            double fractionBack = along/step;
            double offset = across - fractionBack*(dx*sz - dz*sx);
            double height = y - fractionBack*dy - door.getY();
            if (Math.abs(offset)+halfWidth > 0.51 || height < -0.6 || height > 1) return Result.DIVERTED;
            throughCentre = true;
        }
        if (throughCentre && step > 0 && along >= 0.5 && along-step < 0.5) {
            double atFarFace = across - (along-0.5)/step*(dx*sz - dz*sx);
            if (Math.abs(atFarFace)+halfWidth > 0.51) return Result.DIVERTED;
        }
        if (throughCentre && along >= 0.5+halfWidth) return Result.CROSSED;
        if ((throughCentre && along <= -halfWidth) || along < -2.9) return Result.RETREATED;
        if (Math.abs(across)>1.75 || y < door.getY()-1 || y > door.getY()+1.5) return Result.DIVERTED;
        if (!java.util.Objects.equals(goal,currentGoal) || job != currentJob) return Result.GOAL_CHANGED;
        if (dx*dx + dy*dy + dz*dz < 0.0004) stationaryTicks++;
        if (tick-started >= 100) return Result.TIMEOUT;
        lastTick=tick; lastX=x; lastY=y; lastZ=z;
        return null;
    }
}
