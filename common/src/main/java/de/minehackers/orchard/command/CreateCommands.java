package de.minehackers.orchard.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import de.minehackers.orchard.Constants;
import de.minehackers.orchard.OrchardCommon;

/// /orchard create command - exports a world region as an NBT structure file.
public final class CreateCommands {

    private CreateCommands() {}

    /// Captures the region between two positions and saves it as an NBT file.
    static int runCreate(CommandContext<CommandSourceStack> ctx) {
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

        if (!(player.level() instanceof ServerLevel level)) {
            StatusCommands.sendError(src, "Must be in a server level.");
            return 0;
        }

        BlockPos pos1;
        BlockPos pos2;
        try {
            pos1 = BlockPosArgument.getLoadedBlockPos(ctx, "pos1");
            pos2 = BlockPosArgument.getLoadedBlockPos(ctx, "pos2");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            StatusCommands.sendError(src, "Invalid position: " + e.getMessage());
            return 0;
        }

        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        BlockPos origin = new BlockPos(minX, minY, minZ);
        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;

        if (sizeX * sizeY * sizeZ > 512 * 512 * 512) {
            StatusCommands.sendError(src, "Region too large: " + sizeX + "x" + sizeY + "x" + sizeZ
                + " (max 512x512x512)");
            return 0;
        }

        String name = StringArgumentType.getString(ctx, "name");
        if (name.endsWith(".nbt")) {
            name = name.substring(0, name.length() - 4);
        }

        String fileName = name + ".nbt";
        Path generatedDir = OrchardCommon.getGeneratedDirectory();
        Path filePath = generatedDir.resolve(fileName);

        StatusCommands.send(src, "==============================");
        StatusCommands.send(src, "  Creating: " + fileName);
        StatusCommands.send(src, "  Region: " + sizeX + "x" + sizeY + "x" + sizeZ);
        StatusCommands.send(src, "==============================");

        try {
            StructureTemplate template = new StructureTemplate();
            template.fillFromWorld(level, origin, new Vec3i(sizeX, sizeY, sizeZ),
                true, Collections.emptyList());

            CompoundTag nbt = template.save(new CompoundTag());
            Vec3i actualSize = template.getSize();
            int blockCount = template.filterBlocks(origin, new StructurePlaceSettings(),
                null).size();

            try (OutputStream out = Files.newOutputStream(filePath)) {
                NbtIo.writeCompressed(nbt, out);
            }

            long fileSize = Files.size(filePath);

            StatusCommands.send(src, "Exported: " + fileName);
            StatusCommands.send(src, "  Blocks: " + blockCount);
            StatusCommands.send(src, "  Dimensions: " + actualSize.getX()
                + "x" + actualSize.getY() + "x" + actualSize.getZ());
            StatusCommands.send(src, "  File size: " + NbtCommands.formatSize(fileSize));
            StatusCommands.send(src, "  Saved to: " + generatedDir.resolve(fileName));
            StatusCommands.send(src, "");
            StatusCommands.send(src, "Next steps:");
            StatusCommands.send(src, "  1. Move to config/orchard/nbt/ when ready");
            StatusCommands.send(src, "  2. Create a JSON config in config/orchard/data/");
            StatusCommands.send(src, "  3. Run /orchard reload");
            StatusCommands.send(src, "==============================");

            Constants.LOG.info("[Orchard] Exported structure: {} ({}x{}x{}, {} blocks, {})",
                fileName, actualSize.getX(), actualSize.getY(), actualSize.getZ(),
                blockCount, NbtCommands.formatSize(fileSize));
            return 1;
        } catch (IOException e) {
            StatusCommands.sendError(src, "Failed to write file: " + e.getMessage());
            Constants.LOG.error("[Orchard] Failed to export {}: {}", fileName, e.getMessage());
            return 0;
        } catch (Exception e) {
            StatusCommands.sendError(src, "Failed to capture region: " + e.getMessage());
            Constants.LOG.error("[Orchard] Failed to export {}: {}", fileName, e.getMessage());
            return 0;
        }
    }
}
