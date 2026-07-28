package de.minehackers.orchard;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

final class TerrainPreservingProcessor extends StructureProcessor {

    static final TerrainPreservingProcessor INSTANCE = new TerrainPreservingProcessor(-1, -1);

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
            LevelReader level, BlockPos offset, BlockPos pivot,
            StructureTemplate.StructureBlockInfo originalInfo,
            StructureTemplate.StructureBlockInfo currentInfo,
            StructurePlaceSettings settings) {
        if (currentInfo.state().isAir()) return null;

        BlockPos pos = currentInfo.pos();
        if (chunkX >= 0 && chunkZ >= 0) {
            int dx = Math.abs((pos.getX() >> 4) - chunkX);
            int dz = Math.abs((pos.getZ() >> 4) - chunkZ);
            if (dx > 1 || dz > 1) return null;
        }

        BlockState existing = level.getBlockState(pos);
        if (existing.is(Blocks.BEDROCK)) return null;
        if (existing.getFluidState().is(FluidTags.LAVA)) return null;

        return currentInfo;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return StructureProcessorType.NOP;
    }
}
