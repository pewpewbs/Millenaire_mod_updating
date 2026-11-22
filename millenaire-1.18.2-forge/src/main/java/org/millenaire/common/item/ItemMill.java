package org.millenaire.common.item;

import net.minecraft.world.item.Item;
import org.millenaire.common.block.MillBlocks;

public class ItemMill extends Item {
    public ItemMill(String name) {
        this(name, new Item.Properties());
    }
    public ItemMill(String name, Item.Properties properties) {
        super(properties.tab(MillBlocks.tabMillenaire));
        setRegistryName("millenaire", name);
    }
}
