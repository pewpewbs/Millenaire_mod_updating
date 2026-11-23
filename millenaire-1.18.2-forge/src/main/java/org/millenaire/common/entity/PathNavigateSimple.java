package org.millenaire.common.entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
public class PathNavigateSimple extends GroundPathNavigation {
    private org.millenaire.common.pathing.DetourPathFinder detourPathFinder;
    public PathNavigateSimple(Mob mob, Level level) {
        super(mob, level);
        setCanOpenDoors(true);
        setCanPassDoors(true);
        setCanFloat(true);
        mob.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.WATER, 32.0F);
        mob.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.DANGER_OTHER, 8.0F);
        setMaxVisitedNodesMultiplier(4.0F);
    }
    @Override protected net.minecraft.world.level.pathfinder.PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new org.millenaire.common.pathing.VillagerWalkNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        this.detourPathFinder = new org.millenaire.common.pathing.DetourPathFinder(this.nodeEvaluator, maxVisitedNodes);
        return this.detourPathFinder;
    }
    /** Request a single CPU-bounded broader search on the next navigation attempt. */
    public void requestRecoveryDetour() {
        if (this.detourPathFinder != null) this.detourPathFinder.requestRecoveryDetour();
    }

    @Override protected Path createPath(java.util.Set<net.minecraft.core.BlockPos> targets, int padding, boolean above, int accuracy) {
        // Search beyond the default mob range so the exit on the far side of a fort is visible.
        long began = System.nanoTime();
        try { return super.createPath(targets, padding, above, accuracy, 64.0F); }
        finally { org.millenaire.common.pathing.NavigationMetrics.record(0, System.nanoTime()-began); }
    }
    @Override public boolean moveTo(Path requested, double speed) {
        if (requested != null && requested == this.path && !isDone()) {
            this.speedModifier = speed;
            return true;
        }
        return super.moveTo(requested, speed);
    }
}
