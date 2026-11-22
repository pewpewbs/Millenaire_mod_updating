package net.minecraftforge.registries;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.nbt.CompoundTag;
import org.millenaire.common.item.*;
public class SpecialItemCheck {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
        ((ForgeRegistry<Item>)ForgeRegistries.ITEMS).unfreeze();((NamespacedDefaultedWrapper<Item>)Registry.ITEM).unfreeze();
        var crown=new ItemMayanQuestCrown("mayanquestcrown_check",EquipmentSlot.HEAD);ForgeRegistries.ITEMS.register(crown);
        var stack=new ItemStack(crown);check(!stack.isDamageableItem(),"quest crown must retain unlimited durability");
        crown.applyEnchantments(stack);check(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.RESPIRATION,stack)==3,"respiration applied");
        check(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.AQUA_AFFINITY,stack)==1,"aqua affinity applied");
        check(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION,stack)==4,"protection applied");
        var first=stack.save(new CompoundTag());crown.applyEnchantments(stack);check(first.equals(stack.save(new CompoundTag())),"enchantments not duplicated on every tick");
        var stronger=new ItemStack(crown);stronger.enchant(Enchantments.RESPIRATION,5);crown.applyEnchantments(stronger);check(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.RESPIRATION,stronger)==5,"existing enchantment not overwritten");
        check(ItemStack.of(stack.save(new CompoundTag())).is(crown),"crown survives stack save/load");
        var bow=new ItemMillenaireBow("bow_check",2,.5f,20);ForgeRegistries.ITEMS.register(bow);
        check(new ItemStack(bow).getMaxDamage()==new ItemStack(Items.BOW).getMaxDamage(),"bow durability preserved");
        check(bow.getEnchantmentValue()==20 && bow.speedFactor==2 && bow.damageBonus==.5f,"bow properties retained for villager combat");
        var purse=new ItemPurse("purse_check");ForgeRegistries.ITEMS.register(purse);var coins=new ItemStack(purse);
        coins.getOrCreateTag().putInt("ml_Purse_DENIER",7);coins.getTag().putInt("ml_Purse_DENIERargent",3);coins.getTag().putInt("ml_Purse_DENIERor",2);
        check(purse.totalDeniers(coins)==7+3*64+2*4096,"old purse denomination fields preserved");
        String name=coins.getHoverName().getString();check(name.contains("2o") && name.contains("3a") && name.contains("7d"),"modern hover name includes all denominations");
        check(purse.totalDeniers(ItemStack.of(coins.save(new CompoundTag())))==purse.totalDeniers(coins),"purse balance survives NBT save/load");
        System.out.println("PASS: crown enchantments and durability, bow properties, purse NBT and hover name; live item use not tested");
    }
}
