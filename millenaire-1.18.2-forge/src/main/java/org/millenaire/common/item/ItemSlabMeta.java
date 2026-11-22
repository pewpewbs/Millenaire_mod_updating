package org.millenaire.common.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.millenaire.common.block.LegacyVariantBlock;
import org.millenaire.common.block.BlockOrientedSlab;
import net.minecraft.world.level.block.state.properties.Half;

public class ItemSlabMeta extends ItemBlockMeta {
    private final Block fullBlock;
    public ItemSlabMeta(Block halfBlock, Block fullBlock) { super(halfBlock); this.fullBlock = fullBlock; }

    @Override public InteractionResult useOn(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState state = context.getLevel().getBlockState(pos);
        if (matchesVariant(state, context)) {
            SlabType half = state.hasProperty(SlabBlock.TYPE) ? state.getValue(SlabBlock.TYPE)
                : state.getValue(BlockOrientedSlab.HALF) == Half.TOP ? SlabType.TOP : SlabType.BOTTOM;
            if (context.getClickedFace() == Direction.UP && half == SlabType.BOTTOM
                    || context.getClickedFace() == Direction.DOWN && half == SlabType.TOP) return combine(context, pos, state);
        }
        BlockPos adjacent = pos.relative(context.getClickedFace());
        BlockState neighbor = context.getLevel().getBlockState(adjacent);
        if (matchesVariant(neighbor, context)) return combine(context, adjacent, neighbor);
        return super.useOn(context);
    }
    private boolean matchesVariant(BlockState state, UseOnContext context) {
        if (!state.is(getBlock())) return false;
        if (state.hasProperty(SlabBlock.TYPE)) {
            if (state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) return false;
        } else if (!(getBlock() instanceof BlockOrientedSlab oriented) || oriented.func_176552_j()) return false;
        if (!(getBlock() instanceof LegacyVariantBlock codec)) return false;
        BlockState itemState = codec.func_176203_a(LegacyItemStacks.metadata(context.getItemInHand()));
        Property<?> variant = getBlock().getStateDefinition().getProperty("variant");
        return variant == null || state.getValue(variant).equals(itemState.getValue(variant));
    }
    private InteractionResult combine(UseOnContext context, BlockPos pos, BlockState half) {
        var level = context.getLevel();
        var player = context.getPlayer();
        if (context.getItemInHand().isEmpty() || player != null && (!level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))) return InteractionResult.FAIL;
        BlockState full = fullBlock.defaultBlockState();
        for (Property<?> property : half.getProperties()) {
            if (full.hasProperty(property)) full = copy(full, half, property);
        }
        if (full.hasProperty(SlabBlock.TYPE)) full = full.setValue(SlabBlock.TYPE, SlabType.DOUBLE).setValue(SlabBlock.WATERLOGGED, false);
        if (!level.isUnobstructed(full, pos, CollisionContext.empty())) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            if (!level.setBlock(pos, full, 11)) return InteractionResult.FAIL;
            var sound = full.getSoundType(level, pos, player);
            level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * 0.8F);
            if (player instanceof ServerPlayer serverPlayer) CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, context.getItemInHand());
            if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    private static <T extends Comparable<T>> BlockState copy(BlockState target, BlockState source, Property<T> property) { return target.setValue(property, source.getValue(property)); }
}
