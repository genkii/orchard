package de.minehackers.orchard.pack;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import de.minehackers.orchard.Constants;

/// Copies the built-in default config from the JAR into
/// config/orchard/bundled/ on first run. Resources live under
/// default-config/ and are enumerated via a manifest because classloaders
/// can't list directories. If the target dir exists we skip entirely -
/// deliberate, so users can delete defaults they don't want - and that
/// also means an interrupted extraction is never resumed.
public final class BundledPackExtractor {

    private static final String MANIFEST_PATH = "default-config/manifest.txt";
    private static final String RESOURCE_PREFIX = "default-config/";

    private BundledPackExtractor() {}

    /// Whether this JAR ships the bundled defaults at all (-TINY builds don't).
    public static boolean isAvailable() {
        return BundledPackExtractor.class.getClassLoader().getResource(MANIFEST_PATH) != null;
    }

    /// Extracts the defaults unless bundled/ already exists. Returns the
    /// target path, which may simply not exist for -TINY builds - callers
    /// must cope with that.
    public static Path extractIfMissing(Path orchardDir) {
        Path target = orchardDir.resolve("bundled");
        if (Files.isDirectory(target)) {
            return target;
        }

        try {
            InputStream manifest = BundledPackExtractor.class
                    .getClassLoader()
                    .getResourceAsStream(MANIFEST_PATH);
            if (manifest == null) {
                Constants.LOG.debug("[Orchard] No built-in default config in this JAR (-TINY build).");
                return target;
            }

            int count = 0;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(manifest, StandardCharsets.UTF_8))) {
                String resourcePath;
                while ((resourcePath = reader.readLine()) != null) {
                    resourcePath = resourcePath.trim();
                    if (resourcePath.isEmpty()) continue;
                    String relative = resourcePath.substring(RESOURCE_PREFIX.length());
                    Path fileTarget = target.resolve(relative);
                    // Never overwrite - the user may have tweaked their copy.
                    if (Files.exists(fileTarget)) continue;

                    try (InputStream in = BundledPackExtractor.class
                            .getClassLoader()
                            .getResourceAsStream(resourcePath)) {
                        if (in == null) {
                            Constants.LOG.warn("[Orchard] Bundled resource missing from JAR: {}", resourcePath);
                            continue;
                        }
                        Files.createDirectories(fileTarget.getParent());
                        Files.copy(in, fileTarget, StandardCopyOption.REPLACE_EXISTING);
                        count++;
                    }
                }
            }
            Constants.LOG.info("[Orchard] Extracted bundled pack to {} ({} file(s)).", target, count);
        } catch (Exception e) {
            Constants.LOG.error("[Orchard] Failed to extract bundled pack: {}", e.getMessage());
        }
        return target;
    }
}
