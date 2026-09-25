package de.minehackers.orchard.pack;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import de.minehackers.orchard.Constants;
import de.minehackers.orchard.config.YamlValues;

/// User settings loaded from config/orchard/orchard.yaml, falling back to defaults on errors.
public final class OrchardSettings {

    /// Sentinel selecting the first valid pack in deterministic order.
    public static final String AUTO = "auto";

    private static final String DEFAULT_FILE = """
            # Orchard configuration
            # Documentation: https://github.com/genkii/orchard

            pack:
              # Which pack to load from config/orchard/packs/.
              #   auto        pick the first valid pack (alphabetical order)
              #   <name>      always load packs/<name>/ - falls back to auto if invalid
              selected: auto

            placement:
              # Fraction of columns allowed to be obstructed before a placement is
              # rejected (0.0 - 1.0). Lower = stricter, fewer trees near structures.
              max_obstructed_fraction: 0.1

            rarity:
              # Chance that the rare pool is rolled when both rare and common
              # candidates match (0.0 - 1.0).
              rare_pool_probability: 0.025
            """;

    private final String selectedPack;
    private final float rarePoolProbability;
    private final double maxObstructedFraction;

    private OrchardSettings(String selectedPack, float rarePoolProbability, double maxObstructedFraction) {
        this.selectedPack = selectedPack;
        this.rarePoolProbability = rarePoolProbability;
        this.maxObstructedFraction = maxObstructedFraction;
    }

    public static OrchardSettings withDefaults() {
        return new OrchardSettings(AUTO, 0.025f, 0.10);
    }

    /// Loads orchard.yaml, writing the default file on first run.
    public static OrchardSettings load(Path orchardDir) {
        Path file = orchardDir.resolve("orchard.yaml");
        OrchardSettings settings = withDefaults();

        if (!Files.exists(file)) {
            try {
                Files.createDirectories(orchardDir);
                Files.writeString(file, DEFAULT_FILE, StandardCharsets.UTF_8);
                Constants.LOG.info("[Orchard] Created default configuration: {}", file);
            } catch (IOException e) {
                Constants.LOG.warn("[Orchard] Could not write default orchard.yaml: {}", e.getMessage());
            }
            return settings;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            LoaderOptions options = new LoaderOptions();
            Object root = new Yaml(new SafeConstructor(options)).load(reader);
            if (root == null) return settings;
            Map<?, ?> map = YamlValues.asMap(root, "orchard.yaml");
            return parse(map, settings);
        } catch (Exception e) {
            Constants.LOG.error("[Orchard] Failed to read orchard.yaml ({}), using defaults.", e.getMessage());
            return settings;
        }
    }

    private static OrchardSettings parse(Map<?, ?> map, OrchardSettings fallback) {
        String selected = fallback.selectedPack;
        float rareProbability = fallback.rarePoolProbability;
        double obstructedFraction = fallback.maxObstructedFraction;

        if (map.containsKey("pack")) {
            Map<?, ?> packMap = YamlValues.asMap(map.get("pack"), "orchard.yaml 'pack'");
            for (Object key : packMap.keySet()) {
                if (!key.equals("selected")) {
                    Constants.LOG.warn("[Orchard] Unknown option 'pack.{}' in orchard.yaml - ignoring", key);
                }
            }
            if (packMap.containsKey("selected")) {
                String value = YamlValues.asString(packMap.get("selected"), "orchard.yaml 'pack.selected'");
                selected = value.isBlank() ? AUTO : value.trim();
            }
        }

        if (map.containsKey("placement")) {
            Map<?, ?> placement = YamlValues.asMap(map.get("placement"), "orchard.yaml 'placement'");
            for (Object key : placement.keySet()) {
                if (!key.equals("max_obstructed_fraction")) {
                    Constants.LOG.warn("[Orchard] Unknown option 'placement.{}' in orchard.yaml - ignoring", key);
                }
            }
            if (placement.containsKey("max_obstructed_fraction")) {
                double value = clamp(YamlValues.asNumber(
                        placement.get("max_obstructed_fraction"),
                        "orchard.yaml 'placement.max_obstructed_fraction'"), 0, 1);
                obstructedFraction = value;
            }
        }

        if (map.containsKey("rarity")) {
            Map<?, ?> rarity = YamlValues.asMap(map.get("rarity"), "orchard.yaml 'rarity'");
            for (Object key : rarity.keySet()) {
                if (!key.equals("rare_pool_probability")) {
                    Constants.LOG.warn("[Orchard] Unknown option 'rarity.{}' in orchard.yaml - ignoring", key);
                }
            }
            if (rarity.containsKey("rare_pool_probability")) {
                double value = clamp(YamlValues.asNumber(
                        rarity.get("rare_pool_probability"),
                        "orchard.yaml 'rarity.rare_pool_probability'"), 0, 1);
                rareProbability = (float) value;
            }
        }

        return new OrchardSettings(selected, rareProbability, obstructedFraction);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /// Selected pack name, either "auto" or an explicit pack directory name.
    public String selectedPack() {
        return selectedPack;
    }

    public boolean isAutoSelect() {
        return AUTO.equalsIgnoreCase(selectedPack);
    }

    @Nullable
    public String explicitPackName() {
        return isAutoSelect() ? null : selectedPack;
    }

    public float rarePoolProbability() {
        return rarePoolProbability;
    }

    public double maxObstructedFraction() {
        return maxObstructedFraction;
    }
}
