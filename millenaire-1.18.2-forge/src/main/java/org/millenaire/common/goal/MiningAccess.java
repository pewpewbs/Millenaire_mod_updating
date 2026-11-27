package org.millenaire.common.goal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.generic.GoalGenericMining;
import org.millenaire.common.pathing.PathWorldSnapshot;
import org.millenaire.common.pathing.atomicstryker.AStarConfig;
import org.millenaire.common.pathing.atomicstryker.AStarStatic;
import org.millenaire.common.pathing.atomicstryker.TransitionClearanceRules;
import org.millenaire.common.utilities.ThreadSafeUtilities;
import org.millenaire.common.utilities.MillCommonUtilities;

/** Server-side mining stands. One small, transient cache per working miner. */
public final class MiningAccess {
    private static final int CAPACITY = 48;
    // Covers the bundled mining ranges. A custom longer reach can still be used from the live position.
    private static final int SEARCH_RADIUS = 5;
    private static final List<BlockPos> OFFSETS = offsets();
    private record Key(BlockPos source, int range, AStarConfig config) {}
    private record Entry(BlockState sourceState, long epoch, long expires, BlockPos stand) {}
    private final LinkedHashMap<Key, Entry> entries = new LinkedHashMap<>(16, .75f, true);
    private BlockGetter lastWorld;

    public static boolean isMining(Goal goal) {
        return goal instanceof GoalGenericMining || goal instanceof GoalMinerMineResource;
    }

    /** Keep the original random distribution among accessible registered sources. */
    public static Goal.GoalInformation choose(MillVillager miner, Goal goal, List<Goal.GoalInformation> candidates) {
        for (int remaining = candidates.size(); remaining > 0; remaining--) {
            int i = MillCommonUtilities.randomInt(remaining);
            Goal.GoalInformation candidate = candidates.get(i);
            if (miner.hasMiningAccess(goal, candidate.getDest())) return candidate;
            candidates.set(i, candidates.get(remaining - 1));
        }
        return null;
    }

    private static List<BlockPos> offsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(-SEARCH_RADIUS, -SEARCH_RADIUS, -SEARCH_RADIUS,
                SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS)) offsets.add(pos.immutable());
        offsets.sort(Comparator.comparingDouble(pos -> pos.distSqr(BlockPos.ZERO)));
        return List.copyOf(offsets);
    }

    public BlockPos first(MillVillager miner, Goal goal, BlockPos source) {
        if (!(miner.level instanceof ServerLevel world) || !world.hasChunkAt(source)) return null;
        return first(world, source, goal.range(miner), goal.getPathingConfig(miner),
                world.getGameTime(), PathWorldSnapshot.knownMutationEpoch(world));
    }

    BlockPos first(BlockGetter world, BlockPos source, int range, AStarConfig config, long now, long epoch) {
        if (lastWorld != world) {
            entries.clear();
            lastWorld = world;
        }
        BlockState state = world.getBlockState(source);
        Key key = new Key(source, range, config);
        Entry entry = entries.get(key);
        if (entry != null && state == entry.sourceState && entry.epoch == epoch && now < entry.expires)
            return entry.stand;
        BlockPos stand = null;
        if (!enclosed(world, source)) {
            for (BlockPos offset : OFFSETS) {
                if (offset.distSqr(BlockPos.ZERO) > (range + 1.5D) * (range + 1.5D)) break;
                BlockPos candidate = source.offset(offset);
                if (canWorkFrom(world, source, candidate, range, config)) {
                    stand = candidate;
                    break;
                }
            }
        }
        remember(key, new Entry(state, epoch, now + 40, stand));
        return stand;
    }

    public void retryLater(MillVillager miner, Goal goal, BlockPos source) {
        if (!(miner.level instanceof ServerLevel world) || !world.hasChunkAt(source)) return;
        remember(new Key(source, goal.range(miner), goal.getPathingConfig(miner)), new Entry(world.getBlockState(source),
                PathWorldSnapshot.knownMutationEpoch(world), world.getGameTime() + 160, null));
    }

    private void remember(Key key, Entry entry) {
        entries.put(key, entry);
        if (entries.size() > CAPACITY) entries.remove(entries.keySet().iterator().next());
    }

    private static boolean enclosed(BlockGetter world, BlockPos source) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = source.relative(direction);
            if (world instanceof Level level && !level.hasChunkAt(neighbor)) return false;
            if (!world.getBlockState(neighbor).isCollisionShapeFullBlock(world, neighbor)) return false;
        }
        return true;
    }

    /** Only on route failure. One multi-target search can approach a different face of the source. */
    public static Set<BlockPos> alternatives(BlockGetter world, BlockPos source, int range, AStarConfig config) {
        Set<BlockPos> stands = new LinkedHashSet<>();
        if (enclosed(world, source)) return stands;
        for (BlockPos offset : OFFSETS) {
            if (offset.distSqr(BlockPos.ZERO) > (range + 1.5D) * (range + 1.5D)) break;
            BlockPos candidate = source.offset(offset);
            if (canWorkFrom(world, source, candidate, range, config)) stands.add(candidate);
        }
        return stands;
    }

    public static boolean canWorkFrom(BlockGetter world, BlockPos source, BlockPos stand, int range, AStarConfig config) {
        if (range <= 0 || !WorkAccess.loadedBetween(world, stand, source)) return false;
        try {
            if (!AStarStatic.isViable(world, stand.getX(), stand.getY(), stand.getZ(), 0, config)) return false;
        } catch (ThreadSafeUtilities.ChunkAccessException unloaded) {
            return false;
        }
        double floor = WalkNodeEvaluator.getFloorLevel(world, stand);
        if (floor <= stand.getY() - 1 || floor > stand.getY()) return false;
        Vec3 feet = new Vec3(stand.getX() + .5, floor, stand.getZ() + .5);
        double half = TransitionClearanceRules.VILLAGER_WIDTH * .5;
        AABB body = new AABB(feet.x - half, floor, feet.z - half, feet.x + half,
                floor + TransitionClearanceRules.VILLAGER_HEIGHT, feet.z + half);
        for (int y = stand.getY(); y < body.maxY; y++) {
            BlockPos cell = new BlockPos(stand.getX(), y, stand.getZ());
            BlockState state = world.getBlockState(cell);
            if (state.isAir()) continue;
            for (AABB box : state.getCollisionShape(world, cell).toAabbs())
                if (body.intersects(box.move(cell))) return false;
        }
        return WorkAccess.canAccessResource(world, feet, source, range);
    }
}
