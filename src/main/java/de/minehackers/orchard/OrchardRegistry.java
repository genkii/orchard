package de.minehackers.orchard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.HugeFungusConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public final class OrchardRegistry {

    private OrchardRegistry() {}

    public static final float RARE_POOL_PROBABILITY = 0.025f;

    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();

    private static List<OrchardDefinition> definitions = Collections.emptyList();

    public static void register(OrchardDefinition definition) {
        LOCK.writeLock().lock();
        try {
            var list = new ArrayList<>(definitions);
            list.add(definition);
            definitions = Collections.unmodifiableList(list);
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    public static void registerAll(List<OrchardDefinition> newDefs) {
        LOCK.writeLock().lock();
        try {
            var list = new ArrayList<>(definitions);
            list.addAll(newDefs);
            definitions = Collections.unmodifiableList(list);
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    public static void clearAndRegisterAll(List<OrchardDefinition> newDefs) {
        LOCK.writeLock().lock();
        try {
            definitions = List.copyOf(newDefs);
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    public static void clear() {
        LOCK.writeLock().lock();
        try {
            definitions = Collections.emptyList();
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

    @Nullable
    public static OrchardDefinition pickByWorldGen(
            TreeConfiguration config, Holder<Biome> biome, RandomSource random) {
        List<OrchardDefinition> snapshot;
        LOCK.readLock().lock();
        try {
            snapshot = definitions;
        } finally {
            LOCK.readLock().unlock();
        }
        List<OrchardDefinition> pool = null;
        for (OrchardDefinition def : snapshot) {
            if (def.matchesWorldGen(config) && def.matchesBiome(biome)) {
                if (pool == null) pool = new ArrayList<>(4);
                pool.add(def);
            }
        }
        return pickWeighted(pool, random);
    }

    @Nullable
    public static OrchardDefinition pickByFungusWorldGen(
            HugeFungusConfiguration config, Holder<Biome> biome, RandomSource random) {
        List<OrchardDefinition> snapshot;
        LOCK.readLock().lock();
        try {
            snapshot = definitions;
        } finally {
            LOCK.readLock().unlock();
        }
        List<OrchardDefinition> pool = null;
        for (OrchardDefinition def : snapshot) {
            if (def.matchesFungusWorldGen(config) && def.matchesBiome(biome)) {
                if (pool == null) pool = new ArrayList<>(4);
                pool.add(def);
            }
        }
        return pickWeighted(pool, random);
    }

    @Nullable
    public static OrchardDefinition pickByMushroomWorldGen(
            HugeMushroomFeatureConfiguration config, Holder<Biome> biome, RandomSource random) {
        List<OrchardDefinition> snapshot;
        LOCK.readLock().lock();
        try {
            snapshot = definitions;
        } finally {
            LOCK.readLock().unlock();
        }
        List<OrchardDefinition> pool = null;
        for (OrchardDefinition def : snapshot) {
            if (def.matchesMushroomWorldGen(config) && def.matchesBiome(biome)) {
                if (pool == null) pool = new ArrayList<>(4);
                pool.add(def);
            }
        }
        return pickWeighted(pool, random);
    }

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
                hasRare && (!hasNormal || random.nextFloat() < RARE_POOL_PROBABILITY);
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
}
