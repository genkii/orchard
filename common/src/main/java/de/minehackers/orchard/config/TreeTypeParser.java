package de.minehackers.orchard.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.trunkplacers.DarkOakTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.ForkingTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.GiantTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.MegaJungleTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.UpwardsBranchingTrunkPlacer;
import org.jetbrains.annotations.Nullable;
import de.minehackers.orchard.matchers.FeatureIndex;
import de.minehackers.orchard.matchers.TreeMatchers;
import de.minehackers.orchard.pack.DynamicReferences;
import de.minehackers.orchard.pack.PackLoadException;

/// Compiles tree_type selectors into tree-matching predicates.
final class TreeTypeParser {

    private TreeTypeParser() {}

    static BiPredicate<TreeFeature, WorldGenLevel> parse(
            Object node, @Nullable DynamicReferences.Builder refs, String where) {
        if (node instanceof String s) {
            return resolveName(s.trim(), refs, where);
        }
        if (node instanceof Map<?, ?> map) {
            return parseObject(map, where);
        }
        if (node instanceof List<?> list) {
            List<BiPredicate<TreeFeature, WorldGenLevel>> matchers = new ArrayList<>(list.size());
            for (int i = 0; i < list.size(); i++) {
                Object element = list.get(i);
                if (element == null) continue;
                matchers.add(parse(element, refs, where + "[" + i + "]"));
            }
            if (matchers.isEmpty()) {
                throw new PackLoadException(where + ": tree_type list must not be empty");
            }
            if (matchers.size() == 1) return matchers.get(0);
            return or(matchers);
        }
        throw new PackLoadException(where + ": tree_type must be text, a mapping, or a list");
    }

    private static BiPredicate<TreeFeature, WorldGenLevel> parseObject(Map<?, ?> map, String where) {
        BiPredicate<TreeFeature, WorldGenLevel> result = null;

        for (Object keyObj : map.keySet()) {
            String key = String.valueOf(keyObj);
            if (!key.equals("foliage") && !key.equals("trunk") && !key.equals("trunk_block")) {
                throw new PackLoadException(where + ": unknown tree_type filter '" + key
                        + "' (expected 'foliage', 'trunk' or 'trunk_block')");
            }
        }

        if (map.containsKey("foliage")) {
            String name = YamlValues.asString(map.get("foliage"), where + " field 'foliage'");
            result = combine(result, resolveFoliage(name, where));
        }
        if (map.containsKey("trunk")) {
            String name = YamlValues.asString(map.get("trunk"), where + " field 'trunk'");
            result = combine(result, resolveTrunk(name, where));
        }
        if (map.containsKey("trunk_block")) {
            String id = YamlValues.asString(map.get("trunk_block"), where + " field 'trunk_block'");
            result = combine(result, resolveBlock(id, where));
        }

        if (result == null) {
            throw new PackLoadException(where
                    + ": tree_type mapping needs at least one of 'foliage', 'trunk', 'trunk_block'");
        }
        return result;
    }

    /// Resolves shorthand names to matchers and namespaced names to feature ids.
    static BiPredicate<TreeFeature, WorldGenLevel> resolveName(
            String name, @Nullable DynamicReferences.Builder refs, String where) {
        if (name.indexOf(':') >= 0) {
            Identifier id = parseIdentifier(name, where);
            if (refs != null) refs.addFeature(id);
            return (config, level) -> FeatureIndex.matches(config, level, id);
        }
        BiPredicate<TreeFeature, WorldGenLevel> matcher = switch (name) {
            case "oak" -> TreeMatchers.OAK;
            case "fancy_oak" -> TreeMatchers.FANCY_OAK;
            case "birch" -> TreeMatchers.BIRCH;
            case "spruce" -> TreeMatchers.SPRUCE;
            case "spruce_only" -> TreeMatchers.SPRUCE_ONLY;
            case "pine" -> TreeMatchers.PINE;
            case "mega_pine" -> TreeMatchers.MEGA_PINE;
            case "mega_spruce" -> TreeMatchers.MEGA_SPRUCE;
            case "jungle" -> TreeMatchers.JUNGLE;
            case "jungle_small" -> TreeMatchers.JUNGLE_SMALL;
            case "jungle_mega" -> TreeMatchers.JUNGLE_MEGA;
            case "jungle_bush" -> TreeMatchers.JUNGLE_BUSH;
            case "acacia" -> TreeMatchers.ACACIA;
            case "dark_oak" -> TreeMatchers.DARK_OAK;
            case "pale_oak" -> TreeMatchers.PALE_OAK;
            case "cherry" -> TreeMatchers.CHERRY;
            case "swamp" -> TreeMatchers.SWAMP;
            case "azalea" -> TreeMatchers.AZALEA;
            case "mangrove" -> TreeMatchers.MANGROVE;
            default -> null;
        };
        if (matcher == null) {
            throw new PackLoadException(where + ": unknown tree_type '" + name
                    + "' - use a built-in name, a feature id like 'minecraft:oak', "
                    + "or a filter mapping");
        }
        return matcher;
    }

