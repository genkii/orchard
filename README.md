# Orchard

> **All the images featured in `gallery` are using our example config and the recommended mods**

## What is orchard

Simply said, orchard is a mod giving you the ability to replace any tree and fungi in Minecraft with a custom `.nbt` structure that you can create and customize however you like.

## Why You'll Love It

* **Endless variety** With orchard you have the ability to add anything you want and as much as you want to the Minecraft flora
* **Packs** Organise your trees into multiple packs and switch between them with one line of YAML
* **Modded trees too** Replace trees from other mods by referencing their configured features
* **Biome-specific trees** Make certain designs only appear in specific biomes.
* **Rare finds** Mark special designs as rare so they only appear occasionally.
* **Underground roots** Gives you the ability to offset trees so you can easily add roots to your trees
* **Zero client needed** Works entirely server-side. Join any server running Orchard and enjoy the enhanced worldgen without installing anything.
* **Hot reload** Easily reload the mod and apply changes using `/orchard reload`

## Supported Tree Types

Oak, birch, spruce, pine, jungle, acacia, dark oak, cherry, swamp, azalea, mangrove, plus nether fungi (warped, crimson) and huge mushrooms (red, brown). Any other tree from vanilla or mods can be targeted by its configured-feature id.

## Commands

All commands are run in-game and require operator permissions.

| Command                    | What it does                                                                            |
| -------------------------- | --------------------------------------------------------------------------------------- |
| `/orchard packs`           | Shows the active pack, available packs and how to switch                                |
| `/orchard status`          | Shows all registered tree definitions, whether their NBT files exist, and their weights |
| `/orchard stats`           | Shows runtime statistics (cache hits, placements, etc.)                                 |
| `/orchard list`            | Lists all loaded definitions with their full properties                                 |
| `/orchard create <name> <pos1> <pos2>` | Captures a region and saves it as an NBT file in `config/orchard/generated/` |
| `/orchard test <name>`     | Places a structure at your position (with optional rotation)                            |
| `/orchard what`            | Shows your current biome and which definitions would match                              |
| `/orchard find <query>`    | Searches through all definitions and NBT files by name                                  |
| `/orchard validate`        | Checks all your config files for parsing errors                                         |
| `/orchard nbt info <name>` | Shows you the tree and file size                                                        |
| `/orchard reload`          | Reloads the active pack from disk                                                       |
| `/orchard clearcache`      | Clears all cached `.nbt` files                                                          |

## Getting Started

Orchard makes it easy to get started adding custom trees to your world.

**Quick start (in-game):**

1. Build your tree in creative mode
2. Run `/orchard create my_tree <pos1> <pos2>` to capture it as an NBT file
3. Move the file from `config/orchard/generated/` to `config/orchard/packs/my-pack/nbt/`
4. Create a YAML config in `config/orchard/packs/my-pack/data/` (see below)
5. Run `/orchard reload` to apply

**Manual setup:**

Orchard loads content from packs:

```text
config/orchard/
├── orchard.yaml              <- global settings (which pack is active)
├── packs/
│   └── my-pack/              <- your own pack
│       ├── pack.yaml         <- pack metadata
│       ├── data/             <- .yaml tree definitions
│       └── nbt/              <- .nbt structures
├── bundled/                  <- built-in defaults, extracted on first run
                               (plain data/ + nbt/, not a pack)
└── generated/                <- /orchard create saves here (staging area)
```

When `pack.selected` is `auto` (the default), Orchard uses the first valid pack
from `packs/` in alphabetical order; if you have no packs it falls back to the
built-in defaults in `bundled/`, and if those are missing too (-TINY JAR),
vanilla worldgen stays untouched.

**Create A Pack**

1. Make the folder `config/orchard/packs/my-pack/` with a `pack.yaml` inside:

```yaml
name: my-pack
format: 0.1
version: 1.0.0
description: My custom Orchard trees
```

2. Add a definition in `data/trees.yaml`:

```yaml
- nbt: mytree.nbt
  tree_type: oak
```

3. Put `mytree.nbt` into the pack's `nbt/` folder and run `/orchard reload`.

To activate a specific pack instead of auto-picking, set it in
`config/orchard/orchard.yaml`:

```yaml
pack:
  selected: my-pack
```

### More Options

Definitions support more fields:

```yaml
- nbt: mytree.nbt
  tree_type: oak
  weight: 2
  min_spacing: 5
  biomes: ["#minecraft:is_forest"]
  origin_y_offset: -2
  rare: false
```

| Field             | What it does                                    |
| ----------------- | ----------------------------------------------- |
| `weight`          | Higher = more common (default: 1)               |
| `min_spacing`     | Minimum distance between your custom trees      |
| `biomes`          | Limit which biomes your tree spawns in          |
| `origin_y_offset` | Shift the tree down to create underground roots |
| `rare`            | Only 2.5% chance to spawn                       |

See [docs/config-reference.md](docs/config-reference.md) for every field,
including modded-tree overrides via configured-feature ids.

### Multiple Variants

Add multiple entries with the same `tree_type` and Orchard will randomly pick between them. Give them different weights to control how often each appears.

## Premade Configs

Premade configs are available on our Discord server

## Other Versions

Orchard supports both **NeoForge** and **Fabric** for Minecraft 26.3.

* Full JARs include the built-in defaults and are the recommended download.
* `-TINY` JARs ship without the built-in defaults (smaller download, nothing else differs).

## Mod Recommendations

We recommend using the mod with some other mods for the best experience

* [Lithosphere](https://modrinth.com/datapack/lithosphere)
* [Wilderness](https://modrinth.com/datapack/wilderness_)
* [Stony Cliffs Are Cool](https://modrinth.com/datapack/stony-cliffs-are-cool)
* [FallingTree](https://modrinth.com/mod/fallingtree)
* [Solas Shader](https://modrinth.com/shader/solas-shader)
* [Concurrent Chunk Management Engine (NeoForge)](https://modrinth.com/mod/c2me-neoforge)

## License

See [LICENSE](LICENSE) (MIT).

## Building

```bash
./gradlew build          # development JARs without the bundled pack
./bundle.sh              # full + -TINY distribution JARs into bundled/
./bundle.sh --tiny       # only -TINY JARs
```

**Releases:** pushing a git tag `v<version>` (e.g. `v0.6.0-BETA`, matching
`gradle.properties`) runs the full bundle in CI and attaches all JARs -
full and `-TINY` - to a GitHub Release automatically.
