package org.millenaire.common.item;
import net.minecraft.core.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.millenaire.common.entity.EntityWallDecoration;

public class ItemWallDecoration extends ItemMill {
    public final int type;
    public ItemWallDecoration(String name, int type) { super(name); this.type=type; }
    @Override public InteractionResult useOn(UseOnContext context) {
        Player player=context.getPlayer(); Level level=context.getLevel();
        Direction face=context.getClickedFace(); BlockPos anchor=context.getClickedPos().relative(face);
        ItemStack stack=context.getItemInHand();
        if (!face.getAxis().isHorizontal() || (player != null && !player.mayUseItemAt(anchor,face,stack))) return InteractionResult.FAIL;
        if (player != null && !level.mayInteract(player,context.getClickedPos())) return InteractionResult.FAIL;
        EntityWallDecoration decoration=new EntityWallDecoration(level,anchor,face,type,false);
        if (!decoration.survives()) return InteractionResult.CONSUME;
        if (!level.isClientSide) {
            if (!level.addFreshEntity(decoration)) return InteractionResult.FAIL;
            decoration.playPlacementSound();
            level.gameEvent(player,GameEvent.ENTITY_PLACE,anchor);
        }
        stack.shrink(1);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
