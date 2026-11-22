package org.millenaire.common.pathing;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.utilities.MillLog;

/**
 * Expand expensive-looking routes with a bounded second vanilla A* search.
 * The gate is never a mandatory waypoint: each search compares complete
 * physical paths to the same destination using one consistent cost measure.
 */
public final class DetourPathFinder extends PathFinder {
    public static final int DETOUR_NODE_BUDGET = 4096;
    public static final float DETOUR_WALK_LIMIT = 192.0F;
    private static final Map<Level, Long> LAST_EXPANDED_TICK = new WeakHashMap<>();
    private static final long MIN_WORLD_RETRY_TICKS = 5L;
    private static final long MIN_ENTITY_RETRY_TICKS = 120L;
    private static final double INEFFICIENT_ROUTE_FACTOR = 1.35D;

    private final PathFinder detours;
    private long nextDetourTick = Long.MIN_VALUE;

    public DetourPathFinder(NodeEvaluator evaluator, int normalBudget) {
        super(evaluator, normalBudget);
        this.detours = new PathFinder(evaluator, DETOUR_NODE_BUDGET);
    }

    /**
     * Comparable traversal estimate, measured from actual path node geometry.
     * Vertical climbs and terrain penalties count, instead of just end-to-end
     * Euclidean distance (which ignores all obstacles).
     */
    private static double traversalCost(Path path) {
        if (path == null || path.getNodeCount() == 0) return Double.POSITIVE_INFINITY;
        double result = 0.0D;
        Node previous = path.getNode(0);
        for (int i = 1; i < path.getNodeCount(); i++) {
            Node current = path.getNode(i);
            int dx = current.x - previous.x;
            int dy = current.y - previous.y;
            int dz = current.z - previous.z;
            result += Math.sqrt((double)dx * dx + (double)dy * dy + (double)dz * dz);
            result += Math.max(0, dy) * 0.50D + Math.max(0, -dy) * 0.20D;
            result += Math.max(0.0F, current.costMalus);
            previous = current;
        }
        return result;
    }

    private static boolean worthComparing(Mob mob, Path path, double cost) {
        if (path == null || !path.canReach()) return true;
        if (path.getNodeCount() < 10 || cost < 12.0D) return false;
        Node end = path.getEndNode();
        if (end == null) return true;
        double dx = end.x + 0.5D - mob.getX();
        double dy = end.y - mob.getY();
        double dz = end.z + 0.5D - mob.getZ();
        double direct = Math.sqrt(dx * dx + dy * dy + dz * dz);
        // This is not a demand for a straight line; it simply warrants a
        // second search when a full path is substantially longer than ideal.
        return cost > Math.max(14.0D, direct * INEFFICIENT_ROUTE_FACTOR + 3.0D);
    }

    @Override
    public Path findPath(PathNavigationRegion region, Mob mob, Set<BlockPos> targets,
            float range, int accuracy, float multiplier) {
        Path normal = super.findPath(region, mob, targets, range, accuracy, multiplier);
        double normalCost = traversalCost(normal);
        if (!worthComparing(mob, normal, normalCost)) return normal;

        long tick = mob.level.getGameTime();
        if (tick < this.nextDetourTick) return normal;
        // No more than one expensive attempt per world every five ticks, and
        // once per pathfinder/actor per six seconds to control TPS cost.
        synchronized (LAST_EXPANDED_TICK) {
            Long previous = LAST_EXPANDED_TICK.get(mob.level);
            if (previous != null && tick - previous < MIN_WORLD_RETRY_TICKS) {
                return normal;
            }
            LAST_EXPANDED_TICK.put(mob.level, tick);
        }
        this.nextDetourTick = tick + MIN_ENTITY_RETRY_TICKS;

        // Both searches use exactly the same terrain rules and target set.
        // A larger search is not automatically better: use it ONLY if it
        // genuinely reaches the destination with a lower estimated cost.
        Path expanded = this.detours.findPath(region, mob, targets,
                Math.max(range, DETOUR_WALK_LIMIT), accuracy, 1.0F);
        if (expanded == null || !expanded.canReach()) return normal;
        if (normal == null || !normal.canReach()) return expanded;

        double expandedCost = traversalCost(expanded);
        boolean preferExpanded = expandedCost + 0.5D < normalCost;
        if (MillConfigValues.LogPathing >= 2
                || MillConfigValues.LogPathing >= 1 && preferExpanded) {
            MillLog.major(mob, "NAV-PATH quality normalCost=" + normalCost
                    + " expandedCost=" + expandedCost
                    + " selected=" + (preferExpanded ? "expanded" : "normal")
                    + " target=" + (normal.getEndNode() == null
                        ? "unknown" : normal.getEndNode().asBlockPos()));
        }
        return preferExpanded ? expanded : normal;
    }
}
