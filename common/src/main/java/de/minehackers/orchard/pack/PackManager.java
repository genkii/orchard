package de.minehackers.orchard.pack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import de.minehackers.orchard.Constants;
import de.minehackers.orchard.NbtTreePlacer;
import de.minehackers.orchard.OrchardRegistry;
import de.minehackers.orchard.matchers.FeatureIndex;

/// Discovers, selects and activates packs. Selection walks a chain:
/// user packs in config/orchard/packs (scanned alphabetically), then the
/// bundled defaults from the JAR, and finally vanilla worldgen if nothing
/// else worked. A pack.selected entry in orchard.yaml pins a specific
/// pack, but when it can't be loaded we log the error and fall back to
/// auto rather than ending up with nothing.
public final class PackManager {

    // All three are volatile: boot/reload swap them wholesale while other
    // threads may read them at any moment.
    private static volatile OrchardSettings settings = OrchardSettings.withDefaults();
    private static volatile Pack activePack = Pack.vanilla();
    private static volatile Map<String, PackDataHandler> dataHandlers = Map.of();
    private static volatile Path orchardDirectory;
    private static volatile String bootError;

    private PackManager() {}

    /// Startup: create the folder layout, pick up data-handler addons, read
    /// orchard.yaml, extract the defaults, then choose and activate a pack.
    public static void boot(Path configDir) {
        Path orchardDir = configDir.resolve("orchard");
        ensureDirectory(orchardDir);
        ensureDirectory(orchardDir.resolve("packs"));
        ensureDirectory(orchardDir.resolve("generated"));
        orchardDirectory = orchardDir.toAbsolutePath();

        discoverDataHandlers();
        settings = OrchardSettings.load(orchardDir);
        OrchardRegistry.setRarePoolProbability(settings.rarePoolProbability());
        NbtTreePlacer.setMaxObstructedFraction(settings.maxObstructedFraction());

        BundledPackExtractor.extractIfMissing(orchardDir);

        selectAndActivate("startup");
        bootError = null;
    }

    /// Remembers why startup failed, so commands can tell the user instead of
    /// silently acting like a vanilla-placeholder install.
    public static void recordBootFailure(String message) {
        bootError = message;
    }

    /// Non-null when init failed; check before assuming anything works.
    public static String bootError() {
        return bootError;
    }

    /// Runs selection and activation again; this is what /orchard reload
    /// calls. Settings and handlers are refreshed along the way. The template
    /// cache is dropped first: once the new definitions are published the old
    /// pack's files must not be placeable anymore, even briefly.
    public static Pack reload() {
        if (orchardDirectory == null) return activePack;
        discoverDataHandlers();
        settings = OrchardSettings.load(orchardDirectory);
        OrchardRegistry.setRarePoolProbability(settings.rarePoolProbability());
        NbtTreePlacer.setMaxObstructedFraction(settings.maxObstructedFraction());
        NbtTreePlacer.clearCache();
        selectAndActivate("reload");
        bootError = null;
        return activePack;
    }

    /// Late check of the pack's dynamic references, now that the registries
    /// finally exist. Missing identifiers only ever warn.
    public static void onServerStarted(ServerLevel level) {
        HolderLookup.Provider registries = level.registryAccess();
        FeatureIndex.rebuild(registries, level);

        Pack pack = activePack;
        if (pack.source() == PackSource.NONE || pack.references().isEmpty()) {
            return;
        }
        warnMissingReferences(pack, registries);
    }

    public static OrchardSettings settings() {
        return settings;
    }

    public static Pack activePack() {
        return activePack;
    }

    /// Everything found in the packs folder: loadable packs plus, separately,
    /// the folders that failed with their error so commands can show them.
    public record PackScan(List<Pack> valid, Map<String, String> failed) {}

    public static PackScan scanPacks() {
        List<Path> dirs = listPackDirectories();
        List<Pack> packs = new ArrayList<>(dirs.size());
        Map<String, String> failed = new TreeMap<>();
        for (Path dir : dirs) {
            PackLoader.Result result = PackLoader.load(dir, PackSource.PACKS);
            if (result.success()) {
                packs.add(result.pack());
            } else {
                failed.put(String.valueOf(dir.getFileName()),
                        result.error() != null ? result.error() : "unknown error");
            }
        }
        return new PackScan(List.copyOf(packs), Map.copyOf(failed));
    }

    /// Re-scans for PackDataHandler addons on every boot and reload, so an
    /// addon installed between sessions (or mid-session, after a reload)
    /// gets picked up. If two handlers claim the same extension, whoever
    /// registered first wins and the clash is only warned about.
    public static void discoverDataHandlers() {
        Map<String, PackDataHandler> found = new TreeMap<>();
        try {
            ServiceLoader<PackDataHandler> loader = ServiceLoader.load(
                    PackDataHandler.class, PackManager.class.getClassLoader());
            for (PackDataHandler handler : loader) {
                for (String rawExtension : handler.supportedExtensions()) {
                    if (rawExtension == null || rawExtension.isBlank()) continue;
                    String extension = rawExtension.trim().toLowerCase(java.util.Locale.ROOT);
                    PackDataHandler previous = found.putIfAbsent(extension, handler);
                    if (previous != null && previous != handler) {
                        Constants.LOG.warn("[Orchard] Both {} and {} claim '.{}' files - keeping {}",
                                previous.getClass().getName(), handler.getClass().getName(),
                                extension, previous.getClass().getName());
                    }
                }
            }
        } catch (Exception e) {
            Constants.LOG.error("[Orchard] Failed to load pack data handlers: {}", e.getMessage(), e);
        }
        dataHandlers = Map.copyOf(found);
        if (!found.isEmpty()) {
            Constants.LOG.info("[Orchard] Registered pack data handler(s) for extension(s): {}",
                    found.keySet());
        }
    }

