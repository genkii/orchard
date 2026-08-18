package de.minehackers.orchard.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import de.minehackers.orchard.Constants;
import de.minehackers.orchard.matchers.BiomeMatchers;

/// Parses biomes JSON fields into biome-matching predicates.
final class BiomeFilterParser {

    private BiomeFilterParser() {}

    @SuppressWarnings("unchecked")
    @Nullable
    public static Predicate<Holder<Biome>> parseBiomeFilter(JsonElement element) {
        if (element.isJsonPrimitive()) {
            String value = element.getAsString();
            if (value.startsWith("#")) {
                String tagName = value.substring(1);
                TagKey<Biome> tag = TagKey.create(
                        Registries.BIOME,
                        Identifier.parse(tagName));
                return BiomeMatchers.hasTag(tag);
            }
            return resolveBiome(value);
        }

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            if (obj.has("any_of")) {
                List<Predicate<Holder<Biome>>> predicates = new ArrayList<>();
                JsonArray arr = obj.getAsJsonArray("any_of");
                for (JsonElement e : arr) {
                    Predicate<Holder<Biome>> p = parseBiomeFilter(e);
                    if (p != null) predicates.add(p);
                }
                if (predicates.isEmpty()) return null;
                return BiomeMatchers.any(predicates.toArray(new Predicate[0]));
            }

            if (obj.has("all_of")) {
                List<Predicate<Holder<Biome>>> predicates = new ArrayList<>();
                JsonArray arr = obj.getAsJsonArray("all_of");
                for (JsonElement e : arr) {
                    Predicate<Holder<Biome>> p = parseBiomeFilter(e);
                    if (p != null) predicates.add(p);
                }
                if (predicates.isEmpty()) return null;
                return BiomeMatchers.all(predicates.toArray(new Predicate[0]));
            }

            if (obj.has("not")) {
                Predicate<Holder<Biome>> inner = parseBiomeFilter(obj.get("not"));
                if (inner != null) return inner.negate();
            }
        }

        if (element.isJsonArray()) {
            List<Predicate<Holder<Biome>>> predicates = new ArrayList<>();
            for (JsonElement e : element.getAsJsonArray()) {
                Predicate<Holder<Biome>> p = parseBiomeFilter(e);
                if (p != null) predicates.add(p);
            }
            if (predicates.isEmpty()) return null;
            return BiomeMatchers.any(predicates.toArray(new Predicate[0]));
        }

        return null;
    }

    @Nullable
    static Predicate<Holder<Biome>> resolveBiome(String name) {
        return switch (name) {
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
            case "lush_caves" -> BiomeMatchers.LUSH_CAVES;
            case "crimson_forest" -> BiomeMatchers.CRIMSON_FOREST;
            case "warped_forest" -> BiomeMatchers.WARPED_FOREST;
            case "nether_wastes" -> BiomeMatchers.NETHER_WASTES;
            case "soul_sand_valley" -> BiomeMatchers.SOUL_SAND_VALLEY;
            case "basalt_deltas" -> BiomeMatchers.BASALT_DELTAS;
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
            default -> {
                Constants.LOG.warn("[Orchard] Unknown biome: {}", name);
                yield null;
            }
        };
    }
}
