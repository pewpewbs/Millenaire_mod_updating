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

@OnlyIn(value=Dist.CLIENT)
public class ClientTickHandler {
    private boolean startupMessageShow;

    @SubscribeEvent
    public void tickStart(TickEvent.ClientTickEvent event) {
        if (Mill.clientWorld == null || !Mill.clientWorld.millenaireEnabled || Minecraft.getInstance().player == null) {
            return;
        }
        boolean inOverworld = Minecraft.getInstance().player.field_71093_bK == 0;
        Mill.clientWorld.updateWorldClient(inOverworld);
        if (!this.startupMessageShow) {
            DisplayActions.displayStartupOrError((Player)Minecraft.getInstance().player, Mill.startupError);
            this.startupMessageShow = true;
        }
        Mill.proxy.handleClientGameUpdate();
    }
}

