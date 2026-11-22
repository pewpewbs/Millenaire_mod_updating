package org.millenaire.common.commands;

import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

public interface MillCommand extends Comparable<MillCommand> {
    String getName();
    String getUsage(CommandSourceStack source);
    List<String> getAliases();
    boolean canUse(MinecraftServer server, CommandSourceStack source);
    void execute(MinecraftServer server, CommandSourceStack source, String[] args);
    List<String> suggest(MinecraftServer server, CommandSourceStack source, String[] args, BlockPos target);
    boolean isUsernameIndex(String[] args, int index);
}
