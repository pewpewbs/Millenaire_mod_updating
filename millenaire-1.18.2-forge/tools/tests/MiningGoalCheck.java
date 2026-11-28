import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.goal.GoalMinerMineResource;
import org.millenaire.common.goal.MiningAccess;
import org.millenaire.common.goal.generic.GoalGenericMining;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.pathing.PathWorldSnapshot;
import org.millenaire.common.pathing.atomicstryker.AStarStatic;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;
import org.millenaire.common.village.buildingmanagers.ResManager;
import org.millenaire.common.block.BlockDecorativeEarth;
import org.millenaire.common.block.BlockPath;
import org.millenaire.common.block.BlockPathSlab;
import org.millenaire.common.block.MillBlocks;

/** Exercise real selection, reach and production against a small in-memory quarry. */
public class MiningGoalCheck {
    static class Chunks extends ServerChunkCache {
        Chunks() { super(null,null,null,null,null,null,0,0,false,null,null,null); }
        @Override public boolean hasChunk(int x,int z) { return true; }
    }
    static class Quarry extends ServerLevel {
        Map<BlockPos,BlockState> cells;
        Chunks chunks;
        long tick;
        int sounds;
        Quarry() { super(null,null,null,null,null,null,null,null,false,0,List.of(),false); }
        @Override public ServerChunkCache getChunkSource() { return chunks; }
        @Override public boolean hasChunk(int x,int z) { return true; }
        @Override public int getMinBuildHeight() { return -64; }
        @Override public int getHeight() { return 384; }
        @Override public long getGameTime() { return tick; }
        @Override public boolean isDay() { return true; }
        @Override public BlockState getBlockState(BlockPos pos) {
            return cells.getOrDefault(pos, (pos.getY()<64 ? Blocks.STONE : Blocks.AIR).defaultBlockState());
        }
        @Override public net.minecraft.world.level.material.FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }
        @Override public void playSound(Player player,double x,double y,double z,SoundEvent sound,
                SoundSource category,float volume,float pitch) { sounds++; }
    }
    static class Miner extends MillVillager.EntityGenericMale {
        Building house;
        Point target;
        int awarded;
        Miner() { super(null); }
        @Override public boolean canVillagerClearLeaves() { return true; }
        @Override public Building getHouse() { return house; }
        @Override public Building getTownHall() { return house; }
        @Override public Building getGoalBuildingDest() { return house; }
        @Override public Point getGoalDestPoint() { return target; }
        @Override public void addToInv(InvItem item,int quantity) { awarded+=quantity; }
        @Override public void speakSentence(String key) { }
        @Override public void swing(net.minecraft.world.InteractionHand hand) { }
        @Override public void setPathDestPoint(Point destination,int tolerance) { }
    }
    static class Store extends Building {
        int stock;
        Store() { super(null,null); }
        @Override public int nbGoodAvailable(InvItem item, boolean a, boolean b, boolean c) { return stock; }
    }

    public static void run(sun.misc.Unsafe allocator) throws Exception {
        // The offline runner has no Forge registry events. InvItem's free-goods table
        // needs these object holders even when the only test product is cobblestone.
        var unlock=net.minecraft.core.Registry.BLOCK.getClass().getMethod("unfreeze");
        unlock.setAccessible(true);
        unlock.invoke(net.minecraft.core.Registry.BLOCK);
        MillBlocks.EARTH_DECORATION=new BlockDecorativeEarth("earth_deco");
        for (String field : List.of("PATHDIRT","PATHGRAVEL","PATHSLABS","PATHSANDSTONE","PATHGRAVELSLABS","PATHOCHRESLABS")) {
            String name=field.toLowerCase(java.util.Locale.ROOT);
            var color=net.minecraft.world.level.material.MaterialColor.DIRT;
            var sound=net.minecraft.world.level.block.SoundType.GRAVEL;
            MillBlocks.class.getField(field).set(null,new BlockPath(name,color,sound));
            MillBlocks.class.getField(field+"_SLAB").set(null,new BlockPathSlab(name,color,sound));
        }
        Quarry world=(Quarry)allocator.allocateInstance(Quarry.class);
        world.chunks=(Chunks)allocator.allocateInstance(Chunks.class);
        world.cells=new HashMap<>();
        Miner miner=(Miner)allocator.allocateInstance(Miner.class);
        NavigationGeometryCheck.field(miner,net.minecraft.world.entity.Entity.class,"level",world);
        NavigationGeometryCheck.position(miner,.5,64,.5);
        Store house=(Store)allocator.allocateInstance(Store.class);
        miner.house=house;
        var resources=new ResManager(house);
        NavigationGeometryCheck.field(house,Building.class,"resManager",resources);
        NavigationGeometryCheck.field(house,Building.class,"pos",new Point(0,64,0));
        GoalGenericMining goal=new GoalGenericMining();
        goal.range=4;
        goal.duration=80;
        goal.sourceBlockState=Blocks.STONE.defaultBlockState();
        var loot=InvItem.createInvItem(Items.COBBLESTONE);
        goal.loots.put(loot,3);
        Point sealed=new Point(0,62,0), exposed=new Point(2,63,0);
        resources.addSourcePoint(goal.sourceBlockState,sealed);
        miner.target=sealed; // An existing save may already have assigned the buried target.
        NavigationGeometryCheck.check(!goal.isStillValid(miner),"old buried assignment expires");
        int requests=0;
        for(int i=0;i<154;i++) {
            world.tick=i;
            if(goal.getDestination(miner)!=null) requests++;
        }
        NavigationGeometryCheck.check(requests==0 && !goal.isDestPossibleSpecific(miner,house),
                "154 selections with only a buried source yield zero routing requests");
        goal.performAction(miner);
        NavigationGeometryCheck.check(miner.awarded==0 && world.sounds==0,"buried source produces nothing");
        resources.addSourcePoint(goal.sourceBlockState,exposed);
        for(int i=0;i<12;i++) {
            Goal.GoalInformation dest=goal.getDestination(miner);
            if(dest==null || !exposed.equals(dest.getDest()) || !house.getPos().equals(dest.getDestBuildingPos()))
                throw new AssertionError("exposed alternative must keep its source and building");
        }
        NavigationGeometryCheck.check(goal.isDestPossibleSpecific(miner,house),"exposed alternative makes job eligible");
        GoalMinerMineResource legacy=new GoalMinerMineResource();
        NavigationGeometryCheck.check(exposed.equals(legacy.getDestination(miner).getDest()),"legacy mining chooses accessible source");
        miner.target=exposed;
        NavigationGeometryCheck.check(!goal.performAction(miner) && miner.awarded==0,
                "source in range behind the floor edge is still physically blocked");
        BlockPos work=new MiningAccess().first(miner,goal,exposed.getBlockPos());
        NavigationGeometryCheck.position(miner,work.getX()+.5,work.getY(),work.getZ()+.5);
        NavigationGeometryCheck.check(goal.actionDuration(miner)==80 && goal.performAction(miner)
                && miner.awarded==3 && world.sounds==1,"configured duration and one configured production batch");
        house.stock=4;
        goal.buildingLimit.put(loot,3);
        goal.performAction(miner);
        NavigationGeometryCheck.check(goal.getDestination(miner)==null && miner.awarded==3 && world.sounds==1,
                "stock cap at selection and at final production");
        goal.buildingLimit.clear();
        world.cells.put(exposed.getBlockPos(),Blocks.DIRT.defaultBlockState());
        goal.performAction(miner);
        NavigationGeometryCheck.check(miner.awarded==3 && !goal.isStillValid(miner),"replaced source cannot yield stale loot");
        BlockPos opening=sealed.getBlockPos().above();
        world.cells.put(opening,Blocks.AIR.defaultBlockState());
        PathWorldSnapshot.onKnownBlockMutation(world,opening);
        NavigationGeometryCheck.check(sealed.equals(goal.getDestination(miner).getDest()),"known opening wakes cached buried source");
        world.cells.remove(opening);
        PathWorldSnapshot.onKnownBlockMutation(world,opening);
        NavigationGeometryCheck.check(goal.getDestination(miner)==null,"closing access invalidates cached stand");
        world.cells.put(opening,Blocks.AIR.defaultBlockState());
        NavigationGeometryCheck.position(miner,10.5,64,10.5);
        world.tick+=41;
        NavigationGeometryCheck.check(sealed.equals(goal.getDestination(miner).getDest()),"unreported edit retried after short expiry");
        NavigationGeometryCheck.check(!goal.getPathingConfig(miner).tolerance && !legacy.getPathingConfig(miner).tolerance,
                "both mining planners must reach an exact stand");
        var alternatives=MiningAccess.alternatives(world,sealed.getBlockPos(),4,goal.getPathingConfig(miner));
        NavigationGeometryCheck.check(!alternatives.isEmpty() && alternatives.stream().allMatch(p->
                MiningAccess.canWorkFrom(world,sealed.getBlockPos(),p,4,goal.getPathingConfig(miner))),
                "every fallback endpoint can access the same registered source");
        MiningAccess retries=new MiningAccess();
        NavigationGeometryCheck.check(retries.first(miner,goal,sealed.getBlockPos())!=null,"visible source has stand before route failure");
        retries.retryLater(miner,goal,sealed.getBlockPos());
        NavigationGeometryCheck.check(retries.first(miner,legacy,sealed.getBlockPos())!=null
                && retries.first(miner,goal,sealed.getBlockPos())==null,
                "checking another range cannot erase the failed route cooldown");
        NavigationGeometryCheck.check(new MiningAccess().first(miner,goal,sealed.getBlockPos())!=null,
                "another miner is free to approach the same source");
        world.tick+=160;
        NavigationGeometryCheck.check(retries.first(miner,goal,sealed.getBlockPos())!=null,
                "a route failure never blacklists the source permanently");
        checkSavedQuarry(world,miner,goal);
    }

    private static void checkSavedQuarry(Quarry world,Miner miner,GoalGenericMining goal) throws Exception {
        var fixture=JsonParser.parseString(Files.readString(Path.of("tools/fixtures/kaland-mining-source.json"))).getAsJsonObject();
        var origin=fixture.getAsJsonArray("origin");
        var source=fixture.getAsJsonArray("source");
        var worker=fixture.getAsJsonArray("worker");
        BlockPos target=new BlockPos(source.get(0).getAsInt(),source.get(1).getAsInt(),source.get(2).getAsInt());
        BlockPos start=new BlockPos(worker.get(0).getAsInt(),worker.get(1).getAsInt(),worker.get(2).getAsInt());
        Map<Character,BlockState> palette=new HashMap<>();
        fixture.getAsJsonObject("palette").entrySet().forEach(pair->{
            var id=new ResourceLocation(pair.getValue().getAsString());
            // The original audit likewise used solid substitutes for unregistered mod blocks.
            palette.put(pair.getKey().charAt(0),(Registry.BLOCK.containsKey(id) ? Registry.BLOCK.get(id) : Blocks.BEDROCK).defaultBlockState());
        });
        int y=origin.get(1).getAsInt();
        for(var layer:fixture.getAsJsonArray("layersYZx")) {
            int z=origin.get(2).getAsInt();
            for(var row:layer.getAsJsonArray()) {
                int x=origin.get(0).getAsInt();
                for(char block:row.getAsString().toCharArray()) world.cells.put(new BlockPos(x++,y,z),palette.get(block));
                z++;
            }
            y++;
        }
        var manager=new ResManager(miner.house);
        NavigationGeometryCheck.field(miner.house,Building.class,"resManager",manager);
        miner.target=new Point(target);
        manager.addSourcePoint(goal.sourceBlockState,miner.target);
        NavigationGeometryCheck.position(miner,start.getX()+.5,start.getY(),start.getZ()+.5);
        var previous=AStarStatic.getAccessNodesSorted(world,start.getX(),start.getY(),start.getZ(),
                target.getX(),target.getY(),target.getZ(),Goal.JPS_CONFIG_WIDE);
        NavigationGeometryCheck.check(previous.length==10 && previous[0].x==start.getX()
                && previous[0].y==start.getY() && previous[0].z==start.getZ(),
                "saved Kaland mine reproduces the original ten stands, first at miner's feet");
        int selected=0;
        for(int i=0;i<154;i++) {
            world.tick++;
            if(goal.getDestination(miner)!=null) selected++;
        }
        NavigationGeometryCheck.check(selected==0 && !goal.isStillValid(miner),
                "Kaland: zero of 154 sealed-source selections qualify for routing");
        int initialLoot=miner.awarded;
        goal.performAction(miner);
        NavigationGeometryCheck.check(miner.awarded==initialLoot,"saved blocked source gives zero rewards");
        world.cells.put(target.south(),Blocks.AIR.defaultBlockState());
        world.cells.put(target.above(),Blocks.AIR.defaultBlockState());
        PathWorldSnapshot.onKnownBlockMutation(world,target.above());
        NavigationGeometryCheck.check(goal.getDestination(miner)!=null && goal.performAction(miner)
                && miner.awarded==initialLoot+3,"same Kaland position, reopened source produces configured loot");
        Goal.goals=new HashMap<>();
        Goal.goals.put("test_mining_duration",goal);
        miner.goalKey="test_mining_duration";
        miner.goalStarted=world.tick;
        miner.checkGoals();
        world.tick+=goal.duration-1;
        miner.checkGoals();
        NavigationGeometryCheck.check(miner.awarded==initialLoot+3,"goal clock pays nothing before its duration");
        world.tick++;
        miner.checkGoals();
        NavigationGeometryCheck.check(miner.awarded==initialLoot+6 && miner.goalKey==null,
                "one full interval pays exactly one additional batch and completes the task");
    }
}
