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
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.millenaire.common.utilities.BlockItemUtilities;

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
    private static final Map<ServerLevel, Map<BlockPos, Long>> OPENED = new WeakHashMap<>();

    private VillageDoorAccess() {}

    private static BlockPos canonical(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof DoorBlock
                && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        return pos;
    }

    private static void remember(ServerLevel level, BlockPos pos) {
        OPENED.computeIfAbsent(level, ignored -> new HashMap<>())
            .put(pos.immutable(), level.getGameTime());
    }

    /** Adopt an old saved door-to-close hint after upgrading from per-villager handling. */
    public static void adoptLegacy(Level world, BlockPos raw) {
        if (!(world instanceof ServerLevel level) || !level.hasChunkAt(raw)) return;
        BlockState sample = level.getBlockState(raw);
        BlockPos pos = canonical(level, raw, sample);
        BlockState state = level.getBlockState(pos);
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
                    LivingEntity::isAlive).isEmpty()) continue;

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
        if (event.getWorld() instanceof ServerLevel level) OPENED.remove(level);
    }
}
