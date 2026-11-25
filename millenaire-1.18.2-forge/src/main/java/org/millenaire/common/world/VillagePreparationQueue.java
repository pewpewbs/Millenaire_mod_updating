package org.millenaire.common.world;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.culture.Culture;
import org.millenaire.common.culture.VillageType;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;

/**
 * Bounded fallback for natural village candidates rejected only because their
 * surrounding chunks have not been generated. Main server thread only.
 * Temporary vanilla tickets are not persistent Forge force-load tickets.
 */
public final class VillagePreparationQueue extends SavedData {
    private static final String ID = "millenaire_village_preparation";
    private static final TicketType<Long> TICKET = TicketType.create(ID, Long::compare);
    private static final int TIMEOUT = 1200;
    private static final int HALO = 12;
    private final List<Job> pending = new ArrayList<>();
    private final Set<ChunkPos> held = new HashSet<>();
    private Job active;
    private List<VillagePreparationPolicy.Cell> cells = List.of();
    private int cursor;
    private ChunkPos waiting;
    private long lastProgress, nextTick, pausedSince = -1;
    private int recentX, recentZ;
    private long recentUntil = -1;
    private boolean building;

    private static final class Job {
        int x,z;
        long seed;
        float completion;
        String culture="", type="";
        Job(int x,int z,long seed,float completion){
            this.x=x;this.z=z;this.seed=seed;this.completion=completion;
        }
    }

    private static VillagePreparationQueue get(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(VillagePreparationQueue::load,
                VillagePreparationQueue::new, ID);
    }

    private static VillagePreparationQueue load(CompoundTag tag) {
        VillagePreparationQueue queue = new VillagePreparationQueue();
        ListTag list = tag.getList("pending",10);
        for(int i=0;i<list.size() && queue.pending.size()<VillagePreparationPolicy.MAX_PENDING;i++){
            CompoundTag entry=list.getCompound(i);
            float completion=entry.getFloat("completion");
            if(!Float.isFinite(completion)||completion<0||completion>1)continue;
            Job job=new Job(entry.getInt("x"),entry.getInt("z"),entry.getLong("seed"),completion);
            job.culture=entry.getString("culture");job.type=entry.getString("type");
            queue.pending.add(job);
        }
        return queue;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        ListTag list=new ListTag();
        for(Job job:pending){
            CompoundTag entry=new CompoundTag();
            entry.putInt("x",job.x);entry.putInt("z",job.z);
            entry.putLong("seed",job.seed);entry.putFloat("completion",job.completion);
            entry.putString("culture",job.culture);entry.putString("type",job.type);
            list.add(entry);
        }
        tag.put("pending",list);
        return tag;
    }

    private static boolean enabled(MillWorldData mw){
        return Mill.loadingComplete && mw!=null && (mw.generateVillagesSet
                ? mw.generateVillages : MillConfigValues.generateVillages);
    }

    private static boolean nearPlayer(ServerLevel world,int x,int z){
        for(Player player:world.players()){
            double dx=player.getX()-(x*16.0+8),dz=player.getZ()-(z*16.0+8);
            if(!player.isSpectator() && dx*dx+dz*dz<=200.0*200.0)return true;
        }
        return false;
    }

    public static boolean suppressNatural(ServerLevel world,int x,int z){
        if(!world.getServer().isSameThread())return false;
        VillagePreparationQueue q=get(world);
        if(q.building)return true;
        if(q.active!=null && VillagePreparationPolicy.nearby(x,z,q.active.x,q.active.z,
                VillagePreparationPolicy.MAX_RADIUS+HALO))return true;
        return world.getGameTime()<q.recentUntil
                && VillagePreparationPolicy.nearby(x,z,q.recentX,q.recentZ,VillagePreparationPolicy.MAX_RADIUS+HALO);
    }

    static void offer(ServerLevel world,MillWorldData mw,WorldGenVillage generator,
                      int x,int z,long seed,float completion){
        if(!world.getServer().isSameThread() || !enabled(mw) || !nearPlayer(world,x,z))return;
        VillagePreparationQueue q=get(world);
        if(q.pending.size()>=VillagePreparationPolicy.MAX_PENDING || suppressNatural(world,x,z))return;
        if(!generator.preparationDistanceValid(world,mw,x,z,null))return;
        int spacing=Math.max(8,MillConfigValues.minDistanceBetweenVillages/32);
        for(Job job:q.pending)
            if(VillagePreparationPolicy.nearby(x,z,job.x,job.z,spacing))return;
        q.pending.add(new Job(x,z,seed,completion));q.setDirty();
        MillLog.major(generator,"VILLAGE-PREP queued="+x+"/"+z+" pending="+q.pending.size());
    }

    public static void tick(ServerLevel world,MillWorldData mw,long worldUpdateStarted){
        if(!world.dimension().equals(Level.OVERWORLD) || !world.getServer().isSameThread())return;
        VillagePreparationQueue q=get(world);
        try{q.advance(world,mw,worldUpdateStarted);}
        catch(Exception error){
            MillLog.printException("Village preparation failed",error);
            q.finish(world,"error");
        }
    }

