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
    private boolean ladderMotionControlled;
    public boolean isLadderMotionControlled() { return this.ladderMotionControlled; }

    @Override protected boolean canUpdatePath() {
        // Vanilla's airborne branch skips a node whenever feet are above it,
        // even if the NPC has not descended to that rung yet.
        return this.path instanceof org.millenaire.common.pathing.atomicstryker.AS_PathEntity
                || super.canUpdatePath();
    }

    @Override protected void followThePath() {
        if (!(this.path instanceof org.millenaire.common.pathing.atomicstryker.AS_PathEntity)) {
            super.followThePath();
            return;
        }
        var next = this.path.getNextNode();
        if (next == null) return;
        if (!this.isLadderNode(next)) {
            var target = this.path.getNextEntityPos(this.mob);
            // Do not use vanilla's dot-product corner shortcut: it can skip
            // the only validated approach to a stair, door, or ladder shaft.
            // Aim/reach use the SAME centre (vanilla reach uses x/z + .8 for
            // a .6-wide mob whereas Path.getNextEntityPos uses + .5).
            this.maxDistanceToWaypoint = 0.20F;
            if (Math.abs(this.mob.getX() - target.x) <= this.maxDistanceToWaypoint
                    && Math.abs(this.mob.getZ() - target.z) <= this.maxDistanceToWaypoint
                    && Math.abs(this.mob.getY() - this.getGroundY(target)) < 0.25D) {
                this.path.advance();
            }
        }
        // Ladder advancement belongs solely to assistLadderRung's directional
        // contact/height test; timeouts and physical stall recovery remain active.
        this.doStuckDetection(this.mob.position());
    }

    /**
     * GroundPathNavigation does not reliably drive a mob onto a ladder's thin
     * collision plane or actively climb vertically once a node is reached.
     * Assist only when the current path targets an actual vanilla ladder rung.
     * All motion is velocity-based and still uses Minecraft collision checks.
     */
    @Override public void tick() {
        this.ladderMotionControlled = false;
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
            this.assistOpenDoorWaypoint(rung, state);
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
        if (!state.is(net.minecraft.world.level.block.Blocks.LADDER)) {
            this.assistLeavingLadder(next);
            return;
        }
        this.assistLadderRung(next, state);
    }

    // An OPEN door still has a collision panel. Aim at the actual clear
    // cross-section rather than repeatedly pushing into the hinged panel.
    private void assistOpenDoorWaypoint(net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state) {
        if (!state.getValue(net.minecraft.world.level.block.DoorBlock.OPEN)
                || !this.mob.horizontalCollision || Math.abs(this.mob.getY() - pos.getY()) > 0.25D) return;
        double x = pos.getX() + 0.5D, z = pos.getZ() + 0.5D;
        double dx = x - this.mob.getX(), dz = z - this.mob.getZ();
        if (dx * dx + dz * dz > 4.0D) return;
        var shape = state.getCollisionShape(this.mob.level, pos);
        if (shape.isEmpty()) return;
        var panel = shape.bounds();
        if (panel.maxX - panel.minX < panel.maxZ - panel.minZ)
            x = pos.getX() + (panel.minX >= 0.5D ? panel.minX * 0.5D : (1.0D + panel.maxX) * 0.5D);
        else
            z = pos.getZ() + (panel.minZ >= 0.5D ? panel.minZ * 0.5D : (1.0D + panel.maxZ) * 0.5D);
        var targetBox = this.mob.getBoundingBox().move(x - this.mob.getX(), 0.0D, z - this.mob.getZ());
        if (!this.mob.level.noCollision(this.mob, targetBox)) return;
        this.mob.getMoveControl().setWantedPosition(x, this.mob.getY(), z, this.speedModifier);
    }

    /** A route node is a real, loaded vanilla ladder (not vines/scaffolding). */
    protected boolean isLadderNode(net.minecraft.world.level.pathfinder.Node node) {
        if (node == null) return false;
        net.minecraft.core.BlockPos at = new net.minecraft.core.BlockPos(node.x, node.y, node.z);
        return this.mob.level.hasChunkAt(at)
                && this.mob.level.getBlockState(at).is(net.minecraft.world.level.block.Blocks.LADDER);
    }

    private boolean touchesLadder() {
        net.minecraft.core.BlockPos feet = this.mob.blockPosition();
        return this.mob.onClimbable()
                || (this.mob.level.hasChunkAt(feet)
                    && this.mob.level.getBlockState(feet).is(net.minecraft.world.level.block.Blocks.LADDER));
    }

    private static boolean sameColumn(net.minecraft.world.level.pathfinder.Node a,
                                      net.minecraft.world.level.pathfinder.Node b) {
        return a != null && b != null && a.x == b.x && a.z == b.z;
    }

    private int ladderRouteDirection(net.minecraft.world.level.pathfinder.Node rung) {
        int index = this.path.getNextNodeIndex();
        net.minecraft.world.level.pathfinder.Node previous = index > 0
                ? this.path.getNode(index - 1) : null;
        net.minecraft.world.level.pathfinder.Node following = index + 1 < this.path.getNodeCount()
                ? this.path.getNode(index + 1) : null;
        return org.millenaire.common.pathing.LadderTransitionRules.routeDirection(
                previous == null ? rung.y : previous.y,
                sameColumn(previous, rung) && this.isLadderNode(previous),
                rung.y,
                following == null ? rung.y : following.y,
                sameColumn(following, rung) && this.isLadderNode(following));
    }

    private static double ladderTargetX(net.minecraft.world.level.pathfinder.Node rung,
                                        net.minecraft.world.level.block.state.BlockState state) {
        return rung.x + 0.5D; // onClimbable uses the cell, not collision with the panel
    }

    private static double ladderTargetZ(net.minecraft.world.level.pathfinder.Node rung,
                                        net.minecraft.world.level.block.state.BlockState state) {
        return rung.z + 0.5D; // keep the .6-wide body clear of the 3/16 ladder panel
    }

    private boolean canRiseInsideLadder() {
        // No bypassing an obstructed top exit or a low ceiling.
        return this.mob.level.noCollision(
                this.mob, this.mob.getBoundingBox().move(0.0D, 0.18D, 0.0D));
    }

    private void assistLadderRung(net.minecraft.world.level.pathfinder.Node next,
                                  net.minecraft.world.level.block.state.BlockState state) {
        double targetX = ladderTargetX(next, state);
        double targetZ = ladderTargetZ(next, state);
        double ex = targetX - this.mob.getX();
        double ez = targetZ - this.mob.getZ();
        double horizontalErrorSquared = ex * ex + ez * ez;
        if (!org.millenaire.common.pathing.LadderTransitionRules.shouldApproach(
                true, horizontalErrorSquared)) return;

        net.minecraft.core.BlockPos feet = this.mob.blockPosition();
        boolean sameColumn = next.x == feet.getX() && next.z == feet.getZ();
        boolean contact = this.touchesLadder();
        // A first rung above the ground may require a normal jump before
        // contact. Do not take movement ownership while still below that rung.
        if (!contact && next.y - this.mob.getY() > 0.5D) return;
        int direction = this.ladderRouteDirection(next);
        boolean reached = org.millenaire.common.pathing.LadderTransitionRules.reachedRung(
                direction, this.mob.getY(), next.y, horizontalErrorSquared);

        // GroundPathNavigation considers close-by Y coordinates "reached"
        // even when there is another vertical rung to go. Conversely it can
        // lag one rung and its negative dy would push an ascending mob DOWN.
        // Advance only a rung that the mob really climbed/descended past.
        if (contact && sameColumn && reached) {
            this.path.advance();
            if (this.path.isDone()) return;
            next = this.path.getNextNode();
            if (!this.isLadderNode(next)) {
                this.assistLeavingLadder(next);
                return;
            }
            net.minecraft.core.BlockPos target = new net.minecraft.core.BlockPos(
                    next.x, next.y, next.z);
            state = this.mob.level.getBlockState(target);
            targetX = ladderTargetX(next, state);
            targetZ = ladderTargetZ(next, state);
            ex = targetX - this.mob.getX();
            ez = targetZ - this.mob.getZ();
            horizontalErrorSquared = ex * ex + ez * ez;
            sameColumn = next.x == feet.getX() && next.z == feet.getZ();
            direction = this.ladderRouteDirection(next);
            reached = org.millenaire.common.pathing.LadderTransitionRules.reachedRung(
                    direction, this.mob.getY(), next.y, horizontalErrorSquared);
        }

        net.minecraft.world.phys.Vec3 velocity = this.mob.getDeltaMovement();
        double vy = velocity.y;
        if (org.millenaire.common.pathing.LadderTransitionRules.shouldClimb(
                direction, sameColumn, contact, reached, horizontalErrorSquared)) {
            if (direction > 0) {
                if (this.canRiseInsideLadder()) vy = Math.max(0.20D, velocity.y);
            } else {
                vy = Math.min(-0.12D, velocity.y);
            }
        }
        // Approach the surface even before feet reach the ladder. Apply no
        // upward/downward force until contact and column alignment are real.
        this.ladderMotionControlled = true;
        this.mob.setDeltaMovement(
                org.millenaire.common.pathing.LadderTransitionRules.approachSpeed(ex),
                vy,
                org.millenaire.common.pathing.LadderTransitionRules.approachSpeed(ez));
        this.mob.hasImpulse = true;
        if (org.millenaire.common.config.MillConfigValues.LogPathing >= 2
                && this.mob.level.getGameTime() >= this.nextLadderTraceTick) {
            this.nextLadderTraceTick = this.mob.level.getGameTime() + 40L;
            org.millenaire.common.utilities.MillLog.major(this.mob,
                    "LADDER-ASSIST next=" + next.asBlockPos()
                    + " feet=" + feet
                    + " exactY=" + String.format(java.util.Locale.ROOT, "%.2f", this.mob.getY())
                    + " direction=" + direction
                    + " aligned=" + sameColumn
                    + " contact=" + contact
                    + " reached=" + reached
                    + " dy=" + String.format(java.util.Locale.ROOT, "%.2f", next.y - this.mob.getY())
                    + " velocityY=" + String.format(java.util.Locale.ROOT, "%.2f", vy)
                    + " path=" + this.path.getNextNodeIndex() + "/" + this.path.getNodeCount());
        }
    }

    /**
     * The last rung may be below the solid lip of the upper landing. Vanilla
     * navigation starts moving horizontally too soon and collides with it.
     * Stay on the shaft and climb to platform height, then hand control back
     * to vanilla movement. Never lift without touching a real ladder.
     */
    private void assistLeavingLadder(net.minecraft.world.level.pathfinder.Node exit) {
        int index = this.path.getNextNodeIndex();
        if (index <= 0 || exit == null) return;
        net.minecraft.world.level.pathfinder.Node previous = this.path.getNode(index - 1);
        if (!this.isLadderNode(previous)) return;
        int dx = exit.x - previous.x, dz = exit.z - previous.z;
        if (Math.abs(dx) > 1 || Math.abs(dz) > 1 || (dx == 0 && dz == 0)
                || Math.abs(exit.y - previous.y) > 1) return;
        net.minecraft.core.BlockPos shaft = new net.minecraft.core.BlockPos(
                previous.x, previous.y, previous.z);
        net.minecraft.world.level.block.state.BlockState state = this.mob.level.getBlockState(shaft);
        double ex = ladderTargetX(previous, state) - this.mob.getX();
        double ez = ladderTargetZ(previous, state) - this.mob.getZ();
        double distanceSquared = ex * ex + ez * ez;
        if (!org.millenaire.common.pathing.LadderTransitionRules.needsTopOut(
                true, this.touchesLadder(), previous.y, exit.y,
                this.mob.getY(), distanceSquared)) return;
        if (!this.canRiseInsideLadder()) return;
        net.minecraft.world.phys.Vec3 velocity = this.mob.getDeltaMovement();
        this.ladderMotionControlled = true;
        this.mob.setDeltaMovement(
                org.millenaire.common.pathing.LadderTransitionRules.approachSpeed(ex),
                Math.max(0.20D, velocity.y),
                org.millenaire.common.pathing.LadderTransitionRules.approachSpeed(ez));
        this.mob.hasImpulse = true;
        if (org.millenaire.common.config.MillConfigValues.LogPathing >= 2
                && this.mob.level.getGameTime() >= this.nextLadderTraceTick) {
            this.nextLadderTraceTick = this.mob.level.getGameTime() + 40L;
            org.millenaire.common.utilities.MillLog.major(this.mob,
                    "LADDER-EXIT lastRung=" + shaft
                    + " exit=" + exit.asBlockPos()
                    + " feet=" + this.mob.blockPosition()
                    + " exactY=" + String.format(java.util.Locale.ROOT, "%.2f", this.mob.getY())
                    + " velocityY=" + String.format(java.util.Locale.ROOT, "%.2f",
                            this.mob.getDeltaMovement().y));
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
