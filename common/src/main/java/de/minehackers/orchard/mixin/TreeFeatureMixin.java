package de.minehackers.orchard.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import de.minehackers.orchard.NbtTreePlacer;
import de.minehackers.orchard.OrchardDefinition;
import de.minehackers.orchard.OrchardRegistry;

/// Intercepts TreeFeature.place to replace vanilla trees with NBT structures.
@Mixin(TreeFeature.class)
public class TreeFeatureMixin {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void onPlace(
            FeaturePlaceContext<TreeConfiguration> context,
            CallbackInfoReturnable<Boolean> cir) {

        TreeConfiguration config = context.config();
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        NbtTreePlacer.logFirstInterception(NbtTreePlacer.TREE_FIRED_ONCE,
                "[Orchard] TreeFeatureMixin is active - first TreeFeature.place() intercepted.");

        Holder<Biome> biome = level.getBiome(origin);
        OrchardDefinition def =
                OrchardRegistry.pickByWorldGen(config, level, biome, context.random());
        if (def == null) return;

        BlockState originState = level.getBlockState(origin);
        if (originState.getFluidState().is(FluidTags.LAVA)) return;
        if (originState.getFluidState().is(FluidTags.WATER)) {
            if (!biome.is(Biomes.MANGROVE_SWAMP) && !biome.is(Biomes.SWAMP)) {
                return;
            }
        }

        if (def.getValidFloor() != null && !def.getValidFloor().test(originState)) {
            return;
        }

        origin = NbtTreePlacer.groundAdjust(level, origin, NbtTreePlacer.getMaxGroundAdjust());

        NbtTreePlacer.interceptTree(context, cir, def, level, origin);
    }
}
