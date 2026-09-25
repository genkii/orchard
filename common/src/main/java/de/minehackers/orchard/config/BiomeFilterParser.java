package de.minehackers.orchard.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;
import de.minehackers.orchard.matchers.BiomeMatchers;
import de.minehackers.orchard.pack.DynamicReferences;
import de.minehackers.orchard.pack.PackLoadException;

/// Compiles biome selectors into biome-matching predicates.
final class BiomeFilterParser {

    private BiomeFilterParser() {}

    static Predicate<Holder<Biome>> parse(Object node, @Nullable DynamicReferences.Builder refs, String where) {
        if (node instanceof String s) {
            return resolveName(s.trim(), refs, where);
        }
        if (node instanceof Map<?, ?> map) {
            return parseObject(map, refs, where);
        }
        if (node instanceof List<?> list) {
            List<Predicate<Holder<Biome>>> predicates = new ArrayList<>(list.size());
            for (int i = 0; i < list.size(); i++) {
                Object element = list.get(i);
                if (element == null) continue;
                predicates.add(parse(element, refs, where + "[" + i + "]"));
            }
            if (predicates.isEmpty()) {
                throw new PackLoadException(where + ": biomes list must not be empty");
            }
            return BiomeMatchers.any(predicates.toArray(new Predicate[0]));
        }
        throw new PackLoadException(where + ": biomes must be text, a mapping, or a list");
    }

    private static Predicate<Holder<Biome>> parseObject(Map<?, ?> map, @Nullable DynamicReferences.Builder refs, String where) {
        if (map.size() != 1) {
            throw new PackLoadException(where
                    + ": biome combinator mapping must have exactly one of 'any_of', 'all_of' or 'not'");
        }
        Map.Entry<?, ?> entry = map.entrySet().iterator().next();
        String key = String.valueOf(entry.getKey());

        return switch (key) {
            case "any_of" -> anyOf(entry.getValue(), refs, where);
            case "all_of" -> allOf(entry.getValue(), refs, where);
            case "not" -> {
                Predicate<Holder<Biome>> inner =
                        parse(requireChild(entry.getValue(), "'not'"), refs, where + ".not");
                yield inner.negate();
            }
            default -> throw new PackLoadException(where + ": unknown biome filter '" + key
                    + "' (expected 'any_of', 'all_of' or 'not')");
        };
    }

    private static Object requireChild(Object value, String field) {
        if (value == null) {
            throw new PackLoadException("biomes " + field + " requires a value");
        }
        return value;
    }

    private static Predicate<Holder<Biome>> anyOf(Object value, @Nullable DynamicReferences.Builder refs, String where) {
        List<?> raw = YamlValues.asList(value, where + " 'any_of'");
        List<Predicate<Holder<Biome>>> predicates = new ArrayList<>(raw.size());
        for (int i = 0; i < raw.size(); i++) {
            Object element = raw.get(i);
            if (element == null) continue;
            predicates.add(parse(element, refs, where + ".any_of[" + i + "]"));
        }
        if (predicates.isEmpty()) {
            throw new PackLoadException(where + ": 'any_of' must not be empty");
        }
        return predicates.size() == 1 ? predicates.get(0)
                : BiomeMatchers.any(predicates.toArray(new Predicate[0]));
    }

    private static Predicate<Holder<Biome>> allOf(Object value, @Nullable DynamicReferences.Builder refs, String where) {
        List<?> raw = YamlValues.asList(value, where + " 'all_of'");
        List<Predicate<Holder<Biome>>> predicates = new ArrayList<>(raw.size());
        for (int i = 0; i < raw.size(); i++) {
            Object element = raw.get(i);
            if (element == null) continue;
            predicates.add(parse(element, refs, where + ".all_of[" + i + "]"));
        }
        if (predicates.isEmpty()) {
            throw new PackLoadException(where + ": 'all_of' must not be empty");
        }
        return predicates.size() == 1 ? predicates.get(0)
                : BiomeMatchers.all(predicates.toArray(new Predicate[0]));
    }

    /// Handles the string form: shorthand names, #tags, and full biome ids.
    static Predicate<Holder<Biome>> resolveName(String name, @Nullable DynamicReferences.Builder refs, String where) {
        if (name.startsWith("#")) {
            return resolveTag(name.substring(1), refs, where);
        }
        if (name.indexOf(':') >= 0) {
            Identifier id = TreeTypeParser.parseIdentifier(name, where);
            if (refs != null) refs.addBiome(id);
            ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, id);
            return biome -> biome.is(key);
        }

