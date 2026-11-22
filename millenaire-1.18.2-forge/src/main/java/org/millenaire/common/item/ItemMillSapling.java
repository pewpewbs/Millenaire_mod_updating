package org.millenaire.common.item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.network.ServerSender;
public class ItemMillSapling extends BlockItem {
    public final String cropKey;
    public ItemMillSapling(Block block,String cropKey) {
        super(block,new Item.Properties().tab(MillBlocks.tabMillenaire));this.cropKey=cropKey;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var world=context.getLevel();var player=context.getPlayer();
        if(player==null)return InteractionResult.FAIL;
        if(!MillConfigValues.DEV) {
            var data=Mill.getMillWorld(world);
            var profile=data==null?null:data.getProfile(player);
            if(profile==null || !profile.isTagSet("cropplanting_"+cropKey)) {
                if(!world.isClientSide)ServerSender.sendTranslatedSentence(player,'f',"ui.cropplantingknowledge");
                // Let the server decide while the client profile is still loading.
                return world.isClientSide?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        }
        return super.useOn(context);
    }
}
