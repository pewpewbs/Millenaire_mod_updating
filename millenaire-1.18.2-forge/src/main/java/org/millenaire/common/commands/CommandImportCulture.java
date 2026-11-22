/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.command.CommandBase
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.millenaire.common.buildingplan.BuildingImportExport;
import org.millenaire.common.buildingplan.BuildingPlanSet;
import org.millenaire.common.commands.CommandUtilities;
import org.millenaire.common.culture.Culture;
import org.millenaire.common.network.ServerSender;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;

public class CommandImportCulture
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
            int z;
            int x;
            Player player = null;
            if (sender.getEntity() instanceof Player) {
                player = (Player)sender.getEntity();
            }
            if (args.length != 1 && args.length != 3) {
                throw new CommandRuntimeException(new TranslatableComponent(this.getUsage(sender)));
            }
            String cultureParam = args[0];
            Culture culture = Culture.getCultureByName(cultureParam);
            if (culture == null) {
                throw new CommandRuntimeException(new TextComponent(LanguageUtilities.string("command.spawnvillage_unknownculture", cultureParam)));
            }
            if (args.length > 1) {
                x = MillCommandSupport.parseInt((String)args[1]);
                z = MillCommandSupport.parseInt((String)args[2]);
            } else {
                x = (int)player.getX();
                z = (int)player.getZ();
            }
            int y = WorldUtilities.findTopSoilBlock(world, x, z);
            Point startPoint = new Point(x, y, z);
            Point adjustedStartPoint = new Point(x, y, z);
            List<BuildingPlanSet> planSets = new ArrayList<BuildingPlanSet>(culture.ListPlanSets);
            planSets = planSets.stream().sorted((p1, p2) -> p1.mainFile.compareTo(p2.mainFile)).collect(Collectors.toList());
            for (BuildingPlanSet planSet : planSets) {
                if (planSet.getFirstStartingPlan().isSubBuilding) continue;
                ServerSender.sendTranslatedSentence(player, 'f', "command.importculture_importingbuilding", planSet.getNameTranslated());
                int xDelta = BuildingImportExport.importTableHandleImportRequest(player, adjustedStartPoint, culture.key, planSet.key, true, 0, 0, 0, true);
                adjustedStartPoint = new Point(adjustedStartPoint.x + (double)xDelta + 5.0, startPoint.y, startPoint.z);
            }
        }
    }

    public List<String> getAliases() {
        return Collections.emptyList();
    }

    public String getName() {
        return "millImportCulture";
    }

    public int getRequiredPermissionLevel() {
        return 3;
    }

    public List<String> suggest(MinecraftServer server, CommandSourceStack sender, String[] args, BlockPos targetPos) {
        if (args.length == 1) {
            ArrayList<String> possibleMatches = new ArrayList<String>();
            String partialKey = CommandUtilities.normalizeString(args[0]);
            for (Culture c : Culture.ListCultures) {
                if (!CommandUtilities.normalizeString(c.key).startsWith(partialKey)) continue;
                possibleMatches.add(CommandUtilities.normalizeString(c.key));
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

