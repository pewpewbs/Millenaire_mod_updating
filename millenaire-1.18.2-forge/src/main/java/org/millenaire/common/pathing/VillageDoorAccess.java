package org.millenaire.common.pathing;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.millenaire.common.utilities.BlockItemUtilities;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.utilities.MillLog;

/**
 * A shared, server-only lease for doors and fence gates opened by Millenaire residents.
 * One resident must not close a doorway while other residents are still approaching it.
 * Player-opened doors are not registered here and are therefore not auto-closed.
 */
@Mod.EventBusSubscriber(modid = "millenaire")
public final class VillageDoorAccess {
    // Both livestock-pen doors and gates close promptly once the crossing is
    // clear. A route follower refreshes the lease while still approaching.
    private static final long GATE_IDLE_TICKS = 10L;
    private static final long DOOR_IDLE_TICKS = 10L;
    // A manual door/gate interaction takes precedence over NPC leases.
    // Keep a short grace period for players who close a door as well, so a
    // nearby resident cannot instantly undo their interaction.
    private static final long PLAYER_CONTROL_TICKS = 100L;
    private static final Map<ServerLevel, Map<BlockPos, Long>> OPENED = new WeakHashMap<>();
    private static final Map<ServerLevel, Map<BlockPos, Long>> PLAYER_CONTROL = new WeakHashMap<>();

    private VillageDoorAccess() {}

    private static void trace(ServerLevel level, BlockPos pos, String reason) {
        if (MillConfigValues.LogPathing >= 2) {
            MillLog.major("VillageDoorAccess", "DOOR-LEASE " + reason
                    + " pos=" + pos + " tick=" + level.getGameTime());
        }
    }

    private static BlockPos canonical(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof DoorBlock
                && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        return pos;
    }

    private static void remember(ServerLevel level, BlockPos pos) {
        remember(level, pos, null, "LEGACY");
    }

    private static void remember(ServerLevel level, BlockPos pos,
            LivingEntity actor, String source) {
        Long previous = OPENED.computeIfAbsent(level, ignored -> new HashMap<>())
            .put(pos.immutable(), level.getGameTime());
        if (previous == null) {
            // OPENED only happens on a real block transition or adoption.
            // Identify which specific NPC and route type causes re-opening
            // after the manager just closed this same doorway.
            String origin = " source=" + source;
            if (actor != null) {
                origin += " actorId=" + actor.getId()
                        + " actorName=" + actor.getName().getString()
                        + " actorFeet=" + actor.blockPosition();
            }
            trace(level, pos, "OPENED" + origin);
        } else if (level.getGameTime() % 100L == 0L) {
            // With only the door position, we cannot distinguish a resident
            // legitimately crossing it from one that renews indefinitely
            // while stalled. Keep actor/source at the sampled renewal tick.
            String origin = " source=" + source;
            if (actor != null) {
                origin += " actorId=" + actor.getId()
                        + " actorName=" + actor.getName().getString()
                        + " actorFeet=" + actor.blockPosition();
            }
            trace(level, pos, "RENEWED" + origin);
        }
    }

    private static boolean isPlayerControlled(ServerLevel level, BlockPos pos) {
        Map<BlockPos, Long> overrides = PLAYER_CONTROL.get(level);
        if (overrides == null) return false;
        Long until = overrides.get(pos);
        if (until == null) return false;
        if (level.getGameTime() < until) return true;
        overrides.remove(pos);
        if (overrides.isEmpty()) PLAYER_CONTROL.remove(level);
        return false;
    }

    /**
     * Players can take control of wooden doors and gates without competing
     * with an old NPC closing lease. This event precedes the vanilla toggle;
     * it does not cancel or perform that toggle itself.
     */
    @SubscribeEvent
    public static void onPlayerUseDoor(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || !(event.getWorld() instanceof ServerLevel level)) return;
        BlockPos raw = event.getPos();
        if (!level.hasChunkAt(raw)) return;
        BlockState sample = level.getBlockState(raw);
        BlockPos pos = canonical(level, raw, sample);
        BlockState state = level.getBlockState(pos);
        boolean wooden = BlockItemUtilities.isWoodenDoor(state.getBlock());
        boolean gate = state.getBlock() instanceof FenceGateBlock;
        if (!wooden && !gate) return;

