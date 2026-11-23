package org.millenaire.common.pathing;

import java.lang.ref.WeakReference;
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
    // One snapshot may be shared by searches whose bounding rectangles fit
    // inside it during the SAME game tick. Never reuse across ticks: buildings,
    // doors and player edits can change collision states between ticks.
    private record Capture(long tick, int minX, int maxX, int minZ, int maxZ,
                           WeakReference<PathWorldSnapshot> snapshot) {}
    private static final Map<ServerLevel, Capture> LAST_CAPTURE = new java.util.WeakHashMap<>();
    private final Map<Long, PalettedContainer<BlockState>> sections;
    private final Set<Long> chunks;
    private final int minHeight, height;
    PathWorldSnapshot(Map<Long, PalettedContainer<BlockState>> source, Set<Long> loaded, int min, int height) {
        Map<Long, PalettedContainer<BlockState>> copied = new HashMap<>();
        source.forEach((key, palette) -> copied.put(key, palette.copy()));
        this.sections = Map.copyOf(copied); this.chunks = Set.copyOf(loaded);
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
        synchronized(LAST_CAPTURE) {
            Capture previous=LAST_CAPTURE.get(level);
            if(previous!=null && previous.tick()==tick) {
                PathWorldSnapshot shared=previous.snapshot().get();
                if(shared!=null && minX>=previous.minX() && maxX<=previous.maxX()
                        && minZ>=previous.minZ() && maxZ<=previous.maxZ()) {
                    NavigationMetrics.snapshotReused();
                    return shared;
                }
                NavigationMetrics.snapshotBudgetDeferred();
                return null;
            }
            LAST_CAPTURE.put(level,new Capture(tick,minX,maxX,minZ,maxZ,
                    new WeakReference<>(null)));
        }
        Map<Long, PalettedContainer<BlockState>> source = new HashMap<>();
        Set<Long> loaded = new HashSet<>();
        for(int x=minX;x<=maxX;x++) for(int z=minZ;z<=maxZ;z++) {
            var chunk = level.getChunkSource().getChunkNow(x,z);
            if(chunk==null) continue; // Never load/generate a chunk for a background search.
            loaded.add(ChunkPos.asLong(x,z));
            var sections=chunk.getSections();
            for(int i=0;i<sections.length;i++) if(!sections[i].hasOnlyAir()) {
                if(source.size()>=1024){NavigationMetrics.snapshotSectionRejected();NavigationMetrics.deferred();return null;}
                source.put(SectionPos.asLong(x,level.getSectionYFromSectionIndex(i),z),sections[i].getStates());
            }
        }
        var result=new PathWorldSnapshot(source,loaded,level.getMinBuildHeight(),level.getHeight());
        synchronized(LAST_CAPTURE) {
            Capture pending=LAST_CAPTURE.get(level);
            if(pending!=null && pending.tick()==tick) {
                LAST_CAPTURE.put(level,new Capture(tick,minX,maxX,minZ,maxZ,
                        new WeakReference<>(result)));
            }
        }
        NavigationMetrics.sections(source.size());NavigationMetrics.record(1,System.nanoTime()-began);
        return result;
    }
    /**
     * Primitive fast path for hot A*/JPS block-state reads. Identical bounds
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
