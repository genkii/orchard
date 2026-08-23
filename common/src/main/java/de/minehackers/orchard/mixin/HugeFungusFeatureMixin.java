package de.minehackers.orchard.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.HugeFungusConfiguration;
import net.minecraft.world.level.levelgen.feature.HugeFungusFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import de.minehackers.orchard.Constants;
import de.minehackers.orchard.NbtTreePlacer;
import de.minehackers.orchard.OrchardDefinition;
import de.minehackers.orchard.OrchardRegistry;

/// Swaps naturally generated huge fungi for NBT structures, again via a HEAD
/// hook on place(). Player-planted fungi carry the planted flag in their
/// config (bone meal sets it) and are ignored. We only take over when the
/// fungus sits on its configured valid base block.
@Mixin(HugeFungusFeature.class)
public class HugeFungusFeatureMixin {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void onPlace(
            FeaturePlaceContext<HugeFungusConfiguration> context,
            CallbackInfoReturnable<Boolean> cir) {

        HugeFungusConfiguration config = context.config();
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        NbtTreePlacer.logFirstInterception(NbtTreePlacer.FUNGUS_FIRED_ONCE,
                "[Orchard] HugeFungusFeatureMixin active - stem=" + config.stemState.getBlock().getDescriptionId()
                        + " origin=" + origin);

        if (config.planted) return;

        // Same crash-containment rule as the tree mixin: never let a pack
        // problem kill the chunk worker - vanilla proceeds on any failure.
        try {
            Holder<Biome> biome = level.getBiome(origin);

            OrchardDefinition def =
                    OrchardRegistry.pickByFungusWorldGen(config, level, biome, context.random());
            if (def == null) return;

            Block validBase = config.validBaseState.getBlock();
            BlockState originState = level.getBlockState(origin);
            BlockState belowState = level.getBlockState(origin.below());

            if (!originState.is(validBase) && !belowState.is(validBase)) {
                cir.setReturnValue(false);
                return;
            }

            NbtTreePlacer.interceptFungus(context, cir, def, level, origin);
        } catch (Exception e) {
            Constants.LOG.error("[Orchard] Fungus interception failed at {} - falling back to vanilla",
                    origin, e);
        }
    }
}
