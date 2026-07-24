# Orchard

Replaces vanilla Minecraft trees during world generation with custom NBT structure files, giving forests far more visual variety than vanilla provides.

Trees are randomly rotated at placement time so that even identical NBT files produce visually diverse forests.

## Features

- Replace any overworld tree, nether fungus, or huge mushroom with custom NBT structures
- Random 90-degree rotation at placement for natural-looking forests
- Biome-specific replacements (e.g., different trees in dark forest vs plains)
- Weighted random selection with normal (97.5%) and rare (2.5%) pools
- Minimum spacing control to prevent overcrowding
- Underground root placement via `origin_y_offset`
- Terrain-preserving processor (never overwrites bedrock, lava, or air)
- Wall-overlap detection near artificial structures (fortresses, villages)
- Config-driven via JSON files (no code changes needed)

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.235+
- Java 21

## Quick Start

1. Place `.nbt` structure files in `config/orchard/nbt/`
2. Create a `.json` config in `config/orchard/data/`:
   ```json
   { "nbt": "my_oak", "tree_type": "oak" }
   ```
3. Restart the server/client

### Example: Multiple Oak Variants

```json
[
  { "nbt": "oak1", "tree_type": "oak" },
  { "nbt": "oak2", "tree_type": "oak" },
  { "nbt": "oak3", "tree_type": "oak" }
]
```

Each vanilla oak tree will be randomly replaced with one of your NBT structures, rotated to a random orientation.

## Directory Structure

On first launch, the mod creates these directories inside your Minecraft config folder:

```
config/
  orchard/
    nbt/          Place your .nbt structure files here
    data/         Place your .json config files here
```

## How It Works

During world generation, when Minecraft tries to place a tree, a mixin intercepts the call and checks if any registered definition matches the tree type and biome. If a match is found, the vanilla tree is replaced with your NBT structure (randomly rotated). If no match is found, vanilla generation proceeds normally.

The mod supports three types of features:
- **Overworld trees** (oak, birch, spruce, jungle, acacia, dark oak, cherry, etc.)
- **Nether fungi** (warped and crimson huge fungi)
- **Large mushrooms** (red and brown huge mushrooms)

## Loading Behavior

- All JSON files in `config/orchard/data/` are loaded at server startup
- Each file can contain a single JSON object or an array of objects
- NBT files are loaded lazily on first use and cached in memory
- If an NBT file is missing, vanilla tree generation is used as fallback
- Run `/orchard reload` in-game to hot-reload definitions without restarting
- Run `/orchard clearcache` to force NBT templates to reload from disk

## Documentation

| Document | Description |
|----------|-------------|
| [Configuration Reference](docs/CONFIGURATION.md) | Full JSON config option reference |
| [Tree Types](docs/tree-types.md) | Overworld tree type matchers |
| [Fungus Types](docs/fungus-types.md) | Nether fungus type matchers |
| [Mushroom Types](docs/mushroom-types.md) | Huge mushroom type matchers |
| [Biome Filters](docs/biome-filters.md) | Biome filter syntax and values |
| [NBT Structures](docs/nbt-structures.md) | How to create and author NBT files |
| [Spacing & Rarity](docs/spacing-and-rarity.md) | Spacing, weight, and rare pool mechanics |
| [Examples](docs/examples.md) | Config examples for common use cases |
| [Troubleshooting](docs/troubleshooting.md) | Common issues and fixes |
| [Migration Guide](docs/migration.md) | Migrating from the Fabric CustomTree mod |

## Building

```bash
./gradlew build
```

The mod JAR will be in `build/libs/`.

## License

See [TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt) for the NeoForged MDK template license.
