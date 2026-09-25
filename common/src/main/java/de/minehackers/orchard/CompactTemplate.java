package de.minehackers.orchard;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/// Pre-flattened tree template with resolved palette and pre-rotated offsets for fast placement.
public final class CompactTemplate {

    /// Single template block: offset from origin plus palette index.
    record BlockEntry(int dx, int dy, int dz, int paletteIndex) {}

    /// One palette layer with resolved states and pre-rotated offset lists.
    static final class Layer {

        final BlockState[] palette;
        final List<BlockEntry>[] blocksByRotation;

        Layer(BlockState[] palette, List<BlockEntry>[] blocksByRotation) {
            this.palette = palette;
            this.blocksByRotation = blocksByRotation;
        }
    }

    private final Vec3i size;
    final Layer[] layers;

    CompactTemplate(Vec3i size, Layer[] layers) {
        this.size = size;
        this.layers = layers;
    }

    /// Reads a saved structure file into palette layers and positioned blocks.
    static CompactTemplate fromNbt(CompoundTag root, HolderGetter<Block> blockRegistry) {
        ListTag sizeTag = root.getListOrEmpty("size");
        Vec3i size = new Vec3i(
                sizeTag.getIntOr(0, 0),
                sizeTag.getIntOr(1, 0),
                sizeTag.getIntOr(2, 0));

        ListTag blocksTag = root.getListOrEmpty("blocks");

        // Either a single palette or several random-variant layers - the same
        // split vanilla's loader makes between "palette" and "palettes".
        List<Layer> parsed = new ArrayList<>(1);
        var variantLayers = root.getList("palettes");
        if (variantLayers.isPresent()) {
            ListTag variants = variantLayers.get();
            for (int i = 0; i < variants.size(); i++) {
                parsed.add(parseLayer(variants.getListOrEmpty(i), blocksTag, blockRegistry, size));
            }
        } else {
            parsed.add(parseLayer(root.getListOrEmpty("palette"), blocksTag, blockRegistry, size));
        }

        return new CompactTemplate(size, parsed.toArray(new Layer[0]));
    }

    /// Rotates a template-space position into an offset relative to the footprint-center origin.
    private static int[] rotatedOffset(int x, int z, int sizeX, int sizeZ, Rotation rotation) {
        return switch (rotation) {
            case NONE                -> new int[]{ x - sizeX / 2,
                                                   z - sizeZ / 2 };
            case CLOCKWISE_90        -> new int[]{ sizeZ - 1 - z - sizeZ / 2,
                                                   x - sizeX / 2 };
            case CLOCKWISE_180       -> new int[]{ sizeX - 1 - x - sizeX / 2,
                                                   sizeZ - 1 - z - sizeZ / 2 };
            case COUNTERCLOCKWISE_90 -> new int[]{ z - sizeZ / 2,
                                                   sizeX - 1 - x - sizeX / 2 };
        };
    }

    /// Resolves one palette layer and builds its four pre-rotated offset lists.
    private static Layer parseLayer(ListTag paletteTag, ListTag blocksTag,
                                    HolderGetter<Block> blockRegistry, Vec3i size) {
        BlockState[] palette = new BlockState[paletteTag.size()];
        for (int i = 0; i < palette.length; i++) {
            palette[i] = NbtUtils.readBlockState(blockRegistry, paletteTag.getCompoundOrEmpty(i));
        }

        int sizeX = size.getX();
        int sizeZ = size.getZ();

        @SuppressWarnings("unchecked")
        List<BlockEntry>[] byRotation = new List[4];
        for (int r = 0; r < 4; r++) {
            byRotation[r] = new ArrayList<>();
        }

        Rotation[] rotations = Rotation.values();
        for (int i = 0; i < blocksTag.size(); i++) {
            CompoundTag entry = blocksTag.getCompoundOrEmpty(i);
            ListTag posTag = entry.getListOrEmpty("pos");

            int stateIndex = entry.getIntOr("state", 0);
            if (stateIndex < 0 || stateIndex >= palette.length) continue;
            BlockState state = palette[stateIndex];

            // Skip air - nothing to place and the spot is air already.
            if (state.isAir()) continue;

            int bx = posTag.getIntOr(0, 0);
            int by = posTag.getIntOr(1, 0);
            int bz = posTag.getIntOr(2, 0);

            for (int r = 0; r < 4; r++) {
                int[] offset = rotatedOffset(bx, bz, sizeX, sizeZ, rotations[r]);
                byRotation[r].add(new BlockEntry(offset[0], by, offset[1], stateIndex));
            }
        }

        @SuppressWarnings("unchecked")
        List<BlockEntry>[] trimmed = new List[4];
        for (int r = 0; r < 4; r++) {
            trimmed[r] = List.copyOf(byRotation[r]);
        }

        return new Layer(palette, trimmed);
    }

