package de.minehackers.orchard;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import net.minecraft.world.level.levelgen.feature.HugeFungusFeature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import org.jspecify.annotations.Nullable;

/// Describes how one NBT structure file replaces trees, fungi or huge mushrooms,
/// with optional filters for biome, dimension, height and floor. Build one via
/// the fluent builder returned by forNbt().
public final class OrchardDefinition {

    private final String nbtFileName;
    private final Path nbtDirectory;
    private final BiPredicate<TreeFeature, WorldGenLevel> worldGenMatcher;
    private final BiPredicate<HugeFungusFeature, WorldGenLevel> hugeFungusWorldGenMatcher;
    private final BiPredicate<AbstractHugeMushroomFeature, WorldGenLevel> hugeMushroomWorldGenMatcher;
    private final Predicate<Holder<Biome>> biomeMatcher;
    private final Set<ResourceKey<Level>> dimensions;
    private final int minSpacing;
    private final int weight;
    private final boolean rare;
    private final int originYOffset;
    private final int minY;
    private final int maxY;
    private final Predicate<BlockState> validFloor;

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
        this.validFloor = b.validFloor;
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

    @Nullable
    public Predicate<BlockState> getValidFloor() {
        return validFloor;
    }

    public Set<ResourceKey<Level>> getDimensions() {
        return dimensions;
    }

    public boolean hasTreeMatcher() {
        return worldGenMatcher != null;
    }

    public boolean hasFungusMatcher() {
        return hugeFungusWorldGenMatcher != null;
    }

    public boolean hasMushroomMatcher() {
        return hugeMushroomWorldGenMatcher != null;
    }

    public boolean matchesWorldGen(TreeFeature config, WorldGenLevel level) {
        return worldGenMatcher != null && worldGenMatcher.test(config, level);
    }

    public boolean matchesFungusWorldGen(HugeFungusFeature config, WorldGenLevel level) {
        return hugeFungusWorldGenMatcher != null && hugeFungusWorldGenMatcher.test(config, level);
    }

    public boolean matchesMushroomWorldGen(AbstractHugeMushroomFeature config, WorldGenLevel level) {
        return hugeMushroomWorldGenMatcher != null && hugeMushroomWorldGenMatcher.test(config, level);
    }

    /// With no biome filter set, every biome matches.
    public boolean matchesBiome(Holder<Biome> biome) {
        return biomeMatcher == null || biomeMatcher.test(biome);
    }

    /// An empty dimensions set matches everywhere.
    public boolean matchesDimension(ResourceKey<Level> dimensionKey) {
        return dimensions.isEmpty() || dimensions.contains(dimensionKey);
    }

    public boolean hasYRange() {
        return minY != Integer.MIN_VALUE || maxY != Integer.MAX_VALUE;
    }

    /// Unrestricted unless the builder set bounds.
    public boolean matchesYRange(int y) {
        return y >= minY && y <= maxY;
    }

    @Override
    public String toString() {
        return "OrchardDefinition{" + nbtFileName + "}";
    }

    public static Builder forNbt(String nbtFileName, Path nbtDirectory) {
        return new Builder(nbtFileName, nbtDirectory);
    }

    public static final class Builder {

        private final String nbtFileName;
        private final Path nbtDirectory;
        private BiPredicate<TreeFeature, WorldGenLevel> worldGenMatcher;
        private BiPredicate<HugeFungusFeature, WorldGenLevel> hugeFungusWorldGenMatcher;
        private BiPredicate<AbstractHugeMushroomFeature, WorldGenLevel> hugeMushroomWorldGenMatcher;
        private Predicate<Holder<Biome>> biomeMatcher;
        private Set<ResourceKey<Level>> dimensions = Set.of();
        private int minSpacing = 0;
        private int weight = 1;
        private boolean rare = false;
        private int originYOffset = 0;
        private int minY = Integer.MIN_VALUE;
        private int maxY = Integer.MAX_VALUE;
        private Predicate<BlockState> validFloor;

        private Builder(String nbtFileName, Path nbtDirectory) {
            if (nbtFileName == null || nbtFileName.isBlank()) {
                throw new IllegalArgumentException("nbtFileName must not be null or blank");
            }
            this.nbtFileName = nbtFileName.endsWith(".nbt") ? nbtFileName : nbtFileName + ".nbt";
            this.nbtDirectory = nbtDirectory;
        }

        public Builder worldGen(BiPredicate<TreeFeature, WorldGenLevel> matcher) {
            this.worldGenMatcher = matcher;
            return this;
        }

        public Builder fungusWorldGen(BiPredicate<HugeFungusFeature, WorldGenLevel> matcher) {
            this.hugeFungusWorldGenMatcher = matcher;
            return this;
        }

        public Builder mushroomWorldGen(BiPredicate<AbstractHugeMushroomFeature, WorldGenLevel> matcher) {
            this.hugeMushroomWorldGenMatcher = matcher;
            return this;
        }

        public Builder biomes(Predicate<Holder<Biome>> matcher) {
            this.biomeMatcher = matcher;
            return this;
        }

        public Builder dimensions(Set<ResourceKey<Level>> dims) {
            this.dimensions = dims == null ? Set.of() : Set.copyOf(dims);
            return this;
        }

        public Builder minSpacing(int blocks) {
            if (blocks < 0) throw new IllegalArgumentException("minSpacing must be >= 0");
            this.minSpacing = blocks;
            return this;
        }

        public Builder weight(int w) {
            if (w < 1) throw new IllegalArgumentException("weight must be >= 1");
            this.weight = w;
            return this;
        }

        public Builder rare() {
            this.rare = true;
            return this;
        }

        public Builder originYOffset(int offset) {
            this.originYOffset = offset;
            return this;
        }

        public Builder minY(int y) {
            this.minY = y;
            return this;
        }

        public Builder maxY(int y) {
            this.maxY = y;
            return this;
        }

        public Builder validFloor(Predicate<BlockState> predicate) {
            this.validFloor = predicate;
            return this;
        }

        /// Convenience wrapper: dirt-family floor required.
        public Builder onDirt() {
            return validFloor(state -> state.is(BlockTags.DIRT));
        }

        /// Convenience wrapper: nylium floor required.
        public Builder onNylium() {
            return validFloor(state -> state.is(BlockTags.NYLIUM));
        }

        public OrchardDefinition build() {
            return new OrchardDefinition(this);
        }
    }
}
