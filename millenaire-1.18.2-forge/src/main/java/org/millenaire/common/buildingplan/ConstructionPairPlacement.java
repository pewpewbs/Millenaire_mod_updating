package org.millenaire.common.buildingplan;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Complete a multipart fixture before its halves inspect each other. */
public final class ConstructionPairPlacement {
    private ConstructionPairPlacement() {}

    static BlockPos partner(BlockPos p,BlockState s){
        if(s.getBlock() instanceof DoorBlock || s.getBlock() instanceof DoublePlantBlock)
            return s.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)==DoubleBlockHalf.LOWER?p.above():p.below();
        if(s.getBlock() instanceof BedBlock){
            Direction facing=s.getValue(BedBlock.FACING);
            return p.relative(s.getValue(BedBlock.PART)==BedPart.FOOT?facing:facing.getOpposite());
        }
        return null;
    }

    private static BlockState otherState(BlockState s){
        if(s.getBlock() instanceof BedBlock)return s.setValue(BedBlock.PART,
                s.getValue(BedBlock.PART)==BedPart.HEAD?BedPart.FOOT:BedPart.HEAD);
        return s.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
                s.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)==DoubleBlockHalf.LOWER?DoubleBlockHalf.UPPER:DoubleBlockHalf.LOWER);
    }

    static boolean matches(Level world,BlockPos p,BlockState planned){
        BlockPos q=partner(p,planned);
        return q==null || (!world.isOutsideBuildHeight(q) && world.hasChunkAt(q)
                && world.getBlockState(q).equals(otherState(planned)));
    }

    // Used for beds and tall plants. Doors keep their established hinge logic.
    static boolean place(Level world,BlockPos p,BlockState state){
        BlockPos q=partner(p,state);
        if(q==null || world.isOutsideBuildHeight(p) || world.isOutsideBuildHeight(q))return false;
        BlockState other=otherState(state);
        if(state.getBlock() instanceof DoublePlantBlock){
            BlockPos bottom=state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)==DoubleBlockHalf.LOWER?p:q;
            BlockState lower=state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.LOWER);
            if(!lower.canSurvive(world,bottom))return false;
        }
        world.setBlock(p,state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
        world.setBlock(q,other,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
        finish(world,p);
        finish(world,q);
        return world.getBlockState(p).equals(state)&&world.getBlockState(q).equals(other);
    }

    /** Remove an old, explicitly replaced pair without dropping the other half. */
    static void removeIfReplaced(Level world,BlockPos p,BlockState replacement){
        BlockState old=world.getBlockState(p);
        BlockPos q=partner(p,old);
        if(q==null || old.getBlock()==replacement.getBlock()
                || world.isOutsideBuildHeight(q) || !world.hasChunkAt(q))return;
        BlockState other=world.getBlockState(q);
        if(other.getBlock()!=old.getBlock() || !p.equals(partner(q,other)))return;
        world.setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
        world.setBlock(q,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
        finish(world,p);
        finish(world,q);
    }

    static void finish(Level world,BlockPos p){
        BlockState current=world.getBlockState(p);
        world.updateNeighborsAt(p,current.getBlock());
        current.updateNeighbourShapes(world,p,Block.UPDATE_CLIENTS);
        current.updateIndirectNeighbourShapes(world,p,Block.UPDATE_CLIENTS);
    }
}
