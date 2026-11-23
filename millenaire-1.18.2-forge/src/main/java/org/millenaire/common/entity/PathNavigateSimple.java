package org.millenaire.common.entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
public class PathNavigateSimple extends GroundPathNavigation {
    private org.millenaire.common.pathing.DetourPathFinder detourPathFinder;
    private long nextLadderTraceTick;
    private long nextDoorWaypointTraceTick;
    // Diagnostic only: separate real floor/collision height from integer
    // stair waypoint height; do not alter movement until gameplay evidence.
    private long nextStairTraceTick;
    private long stairStationarySince = -1L;
    private double stairLastX, stairLastZ;
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
     * GroundPathNavigation does not reliably drive a mob onto a ladder's thin
     * collision plane or actively climb vertically once a node is reached.
     * Assist only when the current path targets an actual vanilla ladder rung.
     * All motion is velocity-based and still uses Minecraft collision checks.
     */
    @Override public void tick() {
        super.tick();
        if (this.mob.level.isClientSide || this.path == null || this.path.isDone()) return;
        net.minecraft.world.level.pathfinder.Node next = this.path.getNextNode();
        if (next == null) return;

        net.minecraft.core.BlockPos rung = new net.minecraft.core.BlockPos(next.x, next.y, next.z);
        if (!this.mob.level.hasChunkAt(rung)) return;
        net.minecraft.world.level.block.state.BlockState state = this.mob.level.getBlockState(rung);
        this.traceNearbyStair(rung);
        // A path can have the OPEN door block as its next node for many ticks
        // while a villager remains stationary in front of it. Distinguish
        // collision, bad waypoint height, and absent physical movement before
        // changing movement physics. Emit only near a DOOR node (not gates).
        if (org.millenaire.common.utilities.BlockItemUtilities.isWoodenDoor(state.getBlock())) {
            double ex = rung.getX() + 0.5D - this.mob.getX();
            double ez = rung.getZ() + 0.5D - this.mob.getZ();
            long now = this.mob.level.getGameTime();
            if (ex * ex + ez * ez <= 6.25D
                    && org.millenaire.common.config.MillConfigValues.LogPathing >= 2
                    && now >= this.nextDoorWaypointTraceTick) {
                this.nextDoorWaypointTraceTick = now + 40L;
                org.millenaire.common.utilities.MillLog.major(this.mob,
                        "DOOR-WAYPOINT next=" + rung
                        + " open=" + state.getValue(net.minecraft.world.level.block.DoorBlock.OPEN)
                        + " facing=" + state.getValue(net.minecraft.world.level.block.DoorBlock.FACING)
                        + " feet=" + this.mob.blockPosition()
                        + " exactY=" + String.format(java.util.Locale.ROOT, "%.2f", this.mob.getY())
                        + " collision=" + this.mob.horizontalCollision
                        + " velocity=" + this.mob.getDeltaMovement()
                        + " waypoint=" + this.path.getNextNodeIndex() + "/" + this.path.getNodeCount());
            }
        }
        if (!state.is(net.minecraft.world.level.block.Blocks.LADDER)) return;

        // An entity must press into the back face of a ladder to engage the
        // climbing surface. The rung's center (0.5, 0.5) can be too far from
        // that surface, leaving the entity jumping but not actually climbing.
        net.minecraft.core.Direction wall = state.getValue(
                net.minecraft.world.level.block.LadderBlock.FACING).getOpposite();
        double targetX = next.x + 0.5D + wall.getStepX() * 0.12D;
        double targetZ = next.z + 0.5D + wall.getStepZ() * 0.12D;
        double ex = targetX - this.mob.getX();
        double ez = targetZ - this.mob.getZ();
        double horizontalErrorSquared = ex * ex + ez * ez;
        if (!org.millenaire.common.pathing.LadderTransitionRules.shouldApproach(
                true, horizontalErrorSquared)) return;

        net.minecraft.world.phys.Vec3 velocity = this.mob.getDeltaMovement();
        // Even before its feet enter the rung block, nudge the mob into the
        // column. Do NOT start vertical lift while it is still off-column.
        double vx = org.millenaire.common.pathing.LadderTransitionRules.approachSpeed(ex);
        double vz = org.millenaire.common.pathing.LadderTransitionRules.approachSpeed(ez);
        double vy = velocity.y;
        net.minecraft.core.BlockPos feet = this.mob.blockPosition();
        boolean sameColumn = next.x == feet.getX() && next.z == feet.getZ();
        boolean ladderContact = this.mob.onClimbable()
                || this.mob.level.getBlockState(feet).is(net.minecraft.world.level.block.Blocks.LADDER);
        double dy = next.y - this.mob.getY();
        if (org.millenaire.common.pathing.LadderTransitionRules.shouldAssist(
                sameColumn, ladderContact, dy, horizontalErrorSquared)) {
            vy = dy > 0 ? Math.max(0.20D, velocity.y) : Math.min(-0.12D, velocity.y);
        }
        this.mob.setDeltaMovement(vx, vy, vz);
        this.mob.hasImpulse = true;
        // Log only when requested and at most once per 100 ticks for this
        // navigator. Distinguishes route-selection failures from failed
        // physical climbing in the Minecraft playtest log.
        if (org.millenaire.common.config.MillConfigValues.LogPathing >= 2
                && this.mob.level.getGameTime() >= this.nextLadderTraceTick) {
            this.nextLadderTraceTick = this.mob.level.getGameTime() + 100L;
            org.millenaire.common.utilities.MillLog.major(this.mob,
                    "LADDER-ASSIST next=" + rung + " feet=" + feet
                    + " dy=" + String.format(java.util.Locale.ROOT, "%.2f", dy)
                    + " sameColumn=" + sameColumn
                    + " contact=" + ladderContact
                    + " velocityY=" + String.format(java.util.Locale.ROOT, "%.2f", vy));
        }
    }

