package org.millenaire.common.item;

import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlot;
import org.millenaire.common.block.MillBlocks;

public class ItemMillenaireShovel extends ShovelItem {
    public ItemMillenaireShovel(String itemName, Tier material) {
        this(itemName, material, new Item.Properties().tab(MillBlocks.tabMillenaire));
    }

    private ItemMillenaireShovel(String itemName, Tier material, Item.Properties properties) {
        super(material, 1.5F, -3.0F, properties);
        setRegistryName("millenaire", itemName);
    }
}
