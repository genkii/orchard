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
import de.minehackers.orchard.Orchard;
import de.minehackers.orchard.OrchardDefinition;
import de.minehackers.orchard.OrchardRegistry;

/**
 * Status and list commands for inspecting registered orchard definitions.
 */
public final class StatusCommands {

    private StatusCommands() {}

    /**
     * Displays the current orchard status including all registered definitions and their NBT file states.
     *
     * @param ctx the command context
     * @return the number of registered definitions
     */
    static int runStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        List<OrchardDefinition> defs = OrchardRegistry.getAll();
        Path nbtDir = Orchard.getConfigDirectory().resolve("orchard").resolve("nbt");

        send(src, "==============================");
        send(src, "      Orchard Status");
        send(src, "==============================");
        send(src, "Definitions registered: " + defs.size());
        send(src, "");

        if (defs.isEmpty()) {
            sendError(src, "No definitions found.");
            send(src, "Place .json files in: config/orchard/data/");
        } else {
            for (OrchardDefinition def : defs) {
                Path filePath = nbtDir.resolve(def.getNbtFileName());
                String check = Files.exists(filePath) ? "OK" : "MISSING";
                String tag = def.isRare() ? " [rare]" : "";
                String info = "  " + def.getNbtFileName()
                    + "  (w=" + def.getWeight()
                    + ", spacing=" + def.getMinSpacing()
                    + ", offset=" + def.getOriginYOffset()
                    + tag + ")  " + check;
                if ("MISSING".equals(check)) {
                    sendError(src, info);
                } else {
                    send(src, info);
                }
            }
        }

        send(src, "");
        send(src, "NBT directory: " + nbtDir);

        if (src.isPlayer()) {
            try {
                ServerPlayer player = src.getPlayerOrException();
                Optional<ResourceKey<Biome>> biomeKey = player.level().getBiome(player.blockPosition()).unwrapKey();
                String biomeName = biomeKey.map(k -> k.location().toString()).orElse("unknown");
                send(src, "Player biome: " + biomeName);
                send(src, "Player pos: " + player.blockPosition());
            } catch (Exception e) {
                sendError(src, "Failed to get player info.");
            }
        }

        send(src, "==============================");
        return defs.size();
    }

    /**
     * Lists all registered orchard definitions with their properties.
     *
     * @param ctx the command context
     * @return the number of registered definitions
     */
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

    /**
     * Sends a success message to the command source.
     *
     * @param src  the command source stack
     * @param text the message text
     */
    static void send(CommandSourceStack src, String text) {
        src.sendSuccess(() -> Component.literal(text), false);
    }

    /**
     * Sends an error message to the command source.
     *
     * @param src  the command source stack
     * @param text the error message text
     */
    static void sendError(CommandSourceStack src, String text) {
        src.sendFailure(Component.literal(text));
    }
}
