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

/**
 * Structure processor that prevents placement of blocks that would overwrite bedrock, lava, or air.
 */
final class TerrainPreservingProcessor extends StructureProcessor {

    static final TerrainPreservingProcessor INSTANCE = new TerrainPreservingProcessor();

    /**
     * Processes a block during structure placement, rejecting blocks that would overwrite
     * bedrock, lava, or that are already air.
     *
     * @param level        the world reader
     * @param offset       the offset position
     * @param pivot        the pivot position
     * @param originalInfo the original block info
     * @param currentInfo  the current block info
     * @param settings     the placement settings
     * @return the current info if allowed, or {@code null} to skip placement
     */
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level, BlockPos offset, BlockPos pivot,
            StructureTemplate.StructureBlockInfo originalInfo,
            StructureTemplate.StructureBlockInfo currentInfo,
            StructurePlaceSettings settings) {
        if (currentInfo.state().isAir()) return null;

        BlockState existing = level.getBlockState(currentInfo.pos());
        if (existing.is(Blocks.BEDROCK)) return null;
        if (existing.getFluidState().is(FluidTags.LAVA)) return null;

        return currentInfo;
    }

    /**
     * Returns the processor type, using the no-op type to avoid serialization overhead.
     *
     * @return the structure processor type
     */
    @Override
    protected StructureProcessorType<?> getType() {
        return StructureProcessorType.NOP;
    }
}
