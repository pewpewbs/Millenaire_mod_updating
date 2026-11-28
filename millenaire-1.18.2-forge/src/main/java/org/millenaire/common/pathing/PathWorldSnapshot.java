package org.millenaire.common.pathing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.material.FluidState;
import org.millenaire.common.pathing.atomicstryker.AStarNode;

/** Private copies of block palettes. Retains no Level, chunk or block entity. */
public final class PathWorldSnapshot implements BlockGetter {
    // Up to four distinct detached snapshots may be recalled within the SAME
    // game tick. No references or copies survive into subsequent ticks.
    // Both maps are accessed only under SNAPSHOT_LOCK; callback invalidations
    // and server-thread captures are serialized at this boundary.
    private static final Object SNAPSHOT_LOCK = new Object();
    private static final Map<ServerLevel, SnapshotCaptureIndex<PathWorldSnapshot>>
            CAPTURE_INDICES = new java.util.WeakHashMap<>();
    private static final Map<ServerLevel, SnapshotSectionPool<PalettedContainer<BlockState>>>
            SECTION_POOLS = new java.util.WeakHashMap<>();
    // Signals from KNOWN mutation callbacks only; not a universal world
    // block-revision stream. Used to wake failed-route retries early.
    private static final Map<ServerLevel, Long> KNOWN_MUTATION_EPOCHS =
            new java.util.WeakHashMap<>();

    public static long knownMutationEpoch(ServerLevel level) {
        if (level == null) return 0L;
        synchronized (SNAPSHOT_LOCK) {
            return KNOWN_MUTATION_EPOCHS.getOrDefault(level, 0L);
        }
    }

