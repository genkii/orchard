package de.minehackers.orchard.command;

import com.mojang.brigadier.context.CommandContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import de.minehackers.orchard.Constants;
import de.minehackers.orchard.NbtTreePlacer;
import de.minehackers.orchard.OrchardCommon;
import de.minehackers.orchard.OrchardDefinition;
import de.minehackers.orchard.OrchardRegistry;
import de.minehackers.orchard.pack.Pack;
import de.minehackers.orchard.pack.PackManager;

/// The /orchard status and /orchard list subcommands.
public final class StatusCommands {

    private StatusCommands() {}

    /// Dumps active pack, definitions, settings, and NBT file existence.
    static int runStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        List<OrchardDefinition> defs = OrchardRegistry.getAll();
        Path nbtDir = OrchardCommon.getNbtDirectory();

        send(src, "==============================");
        send(src, "      Orchard Status");
        send(src, "==============================");

        String bootError = PackManager.bootError();
        if (bootError != null) {
            sendError(src, "Startup FAILED: " + bootError);
            sendError(src, "Orchard is not intercepting world generation - check the log.");
        }

        Pack pack = PackManager.activePack();
        switch (pack.source()) {
            case PACKS -> send(src, "Active pack: " + pack.metadata().name()
                    + " (" + pack.root().getFileName() + ")");
            case BUNDLED -> send(src, "Active source: built-in defaults (" + pack.root().getFileName() + "/)");
            case NONE -> sendError(src, "No pack active - vanilla world generation is untouched.");
        }
        if (!pack.metadata().version().isBlank()) {
            send(src, "Pack version: " + pack.metadata().version());
        }
        if (!pack.metadata().description().isBlank()) {
            send(src, "Description:  " + pack.metadata().description());
        }
        send(src, "");

        send(src, "Definitions registered: " + defs.size());
        send(src, "");

        if (defs.isEmpty()) {
            if (pack.source() == de.minehackers.orchard.pack.PackSource.NONE) {
                send(src, "Create a pack in: " + OrchardCommon.getPackDirectory());
            }
        } else {
            for (OrchardDefinition def : defs) {
                Path filePath = nbtDir.resolve(def.getNbtFileName());
                String check = Files.exists(filePath) ? "OK" : "MISSING";
                String tag = def.isRare() ? " [rare]" : "";
                String dimTag = def.getDimensions().isEmpty()
                        ? "" : " dims=" + def.getDimensions().size();
                String yTag = def.hasYRange()
                        ? " y=[" + def.getMinY() + "," + def.getMaxY() + "]" : "";
                String info = "  " + def.getNbtFileName()
                    + "  (w=" + def.getWeight()
                    + ", spacing=" + def.getMinSpacing()
                    + ", offset=" + def.getOriginYOffset()
                    + tag + dimTag + yTag + ")  " + check;
                if ("MISSING".equals(check)) {
                    sendError(src, info);
                } else {
                    send(src, info);
                }
            }
        }

        send(src, "");
        send(src, "NBT directory: " + nbtDir);
        send(src, "Template cache: " + NbtTreePlacer.getCacheStats());
        send(src, "Feature index: " + de.minehackers.orchard.matchers.FeatureIndex.size() + " signature(s)");
        send(src, "Rare pool probability: " + String.format("%.1f%%", OrchardRegistry.getRarePoolProbability() * 100));
        send(src, "Max obstructed fraction: " + String.format("%.0f%%", NbtTreePlacer.getMaxObstructedFraction() * 100));

        if (src.isPlayer()) {
            try {
                ServerPlayer player = src.getPlayerOrException();
                Optional<ResourceKey<Biome>> biomeKey = player.level().getBiome(player.blockPosition()).unwrapKey();
                String biomeName = biomeKey.map(k -> k.identifier().toString()).orElse("unknown");
                send(src, "Player biome: " + biomeName);
                send(src, "Player pos: " + player.blockPosition());
            } catch (Exception e) {
                sendError(src, "Failed to get player info.");
            }
        }

        send(src, "==============================");
        return defs.size();
    }

    /// One line per definition, with weight and spacing.
    static int runList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        List<OrchardDefinition> defs = OrchardRegistry.getAll();

        send(src, "==============================");
        send(src, "      Orchard Definitions (" + defs.size() + ")");
        send(src, "==============================");

        if (defs.isEmpty()) {
            sendError(src, "No definitions registered.");
        } else {
            for (int i = 0; i < defs.size(); i++) {
                OrchardDefinition def = defs.get(i);
                String rare = def.isRare() ? " [rare]" : "";
                send(src, (i + 1) + ". " + def.getNbtFileName()
                    + "  w=" + def.getWeight()
                    + "  spacing=" + def.getMinSpacing()
                    + "  offset=" + def.getOriginYOffset()
                    + rare);
            }
        }

        send(src, "==============================");
        return defs.size();
    }

    static void send(CommandSourceStack src, String text) {
        src.sendSuccess(() -> Component.literal(text), false);
    }

    static void sendError(CommandSourceStack src, String text) {
        src.sendFailure(Component.literal(text));
    }
}
