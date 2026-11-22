/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockHorizontal
 *  net.minecraft.block.SoundType
 *  net.minecraft.block.material.Material
 *  net.minecraft.block.properties.IProperty
 *  net.minecraft.block.properties.PropertyDirection
 *  net.minecraft.block.state.BlockFaceShape
 *  net.minecraft.block.state.BlockStateContainer
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.client.renderer.block.model.ModelResourceLocation
 *  net.minecraft.creativetab.CreativeTabs
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.item.Item
 *  net.minecraft.util.BlockRenderLayer
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.EnumFacing$Axis
 *  net.minecraft.util.EnumFacing$Plane
 *  net.minecraft.util.Mirror
 *  net.minecraft.util.Rotation
 *  net.minecraft.util.math.AxisAlignedBB
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.World
 *  net.minecraftforge.client.model.ModelLoader
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package net.minecraft.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.millenaire.common.block.MillBlocks;

public class BlockMillWallItem
extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    protected static final AABB LADDER_EAST_AABB = new AABB(0.0, 0.0, 0.0, 0.1875, 1.0, 1.0);
    protected static final AABB LADDER_WEST_AABB = new AABB(0.8125, 0.0, 0.0, 1.0, 1.0, 1.0);
    protected static final AABB LADDER_SOUTH_AABB = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 0.1875);
    protected static final AABB LADDER_NORTH_AABB = new AABB(0.0, 0.0, 0.8125, 1.0, 1.0, 1.0);

    public BlockMillWallItem(String blockName, Material material, SoundType soundType) {
        super(material);
        this.func_149672_a(soundType);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.func_149647_a(CreativeModeTab.TAB_DECORATIONS);
        this.func_149663_c("millenaire." + blockName);
        this.setRegistryName(blockName);
        this.func_149647_a(MillBlocks.tabMillenaire);
        this.setHarvestLevel("pickaxe", 0);
    }

    public AABB func_185496_a(BlockState state, BlockGetter source, BlockPos pos) {
        switch ((Direction)state.getValue(FACING)) {
            case NORTH: {
                return LADDER_NORTH_AABB;
            }
            case SOUTH: {
                return LADDER_SOUTH_AABB;
            }
            case WEST: {
                return LADDER_WEST_AABB;
            }
        }
        return LADDER_EAST_AABB;
    }

    public boolean func_149662_c(BlockState state) {
        return false;
    }

    public boolean func_149686_d(BlockState state) {
        return false;
    }

    public boolean func_176198_a(Level worldIn, BlockPos pos, Direction side) {
        if (this.canAttachTo(worldIn, pos.west(), side)) {
            return true;
        }
        if (this.canAttachTo(worldIn, pos.east(), side)) {
            return true;
        }
        if (this.canAttachTo(worldIn, pos.north(), side)) {
            return true;
        }
        return this.canAttachTo(worldIn, pos.south(), side);
    }

    private boolean canAttachTo(Level p_193392_1_, BlockPos p_193392_2_, Direction p_193392_3_) {
        BlockState iblockstate = p_193392_1_.getBlockState(p_193392_2_);
        boolean flag = BlockMillWallItem.func_193382_c((Block)iblockstate.getBlock());
        return !flag && iblockstate.func_193401_d((BlockGetter)p_193392_1_, p_193392_2_, p_193392_3_) == BlockFaceShape.SOLID && !iblockstate.isSignalSource();
    }

    public BlockState func_180642_a(Level worldIn, BlockPos pos, Direction facing, float hitX, float hitY, float hitZ, int meta, LivingEntity placer) {
        if (facing.getAxis().isHorizontal() && this.canAttachTo(worldIn, pos.relative(facing.getOpposite()), facing)) {
            return this.defaultBlockState().setValue(FACING, facing);
        }
        for (Direction enumfacing : Direction.Plane.HORIZONTAL) {
            if (!this.canAttachTo(worldIn, pos.relative(enumfacing.getOpposite()), enumfacing)) continue;
            return this.defaultBlockState().setValue(FACING, enumfacing);
        }
        return this.defaultBlockState();
    }

    public void func_189540_a(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        Direction enumfacing = (Direction)state.getValue(FACING);
        if (!this.canAttachTo(worldIn, pos.relative(enumfacing.getOpposite()), enumfacing)) {
            this.func_176226_b(worldIn, pos, state, 0);
            worldIn.func_175698_g(pos);
        }
        super.func_189540_a(state, worldIn, pos, blockIn, fromPos);
    }

    public BlockState func_176203_a(int meta) {
        Direction enumfacing = Direction.from3DDataValue((int)meta);
        if (enumfacing.getAxis() == Direction.Axis.Y) {
            enumfacing = Direction.NORTH;
        }
        return this.defaultBlockState().setValue(FACING, enumfacing);
    }

    @OnlyIn(value=Dist.CLIENT)
    public BlockRenderLayer func_180664_k() {
        return BlockRenderLayer.CUTOUT;
    }

    public int func_176201_c(BlockState state) {
        return ((Direction)state.getValue(FACING)).get3DDataValue();
    }

    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate((Direction)state.getValue(FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation((Direction)state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    public boolean isLadder(BlockState state, BlockGetter world, BlockPos pos, LivingEntity entity) {
        return true;
    }

    public BlockFaceShape func_193383_a(BlockGetter worldIn, BlockState state, BlockPos pos, Direction face) {
        return BlockFaceShape.UNDEFINED;
    }

    @OnlyIn(value=Dist.CLIENT)
    public void initModel() {
        ModelLoader.setCustomModelResourceLocation((Item)Item.byBlock((Block)this), (int)0, (ModelResourceLocation)new ModelResourceLocation(this.getRegistryName(), ""));
    }
}

