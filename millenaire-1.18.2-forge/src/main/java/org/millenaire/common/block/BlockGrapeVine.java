package org.millenaire.common.block;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ForgeHooks;

public class BlockGrapeVine extends BlockMillCrops {
    public static final EnumProperty<DoubleBlockHalf> HALF = EnumProperty.create("half", DoubleBlockHalf.class);
    public BlockGrapeVine(String name, boolean irrigation, boolean slow, ResourceLocation seed) {
        super(name, irrigation, slow, seed);
        registerDefaultState(defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); builder.add(HALF); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.block(); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        return pos.getY() < context.getLevel().getMaxBuildHeight() - 1 && context.getLevel().isEmptyBlock(pos.above())
            && super.canSurvive(defaultBlockState(), context.getLevel(), pos) ? defaultBlockState() : null;
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        BlockState lowerState = level.getBlockState(lower), upperState = level.getBlockState(lower.above());
        return lowerState.is(this) && upperState.is(this) && lowerState.getValue(HALF) == DoubleBlockHalf.LOWER
            && upperState.getValue(HALF) == DoubleBlockHalf.UPPER && super.canSurvive(lowerState, level, lower);
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // Defer validation until both blocks have been placed, including village construction.
        if (!level.isClientSide()) level.scheduleTick(pos, this, 1);
        return state;
    }
    private boolean validatePair(Level level, BlockPos pos, BlockState state) {
        if (canSurvive(state, level, pos)) return true;
        BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        if (level.getBlockState(lower).is(this)) level.removeBlock(lower, false);
        if (level.getBlockState(lower.above()).is(this)) level.removeBlock(lower.above(), false);
        return false;
    }
    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, Random random) { validatePair(level, pos, state); }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }
    private void setPairAge(Level level, BlockPos lower, int age) {
        level.setBlock(lower, withAge(age), 2);
        level.setBlock(lower.above(), withAge(age).setValue(HALF, DoubleBlockHalf.UPPER), 2);
    }
    @Override public void grow(Level level, BlockPos pos, BlockState state) {
        if (!validatePair(level, pos, state)) return;
        BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        setPairAge(level, lower, Math.min(getMaxAge(), getAge(state) + getBonemealAgeIncrease(level)));
    }
    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, Random random) {
        if (!level.isAreaLoaded(pos, 1) || !validatePair(level, pos, state)
                || state.getValue(HALF) == DoubleBlockHalf.UPPER || isMaxAge(state) || level.getMaxLocalRawBrightness(pos.above()) < 9) return;
        float chance = getGrowthChance(this, level, pos);
        if (chance > 0 && ForgeHooks.onCropsGrowPre(level, pos, state, random.nextInt((int)(25 / chance)) == 0)) {
            setPairAge(level, pos, getAge(state) + 1);
            ForgeHooks.onCropsGrowPost(level, pos, state);
        }
    }
    @Override public BlockState func_176203_a(int metadata) { return withAge(metadata & 7).setValue(HALF, (metadata & 8) == 0 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER); }
    @Override public int func_176201_c(BlockState state) { return getAge(state) | (state.getValue(HALF) == DoubleBlockHalf.UPPER ? 8 : 0); }
}
