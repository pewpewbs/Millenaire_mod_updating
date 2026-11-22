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

@Mod.EventBusSubscriber(modid="millenaire",bus=Mod.EventBusSubscriber.Bus.MOD)
public class BuildingChunkLoader {
    private final Building townHall;
    private final BlockPos owner;
    private final Set<ChunkPos> forcedChunks=new LinkedHashSet<>();
    public boolean chunksLoaded;
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
        var info=townHall.winfo;Set<ChunkPos> required=requiredChunks(info.chunkStartX,info.chunkStartZ,info.length,info.width);
        for(ChunkPos chunk:new ArrayList<>(forcedChunks))if(!required.contains(chunk)) {
            ForgeChunkManager.forceChunk(world,"millenaire",owner,chunk.x,chunk.z,false,true);forcedChunks.remove(chunk);
        }
        for(ChunkPos chunk:required)if(!forcedChunks.contains(chunk)) {
            if(ForgeChunkManager.forceChunk(world,"millenaire",owner,chunk.x,chunk.z,true,true))forcedChunks.add(chunk);
            else MillLog.warning(townHall,"Could not force-load village chunk "+chunk);
        }
        chunksLoaded=forcedChunks.containsAll(required);
        if(MillConfigValues.LogChunkLoader>=1)MillLog.major(townHall,"Force-loaded "+forcedChunks.size()+" village chunks.");
    }
    public void unloadChunks() {
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
