package de.minehackers.orchard.config;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.level.levelgen.feature.HugeFungusConfiguration;
import de.minehackers.orchard.Orchard;
import de.minehackers.orchard.matchers.TreeMatchers;

/**
 * Parses {@code fungus_type} JSON fields into predicates that match
 * {@link HugeFungusConfiguration} instances.
 */
final class FungusTypeParser {

    private FungusTypeParser() {}

    @Nullable
    static Predicate<HugeFungusConfiguration> parseFungusType(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return resolveFungusMatcher(element.getAsString());
        }
        if (element.isJsonArray()) {
            List<Predicate<HugeFungusConfiguration>> matchers = new ArrayList<>();
            for (JsonElement e : element.getAsJsonArray()) {
                Predicate<HugeFungusConfiguration> m = parseFungusType(e);
                if (m != null) matchers.add(m);
            }
            if (matchers.isEmpty()) return null;
            if (matchers.size() == 1) return matchers.get(0);
            return config -> matchers.stream().anyMatch(m -> m.test(config));
        }
        return null;
    }

    @Nullable
    static Predicate<HugeFungusConfiguration> resolveFungusMatcher(String name) {
        return switch (name) {
            case "warped" -> TreeMatchers.WARPED_FUNGUS;
            case "crimson" -> TreeMatchers.CRIMSON_FUNGUS;
            case "any" -> TreeMatchers.ANY_FUNGUS;
            default -> {
                Orchard.LOGGER.warn("[Orchard] Unknown fungus_type: {}", name);
                yield null;
            }
        };
    }
}
