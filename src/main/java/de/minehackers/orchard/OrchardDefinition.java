package de.minehackers.orchard;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.HugeFungusConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

/**
 * An immutable definition that maps an NBT structure file to one or more vanilla
 * tree, fungus, or mushroom types with optional biome, dimension, and Y-range filtering.
 * <p>
 * Instances are created via the {@link Builder} pattern:
 * <pre>{@code
 * OrchardDefinition def = OrchardDefinition.forNbt("my_tree", nbtDir)
 *     .worldGen(TreeMatchers.OAK)
 *     .biomes(BiomeMatchers.IS_FOREST)
 *     .dimensions(Set.of(Level.OVERWORLD))
 *     .weight(3)
 *     .minSpacing(8)
 *     .build();
 * }</pre>
 *
 * @see OrchardRegistry
 * @see Builder
 */
public final class OrchardDefinition {

    private final String nbtFileName;
    private final Path nbtDirectory;
    private final Predicate<TreeConfiguration> worldGenMatcher;
    private final Predicate<HugeFungusConfiguration> hugeFungusWorldGenMatcher;
    private final Predicate<HugeMushroomFeatureConfiguration> hugeMushroomWorldGenMatcher;
    private final Predicate<Holder<Biome>> biomeMatcher;
    private final Set<ResourceKey<Level>> dimensions;
    private final int minSpacing;
    private final int weight;
    private final boolean rare;
    private final int originYOffset;
    private final int minY;
    private final int maxY;

    private OrchardDefinition(Builder b) {
        this.nbtFileName = b.nbtFileName;
        this.nbtDirectory = b.nbtDirectory;
        this.worldGenMatcher = b.worldGenMatcher;
        this.hugeFungusWorldGenMatcher = b.hugeFungusWorldGenMatcher;
        this.hugeMushroomWorldGenMatcher = b.hugeMushroomWorldGenMatcher;
        this.biomeMatcher = b.biomeMatcher;
        this.dimensions = b.dimensions;
        this.minSpacing = b.minSpacing;
        this.weight = b.weight;
        this.rare = b.rare;
        this.originYOffset = b.originYOffset;
        this.minY = b.minY;
        this.maxY = b.maxY;
    }

    public String getNbtFileName() {
        return nbtFileName;
    }

    public Path getNbtDirectory() {
        return nbtDirectory;
    }

