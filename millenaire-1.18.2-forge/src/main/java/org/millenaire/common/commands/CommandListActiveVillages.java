/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.command.CommandException
 *  net.minecraft.command.MillCommand
 *  net.minecraft.command.CommandSourceStack
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.World
 */
package org.millenaire.common.commands;

import java.text.NumberFormat;
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
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.network.ServerSender;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.WorldUtilities;
import org.millenaire.common.village.Building;
import org.millenaire.common.world.MillWorldData;

public class CommandListActiveVillages
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
            MillWorldData worldData = Mill.getMillWorld(world);
            for (int i = 0; i < worldData.villagesList.pos.size(); ++i) {
                Building townHall = worldData.getBuilding(worldData.villagesList.pos.get(i));
                if (townHall == null || !townHall.isActive) continue;
                long villagersTime = 0L;
                long buildingsTime = 0L;
                for (Long sample : worldData.villagesList.villagersTime.get(i)) {
                    villagersTime += sample.longValue();
                }
                for (Long sample : worldData.villagesList.buildingsTime.get(i)) {
                    buildingsTime += sample.longValue();
                }
                NumberFormat nf = NumberFormat.getInstance();
                String buildingsTimeStr = nf.format(buildingsTime / 1000L);
                String villagersTimeStr = nf.format(villagersTime / 1000L);
                List<Entity> entities = WorldUtilities.getEntitiesWithinAABB(world, Player.class, townHall.getPos(), MillConfigValues.KeepActiveRadius, 1000);
                String players = "";
                for (Entity playerEntity : entities) {
                    Player player = (Player)playerEntity;
                    if (players.length() > 0) {
                        players = players + ", ";
                    }
                    players = players + player.getName().getString();
                }
                if (!(sender.getEntity() instanceof Player)) {
                    MillLog.major(this, "Village " + townHall.getVillageQualifiedName() + " is active. It knows " + townHall.getKnownVillagers().size() + " villagers (" + townHall.getAllVillagerRecords().size() + " in the archives). Within the last 20 ticks, it took " + buildingsTimeStr + " ns to handle buildings and " + villagersTimeStr + " ns to handle villagers. Kept alive by: " + players);
                    continue;
                }
                Player senderPlayer = (Player)sender.getEntity();
                ServerSender.sendTranslatedSentence(senderPlayer, 'f', "command.listactivevillages_list", townHall.getVillageQualifiedName(), "" + townHall.getKnownVillagers().size(), "" + townHall.getAllVillagerRecords().size(), buildingsTimeStr, villagersTimeStr, players);
            }
        }
    }

    public List<String> getAliases() {
        return Collections.emptyList();
    }

    public String getName() {
        return "millListActiveVillages";
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

