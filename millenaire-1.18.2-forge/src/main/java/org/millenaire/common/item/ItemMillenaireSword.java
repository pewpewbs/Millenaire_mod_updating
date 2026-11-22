/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.renderer.block.model.ModelResourceLocation
 *  net.minecraft.enchantment.Enchantment
 *  net.minecraft.enchantment.EnchantmentHelper
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.init.Enchantments
 *  net.minecraft.item.Item
 *  net.minecraft.item.Item$ToolMaterial
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.ItemSword
 *  net.minecraft.util.EnumActionResult
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.EnumHand
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.World
 *  net.minecraftforge.client.model.ModelLoader
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package org.millenaire.common.item;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.millenaire.common.advancements.MillAdvancements;
import org.millenaire.common.block.MillBlocks;
import org.millenaire.common.item.InvItem;

public class ItemMillenaireSword
extends SwordItem
implements InvItem.IItemInitialEnchantmens {
    private final boolean knockback;
    private final int enchantability;

    public ItemMillenaireSword(String itemName, net.minecraft.world.item.Tier material, int enchantability, boolean knockback) {
        super(material, 3, -2.4F, new Item.Properties().tab(MillBlocks.tabMillenaire));
        this.knockback = knockback;
        this.enchantability = enchantability;
        this.setRegistryName(itemName);
    }

    @Override
    public void applyEnchantments(ItemStack stack) {
        if (this.knockback && EnchantmentHelper.getItemEnchantmentLevel((Enchantment)Enchantments.KNOCKBACK, (ItemStack)stack) == 0) {
            stack.enchant(Enchantments.KNOCKBACK, 2);
        }
    }

    public int getEnchantmentValue() {
        if (this.enchantability >= 0) {
            return this.enchantability;
        }
        return super.getEnchantmentValue();
    }


    public void onCraftedBy(ItemStack stack, Level par2World, Player par3EntityPlayer) {
        this.applyEnchantments(stack);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, net.minecraft.world.item.context.UseOnContext context) {
        this.applyEnchantments(context.getItemInHand());
        return super.onItemUseFirst(stack, context);
    }

    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (entity instanceof Player) {
            MillAdvancements.MP_WEAPON.grant(player);
        }
        this.applyEnchantments(stack);
        return super.onLeftClickEntity(stack, player, entity);
    }
}

