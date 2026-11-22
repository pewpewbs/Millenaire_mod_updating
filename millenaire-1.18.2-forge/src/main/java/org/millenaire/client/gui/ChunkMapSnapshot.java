package org.millenaire.client.gui;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.utilities.RegionChunkIndex;
import org.millenaire.common.village.Building;

/** Immutable debug-map data; server state is captured only on the server thread. */
public record ChunkMapSnapshot(ResourceKey<Level> dimension,int startX,int startZ,Set<Long> saved,
        Set<Long> loaded,Set<Long> forced,List<VillageArea> villages) {
    public static final int SIZE=80;
    public record VillageArea(int minX,int minZ,int maxX,int maxZ,String name,boolean lone) {
        public boolean contains(int x,int z) { return x>=minX && x<=maxX && z>=minZ && z<=maxZ; }
    }
    public ChunkMapSnapshot {
        saved=Set.copyOf(saved);loaded=Set.copyOf(loaded);forced=Set.copyOf(forced);villages=List.copyOf(villages);
    }
    private record Capture(ChunkMapSnapshot snapshot,Path regionFolder) {}
    public static CompletableFuture<ChunkMapSnapshot> request(MinecraftServer server,ResourceKey<Level> dimension,int centerX,int centerZ) {
        return server.submit(() -> capture(server,dimension,centerX,centerZ)).thenApplyAsync(capture -> {
            if (capture==null) return null;
            var s=capture.snapshot();
            try {
                Set<Long> saved=new HashSet<>(RegionChunkIndex.read(capture.regionFolder(),s.startX(),s.startZ(),SIZE));
                saved.addAll(s.loaded());
                return new ChunkMapSnapshot(s.dimension(),s.startX(),s.startZ(),saved,s.loaded(),s.forced(),s.villages());
            } catch (java.io.IOException error) { throw new java.util.concurrent.CompletionException(error); }
        },Util.ioPool());
    }
    private static Capture capture(MinecraftServer server,ResourceKey<Level> dimension,int centerX,int centerZ) {
        ServerLevel level=server.getLevel(dimension);
        if (level==null) return null;
        int startX=centerX-SIZE/2,startZ=centerZ-SIZE/2;
        Set<Long> loaded=new HashSet<>();
        for(int x=startX;x<startX+SIZE;x++)for(int z=startZ;z<startZ+SIZE;z++)
            if(level.getChunkSource().hasChunk(x,z))loaded.add(ChunkPos.asLong(x,z));
        var data=level.getDataStorage().get(ForcedChunksSavedData::load,ForcedChunksSavedData.FILE_ID);
        Set<Long> forced=collectForced(data);
        List<VillageArea> villages=new ArrayList<>();
        var world=Mill.getMillWorld(level);
        if(world!=null)for(Building building:world.allBuildings()) {
            if(!building.isTownhall || building.winfo==null || building.villageType==null)continue;
            var info=building.winfo;
            if(info.length<=0 || info.width<=0)continue;
            villages.add(new VillageArea(info.mapStartX>>4,info.mapStartZ>>4,(info.mapStartX+info.length-1)>>4,
                (info.mapStartZ+info.width-1)>>4,building.getVillageQualifiedName(),building.villageType.lonebuilding));
        }
        Path region=DimensionType.getStorageFolder(dimension,server.getWorldPath(LevelResource.ROOT)).resolve("region");
        return new Capture(new ChunkMapSnapshot(dimension,startX,startZ,loaded,loaded,forced,villages),region);
    }
    public static Set<Long> collectForced(ForcedChunksSavedData data) {
        if(data==null)return Set.of();
        Set<Long> result=new HashSet<>(data.getChunks());
        data.getBlockForcedChunks().getChunks().values().forEach(result::addAll);
        data.getBlockForcedChunks().getTickingChunks().values().forEach(result::addAll);
        data.getEntityForcedChunks().getChunks().values().forEach(result::addAll);
        data.getEntityForcedChunks().getTickingChunks().values().forEach(result::addAll);
        return Set.copyOf(result);
    }
}
