package org.millenaire.common.forge;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.village.Building;
import org.millenaire.common.village.VillageMapInfo;

@Mod.EventBusSubscriber(modid="millenaire",bus=Mod.EventBusSubscriber.Bus.MOD)
public class BuildingChunkLoader {
    private final Building townHall;
    private final BlockPos owner;
    private final Set<ChunkPos> forcedChunks=new LinkedHashSet<>();
    public boolean chunksLoaded;
    // Recovery: no global MSPT admission gate or pending-ticket wait that
    // could starve a village. Fast path for FULL chunks, at most one cold
    // Forge admission per village/tick. Keep the ticking-ticket semantics.
    private static final int MAX_READY_PER_TICK=4;
    private long lastLoadTick=Long.MIN_VALUE;
    private long lastWarning=Long.MIN_VALUE;
    private Set<ChunkPos> required=Set.of();
    private VillageMapInfo.ActivationBounds cachedBounds;
    public BuildingChunkLoader(Building townHall) {
        this.townHall=townHall;var pos=townHall.getPos();owner=new BlockPos(pos.getiX(),pos.getiY(),pos.getiZ());
    }
    @SubscribeEvent public static void setup(FMLCommonSetupEvent event) {
        ForgeChunkManager.setForcedChunkLoadingCallback("millenaire",new ChunkLoaderCallback());
    }
    public static Set<ChunkPos> requiredChunks(int startX,int startZ,int length,int width) {
        Set<ChunkPos> result=new LinkedHashSet<>();
        // Preserve the original village rectangle plus its one-chunk border.
        for(int x=startX-1;x<startX+length/16+1;x++)
            for(int z=startZ-1;z<startZ+width/16+1;z++)result.add(new ChunkPos(x,z));
        return result;
    }
    public void loadChunks(VillageMapInfo.ActivationBounds bounds) {
        if(!(townHall.world instanceof ServerLevel world)||bounds==null)return;
        long tick=world.getGameTime();
        if(lastLoadTick==tick)return;
        lastLoadTick=tick;
        long started=System.nanoTime();
        if(!bounds.equals(cachedBounds)){
            cachedBounds=bounds;
            required=requiredChunks(bounds.startX(),bounds.startZ(),bounds.length(),bounds.width());
            for(ChunkPos chunk:new ArrayList<>(forcedChunks))if(!required.contains(chunk)){
                ForgeChunkManager.forceChunk(world,"millenaire",owner,chunk.x,chunk.z,false,true);
                forcedChunks.remove(chunk);
            }
            chunksLoaded=forcedChunks.containsAll(required);
        }
        if(chunksLoaded)return;
        int attempted=0;
        ChunkPos cold=null;
        for(ChunkPos chunk:required){
            if(forcedChunks.contains(chunk))continue;
            if(world.getChunkSource().getChunkNow(chunk.x,chunk.z)==null){
                if(cold==null)cold=chunk;
                continue;
            }
            attempted++;
            claim(world,chunk,tick);
            if(attempted>=MAX_READY_PER_TICK)break;
        }
        // Give cold chunks guaranteed progress, even if MSPT is high. Do not
        // batch multiple synchronous cold loads in one village update.
        if(cold!=null&&attempted<MAX_READY_PER_TICK){
            attempted++;
            claim(world,cold,tick);
        }
        chunksLoaded=forcedChunks.containsAll(required);
        long elapsed=System.nanoTime()-started;
        if(elapsed>=100_000_000L)MillLog.major(townHall,"MILL-SLOW stage=village-tickets ms="+elapsed/1_000_000L
                +" attempted="+attempted+" forced="+forcedChunks.size()+" required="+required.size());
        if(chunksLoaded&&MillConfigValues.LogChunkLoader>=1)MillLog.major(townHall,"Force-loaded "+forcedChunks.size()+" village chunks.");
    }
    private void claim(ServerLevel world,ChunkPos chunk,long tick){
        if(ForgeChunkManager.forceChunk(world,"millenaire",owner,chunk.x,chunk.z,true,true)){
            forcedChunks.add(chunk);
        }else if(lastWarning==Long.MIN_VALUE||tick-lastWarning>=100){
            lastWarning=tick;
            MillLog.warning(townHall,"Could not force-load village chunk "+chunk);
        }
    }
    public int forcedCount(){return forcedChunks.size();}
    public int requiredCount(){return required.size();}
    public void unloadChunks() {
        if(forcedChunks.isEmpty()&&!chunksLoaded)return;
        if(townHall.world instanceof ServerLevel world)
            for(ChunkPos chunk:forcedChunks)ForgeChunkManager.forceChunk(world,"millenaire",owner,chunk.x,chunk.z,false,true);
        forcedChunks.clear();chunksLoaded=false;
        if(MillConfigValues.LogChunkLoader>=1)MillLog.major(townHall,"Unloaded the village chunks.");
    }
    public static class ChunkLoaderCallback implements ForgeChunkManager.LoadingValidationCallback {
        @Override public void validateTickets(ServerLevel world,ForgeChunkManager.TicketHelper tickets) {
            // Original behavior: discard persisted tickets; active villages request them anew.
            for(BlockPos owner:new ArrayList<>(tickets.getBlockTickets().keySet()))tickets.removeAllTickets(owner);
            for(java.util.UUID owner:new ArrayList<>(tickets.getEntityTickets().keySet()))tickets.removeAllTickets(owner);
        }
    }
}
