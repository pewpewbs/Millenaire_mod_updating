/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraftforge.fml.common.eventhandler.SubscribeEvent
 *  net.minecraftforge.fml.common.gameevent.TickEvent$ClientTickEvent
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package org.millenaire.client.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.millenaire.client.gui.DisplayActions;
import org.millenaire.common.forge.Mill;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="millenaire",value=Dist.CLIENT)
@OnlyIn(value=Dist.CLIENT)
public class ClientTickHandler {
    private static boolean startupMessageShow;

    @SubscribeEvent
    public static void tickStart(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Mill.clientWorld == null || !Mill.clientWorld.millenaireEnabled || Minecraft.getInstance().player == null) {
            startupMessageShow=false;
            return;
        }
        boolean inOverworld = Minecraft.getInstance().player.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD);
        Mill.clientWorld.updateWorldClient(inOverworld);
        if (!startupMessageShow) {
            DisplayActions.displayStartupOrError((Player)Minecraft.getInstance().player, Mill.startupError);
            startupMessageShow = true;
        }
        Mill.proxy.handleClientGameUpdate();
    }
}

