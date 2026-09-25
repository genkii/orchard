package de.minehackers.orchard.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import de.minehackers.orchard.Constants;
import de.minehackers.orchard.NbtTreePlacer;
import de.minehackers.orchard.OrchardCommon;
import de.minehackers.orchard.OrchardDefinition;
import de.minehackers.orchard.OrchardRegistry;
import de.minehackers.orchard.pack.Pack;

/// Grab-bag of /orchard subcommands: reload, clearcache, validate, find, what.
public final class UtilityCommands {

    private UtilityCommands() {}

    /// Runs the whole pack pipeline again: parse, validate, resolve, activate.
    static int runReload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();

        if (de.minehackers.orchard.pack.PackManager.bootError() != null) {
            StatusCommands.sendError(src,
                    "Startup failed earlier - fixing the underlying problem first is required.");
            StatusCommands.send(src, "Reason: " + de.minehackers.orchard.pack.PackManager.bootError());
            StatusCommands.send(src, "After fixing it, restart the server or try this command again.");
        }

        Pack pack;
        try {
            pack = de.minehackers.orchard.pack.PackManager.reload();
        } catch (Exception e) {
            StatusCommands.sendError(src, "Reload failed: " + e.getMessage());
            Constants.LOG.error("[Orchard] Reload failed: {}", e.getMessage(), e);
            return 0;
        }

