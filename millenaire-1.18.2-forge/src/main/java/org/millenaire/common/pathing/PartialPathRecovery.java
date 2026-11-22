package org.millenaire.common.pathing;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
public final class PartialPathRecovery {
    private PartialPathRecovery() {}
    public static boolean canUse(Path path, Vec3 current, Vec3 target) {
        if (path == null || path.getNodeCount() < 2 || path.isDone() || path.getEndNode() == null) return false;
        Vec3 end = Vec3.atBottomCenterOf(path.getEndNode().asBlockPos());
        // Partial routes are useful only if they make measurable progress toward the destination.
        return end.distanceTo(target) + 1.0 < current.distanceTo(target);
    }
}
