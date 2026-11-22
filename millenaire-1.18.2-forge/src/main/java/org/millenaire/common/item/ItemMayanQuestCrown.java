package org.millenaire.common.item;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;
import org.millenaire.common.block.MillBlocks;
public class ItemMayanQuestCrown extends ArmorItem implements InvItem.IItemInitialEnchantmens {
    public ItemMayanQuestCrown(String name,EquipmentSlot slot) {
        super(MillItems.ARMOUR_mayan_quest_crown,slot,new Item.Properties().stacksTo(1).tab(MillBlocks.tabMillenaire));
        setRegistryName("millenaire",name);
    }
    @Override public void applyEnchantments(ItemStack stack) {
        if(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.RESPIRATION,stack)==0)stack.enchant(Enchantments.RESPIRATION,3);
        if(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.AQUA_AFFINITY,stack)==0)stack.enchant(Enchantments.AQUA_AFFINITY,1);
        if(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION,stack)==0)stack.enchant(Enchantments.ALL_DAMAGE_PROTECTION,4);
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context) {
        if(!context.getLevel().isClientSide)applyEnchantments(stack);
        return InteractionResult.PASS;
    }
    @Override public void inventoryTick(ItemStack stack,Level world,Entity entity,int slot,boolean selected) {
        if(!world.isClientSide)applyEnchantments(stack);
        super.inventoryTick(stack,world,entity,slot,selected);
    }
}