        Predicate<Holder<Biome>> matcher = switch (name) {
            case "plains" -> BiomeMatchers.PLAINS;
            case "sunflower_plains" -> BiomeMatchers.SUNFLOWER_PLAINS;
            case "meadow" -> BiomeMatchers.MEADOW;
            case "snowy_plains" -> BiomeMatchers.SNOWY_PLAINS;
            case "forest" -> BiomeMatchers.FOREST;
            case "flower_forest" -> BiomeMatchers.FLOWER_FOREST;
            case "birch_forest" -> BiomeMatchers.BIRCH_FOREST;
            case "old_growth_birch_forest" -> BiomeMatchers.OLD_GROWTH_BIRCH_FOREST;
            case "dark_forest" -> BiomeMatchers.DARK_FOREST;
            case "windswept_forest" -> BiomeMatchers.WINDSWEPT_FOREST;
            case "taiga" -> BiomeMatchers.TAIGA;
            case "snowy_taiga" -> BiomeMatchers.SNOWY_TAIGA;
            case "old_growth_pine_taiga" -> BiomeMatchers.OLD_GROWTH_PINE_TAIGA;
            case "old_growth_spruce_taiga" -> BiomeMatchers.OLD_GROWTH_SPRUCE_TAIGA;
            case "jungle" -> BiomeMatchers.JUNGLE;
            case "sparse_jungle" -> BiomeMatchers.SPARSE_JUNGLE;
            case "bamboo_jungle" -> BiomeMatchers.BAMBOO_JUNGLE;
            case "savanna" -> BiomeMatchers.SAVANNA;
            case "savanna_plateau" -> BiomeMatchers.SAVANNA_PLATEAU;
            case "windswept_savanna" -> BiomeMatchers.WINDSWEPT_SAVANNA;
            case "windswept_hills" -> BiomeMatchers.WINDSWEPT_HILLS;
            case "windswept_gravelly_hills" -> BiomeMatchers.WINDSWEPT_GRAVELLY_HILLS;
            case "grove" -> BiomeMatchers.GROVE;
            case "swamp" -> BiomeMatchers.SWAMP;
            case "mangrove_swamp" -> BiomeMatchers.MANGROVE_SWAMP;
            case "cherry_grove" -> BiomeMatchers.CHERRY_GROVE;
            case "mushroom_fields" -> BiomeMatchers.MUSHROOM_FIELDS;
            case "pale_garden" -> BiomeMatchers.PALE_GARDEN;
            case "dappled_forest" -> BiomeMatchers.DAPPLED_FOREST;
            case "lush_caves" -> BiomeMatchers.LUSH_CAVES;
            case "crimson_forest" -> BiomeMatchers.CRIMSON_FOREST;
            case "warped_forest" -> BiomeMatchers.WARPED_FOREST;
            case "nether_wastes" -> BiomeMatchers.NETHER_WASTES;
            case "soul_sand_valley" -> BiomeMatchers.SOUL_SAND_VALLEY;
            case "basalt_deltas" -> BiomeMatchers.BASALT_DELTAS;
            default -> null;
        };
        if (matcher != null) return matcher;

        // Group aliases like is_forest - backed by tags or combined matchers.
        Predicate<Holder<Biome>> group = switch (name) {
            case "is_forest" -> BiomeMatchers.IS_FOREST;
            case "is_taiga" -> BiomeMatchers.IS_TAIGA;
            case "is_jungle" -> BiomeMatchers.IS_JUNGLE;
            case "is_savanna" -> BiomeMatchers.IS_SAVANNA;
            case "is_badlands" -> BiomeMatchers.IS_BADLANDS;
            case "is_ocean" -> BiomeMatchers.IS_OCEAN;
            case "is_river" -> BiomeMatchers.IS_RIVER;
            case "is_beach" -> BiomeMatchers.IS_BEACH;
            case "is_overworld" -> BiomeMatchers.IS_OVERWORLD;
            case "is_nether" -> BiomeMatchers.IS_NETHER;
            case "is_end" -> BiomeMatchers.IS_END;
            case "snowy_spruce_biomes" -> BiomeMatchers.SNOWY_SPRUCE_BIOMES;
            case "non_snowy_taiga" -> BiomeMatchers.NON_SNOWY_TAIGA;
            case "pine_biomes" -> BiomeMatchers.PINE_BIOMES;
            default -> null;
        };
        if (group != null) return group;

        throw new PackLoadException(where + ": unknown biome '" + name
                + "' - use a built-in name, '#tag', or a full id like 'minecraft:plains'");
    }

    private static Predicate<Holder<Biome>> resolveTag(String tagName, @Nullable DynamicReferences.Builder refs, String where) {
        Identifier id;
        if (tagName.indexOf(':') >= 0) {
            id = TreeTypeParser.parseIdentifier(tagName, where);
        } else {
            // shorthands like #is_forest mean the minecraft namespace
            id = Identifier.withDefaultNamespace(tagName);
        }
        TagKey<Biome> tag = TagKey.create(Registries.BIOME, id);
        return biome -> biome.is(tag);
    }
}
