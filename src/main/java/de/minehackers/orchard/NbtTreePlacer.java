package de.minehackers.orchard;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Handles loading, caching, and placing NBT structure templates as trees.
 * Delegates spatial indexing to {@link PlacementIndex} and terrain protection
 * to {@link TerrainPreservingProcessor}.
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

    /** Pre-built placement settings for each rotation, avoids allocation per placement. */
    private static final StructurePlaceSettings[] SETTINGS_BY_ROTATION = new StructurePlaceSettings[4];
    static {
        for (int i = 0; i < 4; i++) {
            SETTINGS_BY_ROTATION[i] = new StructurePlaceSettings()
                    .setMirror(Mirror.NONE)
                    .setRotation(ROTATIONS[i])
                    .setIgnoreEntities(true)
                    .addProcessor(TerrainPreservingProcessor.INSTANCE);
        }
    }

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
     *
     * @param def   the orchard definition
     * @param level the server level for registry access
     * @return the loaded template, or {@code null} if it could not be loaded
     */
    @Nullable
    public static StructureTemplate getOrLoad(OrchardDefinition def, ServerLevelAccessor level) {
        String key = def.getNbtFileName();
        StructureTemplate cached = CACHE.get(key);
        if (cached != null) {
            return cached == FAILED_LOAD_SENTINEL ? null : cached;
        }

        if (CACHE.containsKey(key)) return null;

        StructureTemplate loaded = tryLoad(key, def.getNbtDirectory(), level);
        CACHE.put(key, loaded != null ? loaded : FAILED_LOAD_SENTINEL);
        return loaded;
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
     *
     * @param level  the server level
     * @param origin the center position
     * @param radius the search radius in blocks
     * @return {@code true} if a log block is found
     */
    public static boolean hasNearbyLog(ServerLevelAccessor level, BlockPos origin, int radius) {
        long r2 = (long) radius * radius;
        BlockPos.MutableBlockPos check = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if ((long) dx * dx + (long) dz * dz > r2) continue;
                int wx = origin.getX() + dx;
                int wz = origin.getZ() + dz;
                int chunkX = wx >> 4;
                int chunkZ = wz >> 4;
                if (!level.hasChunk(chunkX, chunkZ)) continue;
                for (int dy = -2; dy <= 6; dy++) {
                    check.setWithOffset(origin, dx, dy, dz);
                    if (level.getBlockState(check).is(BlockTags.LOGS)) return true;
                }
            }
        }
        return false;
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

    private static final double WALL_MAX_OBSTRUCTED_FRACTION = 0.10;
    private static final int WALL_CHECK_HEIGHT = 5;

    /**
     * Checks whether the area around the origin is sufficiently clear of obstructing blocks.
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

        boolean clear = (double) obstructedColumns / totalColumns <= WALL_MAX_OBSTRUCTED_FRACTION;

        if (!clear) {
            Orchard.LOGGER.debug("[Orchard] Blocked placement at {} – {}/{} columns obstructed ({}%, limit {}%)",
                    origin, obstructedColumns, totalColumns,
                    (obstructedColumns * 100) / totalColumns,
                    (int) (WALL_MAX_OBSTRUCTED_FRACTION * 100));
        }
        return clear;
    }

    /**
     * Determines whether the given block state should be considered an obstruction.
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
            case CLOCKWISE_90         -> { cornerX = -hz; cornerZ =  hx; }
            case CLOCKWISE_180        -> { cornerX =  hx; cornerZ =  hz; }
            case COUNTERCLOCKWISE_90  -> { cornerX =  hz; cornerZ = -hx; }
            default                   -> { cornerX = -hx; cornerZ = -hz; }
        }

        BlockPos corner = origin.offset(cornerX, originYOffset, cornerZ);

        StructurePlaceSettings settings = SETTINGS_BY_ROTATION[rotation.ordinal()];

        template.placeInWorld(level, corner, BlockPos.ZERO, settings, random, 3);
    }
}
