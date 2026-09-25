package de.minehackers.orchard;

import java.nio.file.Path;
import net.minecraft.world.level.ServerLevelAccessor;
import de.minehackers.orchard.pack.Pack;
import de.minehackers.orchard.pack.PackManager;

/// Loader-agnostic startup: reads config, scans packs, and activates one pack.
public final class OrchardCommon {

    private static Path configDirectory;

    private OrchardCommon() {}

    /// One-time startup: wires up directories and brings a pack online.
    public static void init(Path configDir) {
        configDirectory = configDir.toAbsolutePath();

        try {
            PackManager.boot(configDir);
        } catch (Exception e) {
            // Rethrow so the loader entrypoint keeps logging the stack trace,
            // but remember the failure - /orchard status and /orchard reload
            // use it to explain why nothing is happening.
            PackManager.recordBootFailure(e.getMessage() != null ? e.getMessage() : e.toString());
            throw e;
        }

        Constants.LOG.info("[Orchard] ========================================");
        Constants.LOG.info("[Orchard] Mod initialised.");
        logActivePack();
        Constants.LOG.info("[Orchard] Packs directory: {}", getPackDirectory());
        Constants.LOG.info("[Orchard] Generated dir:   {}", getGeneratedDirectory());
        Constants.LOG.info("[Orchard] ========================================");
    }

    /// Post-server startup: pre-warms the NBT cache and validates pack references.
    public static void onServerStarted(ServerLevelAccessor level) {
        Constants.LOG.info("[Orchard] Server fully started - pre-warming NBT cache...");
        NbtTreePlacer.preWarmAll(level, getNbtDirectory());
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            PackManager.onServerStarted(serverLevel);
        }
    }

    private static void logActivePack() {
        Pack pack = PackManager.activePack();
        switch (pack.source()) {
            case PACKS -> Constants.LOG.info("[Orchard] Active pack:    {} ({})",
                    pack.metadata().name(), pack.root().getFileName());
            case BUNDLED -> Constants.LOG.info("[Orchard] Active pack:    {} (bundled)",
                    pack.metadata().name());
            case NONE -> {
                Constants.LOG.warn("[Orchard] No pack active  - vanilla world generation is untouched.");
                Constants.LOG.warn("[Orchard] Create a pack under: " + getPackDirectory());
            }
        }
        Constants.LOG.info("[Orchard] Definitions:     {}", pack.definitions().size());
    }

    public static Path getConfigDirectory() {
        return configDirectory;
    }

    /// Root of the orchard folder under the game's config directory.
    public static Path getOrchardDirectory() {
        return configDirectory.resolve("orchard");
    }

    /// Where user-provided packs live.
    public static Path getPackDirectory() {
        return getOrchardDirectory().resolve("packs");
    }

    /// Staging area for output of the /orchard create command.
    public static Path getGeneratedDirectory() {
        return getOrchardDirectory().resolve("generated");
    }

    /// Points into whatever pack is currently active.
    public static Path getNbtDirectory() {
        return PackManager.activePack().nbtDirectory();
    }
}
