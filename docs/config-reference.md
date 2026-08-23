# Config Reference

Orchard loads content from **packs**. A pack is a folder containing metadata,
tree definitions and NBT structures:

```text
config/orchard/
├── orchard.yaml              <- global settings
├── packs/
│   └── my-pack/
│       ├── pack.yaml         <- pack metadata (required)
│       ├── data/*.yaml       <- tree definitions
│       └── nbt/*.nbt         <- structures
└── bundled/                  <- built-in defaults, extracted on first run
                               (plain data/ + nbt/, not a pack)
```

## orchard.yaml

Created automatically on first run:

```yaml
# Which pack to activate.
#   auto     - first valid pack from packs/ in alphabetical order,
#              falling back to the bundled pack
#   <name>   - a specific pack by folder name; if it cannot load,
#              Orchard falls back to auto behaviour
pack:
  selected: auto

# Maximum fraction of obstructed blocks tolerated when placing NBT trees.
placement:
  max_obstructed_fraction: 0.1

# Chance for definitions marked `rare: true` to spawn.
rarity:
  rare_pool_probability: 0.025
```

## pack.yaml

Every pack needs this file:

```yaml
name: my-pack            # display name (defaults to the folder name)
format: 0.1              # pack format - 0.1 is the only supported one
version: 1.0.0           # your pack's version (informational)
description: My trees    # shown by /orchard packs (informational)
```

Packs with an unsupported format are skipped with a log message.

## Pack Selection

`/orchard packs` shows what is available. On startup and after `/orchard reload`
Orchard picks the active pack like this:

1. If `pack.selected` names a specific pack, try that pack.
2. Otherwise pick the first valid pack from `packs/` (alphabetical order).
3. If no user pack exists or it fails to load, use the built-in defaults
   extracted to `bundled/` (plain `data/` + `nbt/`, no `pack.yaml` needed).
4. If even the defaults are missing (-TINY JAR), vanilla worldgen runs untouched.

Problems never crash the game: broken files inside a pack are logged and
skipped, and a pack that cannot load at all falls back as described above.

## Data Files

Place `.yaml` files in a pack's `data/` folder. Each file can contain one
definition mapping or a list of definitions.

### Minimal Example

```yaml
nbt: my_oak_tree.nbt
tree_type: oak
```

---

