package de.minehackers.orchard.pack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import de.minehackers.orchard.OrchardDefinition;

/// Fully loaded pack with metadata and compiled ready-to-use definitions.
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

    /// Empty placeholder pack keeping worldgen vanilla when nothing is loaded.
    public static Pack vanilla() {
        return new Pack(
                new PackMetadata("vanilla", PackMetadata.SUPPORTED_FORMAT, "", ""),
                Path.of(""), PackSource.NONE, List.of(), DynamicReferences.EMPTY);
    }

    public PackMetadata metadata() {
        return metadata;
    }

    /// Pack root directory on disk, empty path for the vanilla placeholder.
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
