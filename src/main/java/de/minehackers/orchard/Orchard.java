package de.minehackers.orchard;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import de.minehackers.orchard.config.ConfigLoader;
import de.minehackers.orchard.command.CommandRegistry;

@Mod(Orchard.MODID)
public class Orchard {

    public static final String MODID = "orchard";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static Path configDirectory;

    public static Path getConfigDirectory() {
        return configDirectory;
    }

    public Orchard(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        CommandRegistry.register();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("[Orchard] Common setup - loading config...");

        try {
            configDirectory = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
        } catch (Exception e) {
            LOGGER.error("[Orchard] Failed to resolve config directory: {}", e.getMessage());
            return;
        }

        Path customTreeDir = configDirectory.resolve("orchard");
        Path nbtDir = customTreeDir.resolve("nbt");
        Path dataDir = customTreeDir.resolve("data");

        ensureDirectory(customTreeDir);
        ensureDirectory(nbtDir);
        ensureDirectory(dataDir);

        List<OrchardDefinition> definitions = ConfigLoader.loadAll(configDirectory);
        OrchardRegistry.clearAndRegisterAll(definitions);

        int count = OrchardRegistry.getAll().size();
        LOGGER.info("[Orchard] ========================================");
        LOGGER.info("[Orchard] Mod initialised - {} definition(s) registered.", count);

        boolean anyMissing = false;
        for (OrchardDefinition def : OrchardRegistry.getAll()) {
            Path filePath = nbtDir.resolve(def.getNbtFileName());
            if (Files.exists(filePath)) {
                LOGGER.info("[Orchard]   found    {}", def.getNbtFileName());
            } else {
                LOGGER.warn("[Orchard]   MISSING  {}", def.getNbtFileName());
                anyMissing = true;
            }
        }

        if (anyMissing) {
            LOGGER.warn("[Orchard] One or more NBT files are missing.");
            LOGGER.warn("[Orchard] Place NBT files in: config/orchard/nbt/");
        }

        LOGGER.info("[Orchard] NBT dir: {}", nbtDir);
        LOGGER.info("[Orchard] Data dir: {}", dataDir);
        LOGGER.info("[Orchard] ========================================");
    }

    @net.neoforged.bus.api.SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("[Orchard] Server fully started - pre-warming NBT cache...");
        NbtTreePlacer.preWarmAll(
                event.getServer().overworld(),
                configDirectory.resolve("orchard").resolve("nbt"));
    }

    private static void ensureDirectory(Path dir) {
        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                LOGGER.info("[Orchard] Created directory: {}", dir);
            }
        } catch (Exception e) {
            LOGGER.error("[Orchard] Failed to create directory {}: {}", dir, e.getMessage());
        }
    }
}
