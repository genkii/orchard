package de.minehackers.orchard.matchers;

import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public final class BiomeMatchers {

    private BiomeMatchers() {}

    public static final Predicate<Holder<Biome>> IS_FOREST = hasTag(BiomeTags.IS_FOREST);
    public static final Predicate<Holder<Biome>> IS_TAIGA = hasTag(BiomeTags.IS_TAIGA);
    public static final Predicate<Holder<Biome>> IS_JUNGLE = hasTag(BiomeTags.IS_JUNGLE);
    public static final Predicate<Holder<Biome>> IS_SAVANNA = hasTag(BiomeTags.IS_SAVANNA);
    public static final Predicate<Holder<Biome>> IS_BADLANDS = hasTag(BiomeTags.IS_BADLANDS);
    public static final Predicate<Holder<Biome>> IS_OCEAN = hasTag(BiomeTags.IS_OCEAN);
    public static final Predicate<Holder<Biome>> IS_RIVER = hasTag(BiomeTags.IS_RIVER);
    public static final Predicate<Holder<Biome>> IS_BEACH = hasTag(BiomeTags.IS_BEACH);
    public static final Predicate<Holder<Biome>> IS_OVERWORLD = hasTag(BiomeTags.IS_OVERWORLD);
    public static final Predicate<Holder<Biome>> IS_NETHER = hasTag(BiomeTags.IS_NETHER);
    public static final Predicate<Holder<Biome>> IS_END = hasTag(BiomeTags.IS_END);

    public static final Predicate<Holder<Biome>> PLAINS = is(Biomes.PLAINS);
    public static final Predicate<Holder<Biome>> SUNFLOWER_PLAINS = is(Biomes.SUNFLOWER_PLAINS);
    public static final Predicate<Holder<Biome>> MEADOW = is(Biomes.MEADOW);
    public static final Predicate<Holder<Biome>> SNOWY_PLAINS = is(Biomes.SNOWY_PLAINS);
    public static final Predicate<Holder<Biome>> FOREST = is(Biomes.FOREST);
    public static final Predicate<Holder<Biome>> FLOWER_FOREST = is(Biomes.FLOWER_FOREST);
    public static final Predicate<Holder<Biome>> BIRCH_FOREST = is(Biomes.BIRCH_FOREST);
    public static final Predicate<Holder<Biome>> OLD_GROWTH_BIRCH_FOREST = is(Biomes.OLD_GROWTH_BIRCH_FOREST);
    public static final Predicate<Holder<Biome>> DARK_FOREST = is(Biomes.DARK_FOREST);
    public static final Predicate<Holder<Biome>> WINDSWEPT_FOREST = is(Biomes.WINDSWEPT_FOREST);
    public static final Predicate<Holder<Biome>> TAIGA = is(Biomes.TAIGA);
    public static final Predicate<Holder<Biome>> SNOWY_TAIGA = is(Biomes.SNOWY_TAIGA);
    public static final Predicate<Holder<Biome>> OLD_GROWTH_PINE_TAIGA = is(Biomes.OLD_GROWTH_PINE_TAIGA);
    public static final Predicate<Holder<Biome>> OLD_GROWTH_SPRUCE_TAIGA = is(Biomes.OLD_GROWTH_SPRUCE_TAIGA);
    public static final Predicate<Holder<Biome>> JUNGLE = is(Biomes.JUNGLE);
    public static final Predicate<Holder<Biome>> SPARSE_JUNGLE = is(Biomes.SPARSE_JUNGLE);
    public static final Predicate<Holder<Biome>> BAMBOO_JUNGLE = is(Biomes.BAMBOO_JUNGLE);
    public static final Predicate<Holder<Biome>> SAVANNA = is(Biomes.SAVANNA);
    public static final Predicate<Holder<Biome>> SAVANNA_PLATEAU = is(Biomes.SAVANNA_PLATEAU);
    public static final Predicate<Holder<Biome>> WINDSWEPT_SAVANNA = is(Biomes.WINDSWEPT_SAVANNA);
    public static final Predicate<Holder<Biome>> WINDSWEPT_HILLS = is(Biomes.WINDSWEPT_HILLS);
    public static final Predicate<Holder<Biome>> WINDSWEPT_GRAVELLY_HILLS = is(Biomes.WINDSWEPT_GRAVELLY_HILLS);
    public static final Predicate<Holder<Biome>> GROVE = is(Biomes.GROVE);
    public static final Predicate<Holder<Biome>> SWAMP = is(Biomes.SWAMP);
    public static final Predicate<Holder<Biome>> MANGROVE_SWAMP = is(Biomes.MANGROVE_SWAMP);
    public static final Predicate<Holder<Biome>> CHERRY_GROVE = is(Biomes.CHERRY_GROVE);
    public static final Predicate<Holder<Biome>> MUSHROOM_FIELDS = is(Biomes.MUSHROOM_FIELDS);
    public static final Predicate<Holder<Biome>> LUSH_CAVES = is(Biomes.LUSH_CAVES);
    public static final Predicate<Holder<Biome>> CRIMSON_FOREST = is(Biomes.CRIMSON_FOREST);
    public static final Predicate<Holder<Biome>> WARPED_FOREST = is(Biomes.WARPED_FOREST);
    public static final Predicate<Holder<Biome>> NETHER_WASTES = is(Biomes.NETHER_WASTES);
    public static final Predicate<Holder<Biome>> SOUL_SAND_VALLEY = is(Biomes.SOUL_SAND_VALLEY);
    public static final Predicate<Holder<Biome>> BASALT_DELTAS = is(Biomes.BASALT_DELTAS);

    public static final Predicate<Holder<Biome>> SNOWY_SPRUCE_BIOMES = any(
            SNOWY_TAIGA, SNOWY_PLAINS, WINDSWEPT_HILLS,
            WINDSWEPT_FOREST, WINDSWEPT_GRAVELLY_HILLS, GROVE);

    public static final Predicate<Holder<Biome>> NON_SNOWY_TAIGA = any(
            TAIGA, OLD_GROWTH_PINE_TAIGA, OLD_GROWTH_SPRUCE_TAIGA);

    public static final Predicate<Holder<Biome>> PINE_BIOMES = any(IS_TAIGA, GROVE);

    @SafeVarargs
    public static Predicate<Holder<Biome>> any(Predicate<Holder<Biome>>... matchers) {
        return biome -> {
            for (Predicate<Holder<Biome>> m : matchers) {
                if (m.test(biome)) return true;
            }
            return false;
        };
    }

    @SafeVarargs
    public static Predicate<Holder<Biome>> all(Predicate<Holder<Biome>>... matchers) {
        return biome -> {
            for (Predicate<Holder<Biome>> m : matchers) {
                if (!m.test(biome)) return false;
            }
            return true;
        };
    }

    public static Predicate<Holder<Biome>> is(ResourceKey<Biome> key) {
        return biome -> biome.is(key);
    }

    public static Predicate<Holder<Biome>> hasTag(TagKey<Biome> tag) {
        return biome -> biome.is(tag);
    }
}
