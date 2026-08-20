package de.minehackers.orchard;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.Nullable;
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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/// Loads, caches, and places NBT structure templates as tree replacements.
/// Templates are loaded once, converted to a compact representation with pre-computed
/// rotation transforms, and cached. Placement is a tight loop over flat int arrays.
public final class NbtTreePlacer {

    private NbtTreePlacer() {}

    private static final CompactTemplate FAILED_LOAD_SENTINEL = new CompactTemplate(
            Vec3i.ZERO, new net.minecraft.world.level.block.state.BlockState[0], new java.util.List[0]);
    private static final int MAX_CACHE_SIZE = 256;
    private static final Map<String, CompactTemplate> CACHE = new ConcurrentHashMap<>(16);
    private static final Map<String, Long> CACHE_ACCESS_TIMES = new ConcurrentHashMap<>(16);
    private static final AtomicLong CACHE_HITS = new AtomicLong();
    private static final AtomicLong CACHE_MISSES = new AtomicLong();
    private static final AtomicLong CACHE_EVICTIONS = new AtomicLong();
    private static final AtomicLong TOTAL_PLACEMENTS = new AtomicLong();
    private static final int MAX_GROUND_ADJUST = 4;
    private static final int LOG_SCAN_HEIGHT_ABOVE = 6;
    private static final int LOG_SCAN_DEPTH_BELOW = 2;

    private static double maxObstructedFraction = 0.10;
    private static final int WALL_CHECK_HEIGHT = 5;
    private static final int SKY_SCAN_HEIGHT = 24;

    public static void clearCache() {
        CACHE.clear();
        CACHE_ACCESS_TIMES.clear();
        CACHE_HITS.set(0);
        CACHE_MISSES.set(0);
        CACHE_EVICTIONS.set(0);
        TOTAL_PLACEMENTS.set(0);
        PlacementIndex.clear();
        Constants.LOG.info("[Orchard] Template cache cleared.");
    }

    public static net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor
            getTerrainPreservingProcessor() {
        return TerrainPreservingProcessor.INSTANCE;
    }

    public static void preWarmAll(ServerLevelAccessor level, Path nbtDir) {
        int loaded = 0, missing = 0;
        for (OrchardDefinition def : OrchardRegistry.getAll()) {
            if (getOrLoad(def, level) != null) loaded++;
            else missing++;
        }
        Constants.LOG.info("[Orchard] Cache pre-warmed: {} template(s) loaded, {} missing.", loaded, missing);
    }

    /// Returns the cached compact template, loading and converting from disk on first access.
    @Nullable
    public static CompactTemplate getOrLoad(OrchardDefinition def, ServerLevelAccessor level) {
        String key = def.getNbtFileName();
        long now = System.nanoTime();
        CompactTemplate result = CACHE.computeIfAbsent(key, k -> {
            CompactTemplate loaded = tryLoadAndCompact(k, def.getNbtDirectory(), level);
            return loaded != null ? loaded : FAILED_LOAD_SENTINEL;
        });
        if (result == FAILED_LOAD_SENTINEL) {
            CACHE_MISSES.incrementAndGet();
            return null;
        }
        CACHE_HITS.incrementAndGet();
        CACHE_ACCESS_TIMES.put(key, now);
        if (CACHE.size() > MAX_CACHE_SIZE) {
            evictOldest();
        }
        return result;
    }

