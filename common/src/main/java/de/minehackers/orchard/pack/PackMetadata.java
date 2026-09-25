package de.minehackers.orchard.pack;

import java.util.Map;
import java.util.Set;
import de.minehackers.orchard.Constants;
import de.minehackers.orchard.config.YamlValues;

/// Metadata from pack.yaml: name, format, version and description.
public final class PackMetadata {

    /// Pack format version supported by this build.
    public static final String SUPPORTED_FORMAT = "0.1";

    private static final Set<String> KNOWN_KEYS = Set.of("name", "format", "version", "description");

    private final String name;
    private final String format;
    private final String version;
    private final String description;

    PackMetadata(String name, String format, String version, String description) {
        this.name = name;
        this.format = format;
        this.version = version;
        this.description = description;
    }

    /// Stand-in metadata for the built-in defaults without a pack.yaml on disk.
    public static PackMetadata bundled() {
        return new PackMetadata("bundled", SUPPORTED_FORMAT, "", "Built-in default trees");
    }

    /// Parses metadata from a decoded YAML map, rejecting malformed or unsupported content.
    public static PackMetadata parse(Map<?, ?> map, String directoryName) {
        for (Object key : map.keySet()) {
            if (!KNOWN_KEYS.contains(String.valueOf(key))) {
                Constants.LOG.warn("[Orchard] Unknown key '{}' in pack.yaml of pack '{}' - ignoring",
                        key, directoryName);
            }
        }

        Object rawName = map.get("name");
        String name = rawName == null ? directoryName : YamlValues.asString(rawName, "pack.yaml 'name'");
        if (name.isBlank()) {
            throw new PackLoadException("pack.yaml 'name' must not be blank");
        }

        Object rawFormat = map.get("format");
        String format = rawFormat == null
                ? SUPPORTED_FORMAT
                : normalizeFormat(YamlValues.asString(rawFormat, "pack.yaml 'format'"));

        String version = map.containsKey("version")
                ? YamlValues.asString(map.get("version"), "pack.yaml 'version'")
                : "";
        String description = map.containsKey("description")
                ? YamlValues.asString(map.get("description"), "pack.yaml 'description'")
                : "";

        return new PackMetadata(name, format, version, description);
    }

    /// Normalizes a raw pack.yaml format value into clean dotted numeric form.
    static String normalizeFormat(String raw) {
        String trimmed = raw.trim();
        try {
            String[] parts = trimmed.split("\\.");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) sb.append('.');
                sb.append(Integer.parseInt(parts[i].trim()));
            }
            return sb.toString();
        } catch (NumberFormatException e) {
            throw new PackLoadException("pack.yaml 'format' must look like '0.1', got: '" + raw + "'");
        }
    }

    /// Returns true when this pack format can be loaded by this build.
    public boolean isFormatSupported() {
        String[] packParts = format.split("\\.");
        String[] supportedParts = SUPPORTED_FORMAT.split("\\.");
        try {
            int packMajor = Integer.parseInt(packParts[0]);
            int supportedMajor = Integer.parseInt(supportedParts[0]);
            if (packMajor != supportedMajor) return false;
            int packMinor = packParts.length > 1 ? Integer.parseInt(packParts[1]) : 0;
            int supportedMinor = supportedParts.length > 1 ? Integer.parseInt(supportedParts[1]) : 0;
            return packMinor <= supportedMinor;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public String name() {
        return name;
    }

    public String format() {
        return format;
    }

    public String version() {
        return version;
    }

    public String description() {
        return description;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name).append(" (format ").append(format);
        if (!version.isBlank()) sb.append(", v").append(version);
        return sb.append(')').toString();
    }

    public String describe() {
        StringBuilder sb = new StringBuilder(toString());
        if (!description.isBlank()) sb.append(" - ").append(description);
        return sb.toString();
    }
}
