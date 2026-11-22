package org.millenaire.client.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.millenaire.common.forge.Mill;

@Mod.EventBusSubscriber(modid="millenaire", value=Dist.CLIENT, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientConnectionEvents {
    private ClientConnectionEvents() {}
    @SubscribeEvent public static void loggedIn(ClientPlayerNetworkEvent.LoggedInEvent event) {
        if (Mill.displayMillenaireLocationError) {
            Mill.proxy.sendLocalChat(event.getPlayer(), '4', "ERROR: Could not find the config file at " + Mill.proxy.getConfigFile().getAbsolutePath());
            return;
        }
        if (!Mill.startupError) Mill.proxy.handleClientLogin();
    }
}
