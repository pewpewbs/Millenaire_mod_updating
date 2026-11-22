package org.millenaire.common.item;

import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlot;
import org.millenaire.common.block.MillBlocks;

public class ItemMillenaireHoe extends HoeItem {
    public ItemMillenaireHoe(String itemName, Tier material) {
        this(itemName, material, new Item.Properties().tab(MillBlocks.tabMillenaire));
    }

    private ItemMillenaireHoe(String itemName, Tier material, Item.Properties properties) {
        super(material, -(int)material.getAttackDamageBonus(), material.getAttackDamageBonus() - 3.0F, properties);
        setRegistryName("millenaire", itemName);
    }
}
