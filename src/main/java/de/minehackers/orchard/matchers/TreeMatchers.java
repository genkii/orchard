package de.minehackers.orchard.matchers;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.HugeFungusConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
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

public final class TreeMatchers {

    private TreeMatchers() {}

    private static final RandomSource FIXED_RANDOM = RandomSource.create(0L);

    public static final Predicate<TreeConfiguration> OAK = byFoliage(BlobFoliagePlacer.class)
            .and(byTrunkBlock(Blocks.OAK_LOG))
            .and(config -> config.ignoreVines);

    public static final Predicate<TreeConfiguration> FANCY_OAK = byFoliage(FancyFoliagePlacer.class);

    public static final Predicate<TreeConfiguration> BIRCH = byFoliage(BlobFoliagePlacer.class)
            .and(byTrunkBlock(Blocks.BIRCH_LOG));

    public static final Predicate<TreeConfiguration> SPRUCE = byFoliage(SpruceFoliagePlacer.class);

    public static final Predicate<TreeConfiguration> SPRUCE_ONLY = byFoliage(SpruceFoliagePlacer.class)
            .and(byTrunk(GiantTrunkPlacer.class).negate());

    public static final Predicate<TreeConfiguration> PINE = byFoliage(PineFoliagePlacer.class);

    public static final Predicate<TreeConfiguration> MEGA_PINE = byFoliage(MegaPineFoliagePlacer.class);

    public static final Predicate<TreeConfiguration> MEGA_SPRUCE = byTrunk(GiantTrunkPlacer.class)
            .and(byFoliage(SpruceFoliagePlacer.class));

    public static final Predicate<TreeConfiguration> JUNGLE_SMALL = byFoliage(BlobFoliagePlacer.class)
            .and(byTrunkBlock(Blocks.JUNGLE_LOG));

    public static final Predicate<TreeConfiguration> JUNGLE_MEGA = byFoliage(MegaJungleFoliagePlacer.class);

    public static final Predicate<TreeConfiguration> JUNGLE_BUSH = byFoliage(BushFoliagePlacer.class);

    public static final Predicate<TreeConfiguration> ACACIA = byFoliage(AcaciaFoliagePlacer.class);

    public static final Predicate<TreeConfiguration> DARK_OAK = byFoliage(DarkOakFoliagePlacer.class)
            .and(byTrunkBlock(Blocks.DARK_OAK_LOG));

    public static final Predicate<TreeConfiguration> CHERRY = byFoliage(CherryFoliagePlacer.class);

    public static final Predicate<TreeConfiguration> SWAMP = byFoliage(BlobFoliagePlacer.class)
            .and(byTrunkBlock(Blocks.OAK_LOG))
            .and(config -> !config.ignoreVines);

    public static final Predicate<TreeConfiguration> AZALEA = byFoliage(RandomSpreadFoliagePlacer.class)
            .and(byTrunkBlock(Blocks.OAK_LOG));

    public static final Predicate<TreeConfiguration> MANGROVE = byTrunk(UpwardsBranchingTrunkPlacer.class)
            .and(byTrunkBlock(Blocks.MANGROVE_LOG));

    public static final Predicate<HugeFungusConfiguration> WARPED_FUNGUS =
            config -> config.stemState.is(Blocks.WARPED_STEM);

    public static final Predicate<HugeFungusConfiguration> CRIMSON_FUNGUS =
            config -> config.stemState.is(Blocks.CRIMSON_STEM);

    public static final Predicate<HugeFungusConfiguration> ANY_FUNGUS = config ->
            config.stemState.is(Blocks.WARPED_STEM) || config.stemState.is(Blocks.CRIMSON_STEM);

    public static final Predicate<HugeMushroomFeatureConfiguration> RED_MUSHROOM = config -> {
        try {
            return config.capProvider.getState(FIXED_RANDOM, BlockPos.ZERO)
                    .is(Blocks.RED_MUSHROOM_BLOCK);
        } catch (Exception e) {
            return false;
        }
    };

    public static final Predicate<HugeMushroomFeatureConfiguration> BROWN_MUSHROOM = config -> {
        try {
            return config.capProvider.getState(FIXED_RANDOM, BlockPos.ZERO)
                    .is(Blocks.BROWN_MUSHROOM_BLOCK);
        } catch (Exception e) {
            return false;
        }
    };

    public static final Predicate<HugeMushroomFeatureConfiguration> ANY_MUSHROOM =
            config -> RED_MUSHROOM.test(config) || BROWN_MUSHROOM.test(config);

    @SafeVarargs
    public static Predicate<TreeConfiguration> any(Predicate<TreeConfiguration>... matchers) {
        return config -> {
            for (Predicate<TreeConfiguration> m : matchers) {
                if (m.test(config)) return true;
            }
            return false;
        };
    }

    public static Predicate<TreeConfiguration> byFoliage(Class<? extends FoliagePlacer> cls) {
        return config -> cls.isInstance(config.foliagePlacer);
    }

    public static Predicate<TreeConfiguration> byTrunk(Class<? extends TrunkPlacer> cls) {
        return config -> cls.isInstance(config.trunkPlacer);
    }

    public static Predicate<TreeConfiguration> byTrunkBlock(Block block) {
        return config -> {
            try {
                return config.trunkProvider.getState(FIXED_RANDOM, BlockPos.ZERO).is(block);
            } catch (Exception e) {
                return false;
            }
        };
    }
}
