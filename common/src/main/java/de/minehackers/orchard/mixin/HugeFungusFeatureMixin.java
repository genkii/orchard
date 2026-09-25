package de.minehackers.orchard.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.HugeFungusFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import de.minehackers.orchard.Constants;
import de.minehackers.orchard.NbtTreePlacer;
import de.minehackers.orchard.OrchardDefinition;
import de.minehackers.orchard.OrchardRegistry;

/// Replaces natural huge fungi with NBT structures via HEAD hook on place().
@Mixin(HugeFungusFeature.class)
public class HugeFungusFeatureMixin {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void onPlace(
            WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin,
            CallbackInfoReturnable<Boolean> cir) {

        HugeFungusFeature config = (HugeFungusFeature) (Object) this;

        NbtTreePlacer.logFirstInterception(NbtTreePlacer.FUNGUS_FIRED_ONCE,
                "[Orchard] HugeFungusFeatureMixin active - stem=" + config.stemState().getBlock().getDescriptionId()
                        + " origin=" + origin);

        if (config.planted()) return;

        // Same crash-containment rule as the tree mixin: never let a pack
        // problem kill the chunk worker - vanilla proceeds on any failure.
        try {
            Holder<Biome> biome = level.getBiome(origin);

            OrchardDefinition def =
                    OrchardRegistry.pickByFungusWorldGen(config, level, biome, random);
            if (def == null) return;

            Block validBase = config.validBaseState().getBlock();
            BlockState originState = level.getBlockState(origin);
            BlockState belowState = level.getBlockState(origin.below());

            if (!originState.is(validBase) && !belowState.is(validBase)) {
                cir.setReturnValue(false);
                return;
            }

            NbtTreePlacer.interceptFungus(random, cir, def, level, origin);
        } catch (Exception e) {
            Constants.LOG.error("[Orchard] Fungus interception failed at {} - falling back to vanilla",
                    origin, e);
        }
    }
}
