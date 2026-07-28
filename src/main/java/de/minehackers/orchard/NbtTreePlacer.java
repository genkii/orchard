package de.minehackers.orchard;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Handles loading, caching, and placing NBT structure templates as trees.
 * <p>
 * Templates are cached in a thread-safe {@link ConcurrentHashMap} keyed by
 * file name. A sentinel object is stored for failed loads to prevent repeated
 * disk I/O for missing files.
 * <p>
 * {@link StructurePlaceSettings} are allocated per-placement to avoid
 * shared mutable state across threads during concurrent world generation.
 * <p>
 * Spatial indexing is delegated to {@link PlacementIndex} and terrain
 * protection to {@link TerrainPreservingProcessor}.
 */
public final class NbtTreePlacer {

    private NbtTreePlacer() {}

    /** The four rotations that can be applied to a placed tree. */
    private static final Rotation[] ROTATIONS = {
            Rotation.NONE, Rotation.CLOCKWISE_90,
            Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90
    };

    /** Sentinel object used as a cache value for failed load attempts (ConcurrentHashMap prohibits null values). */
    private static final StructureTemplate FAILED_LOAD_SENTINEL = new StructureTemplate();

    /** Cache of loaded templates keyed by file name. The sentinel indicates a failed load attempt. */
    private static final Map<String, StructureTemplate> CACHE = new ConcurrentHashMap<>(16);

    /** Maximum file size for NBT files (10 MB). */
    private static final long MAX_NBT_FILE_SIZE = 10 * 1024 * 1024;

    /** Maximum number of blocks to check downward when adjusting the origin to solid ground. */
    private static final int MAX_GROUND_ADJUST = 4;

    /** Number of blocks above the origin to scan for nearby logs during spacing checks. */
    private static final int LOG_SCAN_HEIGHT_ABOVE = 6;

    /** Number of blocks below the origin to scan for nearby logs during spacing checks. */
    private static final int LOG_SCAN_DEPTH_BELOW = 2;

    /** Flags passed to {@link StructureTemplate#placeInWorld} (2 = send change to clients, no neighbor updates). */
    private static final int PLACE_FLAGS = 3;

    /** Default maximum fraction of obstructed columns allowed during placement clearance checks. */
    private static double maxObstructedFraction = 0.10;

    /** Number of blocks upward to check when scanning for obstructing columns. */
    private static final int WALL_CHECK_HEIGHT = 5;

    /**
     * Returns the terrain-preserving structure processor singleton.
     *
     * @return the processor instance
     */
    public static StructureProcessor getTerrainPreservingProcessor() {
        return TerrainPreservingProcessor.INSTANCE;
    }

    /**
     * Clears the template cache and placement index.
     */
    public static void clearCache() {
        CACHE.clear();
        PlacementIndex.clear();
        Orchard.LOGGER.info("[Orchard] Template cache cleared.");
    }

    /**
     * Pre-loads all registered orchard definitions into the template cache.
     *
     * @param level  the server level for registry access
     * @param nbtDir the directory containing NBT files
     */
    public static void preWarmAll(ServerLevelAccessor level, Path nbtDir) {
        int loaded = 0, missing = 0;
        for (OrchardDefinition def : OrchardRegistry.getAll()) {
            if (getOrLoad(def, level) != null) loaded++;
            else missing++;
        }
        Orchard.LOGGER.info("[Orchard] Cache pre-warmed: {} template(s) loaded, {} missing.", loaded, missing);
    }

    /**
     * Returns the cached template for the given definition, loading it from disk if necessary.
     * <p>
     * Uses {@link ConcurrentHashMap#computeIfAbsent} to guarantee that each
     * file is loaded at most once, even under concurrent access.
     *
     * @param def   the orchard definition
     * @param level the server level for registry access
     * @return the loaded template, or {@code null} if it could not be loaded
     */
    @Nullable
    public static StructureTemplate getOrLoad(OrchardDefinition def, ServerLevelAccessor level) {
        String key = def.getNbtFileName();
        StructureTemplate result = CACHE.computeIfAbsent(key, k -> {
            StructureTemplate loaded = tryLoad(k, def.getNbtDirectory(), level);
            return loaded != null ? loaded : FAILED_LOAD_SENTINEL;
        });
        return result == FAILED_LOAD_SENTINEL ? null : result;
    }

