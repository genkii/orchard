package de.minehackers.orchard;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import de.minehackers.orchard.client.OrchardConfigScreen;

/// Mod Menu integration (Fabric only). Fabric instantiates this class only
/// when Mod Menu is installed, because the "modmenu" entrypoint is looked up
/// by Mod Menu itself - so the API classes here never load otherwise and
/// Orchard keeps working without it.
public class OrchardModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return OrchardConfigScreen::new;
    }
}
