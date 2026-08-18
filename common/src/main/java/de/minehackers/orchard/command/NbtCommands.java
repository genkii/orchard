package de.minehackers.orchard.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import de.minehackers.orchard.OrchardCommon;

/// /orchard nbt info command.
public final class NbtCommands {

    private NbtCommands() {}

    /// Shows file size and parsed dimensions of an NBT template.
    static int runNbtInfo(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");
        if (name.endsWith(".nbt")) {
            name = name.substring(0, name.length() - 4);
        }

        String fileName = name + ".nbt";
        Path nbtDir = OrchardCommon.getNbtDirectory();
        Path filePath = nbtDir.resolve(fileName);

        if (!Files.exists(filePath)) {
            StatusCommands.sendError(src, "NBT file not found: " + fileName);
            StatusCommands.sendError(src, "Looked in: " + nbtDir);
            return 0;
        }

        StatusCommands.send(src, "==============================");
        StatusCommands.send(src, "  NBT Info: " + fileName);
        StatusCommands.send(src, "==============================");

        try {
            long fileSize = Files.size(filePath);
            StatusCommands.send(src, "File size: " + formatSize(fileSize));

            if (src.isPlayer()) {
                ServerPlayer player = src.getPlayerOrException();
                if (player.level() instanceof ServerLevel level) {
                    StructureTemplate template = TestCommands.loadTemplate(filePath, level);
                    if (template != null) {
                        Vec3i size = template.getSize();
                        StatusCommands.send(src, "Dimensions: " + size.getX() + " x " + size.getY() + " x " + size.getZ());
                    } else {
                        StatusCommands.sendError(src, "Failed to parse NBT structure.");
                    }
                }
            }
        } catch (Exception e) {
            StatusCommands.sendError(src, "Error reading file: " + e.getMessage());
        }

        StatusCommands.send(src, "==============================");
        return 1;
    }

    static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
