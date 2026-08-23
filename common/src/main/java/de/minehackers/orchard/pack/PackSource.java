package de.minehackers.orchard.pack;

/// Where the active pack came from. Resolution goes packs/ first, then the
/// bundled defaults pulled out of the JAR, and if neither pans out we simply
/// stay on vanilla worldgen.
public enum PackSource {

    /// A user-made pack living in config/orchard/packs/<name>/.
    PACKS,

    /// The built-in defaults extracted from the Orchard JAR into config/orchard/bundled/.
    BUNDLED,

    /// Nothing usable was found, so vanilla world generation stays untouched.
    NONE
}
