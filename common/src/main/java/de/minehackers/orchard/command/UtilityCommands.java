package de.minehackers.orchard.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import de.minehackers.orchard.Constants;
import de.minehackers.orchard.NbtTreePlacer;
import de.minehackers.orchard.OrchardCommon;
import de.minehackers.orchard.OrchardDefinition;
import de.minehackers.orchard.OrchardRegistry;
import de.minehackers.orchard.config.ConfigLoader;

/// Utility commands: reload, clearcache, validate, find, what.
public final class UtilityCommands {

    private UtilityCommands() {}

    /// Reloads all definitions from disk with validation, then applies if valid.
    static int runReload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        Path configDir = OrchardCommon.getConfigDirectory();
        Path nbtDir = OrchardCommon.getNbtDirectory();

        List<OrchardDefinition> defs;
        try {
            defs = ConfigLoader.loadAll(configDir);
        } catch (Exception e) {
            StatusCommands.sendError(src, "Failed to load configs: " + e.getMessage());
            Constants.LOG.error("[Orchard] Reload load failed: {}", e.getMessage());
            return 0;
        }

        StatusCommands.send(src, "Validating " + defs.size() + " definition(s)...");

        int warnings = 0;
        int errors = 0;

        for (OrchardDefinition def : defs) {
            Path filePath = nbtDir.resolve(def.getNbtFileName());
            if (!Files.exists(filePath)) {
                StatusCommands.sendError(src, "  MISSING: " + def.getNbtFileName());
                errors++;
            } else {
                try {
                    long size = Files.size(filePath);
                    if (size == 0) {
                        StatusCommands.sendError(src, "  EMPTY: " + def.getNbtFileName());
                        errors++;
                    } else if (size > Constants.MAX_NBT_FILE_SIZE) {
                        StatusCommands.sendError(src, "  TOO LARGE: " + def.getNbtFileName()
                            + " (" + size + " bytes)");
                        errors++;
                    }
                } catch (Exception e) {
                    StatusCommands.sendError(src, "  UNREADABLE: " + def.getNbtFileName());
                    errors++;
                }
            }
        }

        if (errors > 0) {
            StatusCommands.sendError(src, errors + " error(s) found. Reload aborted to prevent issues.");
            StatusCommands.send(src, "Fix the issues above and try again.");
            Constants.LOG.warn("[Orchard] Reload aborted: {} error(s) in definitions.", errors);
            return 0;
        }

        OrchardRegistry.clearAndRegisterAll(defs);
        NbtTreePlacer.clearCache();

        StatusCommands.send(src, "Reloaded " + defs.size() + " definition(s) successfully."
            + (warnings > 0 ? " (" + warnings + " warning(s))" : ""));
        Constants.LOG.info("[Orchard] Reloaded {} definition(s) via command.", defs.size());
        return defs.size();
    }

    static int runClearCache(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        NbtTreePlacer.clearCache();
        StatusCommands.send(src, "NBT template cache cleared.");
        Constants.LOG.info("[Orchard] Cache cleared via command.");
        return 1;
    }

    /// Checks all definitions for missing files, empty files, and parse errors.
    static int runValidate(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        List<OrchardDefinition> defs = OrchardRegistry.getAll();
        Path nbtDir = OrchardCommon.getNbtDirectory();

        StatusCommands.send(src, "==============================");
        StatusCommands.send(src, "      Orchard Validation");
        StatusCommands.send(src, "==============================");

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
                                Vec3i size = template.getSize();
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

    /// Searches definitions and NBT files matching a query string.
    static int runFind(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        String query = StringArgumentType.getString(ctx, "name").toLowerCase();

        List<OrchardDefinition> allDefs = OrchardRegistry.getAll();
        List<OrchardDefinition> matched = new ArrayList<>();
        for (OrchardDefinition def : allDefs) {
            if (def.getNbtFileName().toLowerCase().contains(query)) {
                matched.add(def);
            }
        }

        Path nbtDir = OrchardCommon.getNbtDirectory();
        List<String> nbtFiles = new ArrayList<>();
        try {
            try (var stream = Files.list(nbtDir)) {
                stream.filter(p -> p.toString().toLowerCase().contains(query))
                      .forEach(p -> nbtFiles.add(nbtDir.relativize(p).toString()));
            }
        } catch (Exception e) {
            // directory might not exist
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

    /// Shows current biome and which definitions match it.
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
