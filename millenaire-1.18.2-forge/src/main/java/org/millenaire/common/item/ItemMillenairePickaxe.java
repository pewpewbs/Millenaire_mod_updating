package org.millenaire.common.item;

import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlot;
import org.millenaire.common.block.MillBlocks;

public class ItemMillenairePickaxe extends PickaxeItem {
    public ItemMillenairePickaxe(String itemName, Tier material) {
        this(itemName, material, new Item.Properties().tab(MillBlocks.tabMillenaire));
    }

    private ItemMillenairePickaxe(String itemName, Tier material, Item.Properties properties) {
        super(material, 1, -2.8F, properties);
        setRegistryName("millenaire", itemName);
    }
}
