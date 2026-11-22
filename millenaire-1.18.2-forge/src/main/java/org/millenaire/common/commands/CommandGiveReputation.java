/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.command.CommandBase
 *  net.minecraft.command.CommandException
 *  net.minecraft.command.MillCommand
 *  net.minecraft.command.CommandSourceStack
 *  net.minecraft.command.WrongUsageException
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.util.math.BlockPos
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
import net.minecraft.world.level.Level;
import org.millenaire.common.commands.CommandUtilities;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.network.ServerSender;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.village.Building;
import org.millenaire.common.world.MillWorldData;

public class CommandGiveReputation
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
            if (args.length != 3) {
                throw new CommandRuntimeException(new TranslatableComponent(this.getUsage(sender)));
            }
            Entity entity = MillCommandSupport.getPlayer((MinecraftServer)server, (CommandSourceStack)sender, (String)args[0]);
            MillWorldData worldData = Mill.getMillWorld(world);
            List<Building> townHalls = CommandUtilities.getMatchingVillages(worldData, args[1]);
            if (townHalls.size() == 0) {
                throw new CommandRuntimeException(new TextComponent(LanguageUtilities.string("command.tp_nomatchingvillage")));
            }
            if (townHalls.size() > 1) {
                throw new CommandRuntimeException(new TextComponent(LanguageUtilities.string("command.tp_multiplematchingvillages", "" + townHalls.size())));
            }
            Building village = townHalls.get(0);
            if (entity instanceof Player) {
                int repToGive = MillCommandSupport.parseInt((String)args[2]);
                village.adjustReputation((Player)entity, repToGive);
                ServerSender.sendTranslatedSentence((Player)entity, '9', "command.giverep_notification", sender.getTextName(), entity.getName().getString(), "" + repToGive, village.getVillageQualifiedName());
                if (sender.getEntity() instanceof Player) {
                    ServerSender.sendTranslatedSentence((Player)sender.getEntity(), '9', "command.giverep_notification", sender.getTextName(), entity.getName().getString(), "" + repToGive, village.getVillageQualifiedName());
                }
            }
        }
    }

    public List<String> getAliases() {
        return Collections.emptyList();
    }

    public String getName() {
        return "millGiveRep";
    }

    public int getRequiredPermissionLevel() {
        return 3;
    }

    public List<String> suggest(MinecraftServer server, CommandSourceStack sender, String[] args, BlockPos targetPos) {
        if (args.length == 1) {
            return MillCommandSupport.matching((String[])args, (String[])server.getPlayerNames());
        }
        if (args.length == 2) {
            Level world = sender.getLevel();
            MillWorldData worldData = Mill.getMillWorld(world);
            List<Building> townHalls = CommandUtilities.getMatchingVillages(worldData, args[1]);
            ArrayList<String> possibleMatches = new ArrayList<String>();
            for (Building th : townHalls) {
                possibleMatches.add(CommandUtilities.normalizeString(th.getVillageQualifiedName()));
            }
            return possibleMatches;
        }
        return Collections.emptyList();
    }

    public String getUsage(CommandSourceStack sender) {
        return "commands." + this.getName().toLowerCase() + ".usage";
    }

    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }
}

