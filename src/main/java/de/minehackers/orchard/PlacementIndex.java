package de.minehackers.orchard;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.core.BlockPos;

/**
 * Chunk-indexed spatial tracking for placed tree positions. Uses ConcurrentHashMap
 * for thread-safe O(1) chunk lookups during spacing checks.
 */
final class PlacementIndex {

    private PlacementIndex() {}

    private static final ConcurrentMap<Long, ConcurrentHashMap<Long, String>> CHUNK_INDEX =
            new ConcurrentHashMap<>(64);

    /**
     * Records a placement at the given origin for the specified NBT path.
     *
     * @param nbtPath the NBT template file path
     * @param origin  the world position where the structure was placed
     */
    public static void markPlaced(String nbtPath, BlockPos origin) {
        long chunkKey = chunkKey(origin);
        CHUNK_INDEX.computeIfAbsent(chunkKey, k -> new ConcurrentHashMap<>(4))
                .put(origin.asLong(), nbtPath);
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

                for (var entry : chunk.entrySet()) {
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
