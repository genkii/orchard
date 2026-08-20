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

    static final TerrainPreservingProcessor INSTANCE = new TerrainPreservingProcessor();

    private static final MapCodec<TerrainPreservingProcessor> CODEC = MapCodec.unit(() -> INSTANCE);

    private TerrainPreservingProcessor() {}

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level, BlockPos targetPosition, BlockPos referencePos,
            BlockPos templateRelativePos,
            StructureTemplate.StructureBlockInfo processedBlockInfo,
            StructurePlaceSettings settings) {
        if (processedBlockInfo.state().isAir()) return null;

        BlockState existing = level.getBlockState(processedBlockInfo.pos());
        if (existing.is(Blocks.BEDROCK)) return null;
        if (existing.getFluidState().is(FluidTags.LAVA)) return null;

        return processedBlockInfo;
    }

    @Override
    public MapCodec<? extends StructureProcessor> codec() {
        return CODEC;
    }
}
