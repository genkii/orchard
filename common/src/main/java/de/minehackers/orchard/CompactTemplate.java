package de.minehackers.orchard;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/// A tree template flattened ahead of time: resolved palette plus pre-rotated
/// block offsets for all four rotations. Placing is then just a tight loop over
/// flat data - no NBT parsing, no settings objects, no processor callbacks.
final class CompactTemplate {

    /// One block of the template: offset from the origin plus its palette index.
    record BlockEntry(int dx, int dy, int dz, int paletteIndex) {}

    private final Vec3i size;
    private final BlockState[] palette;
    private final List<BlockEntry>[] blocksByRotation;

    /// Rotates a template-space position around the template's center point.
    private static int[] rotatePos(int x, int y, int z, int hx, int hz, Rotation rotation) {
        return switch (rotation) {
            case NONE                  -> new int[]{ x, y,  z };
            case CLOCKWISE_90          -> new int[]{ hz - 1 - z, y,  x - hx + hz - 1 };
            case CLOCKWISE_180        -> new int[]{ hx - 1 - x, y, hz - 1 - z };
            case COUNTERCLOCKWISE_90  -> new int[]{ z - hz + hx, y, hz - 1 - x };
        };
    }

    CompactTemplate(Vec3i size, BlockState[] palette, List<BlockEntry>[] blocksByRotation) {
        this.size = size;
        this.palette = palette;
        this.blocksByRotation = blocksByRotation;
    }

    /// Converts a loaded StructureTemplate into the compact form: builds the
    /// palette and writes out pre-rotated offset lists for all four rotations.
    static CompactTemplate fromStructureTemplate(StructureTemplate template) {
        Vec3i size = template.getSize();
        int hx = size.getX() / 2;
        int hz = size.getZ() / 2;

        // filterBlocks with a null block filter and transform=false leaves positions in template space.
        var blocks = template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), (net.minecraft.world.level.block.Block) null, false);

        // StructureTemplate gives us fully resolved block states rather than palette
        // indices, so we build our own palette of unique states as we go.
        var paletteList = new ArrayList<BlockState>();
        var stateToIndex = new java.util.IdentityHashMap<BlockState, Integer>();

        @SuppressWarnings("unchecked")
        List<BlockEntry>[] byRotation = new List[4];
        for (int r = 0; r < 4; r++) {
            byRotation[r] = new ArrayList<>(blocks.size());
        }

        for (StructureTemplate.StructureBlockInfo info : blocks) {
            BlockState state = info.state();

            // Skip air - nothing to place and the spot is air already.
            if (state.isAir()) continue;

            Integer idx = stateToIndex.get(state);
            if (idx == null) {
                idx = paletteList.size();
                paletteList.add(state);
                stateToIndex.put(state, idx);
            }

            BlockPos pos = info.pos();
            int bx = pos.getX();
            int by = pos.getY();
            int bz = pos.getZ();

            for (int r = 0; r < 4; r++) {
                Rotation rot = Rotation.values()[r];
                int[] rotated = rotatePos(bx, by, bz, hx, hz, rot);
                byRotation[r].add(new BlockEntry(
                        rotated[0] - hx, rotated[1], rotated[2] - hz, idx));
            }
        }

        // Freeze everything into immutable copies.
        var paletteArray = paletteList.toArray(BlockState[]::new);
        @SuppressWarnings("unchecked")
        List<BlockEntry>[] trimmed = new List[4];
        for (int r = 0; r < 4; r++) {
            trimmed[r] = List.copyOf(byRotation[r]);
        }

        return new CompactTemplate(size, paletteArray, trimmed);
    }

    /// Places the template at origin with a random rotation. This is the worldgen
    /// hot path: one RNG call, tight loop, terrain preservation inlined below.
    static void place(CompactTemplate template, ServerLevelAccessor level,
                      BlockPos origin, net.minecraft.util.RandomSource random, int originYOffset) {
        int rotationIndex = random.nextInt(4);
        place(template, level, origin, originYOffset, Rotation.values()[rotationIndex]);
    }

    /// Same, but with the rotation chosen by the caller.
    static void place(CompactTemplate template, ServerLevelAccessor level,
                      BlockPos origin, int originYOffset, Rotation rotation) {
        List<BlockEntry> blocks = template.blocksByRotation[rotation.ordinal()];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        // Never write outside the build height - templates near the world
        // ceiling would otherwise crash chunk generation. maxY is exclusive.
        int minY = level.getMinY();
        int maxY = level.getMaxY();

        for (int i = 0; i < blocks.size(); i++) {
            BlockEntry entry = blocks.get(i);
            int y = origin.getY() + originYOffset + entry.dy();
            if (y < minY || y >= maxY) continue;

            pos.setWithOffset(origin, entry.dx(), originYOffset + entry.dy(), entry.dz());

            // Terrain preservation, inlined: never overwrite bedrock or lava.
            BlockState existing = level.getBlockState(pos);
            if (existing.is(Blocks.BEDROCK)) continue;
            if (existing.getFluidState().is(FluidTags.LAVA)) continue;

            level.setBlock(pos, template.palette[entry.paletteIndex()], 3);
        }
    }

    Vec3i getSize() {
        return size;
    }

    int getBlockCount() {
        int total = 0;
        for (List<BlockEntry> list : blocksByRotation) {
            total += list.size();
        }
        return total / 4;
    }
}