    /**
     * Attempts to load a structure template from an NBT file.
     *
     * @param fileName the file name within the NBT directory
     * @param nbtDir   the directory containing NBT files
     * @param level    the server level for registry access
     * @return the loaded template, or {@code null} on failure
     */
    @Nullable
    private static StructureTemplate tryLoad(String fileName, Path nbtDir, ServerLevelAccessor level) {
        Path filePath = nbtDir.resolve(fileName);

        if (!Files.exists(filePath)) {
            Orchard.LOGGER.warn("[Orchard] NBT file not found: {} – place it in the nbt/ config folder", filePath);
            return null;
        }

        try {
            long fileSize = Files.size(filePath);
            if (fileSize > MAX_NBT_FILE_SIZE) {
                Orchard.LOGGER.error("[Orchard] NBT file too large: {} ({} bytes, max {} bytes)",
                        fileName, fileSize, MAX_NBT_FILE_SIZE);
                return null;
            }
            if (fileSize == 0) {
                Orchard.LOGGER.error("[Orchard] NBT file is empty: {}", fileName);
                return null;
            }
        } catch (Exception e) {
            Orchard.LOGGER.warn("[Orchard] Could not check file size for {}: {}", fileName, e.getMessage());
        }

        try (InputStream in = Files.newInputStream(filePath)) {
            CompoundTag nbt = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            StructureTemplate template = new StructureTemplate();
            template.load(level.registryAccess().lookupOrThrow(Registries.BLOCK), nbt);
            Orchard.LOGGER.info("[Orchard] Loaded {} (size: {})", fileName, template.getSize());
            return template;
        } catch (Exception e) {
            Orchard.LOGGER.error("[Orchard] Failed to load {}: {}", fileName, e.getMessage());
            return null;
        }
    }

    /**
     * Records a placement in the spatial index.
     *
     * @param nbtPath the NBT template file path
     * @param origin  the world position where the structure was placed
     */
    public static void markPlaced(String nbtPath, BlockPos origin) {
        PlacementIndex.markPlaced(nbtPath, origin);
    }

    /**
     * Checks whether a placement with the same NBT path already exists within the given radius.
     *
     * @param nbtPath the NBT template file path to match
     * @param origin  the world position to check around
     * @param radius  the maximum distance in blocks
     * @return {@code true} if a nearby matching placement exists
     */
    public static boolean hasNearbyPlacement(String nbtPath, BlockPos origin, int radius) {
        return PlacementIndex.hasNearbyPlacement(nbtPath, origin, radius);
    }

