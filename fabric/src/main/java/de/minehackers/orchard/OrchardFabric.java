package de.minehackers.orchard;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import de.minehackers.orchard.command.CommandRegistry;

/// Fabric entrypoint.
public class OrchardFabric implements ModInitializer {

    public OrchardFabric() {}

    @Override
    public void onInitialize() {
        try {
            var configDir = FabricLoader.getInstance().getConfigDir();
            OrchardCommon.init(configDir);
        } catch (Exception e) {
            Constants.LOG.error("[Orchard] Failed to initialise: {}", e.getMessage(), e);
        }

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
                CommandRegistry.register(dispatcher));

        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                OrchardCommon.onServerStarted(server.overworld()));

        Constants.LOG.info("[Orchard] Fabric init complete.");
    }
}
