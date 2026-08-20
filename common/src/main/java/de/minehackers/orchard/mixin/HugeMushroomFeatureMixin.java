package de.minehackers.orchard.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import de.minehackers.orchard.NbtTreePlacer;
import de.minehackers.orchard.OrchardDefinition;
import de.minehackers.orchard.OrchardRegistry;

/// Intercepts AbstractHugeMushroomFeature.place to replace huge mushrooms with NBT structures.
/// Skips bone-mealed small mushrooms. Requires dirt or mycelium ground.
@Mixin(AbstractHugeMushroomFeature.class)
public class HugeMushroomFeatureMixin {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void onPlace(
            FeaturePlaceContext<HugeMushroomFeatureConfiguration> context,
            CallbackInfoReturnable<Boolean> cir) {

        HugeMushroomFeatureConfiguration config = context.config();
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        NbtTreePlacer.logFirstInterception(NbtTreePlacer.MUSHROOM_FIRED_ONCE,
                "[Orchard] HugeMushroomFeatureMixin active - origin=" + origin);

        BlockState originState = level.getBlockState(origin);
        boolean isBoneMeal =
                originState.is(Blocks.RED_MUSHROOM) || originState.is(Blocks.BROWN_MUSHROOM);
        if (isBoneMeal) return;

        Holder<Biome> biome = level.getBiome(origin);

        OrchardDefinition def =
                OrchardRegistry.pickByMushroomWorldGen(config, level, biome, context.random());
        if (def == null) return;

        BlockState groundState = level.getBlockState(origin.below());

        if (groundState.liquid()) {
            cir.setReturnValue(false);
            return;
        }
        if (!groundState.is(BlockTags.DIRT) && !groundState.is(Blocks.MYCELIUM)) {
            cir.setReturnValue(false);
            return;
        }

        NbtTreePlacer.interceptMushroom(context, cir, def, level, origin);
    }
}
