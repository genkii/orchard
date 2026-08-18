package de.minehackers.orchard.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.trunkplacers.DarkOakTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.ForkingTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.GiantTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.MegaJungleTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.UpwardsBranchingTrunkPlacer;
import de.minehackers.orchard.Constants;
import de.minehackers.orchard.matchers.TreeMatchers;

/// Parses tree_type JSON fields into tree-matching predicates.
final class TreeTypeParser {

    private TreeTypeParser() {}

    @Nullable
    static BiPredicate<TreeConfiguration, WorldGenLevel> parseTreeType(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return resolveTreeMatcher(element.getAsString());
        }
        if (element.isJsonObject()) {
            return parseTreeTypeObject(element.getAsJsonObject());
        }
        if (element.isJsonArray()) {
            List<BiPredicate<TreeConfiguration, WorldGenLevel>> matchers = new ArrayList<>();
            for (JsonElement e : element.getAsJsonArray()) {
                BiPredicate<TreeConfiguration, WorldGenLevel> m = parseTreeType(e);
                if (m != null) matchers.add(m);
            }
            if (matchers.isEmpty()) return null;
            if (matchers.size() == 1) return matchers.get(0);
            return TreeMatchers.any(matchers.toArray(new BiPredicate[0]));
        }
        return null;
    }

    static BiPredicate<TreeConfiguration, WorldGenLevel> parseTreeTypeObject(JsonObject obj) {
        BiPredicate<TreeConfiguration, WorldGenLevel> result = (config, level) -> true;

        if (obj.has("foliage")) {
            String foliage = obj.get("foliage").getAsString();
            BiPredicate<TreeConfiguration, WorldGenLevel> foliageCheck = resolveFoliageCheck(foliage);
            if (foliageCheck != null) result = result.and(foliageCheck);
        }

        if (obj.has("trunk")) {
            String trunk = obj.get("trunk").getAsString();
            BiPredicate<TreeConfiguration, WorldGenLevel> trunkCheck = resolveTrunkCheck(trunk);
            if (trunkCheck != null) result = result.and(trunkCheck);
        }

        if (obj.has("trunk_block")) {
            String blockId = obj.get("trunk_block").getAsString();
            Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(blockId));
            if (block != null && block != Blocks.AIR) {
                result = result.and(TreeMatchers.byTrunkBlock(block));
            }
        }

        return result;
    }

    @Nullable
    static BiPredicate<TreeConfiguration, WorldGenLevel> resolveTreeMatcher(String name) {
        return switch (name) {
            case "oak" -> TreeMatchers.OAK;
            case "fancy_oak" -> TreeMatchers.FANCY_OAK;
            case "birch" -> TreeMatchers.BIRCH;
            case "spruce" -> TreeMatchers.SPRUCE;
            case "spruce_only" -> TreeMatchers.SPRUCE_ONLY;
            case "pine" -> TreeMatchers.PINE;
            case "mega_pine" -> TreeMatchers.MEGA_PINE;
            case "mega_spruce" -> TreeMatchers.MEGA_SPRUCE;
            case "jungle_small" -> TreeMatchers.JUNGLE_SMALL;
            case "jungle_mega" -> TreeMatchers.JUNGLE_MEGA;
            case "jungle_bush" -> TreeMatchers.JUNGLE_BUSH;
            case "acacia" -> TreeMatchers.ACACIA;
            case "dark_oak" -> TreeMatchers.DARK_OAK;
            case "cherry" -> TreeMatchers.CHERRY;
            case "swamp" -> TreeMatchers.SWAMP;
            case "azalea" -> TreeMatchers.AZALEA;
            case "mangrove" -> TreeMatchers.MANGROVE;
            default -> {
                Constants.LOG.warn("[Orchard] Unknown tree_type: {}", name);
                yield null;
            }
        };
    }

    @Nullable
    static BiPredicate<TreeConfiguration, WorldGenLevel> resolveFoliageCheck(String name) {
        return switch (name) {
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
    }

    @Nullable
    static BiPredicate<TreeConfiguration, WorldGenLevel> resolveTrunkCheck(String name) {
        return switch (name) {
            case "dark_oak" -> TreeMatchers.byTrunk(DarkOakTrunkPlacer.class);
            case "forking" -> TreeMatchers.byTrunk(ForkingTrunkPlacer.class);
            case "giant" -> TreeMatchers.byTrunk(GiantTrunkPlacer.class);
            case "mega_jungle" -> TreeMatchers.byTrunk(MegaJungleTrunkPlacer.class);
            case "upwards_branching" -> TreeMatchers.byTrunk(UpwardsBranchingTrunkPlacer.class);
            default -> null;
        };
    }
}
