package org.millenaire.common.commands;

import java.util.List;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "millenaire", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MillCommandRegistration {
    private MillCommandRegistration() {}
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("millNavigationStats")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                var source=context.getSource();
                String report=org.millenaire.common.pathing.NavigationMetrics.report();
                source.sendSuccess(new net.minecraft.network.chat.TextComponent(report),false);
                org.millenaire.common.utilities.MillLog.major(null,"Navigation metrics: "+report);
                return 1;
            })
            .then(Commands.literal("reset").executes(context -> {
                org.millenaire.common.pathing.NavigationMetrics.reset();
                context.getSource().sendSuccess(new net.minecraft.network.chat.TextComponent("Navigation measurements reset"),false);
                return 1;
            })));
        List<MillCommand> commands = List.of(
            new CommandDebugResendProfiles(), new CommandDebugResetVillagers(),
            new CommandRenameVillage(), new CommandListActiveVillages(),
            new CommandTeleportToVillage(), new CommandGiveReputation(),
            new CommandSpawnVillage(false), new CommandSpawnVillage(true),
            new CommandImportCulture(), new CommandSwitchVillageControl());
        for (MillCommand command : commands) {
            var node = event.getDispatcher().register(Commands.literal(command.getName())
                .requires(source -> command.canUse(source.getServer(), source))
                .executes(context -> {
                    var source = context.getSource();
                    command.execute(source.getServer(), source, new String[0]);
                    return 1;
                })
                .then(Commands.argument("arguments", StringArgumentType.greedyString())
                    .suggests((context, builder) -> {
                        String remaining = builder.getRemaining();
                        String[] args = remaining.split("\\s+", -1);
                        String last = args[args.length - 1];
                        var output = builder.createOffset(builder.getStart() + remaining.length() - last.length());
                        var source = context.getSource();
                        for (String value : command.suggest(source.getServer(), source, args, new BlockPos(source.getPosition())))
                            output.suggest(value);
                        return output.buildFuture();
                    })
                    .executes(context -> {
                        var source = context.getSource();
                        String input = StringArgumentType.getString(context, "arguments");
                        command.execute(source.getServer(), source, input.trim().split("\\s+"));
                        return 1;
                    })));
            for (String alias : command.getAliases())
                event.getDispatcher().register(Commands.literal(alias)
                    .requires(source -> command.canUse(source.getServer(), source)).redirect(node));
        }
    }
}
