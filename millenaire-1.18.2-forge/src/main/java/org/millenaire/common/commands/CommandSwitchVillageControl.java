/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  net.minecraft.command.CommandException
 *  net.minecraft.command.MillCommand
 *  net.minecraft.command.CommandSourceStack
 *  net.minecraft.command.WrongUsageException
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.World
 */
package org.millenaire.common.commands;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.millenaire.common.commands.CommandUtilities;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.network.ServerSender;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.village.Building;
import org.millenaire.common.world.MillWorldData;

public class CommandSwitchVillageControl
implements MillCommand {
    public boolean canUse(MinecraftServer server, CommandSourceStack sender) {
        return sender.hasPermission(this.getRequiredPermissionLevel());
    }

    public int compareTo(MillCommand o) {
        return this.getName().compareTo(o.getName());
    }

    public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) throws CommandRuntimeException {
        Level world = sender.getLevel();
        if (!world.isClientSide) {
            if (args.length != 2) {
                throw new CommandRuntimeException(new TranslatableComponent(this.getUsage(sender)));
            }
            MillWorldData worldData = Mill.getMillWorld(world);
            List<Building> townHalls = CommandUtilities.getMatchingVillages(worldData, args[0]);
            if (townHalls.size() == 0) {
                throw new CommandRuntimeException(new TextComponent(LanguageUtilities.string("command.tp_nomatchingvillage")));
            }
            if (townHalls.size() > 1) {
                throw new CommandRuntimeException(new TextComponent(LanguageUtilities.string("command.tp_multiplematchingvillages", "" + townHalls.size())));
            }
            Building village = townHalls.get(0);
            if (!village.villageType.playerControlled) {
                throw new CommandRuntimeException(new TextComponent(LanguageUtilities.string("command.switchcontrol_notcontrolled", village.getVillageQualifiedName())));
            }
            String playerName = args[1];
            GameProfile profile = server.getProfileCache().get(playerName).orElse(null);
            if (profile == null) {
                throw new CommandRuntimeException(new TextComponent(LanguageUtilities.string("command.switchcontrol_playernotfound", playerName)));
            }
            String oldControllerName = village.controlledByName;
            village.controlledBy = profile.getId();
            village.controlledByName = profile.getName();
            MillLog.major(this, "Switched controller from " + oldControllerName + " to " + village.controlledByName + " via command by " + sender.getTextName() + ".");
            for (Player player : world.players()) {
                ServerSender.sendTranslatedSentence(player, '9', "command.switchcontrol_notification", sender.getTextName(), oldControllerName, profile.getName(), village.getVillageQualifiedName());
            }
        }
    }

    public List<String> getAliases() {
        return Collections.emptyList();
    }

    public String getName() {
        return "millSwitchVillageControl";
    }

    public int getRequiredPermissionLevel() {
        return 3;
    }

    public List<String> suggest(MinecraftServer server, CommandSourceStack sender, String[] args, BlockPos targetPos) {
        if (args.length == 1) {
            Level world = sender.getLevel();
            MillWorldData worldData = Mill.getMillWorld(world);
            List<Building> townHalls = CommandUtilities.getMatchingVillages(worldData, args[0]);
            ArrayList<String> possibleMatches = new ArrayList<String>();
            for (Building th : townHalls) {
                if (!th.villageType.playerControlled) continue;
                possibleMatches.add(CommandUtilities.normalizeString(th.getVillageQualifiedName()));
            }
            return possibleMatches;
        }
        if (args.length == 2) {
            Level world = sender.getLevel();
            ArrayList<String> possibleMatches = new ArrayList<String>();
            String normalizedQuery = CommandUtilities.normalizeString(args[1]);
            for (String userName : server.getPlayerNames()) {
                String normalizedName = CommandUtilities.normalizeString(userName);
                if (!normalizedName.startsWith(normalizedQuery)) continue;
                possibleMatches.add(normalizedName);
            }
            return possibleMatches;
        }
        return Collections.emptyList();
    }

    public String getUsage(CommandSourceStack sender) {
        return "commands." + this.getName().toLowerCase() + ".usage";
    }

    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }
}

