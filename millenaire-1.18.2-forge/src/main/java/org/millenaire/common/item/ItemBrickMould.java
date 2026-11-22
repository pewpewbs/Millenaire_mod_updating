package org.millenaire.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.network.ServerSender;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;

public class ItemBrickMould extends ItemMill {
    public ItemBrickMould(String name) {
        super(name, new net.minecraft.world.item.Item.Properties().durability(512));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        if (!world.isEmptyBlock(pos) || !world.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) return InteractionResult.PASS;
        var brick = MillBlocks.BS_WET_BRICK;
        if (!brick.canSurvive(world, pos) || !world.isUnobstructed(brick, pos, CollisionContext.empty()))
            return InteractionResult.PASS;
        boolean needsResources = stack.getDamageValue() % 4 == 0;
        if (needsResources && (MillCommonUtilities.countChestItems(player.getInventory(), Blocks.DIRT, 0) == 0
                || MillCommonUtilities.countChestItems(player.getInventory(), Blocks.SAND, 0) == 0)) {
            if (!world.isClientSide) ServerSender.sendTranslatedSentence(player, 'f', "ui.brickinstructions");
            return InteractionResult.PASS;
        }
        if (!world.isClientSide) {
            if (needsResources) {
                WorldUtilities.getItemsFromChest(player.getInventory(), Blocks.DIRT, 0, 1);
                WorldUtilities.getItemsFromChest(player.getInventory(), Blocks.SAND, 0, 1);
            }
            WorldUtilities.setBlockstate(world, new Point(pos), brick, true, false);
            stack.hurtAndBreak(1, player, user -> user.broadcastBreakEvent(context.getHand()));
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }
}
