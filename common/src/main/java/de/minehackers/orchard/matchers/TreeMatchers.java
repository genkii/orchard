package de.minehackers.orchard.matchers;

import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import net.minecraft.world.level.levelgen.feature.HugeFungusFeature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.AcaciaFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BushFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.CherryFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.DarkOakFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FancyFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.MegaJungleFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.MegaPineFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.PineFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.RandomSpreadFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.SpruceFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.DarkOakTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.ForkingTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.GiantTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.MegaJungleTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.UpwardsBranchingTrunkPlacer;

/// Ready-made matchers the config parsers use to recognise specific tree,
/// fungus and mushroom types.
public final class TreeMatchers {

    private TreeMatchers() {}

    /// Throwaway RandomSource for sampling block state providers. Deliberately
    /// NOT the feature's own random: matching must never perturb the world's
    /// random stream. ThreadLocal because worldgen runs on worker threads and
    /// RandomSource is not thread-safe.
    private static final ThreadLocal<RandomSource> FIXED_RANDOM =
            ThreadLocal.withInitial(() -> RandomSource.create(0L));

    // --- Overworld tree matchers ---

    public static final BiPredicate<TreeFeature, WorldGenLevel> OAK = byFoliage(BlobFoliagePlacer.class)
            .and(byTrunkBlock(Blocks.OAK_LOG))
            .and((config, level) -> config.ignoreVines());

    public static final BiPredicate<TreeFeature, WorldGenLevel> FANCY_OAK = byFoliage(FancyFoliagePlacer.class);

    public static final BiPredicate<TreeFeature, WorldGenLevel> BIRCH = byFoliage(BlobFoliagePlacer.class)
            .and(byTrunkBlock(Blocks.BIRCH_LOG));

    /// Every spruce, mega variants included.
    public static final BiPredicate<TreeFeature, WorldGenLevel> SPRUCE = byFoliage(SpruceFoliagePlacer.class);

    /// Regular spruces only, giant mega trunks excluded.
    public static final BiPredicate<TreeFeature, WorldGenLevel> SPRUCE_ONLY = byFoliage(SpruceFoliagePlacer.class)
            .and(byTrunk(GiantTrunkPlacer.class).negate());

    public static final BiPredicate<TreeFeature, WorldGenLevel> PINE = byFoliage(PineFoliagePlacer.class);

    public static final BiPredicate<TreeFeature, WorldGenLevel> MEGA_PINE = byFoliage(MegaPineFoliagePlacer.class);

    public static final BiPredicate<TreeFeature, WorldGenLevel> MEGA_SPRUCE = byTrunk(GiantTrunkPlacer.class)
            .and(byFoliage(SpruceFoliagePlacer.class));

    public static final BiPredicate<TreeFeature, WorldGenLevel> JUNGLE = byTrunkBlock(Blocks.JUNGLE_LOG);

    public static final BiPredicate<TreeFeature, WorldGenLevel> JUNGLE_SMALL = byFoliage(BlobFoliagePlacer.class)
            .and(byTrunkBlock(Blocks.JUNGLE_LOG));

    public static final BiPredicate<TreeFeature, WorldGenLevel> JUNGLE_MEGA = byFoliage(MegaJungleFoliagePlacer.class);

    public static final BiPredicate<TreeFeature, WorldGenLevel> JUNGLE_BUSH = byFoliage(BushFoliagePlacer.class);

    public static final BiPredicate<TreeFeature, WorldGenLevel> ACACIA = byFoliage(AcaciaFoliagePlacer.class);

    public static final BiPredicate<TreeFeature, WorldGenLevel> DARK_OAK = byFoliage(DarkOakFoliagePlacer.class)
            .and(byTrunkBlock(Blocks.DARK_OAK_LOG));

    /// Pale oak, creaking variant included - same trunk and foliage placers.
    public static final BiPredicate<TreeFeature, WorldGenLevel> PALE_OAK = byFoliage(DarkOakFoliagePlacer.class)
            .and(byTrunkBlock(Blocks.PALE_OAK_LOG));

