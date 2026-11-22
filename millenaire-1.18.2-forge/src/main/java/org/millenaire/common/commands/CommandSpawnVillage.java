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
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.millenaire.common.commands.CommandUtilities;
import org.millenaire.common.culture.Culture;
import org.millenaire.common.culture.VillageType;
import org.millenaire.common.utilities.LanguageUtilities;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.world.WorldGenVillage;

public class CommandSpawnVillage
implements MillCommand {
    private final boolean spawnLoneBuilding;

    public CommandSpawnVillage(boolean spawnLoneBuildings) {
        this.spawnLoneBuilding = spawnLoneBuildings;
    }

    public boolean canUse(MinecraftServer server, CommandSourceStack sender) {
        return sender.hasPermission(this.getRequiredPermissionLevel());
    }

    public int compareTo(MillCommand o) {
        return this.getName().compareTo(o.getName());
    }

    public void execute(MinecraftServer server, CommandSourceStack sender, String[] args) throws CommandRuntimeException {
        Level world = sender.getLevel();
        if (!world.isClientSide) {
            if (args.length < 2 || args.length > 5) {
                throw new CommandRuntimeException(new TranslatableComponent(this.getUsage(sender)));
            }
            String cultureParam = args[0];
            String villageTypeParam = args[1];
            Culture culture = Culture.getCultureByName(cultureParam);
            if (culture == null) {
                throw new CommandRuntimeException(new TextComponent(LanguageUtilities.string("command.spawnvillage_unknownculture", cultureParam)));
            }
            VillageType villageType = this.spawnLoneBuilding ? culture.getLoneBuildingType(villageTypeParam) : culture.getVillageType(villageTypeParam);
            if (villageType == null) {
                throw new CommandRuntimeException(new TextComponent(LanguageUtilities.string("command.spawnvillage_unknownvillage", villageTypeParam)));
            }
            Player player = null;
            if (sender.getEntity() instanceof Player) {
                player = (Player)sender.getEntity();
            }
            int x = 0;
            int z = 0;
            float completion = 0.0f;
            if (args.length > 3) {
                x = MillCommandSupport.parseInt((String)args[2]);
                z = MillCommandSupport.parseInt((String)args[3]);
                if (args.length > 4) {
                    completion = (float)MillCommandSupport.parseInt((String)args[4]) / 100.0f;
                }
            } else if (player != null) {
                x = (int)player.getX();
                z = (int)player.getZ();
                if (args.length > 2) {
                    completion = (float)MillCommandSupport.parseInt((String)args[2]) / 100.0f;
                }
            }
            MillLog.major(null, "Attempting to spawn village of type " + cultureParam + ":" + villageTypeParam + " at " + x + "/" + z + ".");
            WorldGenVillage genVillage = new WorldGenVillage();
            boolean result = genVillage.generateVillageAtPoint(world, MillCommonUtilities.random, x, 0, z, player, false, true, false, 0, villageType, null, null, completion);
            MillLog.major(null, "Result of spawn attempt: " + result);
        }
    }

    public List<String> getAliases() {
        return Collections.emptyList();
    }

    public String getName() {
        if (this.spawnLoneBuilding) {
            return "millSpawnLoneBuilding";
        }
        return "millSpawnVillage";
    }

    public int getRequiredPermissionLevel() {
        return 3;
    }

    public List<String> suggest(MinecraftServer server, CommandSourceStack sender, String[] args, BlockPos targetPos) {
        Culture culture;
        if (args.length == 1) {
            ArrayList<String> possibleMatches = new ArrayList<String>();
            String partialKey = CommandUtilities.normalizeString(args[0]);
            for (Culture c : Culture.ListCultures) {
                if (!CommandUtilities.normalizeString(c.key).startsWith(partialKey)) continue;
                possibleMatches.add(CommandUtilities.normalizeString(c.key));
            }
            return possibleMatches;
        }
        if (args.length == 2 && (culture = Culture.getCultureByName(args[0])) != null) {
            ArrayList<String> possibleMatches = new ArrayList<String>();
            String partialKey = CommandUtilities.normalizeString(args[1]);
            List<VillageType> types = this.spawnLoneBuilding ? culture.listLoneBuildingTypes : culture.listVillageTypes;
            for (VillageType vtype : types) {
                if (!CommandUtilities.normalizeString(vtype.key).startsWith(partialKey)) continue;
                possibleMatches.add(CommandUtilities.normalizeString(vtype.key));
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

