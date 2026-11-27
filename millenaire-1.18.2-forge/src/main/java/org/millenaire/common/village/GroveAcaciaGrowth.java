package org.millenaire.common.village;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.eventbus.api.Event;
import org.millenaire.common.utilities.MillLog;

/** Compact acacia for registered grove saplings, never general world generation. */
public final class GroveAcaciaGrowth {
    private GroveAcaciaGrowth() {}

    private static boolean hasLoadedArea(ServerLevel world,BlockPos pos){
        // Tree feature reads neighbours for leaf distance; never request a new chunk.
        for(int x=(pos.getX()-8)>>4;x<=(pos.getX()+8)>>4;x++)
            for(int z=(pos.getZ()-8)>>4;z<=(pos.getZ()+8)>>4;z++)
                if(world.getChunkSource().getChunkNow(x,z)==null)return false;
        return true;
    }

    /** Growth clearance is separate from planting viability. Only reads loaded chunks. */
    public static boolean hasCompactRoom(ServerLevel world,BlockPos pos,BlockState sapling){
        if(!hasLoadedArea(world,pos))return false;
        if(pos.getY()<=world.getMinBuildHeight()||pos.getY()+7>=world.getMaxBuildHeight())return false;
        if(!sapling.canSurvive(world,pos))return false;
        var ground=world.getBlockState(pos.below());
        if(!(ground.is(Blocks.GRASS_BLOCK)||ground.is(Blocks.DIRT)||ground.is(Blocks.COARSE_DIRT)
                ||ground.is(Blocks.PODZOL)||ground.is(Blocks.ROOTED_DIRT)))return false;
        // Lower trunk needs only its own column; adjacent fences do not
        // occupy it. Upper canopy may overlap natural acacia foliage, never
        // player-persistent leaves or structural blocks.
        for(int dy=1;dy<=6;dy++){
            int radius=dy<3?0:2;
            for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
                BlockPos p=pos.offset(dx,dy,dz);
                if(!world.getWorldBorder().isWithinBounds(p))return false;
                var state=world.getBlockState(p);
                if(!state.isAir()&&!(state.is(Blocks.ACACIA_LEAVES)
                        &&!state.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT)))return false;
            }
        }
        return world.getWorldBorder().isWithinBounds(pos);
    }

    /** The normal minimum-size volume is blocked even at minimum trunk height.
     * Do not interpret a randomly tall tree failure as lack of room. */
    private static boolean vanillaMinimumObstructed(ServerLevel world,BlockPos pos,TreeConfiguration config){
        if(config.minimumSize.minClippedHeight().isPresent())return false;
        int height=config.trunkPlacer.getTreeHeight(new Random(0L){
            @Override public int nextInt(int bound){return 0;}
        });
        for(int dy=0;dy<=height+1;dy++){
            int radius=config.minimumSize.getSizeAtHeight(height,dy);
            for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
                if(dx==0&&dy==0&&dz==0)continue; // the grower replaces the sapling itself
                BlockPos at=pos.offset(dx,dy,dz);
                if(!TreeFeature.isFree(world,at)||!config.ignoreVines&&world.getBlockState(at).is(Blocks.VINE))return true;
            }
        }
        return false;
    }

    /** Ordinary roomy groves keep the original vanilla grower. Only after a
     * genuine vanilla failure at a provably constrained site may compact growth run. */
    static boolean tryGrow(ServerLevel world,BlockPos pos,BlockState sapling,Random random){
        if(!hasLoadedArea(world,pos))return true;
        if(!hasCompactRoom(world,pos,sapling))return false;
        var vanilla=TreeFeatures.ACACIA;
        if(!vanillaMinimumObstructed(world,pos,vanilla.value().config()))return false;
        var normalEvent=ForgeEventFactory.blockGrowFeature(world,random,pos,vanilla);
        if(normalEvent.getResult()==Event.Result.DENY||normalEvent.getFeature()==null)return true;
        if(!world.getBlockState(pos).equals(sapling))return true;
        world.setBlock(pos,Blocks.AIR.defaultBlockState(),4);
        boolean normalGrown=false;
        try{
            normalGrown=normalEvent.getFeature().value().place(world,world.getChunkSource().getGenerator(),random,pos);
        }finally{
            if(!normalGrown&&world.getBlockState(pos).isAir())world.setBlock(pos,sapling,4);
        }
        if(normalGrown||normalEvent.getFeature()!=vanilla||!world.getBlockState(pos).equals(sapling))return true;
        if(!hasCompactRoom(world,pos,sapling)||!vanillaMinimumObstructed(world,pos,vanilla.value().config()))return true;
        var configuration=new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(Blocks.ACACIA_LOG),new StraightTrunkPlacer(4,0,0),
                BlockStateProvider.simple(Blocks.ACACIA_LEAVES),
                new BlobFoliagePlacer(ConstantInt.of(1),ConstantInt.of(0),2),
                new TwoLayersFeatureSize(3,0,1)).ignoreVines().build();
        Holder<ConfiguredFeature<?,?>> feature=Holder.direct(new ConfiguredFeature<>(Feature.TREE,configuration));
        var event=ForgeEventFactory.blockGrowFeature(world,random,pos,feature);
        // Respect protection/other mods. Do not execute an unvalidated substitute.
        if(event.getResult()==Event.Result.DENY||event.getFeature()!=feature)return true;
        if(!world.getBlockState(pos).equals(sapling))return true;
        world.setBlock(pos,Blocks.AIR.defaultBlockState(),4);
        boolean grown=false;
        try{
            grown=feature.value().place(world,world.getChunkSource().getGenerator(),random,pos);
            return true;
        }finally{
            if(!grown&&world.getBlockState(pos).isAir())world.setBlock(pos,sapling,4);
            if(grown)MillLog.major(null,"GROVE-ACACIA compact-grown pos="+pos);
        }
    }
}