    /**
     * STAIR-01 instrumentation. Integer A-star and JPS nodes do not describe the
     * physical stair collision height (slab portion + full-height portion).
     * This only logs a limited sample near a real stair so a report can
     * distinguish unreachable planned node, ceiling collision, and a mob
     * that never moves horizontally. It does NOT change pathing or physics.
     */
    private void traceNearbyStair(net.minecraft.core.BlockPos next) {
        if (org.millenaire.common.config.MillConfigValues.LogPathing < 2) return;
        long now = this.mob.level.getGameTime();
        if (this.stairStationarySince < 0L || now < this.stairStationarySince
                || (this.mob.getX() - this.stairLastX) * (this.mob.getX() - this.stairLastX)
                 + (this.mob.getZ() - this.stairLastZ) * (this.mob.getZ() - this.stairLastZ)
                    >= 0.16D) {
            this.stairStationarySince = now;
            this.stairLastX = this.mob.getX();
            this.stairLastZ = this.mob.getZ();
        }
        if (now < this.nextStairTraceTick) return;
        net.minecraft.core.BlockPos stair = null;
        var level = this.mob.level;
        // Stair below the NEXT node is normal when the planner counts it
        // as a whole block and routes the feet to y+1. Also detect a stair
        // at the node itself (potential half-height shape misclassification).
        for (var sample : new net.minecraft.core.BlockPos[] {next, next.below()}) {
            if (level.hasChunkAt(sample)
                    && level.getBlockState(sample).getBlock()
                            instanceof net.minecraft.world.level.block.StairBlock) {
                stair = sample;
                break;
            }
        }
        // If the route deliberately goes AROUND stairs, instrument them
        // only while the NPC has been stationary with an active path.
        boolean stalled = now - this.stairStationarySince >= 40L;
        if (stair == null && stalled) {
            var feet = this.mob.blockPosition();
            outer: for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = -1; dy <= 0; dy++) {
                        var candidate = feet.offset(dx, dy, dz);
                        if (!level.hasChunkAt(candidate)) continue;
                        if (level.getBlockState(candidate).getBlock()
                                instanceof net.minecraft.world.level.block.StairBlock) {
                            stair = candidate;
                            break outer;
                        }
                    }
                }
            }
        }
        if (stair == null) return;
        double ox = stair.getX() + 0.5D - this.mob.getX();
        double oz = stair.getZ() + 0.5D - this.mob.getZ();
        if (ox * ox + oz * oz > 6.25D) return;
        this.nextStairTraceTick = now + 60L;
        var shapeState = level.getBlockState(stair);
        var shape = shapeState.getCollisionShape(level, stair);
        var targetBox = this.mob.getBoundingBox().move(
                next.getX() + 0.5D - this.mob.getX(),
                next.getY() - this.mob.getY(),
                next.getZ() + 0.5D - this.mob.getZ());
        org.millenaire.common.utilities.MillLog.major(this.mob,
                "STAIR-WAYPOINT stair=" + stair
                + " facing=" + shapeState.getValue(
                        net.minecraft.world.level.block.StairBlock.FACING)
                + " half=" + shapeState.getValue(
                        net.minecraft.world.level.block.StairBlock.HALF)
                + " shape=" + shapeState.getValue(
                        net.minecraft.world.level.block.StairBlock.SHAPE)
                + " collisionTop=" + (shape.isEmpty() ? "none"
                    : String.format(java.util.Locale.ROOT, "%.2f", shape.bounds().maxY))
                + " feet=" + this.mob.blockPosition()
                + " exactY=" + String.format(java.util.Locale.ROOT, "%.2f", this.mob.getY())
                + " next=" + next
                + " nextHead=" + level.getBlockState(next.above()).getBlock()
                + " atNextClear=" + level.noCollision(this.mob, targetBox)
                + " mobHeight=" + String.format(java.util.Locale.ROOT, "%.2f",
                        this.mob.getBbHeight())
                + " horizontalCollision=" + this.mob.horizontalCollision
                + " velocity=" + this.mob.getDeltaMovement()
                + " stationaryTicks=" + (now - this.stairStationarySince)
                + " path=" + this.path.getNextNodeIndex() + "/" + this.path.getNodeCount());
    }

    @Override public boolean moveTo(Path requested, double speed) {
        if (requested != null && requested == this.path && !isDone()) {
            this.speedModifier = speed;
            return true;
        }
        return super.moveTo(requested, speed);
    }
}
