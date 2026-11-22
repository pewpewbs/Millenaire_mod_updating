package org.millenaire.common.block.mock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.millenaire.common.block.MillBlocks;

public class MockBlockMainChest
extends Block implements org.millenaire.common.block.LegacyVariantBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public MockBlockMainChest(String blockName) {
        super(Properties.of(Material.WOOD).strength(-1.0F, 0.0F));
        this.setRegistryName(blockName);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    public int func_176201_c(BlockState state) {
        return ((Direction)state.getValue(FACING)).get3DDataValue();
    }

    public BlockState func_176203_a(int meta) {
        Direction enumfacing = Direction.from3DDataValue((int)meta);
        if (enumfacing.getAxis() == Direction.Axis.Y) {
            enumfacing = Direction.NORTH;
        }
        return this.defaultBlockState().setValue(FACING, enumfacing);
    }

    @OnlyIn(value=Dist.CLIENT)
    public void initModel() {
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(this, net.minecraft.client.renderer.RenderType.solid());
    }

    @Override public int itemMetadata(BlockState state) { return 0; }
    @Override public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) { return rotate(state, mirror.getRotation(state.getValue(FACING))); }
    @Override public java.util.List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootContext.Builder builder) { return java.util.List.of(new ItemStack(this)); }
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        Direction enumfacing = Direction.from2DDataValue((int)(Mth.floor((double)((double)(placer.getYRot() * 4.0f / 360.0f) + 0.5)) & 3)).getOpposite();
        state = state.setValue(FACING, enumfacing);
        worldIn.setBlock(pos, state, 3);
    }
}

