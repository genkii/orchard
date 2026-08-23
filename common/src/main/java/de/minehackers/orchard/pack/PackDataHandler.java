package de.minehackers.orchard.pack;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import de.minehackers.orchard.config.RawDefinition;

/// Extension point for addons that add their own data-file formats to
/// packs. Found via ServiceLoader on every boot and reload - shipping the
/// META-INF/services entry is the entire integration, nothing else needed.
/// A handler claims extensions under data/ and its output runs through the
/// same compile+validate pipeline as YAML. Throwing PackLoadException from
/// parse just logs and skips the file, same as broken YAML.
public interface PackDataHandler {

    /// Extensions this handler claims, lowercase, no leading dot - e.g. js.
    Set<String> supportedExtensions();

    /// Parses a single data file. If you throw PackLoadException, your
    /// message is what the user sees - make it count.
    List<RawDefinition> parse(Path file) throws PackLoadException;
}
