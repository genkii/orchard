package de.minehackers.orchard.command;

import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import de.minehackers.orchard.OrchardCommon;
import de.minehackers.orchard.pack.Pack;
import de.minehackers.orchard.pack.PackManager;

/// The /orchard packs subcommand - shows the active pack and everything else found.
public final class PackCommands {

    private PackCommands() {}

    static int runPacks(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();

        StatusCommands.send(src, "==============================");
        StatusCommands.send(src, "      Orchard Packs");
        StatusCommands.send(src, "==============================");

        Pack active = PackManager.activePack();
        switch (active.source()) {
            case PACKS -> StatusCommands.send(src, "Active: " + active.metadata().name()
                    + " (" + active.root().getFileName() + ")");
            case BUNDLED -> StatusCommands.send(src, "Active: built-in defaults ("
                    + active.root().getFileName() + "/)");
            case NONE -> StatusCommands.sendError(src, "Active: none - vanilla world generation");
        }
        if (!active.metadata().format().isBlank()) {
            StatusCommands.send(src, "Pack format: " + active.metadata().format());
        }

        StatusCommands.send(src, "");
        PackManager.PackScan scan = PackManager.scanPacks();
        List<Pack> packs = scan.valid();
        if (packs.isEmpty() && scan.failed().isEmpty()) {
            StatusCommands.send(src, "No user packs in: " + OrchardCommon.getPackDirectory());
        } else {
            if (!packs.isEmpty()) {
                StatusCommands.send(src, "Available user packs (" + packs.size() + ", alphabetical order):");
                for (Pack pack : packs) {
                    String marker = active == pack ? " <- active" : "";
                    StatusCommands.send(src, "  " + pack.root().getFileName()
                            + ": " + pack.metadata().describe()
                            + " - " + pack.definitions().size() + " definition(s)" + marker);
                }
            }
            if (!scan.failed().isEmpty()) {
                StatusCommands.sendError(src, "Could not be loaded (" + scan.failed().size()
                        + ") - see the log for details:");
                scan.failed().forEach((name, error) ->
                        StatusCommands.send(src, "  " + name + ": " + firstLine(error)));
            }
        }

        StatusCommands.send(src, "");
        StatusCommands.send(src, "Data formats: yaml, yml");
        StatusCommands.send(src, "Select a pack in orchard.yaml:");
        StatusCommands.send(src, "  pack:");
        StatusCommands.send(src, "    selected: auto");
        StatusCommands.send(src, "==============================");
        return packs.size() + scan.failed().size();
    }

    /// Error messages can span lines; chat rows stay readable with just the first.
    private static String firstLine(String error) {
        int nl = error.indexOf('\n');
        return nl < 0 ? error : error.substring(0, nl);
    }
}
