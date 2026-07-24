# Orchard - Documentation

Orchard replaces vanilla Minecraft trees during world generation with custom NBT structure files, giving forests far more visual variety than vanilla provides.

## Directory Structure

On first launch, the mod creates these directories inside your Minecraft config folder:

```
config/
  orchard/
    nbt/          Place your .nbt structure files here
    data/         Place your .json config files here
```

## Quick Start

1. Export your tree structure as an NBT file from Minecraft (using a structure block or another tool).
2. Place the `.nbt` file in `config/orchard/nbt/`.
3. Create a `.json` file in `config/orchard/data/` describing when to use it.
4. Launch Minecraft - the mod loads all JSON configs at startup.

### Minimal Example

**File:** `config/orchard/data/oak_trees.json`
```json
[
  { "nbt": "oak1", "tree_type": "oak" },
  { "nbt": "oak2", "tree_type": "oak" },
  { "nbt": "oak3", "tree_type": "oak" }
]
```

This replaces all vanilla oak trees with a random selection from your three NBT structures, each randomly rotated.

### Adding Roots

To give trees custom underground roots, add an `origin_y_offset` to shift the structure downward:

```json
{ "nbt": "oak_with_roots", "tree_type": "oak", "origin_y_offset": -4 }
```

This places 4 blocks of the NBT underground, so roots appear in the terrain while the canopy sits at the correct height. See [NBT Structures](nbt-structures.md) for details.

## How It Works

During world generation, when Minecraft tries to place a tree, a mixin intercepts the call and checks if any registered definition matches the tree type and biome. If a match is found, the vanilla tree is replaced with your NBT structure. If no match is found, vanilla generation proceeds normally.

The mod supports three types of features:
- **Overworld trees** (oak, birch, spruce, jungle, acacia, dark oak, cherry, etc.)
- **Nether fungi** (warped and crimson huge fungi)
- **Large mushrooms** (red and brown huge mushrooms)

## Loading Behavior

- All JSON files in `config/orchard/data/` are loaded at server startup.
- Each file can contain a single JSON object or an array of objects.
- NBT files are loaded lazily on first use and cached in memory.
- If an NBT file is missing, vanilla tree generation is used as fallback.
- Run `/orchard reload` in-game to hot-reload definitions without restarting.
- Run `/orchard clearcache` to force NBT templates to reload from disk.
