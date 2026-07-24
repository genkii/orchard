package de.minehackers.orchard.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Registers the /orchard command tree with all subcommands.
 */
public final class CommandRegistry {

    static final int PERMISSION_LEVEL = 2;

    private CommandRegistry() {}

    /**
     * Registers the command event listener with the NeoForge event bus.
     */
    public static void register() {
        NeoForge.EVENT_BUS.register(new CommandRegistry());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("orchard")
                .requires(src -> src.hasPermission(PERMISSION_LEVEL))
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