        // Remove the NPC's lease BEFORE the player's click changes OPEN.
        // Otherwise the shared world-tick closer would override the player.
        Map<BlockPos, Long> opened = OPENED.get(level);
        if (opened != null) {
            opened.remove(pos);
            if (opened.isEmpty()) OPENED.remove(level);
        }
        PLAYER_CONTROL.computeIfAbsent(level, ignored -> new HashMap<>())
            .put(pos.immutable(), level.getGameTime() + PLAYER_CONTROL_TICKS);
        trace(level, pos, "PLAYER-OVERRIDE state=" + state);
    }

    /** Adopt an old saved door-to-close hint after upgrading from per-villager handling. */
    public static void adoptLegacy(Level world, BlockPos raw) {
        if (!(world instanceof ServerLevel level) || !level.hasChunkAt(raw)) return;
        BlockState sample = level.getBlockState(raw);
        BlockPos pos = canonical(level, raw, sample);
        BlockState state = level.getBlockState(pos);
        if (isPlayerControlled(level, pos)) return;
        if (BlockItemUtilities.isWoodenDoor(state.getBlock())
                && state.getValue(DoorBlock.OPEN) && !state.getValue(DoorBlock.POWERED)) {
            remember(level, pos);
        } else if (state.getBlock() instanceof FenceGateBlock
                && state.getValue(FenceGateBlock.OPEN) && !state.getValue(FenceGateBlock.POWERED)) {
            remember(level, pos);
        }
    }

    /** Only the server may change world blocks. Call for nodes near the actual route. */
    public static void openForResident(Level world, BlockPos raw) {
        openForResident(world, raw, null, "UNKNOWN");
    }

    /** Actor and reason are recorded ONLY on successful first opening. */
    public static void openForResident(Level world, BlockPos raw,
            LivingEntity actor, String source) {
        if (!(world instanceof ServerLevel level) || !level.hasChunkAt(raw)) return;
        BlockState sample = level.getBlockState(raw);
        BlockPos pos = canonical(level, raw, sample);
        BlockState state = level.getBlockState(pos);
        if (isPlayerControlled(level, pos)) {
            if (level.getGameTime() % 100L == 0L) trace(level, pos, "PLAYER-OVERRIDE-SKIP");
            return;
        }
        Map<BlockPos, Long> registered = OPENED.get(level);

        if (BlockItemUtilities.isWoodenDoor(state.getBlock())) {
            if (state.getValue(DoorBlock.POWERED)) return;
            Boolean passageOpen = routeDoorState(actor, pos, state);
            if (Boolean.FALSE.equals(passageOpen)) {
                // Legacy plans can have a door rotated across the entrance.
                // In that layout OPEN blocks the route and CLOSED clears it.
                // Never swing the panel into an occupant or fight player control.
                if (state.getValue(DoorBlock.OPEN)) {
                    var shape = state.setValue(DoorBlock.OPEN, false).getCollisionShape(level, pos);
                    var closing = shape.bounds().move(pos).expandTowards(0.0D, 1.0D, 0.0D);
                    if (!level.getEntitiesOfClass(LivingEntity.class, closing,
                            entity -> entity.isAlive()).isEmpty()) return;
                    ((DoorBlock)state.getBlock()).setOpen(null, level, state, pos, false);
                    trace(level, pos, "PASSAGE-CLOSED source=" + source);
                }
                if (registered != null) registered.remove(pos);
                return;
            }
            if (!state.getValue(DoorBlock.OPEN)) {
                ((DoorBlock) state.getBlock()).setOpen(null, level, state, pos, true);
                if (level.getBlockState(pos).getValue(DoorBlock.OPEN)) remember(level, pos, actor, source);
            } else if (registered != null && registered.containsKey(pos)) {
                remember(level, pos, actor, source);
            }
        } else if (state.getBlock() instanceof FenceGateBlock) {
            if (state.getValue(FenceGateBlock.POWERED)) return;
            if (!state.getValue(FenceGateBlock.OPEN)) {
                level.setBlock(pos, state.setValue(FenceGateBlock.OPEN, true), 3);
                if (level.getBlockState(pos).getValue(FenceGateBlock.OPEN)) remember(level, pos, actor, source);
            } else if (registered != null && registered.containsKey(pos)) {
                remember(level, pos, actor, source);
            }
        }
    }

    private static Boolean routeDoorState(LivingEntity actor, BlockPos door, BlockState state) {
        if (!(actor instanceof net.minecraft.world.entity.Mob mob)) return null;
        var route = mob.getNavigation().getPath();
        if (route == null || route.isDone()) return null;
        for (int i = Math.max(0, route.getNextNodeIndex() - 1);
                i < Math.min(route.getNodeCount(), route.getNextNodeIndex() + 3); i++) {
            var node = route.getNode(i);
            if (node.x != door.getX() || node.z != door.getZ()
                    || Math.abs(node.y - door.getY()) > 1) continue;
            // Match the directed edge currently being traversed. A route can
            // turn inside a door cell; its approach and exit need different states.
            boolean leaving = i < route.getNextNodeIndex();
            var before = leaving ? node : i > 0 ? route.getNode(i - 1) : null;
            var after = leaving && i + 1 < route.getNodeCount() ? route.getNode(i + 1) : node;
            double dx = after.x + 0.5D - (before == null ? actor.getX() : before.x + 0.5D);
            double dz = after.z + 0.5D - (before == null ? actor.getZ() : before.z + 0.5D);
            if (dx * dx + dz * dz < 0.01D) return null;
            return DoorPassageGeometry.openForDirection(state, dx, dz);
        }
        return null;
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.world instanceof ServerLevel level)
                || level.getGameTime() % 5L != 0L) return;
        Map<BlockPos, Long> playerOverrides = PLAYER_CONTROL.get(level);
        if (playerOverrides != null) {
            long now = level.getGameTime();
            playerOverrides.entrySet().removeIf(entry -> now >= entry.getValue());
            if (playerOverrides.isEmpty()) PLAYER_CONTROL.remove(level);
        }
        Map<BlockPos, Long> doors = OPENED.get(level);
        if (doors == null || doors.isEmpty()) return;

        Iterator<Map.Entry<BlockPos, Long>> iterator = doors.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Long> entry = iterator.next();
            BlockPos pos = entry.getKey();
            if (!level.hasChunkAt(pos)) {
                iterator.remove(); // Never force a chunk load just to close a door.
                continue;
            }
            // Another mod or player may have taken control after this lease.
            if (isPlayerControlled(level, pos)) {
                iterator.remove();
                continue;
            }
            BlockState state = level.getBlockState(pos);
            boolean wooden = BlockItemUtilities.isWoodenDoor(state.getBlock());
            boolean gate = state.getBlock() instanceof FenceGateBlock;
            if (!wooden && !gate) {
                iterator.remove();
                continue;
            }
            boolean open = wooden ? state.getValue(DoorBlock.OPEN) : state.getValue(FenceGateBlock.OPEN);
            boolean powered = wooden ? state.getValue(DoorBlock.POWERED) : state.getValue(FenceGateBlock.POWERED);
            if (!open || powered) {
                iterator.remove();
                continue;
            }
            long idleTicks = gate ? GATE_IDLE_TICKS : DOOR_IDLE_TICKS;
            if (level.getGameTime() - entry.getValue() < idleTicks) continue;

            // A wooden door's CLOSED collision panel is only a thin slice
            // of the block, and FACING/HINGE can put it on either side.
            // The previous whole-block AABB considered residents standing
            // inside the ADJACENT room to be "in the doorway" indefinitely,
            // causing doors to remain open long after they had been crossed.
            // Keep the gate's established geometry unchanged.
            AABB portal;
            if (wooden) {
                BlockState closed = state.setValue(DoorBlock.OPEN, false);
                net.minecraft.world.phys.shapes.VoxelShape shape =
                        closed.getCollisionShape(level, pos);
                if (shape.isEmpty()) {
                    // Unexpected custom door shape: avoid closing on an entity.
                    portal = new AABB(pos).inflate(0.12D, 0.75D, 0.12D);
                } else {
                    portal = shape.bounds()
                            .move(pos.getX(), pos.getY(), pos.getZ())
                            .inflate(0.10D, 0.85D, 0.10D);
                }
            } else {
                portal = new AABB(pos).inflate(0.12D, 0.75D, 0.12D);
            }
            if (!level.getEntitiesOfClass(LivingEntity.class, portal,
                    LivingEntity::isAlive).isEmpty()) {
                if (level.getGameTime() % 100L == 0L) trace(level, pos, "OCCUPIED-PANEL");
                continue;
            }

            trace(level, pos, "CLOSING state=" + state);
            if (wooden) {
                ((DoorBlock) state.getBlock()).setOpen(null, level, state, pos, false);
            } else {
                level.setBlock(pos, state.setValue(FenceGateBlock.OPEN, false), 3);
            }
            iterator.remove();
        }
        if (doors.isEmpty()) OPENED.remove(level);
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld() instanceof ServerLevel level) {
            OPENED.remove(level);
            PLAYER_CONTROL.remove(level);
        }
    }
}
