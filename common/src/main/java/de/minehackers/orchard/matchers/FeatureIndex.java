package de.minehackers.orchard.matchers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.HugeFungusFeature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.HugeFungusConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import org.jetbrains.annotations.Nullable;

/// Maps configured-feature ids (minecraft:oak etc.) to runtime configurations.
/// The wrinkle: Minecraft never passes the feature id to TreeFeature.place(),
/// only the configuration object - and since the config fully determines
/// placement behaviour, we fingerprint configs and reverse-map them against
/// the configured-feature registry. Packs can therefore override any vanilla
/// or modded tree/fungus/mushroom by id, and ids from mods that aren't
/// installed simply never match. Built once at server start and memoized per
/// instance, so the worldgen hot path performs exactly one hash lookup.
public final class FeatureIndex {

    private FeatureIndex() {}

    /// Configs sharing a fingerprint behave identically, so we treat them as
    /// interchangeable.
    private record Fingerprint(List<String> parts) {}

    /// Identity-based memo key - config classes don't implement equals, so we
    /// can't use them as map keys directly.
    private record IdentityKey(Object config) {
        @Override
        public boolean equals(Object obj) {
            return obj instanceof IdentityKey other && other.config == config;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(config);
        }
    }

    /// Throwaway RandomSource for fingerprinting configs. ThreadLocal because
    /// worldgen runs on worker threads and RandomSource is not thread-safe.
    private static final ThreadLocal<RandomSource> FIXED_RANDOM =
            ThreadLocal.withInitial(() -> RandomSource.create(0L));
    private static final BlockPos ZERO_POS = BlockPos.ZERO;

    private static final Fingerprint UNKNOWN = new Fingerprint(List.of("?"));

    private static final ConcurrentHashMap<IdentityKey, Fingerprint> MEMO = new ConcurrentHashMap<>();

    private static volatile Map<Fingerprint, List<Identifier>> index = Map.of();

    /// Rebuilds the index from current registry data. Safe to re-run: readers
    /// see either the old snapshot or the new one, never something half-built.
    public static void rebuild(net.minecraft.core.HolderLookup.Provider registries,
                               WorldGenLevel level) {
        // Config instances are recreated every server session, so stale memo
        // entries would pile up forever - clear them now while nothing generates.
        MEMO.clear();
        Map<Fingerprint, List<Identifier>> rebuilt = new HashMap<>();
        var registry = registries.lookupOrThrow(Registries.CONFIGURED_FEATURE);

        for (Holder.Reference<ConfiguredFeature<?, ?>> entry : registry.listElements().toList()) {
            ConfiguredFeature<?, ?> feature = entry.value();
            Identifier id = entry.key().identifier();

            Fingerprint fingerprint;
            if (feature.feature() instanceof TreeFeature) {
                fingerprint = fingerprintOf(feature.config(), level);
            } else if (feature.feature() instanceof HugeFungusFeature) {
                fingerprint = fingerprintOf(feature.config(), level);
            } else if (feature.feature() instanceof AbstractHugeMushroomFeature) {
                fingerprint = fingerprintOf(feature.config(), level);
            } else {
                continue;
            }
            if (fingerprint == UNKNOWN) continue;
            rebuilt.computeIfAbsent(fingerprint, f -> new ArrayList<>(2)).add(id);
        }

        Map<Fingerprint, List<Identifier>> frozen = new HashMap<>();
        rebuilt.forEach((fingerprint, ids) -> frozen.put(fingerprint, List.copyOf(ids)));
        index = frozen;
    }

    /// True if the config belongs to the configured feature with that id.
    /// Configs we've never seen never match.
    public static boolean matches(Object config, WorldGenLevel level, Identifier featureId) {
        Fingerprint fingerprint = fingerprintOf(config, level);
        List<Identifier> ids = index.get(fingerprint);
        return ids != null && ids.contains(featureId);
    }

    /// Distinct fingerprints currently indexed - diagnostics only.
    public static int size() {
        return index.size();
    }

    @Nullable
    public static List<Identifier> idsFor(Object config, WorldGenLevel level) {
        return index.get(fingerprintOf(config, level));
    }

    private static Fingerprint fingerprintOf(Object config, WorldGenLevel level) {
        return MEMO.computeIfAbsent(
                new IdentityKey(config), key -> computeFingerprint(key.config(), level));
    }

    private static Fingerprint computeFingerprint(Object config, WorldGenLevel level) {
        if (config instanceof TreeConfiguration tree) {
            return new Fingerprint(List.of(
                    "tree",
                    tree.foliagePlacer.getClass().getName(),
                    tree.trunkPlacer.getClass().getName(),
                    sampledBlock(tree.trunkProvider, level),
                    sampledBlock(tree.foliageProvider, level),
                    String.valueOf(tree.ignoreVines)));
        }
        if (config instanceof HugeFungusConfiguration fungus) {
            return new Fingerprint(List.of(
                    "fungus",
                    fungus.stemState.getBlock().toString(),
                    fungus.hatState.getBlock().toString(),
                    fungus.validBaseState.getBlock().toString()));
        }
        if (config instanceof HugeMushroomFeatureConfiguration mushroom) {
            return new Fingerprint(List.of(
                    "mushroom",
                    sampledBlock(mushroom.capProvider(), level),
                    sampledBlock(mushroom.stemProvider(), level)));
        }
        return UNKNOWN;
    }

    private static String sampledBlock(net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider provider,
                                       WorldGenLevel level) {
        try {
            Block block = provider.getState(level, FIXED_RANDOM.get(), ZERO_POS).getBlock();
            return block.toString();
        } catch (Exception e) {
            return "?";
        }
    }
}
