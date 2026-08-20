package de.minehackers.orchard;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/// Extracts bundled default configs from the JAR to the config directory on first run.
/// Uses a manifest file (default-config/manifest.txt) to list all bundled resources,
/// since Java's classloader cannot list directory contents.
public final class DefaultConfigExtractor {

    private static final String MANIFEST_PATH = "default-config/manifest.txt";
    private static final String BUNDLE_PREFIX = "default-config/";

    private DefaultConfigExtractor() {}

    /// Extracts bundled defaults if the orchard config directory doesn't exist or is empty.
    /// Never overwrites existing files.
    public static void extractIfEmpty(Path configDir) {
        Path orchardDir = configDir.resolve("orchard");
        Path dataDir = orchardDir.resolve("data");
        Path nbtDir = orchardDir.resolve("nbt");

        if (hasConfigFiles(dataDir, nbtDir)) {
            return;
        }

        Constants.LOG.info("[Orchard] Config directory is empty or missing - extracting bundled defaults...");

        try {
            extractAll(configDir);
            int count = countExtracted(dataDir, nbtDir);
            Constants.LOG.info("[Orchard] Extracted {} bundled config file(s).", count);
        } catch (Exception e) {
            Constants.LOG.error("[Orchard] Failed to extract bundled configs: {}", e.getMessage());
        }
    }

    private static void extractAll(Path configDir) throws Exception {
        InputStream manifestStream = DefaultConfigExtractor.class
                .getClassLoader()
                .getResourceAsStream(MANIFEST_PATH);

        if (manifestStream == null) {
            Constants.LOG.debug("[Orchard] No bundled config manifest found - skipping extraction.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(manifestStream))) {
            String resourcePath;
            while ((resourcePath = reader.readLine()) != null) {
                resourcePath = resourcePath.trim();
                if (resourcePath.isEmpty()) continue;

                // Strip the "default-config/" prefix to get the relative path under orchard/
                String relativePath = resourcePath.substring(BUNDLE_PREFIX.length());
                Path target = configDir.resolve("orchard").resolve(relativePath);

                if (Files.exists(target)) continue;

                try (InputStream in = DefaultConfigExtractor.class
                        .getClassLoader()
                        .getResourceAsStream(resourcePath)) {
                    if (in == null) {
                        Constants.LOG.warn("[Orchard] Bundled resource not found: {}", resourcePath);
                        continue;
                    }
                    Files.createDirectories(target.getParent());
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    Constants.LOG.debug("[Orchard] Extracted: {}", relativePath);
                }
            }
        }
    }

    private static boolean hasConfigFiles(Path dataDir, Path nbtDir) {
        if (Files.isDirectory(dataDir)) {
            try (var stream = Files.list(dataDir)) {
                if (stream.anyMatch(p -> p.toString().endsWith(".json"))) return true;
            } catch (Exception ignored) {}
        }
        if (Files.isDirectory(nbtDir)) {
            try (var stream = Files.list(nbtDir)) {
                if (stream.anyMatch(p -> p.toString().endsWith(".nbt"))) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    private static int countExtracted(Path dataDir, Path nbtDir) {
        int count = 0;
        if (Files.isDirectory(dataDir)) {
            try (var stream = Files.list(dataDir)) {
                count += stream.filter(p -> p.toString().endsWith(".json")).count();
            } catch (Exception ignored) {}
        }
        if (Files.isDirectory(nbtDir)) {
            try (var stream = Files.list(nbtDir)) {
                count += stream.filter(p -> p.toString().endsWith(".nbt")).count();
            } catch (Exception ignored) {}
        }
        return count;
    }
}
