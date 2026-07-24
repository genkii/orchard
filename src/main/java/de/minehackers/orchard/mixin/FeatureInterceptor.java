package de.minehackers.orchard.mixin;

import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import de.minehackers.orchard.Orchard;
import de.minehackers.orchard.OrchardDefinition;
import de.minehackers.orchard.OrchardRegistry;
import de.minehackers.orchard.NbtTreePlacer;

/**
 * Shared logic for all feature mixin interceptors. Each mixin delegates to
 * one of the {@code intercept*} methods with its specific early-exit conditions.
 */
final class FeatureInterceptor {

    private FeatureInterceptor() {}

    static final AtomicBoolean TREE_FIRED_ONCE = new AtomicBoolean(false);
    static final AtomicBoolean FUNGUS_FIRED_ONCE = new AtomicBoolean(false);
    static final AtomicBoolean MUSHROOM_FIRED_ONCE = new AtomicBoolean(false);

    static void logFirstInterception(AtomicBoolean flag, String message) {
        if (flag.compareAndSet(false, true)) {
            Orchard.LOGGER.warn(message);
        }
    }

    static void interceptTree(
            FeaturePlaceContext<?> context,
            CallbackInfoReturnable<Boolean> cir,
            OrchardDefinition def,
            WorldGenLevel level,
            BlockPos origin) {

        StructureTemplate template = NbtTreePlacer.getOrLoad(def, level);
        if (template == null) {
            Orchard.LOGGER.warn("[Orchard] Template null for {} – falling back to vanilla",
                    def.getNbtFileName());
            return;
        }

        if (!NbtTreePlacer.isTrunkClear(level, origin, template.getSize().getY())) {
            Orchard.LOGGER.debug("[Orchard] Trunk not clear at {} – falling back to vanilla", origin);
            return;
        }

        if (!NbtTreePlacer.isPlacementClear(level, origin, template.getSize())) {
            Orchard.LOGGER.debug("[Orchard] Placement not clear at {} – falling back to vanilla", origin);
            return;
        }

        Orchard.LOGGER.debug("[Orchard] Placing {} at {}", def.getNbtFileName(), origin);

        NbtTreePlacer.place(template, level, origin, context.random(), def.getOriginYOffset());
        NbtTreePlacer.markPlaced(def.getNbtFileName(), origin);

        cir.setReturnValue(true);
    }

    static void interceptFungus(
            FeaturePlaceContext<?> context,
            CallbackInfoReturnable<Boolean> cir,
            OrchardDefinition def,
            WorldGenLevel level,
            BlockPos origin) {

        StructureTemplate template = NbtTreePlacer.getOrLoad(def, level);
        if (template == null) {
            Orchard.LOGGER.warn("[Orchard] Template null for {} – falling back to vanilla",
                    def.getNbtFileName());
            return;
        }

        if (!NbtTreePlacer.isPlacementClear(level, origin, template.getSize())) {
            Orchard.LOGGER.debug("[Orchard] Placement not clear at {} – falling back to vanilla", origin);
            return;
        }

        NbtTreePlacer.place(template, level, origin, context.random(), def.getOriginYOffset());
        NbtTreePlacer.markPlaced(def.getNbtFileName(), origin);
        cir.setReturnValue(true);
    }

    static void interceptMushroom(
            FeaturePlaceContext<?> context,
            CallbackInfoReturnable<Boolean> cir,
            OrchardDefinition def,
            WorldGenLevel level,
            BlockPos origin) {

        StructureTemplate template = NbtTreePlacer.getOrLoad(def, level);
        if (template == null) {
            Orchard.LOGGER.warn("[Orchard] Template null for {} – falling back to vanilla",
                    def.getNbtFileName());
            return;
        }

        if (!NbtTreePlacer.isPlacementClear(level, origin, template.getSize())) {
            Orchard.LOGGER.debug("[Orchard] Placement not clear at {} – falling back to vanilla", origin);
            return;
        }

        NbtTreePlacer.place(template, level, origin, context.random(), def.getOriginYOffset());
        NbtTreePlacer.markPlaced(def.getNbtFileName(), origin);

        cir.setReturnValue(true);
    }
}
