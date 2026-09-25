package de.minehackers.orchard.config;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;
import de.minehackers.orchard.OrchardDefinition;
import de.minehackers.orchard.pack.DynamicReferences;
import de.minehackers.orchard.pack.PackLoadException;

/// Turns raw definitions into ready-to-run OrchardDefinitions - the "resolve"
/// stage of the V1 pipeline (load, parse, validate, resolve, active). Everything
/// resolvable eagerly (blocks, matchers, predicates) is built up front so world
/// gen never parses or does registry lookups at runtime. Identifiers living in
/// dynamic registries (biomes, dimensions, features) are collected
/// for existence validation at server start; an unknown id only costs a
/// warning, never the whole pack.
public final class DefinitionCompiler {

    /// Keeps NBT file names sane: rejects path traversal and odd characters.
    private static final Pattern SAFE_NBT_FILENAME =
            Pattern.compile("^[a-zA-Z0-9_\\-\\.]+\\.nbt$");

    private DefinitionCompiler() {}

    /// What compile() hands back: the runtime definition plus any deferred references.
    public record Compiled(OrchardDefinition definition,
                           @Nullable DynamicReferences.Builder references) {}

    public static Compiled compile(RawDefinition raw, Path nbtDirectory) {
        String nbtFile = sanitizeNbtFileName(raw.nbt());

        if (!raw.hasAnySelector()) {
            throw new PackLoadException("definition '" + raw.nbt()
                    + "' must have at least one of 'tree_type', 'fungus_type' or 'mushroom_type'");
        }

        DynamicReferences.Builder refs = new DynamicReferences.Builder();
        String where = "definition '" + raw.nbt() + "'";

        OrchardDefinition.Builder builder = OrchardDefinition.forNbt(nbtFile, nbtDirectory);

        if (raw.hasTreeSelector()) {
            builder.worldGen(TreeTypeParser.parse(raw.treeType(), refs, where));
        }
        if (raw.hasFungusSelector()) {
            builder.fungusWorldGen(FungusTypeParser.parse(raw.fungusType(), refs, where));
        }
        if (raw.hasMushroomSelector()) {
            builder.mushroomWorldGen(MushroomTypeParser.parse(raw.mushroomType(), refs, where));
        }

        if (raw.biomes() != null) {
            Predicate<Holder<Biome>> biomes =
                    BiomeFilterParser.parse(raw.biomes(), refs, where);
            builder.biomes(biomes);
        }

        if (raw.dimensions() != null && !raw.dimensions().isEmpty()) {
            Set<ResourceKey<Level>> dims = new HashSet<>(raw.dimensions().size());
            for (int i = 0; i < raw.dimensions().size(); i++) {
                String dimId = raw.dimensions().get(i);
                Identifier id = TreeTypeParser.parseIdentifier(dimId,
                        where + " dimensions[" + i + "]");
                refs.addDimension(ResourceKey.create(Registries.DIMENSION, id));
                dims.add(ResourceKey.create(Registries.DIMENSION, id));
            }
            builder.dimensions(dims);
        }

        builder.minSpacing(raw.minSpacing());
        builder.weight(raw.weight());
        if (raw.rare()) builder.rare();
        builder.originYOffset(raw.originYOffset());
        if (raw.hasYRange()) {
            builder.minY(raw.minY());
            builder.maxY(raw.maxY());
        }
        if ("dirt".equals(raw.validFloor())) {
            builder.onDirt();
        } else if ("nylium".equals(raw.validFloor())) {
            builder.onNylium();
        }

        return new Compiled(builder.build(), refs);
    }

    private static String sanitizeNbtFileName(String raw) {
        String name = raw.endsWith(".nbt") ? raw : raw + ".nbt";
        if (!SAFE_NBT_FILENAME.matcher(name).matches()) {
            throw new PackLoadException("field 'nbt': invalid file name '" + raw
                    + "' - use alphanumeric characters, hyphens, underscores or dots");
        }
        return name;
    }
}