    /**
     * Checks whether any log block exists within the given radius of the origin.
     * <p>
     * Scans a cylinder of radius {@code radius} blocks, from
     * {@link #LOG_SCAN_DEPTH_BELOW} blocks below to {@link #LOG_SCAN_HEIGHT_ABOVE}
     * blocks above the origin. Skips chunks that are not loaded or have already
     * been scanned this session.
     *
     * @param level  the server level
     * @param origin the center position
     * @param radius the search radius in blocks
     * @return {@code true} if a log block is found
     */
    public static boolean hasNearbyLog(ServerLevelAccessor level, BlockPos origin, int radius) {
        long r2 = (long) radius * radius;
        BlockPos.MutableBlockPos check = new BlockPos.MutableBlockPos();
        HashSet<Long> scannedChunks = new HashSet<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if ((long) dx * dx + (long) dz * dz > r2) continue;
                int wx = origin.getX() + dx;
                int wz = origin.getZ() + dz;
                int chunkX = wx >> 4;
                int chunkZ = wz >> 4;
                long chunkKey = chunkKey(chunkX, chunkZ);
                if (!scannedChunks.add(chunkKey)) continue;
                if (!level.hasChunk(chunkX, chunkZ)) continue;
                for (int dy = -LOG_SCAN_DEPTH_BELOW; dy <= LOG_SCAN_HEIGHT_ABOVE; dy++) {
                    check.setWithOffset(origin, dx, dy, dz);
                    if (level.getBlockState(check).is(BlockTags.LOGS)) return true;
                }
            }
        }
        return false;
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    /**
     * Checks whether the trunk column from the origin upward is clear of bedrock and lava.
     *
     * @param level  the server level
     * @param origin the base position
     * @param height the number of blocks to check upward
     * @return {@code true} if the entire trunk column is clear
     */
    public static boolean isTrunkClear(ServerLevelAccessor level, BlockPos origin, int height) {
        BlockPos.MutableBlockPos check = origin.mutable();
        for (int dy = 0; dy < height; dy++) {
            BlockState state = level.getBlockState(check);
            if (state.is(Blocks.BEDROCK) || state.getFluidState().is(FluidTags.LAVA)) {
                return false;
            }
            check.move(0, 1, 0);
        }
        return true;
    }

    /** Maximum number of blocks to scan upward for open sky when checking surface validity. */
    private static final int SKY_SCAN_HEIGHT = 24;

    /**
     * Checks whether the origin is on a valid surface for tree placement.
     * <p>
     * Validates three conditions:
     * <ol>
     *   <li>The origin block is air or replaceable (not solid stone, etc.)</li>
     *   <li>The block directly below is solid, non-liquid, non-replaceable ground</li>
     *   <li>There is open sky above — no solid blocks between origin and {@link #SKY_SCAN_HEIGHT}
     *       blocks up (prevents underground/overhang placement)</li>
     * </ol>
     *
     * @param level  the server level
     * @param origin the candidate placement position (trunk base)
     * @return {@code true} if the origin is on a valid open-air surface
     */
    public static boolean isOnSurface(ServerLevelAccessor level, BlockPos origin) {
        BlockState originState = level.getBlockState(origin);
        if (originState.isSolid()) {
            return false;
        }

        BlockState belowState = level.getBlockState(origin.below());
        if (belowState.isAir() || belowState.liquid() || !belowState.isSolid()) {
            return false;
        }

        BlockPos.MutableBlockPos above = origin.mutable();
        for (int dy = 1; dy <= SKY_SCAN_HEIGHT; dy++) {
            above.move(0, 1, 0);
            if (level.getBlockState(above).isSolid()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether the area around the origin is sufficiently clear of obstructing blocks.
     * <p>
     * Scans a cylinder of columns with radius based on the structure footprint
     * (capped at 8 blocks). For each column, checks up to {@link #WALL_CHECK_HEIGHT}
     * blocks upward. If more than {@link #maxObstructedFraction} of columns contain
     * an obstructing block, placement is rejected.
     *
     * @param level        the server level
     * @param origin       the center position
     * @param structureSize the size of the structure to place
     * @return {@code true} if the obstruction fraction is within tolerance
     */
    public static boolean isPlacementClear(ServerLevelAccessor level, BlockPos origin, Vec3i structureSize) {
        int radius = Math.min(Math.max(structureSize.getX(), structureSize.getZ()) / 2, 8);
        int r2 = radius * radius;

        int obstructedColumns = 0;
        int totalColumns = 0;

        BlockPos.MutableBlockPos check = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > r2) continue;
                totalColumns++;
                for (int dy = 1; dy <= WALL_CHECK_HEIGHT; dy++) {
                    check.setWithOffset(origin, dx, dy, dz);
                    if (isObstructingBlock(level.getBlockState(check))) {
                        obstructedColumns++;
                        break;
                    }
                }
            }
        }

        if (totalColumns == 0) return true;

        boolean clear = (double) obstructedColumns / totalColumns <= maxObstructedFraction;

        if (!clear) {
            Orchard.LOGGER.debug("[Orchard] Blocked placement at {} – {}/{} columns obstructed ({}%, limit {}%)",
                    origin, obstructedColumns, totalColumns,
                    (obstructedColumns * 100) / totalColumns,
                    (int) (maxObstructedFraction * 100));
        }
        return clear;
    }

    /**
     * Determines whether the given block state should be considered an obstruction.
     * <p>
     * Air, replaceable blocks, carver-replaceable blocks, nylium, dirt, leaves,
     * and logs are not considered obstructions. All other solid blocks are.
     *
     * @param state the block state to test
     * @return {@code true} if the block is a solid obstruction
     */
    private static boolean isObstructingBlock(BlockState state) {
        if (state.isAir()) return false;
        if (state.canBeReplaced()) return false;
        if (state.is(BlockTags.NETHER_CARVER_REPLACEABLES)) return false;
        if (state.is(BlockTags.OVERWORLD_CARVER_REPLACEABLES)) return false;
        if (state.is(BlockTags.NYLIUM)) return false;
        if (state.is(BlockTags.DIRT)) return false;
        if (state.is(BlockTags.LEAVES)) return false;
        if (state.is(BlockTags.LOGS)) return false;
        return state.isSolid();
    }

    /**
     * Adjusts the origin downward to find the first solid ground within the given range.
     * <p>
     * Moves down block by block; if the block below is non-air, non-liquid, and
     * replaceable (e.g. snow layer), the origin is shifted down. Stops at the first
     * non-replaceable block or after {@link #MAX_GROUND_ADJUST} blocks.
     *
     * @param level  the server level
     * @param origin the starting position
     * @param maxDown the maximum number of blocks to search downward
     * @return the adjusted position, or the original if no adjustment was needed
     */
    public static BlockPos groundAdjust(ServerLevelAccessor level, BlockPos origin, int maxDown) {
        BlockPos.MutableBlockPos mutable = origin.mutable();
        for (int i = 0; i < maxDown; i++) {
            mutable.move(0, -1, 0);
            BlockState below = level.getBlockState(mutable);
            if (!below.isAir() && !below.liquid() && below.canBeReplaced()) {
                origin = mutable.immutable();
            } else {
                break;
            }
        }
        return origin;
    }

    // -------------------------------------------------------------------------
    // Feature interceptors (called from mixins)
    // -------------------------------------------------------------------------

    public static final AtomicBoolean TREE_FIRED_ONCE = new AtomicBoolean(false);
    public static final AtomicBoolean FUNGUS_FIRED_ONCE = new AtomicBoolean(false);
    public static final AtomicBoolean MUSHROOM_FIRED_ONCE = new AtomicBoolean(false);

    /**
     * Logs the first interception for a feature type.
     */
    public static void logFirstInterception(AtomicBoolean flag, String message) {
        if (flag.compareAndSet(false, true)) {
            Orchard.LOGGER.warn(message);
        }
    }

    /**
     * Unified interception logic for tree, fungus, and mushroom placement.
     */
    public static void tryIntercept(
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

        if (!isOnSurface(level, origin)) {
            return;
        }

        int spacing = def.getMinSpacing();
        if (spacing > 0) {
            boolean tooClose = hasNearbyPlacement(def.getNbtFileName(), origin, spacing);
            if (!tooClose) {
                tooClose = hasNearbyLog(level, origin, spacing);
            }
            if (tooClose) {
                cir.setReturnValue(false);
                return;
            }
        }

        StructureTemplate template = getOrLoad(def, level);
        if (template == null) {
            Orchard.LOGGER.warn("[Orchard] Template null for {} – falling back to vanilla",
                    def.getNbtFileName());
            return;
        }

        if (trunkCheck && !isTrunkClear(level, origin, template.getSize().getY())) {
            Orchard.LOGGER.debug("[Orchard] Trunk not clear at {} – falling back to vanilla", origin);
            return;
        }

        if (!isPlacementClear(level, origin, template.getSize())) {
            Orchard.LOGGER.debug("[Orchard] Placement not clear at {} – falling back to vanilla", origin);
            return;
        }

        Orchard.LOGGER.debug("[Orchard] Placing {} at {}", def.getNbtFileName(), origin);

        place(template, level, origin, context.random(), def.getOriginYOffset());
        markPlaced(def.getNbtFileName(), origin);

        cir.setReturnValue(true);
    }

    public static void interceptTree(
            FeaturePlaceContext<?> context,
            CallbackInfoReturnable<Boolean> cir,
            OrchardDefinition def,
            WorldGenLevel level,
            BlockPos origin) {
        tryIntercept(context, cir, def, level, origin, true);
    }

    public static void interceptFungus(
            FeaturePlaceContext<?> context,
            CallbackInfoReturnable<Boolean> cir,
            OrchardDefinition def,
            WorldGenLevel level,
            BlockPos origin) {
        tryIntercept(context, cir, def, level, origin, false);
    }

    public static void interceptMushroom(
            FeaturePlaceContext<?> context,
            CallbackInfoReturnable<Boolean> cir,
            OrchardDefinition def,
            WorldGenLevel level,
            BlockPos origin) {
        tryIntercept(context, cir, def, level, origin, false);
    }

    /**
     * Places a structure template at the given origin with a random rotation.
     *
     * @param template       the structure template to place
     * @param level          the server level
     * @param origin         the placement origin
     * @param random         the random source
     * @param originYOffset  the Y offset applied to the origin
     */
    public static void place(StructureTemplate template, ServerLevelAccessor level,
                             BlockPos origin, RandomSource random, int originYOffset) {
        Rotation rotation = ROTATIONS[random.nextInt(ROTATIONS.length)];
        place(template, level, origin, random, originYOffset, rotation);
    }

    /**
     * Places a structure template at the given origin with a specific rotation.
     * <p>
     * A fresh {@link StructurePlaceSettings} is created for each placement to
     * avoid thread-safety issues with shared mutable state.
     *
     * @param template       the structure template to place
     * @param level          the server level
     * @param origin         the placement origin
     * @param random         the random source
     * @param originYOffset  the Y offset applied to the origin
     * @param rotation       the rotation to apply
     */
    public static void place(StructureTemplate template, ServerLevelAccessor level,
                             BlockPos origin, RandomSource random, int originYOffset,
                             Rotation rotation) {
        Vec3i size = template.getSize();
        int hx = size.getX() / 2;
        int hz = size.getZ() / 2;

        int cornerX, cornerZ;
        switch (rotation) {
            case CLOCKWISE_90         -> { cornerX =  hz; cornerZ = -hx; }
            case CLOCKWISE_180        -> { cornerX =  hx; cornerZ =  hz; }
            case COUNTERCLOCKWISE_90  -> { cornerX = -hz; cornerZ =  hx; }
            default                   -> { cornerX = -hx; cornerZ = -hz; }
        }

        BlockPos corner = origin.offset(cornerX, originYOffset, cornerZ);

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(rotation)
                .setIgnoreEntities(true)
                .addProcessor(TerrainPreservingProcessor.INSTANCE);

        template.placeInWorld(level, corner, BlockPos.ZERO, settings, random, PLACE_FLAGS);
    }

    /**
     * Returns the current maximum obstructed column fraction for placement clearance checks.
     *
     * @return the maximum fraction (0.0 to 1.0)
     */
    public static double getMaxObstructedFraction() {
        return maxObstructedFraction;
    }

    /**
     * Sets the maximum obstructed column fraction for placement clearance checks.
     *
     * @param fraction the maximum fraction (0.0 to 1.0); values outside this range are clamped
     */
    public static void setMaxObstructedFraction(double fraction) {
        maxObstructedFraction = Math.max(0.0, Math.min(1.0, fraction));
    }

    /**
     * Returns the maximum ground adjustment distance in blocks.
     *
     * @return the maximum number of blocks to search downward
     */
    public static int getMaxGroundAdjust() {
        return MAX_GROUND_ADJUST;
    }
}
