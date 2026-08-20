# Config Reference

Place JSON files in `config/orchard/data/`. Each file can contain one definition or an array of definitions.

A JSON schema is available at [`docs/config-schema.json`](config-schema.json) for editor autocomplete and validation. To use it in VS Code, add to your settings:

```json
{
  "json.schemas": [
    {
      "fileMatch": ["**/orchard/data/*.json"],
      "url": "https://raw.githubusercontent.com/MineHackers/orchard/main/docs/config-schema.json"
    }
  ]
}
```

## Minimal Example

```json
{
  "nbt": "my_oak_tree.nbt",
  "tree_type": "oak"
}
```

---

## Definition Fields

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `nbt` | string | yes | - | NBT filename (placed in `config/orchard/nbt/`) |
| `tree_type` | string/object/array | no* | - | Which vanilla tree to replace |
| `fungus_type` | string/array | no* | - | Which vanilla fungus to replace |
| `mushroom_type` | string/array | no* | - | Which vanilla mushroom to replace |
| `weight` | int | no | 1 | Higher = more likely to be picked |
| `rare` | boolean | no | false | Only spawns ~2.5% of the time |
| `min_spacing` | int | no | 0 | Minimum blocks between this and other trees |
| `origin_y_offset` | int | no | 0 | Shift the structure up/down on placement |
| `min_y` | int | no | 0 | Lowest Y level this can spawn at |
| `max_y` | int | no | 0 | Highest Y level this can spawn at (0 = no limit) |
| `biomes` | string/object/array | no | - | Restrict to specific biomes (omit = all biomes) |
| `dimensions` | string array | no | - | Restrict to specific dimensions (omit = all) |
| `valid_floor` | string | no | - | Required block type under the tree |

\* At least one of `tree_type`, `fungus_type`, or `mushroom_type` is required.

---

## tree_type

Can be a simple string, an object with sub-filters, or an array of either.

### Simple Strings

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

### Object Form

Match by foliage placer, trunk placer, or trunk block. All fields are optional and combined with AND.

| Field | Type | Description |
|-------|------|-------------|
| `foliage` | string | Foliage placer type (see below) |
| `trunk` | string | Trunk placer type (see below) |
| `trunk_block` | string | Specific trunk block ID (e.g. `minecraft:oak_log`) |

#### foliage values

`blob`, `fancy`, `spruce`, `pine`, `mega_pine`, `mega_jungle`, `bush`, `acacia`, `dark_oak`, `cherry`, `random_spread`

#### trunk values

`dark_oak`, `forking`, `giant`, `mega_jungle`, `upwards_branching`

### Object Example

```json
{
  "nbt": "custom_blob_oak.nbt",
  "tree_type": {
    "foliage": "blob",
    "trunk_block": "minecraft:oak_log"
  }
}
```

---

## fungus_type

| Value | Matches |
|-------|---------|
| `warped` | Warped fungus |
| `crimson` | Crimson fungus |
| `any` | Any fungus |

Can also be an array of values (matches any in the array).

---

## mushroom_type

| Value | Matches |
|-------|---------|
| `red` | Red mushroom |
| `brown` | Brown mushroom |
| `any` | Any mushroom |

Can also be an array of values (matches any in the array).

---

## biomes

Can be a string, object, array, or omitted entirely.

### Simple String

| Value | Matches |
|-------|---------|
| `plains` | Plains |
| `sunflower_plains` | Sunflower Plains |
| `meadow` | Meadow |
| `snowy_plains` | Snowy Plains |
| `forest` | Forest |
| `flower_forest` | Flower Forest |
| `birch_forest` | Birch Forest |
| `old_growth_birch_forest` | Old Growth Birch Forest |
| `dark_forest` | Dark Forest |
| `windswept_forest` | Windswept Forest |
| `taiga` | Taiga |
| `snowy_taiga` | Snowy Taiga |
| `old_growth_pine_taiga` | Old Growth Pine Taiga |
| `old_growth_spruce_taiga` | Old Growth Spruce Taiga |
| `jungle` | Jungle |
| `sparse_jungle` | Sparse Jungle |
| `bamboo_jungle` | Bamboo Jungle |
| `savanna` | Savanna |
| `savanna_plateau` | Savanna Plateau |
| `windswept_savanna` | Windswept Savanna |
| `windswept_hills` | Windswept Hills |
| `windswept_gravelly_hills` | Windswept Gravelly Hills |
| `grove` | Grove |
| `swamp` | Swamp |
| `mangrove_swamp` | Mangrove Swamp |
| `cherry_grove` | Cherry Grove |
| `mushroom_fields` | Mushroom Fields |
| `lush_caves` | Lush Caves |
| `crimson_forest` | Crimson Forest |
| `warped_forest` | Warped Forest |
| `nether_wastes` | Nether Wastes |
| `soul_sand_valley` | Soul Sand Valley |
| `basalt_deltas` | Basalt Deltas |

### Biome Tags (prefix with `#`)

| Value | Matches |
|-------|---------|
| `#is_forest` | Any forest biome |
| `#is_taiga` | Any taiga biome |
| `#is_jungle` | Any jungle biome |
| `#is_savanna` | Any savanna biome |
| `#is_badlands` | Any badlands biome |
| `#is_ocean` | Any ocean biome |
| `#is_river` | Any river biome |
| `#is_beach` | Any beach biome |
| `#is_overworld` | Any overworld biome |
| `#is_nether` | Any nether biome |
| `#is_end` | Any end biome |
| `#snowy_spruce_biomes` | Snowy spruce group |
| `#non_snowy_taiga` | Non-snowy taiga group |
| `#pine_biomes` | Pine tree biomes |

You can also use any modded biome tag with `#namespace:tag_name`.

### Object Form

| Field | Type | Description |
|-------|------|-------------|
| `any_of` | array | Match if ANY of the entries match (OR) |
| `all_of` | array | Match if ALL of the entries match (AND) |
| `not` | string/object | Match if the inner filter does NOT match |

### Array Form

An array of biome names/tags is treated as `any_of`.

### Examples

```json
{ "biomes": "forest" }

{ "biomes": "#is_forest" }

{ "biomes": { "any_of": ["forest", "dark_forest", "#is_taiga"] } }

{ "biomes": { "all_of": ["#is_overworld", { "not": "mushroom_fields" }] } }
```

---

## dimensions

Array of dimension IDs. Omit to match all dimensions.

| Value | Matches |
|-------|---------|
| `minecraft:overworld` | Overworld |
| `minecraft:the_nether` | Nether |
| `minecraft:the_end` | End |

Modded dimensions work too (e.g. `modname:dimension_id`).

### Example

```json
{ "dimensions": ["minecraft:overworld", "minecraft:the_nether"] }
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

```json
[
  {
    "nbt": "big_oak.nbt",
    "tree_type": "fancy_oak",
    "weight": 3,
    "min_spacing": 6,
    "biomes": ["forest", "dark_forest"],
    "dimensions": ["minecraft:overworld"]
  },
  {
    "nbt": "small_birch.nbt",
    "tree_type": "birch",
    "weight": 1,
    "origin_y_offset": -1,
    "biomes": "#is_forest",
    "valid_floor": "dirt"
  },
  {
    "nbt": "rare_jungle.nbt",
    "tree_type": ["jungle", "jungle_small"],
    "rare": true,
    "min_y": 60,
    "max_y": 200,
    "biomes": { "any_of": ["jungle", "bamboo_jungle"] }
  }
]
```