    /// Loads an NBT file, converts to CompactTemplate, and caches it.
    @Nullable
    private static CompactTemplate tryLoadAndCompact(String fileName, Path nbtDir, ServerLevelAccessor level) {
        Path filePath = nbtDir.resolve(fileName);

        if (!Files.exists(filePath)) {
            Constants.LOG.warn("[Orchard] NBT file not found: {} - place it in the nbt/ config folder", filePath);
            return null;
        }

        try {
            long fileSize = Files.size(filePath);
            if (fileSize > Constants.MAX_NBT_FILE_SIZE) {
                Constants.LOG.error("[Orchard] NBT file too large: {} ({} bytes, max {} bytes)",
                        fileName, fileSize, Constants.MAX_NBT_FILE_SIZE);
                return null;
            }
            if (fileSize == 0) {
                Constants.LOG.error("[Orchard] NBT file is empty: {}", fileName);
                return null;
            }
        } catch (Exception e) {
            Constants.LOG.warn("[Orchard] Could not check file size for {}: {}", fileName, e.getMessage());
        }

        try (InputStream in = Files.newInputStream(filePath)) {
            CompoundTag nbt = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            StructureTemplate template = new StructureTemplate();
            template.load(level.registryAccess().lookupOrThrow(Registries.BLOCK), nbt);
            CompactTemplate compact = CompactTemplate.fromStructureTemplate(template);
            Constants.LOG.info("[Orchard] Loaded {} (size: {}, blocks: {})",
                    fileName, compact.getSize(), compact.getBlockCount());
            return compact;
        } catch (Exception e) {
            Constants.LOG.error("[Orchard] Failed to load {}: {}", fileName, e.getMessage());
            return null;
        }
    }

    private static void evictOldest() {
        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;
        for (Map.Entry<String, Long> entry : CACHE_ACCESS_TIMES.entrySet()) {
            if (entry.getValue() < oldestTime) {
                oldestTime = entry.getValue();
                oldestKey = entry.getKey();
            }
        }
        if (oldestKey != null) {
            CACHE.remove(oldestKey);
            CACHE_ACCESS_TIMES.remove(oldestKey);
            CACHE_EVICTIONS.incrementAndGet();
        }
    }

    public static String getCacheStats() {
        return "size=" + CACHE.size() + "/" + MAX_CACHE_SIZE
            + ", hits=" + CACHE_HITS.get()
            + ", misses=" + CACHE_MISSES.get()
            + ", evictions=" + CACHE_EVICTIONS.get();
    }

    /// Returns a multi-line stats string for the /orchard stats command.
    public static String[] getDetailedStats() {
        long hits = CACHE_HITS.get();
        long misses = CACHE_MISSES.get();
        long total = hits + misses;
        String hitRate = total > 0 ? String.format("%.1f%%", (double) hits / total * 100) : "n/a";
        return new String[]{
            "Cache size: " + CACHE.size() + "/" + MAX_CACHE_SIZE,
            "Cache hits: " + hits + " (" + hitRate + ")",
            "Cache misses: " + misses,
            "Cache evictions: " + CACHE_EVICTIONS.get(),
            "Total placements: " + TOTAL_PLACEMENTS.get(),
            "Placement index: " + PlacementIndex.getStats()
        };
    }

    public static void markPlaced(String nbtPath, BlockPos origin) {
        PlacementIndex.markPlaced(nbtPath, origin);
        TOTAL_PLACEMENTS.incrementAndGet();
    }

    public static boolean hasNearbyPlacement(String nbtPath, BlockPos origin, int radius) {
        return PlacementIndex.hasNearbyPlacement(nbtPath, origin, radius);
    }

