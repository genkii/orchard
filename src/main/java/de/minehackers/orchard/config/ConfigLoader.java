package de.minehackers.orchard.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.HugeFungusConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import de.minehackers.orchard.Orchard;
import de.minehackers.orchard.OrchardDefinition;

/**
 * Loads orchard definition files from the {@code orchard/data} directory
 * inside the game config folder.
 * <p>
 * Each JSON file can contain a single definition object or an array of
 * definitions. All definitions from all {@code .json} files are merged
 * into a single list at startup.
 * <p>
 * Supported fields per definition:
 * <ul>
 *   <li>{@code nbt} (required) – NBT structure filename</li>
 *   <li>{@code tree_type}, {@code fungus_type}, {@code mushroom_type} – feature matchers (at least one required)</li>
 *   <li>{@code biomes} – biome filter</li>
 *   <li>{@code dimensions} – dimension filter (array of dimension type resource locations)</li>
 *   <li>{@code min_y}, {@code max_y} – Y range restrictions</li>
 *   <li>{@code weight}, {@code min_spacing}, {@code rare}, {@code origin_y_offset} – placement tuning</li>
 * </ul>
 *
 * @see OrchardDefinition
 * @see TreeTypeParser
 * @see BiomeFilterParser
 */
public final class ConfigLoader {

    private ConfigLoader() {}

    private static final java.util.regex.Pattern SAFE_NBT_FILENAME =
            java.util.regex.Pattern.compile("^[a-zA-Z0-9_\\-\\.]+(\\.nbt)?$");