    public int getMinSpacing() {
        return minSpacing;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isRare() {
        return rare;
    }

    public int getOriginYOffset() {
        return originYOffset;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    /**
     * Returns the set of dimension keys this definition applies to.
     * An empty set means the definition applies to all dimensions.
     *
     * @return an unmodifiable set of dimension keys, or empty for all dimensions
     */
    public Set<ResourceKey<Level>> getDimensions() {
        return dimensions;
    }

    /**
     * Tests whether this definition has a tree configuration matcher.
     *
     * @return {@code true} if a tree matcher is set
     */
    public boolean hasTreeMatcher() {
        return worldGenMatcher != null;
    }

    /**
     * Tests whether this definition has a fungus configuration matcher.
     *
     * @return {@code true} if a fungus matcher is set
     */
    public boolean hasFungusMatcher() {
        return hugeFungusWorldGenMatcher != null;
    }

    /**
     * Tests whether this definition has a mushroom configuration matcher.
     *
     * @return {@code true} if a mushroom matcher is set
     */
    public boolean hasMushroomMatcher() {
        return hugeMushroomWorldGenMatcher != null;
    }

    public boolean matchesWorldGen(TreeConfiguration config) {
        return worldGenMatcher != null && worldGenMatcher.test(config);
    }

    public boolean matchesFungusWorldGen(HugeFungusConfiguration config) {
        return hugeFungusWorldGenMatcher != null && hugeFungusWorldGenMatcher.test(config);
    }

    public boolean matchesMushroomWorldGen(HugeMushroomFeatureConfiguration config) {
        return hugeMushroomWorldGenMatcher != null && hugeMushroomWorldGenMatcher.test(config);
    }

    public boolean matchesBiome(Holder<Biome> biome) {
        return biomeMatcher == null || biomeMatcher.test(biome);
    }

    /**
     * Tests whether this definition matches the given dimension.
     * <p>
     * If no dimension filter is set (empty set), all dimensions match.
     *
     * @param dimensionKey the dimension key to test (e.g. {@link Level#OVERWORLD})
     * @return {@code true} if the definition applies to this dimension
     */
    public boolean matchesDimension(ResourceKey<Level> dimensionKey) {
        return dimensions.isEmpty() || dimensions.contains(dimensionKey);
    }

    /**
     * Tests whether the given Y coordinate falls within this definition's
     * allowed Y range. If both minY and maxY are zero (the defaults),
     * all Y values match.
     *
     * @param y the Y coordinate to test
     * @return {@code true} if the Y coordinate is within range
     */
    public boolean matchesYRange(int y) {
        if (minY == 0 && maxY == 0) return true;
        return y >= minY && y <= maxY;
    }

    @Override
    public String toString() {
        return "OrchardDefinition{" + nbtFileName + "}";
    }

    public static Builder forNbt(String nbtFileName, Path nbtDirectory) {
        return new Builder(nbtFileName, nbtDirectory);
    }

    /**
     * Builder for {@link OrchardDefinition}. All fields have sensible defaults.
     * <p>
     * Required: {@link #forNbt(String, Path)} to start building.
     * At least one of {@link #worldGen}, {@link #fungusWorldGen}, or
     * {@link #mushroomWorldGen} must be set for the definition to match anything.
     */
    public static final class Builder {

        private final String nbtFileName;
        private final Path nbtDirectory;
        private Predicate<TreeConfiguration> worldGenMatcher;
        private Predicate<HugeFungusConfiguration> hugeFungusWorldGenMatcher;
        private Predicate<HugeMushroomFeatureConfiguration> hugeMushroomWorldGenMatcher;
        private Predicate<Holder<Biome>> biomeMatcher;
        private Set<ResourceKey<Level>> dimensions = Set.of();
        private int minSpacing = 0;
        private int weight = 1;
        private boolean rare = false;
        private int originYOffset = 0;
        private int minY = 0;
        private int maxY = 0;

        private Builder(String nbtFileName, Path nbtDirectory) {
            if (nbtFileName == null || nbtFileName.isBlank()) {
                throw new IllegalArgumentException("nbtFileName must not be null or blank");
            }
            this.nbtFileName = nbtFileName.endsWith(".nbt") ? nbtFileName : nbtFileName + ".nbt";
            this.nbtDirectory = nbtDirectory;
        }

        /**
         * Sets the tree configuration matcher.
         *
         * @param matcher predicate to test {@link TreeConfiguration}
         * @return this builder
         */
        public Builder worldGen(Predicate<TreeConfiguration> matcher) {
            this.worldGenMatcher = matcher;
            return this;
        }

        /**
         * Sets the fungus configuration matcher.
         *
         * @param matcher predicate to test {@link HugeFungusConfiguration}
         * @return this builder
         */
        public Builder fungusWorldGen(Predicate<HugeFungusConfiguration> matcher) {
            this.hugeFungusWorldGenMatcher = matcher;
            return this;
        }

        /**
         * Sets the mushroom configuration matcher.
         *
         * @param matcher predicate to test {@link HugeMushroomFeatureConfiguration}
         * @return this builder
         */
        public Builder mushroomWorldGen(Predicate<HugeMushroomFeatureConfiguration> matcher) {
            this.hugeMushroomWorldGenMatcher = matcher;
            return this;
        }

        /**
         * Sets the biome filter.
         *
         * @param matcher predicate to test {@link Biome} holders
         * @return this builder
         */
        public Builder biomes(Predicate<Holder<Biome>> matcher) {
            this.biomeMatcher = matcher;
            return this;
        }

        /**
         * Sets the dimension filter. An empty set means all dimensions.
         *
         * @param dims the set of dimension keys this definition applies to
         * @return this builder
         */
        public Builder dimensions(Set<ResourceKey<Level>> dims) {
            this.dimensions = dims == null ? Set.of() : Set.copyOf(dims);
            return this;
        }

        /**
         * Sets the minimum block radius between two placements of this type.
         *
         * @param blocks the minimum spacing in blocks (must be >= 0)
         * @return this builder
         * @throws IllegalArgumentException if blocks is negative
         */
        public Builder minSpacing(int blocks) {
            if (blocks < 0) throw new IllegalArgumentException("minSpacing must be >= 0");
            this.minSpacing = blocks;
            return this;
        }

        /**
         * Sets the relative selection weight.
         *
         * @param w the weight (must be >= 1)
         * @return this builder
         * @throws IllegalArgumentException if w is less than 1
         */
        public Builder weight(int w) {
            if (w < 1) throw new IllegalArgumentException("weight must be >= 1");
            this.weight = w;
            return this;
        }

        /**
         * Marks this definition as rare (placed in the rare pool).
         *
         * @return this builder
         */
        public Builder rare() {
            this.rare = true;
            return this;
        }

        /**
         * Sets the Y offset applied to the placement origin.
         * Negative values shift the structure underground (for roots).
         *
         * @param offset the Y offset in blocks
         * @return this builder
         */
        public Builder originYOffset(int offset) {
            this.originYOffset = offset;
            return this;
        }

        /**
         * Sets the minimum Y coordinate for placement.
         * Used together with {@link #maxY(int)} to restrict placement to a Y range.
         * Both default to 0, meaning no restriction.
         *
         * @param y the minimum Y coordinate
         * @return this builder
         */
        public Builder minY(int y) {
            this.minY = y;
            return this;
        }

        /**
         * Sets the maximum Y coordinate for placement.
         * Used together with {@link #minY(int)} to restrict placement to a Y range.
         * Both default to 0, meaning no restriction.
         *
         * @param y the maximum Y coordinate
         * @return this builder
         */
        public Builder maxY(int y) {
            this.maxY = y;
            return this;
        }

        /**
         * Builds the immutable {@link OrchardDefinition}.
         *
         * @return the definition
         */
        public OrchardDefinition build() {
            return new OrchardDefinition(this);
        }
    }
}
