package de.minehackers.orchard.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/// Builds the /orchard command tree. Each loader forwards its dispatcher here.
public final class CommandRegistry {

    private CommandRegistry() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("orchard")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("status").executes(StatusCommands::runStatus))
                .then(
                    Commands.literal("test").then(
                        Commands.argument("name", StringArgumentType.word())
                            .executes(TestCommands::runTest)
                    )
                )
                .then(Commands.literal("list").executes(StatusCommands::runList))
                .then(Commands.literal("reload").executes(UtilityCommands::runReload))
                .then(Commands.literal("clearcache").executes(UtilityCommands::runClearCache))
                .then(Commands.literal("what").executes(UtilityCommands::runWhat))
                .then(
                    Commands.literal("find").then(
                        Commands.argument("name", StringArgumentType.greedyString())
                            .executes(UtilityCommands::runFind)
                    )
                )
                .then(Commands.literal("validate").executes(UtilityCommands::runValidate))
                .then(
                    Commands.literal("nbt").then(
                        Commands.literal("info").then(
                            Commands.argument("name", StringArgumentType.word())
                                .executes(NbtCommands::runNbtInfo)
                        )
                    )
                )
                .then(
                    Commands.literal("place").then(
                        Commands.argument("name", StringArgumentType.word())
                            .executes(TestCommands::runPlace)
                    )
                )
        );
    }
}
