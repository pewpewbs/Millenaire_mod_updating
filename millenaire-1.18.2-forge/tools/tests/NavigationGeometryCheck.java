import java.util.*;
import java.lang.reflect.*;
import net.minecraft.core.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.pathfinder.*;
import net.minecraft.world.phys.*;
import org.millenaire.common.entity.PathNavigateSimple;
import org.millenaire.common.pathing.atomicstryker.*;

/** Real 1.18.2 block shapes and PathNavigateSimple, no running game/world mutation. */
public class NavigationGeometryCheck {
    static int checks;
    static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    static class Grid implements BlockGetter {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        public BlockState getBlockState(BlockPos p) {
            return blocks.getOrDefault(p, (p.getY() < 64 ? Blocks.STONE : Blocks.AIR).defaultBlockState());
        }
        public BlockEntity getBlockEntity(BlockPos p) { return null; }
        public FluidState getFluidState(BlockPos p) { return getBlockState(p).getFluidState(); }
        public int getHeight() { return 384; }
        public int getMinBuildHeight() { return -64; }
    }
    static class Navigator extends PathNavigateSimple {
        Grid grid;
        Navigator(Mob mob, Grid grid) { super(mob, null); this.grid = grid; }
        @Override protected boolean isLadderNode(Node n) {
            return grid.getBlockState(n.asBlockPos()).is(Blocks.LADDER);
        }
        @Override protected double getGroundY(Vec3 pos) {
            return WalkNodeEvaluator.getFloorLevel(grid, new BlockPos(pos));
        }
        @Override protected void doStuckDetection(Vec3 pos) { }
        void route(Path p) { this.path = p; }
        void follow() { this.followThePath(); }
        boolean canFollow() { return this.canUpdatePath(); }
    }
    static class VanillaNavigator extends net.minecraft.world.entity.ai.navigation.GroundPathNavigation {
        VanillaNavigator(Mob mob) { super(mob,null); }
        @Override protected Vec3 getTempMobPos() { return this.mob.position(); }
        @Override protected void doStuckDetection(Vec3 pos) { }
        void follow(Path p) { this.path=p; this.followThePath(); }
    }
    static void field(Object o, Class<?> owner, String name, Object value) throws Exception {
        Field f = owner.getDeclaredField(name); f.setAccessible(true); f.set(o, value);
    }
    static void position(Mob mob, double x, double y, double z) throws Exception {
        field(mob, Entity.class, "position", new Vec3(x,y,z));
        field(mob, Entity.class, "blockPosition", new BlockPos(x,y,z));
        field(mob, Entity.class, "bb", new AABB(x-.3,y,z-.3,x+.3,y+1.8,z+.3));
    }
    static AS_PathEntity route(int... xyz) {
        Node[] nodes = new Node[xyz.length / 3];
        for (int i=0;i<nodes.length;i++) nodes[i]=new Node(xyz[3*i],xyz[3*i+1],xyz[3*i+2]);
        return new AS_PathEntity(nodes);
    }
    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion(); net.minecraft.server.Bootstrap.bootStrap();
        Field uf=sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); uf.setAccessible(true);
        var unsafe=(sun.misc.Unsafe)uf.get(null);
        Mob mob=(Mob)unsafe.allocateInstance(org.millenaire.common.entity.MillVillager.EntityGenericMale.class);
        field(mob, Entity.class, "dimensions", EntityDimensions.scalable(.6F,1.8F));
        field(mob, LivingEntity.class, "attributes", new AttributeMap(AttributeSupplier.builder()
                .add(Attributes.FOLLOW_RANGE,16).add(Attributes.MOVEMENT_SPEED,.5).build()));
        field(mob, Mob.class, "pathfindingMalus", new EnumMap<BlockPathTypes,Float>(BlockPathTypes.class));
        Grid grid=new Grid(); Navigator nav=new Navigator(mob,grid);
        position(mob,.5,64,.5);
        var p=route(0,64,0, 1,64,0); nav.route(p); nav.follow();
        check(p.getNextNodeIndex()==1,"reached cell centre advances");
        // Vanilla dot-product shortcut skips this elbow despite not reaching it.
        position(mob,.5,64,1.1); p=route(0,64,0, 1,64,1); nav.route(p); nav.follow();
        check(p.getNextNodeIndex()==0,"unreached corner must not be skipped");
        var baseline=route(0,64,0, 1,64,1); new VanillaNavigator(mob).follow(baseline);
        check(baseline.getNextNodeIndex()==1,"same fixture reproduces vanilla unsafe shortcut");
        position(mob,.5,64.5,.5); p=route(0,65,0, 1,65,0); nav.route(p);
        grid.blocks.put(new BlockPos(0,64,0),Blocks.STONE.defaultBlockState()); nav.follow();
        check(p.getNextNodeIndex()==0,"halfway up cannot advance full-height waypoint");
        grid.blocks.clear();
        grid.blocks.put(new BlockPos(0,64,0),Blocks.OAK_SLAB.defaultBlockState());
        p=route(0,65,0, 1,65,0); nav.route(p); nav.follow();
        check(p.getNextNodeIndex()==1,"actual half-slab support height counts as arrival");
        grid.blocks.clear();
        grid.blocks.put(new BlockPos(0,64,0),Blocks.LADDER.defaultBlockState());
        position(mob,.5,64.4,.5); p=route(0,64,0, 0,63,0); nav.route(p); nav.follow();
        check(p.getNextNodeIndex()==0,"vanilla follower cannot advance ladder rung");
        check(nav.canFollow(),"airborne AS routes use controlled follower, not vanilla falling skip");
        // Thin ladder panel: old .12 wallward offset intersects all four facings.
        for(Direction face:Direction.Plane.HORIZONTAL) {
            var state=Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,face);
            var wall=face.getOpposite();
            AABB centred=new AABB(.2,64,.2,.8,65.8,.8);
            AABB old=centred.move(wall.getStepX()*.12,0,wall.getStepZ()*.12);
            boolean oldHit=false;
            for(var part:state.getCollisionShape(grid,new BlockPos(0,64,0)).toAabbs()) {
                part=part.move(0,64,0);
                check(!centred.intersects(part),"shaft centre clears ladder panel "+face);
                oldHit |= old.intersects(part);
            }
            check(oldHit,"fixture reproduces old wallward ladder collision "+face);
        }
        grid.blocks.clear(); var config=new AStarConfig(true,false,true,false,false);
        check(!AStarStatic.isTransitionViable(grid,new AStarNode(0,64,0),new AStarNode(1,66,1),config),
                "two-block upward diagonal rejected even with clear raised body");
        check(AStarStatic.isTransitionViable(grid,new AStarNode(0,64,0),new AStarNode(1,64,0),config),"flat edge retained");
        grid.blocks.put(new BlockPos(1,64,0),Blocks.STONE.defaultBlockState());
        check(AStarStatic.isTransitionViable(grid,new AStarNode(0,64,0),new AStarNode(1,65,0),config),"one-block rise retained");
        grid.blocks.put(new BlockPos(0,66,0),Blocks.STONE_SLAB.defaultBlockState());
        check(!AStarStatic.isTransitionViable(grid,new AStarNode(0,64,0),new AStarNode(1,65,0),config),"low ceiling rejects rise");
        // Ambiguous multi-level column: forward reconstruction must start at 64,
        // not choose 65 merely because it is nearest the final goal's height.
        grid.blocks.clear();
        grid.blocks.put(new BlockPos(1,64,0),Blocks.LADDER.defaultBlockState());
        grid.blocks.put(new BlockPos(1,65,0),Blocks.LADDER.defaultBlockState());
        grid.blocks.put(new BlockPos(2,64,0),Blocks.STONE.defaultBlockState());
        var start=new AStarNode(0,64,0); var end=new AStarNode(2,65,0,3,start);
        var jps=new AStarWorkerJPS(null); jps.setup(grid,start,end,config);
        field(jps,AStarWorker.class,"timeLimit",Long.MAX_VALUE);
        field(jps,AStarWorkerJPS.class,"currentNode",end);
        Method trace=AStarWorkerJPS.class.getDeclaredMethod("backTrace",AStarNode.class);trace.setAccessible(true);
        var nodes=(ArrayList<AStarNode>)trace.invoke(jps,start);
        check(nodes!=null && nodes.size()==3 && nodes.get(1).y==64,"JPS expands from parent in search direction");
        check(nodes.get(0).equals(end)&&nodes.get(2).equals(start),"public goal-to-start order retained");
        // Exercise the actual move controller ownership, including a stale jump.
        field(mob,Mob.class,"navigation",nav);
        field(nav,PathNavigateSimple.class,"ladderMotionControlled",true);
        var control=new org.millenaire.common.entity.VillagerMoveControl(mob);
        Class<?> operation=Class.forName("net.minecraft.world.entity.ai.control.MoveControl$Operation");
        field(control,net.minecraft.world.entity.ai.control.MoveControl.class,"operation",Enum.valueOf((Class)operation,"JUMPING"));
        mob.setDeltaMovement(0,-.12,0); mob.setJumping(true); control.tick();
        Field jumping=LivingEntity.class.getDeclaredField("jumping");jumping.setAccessible(true);
        check(!jumping.getBoolean(mob),"ladder descent clears stale ground jump");
        check(mob.getDeltaMovement().y==-.12 && mob.zza==0 && mob.xxa==0,"ladder controller keeps sole motion ownership");
        // Read-only capture of the reported world's doorway; unknown mod blocks
        // are conservatively solid. The tested stair/door/walls are vanilla.
        grid.blocks.clear();
        var json=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(
                java.nio.file.Path.of("tools/fixtures/navigation-stair-22-66-571.json"))).getAsJsonObject();
        for(var entry:json.entrySet()) {
            var data=entry.getValue().getAsJsonObject(); String name=data.get("Name").getAsString();
            Block block=name.startsWith("minecraft:") ? Registry.BLOCK.get(new net.minecraft.resources.ResourceLocation(name)) : Blocks.BEDROCK;
            BlockState state=block.defaultBlockState();
            if(data.has("Properties")) for(var value:data.getAsJsonObject("Properties").entrySet()) {
                var property=block.getStateDefinition().getProperty(value.getKey());
                if(property!=null) state=setProperty(state,property,value.getValue().getAsString());
            }
            String[] coords=entry.getKey().split(",");
            grid.blocks.put(new BlockPos(Integer.parseInt(coords[0]),Integer.parseInt(coords[1]),Integer.parseInt(coords[2])),state);
        }
        var door=grid.getBlockState(new BlockPos(22,67,570));
        check(door.getBlock() instanceof DoorBlock && door.getValue(DoorBlock.FACING)==Direction.EAST,"saved hotspot orientation reproduced");
        AStarNode[] passage={new AStarNode(22,66,572),new AStarNode(22,67,571),new AStarNode(22,67,570),new AStarNode(22,67,569)};
        for(int i=1;i<passage.length;i++) {
            check(AStarStatic.isTransitionViable(grid,passage[i-1],passage[i],config),"hotspot entrance edge "+i);
            check(AStarStatic.isTransitionViable(grid,passage[i],passage[i-1],config),"hotspot exit edge "+i);
        }
        Method selection=org.millenaire.common.pathing.VillageDoorAccess.class.getDeclaredMethod(
                "routeDoorState",LivingEntity.class,BlockPos.class,BlockState.class);selection.setAccessible(true);
        position(mob,22.5,67,571.5); p=route(22,67,571,22,67,570,22,67,569);nav.route(p);
        check(Boolean.FALSE.equals(selection.invoke(null,mob,new BlockPos(22,67,570),door)),"server opener matches planned sideways door state");
        // The old open panel blocks the whole .6-wide approach despite a clear node centre.
        AABB crossing=new AABB(22.2,67,570.6,22.8,68.8,571.2);
        check(door.getCollisionShape(grid,new BlockPos(22,67,570)).toAabbs().stream()
                .anyMatch(b->crossing.intersects(b.move(22,67,570))),"saved OPEN panel is actual blocker");
        System.out.println("PASS: "+checks+" real geometry / follower / JPS assertions");
    }
    static BlockState setProperty(BlockState state, net.minecraft.world.level.block.state.properties.Property property,String value) {
        return (BlockState)property.getValue(value).map(v->state.setValue(property,(Comparable)v)).orElse(state);
    }
}
