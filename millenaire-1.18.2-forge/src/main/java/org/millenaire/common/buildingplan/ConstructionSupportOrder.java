package org.millenaire.common.buildingplan;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Construction-only ordering; native survival and connection updates remain enabled. */
public final class ConstructionSupportOrder {
    private ConstructionSupportOrder() {}

    static boolean fixture(BlockState state) {
        Block b=state.getBlock();
        return b instanceof DoorBlock || b instanceof BedBlock || b instanceof DoublePlantBlock
                || b instanceof TorchBlock || b instanceof WallTorchBlock
                || b instanceof AbstractBannerBlock || b instanceof SignBlock
                || b instanceof LadderBlock || b instanceof FaceAttachedHorizontalDirectionalBlock
                || b instanceof BasePressurePlateBlock || b instanceof BaseRailBlock
                || b instanceof BushBlock || b instanceof CarpetBlock || b instanceof LanternBlock
                || b instanceof TripWireHookBlock || b instanceof VineBlock
                || b instanceof RedStoneWireBlock || b instanceof SnowLayerBlock
                || b instanceof FlowerPotBlock;
    }

    private static boolean late(BuildingBlock b) {
        return b.special==BuildingBlock.TORCHGUESS
                || ((b.special==0 || b.special==BuildingBlock.INVERTED_DOOR)
                    && !b.getBlockstate().isAir() && fixture(b.getBlockstate()));
    }

    private static boolean concrete(BuildingBlock b) {
        return (b.special==0 || b.special==BuildingBlock.INVERTED_DOOR)
                && !b.getBlockstate().isAir();
    }

    private static boolean clear(BuildingBlock b) {
        return b.special==0 && b.getBlockstate().isAir();
    }

    static void prepare(Level world,List<BuildingBlock> operations) {
        prepare(world, operations, false);
    }

    /** In a new independent building, excavation is a separate first phase.
     * Do not apply this exception to updates or existing fixtures/structures. */
    private static boolean preserveNewTerrainClear(Level world, BuildingBlock b, boolean prepareNewGround) {
        if (!prepareNewGround || b.special != BuildingBlock.CLEARGROUND) return false;
        BlockPos p = b.p.getBlockPos();
        // Keep the planned excavation if its state cannot yet be inspected.
        if (!world.hasChunkAt(p)) return true;
        BlockState s = world.getBlockState(p);
        if (world.getBlockEntity(p) != null) return false;
        Block block = s.getBlock();
        return block == Blocks.GRASS_BLOCK || block == Blocks.DIRT || block == Blocks.COARSE_DIRT
                || block == Blocks.ROOTED_DIRT || block == Blocks.PODZOL || block == Blocks.MYCELIUM
                || block == Blocks.SAND || block == Blocks.RED_SAND || block == Blocks.GRAVEL
                || block == Blocks.CLAY || block == Blocks.STONE || block == Blocks.GRANITE
                || block == Blocks.DIORITE || block == Blocks.ANDESITE || block == Blocks.DEEPSLATE
                || block == Blocks.TUFF || block == Blocks.CALCITE || block == Blocks.SANDSTONE
                || block == Blocks.RED_SANDSTONE || block == Blocks.TERRACOTTA
                || block == Blocks.WHITE_TERRACOTTA || block == Blocks.ORANGE_TERRACOTTA
                || block == Blocks.MAGENTA_TERRACOTTA || block == Blocks.LIGHT_BLUE_TERRACOTTA
                || block == Blocks.YELLOW_TERRACOTTA || block == Blocks.LIME_TERRACOTTA
                || block == Blocks.PINK_TERRACOTTA || block == Blocks.GRAY_TERRACOTTA
                || block == Blocks.LIGHT_GRAY_TERRACOTTA || block == Blocks.CYAN_TERRACOTTA
                || block == Blocks.PURPLE_TERRACOTTA || block == Blocks.BLUE_TERRACOTTA
                || block == Blocks.BROWN_TERRACOTTA || block == Blocks.GREEN_TERRACOTTA
                || block == Blocks.RED_TERRACOTTA || block == Blocks.BLACK_TERRACOTTA;
    }

