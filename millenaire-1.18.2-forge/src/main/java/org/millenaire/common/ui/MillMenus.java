package org.millenaire.common.ui;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.millenaire.common.entity.TileEntityFirePit;
import org.millenaire.common.ui.firepit.ContainerFirePit;

@Mod.EventBusSubscriber(modid = "millenaire", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MillMenus {
    public static MenuType<ContainerFirePit> FIRE_PIT;
    public static MenuType<ContainerLockedChest> LOCKED_CHEST;
    public static MenuType<ContainerPuja> PUJA;
    public static MenuType<ContainerTrade> TRADE;
    @SubscribeEvent public static void register(RegistryEvent.Register<MenuType<?>> event) {
        TRADE = IForgeMenuType.create((id, inventory, buffer) -> {
            var world = org.millenaire.common.forge.Mill.getMillWorld(inventory.player.level);
            if (buffer.readBoolean()) {
                var merchant = world.getVillagerById(buffer.readLong());
                if (merchant == null) throw new IllegalStateException("Missing merchant for menu");
                return new ContainerTrade(id, inventory.player, merchant);
            }
            var building = world.getBuilding(new org.millenaire.common.utilities.Point(buffer.readBlockPos()));
            if (building == null) throw new IllegalStateException("Missing shop for menu");
            return new ContainerTrade(id, inventory.player, building);
        });
        TRADE.setRegistryName("millenaire", "trade"); event.getRegistry().register(TRADE);
        PUJA = IForgeMenuType.create((id, inventory, buffer) -> {
            var pos = buffer.readBlockPos();
            var temple = org.millenaire.common.forge.Mill.getMillWorld(inventory.player.level).getBuilding(new org.millenaire.common.utilities.Point(pos));
            if (temple == null || temple.pujas == null) throw new IllegalStateException("Missing shrine for menu");
            return new ContainerPuja(id, inventory, temple);
        });
        PUJA.setRegistryName("millenaire", "puja"); event.getRegistry().register(PUJA);
        FIRE_PIT = IForgeMenuType.create((id, inventory, buffer) -> {
            var entity = inventory.player.level.getBlockEntity(buffer.readBlockPos());
            if (!(entity instanceof TileEntityFirePit pit)) throw new IllegalStateException("Missing fire pit for menu");
            return new ContainerFirePit(id, inventory, pit);
        });
        FIRE_PIT.setRegistryName("millenaire", "fire_pit");
        event.getRegistry().register(FIRE_PIT);
        LOCKED_CHEST = IForgeMenuType.create((id, inventory, buffer) -> {
            int rows=buffer.readVarInt();boolean locked=buffer.readBoolean();
            if (rows!=3 && rows!=6) throw new IllegalArgumentException("Invalid chest menu row count: "+rows);
            return new ContainerLockedChest(id,inventory,new net.minecraft.world.SimpleContainer(rows*9),inventory.player,null,locked);
        });
        LOCKED_CHEST.setRegistryName("millenaire","locked_chest");event.getRegistry().register(LOCKED_CHEST);
    }
}
