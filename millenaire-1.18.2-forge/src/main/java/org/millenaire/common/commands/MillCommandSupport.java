package org.millenaire.common.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class MillCommandSupport {
    private MillCommandSupport() {}
    public static int parseInt(String value) {
        try {
            StringReader reader = new StringReader(value);
            int result = reader.readInt();
            if (reader.canRead()) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidInt().create(value);
            return result;
        } catch (CommandSyntaxException e) { throw new CommandRuntimeException(net.minecraft.network.chat.ComponentUtils.fromMessage(e.getRawMessage())); }
    }
    public static ServerPlayer getPlayer(MinecraftServer server, CommandSourceStack source, String value) {
        try {
            return new EntitySelectorParser(new StringReader(value)).parse().findSinglePlayer(source);
        } catch (CommandSyntaxException e) { throw new CommandRuntimeException(net.minecraft.network.chat.ComponentUtils.fromMessage(e.getRawMessage())); }
    }
    public static List<String> matching(String[] args, String[] options) {
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return Arrays.stream(options).filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