        switch (pack.source()) {
            case PACKS -> {
                StatusCommands.send(src, "Reloaded pack '" + pack.metadata().name()
                        + "' with " + pack.definitions().size() + " definition(s).");
                if (de.minehackers.orchard.pack.PackManager.settings().isAutoSelect()) {
                    StatusCommands.send(src, "Tip: set 'pack.selected' in orchard.yaml to pin a pack.");
                }
                return pack.definitions().size();
            }
            case BUNDLED -> {
                StatusCommands.send(src, "No user pack found - using the built-in defaults ("
                        + pack.definitions().size() + " definition(s)).");
                return pack.definitions().size();
            }
            case NONE -> {
                StatusCommands.sendError(src,
                        "No valid pack found - vanilla world generation is active.");
                StatusCommands.send(src, "Check the log for details and look at: "
                        + OrchardCommon.getPackDirectory());
                return 0;
            }
            default -> {
                return 0;
            }
        }
    }

    static int runClearCache(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        NbtTreePlacer.clearCache();
        StatusCommands.send(src, "NBT template cache cleared.");
        Constants.LOG.info("[Orchard] Cache cleared via command.");
        return 1;
    }

    /// Validates definitions for missing, empty, unparseable NBT and bad dimensions.
    static int runValidate(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        List<OrchardDefinition> defs = OrchardRegistry.getAll();
        Path nbtDir = OrchardCommon.getNbtDirectory();

        StatusCommands.send(src, "==============================");
        StatusCommands.send(src, "      Orchard Validation");
        StatusCommands.send(src, "==============================");

        Pack pack = de.minehackers.orchard.pack.PackManager.activePack();
        switch (pack.source()) {
            case PACKS -> StatusCommands.send(src, "Pack: " + pack.metadata().name()
                    + " (" + pack.root().getFileName() + ")");
            case BUNDLED -> StatusCommands.send(src, "Source: built-in defaults (" + pack.root().getFileName() + "/)");
            case NONE -> StatusCommands.sendError(src, "No pack is active - nothing to validate.");
        }

        int errors = 0;

        if (defs.isEmpty()) {
            StatusCommands.sendError(src, "No definitions registered.");
            errors++;
        }

        for (OrchardDefinition def : defs) {
            Path filePath = nbtDir.resolve(def.getNbtFileName());
            if (!Files.exists(filePath)) {
                StatusCommands.sendError(src, "MISSING: " + def.getNbtFileName());
                errors++;
            } else {
                try {
                    if (Files.size(filePath) == 0) {
                        StatusCommands.sendError(src, "EMPTY: " + def.getNbtFileName());
                        errors++;
                    }
                } catch (Exception ignored) {}
            }
        }

        if (src.isPlayer()) {
            try {
                ServerPlayer player = src.getPlayerOrException();
                if (!(player.level() instanceof ServerLevel level)) {
                    StatusCommands.sendError(src, "Not in a server level.");
                } else {
                    for (OrchardDefinition def : defs) {
                        Path filePath = nbtDir.resolve(def.getNbtFileName());
                        if (Files.exists(filePath)) {
                            StructureTemplate template = TestCommands.loadTemplate(filePath, level);
                            if (template == null) {
                                StatusCommands.sendError(src, "PARSE FAILED: " + def.getNbtFileName());
                                errors++;
                            } else {
                                var size = template.getSize();
                                if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0) {
                                    StatusCommands.sendError(src, "INVALID SIZE: " + def.getNbtFileName()
                                        + " (" + size.getX() + "x" + size.getY() + "x" + size.getZ() + ")");
                                    errors++;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                StatusCommands.sendError(src, "Failed to get player: " + e.getMessage());
            }
        }

        if (errors == 0) {
            StatusCommands.send(src, "All " + defs.size() + " definition(s) passed validation.");
        } else {
            StatusCommands.sendError(src, errors + " issue(s) found.");
        }

        StatusCommands.send(src, "==============================");
        return errors;
    }

    /// Case-insensitive substring search over definitions and loose .nbt files.
    static int runFind(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        String query = StringArgumentType.getString(ctx, "name").toLowerCase(Locale.ROOT);

        List<OrchardDefinition> allDefs = OrchardRegistry.getAll();
        List<OrchardDefinition> matched = new ArrayList<>();
        for (OrchardDefinition def : allDefs) {
            if (def.getNbtFileName().toLowerCase(Locale.ROOT).contains(query)) {
                matched.add(def);
            }
        }

        Path nbtDir = OrchardCommon.getNbtDirectory();
        List<String> nbtFiles = new ArrayList<>();
        try {
            try (var stream = Files.list(nbtDir)) {
                stream.filter(p -> p.toString().toLowerCase(Locale.ROOT).contains(query))
                      .forEach(p -> nbtFiles.add(nbtDir.relativize(p).toString()));
            }
        } catch (Exception e) {
            // no nbt dir means nothing to search, that's fine
        }

        StatusCommands.send(src, "==============================");
        StatusCommands.send(src, "  Find: \"" + query + "\"");
        StatusCommands.send(src, "==============================");

        if (!matched.isEmpty()) {
            StatusCommands.send(src, "Matching definitions (" + matched.size() + "):");
            for (OrchardDefinition def : matched) {
                StatusCommands.send(src, "  " + def.getNbtFileName()
                    + "  w=" + def.getWeight()
                    + "  spacing=" + def.getMinSpacing());
            }
        }

        if (!nbtFiles.isEmpty()) {
            StatusCommands.send(src, "Matching NBT files (" + nbtFiles.size() + "):");
            for (String f : nbtFiles) {
                StatusCommands.send(src, "  " + f);
            }
        }

        if (matched.isEmpty() && nbtFiles.isEmpty()) {
            StatusCommands.sendError(src, "No matches found.");
        }

        StatusCommands.send(src, "==============================");
        return matched.size() + nbtFiles.size();
    }

    /// Prints the player biome and matching definitions.
    static int runWhat(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (!src.isPlayer()) {
            StatusCommands.sendError(src, "This command must be run by a player.");
            return 0;
        }

        ServerPlayer player;
        try {
            player = src.getPlayerOrException();
        } catch (Exception e) {
            StatusCommands.sendError(src, "Failed to get player.");
            return 0;
        }

        var biomeKey = player.level().getBiome(player.blockPosition()).unwrapKey();
        String biomeName = biomeKey.map(k -> k.identifier().toString()).orElse("unknown");

        StatusCommands.send(src, "==============================");
        StatusCommands.send(src, "  Biome: " + biomeName);
        StatusCommands.send(src, "  Pos:   " + player.blockPosition());
        StatusCommands.send(src, "==============================");

        List<OrchardDefinition> allDefs = OrchardRegistry.getAll();
        if (allDefs.isEmpty()) {
            StatusCommands.sendError(src, "No definitions registered.");
            return 0;
        }

        List<OrchardDefinition> matching = new ArrayList<>();
        var biomeHolder = player.level().getBiome(player.blockPosition());
        for (OrchardDefinition def : allDefs) {
            if (def.matchesBiome(biomeHolder)) {
                matching.add(def);
            }
        }

        if (matching.isEmpty()) {
            StatusCommands.send(src, "No definitions match this biome.");
        } else {
            StatusCommands.send(src, matching.size() + " definition(s) match this biome:");
            for (OrchardDefinition def : matching) {
                String rare = def.isRare() ? " [rare]" : "";
                StatusCommands.send(src, "  " + def.getNbtFileName()
                    + "  (w=" + def.getWeight()
                    + ", spacing=" + def.getMinSpacing()
                    + rare + ")");
            }
        }

        StatusCommands.send(src, "==============================");
        return matching.size();
    }
}
