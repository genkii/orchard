package de.minehackers.orchard.config;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/// Format-independent snapshot of one tree/fungus/mushroom replacement exactly
/// as parsed from a pack file. The selector fields still hold the raw nodes
/// (String/Map/List) as authored; DefinitionCompiler turns them into runtime
/// matchers later, keeping parsing, validation and resolution separate stages.
public record RawDefinition(
        String nbt,
        @Nullable Object treeType,
        @Nullable Object fungusType,
        @Nullable Object mushroomType,
        @Nullable Object biomes,
        @Nullable List<String> dimensions,
        int weight,
        int minSpacing,
        boolean rare,
        int originYOffset,
        int minY,
        int maxY,
        @Nullable String validFloor) {

    public static final int DEFAULT_WEIGHT = 1;

    public boolean hasTreeSelector() {
        return treeType != null;
    }

    public boolean hasFungusSelector() {
        return fungusType != null;
    }

    public boolean hasMushroomSelector() {
        return mushroomType != null;
    }

    public boolean hasAnySelector() {
        return treeType != null || fungusType != null || mushroomType != null;
    }

    public boolean hasYRange() {
        return minY != 0 || maxY != 0;
    }
}
