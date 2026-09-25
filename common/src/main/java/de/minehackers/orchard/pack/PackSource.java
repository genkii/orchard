package de.minehackers.orchard.pack;

/// Active pack origin: user packs, bundled defaults, or vanilla fallback.
public enum PackSource {

    /// User pack in config/orchard/packs/<name>/.
    PACKS,

    /// Built-in defaults extracted from the JAR into config/orchard/bundled/.
    BUNDLED,

    /// No usable pack found, vanilla world generation stays active.
    NONE
}