    /**
     * Validates that an NBT filename is safe (no path traversal characters).
     * Accepts names with or without the {@code .nbt} extension; the Builder
     * appends it later if missing.
     *
     * @param fileName the filename to validate
     * @return true if the filename is safe
     */
    private static boolean isSafeNbtFileName(String fileName) {
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return false;
        }
        return SAFE_NBT_FILENAME.matcher(fileName).matches();
    }

    /**
     * Loads all JSON definition files from the data directory.
     *
     * @param configDir the game config directory root
     * @return a list of parsed orchard definitions
     */
    public static List<OrchardDefinition> loadAll(Path configDir) {
        Path dataDir = configDir.resolve("orchard").resolve("data");
        Path nbtDir = configDir.resolve("orchard").resolve("nbt");

        List<OrchardDefinition> definitions = new ArrayList<>();
        AtomicInteger filesLoaded = new AtomicInteger(0);
        AtomicInteger filesFailed = new AtomicInteger(0);

        if (!Files.isDirectory(dataDir)) {
            Orchard.LOGGER.warn("[Orchard] Data directory does not exist: {}", dataDir);
            return definitions;
        }

        try {
            Files.walkFileTree(dataDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".json")) {
                        try {
                            definitions.addAll(loadFile(file, nbtDir));
                            filesLoaded.incrementAndGet();
                        } catch (Exception e) {
                            filesFailed.incrementAndGet();
                            Orchard.LOGGER.error("[Orchard] Failed to load {}: {}",
                                    dataDir.relativize(file), e.getMessage());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            Orchard.LOGGER.error("[Orchard] Failed to scan data directory: {}", e.getMessage());
        }

        if (filesFailed.get() > 0) {
            Orchard.LOGGER.warn("[Orchard] Loaded {} definition(s) from config. {} file(s) failed to load.",
                    definitions.size(), filesFailed.get());
        } else {
            Orchard.LOGGER.info("[Orchard] Loaded {} definition(s) from config ({} file(s)).",
                    definitions.size(), filesLoaded.get());
        }
        return definitions;
    }

    private static List<OrchardDefinition> loadFile(Path jsonFile, Path nbtDir) throws IOException {
        List<OrchardDefinition> definitions = new ArrayList<>();

        try (Reader reader = Files.newBufferedReader(jsonFile)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root.isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray()) {
                    if (element.isJsonObject()) {
                        definitions.add(parseDefinition(element.getAsJsonObject(), nbtDir));
                    }
                }
            } else if (root.isJsonObject()) {
                definitions.add(parseDefinition(root.getAsJsonObject(), nbtDir));
            }
        }

        return definitions;
    }

    private static OrchardDefinition parseDefinition(JsonObject obj, Path nbtDir) {
        String nbtFile = obj.get("nbt").getAsString();

        if (!isSafeNbtFileName(nbtFile)) {
            throw new IllegalArgumentException(
                    "Invalid NBT filename: '" + nbtFile + "' – must contain only alphanumeric, "
                    + "hyphens, underscores, or dots, and must not contain path separators or '..'");
        }

        int weight = obj.has("weight") ? obj.get("weight").getAsInt() : 1;
        int minSpacing = obj.has("min_spacing") ? obj.get("min_spacing").getAsInt() : 0;
        boolean rare = obj.has("rare") && obj.get("rare").getAsBoolean();
        int originYOffset = obj.has("origin_y_offset") ? obj.get("origin_y_offset").getAsInt() : 0;
        int minY = obj.has("min_y") ? obj.get("min_y").getAsInt() : 0;
        int maxY = obj.has("max_y") ? obj.get("max_y").getAsInt() : 0;

        Predicate<TreeConfiguration> worldGen = null;
        Predicate<HugeFungusConfiguration> fungus = null;
        Predicate<HugeMushroomFeatureConfiguration> mushroom = null;

        if (obj.has("tree_type")) {
            worldGen = TreeTypeParser.parseTreeType(obj.get("tree_type"));
        }

        if (obj.has("fungus_type")) {
            fungus = FungusTypeParser.parseFungusType(obj.get("fungus_type"));
        }

        if (obj.has("mushroom_type")) {
            mushroom = MushroomTypeParser.parseMushroomType(obj.get("mushroom_type"));
        }

        if (worldGen == null && fungus == null && mushroom == null) {
            throw new IllegalArgumentException(
                    "Definition for '" + nbtFile + "' must have at least one of "
                    + "'tree_type', 'fungus_type', or 'mushroom_type'");
        }

        Predicate<Holder<Biome>> biomes = null;
        if (obj.has("biomes")) {
            biomes = BiomeFilterParser.parseBiomeFilter(obj.get("biomes"));
        }

        Set<ResourceKey<Level>> dimensions = parseDimensions(obj);

        OrchardDefinition.Builder builder = OrchardDefinition.forNbt(nbtFile, nbtDir);

        if (worldGen != null) builder.worldGen(worldGen);
        if (fungus != null) builder.fungusWorldGen(fungus);
        if (mushroom != null) builder.mushroomWorldGen(mushroom);
        if (biomes != null) builder.biomes(biomes);
        if (!dimensions.isEmpty()) builder.dimensions(dimensions);

        builder.minSpacing(minSpacing);
        builder.weight(weight);
        if (rare) builder.rare();
        builder.originYOffset(originYOffset);
        if (minY != 0 || maxY != 0) {
            builder.minY(minY);
            builder.maxY(maxY);
        }

        return builder.build();
    }

    /**
     * Parses the {@code dimensions} field from a JSON definition.
     * <p>
     * Accepts a JSON array of dimension resource location strings,
     * e.g. {@code ["minecraft:overworld", "minecraft:the_nether"]}.
     *
     * @param obj the JSON definition object
     * @return a set of dimension keys (empty if not specified)
     */
    private static Set<ResourceKey<Level>> parseDimensions(JsonObject obj) {
        Set<ResourceKey<Level>> dims = new HashSet<>();
        if (!obj.has("dimensions")) return dims;

        JsonElement element = obj.get("dimensions");
        if (element.isJsonArray()) {
            for (JsonElement e : element.getAsJsonArray()) {
                if (e.isJsonPrimitive()) {
                    String dimId = e.getAsString();
                    dims.add(ResourceKey.create(
                            net.minecraft.core.registries.Registries.DIMENSION,
                            ResourceLocation.parse(dimId)));
                }
            }
        }
        return dims;
    }
}
