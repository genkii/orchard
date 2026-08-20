package de.minehackers.orchard;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;

/// Chunk-indexed spatial tracking for placed tree positions.
/// Inner maps are partitioned by NBT path so queries skip irrelevant placements.
/// Auto-prunes chunks older than 30 minutes every 4096 placements.
final class PlacementIndex {

    private PlacementIndex() {}

    private static final int PLACEMENTS_BETWEEN_PRUNES = 4096;
    private static final long CHUNK_MAX_AGE_MS = 30L * 60 * 1000;
    private static final AtomicInteger placementCounter = new AtomicInteger();

    /// chunkKey -> (nbtPath -> set of packed block positions)
    private static final ConcurrentMap<Long, ConcurrentHashMap<String, Set<Long>>> CHUNK_INDEX =
            new ConcurrentHashMap<>(64);
    private static final ConcurrentMap<Long, Long> CHUNK_ACCESS_TIMES =
            new ConcurrentHashMap<>(64);

    public static void markPlaced(String nbtPath, BlockPos origin) {
        long chunkKey = chunkKey(origin);
        CHUNK_INDEX.computeIfAbsent(chunkKey, k -> new ConcurrentHashMap<>(4))
                .computeIfAbsent(nbtPath, k -> ConcurrentHashMap.newKeySet())
                .add(origin.asLong());
        CHUNK_ACCESS_TIMES.put(chunkKey, System.currentTimeMillis());

        if (placementCounter.incrementAndGet() >= PLACEMENTS_BETWEEN_PRUNES) {
            placementCounter.set(0);
            pruneStaleChunks(System.currentTimeMillis());
        }
    }

    /// Checks if the same NBT path was placed within the given radius.
    public static boolean hasNearbyPlacement(String nbtPath, BlockPos origin, int radius) {
        int chunkRadius = (radius >> 4) + 1;
        int originChunkX = origin.getX() >> 4;
        int originChunkZ = origin.getZ() >> 4;
        long r2 = (long) radius * radius;

        for (int cx = originChunkX - chunkRadius; cx <= originChunkX + chunkRadius; cx++) {
            for (int cz = originChunkZ - chunkRadius; cz <= originChunkZ + chunkRadius; cz++) {
                long chunkKey = chunkKey(cx, cz);
                ConcurrentHashMap<String, Set<Long>> chunk = CHUNK_INDEX.get(chunkKey);
                if (chunk == null) continue;

                Set<Long> positions = chunk.get(nbtPath);
                if (positions == null) continue;

                for (long packed : positions) {
                    long dx = BlockPos.getX(packed) - origin.getX();
                    long dz = BlockPos.getZ(packed) - origin.getZ();
                    if (dx * dx + dz * dz <= r2) return true;
                }
            }
        }
        return false;
    }

    public static void clear() {
        CHUNK_INDEX.clear();
        CHUNK_ACCESS_TIMES.clear();
        placementCounter.set(0);
    }

    /// Returns a "chunks=N, tracked_placements=N" summary string.
    public static String getStats() {
        int chunks = CHUNK_INDEX.size();
        int placements = 0;
        for (ConcurrentHashMap<String, Set<Long>> chunk : CHUNK_INDEX.values()) {
            for (Set<Long> positions : chunk.values()) {
                placements += positions.size();
            }
        }
        return "chunks=" + chunks + ", tracked_placements=" + placements;
    }

    /// Evicts chunks whose last access is older than the max age.
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
            Constants.LOG.debug("[Orchard] Pruned {} stale chunk(s) from placement index.", pruned);
        }
    }

    private static long chunkKey(BlockPos pos) {
        return chunkKey(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
}
