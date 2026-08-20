package de.minehackers.orchard.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import de.minehackers.orchard.NbtTreePlacer;

/// /orchard stats command - shows runtime statistics.
public final class StatsCommands {

    private StatsCommands() {}

    static int runStats(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();

        StatusCommands.send(src, "==============================");
        StatusCommands.send(src, "      Orchard Stats");
        StatusCommands.send(src, "==============================");

        for (String line : NbtTreePlacer.getDetailedStats()) {
            StatusCommands.send(src, "  " + line);
        }

        StatusCommands.send(src, "");
        StatusCommands.send(src, "  Rare pool: "
            + String.format("%.1f%%", de.minehackers.orchard.OrchardRegistry.getRarePoolProbability() * 100));
        StatusCommands.send(src, "  Max obstructed: "
            + String.format("%.0f%%", NbtTreePlacer.getMaxObstructedFraction() * 100));

        StatusCommands.send(src, "==============================");
        return 1;
    }
}
