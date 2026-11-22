package org.millenaire.common.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.millenaire.common.block.BlockPathSlab;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.block.BlockPath;
import org.millenaire.common.block.IBlockPath;

public class ItemPathSlab extends BlockItem {
    private final BlockPathSlab singleSlab;
    private final BlockPath doubleSlab;
    public ItemPathSlab(BlockPathSlab slab, BlockPath fullBlock) {
        super(slab, new Item.Properties().tab(MillBlocks.tabMillenaire));
        singleSlab = slab;
        doubleSlab = fullBlock;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState state = context.getLevel().getBlockState(pos);
        Direction face = context.getClickedFace();
        if (state.is(singleSlab)) {
            Half half = state.getValue(BlockPathSlab.HALF);
            if (face == Direction.UP && half == Half.BOTTOM || face == Direction.DOWN && half == Half.TOP)
                return combine(context, pos);
        }
        BlockPos adjacent = pos.relative(face);
        if (context.getLevel().getBlockState(adjacent).is(singleSlab)) return combine(context, adjacent);
        return super.useOn(context);
    }
    private InteractionResult combine(UseOnContext context, BlockPos pos) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (context.getItemInHand().isEmpty() || player != null && (!level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))) return InteractionResult.FAIL;
        BlockState full = doubleSlab.defaultBlockState().setValue(IBlockPath.STABLE, true);
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
}