    private void advance(ServerLevel world,MillWorldData mw,long started) throws Exception{
        long now=world.getGameTime();
        if(!enabled(mw)){
            release(world);
            if(!pending.isEmpty()){pending.clear();setDirty();}
            return;
        }
        if(world.players().isEmpty()){release(world);return;}
        if(pending.isEmpty())return;
        int interval=VillagePreparationPolicy.interval(world.getServer().getAverageTickTime(),System.nanoTime()-started);
        if(interval==0){
            if(pausedSince<0)pausedSince=now;
            // Avoid retaining a whole region indefinitely on a saturated server.
            if(active!=null && now-pausedSince>=TIMEOUT){
                release(world);nextTick=now+200;
            }
            return;
        }
        if(pausedSince>=0){lastProgress=now;pausedSince=-1;}
        if(now<nextTick)return;
        nextTick=now+interval;
        WorldGenVillage generator=new WorldGenVillage();
        if(active==null){
            active=pending.get(0);lastProgress=now;
            if(!generator.preparationDistanceValid(world,mw,active.x,active.z,null)){
                finish(world,"distance");return;
            }
            // Only one outstanding request; initial one is the candidate centre.
            request(world,new ChunkPos(active.x,active.z));return;
        }
        if(now-lastProgress>TIMEOUT){finish(world,"chunk-timeout");return;}
        if(waiting!=null){
            if(world.getChunkSource().getChunkNow(waiting.x,waiting.z)==null)return;
            waiting=null;lastProgress=now;
        }
        if(cells.isEmpty()){
            // A FULL centre is held now. Check type eligibility before requesting the area.
            VillageType type;
            if(active.type.isEmpty()){
                type=generator.selectPreparationType(world,mw,active.x,active.z,active.seed);
                if(type==null){finish(world,"no-eligible-type");return;}
                active.type=type.key;active.culture=type.culture.key;setDirty();
            }else type=resolve(active);
            if(type==null || !generator.preparationTypeValid(world,mw,active.x,active.z,type)){
                finish(world,"type-no-longer-eligible");return;
            }
            int radius=VillagePreparationPolicy.radius(Math.max(type.radius,MillConfigValues.VillageRadius));
            cells=VillagePreparationPolicy.cells(active.x,active.z,radius);
            if(cells.isEmpty()){finish(world,"radius-limit");return;}
            for(var cell:cells){
                if(!world.getWorldBorder().isWithinBounds(new BlockPos(cell.x()*16,0,cell.z()*16))
                        || !world.getWorldBorder().isWithinBounds(new BlockPos(cell.x()*16+15,0,cell.z()*16+15))){
                    finish(world,"world-border");return;
                }
            }
            MillLog.major(this,"VILLAGE-PREP preparing="+active.x+"/"+active.z+" type="+active.culture+"/"+active.type+" chunks="+cells.size());
        }
        if(cursor<cells.size()){
            var cell=cells.get(cursor++);
            ChunkPos pos=new ChunkPos(cell.x(),cell.z());
            if(!held.contains(pos))request(world,pos);
            lastProgress=now;
            return;
        }
        // Every ticket is held until construction completes; getChunkNow never
        // triggers a synchronous load. Verify all FULL chunks one last time.
        for(var cell:cells)if(world.getChunkSource().getChunkNow(cell.x(),cell.z())==null){
            finish(world,"lost-chunk");return;
        }
        VillageType type=resolve(active);
        if(type==null || !generator.preparationTypeValid(world,mw,active.x,active.z,type)){
            finish(world,"stale-candidate");return;
        }
        long began=System.nanoTime();
        building=true;
        boolean success=false;
        try{
            success=generator.generateVillageAtPoint(world,new Random(active.seed),active.x*16,0,active.z*16,
                    null,false,false,true,Integer.MAX_VALUE,type,null,null,active.completion);
        }finally{
            building=false;
            MillLog.major(this,"VILLAGE-PREP build="+active.x+"/"+active.z+" success="+success
                    +" ms="+(System.nanoTime()-began)/1_000_000L);
            finish(world,success?"generated":"layout-rejected");
        }
    }

    private static VillageType resolve(Job job){
        Culture culture=Culture.getCultureByName(job.culture);
        return culture==null?null:culture.getVillageType(job.type);
    }

    private void request(ServerLevel world,ChunkPos pos){
        // Radius zero is a FULL chunk ticket; the vanilla chunk scheduler
        // handles dependency generation. Do not call getChunk() or join().
        held.add(pos);
        world.getChunkSource().addRegionTicket(TICKET,pos,0,pos.toLong());
        waiting=pos;
    }

    private void release(ServerLevel world){
        if(active!=null){
            recentX=active.x;recentZ=active.z;recentUntil=world.getGameTime()+200;
        }
        for(ChunkPos pos:held)world.getChunkSource().removeRegionTicket(TICKET,pos,0,pos.toLong());
        held.clear();active=null;cells=List.of();cursor=0;waiting=null;building=false;
    }

    private void finish(ServerLevel world,String reason){
        if(active!=null){
            MillLog.major(this,"VILLAGE-PREP finished="+active.x+"/"+active.z+" reason="+reason+" tickets="+held.size());
            pending.remove(active);setDirty();
        }
        release(world);
        nextTick=world.getGameTime()+20;
    }

    public static void unload(ServerLevel world){
        if(world.dimension().equals(Level.OVERWORLD) && world.getServer().isSameThread())
            get(world).release(world);
    }
}
