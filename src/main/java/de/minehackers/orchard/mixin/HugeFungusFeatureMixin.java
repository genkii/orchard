package de.minehackers.orchard.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.HugeFungusConfiguration;
import net.minecraft.world.level.levelgen.feature.HugeFungusFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import de.minehackers.orchard.OrchardDefinition;
import de.minehackers.orchard.OrchardRegistry;
import de.minehackers.orchard.NbtTreePlacer;

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

        int spacing = def.getMinSpacing();
        if (spacing > 0) {
            if (NbtTreePlacer.hasNearbyPlacement(def.getNbtFileName(), origin, spacing)) {
                cir.setReturnValue(false);
                return;
            }
        }

        FeatureInterceptor.interceptFungus(context, cir, def, level, origin);
    }
}
