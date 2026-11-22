/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
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
import org.millenaire.common.forge.Mill;
import org.millenaire.common.network.ServerSender;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.world.MillWorldData;
import org.millenaire.common.world.UserProfile;

public class CommandDebugResendProfiles
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
            if (args.length != 1) {
                throw new CommandRuntimeException(new TranslatableComponent(this.getUsage(sender)));
            }
            if (args[0].equals("all")) {
                MillWorldData worldData = Mill.getMillWorld(world);
                for (UserProfile profile : worldData.profiles.values()) {
                    if (!profile.connected) continue;
                    profile.sendInitialPackets();
                    if (sender.getEntity() instanceof Player) {
                        ServerSender.sendTranslatedSentence((Player)sender.getEntity(), '2', "Resent profile data for " + profile.playerName, new String[0]);
                        continue;
                    }
                    MillLog.major(profile, "Resent profile data.");
                }
            } else {
                Player player = server.getPlayerList().getPlayerByName(args[0]);
                if (player == null) {
                    throw new CommandRuntimeException(new net.minecraft.network.chat.TranslatableComponent("millenaire.command.profile_argument"));
                }
                MillWorldData worldData = Mill.getMillWorld(world);
                UserProfile profile = worldData.getProfile(player);
                profile.sendInitialPackets();
                if (sender.getEntity() instanceof Player) {
                    ServerSender.sendTranslatedSentence((Player)sender.getEntity(), '2', "Resent profile data for " + profile.playerName, new String[0]);
                } else {
                    MillLog.major(profile, "Resent profile data.");
                }
            }
        }
    }

    public List<String> getAliases() {
        return Collections.emptyList();
    }

    public String getName() {
        return "millDebugSendProfiles";
    }

    public List<String> suggest(MinecraftServer server, CommandSourceStack sender, String[] args, BlockPos targetPos) {
        return Collections.emptyList();
    }

    public String getUsage(CommandSourceStack sender) {
        return "commands." + this.getName().toLowerCase() + ".usage";
    }

    public boolean isUsernameIndex(String[] args, int index) {
        return index == 1;
    }
}

