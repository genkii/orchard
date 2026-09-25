package de.minehackers.orchard.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import de.minehackers.orchard.Constants;
import de.minehackers.orchard.NbtTreePlacer;
import de.minehackers.orchard.OrchardDefinition;
import de.minehackers.orchard.OrchardRegistry;

/// Same idea as the tree mixin but for huge mushrooms: inject at HEAD of
/// place(), cancel and replace whenever a pack definition matches. Naturally
/// generated ones only - bone-mealed small mushrooms show up as a red/brown
/// mushroom block at the origin and we let those pass. We also insist on dirt
/// or mycelium ground, the same rule vanilla uses.
@Mixin(AbstractHugeMushroomFeature.class)
public class HugeMushroomFeatureMixin {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void onPlace(
            WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin,
            CallbackInfoReturnable<Boolean> cir) {

        AbstractHugeMushroomFeature config = (AbstractHugeMushroomFeature) (Object) this;

        NbtTreePlacer.logFirstInterception(NbtTreePlacer.MUSHROOM_FIRED_ONCE,
                "[Orchard] HugeMushroomFeatureMixin active - origin=" + origin);

        BlockState originState = level.getBlockState(origin);
        boolean isBoneMeal =
                originState.is(Blocks.RED_MUSHROOM) || originState.is(Blocks.BROWN_MUSHROOM);
        if (isBoneMeal) return;

        // Same crash-containment rule as the tree mixin: never let a pack
        // problem kill the chunk worker - vanilla proceeds on any failure.
        try {
            Holder<Biome> biome = level.getBiome(origin);

            OrchardDefinition def =
                    OrchardRegistry.pickByMushroomWorldGen(config, level, biome, random);
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

            NbtTreePlacer.interceptMushroom(random, cir, def, level, origin);
        } catch (Exception e) {
            Constants.LOG.error("[Orchard] Mushroom interception failed at {} - falling back to vanilla",
                    origin, e);
        }
    }
}
