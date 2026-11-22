package net.minecraftforge.registries;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import org.millenaire.common.block.mock.*;
import org.millenaire.common.entity.*;
import org.millenaire.common.item.ItemMockBanner;

/** Uses isolated registry holders; does not launch Forge or test rendering in game. */
public class BannerStateCheck {
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
        ((ForgeRegistry<Block>)ForgeRegistries.BLOCKS).unfreeze();
        ((ForgeRegistry<Item>)ForgeRegistries.ITEMS).unfreeze();
        ((NamespacedDefaultedWrapper<Block>)Registry.BLOCK).unfreeze();
        ((NamespacedDefaultedWrapper<Item>)Registry.ITEM).unfreeze();
        Block vanillaRed = BannerBlock.byColor(DyeColor.RED), vanillaYellow = BannerBlock.byColor(DyeColor.YELLOW);
        var standing = new MockBlockBannerStanding(ItemMockBanner.BANNER_VILLAGE); standing.setRegistryName("millenaire","banner_check_standing"); ForgeRegistries.BLOCKS.register(standing);
        var wall = new MockBlockBannerHanging(ItemMockBanner.BANNER_VILLAGE); wall.setRegistryName("millenaire","banner_check_wall"); ForgeRegistries.BLOCKS.register(wall);
        var culture = new MockBlockBannerStanding(ItemMockBanner.BANNER_CULTURE); culture.setRegistryName("millenaire","culture_check"); ForgeRegistries.BLOCKS.register(culture);
        check(BannerBlock.byColor(DyeColor.RED) == vanillaRed && BannerBlock.byColor(DyeColor.YELLOW) == vanillaYellow, "custom banners must not replace vanilla color mapping");
        var item = new ItemMockBanner(standing,wall,DyeColor.RED,ItemMockBanner.BANNER_VILLAGE); item.setRegistryName("millenaire","banner_check"); ForgeRegistries.ITEMS.register(item); item.registerBlocks(Item.BY_BLOCK,item);
        MillBlockEntities.MOCK_BANNER = BlockEntityType.Builder.of(TileEntityMockBanner::new,standing,wall,culture).build(null);
        for (int meta=0;meta<16;meta++) check(standing.func_176201_c(standing.func_176203_a(meta))==meta,"all standing rotations survive codec");
        for (int meta=2;meta<6;meta++) check(wall.func_176201_c(wall.func_176203_a(meta))==meta,"wall direction survives codec");
        CompoundTag legacy = TagParser.parseTag(ItemMockBanner.BANNER_DESIGNS[0]);
        ItemStack stack = ItemMockBanner.makeBanner(item,DyeColor.RED,legacy);
        check(legacy.getList("Patterns",10).getCompound(0).getInt("Color")==15,"conversion does not mutate source template");
        check(stack.getTagElement("BlockEntityTag").getList("Patterns",10).getCompound(0).getInt("Color")==DyeColor.WHITE.getId(),"old dye white converts to modern white");
        stack.getTagElement("BlockEntityTag").putInt("Base",DyeColor.BLUE.getId()); stack.setHoverName(new TextComponent("Village test"));
        var banner = new TileEntityMockBanner(BlockPos.ZERO,standing.defaultBlockState()); banner.func_175112_a(stack,true);
        check(banner.getBaseColor()==DyeColor.BLUE && banner.getPatterns().size()==3,"village pattern can override default base color");
        var restored = new TileEntityMockBanner(BlockPos.ZERO,wall.defaultBlockState()); restored.load(banner.getUpdateTag());
        check(restored.getBaseColor()==DyeColor.BLUE && restored.getPatterns().equals(banner.getPatterns()),"base and patterns survive save/load");
        check(restored.getName().getString().equals("Village test"),"custom banner name survives save/load");
        check(restored.getItem().is(item) && restored.getItem().getTagElement("BlockEntityTag").getInt("Base")==DyeColor.BLUE.getId(),"wall banner drops custom item and retains base");
        check(restored.getType()==MillBlockEntities.MOCK_BANNER,"custom block entity type retained");
        System.out.println("Banner orientation, vanilla isolation, legacy palette conversion, name/pattern/base persistence and custom item checks passed (isolated registry fixture)");
    }
    private static void check(boolean ok,String reason) { if (!ok) throw new AssertionError(reason); }
}
