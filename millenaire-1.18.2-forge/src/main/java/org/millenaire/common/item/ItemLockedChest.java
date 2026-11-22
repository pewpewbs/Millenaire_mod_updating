package org.millenaire.common.item;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import org.millenaire.common.block.MillBlocks;
public class ItemLockedChest extends BlockItem {
    public ItemLockedChest(Block block) { super(block,new Item.Properties().tab(MillBlocks.tabMillenaire)); }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.IItemRenderProperties> consumer) {
        consumer.accept(new net.minecraftforge.client.IItemRenderProperties() {
            private final org.millenaire.client.render.MillChestItemRenderer renderer=new org.millenaire.client.render.MillChestItemRenderer();
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getItemStackRenderer() { return renderer; }
        });
    }
}
