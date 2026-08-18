package de.minehackers.orchard;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import de.minehackers.orchard.command.CommandRegistry;

/// NeoForge entrypoint.
@Mod(Constants.MOD_ID)
public class OrchardNeoForge {

    public OrchardNeoForge(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(this);
        try {
            var configDir = FMLPaths.CONFIGDIR.get();
            OrchardCommon.init(configDir);
        } catch (Exception e) {
            Constants.LOG.error("[Orchard] Failed to initialise: {}", e.getMessage(), e);
        }
        Constants.LOG.info("[Orchard] NeoForge init complete.");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandRegistry.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        OrchardCommon.onServerStarted(event.getServer().overworld());
    }
}