    static Optional<PackDataHandler> handlerFor(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0) return Optional.empty();
        return Optional.ofNullable(dataHandlers.get(name.substring(dot + 1).toLowerCase(java.util.Locale.ROOT)));
    }

    /// Every registered data-file extension, lowercase, without the dot.
    public static java.util.Set<String> registeredDataExtensions() {
        return dataHandlers.keySet();
    }

    private static void selectAndActivate(String phase) {
        long start = System.nanoTime();

        Pack selected = select();

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        OrchardRegistry.clearAndRegisterAll(selected.definitions());
        activePack = selected;

        switch (selected.source()) {
            case PACKS -> Constants.LOG.info("[Orchard] Active pack: {} [{}] - {} definition(s) loaded in {} ms",
                    selected.metadata().name(), selected.root().getFileName(), selected.definitions().size(), elapsedMs);
            case BUNDLED -> Constants.LOG.info("[Orchard] No user pack found - using built-in defaults: {} definition(s) loaded in {} ms",
                    selected.definitions().size(), elapsedMs);
            case NONE -> Constants.LOG.info("[Orchard] No valid pack available - vanilla world generation will be used ({}).", phase);
        }
    }

    private static Pack select() {
        String explicit = settings.explicitPackName();

        if (explicit != null) {
            Path pinned = orchardDirectory.resolve("packs").resolve(explicit);
            if (!isValidDirectoryName(explicit)) {
                Constants.LOG.error("[Orchard] Invalid pack name '{}' in orchard.yaml - falling back to auto.", explicit);
            } else {
                PackLoader.Result result = PackLoader.load(pinned, PackSource.PACKS);
                if (result.success()) {
                    return result.pack();
                }
                Constants.LOG.error("[Orchard] Selected pack '{}' could not be loaded: {} - falling back to auto.",
                        explicit, result.error());
            }
        }

        for (Path dir : listPackDirectories()) {
            PackLoader.Result result = PackLoader.load(dir, PackSource.PACKS);
            if (result.success()) {
                return result.pack();
            }
            Constants.LOG.error("[Orchard] Skipping invalid pack '{}': {}", dir.getFileName(), result.error());
        }

        if (BundledPackExtractor.isAvailable()) {
            Path bundledDir = BundledPackExtractor.extractIfMissing(orchardDirectory);
            PackLoader.Result result = PackLoader.loadBundled(bundledDir);
            if (result.success()) {
                return result.pack();
            }
            Constants.LOG.error("[Orchard] The built-in default config is invalid: {} - falling back to vanilla world generation.",
                    result.error());
        } else {
            Constants.LOG.info("[Orchard] No user pack found and this JAR ships without the built-in defaults.");
        }

        return Pack.vanilla();
    }

    /// Sorted by name on purpose - pack selection must not depend on
    /// filesystem order.
    private static List<Path> listPackDirectories() {
        Path packsDir = orchardDirectory.resolve("packs");
        if (!Files.isDirectory(packsDir)) return List.of();
        try (Stream<Path> stream = Files.list(packsDir)) {
            return stream
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            Constants.LOG.error("[Orchard] Failed to list packs directory: {}", e.getMessage());
            return List.of();
        }
    }

    private static boolean isValidDirectoryName(String name) {
        if (name.isBlank() || name.contains("/") || name.contains("\\") || name.contains("..")) {
            return false;
        }
        return name.chars().allMatch(c -> Character.isLetterOrDigit(c)
                || c == '-' || c == '_' || c == '.' || c == ' ');
    }

    private static void warnMissingReferences(Pack pack, HolderLookup.Provider registries) {
        String name = pack.metadata().name();

        var biomeRegistry = registries.lookupOrThrow(Registries.BIOME);
        for (Identifier id : pack.references().biomes()) {
            if (!biomeRegistry.get(ResourceKey.create(Registries.BIOME, id)).isPresent()) {
                Constants.LOG.warn(
                        "[Orchard] Pack '{}': biome '{}' is not present in this instance "
                        + "(is its mod missing?) - matching rules will simply never match",
                        name, id);
            }
        }

        var dimensionRegistry = registries.lookupOrThrow(Registries.DIMENSION);
        for (var key : pack.references().dimensions()) {
            if (!dimensionRegistry.get(key).isPresent()) {
                Constants.LOG.warn(
                        "[Orchard] Pack '{}': dimension '{}' is not present in this instance "
                        + "(is its mod missing?) - matching rules will simply never match",
                        name, key.identifier());
            }
        }

        var featureRegistry = registries.lookupOrThrow(Registries.CONFIGURED_FEATURE);
        for (Identifier id : pack.references().features()) {
            if (!featureRegistry.get(ResourceKey.create(Registries.CONFIGURED_FEATURE, id)).isPresent()) {
                Constants.LOG.warn(
                        "[Orchard] Pack '{}': feature '{}' is not present in this instance "
                        + "(is its mod missing?) - matching rules will simply never match",
                        name, id);
            }
        }
    }

    private static void ensureDirectory(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            Constants.LOG.error("[Orchard] Failed to create directory {}: {}", dir, e.getMessage());
        }
    }
}
