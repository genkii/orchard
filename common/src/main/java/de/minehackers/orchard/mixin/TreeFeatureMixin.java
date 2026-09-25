package de.minehackers.orchard.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import de.minehackers.orchard.Constants;
import de.minehackers.orchard.NbtTreePlacer;
import de.minehackers.orchard.OrchardDefinition;
import de.minehackers.orchard.OrchardRegistry;

/// Replaces vanilla trees with NBT structures via HEAD hook on place().
@Mixin(TreeFeature.class)
public class TreeFeatureMixin {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void onPlace(
            WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin,
            CallbackInfoReturnable<Boolean> cir) {

        TreeFeature config = (TreeFeature) (Object) this;

        NbtTreePlacer.logFirstInterception(NbtTreePlacer.TREE_FIRED_ONCE,
                "[Orchard] TreeFeatureMixin is active - first TreeFeature.place() intercepted.");

        // Worldgen runs on shared worker threads: a broken pack definition
        // must never take the chunk build down with it. Any failure here
        // means vanilla places the tree instead.
        try {
            Holder<Biome> biome = level.getBiome(origin);
            OrchardDefinition def =
                    OrchardRegistry.pickByWorldGen(config, level, biome, random);
            if (def == null) return;

            BlockState originState = level.getBlockState(origin);
            if (originState.getFluidState().is(FluidTags.LAVA)) return;
            if (originState.getFluidState().is(FluidTags.WATER)) {
                if (!biome.is(Biomes.MANGROVE_SWAMP) && !biome.is(Biomes.SWAMP)) {
                    return;
                }
            }

            origin = NbtTreePlacer.groundAdjust(level, origin, NbtTreePlacer.getMaxGroundAdjust());

            NbtTreePlacer.interceptTree(random, cir, def, level, origin);
        } catch (Exception e) {
            Constants.LOG.error("[Orchard] Tree interception failed at {} - falling back to vanilla",
                    origin, e);
        }
    }
}
