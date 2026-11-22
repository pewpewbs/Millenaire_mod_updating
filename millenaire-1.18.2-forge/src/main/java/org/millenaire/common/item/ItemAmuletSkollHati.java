/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.ActionResult
 *  net.minecraft.util.EnumActionResult
 *  net.minecraft.util.EnumHand
 *  net.minecraft.world.World
 */
package org.millenaire.common.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.item.ItemMill;
import org.millenaire.common.utilities.MillLog;

public class ItemAmuletSkollHati
extends ItemMill {
    public ItemAmuletSkollHati(String itemName) {
        super(itemName, new net.minecraft.world.item.Item.Properties().stacksTo(1));
    }

    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        if (MillConfigValues.LogOther >= 3) {
            MillLog.debug((Object)this, "Using skoll amulet.");
        }
        if (worldIn.isClientSide) {
            return new InteractionResultHolder(InteractionResult.SUCCESS, (Object)playerIn.getItemInHand(handIn));
        }
        long time = worldIn.getDayTime() + 24000L;
        if (time % 24000L > 11000L && time % 24000L < 23500L) {
            ((net.minecraft.server.level.ServerLevel)worldIn).setDayTime(time - time % 24000L - 500L);
        } else {
            ((net.minecraft.server.level.ServerLevel)worldIn).setDayTime(time - time % 24000L + 13000L);
        }
        return new InteractionResultHolder(InteractionResult.SUCCESS, (Object)playerIn.getItemInHand(handIn));
    }
}

