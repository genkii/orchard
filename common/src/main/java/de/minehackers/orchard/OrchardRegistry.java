package de.minehackers.orchard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.StampedLock;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import net.minecraft.world.level.levelgen.feature.HugeFungusFeature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import org.jspecify.annotations.Nullable;

/// Global registry of orchard definitions, safe to hit from multiple threads.
/// Reads take the StampedLock optimistic path so worldgen pays essentially
/// nothing, and definitions are partitioned by type so the pick methods only
/// ever scan what they need.
public final class OrchardRegistry {

    private OrchardRegistry() {}

    private static final float DEFAULT_RARE_POOL_PROBABILITY = 0.025f;
    private static volatile float rarePoolProbability = DEFAULT_RARE_POOL_PROBABILITY;
    private static final StampedLock LOCK = new StampedLock();

    private static volatile List<OrchardDefinition> definitions = Collections.emptyList();
    private static volatile List<OrchardDefinition> treeDefs = Collections.emptyList();
    private static volatile List<OrchardDefinition> fungusDefs = Collections.emptyList();
    private static volatile List<OrchardDefinition> mushroomDefs = Collections.emptyList();

    // Per-thread candidate list, reused between pick calls to dodge allocations.
    private static final ThreadLocal<List<OrchardDefinition>> POOL = ThreadLocal.withInitial(() -> new ArrayList<>(4));

    public static void register(OrchardDefinition definition) {
        long stamp = LOCK.writeLock();
        try {
            var list = new ArrayList<>(definitions);
            list.add(definition);
            apply(list);
        } finally {
            LOCK.unlockWrite(stamp);
        }
    }

    public static void registerAll(List<OrchardDefinition> newDefs) {
        long stamp = LOCK.writeLock();
        try {
            var list = new ArrayList<>(definitions);
            list.addAll(newDefs);
            apply(list);
        } finally {
            LOCK.unlockWrite(stamp);
        }
    }

    public static void clearAndRegisterAll(List<OrchardDefinition> newDefs) {
        long stamp = LOCK.writeLock();
        try {
            apply(List.copyOf(newDefs));
        } finally {
            LOCK.unlockWrite(stamp);
        }
    }

    public static void clear() {
        long stamp = LOCK.writeLock();
        try {
            apply(Collections.emptyList());
        } finally {
            LOCK.unlockWrite(stamp);
        }
    }

    public static List<OrchardDefinition> getAll() {
        long stamp = LOCK.tryOptimisticRead();
        List<OrchardDefinition> result = definitions;
        if (!LOCK.validate(stamp)) {
            stamp = LOCK.readLock();
            try {
                result = definitions;
            } finally {
                LOCK.unlockRead(stamp);
            }
        }
        return result;
    }

    @Nullable
    public static OrchardDefinition pickByWorldGen(
            TreeFeature config, WorldGenLevel level, Holder<Biome> biome, RandomSource random) {
        List<OrchardDefinition> snapshot = treeDefs;
        List<OrchardDefinition> pool = POOL.get();
        pool.clear();
        for (OrchardDefinition def : snapshot) {
            if (def.matchesWorldGen(config, level) && def.matchesBiome(biome)) {
                pool.add(def);
            }
        }
        return pickWeighted(pool, random);
    }

    @Nullable
    public static OrchardDefinition pickByFungusWorldGen(
            HugeFungusFeature config, WorldGenLevel level, Holder<Biome> biome, RandomSource random) {
        List<OrchardDefinition> snapshot = fungusDefs;
        List<OrchardDefinition> pool = POOL.get();
        pool.clear();
        for (OrchardDefinition def : snapshot) {
            if (def.matchesFungusWorldGen(config, level) && def.matchesBiome(biome)) {
                pool.add(def);
            }
        }
        return pickWeighted(pool, random);
    }

    @Nullable
    public static OrchardDefinition pickByMushroomWorldGen(
            AbstractHugeMushroomFeature config, WorldGenLevel level, Holder<Biome> biome, RandomSource random) {
        List<OrchardDefinition> snapshot = mushroomDefs;
        List<OrchardDefinition> pool = POOL.get();
        pool.clear();
        for (OrchardDefinition def : snapshot) {
            if (def.matchesMushroomWorldGen(config, level) && def.matchesBiome(biome)) {
                pool.add(def);
            }
        }
        return pickWeighted(pool, random);
    }

    /// Weighted random pick. Rare defs only enter through a small side pool
    /// (2.5% chance), so marking one rare really keeps it rare even next to
    /// common candidates - and even when it is the only candidate at all.
    @Nullable
    static OrchardDefinition pickWeighted(List<OrchardDefinition> pool, RandomSource random) {
        int size = pool.size();
        if (size == 0) return null;

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

        boolean rollWon = random.nextFloat() < rarePoolProbability;
        if (!hasRare) {
            return pickFrom(pool, random, normalTotalWeight, false);
        }
        if (hasNormal) {
            return rollWon
                    ? pickFrom(pool, random, rareTotalWeight, true)
                    : pickFrom(pool, random, normalTotalWeight, false);
        }
        // Only rare candidates: the probability gate still applies, and a
        // lost roll means vanilla worldgen proceeds untouched.
        return rollWon ? pickFrom(pool, random, rareTotalWeight, true) : null;
    }

    @Nullable
    private static OrchardDefinition pickFrom(
            List<OrchardDefinition> pool, RandomSource random, int totalWeight, boolean rare) {
        int size = pool.size();

        // Weights are validated >= 1 at parse time; this is just belt and braces.
        if (totalWeight <= 0) {
            for (int i = size - 1; i >= 0; i--) {
                if (pool.get(i).isRare() == rare) return pool.get(i);
            }
            return null;
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < size; i++) {
            OrchardDefinition def = pool.get(i);
            if (def.isRare() != rare) continue;
            cumulative += def.getWeight();
            if (roll < cumulative) return def;
        }

        for (int i = size - 1; i >= 0; i--) {
            if (pool.get(i).isRare() == rare) return pool.get(i);
        }
        return null;
    }

    public static float getRarePoolProbability() {
        return rarePoolProbability;
    }

    public static void setRarePoolProbability(float probability) {
        rarePoolProbability = Math.max(0f, Math.min(1f, probability));
    }

    /// Rebuilds the per-type partitions from the full definition list.
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
