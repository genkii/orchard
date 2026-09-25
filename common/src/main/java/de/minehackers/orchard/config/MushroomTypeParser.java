package de.minehackers.orchard.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import org.jetbrains.annotations.Nullable;
import de.minehackers.orchard.matchers.FeatureIndex;
import de.minehackers.orchard.matchers.TreeMatchers;
import de.minehackers.orchard.pack.DynamicReferences;
import de.minehackers.orchard.pack.PackLoadException;

/// Compiles mushroom_type selectors into mushroom-matching predicates.
final class MushroomTypeParser {

    private MushroomTypeParser() {}

    static BiPredicate<AbstractHugeMushroomFeature, WorldGenLevel> parse(
            Object node, @Nullable DynamicReferences.Builder refs, String where) {
        if (node instanceof String s) {
            return resolveName(s.trim(), refs, where);
        }
        if (node instanceof List<?> list) {
            List<BiPredicate<AbstractHugeMushroomFeature, WorldGenLevel>> matchers =
                    new ArrayList<>(list.size());
            for (int i = 0; i < list.size(); i++) {
                Object element = list.get(i);
                if (element == null) continue;
                matchers.add(parse(element, refs, where + "[" + i + "]"));
            }
            if (matchers.isEmpty()) {
                throw new PackLoadException(where + ": mushroom_type list must not be empty");
            }
            if (matchers.size() == 1) return matchers.get(0);
            return anyOf(matchers);
        }
        throw new PackLoadException(where + ": mushroom_type must be text or a list");
    }

    private static BiPredicate<AbstractHugeMushroomFeature, WorldGenLevel> resolveName(
            String name, @Nullable DynamicReferences.Builder refs, String where) {
        if (name.indexOf(':') >= 0) {
            Identifier id = TreeTypeParser.parseIdentifier(name, where);
            if (refs != null) refs.addFeature(id);
            return (config, level) -> FeatureIndex.matches(config, level, id);
        }
        BiPredicate<AbstractHugeMushroomFeature, WorldGenLevel> matcher = switch (name) {
            case "red" -> TreeMatchers.RED_MUSHROOM;
            case "brown" -> TreeMatchers.BROWN_MUSHROOM;
            case "any" -> TreeMatchers.ANY_MUSHROOM;
            default -> null;
        };
        if (matcher == null) {
            throw new PackLoadException(where + ": unknown mushroom_type '" + name
                    + "' - use red, brown, any, or a feature id");
        }
        return matcher;
    }

    private static BiPredicate<AbstractHugeMushroomFeature, WorldGenLevel> anyOf(
            List<BiPredicate<AbstractHugeMushroomFeature, WorldGenLevel>> matchers) {
        return (config, level) -> {
            for (BiPredicate<AbstractHugeMushroomFeature, WorldGenLevel> m : matchers) {
                if (m.test(config, level)) return true;
            }
            return false;
        };
    }
}
