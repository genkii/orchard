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
import de.minehackers.orchard.OrchardDefinition;
import de.minehackers.orchard.OrchardRegistry;

/**
 * Intercepts {@link HugeFungusFeature#place} to replace vanilla huge fungi
 * with user-defined NBT structure templates.
 * <p>
 * Player-bone-mealed fungi ({@code config.planted}) are skipped. Spacing,
 * dimension, and Y-range checks are handled by {@link FeatureInterceptor}.
 */
@Mixin(HugeFungusFeature.class)
public class HugeFungusFeatureMixin {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void onPlace(
            FeaturePlaceContext<HugeFungusConfiguration> context,
            CallbackInfoReturnable<Boolean> cir) {

        HugeFungusConfiguration config = context.config();
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        FeatureInterceptor.logFirstInterception(FeatureInterceptor.FUNGUS_FIRED_ONCE,
                "[Orchard] HugeFungusFeatureMixin active – stem=" + config.stemState.getBlock().getDescriptionId()
                        + " origin=" + origin);

        if (config.planted) return;

        Holder<Biome> biome = level.getBiome(origin);

        OrchardDefinition def =
                OrchardRegistry.pickByFungusWorldGen(config, biome, context.random());
        if (def == null) return;

        Block validBase = config.validBaseState.getBlock();
        BlockState originState = level.getBlockState(origin);
        BlockState belowState = level.getBlockState(origin.below());

        if (!originState.is(validBase) && !belowState.is(validBase)) {
            cir.setReturnValue(false);
            return;
        }

        FeatureInterceptor.interceptFungus(context, cir, def, level, origin);
    }
}
