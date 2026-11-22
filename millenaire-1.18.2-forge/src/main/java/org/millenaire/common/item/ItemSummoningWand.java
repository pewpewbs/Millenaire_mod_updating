package org.millenaire.common.item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.ui.GuiActions;
import org.millenaire.common.utilities.Point;
public class ItemSummoningWand extends ItemMill {
    public ItemSummoningWand(String name) { super(name); }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context) {
        var world=context.getLevel();var pos=context.getClickedPos();
        if(world.getBlockState(pos).is(MillBlocks.IMPORT_TABLE) || world.isClientSide || !world.dimension().equals(Level.OVERWORLD))return InteractionResult.PASS;
        if(!(context.getPlayer() instanceof ServerPlayer player) || !world.mayInteract(player,pos) || !player.mayUseItemAt(pos,context.getClickedFace(),stack))return InteractionResult.FAIL;
        return GuiActions.useSummoningWand(player,new Point(pos));
    }
}
