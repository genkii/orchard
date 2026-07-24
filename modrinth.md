# Orchard

**Orchard** transforms Minecraft's world generation by replacing vanilla trees, nether fungi, and large mushrooms with custom-built structures. Tired of every oak forest looking the same? Want your warped forests to feel truly alien? Orchard makes it happen.

## What It Does

Every tree that spawns in your world can now look completely different. Build your own tree designs in-game using structure blocks, WorldEdit, or Axiom, export them as `.nbt` files, and Orchard takes care of the rest. Oaks can have gnarled twisted trunks, spruces can tower with layered branches, jungle trees can twist into massive cathedral-like canopies -- the only limit is what you can build.

## Why You'll Love It

- **Endless variety** -- Add as many variants of each tree type as you want. One biome can have dozens of unique tree designs spawning naturally.
- **Biome-specific trees** -- Make certain designs only appear in specific biomes. Cherry trees with hanging vines in warm forests, snow-covered pines in frozen taigas, twisted dark oaks in swamps.
- **Rare finds** -- Mark special designs as rare so they only appear occasionally. Imagine stumbling upon a massive ancient oak hidden deep in the forest.
- **Underground roots** -- Offset designs downward so massive root systems spawn beneath the surface, creating natural cave entrances and underground forests.
- **Zero client needed** -- Works entirely server-side. Join any server running Orchard and enjoy the enhanced worldgen without installing anything.
- **Hot reload** -- Change your tree designs and configs on the fly with `/orchard reload`. No server restart needed.

## Supported Tree Types

Oak, birch, spruce, pine, jungle, acacia, dark oak, cherry, swamp, azalea, mangrove, plus nether fungi (warped, crimson) and huge mushrooms (red, brown).

## Getting Started

### 1. Build Your Tree

Open a Minecraft world in creative mode. Use structure blocks, WorldEdit, or any building tool to create your tree design. Once finished, grab the structure as an `.nbt` file.

### 2. Set Up the Folders

Create these folders in your Minecraft instance:

```
config/orchard/nbt/    ← your .nbt files go here
config/orchard/data/   ← your config files go here
```

### 3. Create a Config

Create a file called `mytree.json` in `config/orchard/data/` with this content:

```json
{
  "nbt": "mytree.nbt",
  "tree_type": "oak"
}
```

This tells Orchard to replace all vanilla oak trees with your design.

### 4. Reload

Run `/orchard reload` in-game and your custom trees will start spawning.

### More Options

Want to control where and how often your trees appear? Add more fields to your config:

```json
{
  "nbt": "mytree.nbt",
  "tree_type": "oak",
  "weight": 2,
  "min_spacing": 5,
  "biomes": ["#minecraft:is_forest"],
  "origin_y_offset": -2,
  "rare": false
}
```

| Field | What it does |
|-------|-------------|
| `weight` | Higher = more common (default: 1) |
| `min_spacing` | Minimum distance between your custom trees |
| `biomes` | Limit which biomes your tree spawns in |
| `origin_y_offset` | Shift the tree down to create underground roots |
| `rare` | Only 2.5% chance to spawn |

### Multiple Variants

Add multiple config files with the same `tree_type` and Orchard will randomly pick between them. Give them different weights to control how often each appears.

## Commands

All commands are run in-game and require operator permissions.

| Command | What it does |
|---------|-------------|
| `/orchard reload` | Reloads all config files from disk without restarting the server |
| `/orchard status` | Shows all registered tree definitions, whether their NBT files exist, and their weights |
| `/orchard list` | Lists all loaded definitions with their full properties |
| `/orchard test <name>` | Places a structure at your position, centered on you -- great for testing designs |
| `/orchard place <name>` | Places a structure at your exact position -- useful for precise placement |
| `/orchard what` | Shows your current biome and which definitions would match |
| `/orchard find <query>` | Searches through all definitions and NBT files by name |
| `/orchard validate` | Checks all your configs for errors like missing files or bad formatting |
| `/orchard nbt info <name>` | Shows the file size and dimensions of a specific NBT structure |
| `/orchard clearcache` | Clears the cached NBT templates from memory |

## Who Is This For

- **Server owners** who want their worlds to feel alive and unique
- **Modpack makers** looking to enhance worldgen without adding gameplay changes
- **Builders** who want their hard work to appear naturally in the world
- **Anyone** tired of seeing the same oak tree 500 times
