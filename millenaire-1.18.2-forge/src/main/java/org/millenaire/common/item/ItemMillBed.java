package org.millenaire.common.item;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import org.millenaire.common.block.MillBlocks;
/** BedBlock handles the second half, placement sound, criteria and creative inventory rules. */
public class ItemMillBed extends BlockItem {
    public ItemMillBed(Block bed) { super(bed,new Item.Properties().tab(MillBlocks.tabMillenaire)); }
}
