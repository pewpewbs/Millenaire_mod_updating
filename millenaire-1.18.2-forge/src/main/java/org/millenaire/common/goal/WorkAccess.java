package org.millenaire.common.goal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.utilities.Point;
public final class WorkAccess {
    private WorkAccess() {}
    /**
     * A stock transfer must be performed from the actual stock position's side
     * of the wall, not from an alternate or partial path endpoint.
     */
    public static boolean canAccessStock(MillVillager villager, Point proposed, double range) {
        if (villager == null || range <= 0) return false;
        Point actual = villager.getGoalDestPoint() != null
                ? villager.getGoalDestPoint() : proposed;
        return actual != null && actual.horizontalDistanceTo(villager) < range
                && visible(villager, Vec3.atCenterOf(actual.getBlockPos()), actual.getBlockPos());
    }

    /** Measure to the registered work target, never an alternate path endpoint. */
    public static boolean canAccessResource(MillVillager villager, Point target, double range) {
        if (villager == null || target == null || range <= 0
                || !villager.level.hasChunkAt(target.getBlockPos())) return false;
        return canAccessResource(villager.level, villager.position(), target.getBlockPos(), range);
    }

    static boolean canAccessResource(net.minecraft.world.level.BlockGetter world, Vec3 feet, BlockPos target, double range) {
        Vec3 center = Vec3.atCenterOf(target);
        Vec3 from = feet.add(0, 0.8, 0);
        return range > 0 && from.distanceToSqr(center) < range * range
                && loadedBetween(world, new BlockPos(from), target) && visible(world, from, center, target);
    }

    static boolean loadedBetween(net.minecraft.world.level.BlockGetter world, BlockPos a, BlockPos b) {
        if (!(world instanceof net.minecraft.world.level.Level level)) return true;
        for (int x = Math.min(a.getX(), b.getX()) >> 4; x <= (Math.max(a.getX(), b.getX()) >> 4); x++)
            for (int z = Math.min(a.getZ(), b.getZ()) >> 4; z <= (Math.max(a.getZ(), b.getZ()) >> 4); z++)
                if (!level.hasChunk(x, z)) return false;
        return true;
    }

    public static boolean visible(MillVillager villager, Vec3 target, BlockPos allowedTarget) {
        // A waist-height ray prevents reaching across a fence or through a wall.
        Vec3 from = villager.position().add(0, 0.8, 0);
        return visible(villager.level, from, target, allowedTarget);
    }
    public static boolean visible(net.minecraft.world.level.BlockGetter world, Vec3 from,
            Vec3 target, BlockPos allowedTarget) {
        var hit = world.clip(new ClipContext(from, target,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
        return hit.getType() == HitResult.Type.MISS
                || allowedTarget != null && hit.getBlockPos().equals(allowedTarget);
    }
}
