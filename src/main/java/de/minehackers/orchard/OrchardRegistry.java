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

/**
 * Thread-safe global store for orchard definitions.
 * <p>
 * Definitions are partitioned at registration time into three lists by feature
 * type (tree, fungus, mushroom) so that each {@code pickBy*} call only iterates
 * the relevant subset rather than the entire list.
 * <p>
 * Selection uses a weighted random algorithm with a rare pool gate:
 * <ol>
 *   <li>If both rare and normal definitions exist, a configurable probability
 *       gate decides which pool to draw from.</li>
 *   <li>Within the chosen pool, a weighted random draw selects one definition.</li>
 * </ol>
 *
 * @see OrchardDefinition
 * @see PlacementIndex
 */
public final class OrchardRegistry {

    private OrchardRegistry() {}

    /** Default probability for entering the rare pool when both pools have entries. */
    private static final float DEFAULT_RARE_POOL_PROBABILITY = 0.025f;

    /** Current rare pool probability (configurable at runtime). */
    private static volatile float rarePoolProbability = DEFAULT_RARE_POOL_PROBABILITY;

    /** Read-write lock protecting the definition lists. */
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();

    /** All registered definitions (unmodifiable). */
    private static List<OrchardDefinition> definitions = Collections.emptyList();

    /** Definitions that have a tree matcher (unmodifiable). */
    private static List<OrchardDefinition> treeDefs = Collections.emptyList();

    /** Definitions that have a fungus matcher (unmodifiable). */
    private static List<OrchardDefinition> fungusDefs = Collections.emptyList();

    /** Definitions that have a mushroom matcher (unmodifiable). */
    private static List<OrchardDefinition> mushroomDefs = Collections.emptyList();

    /**
     * Registers a single definition. Thread-safe.
     *
     * @param definition the definition to add
     */
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

    /**
     * Registers multiple definitions at once. Thread-safe.
     *
     * @param newDefs the definitions to add
     */
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

    /**
     * Replaces all registered definitions. Thread-safe.
     *
     * @param newDefs the new definitions to register
     */
    public static void clearAndRegisterAll(List<OrchardDefinition> newDefs) {
        LOCK.writeLock().lock();
        try {
            apply(List.copyOf(newDefs));
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    /**
     * Removes all registered definitions. Thread-safe.
     */
    public static void clear() {
        LOCK.writeLock().lock();
        try {
            apply(Collections.emptyList());
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    /**
     * Returns all registered definitions. Thread-safe.
     *
     * @return an unmodifiable list of all definitions
     */
    public static List<OrchardDefinition> getAll() {
        LOCK.readLock().lock();
        try {
            return definitions;
        } finally {
            LOCK.readLock().unlock();
        }
    }

    /**
     * Selects a definition matching the given tree configuration and biome.
     * <p>
     * Only iterates definitions that have a tree matcher, skipping fungus
     * and mushroom definitions entirely.
     *
     * @param config the tree configuration to match
     * @param biome  the biome to match
     * @param random the random source for weighted selection
     * @return a matching definition, or {@code null} if none match
     */
    @Nullable
    public static OrchardDefinition pickByWorldGen(
            TreeConfiguration config, Holder<Biome> biome, RandomSource random) {
        List<OrchardDefinition> snapshot;
        LOCK.readLock().lock();
        try {
            snapshot = treeDefs;
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

    /**
     * Selects a definition matching the given fungus configuration and biome.
     * <p>
     * Only iterates definitions that have a fungus matcher.
     *
     * @param config the fungus configuration to match
     * @param biome  the biome to match
     * @param random the random source for weighted selection
     * @return a matching definition, or {@code null} if none match
     */
    @Nullable
    public static OrchardDefinition pickByFungusWorldGen(
            HugeFungusConfiguration config, Holder<Biome> biome, RandomSource random) {
        List<OrchardDefinition> snapshot;
        LOCK.readLock().lock();
        try {
            snapshot = fungusDefs;
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

    /**
     * Selects a definition matching the given mushroom configuration and biome.
     * <p>
     * Only iterates definitions that have a mushroom matcher.
     *
     * @param config the mushroom configuration to match
     * @param biome  the biome to match
     * @param random the random source for weighted selection
     * @return a matching definition, or {@code null} if none match
     */
    @Nullable
    public static OrchardDefinition pickByMushroomWorldGen(
            HugeMushroomFeatureConfiguration config, Holder<Biome> biome, RandomSource random) {
        List<OrchardDefinition> snapshot;
        LOCK.readLock().lock();
        try {
            snapshot = mushroomDefs;
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

    /**
     * Selects a single definition from a pool using weighted random selection.
     * <p>
     * The algorithm works in two phases:
     * <ol>
     *   <li><b>Pool gate:</b> If both rare and normal definitions exist in the
     *       pool, a random float is compared against {@link #rarePoolProbability}
     *       to decide which sub-pool to draw from.</li>
     *   <li><b>Weighted draw:</b> Within the chosen sub-pool, each definition's
     *       {@link OrchardDefinition#getWeight()} determines its probability.</li>
     * </ol>
     * If only one sub-pool has entries, the gate roll is skipped.
     *
     * @param pool   the list of matching definitions, or {@code null}
     * @param random the random source
     * @return the selected definition, or {@code null} if the pool is empty
     */
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

    /**
     * Returns the current rare pool probability.
     *
     * @return the probability (0.0 to 1.0)
     */
    public static float getRarePoolProbability() {
        return rarePoolProbability;
    }

    /**
     * Sets the rare pool probability. Values are clamped to [0.0, 1.0].
     *
     * @param probability the new probability
     */
    public static void setRarePoolProbability(float probability) {
        rarePoolProbability = Math.max(0f, Math.min(1f, probability));
    }

    /**
     * Rebuilds the type-partitioned index from the full definitions list.
     * Must be called under write lock.
     *
     * @param newDefs the new full definitions list
     */
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
