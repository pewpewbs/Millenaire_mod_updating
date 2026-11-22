package org.millenaire.common.item;
import javax.annotation.Nullable;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import org.millenaire.common.block.MillBlocks;

public class ItemMockBanner extends BannerItem {
    public static final int BANNER_VILLAGE = 0, BANNER_CULTURE = 1;
    public static final DyeColor[] BANNER_COLOURS = {DyeColor.RED, DyeColor.YELLOW};
    // These templates retain their original 1.12 dye numbers; makeBanner converts them.
    public static final String[] BANNER_DESIGNS = {"{Patterns:[{Pattern:dls,Color:15},{Pattern:ls,Color:15}]}", "{Patterns:[{Pattern:ls,Color:0},{Pattern:ts,Color:0},{Pattern:bs,Color:0}]}"};
    private final int bannerDesign;
    public ItemMockBanner(AbstractBannerBlock standing, WallBannerBlock wall, DyeColor color, int design) {
        super(standing, wall, new Item.Properties().stacksTo(16).tab(MillBlocks.tabMillenaireContentCreator)); bannerDesign = design;
    }
    public static ItemStack makeBanner(Item banner, DyeColor color, @Nullable CompoundTag legacyPatterns) {
        ItemStack stack = new ItemStack(banner);
        CompoundTag data = stack.getOrCreateTagElement("BlockEntityTag"); data.putInt("Base", color.getId());
        if (legacyPatterns != null) {
            ListTag patterns = legacyPatterns.getList("Patterns", Tag.TAG_COMPOUND).copy();
            for (int i = 0; i < patterns.size(); i++) { CompoundTag pattern = patterns.getCompound(i); pattern.putInt("Color", 15 - (pattern.getInt("Color") & 15)); }
            data.put("Patterns", patterns);
        }
        return stack;
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.IItemRenderProperties> consumer) {
        consumer.accept(new net.minecraftforge.client.IItemRenderProperties() {
            private final org.millenaire.client.render.MillBannerItemRenderer renderer = new org.millenaire.client.render.MillBannerItemRenderer();
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getItemStackRenderer() { return renderer; }
        });
    }
    @Override public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items) {
        if (!allowdedIn(tab)) return;
        try { items.add(makeBanner(this, getColor(), TagParser.parseTag(BANNER_DESIGNS[bannerDesign]))); }
        catch (CommandSyntaxException exception) { throw new IllegalStateException("Invalid built-in Millenaire banner pattern", exception); }
    }
}
