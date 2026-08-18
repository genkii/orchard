package de.minehackers.orchard;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/// Keeps existing terrain intact when placing NBT structures.
/// Won't overwrite bedrock, lava, or air blocks.
final class TerrainPreservingProcessor implements StructureProcessor {

    static final TerrainPreservingProcessor INSTANCE = new TerrainPreservingProcessor(-1, -1);

    private static final MapCodec<TerrainPreservingProcessor> CODEC = MapCodec.unit(() -> INSTANCE);

    private final int chunkX;
    private final int chunkZ;

    private TerrainPreservingProcessor(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    static TerrainPreservingProcessor forChunk(BlockPos origin) {
        return new TerrainPreservingProcessor(origin.getX() >> 4, origin.getZ() >> 4);
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level, BlockPos targetPosition, BlockPos referencePos,
            BlockPos templateRelativePos,
            StructureTemplate.StructureBlockInfo processedBlockInfo,
            StructurePlaceSettings settings) {
        if (processedBlockInfo.state().isAir()) return null;

        BlockPos pos = processedBlockInfo.pos();
        if (chunkX >= 0 && chunkZ >= 0) {
            int dx = Math.abs((pos.getX() >> 4) - chunkX);
            int dz = Math.abs((pos.getZ() >> 4) - chunkZ);
            if (dx > 1 || dz > 1) return null;
        }

        BlockState existing = level.getBlockState(pos);
        if (existing.is(Blocks.BEDROCK)) return null;
        if (existing.getFluidState().is(FluidTags.LAVA)) return null;

        return processedBlockInfo;
    }

    @Override
    public MapCodec<? extends StructureProcessor> codec() {
        return CODEC;
    }
}
