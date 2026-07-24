package de.minehackers.orchard.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.trunkplacers.DarkOakTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.ForkingTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.GiantTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.MegaJungleTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.UpwardsBranchingTrunkPlacer;
import de.minehackers.orchard.Orchard;
import de.minehackers.orchard.matchers.TreeMatchers;

/**
 * Parses {@code tree_type} JSON fields into predicates that match
 * {@link TreeConfiguration} instances.
 */
final class TreeTypeParser {

    private TreeTypeParser() {}

    @Nullable
    static Predicate<TreeConfiguration> parseTreeType(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return resolveTreeMatcher(element.getAsString());
        }
        if (element.isJsonObject()) {
            return parseTreeTypeObject(element.getAsJsonObject());
        }
        if (element.isJsonArray()) {
            List<Predicate<TreeConfiguration>> matchers = new ArrayList<>();
            for (JsonElement e : element.getAsJsonArray()) {
                Predicate<TreeConfiguration> m = parseTreeType(e);
                if (m != null) matchers.add(m);
            }
            if (matchers.isEmpty()) return null;
            if (matchers.size() == 1) return matchers.get(0);
            return TreeMatchers.any(matchers.toArray(new Predicate[0]));
        }
        return null;
    }

    static Predicate<TreeConfiguration> parseTreeTypeObject(JsonObject obj) {
        Predicate<TreeConfiguration> result = config -> true;

        if (obj.has("foliage")) {
            String foliage = obj.get("foliage").getAsString();
            Predicate<TreeConfiguration> foliageCheck = resolveFoliageCheck(foliage);
            if (foliageCheck != null) result = result.and(foliageCheck);
        }

        if (obj.has("trunk")) {
            String trunk = obj.get("trunk").getAsString();
            Predicate<TreeConfiguration> trunkCheck = resolveTrunkCheck(trunk);
            if (trunkCheck != null) result = result.and(trunkCheck);
        }

        if (obj.has("trunk_block")) {
            String blockId = obj.get("trunk_block").getAsString();
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockId));
            if (block != null && block != Blocks.AIR) {
                result = result.and(TreeMatchers.byTrunkBlock(block));
            }
        }

        return result;
    }

    @Nullable
    static Predicate<TreeConfiguration> resolveTreeMatcher(String name) {
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
                Orchard.LOGGER.warn("[Orchard] Unknown tree_type: {}", name);
                yield null;
            }
        };
    }

    @Nullable
    static Predicate<TreeConfiguration> resolveFoliageCheck(String name) {
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
    static Predicate<TreeConfiguration> resolveTrunkCheck(String name) {
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
