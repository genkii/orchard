package de.minehackers.orchard.pack;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import de.minehackers.orchard.Constants;
import de.minehackers.orchard.OrchardDefinition;
import de.minehackers.orchard.config.DataFileParser;
import de.minehackers.orchard.config.DefinitionCompiler;
import de.minehackers.orchard.config.RawDefinition;

/// Turns a pack directory into a Pack. Content problems are handled
/// leniently: broken files or definitions are logged and skipped so one
/// bad apple can't sink the rest of the pack. Structural problems
/// (missing/corrupt pack.yaml, unsupported format, nothing loadable)
/// reject the whole directory. The extracted defaults use loadBundled()
/// instead - they're plain data/ + nbt/ without a metadata file.
final class PackLoader {

    private PackLoader() {}

    record Result(@Nullable Pack pack, @Nullable String error) {

        boolean success() {
            return pack != null;
        }

        static Result ok(Pack pack) {
            return new Result(pack, null);
        }

        static Result fail(String error) {
            return new Result(null, error);
        }
    }

    /// Loads a normal user pack. pack.yaml is required here - a directory
    /// without one is not treated as a pack at all.
    static Result load(Path dir, PackSource source) {
        if (!Files.isDirectory(dir)) {
            return Result.fail("not a directory");
        }

        Path packFile = dir.resolve("pack.yaml");
        if (!Files.isRegularFile(packFile)) {
            return Result.fail("missing pack.yaml");
        }

        PackMetadata metadata;
        try {
            metadata = readMetadata(packFile, dir.getFileName().toString());
        } catch (PackLoadException e) {
            return Result.fail(e.getMessage());
        }

        if (!metadata.isFormatSupported()) {
            return Result.fail("pack format " + metadata.format()
                    + " is newer than the supported format "
                    + PackMetadata.SUPPORTED_FORMAT + " - update Orchard to use this pack");
        }

        return loadContents(dir, source, metadata);
    }

    /// Loads the extracted built-in defaults: data/ plus nbt/, and that's it.
    /// No pack.yaml exists for these - the metadata is synthesised on the spot.
    static Result loadBundled(Path dir) {
        if (!Files.isDirectory(dir)) {
            return Result.fail("not a directory");
        }
        return loadContents(dir, PackSource.BUNDLED, PackMetadata.bundled());
    }

    private static Result loadContents(Path dir, PackSource source, PackMetadata metadata) {
        Path dataDir = dir.resolve("data");
        if (!Files.isDirectory(dataDir)) {
            return Result.fail("missing data/ directory");
        }
        Path nbtDir = dir.resolve("nbt");
        if (!Files.isDirectory(nbtDir)) {
            Constants.LOG.warn("[Orchard] Pack '{}' has no nbt/ directory", metadata.name());
        }

        List<OrchardDefinition> definitions = new ArrayList<>();
        DynamicReferences.Builder refs = new DynamicReferences.Builder();
        List<Path> files = listDataFiles(dataDir);
        int fileFailures = 0;
        int defFailures = 0;

        for (Path file : files) {
            try {
                List<RawDefinition> raws = parseDataFile(file);
                for (int i = 0; i < raws.size(); i++) {
                    RawDefinition raw = raws.get(i);
                    String where = dataDir.relativize(file).toString() + " entry #" + (i + 1);
                    try {
                        DefinitionCompiler.Compiled compiled =
                                DefinitionCompiler.compile(raw, nbtDir);
                        definitions.add(compiled.definition());
                        if (compiled.references() != null) {
                            refs.merge(compiled.references());
                        }
                    } catch (Exception e) {
                        defFailures++;
                        Constants.LOG.error("[Orchard] {} : {}", where, e.getMessage());
                    }
                }
            } catch (Exception e) {
                fileFailures++;
                Constants.LOG.error("[Orchard] Failed to load {}: {}",
                        dataDir.relativize(file), e.getMessage());
            }
        }

        if (definitions.isEmpty()) {
            return Result.fail("no valid definitions found (" + fileFailures
                    + " file(s) and " + defFailures + " definition(s) failed - see log)");
        }

        if (fileFailures > 0 || defFailures > 0) {
            Constants.LOG.warn("[Orchard] Pack '{}': {} definition(s) loaded, {} file(s)/definition(s) skipped",
                    metadata.name(), definitions.size(), fileFailures + defFailures);
        }

        warnMissingNbt(metadata, definitions, nbtDir);

        Pack pack = new Pack(metadata, dir.toAbsolutePath(), source, definitions, refs.build());
        return Result.ok(pack);
    }

    /// yaml/yml files go to the YAML parser; anything else is rejected.
    private static List<RawDefinition> parseDataFile(Path file) {
        String name = file.getFileName().toString();
        if (name.endsWith(".yaml") || name.endsWith(".yml")) {
            return DataFileParser.parseFile(file);
        }
        throw new PackLoadException("unsupported data file extension '" + name + "' (expected .yaml or .yml)");
    }

    private static PackMetadata readMetadata(Path packFile, String directoryName) {
        Object root;
        try (Reader reader = Files.newBufferedReader(packFile, StandardCharsets.UTF_8)) {
            LoaderOptions options = new LoaderOptions();
            Yaml yaml = new Yaml(new SafeConstructor(options));
            root = yaml.load(reader);
        } catch (IOException e) {
            throw new PackLoadException("cannot read pack.yaml: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new PackLoadException("invalid YAML in pack.yaml: " + e.getMessage(), e);
        }
        Map<?, ?> map = de.minehackers.orchard.config.YamlValues.asMap(root, "pack.yaml");
        return PackMetadata.parse(map, directoryName);
    }

    private static List<Path> listDataFiles(Path dataDir) {
        try (Stream<Path> stream = Files.list(dataDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(PackLoader::hasKnownExtension)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            Constants.LOG.error("[Orchard] Failed to list {}: {}", dataDir, e.getMessage());
            return List.of();
        }
    }

    private static boolean hasKnownExtension(Path file) {
        String name = file.getFileName().toString();
        // Anything else is skipped silently - unknown extensions never reach
        // a parser and never cause an error.
        return name.endsWith(".yaml") || name.endsWith(".yml");
    }

    private static void warnMissingNbt(PackMetadata metadata, List<OrchardDefinition> definitions, Path nbtDir) {
        List<String> missing = new ArrayList<>();
        for (OrchardDefinition def : definitions) {
            if (!Files.isRegularFile(nbtDir.resolve(def.getNbtFileName()))) {
                missing.add(def.getNbtFileName());
            }
        }
        if (missing.isEmpty()) return;
        Constants.LOG.warn("[Orchard] Pack '{}' is missing {} NBT file(s) referenced by its data:",
                metadata.name(), missing.size());
        for (String name : missing) {
            Constants.LOG.warn("[Orchard]   MISSING  {}", name);
        }
        Constants.LOG.warn("[Orchard] Affected trees will fall back to vanilla world generation.");
    }
}
