package de.minehackers.orchard.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import de.minehackers.orchard.Constants;
import de.minehackers.orchard.NbtTreePlacer;
import de.minehackers.orchard.OrchardCommon;

/// /orchard test command.
public final class TestCommands {

    private TestCommands() {}

    /// Places an NBT at the player position (centered on the structure).
    /// Shows pre-placement check results so the user can diagnose world-gen issues.
    static int runTest(CommandContext<CommandSourceStack> ctx) {
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

        String name = StringArgumentType.getString(ctx, "name");
        if (name.endsWith(".nbt")) {
            name = name.substring(0, name.length() - 4);
        }

        Rotation rotation = Rotation.NONE;
        try {
            String rotStr = StringArgumentType.getString(ctx, "rotation");
            rotation = switch (rotStr.toLowerCase(Locale.ROOT)) {
                case "clockwise_90" -> Rotation.CLOCKWISE_90;
                case "clockwise_180" -> Rotation.CLOCKWISE_180;
                case "counterclockwise_90" -> Rotation.COUNTERCLOCKWISE_90;
                default -> Rotation.NONE;
            };
        } catch (Exception ignored) {}

        String fileName = name + ".nbt";
        Path nbtDir = OrchardCommon.getNbtDirectory();
        Path filePath = nbtDir.resolve(fileName);

        if (!Files.exists(filePath)) {
            StatusCommands.sendError(src, "NBT file not found: " + fileName);
            StatusCommands.sendError(src, "Place it in: " + nbtDir);
            return 0;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            StatusCommands.sendError(src, "Must be in a server level.");
            return 0;
        }

        try {
            StructureTemplate template = loadTemplate(filePath, level);
            if (template == null) {
                StatusCommands.sendError(src, "Failed to parse NBT file: " + fileName);
                return 0;
            }

            Vec3i size = template.getSize();
            BlockPos playerPos = player.blockPosition();
            BlockPos origin = playerPos.offset(-size.getX() / 2, 0, -size.getZ() / 2);

            StatusCommands.send(src, "==============================");
            StatusCommands.send(src, "  Test: " + fileName);
            StatusCommands.send(src, "  Size: " + size.getX() + "x" + size.getY() + "x" + size.getZ());
            StatusCommands.send(src, "  Rotation: " + rotation.name().toLowerCase(Locale.ROOT));
            StatusCommands.send(src, "==============================");

            StatusCommands.send(src, "Pre-placement checks:");
            boolean surface = NbtTreePlacer.isOnSurface(level, origin);
            StatusCommands.send(src, "  Surface: " + (surface ? "PASS" : "FAIL (not on open air)"));

            boolean trunkClear = NbtTreePlacer.isTrunkClear(level, origin, size.getY());
            StatusCommands.send(src, "  Trunk clear: " + (trunkClear ? "PASS" : "FAIL (bedrock/lava in column)"));

            boolean placementClear = NbtTreePlacer.isPlacementClear(level, origin, size);
            StatusCommands.send(src, "  Area clear: " + (placementClear ? "PASS" : "FAIL (too many obstructions)"));

            boolean groundOk = true;
            BlockPos adjusted = NbtTreePlacer.groundAdjust(level, origin, NbtTreePlacer.getMaxGroundAdjust());
            if (!adjusted.equals(origin)) {
                StatusCommands.send(src, "  Ground adjust: " + origin.getY() + " -> " + adjusted.getY());
                origin = adjusted;
                groundOk = NbtTreePlacer.isOnSurface(level, origin);
                StatusCommands.send(src, "  Surface after adjust: " + (groundOk ? "PASS" : "FAIL"));
            } else {
                StatusCommands.send(src, "  Ground adjust: none needed");
            }

            StatusCommands.send(src, "==============================");

            StructurePlaceSettings settings = new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(rotation)
                .setIgnoreEntities(true)
                .addProcessor(NbtTreePlacer.getTerrainPreservingProcessor());

            template.placeInWorld(level, origin, BlockPos.ZERO, settings, level.getRandom(), 3);

            StatusCommands.send(src, "Placed at: " + origin);
            StatusCommands.send(src, "==============================");
            Constants.LOG.info("[Orchard] Debug placement: {} at {} (rotation: {})",
                    fileName, origin, rotation.name());
            return 1;
        } catch (Exception e) {
            StatusCommands.sendError(src, "Failed to place: " + e.getMessage());
            Constants.LOG.error("[Orchard] Debug test failed for {}: {}", fileName, e.getMessage());
            return 0;
        }
    }

    /// Loads and parses an NBT template from disk.
    static StructureTemplate loadTemplate(Path filePath, ServerLevel level) {
        try {
            long fileSize = Files.size(filePath);
            if (fileSize > Constants.MAX_NBT_FILE_SIZE) {
                Constants.LOG.error("[Orchard] NBT file too large: {} ({} bytes, max {} bytes)",
                        filePath.getFileName(), fileSize, Constants.MAX_NBT_FILE_SIZE);
                return null;
            }
            if (fileSize == 0) {
                Constants.LOG.error("[Orchard] NBT file is empty: {}", filePath.getFileName());
                return null;
            }
        } catch (Exception e) {
            Constants.LOG.warn("[Orchard] Could not check file size for {}: {}",
                    filePath.getFileName(), e.getMessage());
        }

        try {
            CompoundTag nbt;
            try (InputStream in = Files.newInputStream(filePath)) {
                nbt = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            }
            StructureTemplate template = new StructureTemplate();
            template.load(level.registryAccess().lookupOrThrow(Registries.BLOCK), nbt);
            return template;
        } catch (Exception e) {
            Constants.LOG.error("[Orchard] Failed to load template {}: {}", filePath.getFileName(), e.getMessage());
            return null;
        }
    }
}
