package de.minehackers.orchard.pack;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/// Identifiers a pack points at that live in dynamic registries - biomes,
/// dimensions, features. None of these can be checked at load
/// time, so validation happens once the server's registries exist. Unknown
/// ids only warn instead of failing the pack, which is what makes it safe
/// to reference optional modded content.
public final class DynamicReferences {

    public static final DynamicReferences EMPTY =
            new DynamicReferences(Set.of(), Set.of(), Set.of());

    private final Set<Identifier> biomes;
    private final Set<ResourceKey<Level>> dimensions;
    private final Set<Identifier> features;

    private DynamicReferences(Set<Identifier> biomes,
                              Set<ResourceKey<Level>> dimensions,
                              Set<Identifier> features) {
        this.biomes = biomes;
        this.dimensions = dimensions;
        this.features = features;
    }

    /// Mutable collector the parsers fill in while compiling a pack.
    public static final class Builder {

        private final Set<Identifier> biomes = new HashSet<>();
        private final Set<ResourceKey<Level>> dimensions = new HashSet<>();
        private final Set<Identifier> features = new HashSet<>();

        public void addBiome(Identifier id) {
            biomes.add(id);
        }

        public void addDimension(ResourceKey<Level> key) {
            dimensions.add(key);
        }

        public void addFeature(Identifier id) {
            features.add(id);
        }

        public boolean isEmpty() {
            return biomes.isEmpty() && dimensions.isEmpty() && features.isEmpty();
        }

        public void merge(Builder other) {
            biomes.addAll(other.biomes);
            dimensions.addAll(other.dimensions);
            features.addAll(other.features);
        }

        @org.jetbrains.annotations.Nullable
        public DynamicReferences build() {
            if (isEmpty()) return null;
            return new DynamicReferences(
                    Set.copyOf(biomes), Set.copyOf(dimensions), Set.copyOf(features));
        }
    }

    public Set<Identifier> biomes() {
        return biomes;
    }

    public Set<ResourceKey<Level>> dimensions() {
        return dimensions;
    }

    public Set<Identifier> features() {
        return features;
    }

    public boolean isEmpty() {
        return biomes.isEmpty() && dimensions.isEmpty() && features.isEmpty();
    }
}
