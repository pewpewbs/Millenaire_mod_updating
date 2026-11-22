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
        int minX = (Math.min(start.x, end.x) >> 4) - 2, maxX = (Math.max(start.x, end.x) >> 4) + 2;
        int minZ = (Math.min(start.z, end.z) >> 4) - 2, maxZ = (Math.max(start.z, end.z) >> 4) + 2;
        // Extremely distant goals are approached by the native partial route first.
        if ((long)(maxX-minX+1)*(maxZ-minZ+1)>256) return null;
        Map<Long, PalettedContainer<BlockState>> source = new HashMap<>();
        Set<Long> loaded = new HashSet<>();
        for(int x=minX;x<=maxX;x++) for(int z=minZ;z<=maxZ;z++) {
            var chunk = level.getChunkSource().getChunkNow(x,z);
            if(chunk==null) continue; // Never load/generate a chunk for a background search.
            loaded.add(ChunkPos.asLong(x,z));
            var sections=chunk.getSections();
            for(int i=0;i<sections.length;i++) if(!sections[i].hasOnlyAir())
                source.put(SectionPos.asLong(x,level.getSectionYFromSectionIndex(i),z),sections[i].getStates());
        }
        return new PathWorldSnapshot(source,loaded,level.getMinBuildHeight(),level.getHeight());
    }
    @Override public BlockState getBlockState(BlockPos pos) {
        if(pos.getY()<minHeight || pos.getY()>=minHeight+height
                || !chunks.contains(ChunkPos.asLong(pos.getX()>>4,pos.getZ()>>4)))
            return Blocks.BEDROCK.defaultBlockState();
        var palette=sections.get(SectionPos.asLong(pos.getX()>>4,pos.getY()>>4,pos.getZ()>>4));
        return palette==null?Blocks.AIR.defaultBlockState():palette.get(pos.getX()&15,pos.getY()&15,pos.getZ()&15);
    }
    @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
    @Override public BlockEntity getBlockEntity(BlockPos pos) { return null; }
    @Override public int getMinBuildHeight() { return minHeight; }
    @Override public int getHeight() { return height; }
}
