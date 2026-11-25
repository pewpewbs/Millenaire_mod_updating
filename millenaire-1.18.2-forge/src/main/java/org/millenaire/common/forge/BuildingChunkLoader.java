package org.millenaire.common.forge;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import java.util.WeakHashMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.village.Building;

@Mod.EventBusSubscriber(modid="millenaire",bus=Mod.EventBusSubscriber.Bus.MOD)
public class BuildingChunkLoader {
    private final Building townHall;
    private final BlockPos owner;
    private final Set<ChunkPos> forcedChunks=new LinkedHashSet<>();
    public boolean chunksLoaded;
    private static final TicketType<Long> PREPARE=TicketType.create("millenaire_active_village",Long::compare);
    private static final WeakHashMap<ServerLevel,Long> LAST_ADMISSION=new WeakHashMap<>();
    private static final int WAIT_LIMIT=1200;
    private long lastLoadTick=Long.MIN_VALUE, pendingSince, retryAfter;
    private ChunkPos pending;
    private Set<ChunkPos> required=Set.of();
    private int cachedX,cachedZ,cachedLength,cachedWidth;
    private boolean hasRequired;
    private boolean takeBudget(ServerLevel world,long tick) {
        float mspt=world.getServer().getAverageTickTime();
        if(!Float.isFinite(mspt)||mspt>=50)return false;
        Long previous=LAST_ADMISSION.get(world);
        if(previous!=null&&tick>=previous&&tick-previous<(mspt>=40?10:1))return false;
        LAST_ADMISSION.put(world,tick);
        return true;
    }
    private void releasePending(ServerLevel world) {
        if(pending!=null)world.getChunkSource().removeRegionTicket(PREPARE,pending,0,owner.asLong());
        pending=null;
    }
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
    public void loadChunks() {
        if(!(townHall.world instanceof ServerLevel world) || townHall.winfo==null)return;
        long tick=world.getGameTime();
        if(lastLoadTick==tick)return;
        lastLoadTick=tick;
        long started=System.nanoTime();
        var info=townHall.winfo;
        if(!hasRequired||cachedX!=info.chunkStartX||cachedZ!=info.chunkStartZ
                ||cachedLength!=info.length||cachedWidth!=info.width){
            cachedX=info.chunkStartX;cachedZ=info.chunkStartZ;
            cachedLength=info.length;cachedWidth=info.width;
            required=requiredChunks(cachedX,cachedZ,cachedLength,cachedWidth);
            hasRequired=true;
            if(pending!=null&&!required.contains(pending))releasePending(world);
            for(ChunkPos chunk:new ArrayList<>(forcedChunks))if(!required.contains(chunk)){
                ForgeChunkManager.forceChunk(world,"millenaire",owner,chunk.x,chunk.z,false,true);
                forcedChunks.remove(chunk);
            }
            chunksLoaded=forcedChunks.containsAll(required);
        }
        if(chunksLoaded||tick<retryAfter)return;
        int attempted=0;
        if(pending!=null){
            if(tick-pendingSince>=WAIT_LIMIT){
                releasePending(world);retryAfter=tick+100;return;
            }
            if(world.getChunkSource().getChunkNow(pending.x,pending.z)==null)return;
            if(!takeBudget(world,tick))return;
            ChunkPos ready=pending;
            try {
                attempted++;
                if(ForgeChunkManager.forceChunk(world,"millenaire",owner,ready.x,ready.z,true,true))
                    forcedChunks.add(ready);
                else retryAfter=tick+100;
            } finally {releasePending(world);}
        }else{
            for(ChunkPos chunk:required)if(!forcedChunks.contains(chunk)){
                if(!takeBudget(world,tick))return;
                pending=chunk;pendingSince=tick;
                world.getChunkSource().addRegionTicket(PREPARE,chunk,0,owner.asLong());
                attempted++;
                break;
            }
        }
        chunksLoaded=forcedChunks.containsAll(required);
        long elapsed=System.nanoTime()-started;
        if(elapsed>=100_000_000L)MillLog.major(townHall,"MILL-SLOW stage=village-tickets ms="+elapsed/1_000_000L
                +" attempted="+attempted+" forced="+forcedChunks.size()+" required="+required.size());
        if(chunksLoaded&&MillConfigValues.LogChunkLoader>=1)MillLog.major(townHall,"Force-loaded "+forcedChunks.size()+" village chunks.");
    }
    public void unloadChunks() {
        if(forcedChunks.isEmpty()&&!chunksLoaded&&pending==null)return;
        if(townHall.world instanceof ServerLevel world){
            releasePending(world);
            for(ChunkPos chunk:forcedChunks)ForgeChunkManager.forceChunk(world,"millenaire",owner,chunk.x,chunk.z,false,true);
        }
        forcedChunks.clear();chunksLoaded=false;retryAfter=0;
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
