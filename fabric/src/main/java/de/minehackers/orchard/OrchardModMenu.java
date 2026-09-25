package de.minehackers.orchard;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import de.minehackers.orchard.client.OrchardConfigScreen;

/// Mod Menu integration loaded only when Mod Menu is installed.
public class OrchardModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return OrchardConfigScreen::new;
    }
}
