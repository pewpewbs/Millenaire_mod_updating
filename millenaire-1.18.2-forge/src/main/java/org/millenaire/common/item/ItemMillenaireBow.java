package org.millenaire.common.item;
import net.minecraft.world.item.*;
import org.millenaire.common.block.MillBlocks;
public class ItemMillenaireBow extends BowItem {
    public float speedFactor,damageBonus;
    private final int enchantability;
    public ItemMillenaireBow(String name,float speedFactor,float damageBonus,int enchantability) {
        super(new Item.Properties().durability(384).tab(MillBlocks.tabMillenaire));
        setRegistryName("millenaire",name);this.speedFactor=speedFactor;this.damageBonus=damageBonus;this.enchantability=enchantability;
    }
    @Override public int getEnchantmentValue() { return enchantability; }
}