    static void prepare(Level world,List<BuildingBlock> operations,boolean prepareNewGround) {
        // A clear followed by a concrete replacement at the same coordinate
        // is not a real demolition. Replace in one write instead of exposing air.
        // Called only for live construction; CLEARGROUND has no below-ground
        // write in this mode. Preserve other special ground/tree operations.
        // Exception: actual terrain inside a new footprint must be excavated
        // before any walls are erected, even at future wall positions.
        Set<BlockPos> laterReplacement=new HashSet<>();
        Set<BuildingBlock> redundant=Collections.newSetFromMap(new IdentityHashMap<>());
        for(int i=operations.size()-1;i>=0;i--){
            BuildingBlock b=operations.get(i);
            BlockPos p=b.p.getBlockPos();
            if((clear(b) || b.special==BuildingBlock.CLEARGROUND)
                    && laterReplacement.contains(p)
                    && !preserveNewTerrainClear(world,b,prepareNewGround)){
                redundant.add(b);
                continue;
            }
            if(concrete(b)){
                laterReplacement.add(p);
                BlockPos other=ConstructionPairPlacement.partner(p,b.getBlockstate());
                if(other!=null)laterReplacement.add(other);
            }else laterReplacement.remove(p);
        }
        operations.removeIf(redundant::contains);

        // Only move the last direct write at each point. Preserve intentional
        // later overwrites and the order of ground/tree/marker operations.
        Set<BlockPos> seen=new HashSet<>();
        Set<BuildingBlock> finalFixtures=Collections.newSetFromMap(new IdentityHashMap<>());
        for(int i=operations.size()-1;i>=0;i--){
            BuildingBlock b=operations.get(i);
            BlockPos p=b.p.getBlockPos();
            BlockPos other=ConstructionPairPlacement.partner(p,b.getBlockstate());
            boolean lastHere=seen.add(p);
            // Do not move a pair past an explicitly later operation at its companion.
            if(lastHere && late(b) && (other==null || !seen.contains(other)))finalFixtures.add(b);
            if(other!=null)seen.add(other);
        }
        // Explicit fixture demolitions precede demolition of their supports.
        // Only move the first operation at a point; don't reorder later clears.
        List<BuildingBlock> early=new ArrayList<>(), main=new ArrayList<>(), end=new ArrayList<>();
        seen.clear();
        for(BuildingBlock b:operations){
            BlockPos p=b.p.getBlockPos();
            boolean first=seen.add(p);
            if(finalFixtures.contains(b))end.add(b);
            else if(first && clear(b) && world.hasChunkAt(p) && fixture(world.getBlockState(p)))early.add(b);
            else main.add(b);
        }
        operations.clear();
        operations.addAll(early);
        operations.addAll(main);
        operations.addAll(end);
    }

    /** Do not discard a planned fixture just because it matches BEFORE a
     * neighbouring construction operation. It is checked again after supports. */
    static void retainFixtureChecks(List<BuildingBlock> operations,boolean[] deleted) {
        Set<BlockPos> touched=new HashSet<>();
        Map<BlockPos,Integer> last=new HashMap<>();
        for(int i=0;i<operations.size();i++){
            BuildingBlock b=operations.get(i);
            BlockPos p=b.p.getBlockPos();
            last.put(p,i);
            if(!deleted[i]){
                touched.add(p);
                BlockPos other=ConstructionPairPlacement.partner(p,b.getBlockstate());
                if(other!=null)touched.add(other);
                if(b.special==BuildingBlock.PRESERVEGROUNDDEPTH || b.special==BuildingBlock.PRESERVEGROUNDSURFACE
                        || b.special==BuildingBlock.CLEARGROUND || b.special==BuildingBlock.CLEARTREE)
                    touched.add(p.below());
            }
        }
        for(int i=0;i<operations.size();i++){
            BuildingBlock b=operations.get(i);
            BlockPos p=b.p.getBlockPos();
            if(!deleted[i] || !late(b) || last.get(p)!=i)continue;
            BlockPos other=ConstructionPairPlacement.partner(p,b.getBlockstate());
            if(near(touched,p) || (other!=null&&near(touched,other)))deleted[i]=false;
        }
    }

    private static boolean near(Set<BlockPos> points,BlockPos p){
        if(points.contains(p))return true;
        for(Direction d:Direction.values())if(points.contains(p.relative(d)))return true;
        return false;
    }
}
