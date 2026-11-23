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
        // The pathfinder is built with its own evaluator. Configure BOTH flags
        // here: allowing passage alone does not classify a closed wooden door
        // as a traversable/openable node when planning a route.
        this.nodeEvaluator.setCanOpenDoors(true);
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
    /**
     * GroundPathNavigation can follow horizontal paths, but its MoveControl
     * does not reliably supply vertical motion for consecutive ladder nodes.
     * The classic A* ladder edges use the same X/Z with changing Y.
     * Assist ONLY that segment, on an actual ladder, without teleportation.
     */
    @Override public void tick() {
        super.tick();
        if (this.mob.level.isClientSide || this.path == null || this.path.isDone()) return;
        net.minecraft.world.level.pathfinder.Node next = this.path.getNextNode();
        if (next == null) return;
        net.minecraft.core.BlockPos feet = this.mob.blockPosition();
        boolean sameColumn = next.x == feet.getX() && next.z == feet.getZ();
        net.minecraft.core.BlockPos rung = new net.minecraft.core.BlockPos(next.x, next.y, next.z);
        boolean ladderContact = this.mob.level.getBlockState(feet).is(net.minecraft.world.level.block.Blocks.LADDER)
                || this.mob.level.getBlockState(feet.below()).is(net.minecraft.world.level.block.Blocks.LADDER)
                || this.mob.level.getBlockState(rung).is(net.minecraft.world.level.block.Blocks.LADDER);
        double dy = next.y - this.mob.getY();
        double centerX = next.x + 0.5D;
        double centerZ = next.z + 0.5D;
        double xError = centerX - this.mob.getX();
        double zError = centerZ - this.mob.getZ();
        if (!org.millenaire.common.pathing.LadderTransitionRules.shouldAssist(
                sameColumn, ladderContact, dy, xError * xError + zError * zError)) return;
        net.minecraft.world.phys.Vec3 velocity = this.mob.getDeltaMovement();
        double lift = dy > 0 ? Math.max(0.20D, velocity.y) : Math.min(-0.12D, velocity.y);
        // Keep close to the shaft centre and let the usual movement/collision
        // system validate every step; do not change positions directly.
        this.mob.setDeltaMovement(
                velocity.x * 0.5D + xError * 0.12D,
                lift,
                velocity.z * 0.5D + zError * 0.12D);
        this.mob.hasImpulse = true;
    }

    @Override public boolean moveTo(Path requested, double speed) {
        if (requested != null && requested == this.path && !isDone()) {
            this.speedModifier = speed;
            return true;
        }
        return super.moveTo(requested, speed);
    }
}
