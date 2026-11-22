package org.millenaire.common.goal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.millenaire.common.entity.MillVillager;
public final class WorkAccess {
    private WorkAccess() {}
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
