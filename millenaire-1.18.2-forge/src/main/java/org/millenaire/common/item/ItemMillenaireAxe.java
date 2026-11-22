package org.millenaire.common.item;

import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlot;
import org.millenaire.common.block.MillBlocks;

public class ItemMillenaireAxe extends AxeItem {
    public ItemMillenaireAxe(String itemName, Tier material, float damage, float speed) {
        this(itemName, material, damage, speed, new Item.Properties().tab(MillBlocks.tabMillenaire));
    }

    private ItemMillenaireAxe(String itemName, Tier material, float damage, float speed, Item.Properties properties) {
        super(material, damage - material.getAttackDamageBonus(), speed, properties);
        setRegistryName("millenaire", itemName);
    }
}
