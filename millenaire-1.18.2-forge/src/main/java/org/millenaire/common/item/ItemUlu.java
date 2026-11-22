package org.millenaire.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.millenaire.common.block.BlockSod;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.block.WoodVariant;
import org.millenaire.common.network.ServerSender;
import org.millenaire.common.utilities.LanguageUtilities;

public class ItemUlu extends ItemMill {
    public ItemUlu(String name) { super(name, new net.minecraft.world.item.Item.Properties().durability(512)); }

    @Override public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        if (!stack.hasTag()) return base;
        return base.copy().append(new TextComponent(" - " + LanguageUtilities.string("ui.ulusodplanksleft", Integer.toString(stack.getTag().getInt("resUseCount")))));
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) return InteractionResult.FAIL;
        BlockState state = level.getBlockState(pos);
        ItemStack product = ItemStack.EMPTY;
        if (state.is(Blocks.SNOW_BLOCK)) product = new ItemStack(MillBlocks.SNOW_BRICK, 4);
        else if (state.is(Blocks.SNOW)) product = new ItemStack(MillBlocks.SNOW_BRICK, (state.getValue(SnowLayerBlock.LAYERS) + 1) / 2);
        else if (state.is(Blocks.ICE)) product = new ItemStack(MillBlocks.ICE_BRICK, 4);
        if (!product.isEmpty()) {
            if (!level.isClientSide && level.removeBlock(pos, false)) {
                if (!player.getInventory().add(product)) player.drop(product, false);
                stack.hurtAndBreak(1, player, user -> user.broadcastBreakEvent(context.getHand()));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return placeSod(stack, context, player);
    }
    private InteractionResult placeSod(ItemStack stack, UseOnContext context, Player player) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.isEmptyBlock(pos) || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) return InteractionResult.PASS;
        WoodVariant chosen = null;
        int plankSlot = -1;
        for (WoodVariant wood : WoodVariant.values()) {
            int slot = find(player, wood.planks().asItem());
            if (slot >= 0) { chosen = wood; plankSlot = slot; break; }
        }
        if (chosen == null) {
            if (!level.isClientSide) {
                ServerSender.sendTranslatedSentence(player, 'f', "ui.uluexplanations");
                ServerSender.sendTranslatedSentence(player, '6', "ui.ulunoplanks");
            }
            return InteractionResult.PASS;
        }
        int remaining = stack.hasTag() ? stack.getTag().getInt("resUseCount") : 0;
        int dirtSlot = find(player, Blocks.COARSE_DIRT.asItem());
        if (remaining <= 0 && dirtSlot < 0) {
            if (!level.isClientSide) ServerSender.sendTranslatedSentence(player, '6', "ui.ulunodirt");
            return InteractionResult.PASS;
        }
        BlockState sod = MillBlocks.SOD.defaultBlockState().setValue(BlockSod.VARIANT, chosen);
        if (!sod.canSurvive(level, pos) || !level.isUnobstructed(sod, pos, CollisionContext.empty())) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (!level.setBlockAndUpdate(pos, sod)) return InteractionResult.FAIL;
            if (remaining <= 0) {
                player.getInventory().removeItem(dirtSlot, 1);
                player.getInventory().removeItem(plankSlot, 1);
                remaining = 3;
            } else --remaining;
            stack.getOrCreateTag().putInt("resUseCount", remaining);
            stack.hurtAndBreak(1, player, user -> user.broadcastBreakEvent(context.getHand()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    private static int find(Player player, net.minecraft.world.item.Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (!candidate.isEmpty() && candidate.is(item)) return slot;
        }
        return -1;
    }
}
