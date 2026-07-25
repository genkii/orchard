package de.minehackers.orchard.mixin;

import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import de.minehackers.orchard.NbtTreePlacer;
import de.minehackers.orchard.Orchard;
import de.minehackers.orchard.OrchardDefinition;

/**
 * Shared logic for all feature mixin interceptors.
 * <p>
 * Each mixin calls {@link #tryIntercept} with its specific configuration type
 * and early-exit conditions. This class handles:
 * <ul>
 *   <li>Dimension and Y-range validation</li>
 *   <li>Spacing checks (both placement index and in-world log scan)</li>
 *   <li>Template loading and placement clearance</li>
 *   <li>Actual structure placement and placement index recording</li>
 * </ul>
 */
final class FeatureInterceptor {

    private FeatureInterceptor() {}

    /** Tracks whether each feature type has fired at least once (for first-interception logging). */
    static final AtomicBoolean TREE_FIRED_ONCE = new AtomicBoolean(false);
    static final AtomicBoolean FUNGUS_FIRED_ONCE = new AtomicBoolean(false);
    static final AtomicBoolean MUSHROOM_FIRED_ONCE = new AtomicBoolean(false);

    /**
     * Logs the first interception for a feature type. Uses {@link AtomicBoolean#compareAndSet}
     * to ensure the message is printed exactly once.
     *
     * @param flag    the atomic flag for this feature type
     * @param message the message to log on first interception
     */
    static void logFirstInterception(AtomicBoolean flag, String message) {
        if (flag.compareAndSet(false, true)) {
            Orchard.LOGGER.warn(message);
        }
    }

    /**
     * Attempts to intercept a feature placement with an orchard definition.
     * <p>
     * This is the unified entry point for all three mixin types. It performs
     * common validation (dimension, Y-range, spacing, trunk clearance, placement
     * clearance) and delegates to {@link NbtTreePlacer} for actual placement.
     *
     * @param context    the feature place context
     * @param cir        the callback return value to set on successful interception
     * @param def        the matched orchard definition
     * @param level      the world gen level
     * @param origin     the placement origin
     * @param trunkCheck whether to perform trunk clearance checks (true for trees, false for fungi/mushrooms)
     */
    static void tryIntercept(
            FeaturePlaceContext<?> context,
            CallbackInfoReturnable<Boolean> cir,
            OrchardDefinition def,
            WorldGenLevel level,
            BlockPos origin,
            boolean trunkCheck) {

        ResourceKey<Level> dimKey = level.getLevel().dimension();
        if (!def.matchesDimension(dimKey)) {
            return;
        }

        if (!def.matchesYRange(origin.getY())) {
            return;
        }

        int spacing = def.getMinSpacing();
        if (spacing > 0) {
            boolean tooClose = NbtTreePlacer.hasNearbyPlacement(def.getNbtFileName(), origin, spacing);
            if (!tooClose) {
                tooClose = NbtTreePlacer.hasNearbyLog(level, origin, spacing);
            }
            if (tooClose) {
                cir.setReturnValue(false);
                return;
            }
        }

        StructureTemplate template = NbtTreePlacer.getOrLoad(def, level);
        if (template == null) {
            Orchard.LOGGER.warn("[Orchard] Template null for {} – falling back to vanilla",
                    def.getNbtFileName());
            return;
        }

        if (trunkCheck && !NbtTreePlacer.isTrunkClear(level, origin, template.getSize().getY())) {
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

    /**
     * Intercepts a tree feature placement.
     *
     * @param context the feature place context
     * @param cir     the callback return value
     * @param def     the matched orchard definition
     * @param level   the world gen level
     * @param origin  the placement origin
     */
    static void interceptTree(
            FeaturePlaceContext<?> context,
            CallbackInfoReturnable<Boolean> cir,
            OrchardDefinition def,
            WorldGenLevel level,
            BlockPos origin) {
        tryIntercept(context, cir, def, level, origin, true);
    }

    /**
     * Intercepts a fungus feature placement.
     *
     * @param context the feature place context
     * @param cir     the callback return value
     * @param def     the matched orchard definition
     * @param level   the world gen level
     * @param origin  the placement origin
     */
    static void interceptFungus(
            FeaturePlaceContext<?> context,
            CallbackInfoReturnable<Boolean> cir,
            OrchardDefinition def,
            WorldGenLevel level,
            BlockPos origin) {
        tryIntercept(context, cir, def, level, origin, false);
    }

    /**
     * Intercepts a mushroom feature placement.
     *
     * @param context the feature place context
     * @param cir     the callback return value
     * @param def     the matched orchard definition
     * @param level   the world gen level
     * @param origin  the placement origin
     */
    static void interceptMushroom(
            FeaturePlaceContext<?> context,
            CallbackInfoReturnable<Boolean> cir,
            OrchardDefinition def,
            WorldGenLevel level,
            BlockPos origin) {
        tryIntercept(context, cir, def, level, origin, false);
    }
}
