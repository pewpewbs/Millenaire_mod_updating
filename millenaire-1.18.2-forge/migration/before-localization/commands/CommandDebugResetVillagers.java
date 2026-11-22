/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.command.CommandException
 *  net.minecraft.command.MillCommand
 *  net.minecraft.command.CommandSourceStack
 *  net.minecraft.command.WrongUsageException
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.text.TextFormatting
 *  net.minecraft.world.World
 */
package org.millenaire.common.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.network.ServerSender;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;
import org.millenaire.common.village.VillagerRecord;
import org.millenaire.common.world.MillWorldData;

public class CommandDebugResetVillagers
implements MillCommand {
    public boolean canUse(MinecraftServer server, CommandSourceStack sender) {
        return true;
    }

    public int compareTo(MillCommand o) {
        return this.getName().compareTo(o.getName());
    }

    public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) throws CommandRuntimeException {
        Level world = sender.getLevel();
        if (!world.isClientSide) {
            if (!(sender.getEntity() instanceof Player)) {
                throw new CommandRuntimeException(new TranslatableComponent(this.getUsage(sender)));
            }
            Player senderPlayer = (Player)sender.getEntity();
            MillWorldData worldData = Mill.getMillWorld(world);
            Building village = worldData.getClosestVillage(new Point((Entity)senderPlayer));
            if (village == null || village.getPos().distanceTo((Entity)senderPlayer) > 50.0) {
                throw new CommandRuntimeException(new TextComponent("No village within 50 blocks."));
            }
            int despawnedVillagers = 0;
            int respawnedVillagers = 0;
            for (VillagerRecord vr : village.getAllVillagerRecords()) {
                ArrayList<MillVillager> matchingVillagers = new ArrayList<MillVillager>();
                for (MillVillager villager : worldData.getAllKnownVillagers()) {
                    if (villager.getVillagerId() != vr.getVillagerId()) continue;
                    matchingVillagers.add(villager);
                }
                for (int i = matchingVillagers.size() - 1; i >= 0; --i) {
                    if (!((MillVillager)matchingVillagers.get((int)i)).isRemoved()) continue;
                    ((MillVillager)matchingVillagers.get(i)).despawnVillagerSilent();
                    ++despawnedVillagers;
                    matchingVillagers.remove(i);
                }
                if (matchingVillagers.size() != 0) continue;
                village.respawnVillager(vr, vr.getHouse().location.sleepingPos);
                ++respawnedVillagers;
            }
            ServerSender.sendChat(senderPlayer, ChatFormatting.DARK_GREEN, "Repeared the villager list of " + village.getVillageQualifiedName() + ". Despawned " + despawnedVillagers + " dead villager(s) and respawned " + respawnedVillagers + " villagers.");
        }
    }

    public List<String> getAliases() {
        return Collections.emptyList();
    }

    public String getName() {
        return "millDebugResetVillagers";
    }

    public List<String> suggest(MinecraftServer server, CommandSourceStack sender, String[] args, BlockPos targetPos) {
        return Collections.emptyList();
    }

    public String getUsage(CommandSourceStack sender) {
        return "commands." + this.getName().toLowerCase() + ".usage";
    }

    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }
}

