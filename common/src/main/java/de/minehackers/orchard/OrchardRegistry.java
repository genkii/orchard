package de.minehackers.orchard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.HugeFungusConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import org.jspecify.annotations.Nullable;

/// Thread-safe global registry for orchard definitions.
/// Definitions are split into tree/fungus/mushroom lists so pickBy* only iterates what it needs.
public final class OrchardRegistry {

    private OrchardRegistry() {}

    private static final float DEFAULT_RARE_POOL_PROBABILITY = 0.025f;
    private static volatile float rarePoolProbability = DEFAULT_RARE_POOL_PROBABILITY;
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();

    private static List<OrchardDefinition> definitions = Collections.emptyList();
    private static List<OrchardDefinition> treeDefs = Collections.emptyList();
    private static List<OrchardDefinition> fungusDefs = Collections.emptyList();
    private static List<OrchardDefinition> mushroomDefs = Collections.emptyList();

    public static void register(OrchardDefinition definition) {
        LOCK.writeLock().lock();
        try {
            var list = new ArrayList<>(definitions);
            list.add(definition);
            apply(list);
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    public static void registerAll(List<OrchardDefinition> newDefs) {
        LOCK.writeLock().lock();
        try {
            var list = new ArrayList<>(definitions);
            list.addAll(newDefs);
            apply(list);
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    public static void clearAndRegisterAll(List<OrchardDefinition> newDefs) {
        LOCK.writeLock().lock();
        try {
            apply(List.copyOf(newDefs));
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    public static void clear() {
        LOCK.writeLock().lock();
        try {
            apply(Collections.emptyList());
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    public static List<OrchardDefinition> getAll() {
        LOCK.readLock().lock();
        try {
            return definitions;
        } finally {
            LOCK.readLock().unlock();
        }
    }

    /// Pick a tree definition matching config, biome, and level.
    @Nullable
    public static OrchardDefinition pickByWorldGen(
            TreeConfiguration config, WorldGenLevel level, Holder<Biome> biome, RandomSource random) {
        List<OrchardDefinition> snapshot;
        LOCK.readLock().lock();
        try {
            snapshot = treeDefs;
        } finally {
            LOCK.readLock().unlock();
        }
        List<OrchardDefinition> pool = null;
        for (OrchardDefinition def : snapshot) {
            if (def.matchesWorldGen(config, level) && def.matchesBiome(biome)) {
                if (pool == null) pool = new ArrayList<>(4);
                pool.add(def);
            }
        }
        return pickWeighted(pool, random);
    }

    /// Pick a fungus definition matching config, biome, and level.
    @Nullable
    public static OrchardDefinition pickByFungusWorldGen(
            HugeFungusConfiguration config, WorldGenLevel level, Holder<Biome> biome, RandomSource random) {
        List<OrchardDefinition> snapshot;
        LOCK.readLock().lock();
        try {
            snapshot = fungusDefs;
        } finally {
            LOCK.readLock().unlock();
        }
        List<OrchardDefinition> pool = null;
        for (OrchardDefinition def : snapshot) {
            if (def.matchesFungusWorldGen(config, level) && def.matchesBiome(biome)) {
                if (pool == null) pool = new ArrayList<>(4);
                pool.add(def);
            }
        }
        return pickWeighted(pool, random);
    }

    /// Pick a mushroom definition matching config, biome, and level.
    @Nullable
    public static OrchardDefinition pickByMushroomWorldGen(
            HugeMushroomFeatureConfiguration config, WorldGenLevel level, Holder<Biome> biome, RandomSource random) {
        List<OrchardDefinition> snapshot;
        LOCK.readLock().lock();
        try {
            snapshot = mushroomDefs;
        } finally {
            LOCK.readLock().unlock();
        }
        List<OrchardDefinition> pool = null;
        for (OrchardDefinition def : snapshot) {
            if (def.matchesMushroomWorldGen(config, level) && def.matchesBiome(biome)) {
                if (pool == null) pool = new ArrayList<>(4);
                pool.add(def);
            }
        }
        return pickWeighted(pool, random);
    }

    /// Weighted random selection with a rare pool gate (2.5% by default).
    @Nullable
    static OrchardDefinition pickWeighted(@Nullable List<OrchardDefinition> pool, RandomSource random) {
        if (pool == null || pool.isEmpty()) return null;
        int size = pool.size();
        if (size == 1) return pool.get(0);

        boolean hasRare = false, hasNormal = false;
        int rareTotalWeight = 0, normalTotalWeight = 0;
        for (int i = 0; i < size; i++) {
            OrchardDefinition def = pool.get(i);
            if (def.isRare()) {
                hasRare = true;
                rareTotalWeight += def.getWeight();
            } else {
                hasNormal = true;
                normalTotalWeight += def.getWeight();
            }
        }

        final boolean useRare =
                hasRare && (!hasNormal || random.nextFloat() < rarePoolProbability);
        int totalWeight = useRare ? rareTotalWeight : normalTotalWeight;

        if (totalWeight <= 0) {
            for (int i = size - 1; i >= 0; i--) {
                if (pool.get(i).isRare() == useRare) return pool.get(i);
            }
            return pool.get(size - 1);
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < size; i++) {
            OrchardDefinition def = pool.get(i);
            if (def.isRare() != useRare) continue;
            cumulative += def.getWeight();
            if (roll < cumulative) return def;
        }

        for (int i = size - 1; i >= 0; i--) {
            if (pool.get(i).isRare() == useRare) return pool.get(i);
        }
        return pool.get(size - 1);
    }

    public static float getRarePoolProbability() {
        return rarePoolProbability;
    }

    public static void setRarePoolProbability(float probability) {
        rarePoolProbability = Math.max(0f, Math.min(1f, probability));
    }

    /// Rebuilds the type-partitioned lists from the full definitions.
    private static void apply(List<OrchardDefinition> newDefs) {
        definitions = newDefs;
        var trees = new ArrayList<OrchardDefinition>();
        var fungi = new ArrayList<OrchardDefinition>();
        var mushrooms = new ArrayList<OrchardDefinition>();
        for (OrchardDefinition def : newDefs) {
            if (def.hasTreeMatcher()) trees.add(def);
            if (def.hasFungusMatcher()) fungi.add(def);
            if (def.hasMushroomMatcher()) mushrooms.add(def);
        }
        treeDefs = Collections.unmodifiableList(trees);
        fungusDefs = Collections.unmodifiableList(fungi);
        mushroomDefs = Collections.unmodifiableList(mushrooms);
    }
}