    static BiPredicate<TreeFeature, WorldGenLevel> resolveFoliage(String name, String where) {
        BiPredicate<TreeFeature, WorldGenLevel> matcher = switch (name) {
            case "blob" -> TreeMatchers.byFoliage(
                    net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer.class);
            case "fancy" -> TreeMatchers.byFoliage(
                    net.minecraft.world.level.levelgen.feature.foliageplacers.FancyFoliagePlacer.class);
            case "spruce" -> TreeMatchers.byFoliage(
                    net.minecraft.world.level.levelgen.feature.foliageplacers.SpruceFoliagePlacer.class);
            case "pine" -> TreeMatchers.byFoliage(
                    net.minecraft.world.level.levelgen.feature.foliageplacers.PineFoliagePlacer.class);
            case "mega_pine" -> TreeMatchers.byFoliage(
                    net.minecraft.world.level.levelgen.feature.foliageplacers.MegaPineFoliagePlacer.class);
            case "mega_jungle" -> TreeMatchers.byFoliage(
                    net.minecraft.world.level.levelgen.feature.foliageplacers.MegaJungleFoliagePlacer.class);
            case "bush" -> TreeMatchers.byFoliage(
                    net.minecraft.world.level.levelgen.feature.foliageplacers.BushFoliagePlacer.class);
            case "acacia" -> TreeMatchers.byFoliage(
                    net.minecraft.world.level.levelgen.feature.foliageplacers.AcaciaFoliagePlacer.class);
            case "dark_oak" -> TreeMatchers.byFoliage(
                    net.minecraft.world.level.levelgen.feature.foliageplacers.DarkOakFoliagePlacer.class);
            case "cherry" -> TreeMatchers.byFoliage(
                    net.minecraft.world.level.levelgen.feature.foliageplacers.CherryFoliagePlacer.class);
            case "random_spread" -> TreeMatchers.byFoliage(
                    net.minecraft.world.level.levelgen.feature.foliageplacers.RandomSpreadFoliagePlacer.class);
            default -> null;
        };
        if (matcher == null) {
            throw new PackLoadException(where + ": unknown foliage placer '" + name + "'");
        }
        return matcher;
    }

    static BiPredicate<TreeFeature, WorldGenLevel> resolveTrunk(String name, String where) {
        BiPredicate<TreeFeature, WorldGenLevel> matcher = switch (name) {
            case "dark_oak" -> TreeMatchers.byTrunk(DarkOakTrunkPlacer.class);
            case "forking" -> TreeMatchers.byTrunk(ForkingTrunkPlacer.class);
            case "giant" -> TreeMatchers.byTrunk(GiantTrunkPlacer.class);
            case "mega_jungle" -> TreeMatchers.byTrunk(MegaJungleTrunkPlacer.class);
            case "upwards_branching" -> TreeMatchers.byTrunk(UpwardsBranchingTrunkPlacer.class);
            default -> null;
        };
        if (matcher == null) {
            throw new PackLoadException(where + ": unknown trunk placer '" + name + "'");
        }
        return matcher;
    }

    /// Looks the block up in the registry right away, then matches on it.
    static BiPredicate<TreeFeature, WorldGenLevel> resolveBlock(String id, String where) {
        Block block = resolveBlockNow(id, where);
        return TreeMatchers.byTrunkBlock(block);
    }

    /// Eager registry lookup for pack loading; air counts as unknown here.
    static Block resolveBlockNow(String id, String where) {
        Identifier blockId = parseIdentifier(id, where);
        Block block = BuiltInRegistries.BLOCK.getValue(blockId);
        if (block == null || block == Blocks.AIR) {
            throw new PackLoadException(where + ": unknown block '" + id + "'");
        }
        return block;
    }

    static Identifier parseIdentifier(String raw, String where) {
        try {
            Identifier id = Identifier.parse(raw);
            if (id.getPath().isEmpty()) throw new IllegalArgumentException("empty path");
            return id;
        } catch (Exception e) {
            throw new PackLoadException(where + ": invalid identifier '" + raw + "'");
        }
    }

    private static BiPredicate<TreeFeature, WorldGenLevel> combine(
            @Nullable BiPredicate<TreeFeature, WorldGenLevel> a,
            BiPredicate<TreeFeature, WorldGenLevel> b) {
        return a == null ? b : a.and(b);
    }

    private static BiPredicate<TreeFeature, WorldGenLevel> or(
            List<BiPredicate<TreeFeature, WorldGenLevel>> matchers) {
        return (config, level) -> {
            for (BiPredicate<TreeFeature, WorldGenLevel> m : matchers) {
                if (m.test(config, level)) return true;
            }
            return false;
        };
    }

}