    /// Counts non-air blocks in a raw structure tag without building a template.
    public static int countNonAirBlocks(CompoundTag root, HolderGetter<Block> blockRegistry) {
        // All layers reference the same positions, so the first decides the count.
        ListTag paletteTag = root.getList("palettes")
                .flatMap(variants -> variants.getList(0))
                .orElseGet(() -> root.getListOrEmpty("palette"));

        BlockState[] palette = new BlockState[paletteTag.size()];
        for (int i = 0; i < palette.length; i++) {
            palette[i] = NbtUtils.readBlockState(blockRegistry, paletteTag.getCompoundOrEmpty(i));
        }

        int count = 0;
        ListTag blocksTag = root.getListOrEmpty("blocks");
        for (int i = 0; i < blocksTag.size(); i++) {
            CompoundTag entry = blocksTag.getCompoundOrEmpty(i);
            int stateIndex = entry.getIntOr("state", 0);
            if (stateIndex >= 0 && stateIndex < palette.length && !palette[stateIndex].isAir()) {
                count++;
            }
        }
        return count;
    }

    /// Places the template at origin with random layer and rotation, preserving terrain.
    static void place(CompactTemplate template, ServerLevelAccessor level,
                      BlockPos origin, RandomSource random, int originYOffset) {
        Layer layer = template.layers[random.nextInt(template.layers.length)];
        List<BlockEntry> blocks = layer.blocksByRotation[random.nextInt(Rotation.values().length)];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        // Never write outside the build height - templates near the world
        // ceiling would otherwise crash chunk generation. maxY is exclusive.
        int minY = level.getMinY();
        int maxY = level.getMaxY();

        // During chunk generation only a 3x3 window of chunks around the
        // origin is guaranteed writable. Anything beyond that gets dropped -
        // same limit the old structure-processor version enforced.
        int originChunkX = origin.getX() >> 4;
        int originChunkZ = origin.getZ() >> 4;

        for (int i = 0; i < blocks.size(); i++) {
            BlockEntry entry = blocks.get(i);
            int y = origin.getY() + originYOffset + entry.dy();
            if (y < minY || y >= maxY) continue;

            pos.setWithOffset(origin, entry.dx(), originYOffset + entry.dy(), entry.dz());

            if (Math.abs((pos.getX() >> 4) - originChunkX) > 1
                    || Math.abs((pos.getZ() >> 4) - originChunkZ) > 1) {
                continue;
            }

            // Terrain preservation, inlined: never overwrite bedrock or lava.
            BlockState existing = level.getBlockState(pos);
            if (existing.is(Blocks.BEDROCK)) continue;
            if (existing.getFluidState().is(FluidTags.LAVA)) continue;

            level.setBlock(pos, layer.palette[entry.paletteIndex()], 3);
        }
    }

    Vec3i getSize() {
        return size;
    }

    int getBlockCount() {
        // All layers reference the same positions, so the first one decides
        // the count. Summing across layers would multiply by the variant
        // count even though a placement uses exactly one.
        if (layers.length == 0) return 0;
        Layer layer = layers[0];
        int total = 0;
        for (List<BlockEntry> list : layer.blocksByRotation) {
            total += list.size();
        }
        return total / Math.max(1, layer.blocksByRotation.length);
    }
}