    private static void advanceKnownMutation(ServerLevel level) {
        KNOWN_MUTATION_EPOCHS.put(level,
                KNOWN_MUTATION_EPOCHS.getOrDefault(level, 0L) + 1L);
    }
    // Max four snapshots in a tick, but never more than 256 chunk visits or
    // 1024 section copies in total. These are process-local, per-world caps.
    private static final int MAX_SNAPSHOTS_PER_TICK = 4;
    private static final int MAX_CHUNK_VISITS_PER_TICK = 256;
    private static final int MAX_SECTION_COPIES_PER_TICK = 1024;
    private final Map<Long, PalettedContainer<BlockState>> sections;
    private final Set<Long> chunks;
    // Snapshot-local only: geometry is immutable even if the real world
    // changes during a worker search. Never use this memo on live Level.
    private final SnapshotColumnMemo simpleFlatColumns = new SnapshotColumnMemo(2048);
    private final int minHeight, height;
    /**
     * Values are ALREADY detached copies created on the server thread. Never
     * copy again here: several snapshots may share these read-only palettes.
     * No worker mutates the palettes.
     */
    PathWorldSnapshot(Map<Long, PalettedContainer<BlockState>> detached,
                      Set<Long> loaded, int min, int height) {
        this.sections = Map.copyOf(detached); this.chunks = Set.copyOf(loaded);
        this.minHeight = min; this.height = height;
    }
    public static PathWorldSnapshot capture(ServerLevel level, AStarNode start, AStarNode end) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Path snapshot must be captured on server thread");
        long began=System.nanoTime();
        long tick=level.getGameTime();
        int minX = (Math.min(start.x, end.x) >> 4) - 2, maxX = (Math.max(start.x, end.x) >> 4) + 2;
        int minZ = (Math.min(start.z, end.z) >> 4) - 2, maxZ = (Math.max(start.z, end.z) >> 4) + 2;
        // Reject requests which can NEVER fit our bounded snapshot BEFORE
        // reserving the world's one-capture-per-tick budget. Otherwise a
        // distant impossible query starves nearby villagers in the same tick.
        if ((long) (maxX - minX + 1) * (maxZ - minZ + 1) > 256L) {
            NavigationMetrics.snapshotAreaRejected();
            NavigationMetrics.deferred();
            return null;
        }
        int chunkVisits = (maxX - minX + 1) * (maxZ - minZ + 1);
        SnapshotSectionPool<PalettedContainer<BlockState>> pool;
        SnapshotCaptureIndex<PathWorldSnapshot> index;
        boolean additionalSnapshotInTick;
        long captureRevision;
        synchronized(SNAPSHOT_LOCK) {
            // Consult poison BEFORE reusing any snapshot. Known PRE-change
            // notifications disable full-snapshot sharing for the tick.
            pool = SECTION_POOLS.computeIfAbsent(level, unused -> new SnapshotSectionPool<>(
                    MAX_SNAPSHOTS_PER_TICK, MAX_CHUNK_VISITS_PER_TICK,
                    MAX_SECTION_COPIES_PER_TICK));
            index = CAPTURE_INDICES.computeIfAbsent(level,
                    unused -> new SnapshotCaptureIndex<>(MAX_SNAPSHOTS_PER_TICK));
            if (pool.mayReuseSnapshots(tick)) {
                var match = index.find(tick, minX, maxX, minZ, maxZ);
                if (match != null) {
                    NavigationMetrics.snapshotReused();
                    if (match.nonLatest()) NavigationMetrics.snapshotNonLatestReused();
                    return match.snapshot();
                }
            }
            if (!pool.begin(tick,chunkVisits)) {
                NavigationMetrics.snapshotBudgetDeferred();
                return null;
            }
            additionalSnapshotInTick = pool.usedSnapshots() > 1;
            captureRevision = pool.revision(tick);
        }
        Map<Long, PalettedContainer<BlockState>> detached = new HashMap<>();
        Set<Long> loaded = new HashSet<>();
        int reusedSections = 0;
        int copiedSections = 0;
        for(int x=minX;x<=maxX;x++) for(int z=minZ;z<=maxZ;z++) {
            var chunk = level.getChunkSource().getChunkNow(x,z);
            if(chunk==null) continue; // Never load/generate a chunk for a background search.
            loaded.add(ChunkPos.asLong(x,z));
            var sections=chunk.getSections();
            for(int i=0;i<sections.length;i++) if(!sections[i].hasOnlyAir()) {
                if(detached.size()>=1024) {
                    NavigationMetrics.snapshotSectionRejected();
                    NavigationMetrics.deferred();
                    return null;
                }
                long key = SectionPos.asLong(x,level.getSectionYFromSectionIndex(i),z);
                PalettedContainer<BlockState> source = sections[i].getStates();
                PalettedContainer<BlockState> copy = pool.get(tick,key,source);
                if(copy==null) {
                    if(pool.isPoisoned(tick,key)) {
                        NavigationMetrics.snapshotPoisonedBypass();
                    }
                    if(!pool.canCopy(tick)) {
                        NavigationMetrics.snapshotSectionRejected();
                        NavigationMetrics.deferred();
                        return null;
                    }
                    copy = source.copy();  // exactly once per section per tick
                    pool.remember(tick,key,source,copy);
                    copiedSections++;
                } else {
                    reusedSections++;
                }
                detached.put(key,copy);
            }
        }
        var result=new PathWorldSnapshot(detached,loaded,level.getMinBuildHeight(),level.getHeight());
        synchronized(SNAPSHOT_LOCK) {
            // Known events can be raised off the normal snapshot call path.
            // Never publish a copy built across an observed invalidation.
            if(pool.revision(tick)!=captureRevision) {
                NavigationMetrics.snapshotMutationRaceRejected();
                NavigationMetrics.deferred();
                return null;
            }
            // After any known mutation in this tick, a new snapshot may still
            // be used by its own search, but must NOT enter the shared index.
            if (pool.mayReuseSnapshots(tick)) {
                index.remember(tick, minX, maxX, minZ, maxZ, result);
            }
        }
        NavigationMetrics.sections(copiedSections);
        NavigationMetrics.snapshotSectionsReused(reusedSections);
        if(additionalSnapshotInTick) NavigationMetrics.snapshotAdditionalCaptured();
        NavigationMetrics.record(1,System.nanoTime()-began);
        return result;
    }
    /**
     * Best-effort invalidation for Forge block-change notifications. It is
     * deliberately NOT a proof of section immutability across server ticks:
     * some vanilla and modded writes do not publish BlockEvent.
     *
     * All eviction paths keep the existing per-tick copy/visit budgets. A
     * single block edit therefore cannot reset limits and cause a CPU spike.
     */
    public static void onKnownBlockMutation(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return;
        long tick = level.getGameTime();
        int chunkX = pos.getX() >> 4, chunkZ = pos.getZ() >> 4;
        long key = SectionPos.asLong(chunkX, pos.getY() >> 4, chunkZ);
        synchronized (SNAPSHOT_LOCK) {
            advanceKnownMutation(level);
            SnapshotSectionPool<PalettedContainer<BlockState>> pool = SECTION_POOLS.computeIfAbsent(
                    level, unused -> new SnapshotSectionPool<>(
                            MAX_SNAPSHOTS_PER_TICK, MAX_CHUNK_VISITS_PER_TICK,
                            MAX_SECTION_COPIES_PER_TICK));
            if (pool.invalidateSection(tick, key)) {
                NavigationMetrics.snapshotKnownSectionInvalidated();
            }
            // Clear the entire bounded index, including earlier supersets.
            // This is conservative even if the edited chunk is outside them.
            SnapshotCaptureIndex<PathWorldSnapshot> index = CAPTURE_INDICES.get(level);
            if (index != null) {
                NavigationMetrics.snapshotKnownCapturesInvalidated(index.invalidate(tick));
            }
        }
    }

    /**
     * Chunk unload/load and multi-block moves can change more than one
     * section. Drop cached copies conservatively while retaining the
     * admission counters to prevent runaway copying during repeated edits.
     */
    public static void onKnownWorldMutation(ServerLevel level) {
        if (level == null) return;
        long tick = level.getGameTime();
        synchronized (SNAPSHOT_LOCK) {
            advanceKnownMutation(level);
            SnapshotSectionPool<PalettedContainer<BlockState>> pool = SECTION_POOLS.computeIfAbsent(
                    level, unused -> new SnapshotSectionPool<>(
                            MAX_SNAPSHOTS_PER_TICK, MAX_CHUNK_VISITS_PER_TICK,
                            MAX_SECTION_COPIES_PER_TICK));
            NavigationMetrics.snapshotKnownSectionsInvalidated(pool.invalidateAll(tick));
            SnapshotCaptureIndex<PathWorldSnapshot> index = CAPTURE_INDICES.get(level);
            if (index != null) {
                NavigationMetrics.snapshotKnownCapturesInvalidated(index.invalidate(tick));
            }
        }
    }

    /** Do not retain old palettes when a server world is closed. */
    public static void onWorldUnload(ServerLevel level) {
        if (level == null) return;
        synchronized (SNAPSHOT_LOCK) {
            CAPTURE_INDICES.remove(level);
            SECTION_POOLS.remove(level);
            KNOWN_MUTATION_EPOCHS.remove(level);
        }
    }

    /** Results belong to this detached snapshot, never to a live Level. */
    public Boolean getSimpleFlatColumn(int x, int y, int z) {
        return simpleFlatColumns.get(BlockPos.asLong(x, y, z));
    }

    public void rememberSimpleFlatColumn(int x, int y, int z, boolean simple) {
        simpleFlatColumns.remember(BlockPos.asLong(x, y, z), simple);
    }

    /**
     * Primitive fast path for hot A-star and JPS block-state reads. Identical bounds
     * and palette rules to BlockGetter.getBlockState(BlockPos), but avoids
     * allocation of one BlockPos object per neighbor / occupancy check.
     *
     * Immutable detached palettes: this never loads or reads live chunks.
     */
    public BlockState getBlockState(int x, int y, int z) {
        if (y < minHeight || y >= minHeight + height
                || !chunks.contains(ChunkPos.asLong(x >> 4, z >> 4)))
            return Blocks.BEDROCK.defaultBlockState();
        var palette = sections.get(SectionPos.asLong(x >> 4, y >> 4, z >> 4));
        return palette == null ? Blocks.AIR.defaultBlockState()
                : palette.get(x & 15, y & 15, z & 15);
    }

    @Override public BlockState getBlockState(BlockPos pos) {
        return getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }
    @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
    @Override public BlockEntity getBlockEntity(BlockPos pos) { return null; }
    @Override public int getMinBuildHeight() { return minHeight; }
    @Override public int getHeight() { return height; }
}
