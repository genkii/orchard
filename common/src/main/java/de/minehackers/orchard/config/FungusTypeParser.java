package de.minehackers.orchard.config;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.HugeFungusConfiguration;
import de.minehackers.orchard.Constants;
import de.minehackers.orchard.matchers.TreeMatchers;

/// Parses fungus_type JSON fields into fungus-matching predicates.
final class FungusTypeParser {

    private FungusTypeParser() {}

    @Nullable
    static BiPredicate<HugeFungusConfiguration, WorldGenLevel> parseFungusType(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return resolveFungusMatcher(element.getAsString());
        }
        if (element.isJsonArray()) {
            List<BiPredicate<HugeFungusConfiguration, WorldGenLevel>> matchers = new ArrayList<>();
            for (JsonElement e : element.getAsJsonArray()) {
                BiPredicate<HugeFungusConfiguration, WorldGenLevel> m = parseFungusType(e);
                if (m != null) matchers.add(m);
            }
            if (matchers.isEmpty()) return null;
            if (matchers.size() == 1) return matchers.get(0);
            return (config, level) -> {
                for (BiPredicate<HugeFungusConfiguration, WorldGenLevel> m : matchers) {
                    if (m.test(config, level)) return true;
                }
                return false;
            };
        }
        return null;
    }

    @Nullable
    static BiPredicate<HugeFungusConfiguration, WorldGenLevel> resolveFungusMatcher(String name) {
        return switch (name) {
            case "warped" -> TreeMatchers.WARPED_FUNGUS;
            case "crimson" -> TreeMatchers.CRIMSON_FUNGUS;
            case "any" -> TreeMatchers.ANY_FUNGUS;
            default -> {
                Constants.LOG.warn("[Orchard] Unknown fungus_type: {}", name);
                yield null;
            }
        };
    }
}
