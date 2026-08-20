package de.minehackers.orchard;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.world.level.ServerLevelAccessor;
import de.minehackers.orchard.config.ConfigLoader;

/// Loader-agnostic init logic. Each loader calls init() with its config dir.
public final class OrchardCommon {

    private static Path configDirectory;
    private static Path nbtDirectory;

    private OrchardCommon() {}

    /// Sets up directories, loads JSON configs, registers definitions.
    public static void init(Path configDir) {
        configDirectory = configDir;

        Path customTreeDir = configDir.resolve("orchard");
        Path nbtDir = customTreeDir.resolve("nbt");
        Path dataDir = customTreeDir.resolve("data");
        nbtDirectory = nbtDir;

        ensureDirectory(customTreeDir);
        ensureDirectory(nbtDir);
        ensureDirectory(dataDir);

        DefaultConfigExtractor.extractIfEmpty(configDir);

        List<OrchardDefinition> definitions = ConfigLoader.loadAll(configDir);
        OrchardRegistry.clearAndRegisterAll(definitions);

        int count = OrchardRegistry.getAll().size();
        Constants.LOG.info("[Orchard] ========================================");
        Constants.LOG.info("[Orchard] Mod initialised - {} definition(s) registered.", count);

        boolean anyMissing = false;
        for (OrchardDefinition def : OrchardRegistry.getAll()) {
            Path filePath = nbtDir.resolve(def.getNbtFileName());
            if (Files.exists(filePath)) {
                Constants.LOG.info("[Orchard]   found    {}", def.getNbtFileName());
            } else {
                Constants.LOG.warn("[Orchard]   MISSING  {}", def.getNbtFileName());
                anyMissing = true;
            }
        }

        if (anyMissing) {
            Constants.LOG.warn("[Orchard] One or more NBT files are missing.");
            Constants.LOG.warn("[Orchard] Place NBT files in: config/orchard/nbt/");
        }

        Constants.LOG.info("[Orchard] NBT dir: {}", nbtDir);
        Constants.LOG.info("[Orchard] Data dir: {}", dataDir);
        Constants.LOG.info("[Orchard] ========================================");
    }

    /// Pre-warms the NBT template cache once the server is up.
    public static void onServerStarted(ServerLevelAccessor level) {
        Constants.LOG.info("[Orchard] Server fully started - pre-warming NBT cache...");
        NbtTreePlacer.preWarmAll(level, getNbtDirectory());
    }

    public static Path getConfigDirectory() {
        return configDirectory;
    }

    public static Path getNbtDirectory() {
        return nbtDirectory;
    }

    private static void ensureDirectory(Path dir) {
        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                Constants.LOG.info("[Orchard] Created directory: {}", dir);
            }
        } catch (Exception e) {
            Constants.LOG.error("[Orchard] Failed to create directory {}: {}", dir, e.getMessage());
        }
    }
}
