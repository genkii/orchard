package de.minehackers.orchard;

import java.nio.file.Path;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.HugeFungusConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public final class OrchardDefinition {

    private final String nbtFileName;
    private final Path nbtDirectory;
    private final Predicate<TreeConfiguration> worldGenMatcher;
    private final Predicate<HugeFungusConfiguration> hugeFungusWorldGenMatcher;
    private final Predicate<HugeMushroomFeatureConfiguration> hugeMushroomWorldGenMatcher;
    private final Predicate<Holder<Biome>> biomeMatcher;
    private final int minSpacing;
    private final int weight;
    private final boolean rare;
    private final int originYOffset;

    private OrchardDefinition(Builder b) {
        this.nbtFileName = b.nbtFileName;
        this.nbtDirectory = b.nbtDirectory;
        this.worldGenMatcher = b.worldGenMatcher;
        this.hugeFungusWorldGenMatcher = b.hugeFungusWorldGenMatcher;
        this.hugeMushroomWorldGenMatcher = b.hugeMushroomWorldGenMatcher;
        this.biomeMatcher = b.biomeMatcher;
        this.minSpacing = b.minSpacing;
        this.weight = b.weight;
        this.rare = b.rare;
        this.originYOffset = b.originYOffset;
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
        private Predicate<TreeConfiguration> worldGenMatcher;
        private Predicate<HugeFungusConfiguration> hugeFungusWorldGenMatcher;
        private Predicate<HugeMushroomFeatureConfiguration> hugeMushroomWorldGenMatcher;
        private Predicate<Holder<Biome>> biomeMatcher;
        private int minSpacing = 0;
        private int weight = 1;
        private boolean rare = false;
        private int originYOffset = 0;

        private Builder(String nbtFileName, Path nbtDirectory) {
            if (nbtFileName == null || nbtFileName.isBlank()) {
                throw new IllegalArgumentException("nbtFileName must not be null or blank");
            }
            this.nbtFileName = nbtFileName.endsWith(".nbt") ? nbtFileName : nbtFileName + ".nbt";
            this.nbtDirectory = nbtDirectory;
        }

        public Builder worldGen(Predicate<TreeConfiguration> matcher) {
            this.worldGenMatcher = matcher;
            return this;
        }

        public Builder fungusWorldGen(Predicate<HugeFungusConfiguration> matcher) {
            this.hugeFungusWorldGenMatcher = matcher;
            return this;
        }

        public Builder mushroomWorldGen(Predicate<HugeMushroomFeatureConfiguration> matcher) {
            this.hugeMushroomWorldGenMatcher = matcher;
            return this;
        }

        public Builder biomes(Predicate<Holder<Biome>> matcher) {
            this.biomeMatcher = matcher;
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

        public OrchardDefinition build() {
            return new OrchardDefinition(this);
        }
    }
}
