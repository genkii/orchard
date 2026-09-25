package de.minehackers.orchard.config;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import de.minehackers.orchard.Constants;
import de.minehackers.orchard.pack.PackLoadException;

/// Reads pack data files (.yaml) into raw definitions.
public final class DataFileParser {

    private static final Set<String> KNOWN_KEYS = Set.of(
            "nbt", "tree_type", "fungus_type", "mushroom_type", "biomes",
            "dimensions", "weight", "min_spacing", "rare", "origin_y_offset",
            "min_y", "max_y", "valid_floor");

    private static final int MAX_FILE_SIZE = 2 * 1024 * 1024;

    private DataFileParser() {}

    /// Reads one data file from disk and decodes it into raw definitions.
    public static List<RawDefinition> parseFile(Path file) {
        String context = file.getFileName().toString();
        try {
            long size = Files.size(file);
            if (size == 0) {
                Constants.LOG.warn("[Orchard] Skipping empty data file: {}", file);
                return List.of();
            }
            if (size > MAX_FILE_SIZE) {
                throw new PackLoadException(context + ": file too large (" + size + " bytes)");
            }
        } catch (PackLoadException e) {
            throw e;
        } catch (Exception e) {
            throw new PackLoadException(context + ": cannot read file: " + e.getMessage(), e);
        }

        Object root;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            LoaderOptions options = new LoaderOptions();
            options.setMaxAliasesForCollections(50);
            Yaml yaml = new Yaml(new SafeConstructor(options));
            root = yaml.load(reader);
        } catch (PackLoadException e) {
            throw e;
        } catch (Exception e) {
            throw new PackLoadException(context + ": invalid YAML - " + e.getMessage(), e);
        }

        return fromRoot(root, context);
    }

    /// Converts a decoded YAML document into raw definitions.
    public static List<RawDefinition> fromRoot(Object root, String context) {
        List<RawDefinition> out = new ArrayList<>();
        if (root instanceof Map<?, ?> single) {
            out.add(fromMap(single, context));
            return out;
        }
        if (root instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                Object element = list.get(i);
                if (element == null) continue;
                out.add(fromMap(YamlValues.asMap(element, entryContext(context, i)),
                        entryContext(context, i)));
            }
            return out;
        }
        if (root == null) {
            return out; // null root, i.e. an empty file
        }
        throw new PackLoadException(
                context + ": expected a definition or a list of definitions, got "
                + YamlValues.describe(root));
    }

    private static String entryContext(String context, int index) {
        return context + " entry #" + (index + 1);
    }

    /// Builds one raw definition from a single YAML mapping.
    public static RawDefinition fromMap(Map<?, ?> map, String where) {
        for (Object key : map.keySet()) {
            if (!KNOWN_KEYS.contains(String.valueOf(key))) {
                Constants.LOG.warn("[Orchard] {} : unknown field '{}' - ignoring",
                        where, key);
            }
        }

        if (!map.containsKey("nbt")) {
            throw new PackLoadException(where + ": missing required field 'nbt'");
        }
        String nbt = YamlValues.asString(map.get("nbt"), where + " field 'nbt'");

        int weight = map.containsKey("weight")
                ? YamlValues.asInt(map.get("weight"), where + " field 'weight'")
                : RawDefinition.DEFAULT_WEIGHT;
        if (weight < 1) {
            throw new PackLoadException(where + " field 'weight': must be >= 1");
        }

        int minSpacing = map.containsKey("min_spacing")
                ? YamlValues.asInt(map.get("min_spacing"), where + " field 'min_spacing'")
                : 0;
        if (minSpacing < 0) {
            throw new PackLoadException(where + " field 'min_spacing': must be >= 0");
        }

        boolean rare = map.containsKey("rare")
                ? YamlValues.asBool(map.get("rare"), where + " field 'rare'")
                : false;

        int originYOffset = map.containsKey("origin_y_offset")
                ? YamlValues.asInt(map.get("origin_y_offset"), where + " field 'origin_y_offset'")
                : 0;

        boolean hasMinY = map.containsKey("min_y");
        boolean hasMaxY = map.containsKey("max_y");
        int minY = hasMinY
                ? YamlValues.asInt(map.get("min_y"), where + " field 'min_y'")
                : Integer.MIN_VALUE;
        int maxY = hasMaxY
                ? YamlValues.asInt(map.get("max_y"), where + " field 'max_y'")
                : Integer.MAX_VALUE;
        if (hasMinY && hasMaxY && maxY < minY) {
            throw new PackLoadException(where + ": invalid Y range [min_y=" + minY
                    + ", max_y=" + maxY + "] - max_y must not be smaller than min_y");
        }

        List<String> dimensions = null;
        if (map.containsKey("dimensions")) {
            dimensions = YamlValues.stringList(map.get("dimensions"), where + " field 'dimensions'");
        }

        String validFloor = null;
        if (map.containsKey("valid_floor")) {
            validFloor = validateFloor(YamlValues.asString(map.get("valid_floor"),
                    where + " field 'valid_floor'"));
        }

        return new RawDefinition(
                nbt,
                map.get("tree_type"),
                map.get("fungus_type"),
                map.get("mushroom_type"),
                map.get("biomes"),
                dimensions,
                weight,
                minSpacing,
                rare,
                originYOffset,
                minY,
                maxY,
                validFloor);
    }

    private static String validateFloor(String floor) {
        return switch (floor) {
            case "dirt", "nylium" -> floor;
            default -> throw new PackLoadException(
                    "field 'valid_floor': unknown value '" + floor + "' (expected 'dirt' or 'nylium')");
        };
    }
}