## Definition Fields

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `nbt` | string | yes | - | NBT filename (placed in the pack's `nbt/` folder) |
| `tree_type` | string/object/array | no* | - | Which tree(s) to replace |
| `fungus_type` | string/array | no* | - | Which fungus/fungi to replace |
| `mushroom_type` | string/array | no* | - | Which mushroom(s) to replace |
| `weight` | int | no | 1 | Higher = more likely to be picked |
| `rare` | boolean | no | false | Only spawns ~2.5% of the time |
| `min_spacing` | int | no | 0 | Minimum blocks between this and other trees |
| `origin_y_offset` | int | no | 0 | Shift the structure up/down on placement |
| `min_y` | int | no | none | Lowest Y level this can spawn at |
| `max_y` | int | no | none | Highest Y level this can spawn at |
| `biomes` | string/object/array | no | - | Restrict to specific biomes (omit = all biomes) |
| `dimensions` | string array | no | - | Restrict to specific dimensions (omit = all) |
| `valid_floor` | string | no | - | Required block type under the tree |

\* At least one of `tree_type`, `fungus_type`, or `mushroom_type` is required.

Unknown fields produce a warning in the log but do not stop the pack.

---

## tree_type

Can be a simple name, a configured-feature id, an object with structural
filters, or an array of any of these (matches if any entry matches).

### Simple Names

| Value | Matches |
|-------|---------|
| `oak` | Oak tree |
| `fancy_oak` | Fancy (large) oak |
| `birch` | Birch tree |
| `spruce` | Spruce tree |
| `spruce_only` | Spruce-only variant |
| `pine` | Pine tree |
| `mega_pine` | Mega pine |
| `mega_spruce` | Mega spruce |
| `jungle` | Jungle tree |
| `jungle_small` | Small jungle tree |
| `jungle_mega` | Mega jungle tree |
| `jungle_bush` | Jungle bush |
| `acacia` | Acacia tree |
| `dark_oak` | Dark oak tree |
| `cherry` | Cherry tree |
| `swamp` | Swamp tree |
| `azalea` | Azalea tree |
| `mangrove` | Mangrove tree |

### Feature Ids (vanilla + modded)

Use any configured-feature identifier to target trees from vanilla **or other
mods**:

```yaml
- nbt: fancy_replacement.nbt
  tree_type: minecraft:fancy_oak

- nbt: mod_tree_replacement.nbt
  tree_type: some-mod:some_tree
```

Orchard identifies which configured feature fired by comparing the feature's
configuration against the registry at server start. Ids that do not exist are
reported once in the log but never prevent the pack from loading.

### Object Form

Match by foliage placer, trunk placer, or trunk block. All fields are optional
and combined with AND.

| Field | Type | Description |
|-------|------|-------------|
| `foliage` | string | Foliage placer type (see below) |
| `trunk` | string | Trunk placer type (see below) |
| `trunk_block` | string | Specific trunk block ID (e.g. `minecraft:oak_log`) |

This form works for **any** modded tree whose structure uses vanilla-style
placers, without knowing its feature id.

#### foliage values

`blob`, `fancy`, `spruce`, `pine`, `mega_pine`, `mega_jungle`, `bush`,
`acacia`, `dark_oak`, `cherry`, `random_spread`

#### trunk values

`dark_oak`, `forking`, `giant`, `mega_jungle`, `upwards_branching`

### Object Example

```yaml
- nbt: custom_blob_oak.nbt
  tree_type:
    foliage: blob
    trunk_block: minecraft:oak_log
```

---

## fungus_type

| Value | Matches |
|-------|---------|
| `warped` | Warped fungus |
| `crimson` | Crimson fungus |
| `any` | Any fungus |

Feature ids (`minecraft:crimson_fungus`, `some-mod:some_fungus`) work here too.
Can also be an array of values (matches any in the array).

---

## mushroom_type

| Value | Matches |
|-------|---------|
| `red` | Red mushroom |
| `brown` | Brown mushroom |
| `any` | Any mushroom |

Feature ids work here too. Can also be an array of values.

---

## biomes

Can be a string, object, array, or omitted entirely.

### Simple Names

Vanilla biome ids without namespace: `plains`, `sunflower_plains`, `meadow`,
`snowy_plains`, `forest`, `flower_forest`, `birch_forest`,
`old_growth_birch_forest`, `dark_forest`, `windswept_forest`, `taiga`,
`snowy_taiga`, `old_growth_pine_taiga`, `old_growth_spruce_taiga`, `jungle`,
`sparse_jungle`, `bamboo_jungle`, `savanna`, `savanna_plateau`,
`windswept_savanna`, `windswept_hills`, `windswept_gravelly_hills`, `grove`,
`swamp`, `mangrove_swamp`, `cherry_grove`, `mushroom_fields`, `lush_caves`,
`crimson_forest`, `warped_forest`, `nether_wastes`, `soul_sand_valley`,
`basalt_deltas`.

Namespaced ids also work, including modded biomes: `some-mod:some_biome`.

### Biome Tags (prefix with `#`)

Shorthand tags resolve against `minecraft`: `#is_forest`, `#is_taiga`,
`#is_jungle`, `#is_savanna`, `#is_badlands`, `#is_ocean`, `#is_river`,
`#is_beach`, `#is_overworld`, `#is_nether`, `#is_end`, plus Orchard groups
(`#snowy_spruce_biomes`, `#non_snowy_taiga`, `#pine_biomes`).

Any vanilla or modded tag can be used with its full id: `#minecraft:is_hill`,
`#somemod:wonders`.

### Object Form

| Field | Type | Description |
|-------|------|-------------|
| `any_of` | array | Match if ANY of the entries match (OR) |
| `all_of` | array | Match if ALL of the entries match (AND) |
| `not` | string/object | Match if the inner filter does NOT match |

Combinators can be nested arbitrarily.

### Array Form

An array of biome names/tags is treated as `any_of`.

### Examples

```yaml
biomes: forest

biomes: "#is_forest"

biomes:
  any_of: [forest, dark_forest, "#is_taiga"]

biomes:
  all_of:
    - "#is_overworld"
    - not: mushroom_fields

biomes: some-mod:some_biome
```

---

## dimensions

Array of dimension IDs. Omit to match all dimensions.

| Value | Matches |
|-------|---------|
| `minecraft:overworld` | Overworld |
| `minecraft:the_nether` | Nether |
| `minecraft:the_end` | End |

Modded dimensions work too (e.g. `modname:dimension_id`). Unknown dimension
ids are reported at server start but do not invalidate the pack.

```yaml
dimensions: [minecraft:overworld, minecraft:the_nether]
```

---

## valid_floor

Requires the block directly below the tree to be a specific type.

| Value | Matches |
|-------|---------|
| `dirt` | Any dirt-family block |
| `nylium` | Any nylium block |

---

## Complete Example

```yaml
- nbt: big_oak.nbt
  tree_type: fancy_oak
  weight: 3
  min_spacing: 6
  biomes: [forest, dark_forest]
  dimensions: [minecraft:overworld]

- nbt: small_birch.nbt
  tree_type: birch
  weight: 1
  origin_y_offset: -1
  biomes: "#is_forest"
  valid_floor: dirt

- nbt: rare_jungle.nbt
  tree_type: [jungle, jungle_small]
  rare: true
  min_y: 60
  max_y: 200
  biomes:
    any_of: [jungle, bamboo_jungle]

- nbt: modded_tree.nbt
  tree_type: some-mod:some_tree
  weight: 2
```

---

## JavaScript Definitions (Orchard: Scripting addon)

Install the optional **Orchard: Scripting** addon and you can put `.js` files
into a pack's `data/` folder alongside YAML files. A script calls `define(...)`
once per definition; the fields are exactly the same as in YAML:

```js
// data/trees.js
define({
    nbt: "big_oak.nbt",
    tree_type: "fancy_oak",
    weight: 3,
    min_spacing: 6,
    biomes: [orchard.biome("forest"), orchard.biome("dark_forest")]
});

// scripts are real programs - generate variants with loops:
for (var i = 1; i <= 5; i++) {
    define({
        nbt: "birch" + i + ".nbt",
        tree_type: "birch",
        biomes: orchard.tag("#is_forest"),
        rare: (i === 5)
    });
}
```

Helper namespace:

| Function | Returns |
|----------|---------|
| `orchard.biome(name)` | Biome id, namespaced if needed (`forest` → `minecraft:forest`) |
| `orchard.tag(name)` | Biome tag id (`#is_forest` → `#minecraft:is_forest`) |
| `orchard.block(id)` | Block id, namespaced if needed |
| `orchard.log(msg)` | Writes to the server log |

Rules:

* Scripts run once at pack load (and on `/orchard reload`) - never during world
  generation.
* They execute in a sandbox: JavaScript standard library only, no access to
  Java classes, hard 5-second time limit per script.
* Definitions produced by scripts go through exactly the same validation as
  YAML entries; a broken script is logged and skipped.

---

## Migrating From JSON Configs (pre-V1)

* Move `config/orchard/data/*.json` into `config/orchard/packs/<your-pack>/data/`
  and rename them to `.yaml` (the fields are identical, YAML is just a different
  syntax).
* Move `config/orchard/nbt/*` into your pack's `nbt/` folder.
* Add a `pack.yaml` to the pack folder.
* Your old `data/` and `nbt/` folders are no longer read; the bundled pack now
  lives at `config/orchard/bundled/`.
