package de.minehackers.orchard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/// Covers CompactTemplate's structure-file parsing, with focus on the
/// multi-palette variant path ("palettes") that none of the bundled tree
/// files use. Tags are built synthetically to mirror what vanilla's
/// StructureTemplate.save writes.
class CompactTemplateTest {

    private static HolderGetter<Block> blockRegistry;

    @BeforeAll
    static void bootStrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        blockRegistry = BuiltInRegistries.BLOCK;
    }

    private static ListTag sizeTag(int x, int y, int z) {
        ListTag size = new ListTag();
        size.add(IntTag.valueOf(x));
        size.add(IntTag.valueOf(y));
        size.add(IntTag.valueOf(z));
        return size;
    }

    private static ListTag posTag(int x, int y, int z) {
        ListTag pos = new ListTag();
        pos.add(IntTag.valueOf(x));
        pos.add(IntTag.valueOf(y));
        pos.add(IntTag.valueOf(z));
        return pos;
    }

    private static CompoundTag blockEntry(int x, int y, int z, int stateIndex) {
        CompoundTag entry = new CompoundTag();
        entry.put("pos", posTag(x, y, z));
        entry.putInt("state", stateIndex);
        return entry;
    }

    private static ListTag defaultPalette() {
        // Index 0: log, index 1: leaves, index 2: air (must be skipped).
        ListTag palette = new ListTag();
        palette.add(NbtUtils.writeBlockState(Blocks.OAK_LOG.defaultBlockState()));
        palette.add(NbtUtils.writeBlockState(Blocks.OAK_LEAVES.defaultBlockState()));
        palette.add(NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState()));
        return palette;
    }

    /// Three positioned blocks: trunk, canopy, plus one pointing at the air
    /// palette slot and one pointing past the palette entirely.
    private static ListTag blocksTag() {
        ListTag blocks = new ListTag();
        blocks.add(blockEntry(1, 0, 1, 0)); // oak_log, center of 3x3
        blocks.add(blockEntry(0, 1, 1, 1)); // oak_leaves
        blocks.add(blockEntry(2, 1, 2, 2)); // air - parser must skip
        blocks.add(blockEntry(2, 1, 0, 9)); // out of range - parser must skip
        return blocks;
    }

    @Test
    void singlePaletteProducesOneLayer() {
        CompoundTag root = new CompoundTag();
        root.put("size", sizeTag(3, 2, 3));
        root.put("palette", defaultPalette());
        root.put("blocks", blocksTag());

        CompactTemplate template = CompactTemplate.fromNbt(root, blockRegistry);

        assertEquals(1, template.layers.length);
        assertEquals(2, template.getBlockCount());

        CompactTemplate.Layer layer = template.layers[0];
        assertTrue(layer.palette[0].is(Blocks.OAK_LOG));
        assertTrue(layer.palette[1].is(Blocks.OAK_LEAVES));

        for (int r = 0; r < 4; r++) {
            assertEquals(2, layer.blocksByRotation[r].size());
        }
    }

    @Test
    void multiplePalettesProduceIndependentLayers() {
        ListTag secondPalette = new ListTag();
        secondPalette.add(NbtUtils.writeBlockState(Blocks.BIRCH_LOG.defaultBlockState()));
        secondPalette.add(NbtUtils.writeBlockState(Blocks.BIRCH_LEAVES.defaultBlockState()));

        ListTag variants = new ListTag();
        variants.add(defaultPalette());
        variants.add(secondPalette);

        CompoundTag root = new CompoundTag();
        root.put("size", sizeTag(3, 2, 3));
        root.put("palettes", variants);
        root.put("blocks", blocksTag());

        CompactTemplate template = CompactTemplate.fromNbt(root, blockRegistry);

        assertEquals(2, template.layers.length);
        // Block count stays per-layer - layers are alternatives, never summed.
        assertEquals(2, template.getBlockCount());

        CompactTemplate.Layer oak = template.layers[0];
        CompactTemplate.Layer birch = template.layers[1];
        assertTrue(oak.palette[0].is(Blocks.OAK_LOG));
        assertTrue(birch.palette[0].is(Blocks.BIRCH_LOG));

        // Both layers share positions and rotations.
        for (int r = 0; r < 4; r++) {
            assertEquals(oak.blocksByRotation[r].size(), birch.blocksByRotation[r].size());
        }
    }

    @Test
    void rotationOffsetsStayCenteredAndRigid() {
        CompoundTag root = new CompoundTag();
        root.put("size", sizeTag(3, 2, 3));
        root.put("palette", defaultPalette());
        root.put("blocks", blocksTag());

        CompactTemplate template = CompactTemplate.fromNbt(root, blockRegistry);
        CompactTemplate.Layer layer = template.layers[0];

        // Trunk sits at (1,0,1), the center of the 3x3 footprint: every
        // rotation must keep it at offset (0,0).
        for (int r = 0; r < 4; r++) {
            long atCenter = layer.blocksByRotation[r].stream()
                    .filter(e -> e.dx() == 0 && e.dz() == 0)
                    .count();
            assertEquals(1, atCenter, "rotation " + r + " lost the center block");
        }

        // The canopy at (0,1,1) rotates around the center without changing
        // its distance to it: north -> west -> south -> east -> north.
        int[][] expectedOffsets = {{-1, 0}, {0, -1}, {1, 0}, {0, 1}};
        for (int r = 0; r < 4; r++) {
            final int rotation = r;
            boolean found = layer.blocksByRotation[r].stream()
                    .anyMatch(e -> e.dx() == expectedOffsets[rotation][0]
                            && e.dz() == expectedOffsets[rotation][1]);
            assertTrue(found, "rotation " + r + " misplaced the canopy block");
        }
    }

    @Test
    void countNonAirMatchesParsedBlockCount() {
        CompoundTag root = new CompoundTag();
        root.put("size", sizeTag(3, 2, 3));
        root.put("palette", defaultPalette());
        root.put("blocks", blocksTag());

        assertEquals(2, CompactTemplate.countNonAirBlocks(root, blockRegistry));

        ListTag variants = new ListTag();
        variants.add(defaultPalette());
        root.remove("palette");
        root.put("palettes", variants);
        assertEquals(2, CompactTemplate.countNonAirBlocks(root, blockRegistry));
    }
}