    public static final BiPredicate<TreeFeature, WorldGenLevel> CHERRY = byFoliage(CherryFoliagePlacer.class);

    /// Swamp oak: oak log with blob foliage that keeps its vines.
    public static final BiPredicate<TreeFeature, WorldGenLevel> SWAMP = byFoliage(BlobFoliagePlacer.class)
            .and(byTrunkBlock(Blocks.OAK_LOG))
            .and((config, level) -> !config.ignoreVines());

    public static final BiPredicate<TreeFeature, WorldGenLevel> AZALEA = byFoliage(RandomSpreadFoliagePlacer.class)
            .and(byTrunkBlock(Blocks.OAK_LOG));

    public static final BiPredicate<TreeFeature, WorldGenLevel> MANGROVE = byTrunk(UpwardsBranchingTrunkPlacer.class)
            .and(byTrunkBlock(Blocks.MANGROVE_LOG));

    // --- Nether fungus matchers ---

    public static final BiPredicate<HugeFungusFeature, WorldGenLevel> WARPED_FUNGUS =
            (config, level) -> config.stemState().is(Blocks.WARPED_STEM);

    public static final BiPredicate<HugeFungusFeature, WorldGenLevel> CRIMSON_FUNGUS =
            (config, level) -> config.stemState().is(Blocks.CRIMSON_STEM);

    public static final BiPredicate<HugeFungusFeature, WorldGenLevel> ANY_FUNGUS = (config, level) ->
            config.stemState().is(Blocks.WARPED_STEM) || config.stemState().is(Blocks.CRIMSON_STEM);

    // --- Huge mushroom matchers ---

    public static final BiPredicate<AbstractHugeMushroomFeature, WorldGenLevel> RED_MUSHROOM = (config, level) -> {
        try {
            return config.capProvider().value().getState(level, FIXED_RANDOM.get(), BlockPos.ZERO)
                    .is(Blocks.RED_MUSHROOM_BLOCK);
        } catch (Exception e) {
            return false;
        }
    };

    public static final BiPredicate<AbstractHugeMushroomFeature, WorldGenLevel> BROWN_MUSHROOM = (config, level) -> {
        try {
            return config.capProvider().value().getState(level, FIXED_RANDOM.get(), BlockPos.ZERO)
                    .is(Blocks.BROWN_MUSHROOM_BLOCK);
        } catch (Exception e) {
            return false;
        }
    };

    public static final BiPredicate<AbstractHugeMushroomFeature, WorldGenLevel> ANY_MUSHROOM =
            (config, level) -> RED_MUSHROOM.test(config, level) || BROWN_MUSHROOM.test(config, level);

    // --- Utility combinators ---

    /// Matches when any child matcher does.
    @SafeVarargs
    public static BiPredicate<TreeFeature, WorldGenLevel> any(BiPredicate<TreeFeature, WorldGenLevel>... matchers) {
        return (config, level) -> {
            for (BiPredicate<TreeFeature, WorldGenLevel> m : matchers) {
                if (m.test(config, level)) return true;
            }
            return false;
        };
    }

    public static BiPredicate<TreeFeature, WorldGenLevel> byFoliage(Class<? extends FoliagePlacer> cls) {
        return (config, level) -> cls.isInstance(config.foliagePlacer());
    }

    public static BiPredicate<TreeFeature, WorldGenLevel> byTrunk(Class<? extends TrunkPlacer> cls) {
        return (config, level) -> cls.isInstance(config.trunkPlacer());
    }

    /// Samples the trunk provider and compares against the block. A sampling
    /// error counts as "not a match".
    public static BiPredicate<TreeFeature, WorldGenLevel> byTrunkBlock(Block block) {
        return (config, level) -> {
            try {
                return config.trunkProvider().value().getState(level, FIXED_RANDOM.get(), BlockPos.ZERO).is(block);
            } catch (Exception e) {
                return false;
            }
        };
    }
}
