package org.millenaire.common.item;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.*;
import org.millenaire.common.block.MillBlocks;

/** Separate pattern items let the vanilla loom select all original Millenaire designs. */
public class ItemBannerPattern extends BannerPatternItem {
    private final int patternIndex;
    public ItemBannerPattern(String name,int index) {
        super(MillBannerPatterns.get(index),new Item.Properties().tab(MillBlocks.tabMillenaire));
        patternIndex=index; setRegistryName(name);
    }
    @Override public String getDescriptionId() { return "item.millenaire.bannerpattern."+MillBannerPatterns.HASHES[patternIndex]+".name"; }
    @Override public String getDescriptionId(ItemStack stack) { return getDescriptionId(); }
    @Override public MutableComponent getDisplayName() { return new TranslatableComponent(getDescriptionId()); }
}
