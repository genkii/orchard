package de.minehackers.orchard.config;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import de.minehackers.orchard.Constants;
import de.minehackers.orchard.matchers.TreeMatchers;

/// Parses mushroom_type JSON fields into mushroom-matching predicates.
final class MushroomTypeParser {

    private MushroomTypeParser() {}

    @Nullable
    static BiPredicate<HugeMushroomFeatureConfiguration, WorldGenLevel> parseMushroomType(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return resolveMushroomMatcher(element.getAsString());
        }
        if (element.isJsonArray()) {
            List<BiPredicate<HugeMushroomFeatureConfiguration, WorldGenLevel>> matchers = new ArrayList<>();
            for (JsonElement e : element.getAsJsonArray()) {
                BiPredicate<HugeMushroomFeatureConfiguration, WorldGenLevel> m = parseMushroomType(e);
                if (m != null) matchers.add(m);
            }
            if (matchers.isEmpty()) return null;
            if (matchers.size() == 1) return matchers.get(0);
            return (config, level) -> {
                for (BiPredicate<HugeMushroomFeatureConfiguration, WorldGenLevel> m : matchers) {
                    if (m.test(config, level)) return true;
                }
                return false;
            };
        }
        return null;
    }

    @Nullable
    static BiPredicate<HugeMushroomFeatureConfiguration, WorldGenLevel> resolveMushroomMatcher(String name) {
        return switch (name) {
            case "red" -> TreeMatchers.RED_MUSHROOM;
            case "brown" -> TreeMatchers.BROWN_MUSHROOM;
            case "any" -> TreeMatchers.ANY_MUSHROOM;
            default -> {
                Constants.LOG.warn("[Orchard] Unknown mushroom_type: {}", name);
                yield null;
            }
        };
    }
}
