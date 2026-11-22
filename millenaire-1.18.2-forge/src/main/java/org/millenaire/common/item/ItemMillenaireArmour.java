package org.millenaire.common.item;

import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlot;
import org.millenaire.common.block.MillBlocks;

public class ItemMillenaireArmour extends ArmorItem {
    public ItemMillenaireArmour(String itemName, ArmorMaterial material, EquipmentSlot type) {
        this(itemName, material, type, new Item.Properties().tab(MillBlocks.tabMillenaire));
    }

    private ItemMillenaireArmour(String itemName, ArmorMaterial material, EquipmentSlot type, Item.Properties properties) {
        super(material, type, properties);
        setRegistryName("millenaire", itemName);
    }
}
