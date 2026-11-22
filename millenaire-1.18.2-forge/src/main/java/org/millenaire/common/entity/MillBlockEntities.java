package org.millenaire.common.entity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.millenaire.common.block.MillBlocks;

@Mod.EventBusSubscriber(modid = "millenaire", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MillBlockEntities {
    public static BlockEntityType<TileEntityImportTable> IMPORT_TABLE;
    public static BlockEntityType<TileEntityFirePit> FIRE_PIT;
    public static BlockEntityType<TileEntityMockBanner> MOCK_BANNER;
    public static BlockEntityType<TileEntityPanel> PANEL;
    public static BlockEntityType<TileEntityMillBed> MILL_BED;
    public static BlockEntityType<TileEntityLockedChest> LOCKED_CHEST;
    @SubscribeEvent public static void register(RegistryEvent.Register<BlockEntityType<?>> event) {
        IMPORT_TABLE = BlockEntityType.Builder.of(TileEntityImportTable::new, MillBlocks.IMPORT_TABLE).build(null);
        IMPORT_TABLE.setRegistryName("millenaire", "import_table");
        event.getRegistry().register(IMPORT_TABLE);
        FIRE_PIT = BlockEntityType.Builder.of(TileEntityFirePit::new, MillBlocks.FIRE_PIT).build(null);
        FIRE_PIT.setRegistryName("millenaire", "fire_pit");
        event.getRegistry().register(FIRE_PIT);
        MOCK_BANNER = BlockEntityType.Builder.of(TileEntityMockBanner::new, MillBlocks.VILLAGE_BANNER_STANDING, MillBlocks.VILLAGE_BANNER_WALL, MillBlocks.CULTURE_BANNER_STANDING, MillBlocks.CULTURE_BANNER_WALL).build(null);
        MOCK_BANNER.setRegistryName("millenaire", "mockbanner");
        event.getRegistry().register(MOCK_BANNER);
        PANEL = BlockEntityType.Builder.of(TileEntityPanel::new, MillBlocks.PANEL).build(null);
        PANEL.setRegistryName("millenaire", "panel"); event.getRegistry().register(PANEL);
        MILL_BED = BlockEntityType.Builder.of(TileEntityMillBed::new, MillBlocks.BED_STRAW, MillBlocks.BED_CHARPOY).build(null);
        MILL_BED.setRegistryName("millenaire", "millbed"); event.getRegistry().register(MILL_BED);
        LOCKED_CHEST=BlockEntityType.Builder.of(TileEntityLockedChest::new,MillBlocks.LOCKED_CHEST).build(null);
        LOCKED_CHEST.setRegistryName("millenaire","locked_chest");event.getRegistry().register(LOCKED_CHEST);
    }
}
