package de.minehackers.orchard.config;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import de.minehackers.orchard.Orchard;
import de.minehackers.orchard.matchers.TreeMatchers;

/**
 * Parses {@code mushroom_type} JSON fields into predicates that match
 * {@link HugeMushroomFeatureConfiguration} instances.
 */
final class MushroomTypeParser {

    private MushroomTypeParser() {}

    @Nullable
    static Predicate<HugeMushroomFeatureConfiguration> parseMushroomType(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return resolveMushroomMatcher(element.getAsString());
        }
        if (element.isJsonArray()) {
            List<Predicate<HugeMushroomFeatureConfiguration>> matchers = new ArrayList<>();
            for (JsonElement e : element.getAsJsonArray()) {
                Predicate<HugeMushroomFeatureConfiguration> m = parseMushroomType(e);
                if (m != null) matchers.add(m);
            }
            if (matchers.isEmpty()) return null;
            if (matchers.size() == 1) return matchers.get(0);
            return config -> matchers.stream().anyMatch(m -> m.test(config));
        }
        return null;
    }

    @Nullable
    static Predicate<HugeMushroomFeatureConfiguration> resolveMushroomMatcher(String name) {
        return switch (name) {
            case "red" -> TreeMatchers.RED_MUSHROOM;
            case "brown" -> TreeMatchers.BROWN_MUSHROOM;
            case "any" -> TreeMatchers.ANY_MUSHROOM;
            default -> {
                Orchard.LOGGER.warn("[Orchard] Unknown mushroom_type: {}", name);
                yield null;
            }
        };
    }
}
