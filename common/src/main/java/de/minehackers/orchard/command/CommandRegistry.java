package de.minehackers.orchard.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import de.minehackers.orchard.OrchardCommon;
import de.minehackers.orchard.OrchardDefinition;
import de.minehackers.orchard.OrchardRegistry;

/// Builds the /orchard command tree. Each loader forwards its dispatcher here.
public final class CommandRegistry {

    private CommandRegistry() {}

    /// Suggests NBT file names from the registry and the nbt directory.
    private static final SuggestionProvider<CommandSourceStack> NBT_FILE_SUGGESTIONS =
        (ctx, builder) -> {
            var defs = OrchardRegistry.getAll();
            var names = defs.stream()
                .map(OrchardDefinition::getNbtFileName)
                .collect(Collectors.toSet());
            var nbtDir = OrchardCommon.getNbtDirectory();
            if (nbtDir != null) {
                try (var stream = java.nio.file.Files.list(nbtDir)) {
                    stream.filter(p -> p.toString().endsWith(".nbt"))
                        .forEach(p -> names.add(p.getFileName().toString()));
                } catch (Exception ignored) {}
            }
            return SharedSuggestionProvider.suggest(names, builder);
        };

    /// Suggests valid rotation names.
    private static final SuggestionProvider<CommandSourceStack> ROTATION_SUGGESTIONS =
        (ctx, builder) -> SharedSuggestionProvider.suggest(
            java.util.List.of("none", "clockwise_90", "clockwise_180", "counterclockwise_90"), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("orchard")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("status").executes(StatusCommands::runStatus))
                .then(
                    Commands.literal("test").then(
                        Commands.argument("name", StringArgumentType.word())
                            .suggests(NBT_FILE_SUGGESTIONS)
                            .then(
                                Commands.argument("rotation", StringArgumentType.word())
                                    .suggests(ROTATION_SUGGESTIONS)
                                    .executes(TestCommands::runTest)
                            )
                            .executes(TestCommands::runTest)
                    )
                )
                .then(Commands.literal("stats").executes(StatsCommands::runStats))
                .then(
                    Commands.literal("create").then(
                        Commands.argument("name", StringArgumentType.word())
                            .then(
                                Commands.argument("pos1", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                                    .then(
                                        Commands.argument("pos2", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                                            .executes(CreateCommands::runCreate)
                                    )
                            )
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
                                .suggests(NBT_FILE_SUGGESTIONS)
                                .executes(NbtCommands::runNbtInfo)
                        )
                    )
                )
        );
    }
}
