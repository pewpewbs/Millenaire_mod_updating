package org.millenaire.common.pathing;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;

/** A bounded second search for long routes around an enclosure, within the same region. */
public final class DetourPathFinder extends PathFinder {
    public static final int DETOUR_NODE_BUDGET = 4096;
    public static final float DETOUR_WALK_LIMIT = 192.0F;
    private static final Map<Level, Long> LAST_EXPANDED_TICK = new WeakHashMap<>();
    private final PathFinder detours;
    private long nextDetourTick = Long.MIN_VALUE;

    public DetourPathFinder(NodeEvaluator evaluator, int normalBudget) {
        super(evaluator, normalBudget);
        this.detours = new PathFinder(evaluator, DETOUR_NODE_BUDGET);
    }

    @Override public Path findPath(PathNavigationRegion region, Mob mob, Set<BlockPos> targets,
            float range, int accuracy, float multiplier) {
        Path normal = super.findPath(region, mob, targets, range, accuracy, multiplier);
        if (normal != null && normal.canReach()) return normal;
        long tick = mob.level.getGameTime();
        if (tick < this.nextDetourTick) return normal;
        // Run on the server thread: at most one expanded search per level per tick.
        // Weak keys do not retain worlds after unload.
        synchronized (LAST_EXPANDED_TICK) {
            Long previous = LAST_EXPANDED_TICK.get(mob.level);
            if (previous != null && previous.longValue() == tick) return normal;
            LAST_EXPANDED_TICK.put(mob.level, tick);
        }
        this.nextDetourTick = tick + 100L;
        // The region is unchanged. Increasing the walked-distance limit must not load
        // a larger square of chunks. Never multiply the hard detour node budget.
        Path detour = this.detours.findPath(region, mob, targets,
                Math.max(range, DETOUR_WALK_LIMIT), accuracy, 1.0F);
        return detour != null && detour.canReach() ? detour : normal;
    }
}