    /// Scans a cylinder around origin for any log blocks. Used for spacing enforcement.
    public static boolean hasNearbyLog(ServerLevelAccessor level, BlockPos origin, int radius) {
        long r2 = (long) radius * radius;
        int originChunkX = origin.getX() >> 4;
        int originChunkZ = origin.getZ() >> 4;
        int chunkRadius = (radius >> 4) + 1;
        BlockPos.MutableBlockPos check = new BlockPos.MutableBlockPos();

        for (int cx = originChunkX - chunkRadius; cx <= originChunkX + chunkRadius; cx++) {
            for (int cz = originChunkZ - chunkRadius; cz <= originChunkZ + chunkRadius; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                int chunkMinX = cx << 4;
                int chunkMinZ = cz << 4;
                for (int dx = Math.max(-radius, chunkMinX - origin.getX());
                     dx <= Math.min(radius, chunkMinX + 15 - origin.getX()); dx++) {
                    for (int dz = Math.max(-radius, chunkMinZ - origin.getZ());
                         dz <= Math.min(radius, chunkMinZ + 15 - origin.getZ()); dz++) {
                        if ((long) dx * dx + (long) dz * dz > r2) continue;
                        for (int dy = -LOG_SCAN_DEPTH_BELOW; dy <= LOG_SCAN_HEIGHT_ABOVE; dy++) {
                            check.setWithOffset(origin, dx, dy, dz);
                            if (level.getBlockState(check).is(BlockTags.LOGS)) return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /// Checks the trunk column is free of bedrock and lava.
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

    /// Checks origin is on a valid open-air surface with sky above.
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

    /// Checks if too many columns are blocked (prevents placement inside villages etc).
    public static boolean isPlacementClear(ServerLevelAccessor level, BlockPos origin, Vec3i structureSize) {
        int radius = Math.min(Math.max(structureSize.getX(), structureSize.getZ()) / 2, 8);
        int r2 = radius * radius;

        int obstructedColumns = 0;
        int totalColumns = 0;
        int allowedObstructions = (int) Math.ceil(r2 * Math.PI * maxObstructedFraction);

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
                if (obstructedColumns > allowedObstructions) {
                    Constants.LOG.debug("[Orchard] Blocked placement at {} - {}/{} columns obstructed early (limit {}%)",
                            origin, obstructedColumns, totalColumns, (int) (maxObstructedFraction * 100));
                    return false;
                }
            }
        }

        if (totalColumns == 0) return true;

        boolean clear = (double) obstructedColumns / totalColumns <= maxObstructedFraction;

        if (!clear) {
            Constants.LOG.debug("[Orchard] Blocked placement at {} - {}/{} columns obstructed ({}%, limit {}%)",
                    origin, obstructedColumns, totalColumns,
                    (obstructedColumns * 100) / totalColumns,
                    (int) (maxObstructedFraction * 100));
        }
        return clear;
    }

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

    /// Moves origin down through replaceable blocks (snow layers etc) to find solid ground.
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

    // --- Feature interceptors (called from mixins) ---

    public static final AtomicBoolean TREE_FIRED_ONCE = new AtomicBoolean(false);
    public static final AtomicBoolean FUNGUS_FIRED_ONCE = new AtomicBoolean(false);
    public static final AtomicBoolean MUSHROOM_FIRED_ONCE = new AtomicBoolean(false);

    public static void logFirstInterception(AtomicBoolean flag, String message) {
        if (flag.compareAndSet(false, true)) {
            Constants.LOG.warn(message);
        }
    }

    /// Main interception logic shared by tree, fungus, and mushroom mixins.
    /// Uses compact template placement: one RNG call, tight loop, no processor overhead.
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

        CompactTemplate template = getOrLoad(def, level);
        if (template == null) {
            Constants.LOG.warn("[Orchard] Template null for {} - falling back to vanilla",
                    def.getNbtFileName());
            return;
        }

        Vec3i size = template.getSize();

        if (trunkCheck && !isTrunkClear(level, origin, size.getY())) {
            Constants.LOG.debug("[Orchard] Trunk not clear at {} - falling back to vanilla", origin);
            return;
        }

        if (!isPlacementClear(level, origin, size)) {
            Constants.LOG.debug("[Orchard] Placement not clear at {} - falling back to vanilla", origin);
            return;
        }

        Constants.LOG.debug("[Orchard] Placing {} at {}", def.getNbtFileName(), origin);

        CompactTemplate.place(template, level, origin, context.random(), def.getOriginYOffset());
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

    public static double getMaxObstructedFraction() {
        return maxObstructedFraction;
    }

    public static void setMaxObstructedFraction(double fraction) {
        maxObstructedFraction = Math.max(0.0, Math.min(1.0, fraction));
    }

    public static int getMaxGroundAdjust() {
        return MAX_GROUND_ADJUST;
    }
}
