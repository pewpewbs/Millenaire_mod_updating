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

    /** Same compact geometry for site selection and growth; no blanket
     * neighbour-marker distance restriction. Only reads already loaded chunks. */
    public static boolean hasCompactRoom(ServerLevel world,BlockPos pos,BlockState sapling){
        if(!hasLoadedArea(world,pos))return false;
        if(pos.getY()<=world.getMinBuildHeight()||pos.getY()+7>=world.getMaxBuildHeight())return false;
        if(!sapling.canSurvive(world,pos))return false;
        var ground=world.getBlockState(pos.below());
        if(!(ground.is(Blocks.GRASS_BLOCK)||ground.is(Blocks.DIRT)||ground.is(Blocks.COARSE_DIRT)
                ||ground.is(Blocks.PODZOL)||ground.is(Blocks.ROOTED_DIRT)))return false;
        // A ground-level fence is compatible with the straight trunk.
        for(int dy=1;dy<=6;dy++)
            for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){
                BlockPos p=pos.offset(dx,dy,dz);
                if(!world.getWorldBorder().isWithinBounds(p)||!world.getBlockState(p).isAir())return false;
            }
        return world.getWorldBorder().isWithinBounds(pos);
    }

    /** True means the compact path handled the attempt (including a Forge veto).
     *  False leaves the original vanilla grower in charge. */
    static boolean tryGrow(ServerLevel world,BlockPos pos,BlockState sapling,Random random){
        if(!hasLoadedArea(world,pos))return true;
        if(!hasCompactRoom(world,pos,sapling))return false;
        var configuration=new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(Blocks.ACACIA_LOG),new StraightTrunkPlacer(4,0,0),
                BlockStateProvider.simple(Blocks.ACACIA_LEAVES),
                new BlobFoliagePlacer(ConstantInt.of(1),ConstantInt.of(0),2),
                new TwoLayersFeatureSize(1,0,1)).ignoreVines().build();
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
