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
        Long previous = OPENED.computeIfAbsent(level, ignored -> new HashMap<>())
            .put(pos.immutable(), level.getGameTime());
        if (previous == null) {
            trace(level, pos, "OPENED");
        } else if (level.getGameTime() % 100L == 0L) {
            trace(level, pos, "RENEWED");
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
            if (!state.getValue(DoorBlock.OPEN)) {
                ((DoorBlock) state.getBlock()).setOpen(null, level, state, pos, true);
                if (level.getBlockState(pos).getValue(DoorBlock.OPEN)) remember(level, pos);
            } else if (registered != null && registered.containsKey(pos)) {
                remember(level, pos);
            }
        } else if (state.getBlock() instanceof FenceGateBlock) {
            if (state.getValue(FenceGateBlock.POWERED)) return;
            if (!state.getValue(FenceGateBlock.OPEN)) {
                level.setBlock(pos, state.setValue(FenceGateBlock.OPEN, true), 3);
                if (level.getBlockState(pos).getValue(FenceGateBlock.OPEN)) remember(level, pos);
            } else if (registered != null && registered.containsKey(pos)) {
                remember(level, pos);
            }
        }
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

            // Only living entities physically intersecting the doorway can
            // delay closing. Nearby livestock (or a resident already outside)
            // must not hold an entrance open for multiple seconds.
            AABB portal = new AABB(pos).inflate(0.12D, 0.75D, 0.12D);
            if (!level.getEntitiesOfClass(LivingEntity.class, portal,
                    LivingEntity::isAlive).isEmpty()) {
                if (level.getGameTime() % 100L == 0L) trace(level, pos, "OCCUPIED");
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
