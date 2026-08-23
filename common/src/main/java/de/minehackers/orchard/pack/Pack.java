package de.minehackers.orchard.pack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import de.minehackers.orchard.OrchardDefinition;

/// A pack that's fully loaded: metadata plus compiled, ready-to-use
/// definitions. By the time one of these exists, parsing, validation and
/// identifier resolution are all done - worldgen only ever reads the
/// finished product stored here.
public final class Pack {

    private final PackMetadata metadata;
    private final Path root;
    private final PackSource source;
    private final List<OrchardDefinition> definitions;
    private final DynamicReferences references;

    public Pack(PackMetadata metadata, Path root, PackSource source,
                List<OrchardDefinition> definitions, @Nullable DynamicReferences references) {
        this.metadata = metadata;
        this.root = root;
        this.source = source;
        this.definitions = List.copyOf(definitions);
        this.references = references == null ? DynamicReferences.EMPTY : references;
    }

    /// Placeholder for "nothing loaded" - keeps worldgen vanilla.
    public static Pack vanilla() {
        return new Pack(
                new PackMetadata("vanilla", PackMetadata.SUPPORTED_FORMAT, "", ""),
                Path.of(""), PackSource.NONE, List.of(), DynamicReferences.EMPTY);
    }

    public PackMetadata metadata() {
        return metadata;
    }

    /// Where the pack lives on disk (a packs/<name> folder or the bundled
    /// dir). Empty path for the vanilla placeholder.
    public Path root() {
        return root;
    }

    public Path nbtDirectory() {
        return root.resolve("nbt");
    }

    public Path dataDirectory() {
        return root.resolve("data");
    }

    public PackSource source() {
        return source;
    }

    public List<OrchardDefinition> definitions() {
        return definitions;
    }

    public DynamicReferences references() {
        return references;
    }

    public boolean hasNbtFile(String fileName) {
        return Files.isRegularFile(nbtDirectory().resolve(fileName));
    }

    @Override
    public String toString() {
        return metadata.toString();
    }
}
