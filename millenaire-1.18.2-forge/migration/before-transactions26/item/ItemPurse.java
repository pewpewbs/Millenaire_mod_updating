/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.resources.I18n
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.util.ActionResult
 *  net.minecraft.util.EnumHand
 *  net.minecraft.world.World
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package org.millenaire.common.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.millenaire.common.item.ItemMill;
import org.millenaire.common.item.MillItems;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.WorldUtilities;

public class ItemPurse
extends ItemMill {
    private static final String ML_PURSE_DENIER = "ml_Purse_DENIER";
    private static final String ML_PURSE_DENIERARGENT = "ml_Purse_DENIERargent";
    private static final String ML_PURSE_DENIEROR = "ml_Purse_DENIERor";
    private static final String ML_PURSE_RAND = "ml_Purse_rand";

    public ItemPurse(String itemName) {
        super(itemName, new net.minecraft.world.item.Item.Properties().stacksTo(1));
    }

    @Override public net.minecraft.network.chat.Component getName(ItemStack purse) {
        var name=new net.minecraft.network.chat.TranslatableComponent(getDescriptionId());
        if(purse.getTag()==null)return name;
        int copper=purse.getTag().getInt(ML_PURSE_DENIER),silver=purse.getTag().getInt(ML_PURSE_DENIERARGENT),gold=purse.getTag().getInt(ML_PURSE_DENIEROR);
        var result=name.withStyle(net.minecraft.ChatFormatting.WHITE).append(": ");
        if(gold!=0)result.append(new net.minecraft.network.chat.TextComponent(gold+"o ").withStyle(net.minecraft.ChatFormatting.YELLOW));
        if(silver!=0)result.append(new net.minecraft.network.chat.TextComponent(silver+"a ").withStyle(net.minecraft.ChatFormatting.WHITE));
        if(copper!=0 || (gold==0 && silver==0))result.append(new net.minecraft.network.chat.TextComponent(copper+"d").withStyle(net.minecraft.ChatFormatting.GOLD));
        return result;
    }

    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack purse = playerIn.getItemInHand(handIn);
        if(worldIn.isClientSide)return InteractionResultHolder.success(purse);
        if (this.totalDeniers(purse) > 0) {
            this.removeDeniersFromPurse(purse, playerIn);
        } else {
            this.storeDeniersInPurse(purse, playerIn);
        }
        playerIn.getInventory().setChanged();
        return InteractionResultHolder.consume(purse);
    }

    private void removeDeniersFromPurse(ItemStack purse, Player player) {
        if (purse.getTag() != null) {
            int DENIERs = purse.getTag().getInt(ML_PURSE_DENIER);
            int DENIERargent = purse.getTag().getInt(ML_PURSE_DENIERARGENT);
            int DENIERor = purse.getTag().getInt(ML_PURSE_DENIEROR);
            int result = MillCommonUtilities.putItemsInChest((Container)player.getInventory(), MillItems.DENIER, DENIERs);
            purse.getTag().putInt(ML_PURSE_DENIER, DENIERs - result);
            result = MillCommonUtilities.putItemsInChest((Container)player.getInventory(), MillItems.DENIER_ARGENT, DENIERargent);
            purse.getTag().putInt(ML_PURSE_DENIERARGENT, DENIERargent - result);
            result = MillCommonUtilities.putItemsInChest((Container)player.getInventory(), MillItems.DENIER_OR, DENIERor);
            purse.getTag().putInt(ML_PURSE_DENIEROR, DENIERor - result);
            purse.getTag().putInt(ML_PURSE_RAND, player.level.isClientSide ? 0 : 1);
        }
    }

    public void setDeniers(ItemStack purse, Player player, int amount) {
        int denier = amount % 64;
        int denier_argent = (amount - denier) / 64 % 64;
        int denier_or = (amount - denier - denier_argent * 64) / 4096;
        this.setDeniers(purse, player, denier, denier_argent, denier_or);
    }

    public void setDeniers(ItemStack purse, Player player, int DENIER, int DENIERargent, int DENIERor) {
        if (purse.getTag() == null) {
            purse.setTag(new CompoundTag());
        }
        purse.getTag().putInt(ML_PURSE_DENIER, DENIER);
        purse.getTag().putInt(ML_PURSE_DENIERARGENT, DENIERargent);
        purse.getTag().putInt(ML_PURSE_DENIEROR, DENIERor);
        purse.getTag().putInt(ML_PURSE_RAND, player.level.isClientSide ? 0 : 1);
    }

    private void storeDeniersInPurse(ItemStack purse, Player player) {
        int deniers = WorldUtilities.getItemsFromChest((Container)player.getInventory(), MillItems.DENIER, 0, Integer.MAX_VALUE);
        int deniersargent = WorldUtilities.getItemsFromChest((Container)player.getInventory(), MillItems.DENIER_ARGENT, 0, Integer.MAX_VALUE);
        int deniersor = WorldUtilities.getItemsFromChest((Container)player.getInventory(), MillItems.DENIER_OR, 0, Integer.MAX_VALUE);
        int total = this.totalDeniers(purse) + deniers + deniersargent * 64 + deniersor * 64 * 64;
        int new_denier = total % 64;
        int new_deniers_argent = (total - new_denier) / 64 % 64;
        int new_deniers_or = (total - new_denier - new_deniers_argent * 64) / 4096;
        this.setDeniers(purse, player, new_denier, new_deniers_argent, new_deniers_or);
    }

    public int totalDeniers(ItemStack purse) {
        if (purse.getTag() == null) {
            return 0;
        }
        int deniers = purse.getTag().getInt(ML_PURSE_DENIER);
        int denier_argent = purse.getTag().getInt(ML_PURSE_DENIERARGENT);
        int denier_or = purse.getTag().getInt(ML_PURSE_DENIEROR);
        return deniers + denier_argent * 64 + denier_or * 64 * 64;
    }
}

