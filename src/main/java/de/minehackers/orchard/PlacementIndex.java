package de.minehackers.orchard;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.core.BlockPos;

/**
 * Chunk-indexed spatial tracking for placed tree positions.
 * <p>
 * Uses {@link ConcurrentHashMap} for thread-safe O(1) chunk lookups during
 * spacing checks. Entries are keyed by packed chunk coordinates with a nested
 * map of block positions to NBT file paths.
 * <p>
 * To prevent unbounded memory growth on long-running servers, the index
 * supports periodic pruning via {@link #pruneStaleChunks(long)}. Chunks
 * whose last access time exceeds the given age threshold are evicted.
 * Pruning is triggered automatically after a configurable number of
 * placements ({@link #PLACEMENTS_BETWEEN_PRUNES}).
 */
final class PlacementIndex {

    private PlacementIndex() {}

    /** Number of placements between automatic pruning cycles. */
    private static final int PLACEMENTS_BETWEEN_PRUNES = 4096;

    /** Maximum age of a chunk entry in milliseconds before it is eligible for pruning (30 minutes). */
    private static final long CHUNK_MAX_AGE_MS = 30L * 60 * 1000;

    /** Counter for triggering periodic pruning. */
    private static volatile int placementCounter = 0;

    /**
     * Two-level index: outer key = packed chunk key, inner key = packed block position,
     * value = NBT file path string.
     */
    private static final ConcurrentMap<Long, ConcurrentHashMap<Long, String>> CHUNK_INDEX =
            new ConcurrentHashMap<>(64);

    /** Tracks the last access time (epoch millis) for each chunk key. */
    private static final ConcurrentMap<Long, Long> CHUNK_ACCESS_TIMES =
            new ConcurrentHashMap<>(64);

    /**
     * Records a placement at the given origin for the specified NBT path.
     * <p>
     * Also updates the chunk's access time and triggers pruning periodically.
     *
     * @param nbtPath the NBT template file path
     * @param origin  the world position where the structure was placed
     */
    public static void markPlaced(String nbtPath, BlockPos origin) {
        long chunkKey = chunkKey(origin);
        CHUNK_INDEX.computeIfAbsent(chunkKey, k -> new ConcurrentHashMap<>(4))
                .put(origin.asLong(), nbtPath);
        CHUNK_ACCESS_TIMES.put(chunkKey, System.currentTimeMillis());

        if (++placementCounter >= PLACEMENTS_BETWEEN_PRUNES) {
            placementCounter = 0;
            pruneStaleChunks(System.currentTimeMillis());
        }
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
        int chunkRadius = (radius >> 4) + 1;
        int originChunkX = origin.getX() >> 4;
        int originChunkZ = origin.getZ() >> 4;
        long r2 = (long) radius * radius;

        for (int cx = originChunkX - chunkRadius; cx <= originChunkX + chunkRadius; cx++) {
            for (int cz = originChunkZ - chunkRadius; cz <= originChunkZ + chunkRadius; cz++) {
                long chunkKey = chunkKey(cx, cz);
                ConcurrentHashMap<Long, String> chunk = CHUNK_INDEX.get(chunkKey);
                if (chunk == null) continue;

                for (Map.Entry<Long, String> entry : chunk.entrySet()) {
                    long packed = entry.getKey();
                    String entryPath = entry.getValue();
                    if (!entryPath.equals(nbtPath)) continue;

                    int px = BlockPos.getX(packed);
                    int pz = BlockPos.getZ(packed);
                    long dx = px - origin.getX();
                    long dz = pz - origin.getZ();
                    if (dx * dx + dz * dz <= r2) return true;
                }
            }
        }
        return false;
    }

    /**
     * Clears all tracked placements from the index.
     */
    public static void clear() {
        CHUNK_INDEX.clear();
        CHUNK_ACCESS_TIMES.clear();
        placementCounter = 0;
    }

    /**
     * Removes chunks whose last access time exceeds {@link #CHUNK_MAX_AGE_MS}.
     * <p>
     * This prevents unbounded memory growth on long-running servers where
     * previously explored chunks are no longer being generated.
     *
     * @param now the current time in epoch milliseconds
     */
    static void pruneStaleChunks(long now) {
        long cutoff = now - CHUNK_MAX_AGE_MS;
        int pruned = 0;

        Iterator<Map.Entry<Long, Long>> timeIt = CHUNK_ACCESS_TIMES.entrySet().iterator();
        while (timeIt.hasNext()) {
            Map.Entry<Long, Long> entry = timeIt.next();
            if (entry.getValue() < cutoff) {
                long ck = entry.getKey();
                CHUNK_INDEX.remove(ck);
                timeIt.remove();
                pruned++;
            }
        }

        if (pruned > 0) {
            Orchard.LOGGER.debug("[Orchard] Pruned {} stale chunk(s) from placement index.", pruned);
        }
    }

    /**
     * Computes a chunk key from a world position.
     *
     * @param pos the world position
     * @return the packed chunk key
     */
    private static long chunkKey(BlockPos pos) {
        return chunkKey(pos.getX() >> 4, pos.getZ() >> 4);
    }

    /**
     * Computes a chunk key from chunk coordinates.
     *
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return the packed chunk key
     */
    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
}
