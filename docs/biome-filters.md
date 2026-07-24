# Biome Filter Reference

The `biomes` field restricts a tree definition to specific biomes. When omitted or set to `null`, the tree can appear in **any** biome.

## Shorthand Strings

### Individual Biomes

Use the biome's resource path (without `minecraft:` prefix):

| Value | Biome |
|-------|-------|
| `"plains"` | Plains |
| `"sunflower_plains"` | Sunflower Plains |
| `"meadow"` | Meadow |
| `"snowy_plains"` | Snowy Plains |
| `"forest"` | Forest |
| `"flower_forest"` | Flower Forest |
| `"birch_forest"` | Birch Forest |
| `"old_growth_birch_forest"` | Old Growth Birch Forest |
| `"dark_forest"` | Dark Forest |
| `"windswept_forest"` | Windswept Forest |
| `"taiga"` | Taiga |
| `"snowy_taiga"` | Snowy Taiga |
| `"old_growth_pine_taiga"` | Old Growth Pine Taiga |
| `"old_growth_spruce_taiga"` | Old Growth Spruce Taiga |
| `"jungle"` | Jungle |
| `"sparse_jungle"` | Sparse Jungle |
| `"bamboo_jungle"` | Bamboo Jungle |
| `"savanna"` | Savanna |
| `"savanna_plateau"` | Savanna Plateau |
| `"windswept_savanna"` | Windswept Savanna |
| `"windswept_hills"` | Windswept Hills |
| `"windswept_gravelly_hills"` | Windswept Gravelly Hills |
| `"grove"` | Grove |
| `"swamp"` | Swamp |
| `"mangrove_swamp"` | Mangrove Swamp |
| `"cherry_grove"` | Cherry Grove |
| `"mushroom_fields"` | Mushroom Fields |
| `"lush_caves"` | Lush Caves |
| `"crimson_forest"` | Crimson Forest |
| `"warped_forest"` | Warped Forest |
| `"nether_wastes"` | Nether Wastes |
| `"soul_sand_valley"` | Soul Sand Valley |
| `"basalt_deltas"` | Basalt Deltas |

### Biome Tags

Prefix with `#` to match all biomes with a tag:

| Value | Matches |
|-------|---------|
| `"#minecraft:is_forest"` | Forest, Flower Forest, Birch Forest, Old Growth Birch, Dark Forest, Windswept Forest |
| `"#minecraft:is_taiga"` | Taiga, Snowy Taiga, Old Growth Pine Taiga, Old Growth Spruce Taiga |
| `"#minecraft:is_jungle"` | Jungle, Sparse Jungle, Bamboo Jungle |
| `"#minecraft:is_savanna"` | Savanna, Savanna Plateau, Windswept Savanna |
| `"#minecraft:is_badlands"` | Badlands, Eroded Badlands, Wooded Badlands |
| `"#minecraft:is_ocean"` | All ocean biomes |
| `"#minecraft:is_river"` | All river biomes |
| `"#minecraft:is_beach"` | All beach biomes |
| `"#minecraft:is_overworld"` | All overworld biomes |
| `"#minecraft:is_nether"` | All nether biomes |
| `"#minecraft:is_end"` | All end biomes |

### Combined Shorthand Names

These are pre-built groups:

| Value | Biomes Included |
|-------|-----------------|
| `"is_forest"` | Same as `#minecraft:is_forest` |
| `"is_taiga"` | Same as `#minecraft:is_taiga` |
| `"is_jungle"` | Same as `#minecraft:is_jungle` |
| `"is_savanna"` | Same as `#minecraft:is_savanna` |
| `"is_badlands"` | Same as `#minecraft:is_badlands` |
| `"is_ocean"` | Same as `#minecraft:is_ocean` |
| `"is_river"` | Same as `#minecraft:is_river` |
| `"is_beach"` | Same as `#minecraft:is_beach` |
| `"is_overworld"` | Same as `#minecraft:is_overworld` |
| `"is_nether"` | Same as `#minecraft:is_nether` |
| `"is_end"` | Same as `#minecraft:is_end` |
| `"snowy_spruce_biomes"` | Snowy Taiga + Snowy Plains + Windswept Hills + Windswept Forest + Windswept Gravelly Hills + Grove |
| `"non_snowy_taiga"` | Taiga + Old Growth Pine Taiga + Old Growth Spruce Taiga |
| `"pine_biomes"` | All taiga biomes + Grove |

## Array Format (OR Logic)

An array of biome strings matches if **any** of them match:

```json
"biomes": ["forest", "flower_forest"]
```

This matches both Forest and Flower Forest biomes.

## Object Format (Advanced)

### `any_of` (OR)

Match if any of the listed biome filters match:

```json
"biomes": {
  "any_of": ["mushroom_fields", "dark_forest"]
}
```

### `all_of` (AND)

Match only if **all** listed biome filters match:

```json
"biomes": {
  "all_of": ["#minecraft:is_taiga", { "not": "snowy_taiga" }]
}
```

### `not` (Negation)

Match if the inner filter does NOT match:

```json
"biomes": { "not": "dark_forest" }
```

### Combining Operators

Operators can be nested arbitrarily:

```json
"biomes": {
  "all_of": [
    "#minecraft:is_forest",
    { "not": "dark_forest" },
    { "any_of": ["forest", "birch_forest"] }
  ]
}
```

This matches forest or birch_forest but NOT dark_forest.

## Practical Examples

### Forest-only birch trees

```json
{ "nbt": "forest_birch", "tree_type": "birch", "biomes": ["forest", "flower_forest"] }
```

### All taiga except snowy

```json
{
  "nbt": "taiga_spruce",
  "tree_type": "spruce_only",
  "biomes": {
    "all_of": ["#minecraft:is_taiga", { "not": "snowy_taiga" }]
  }
}
```

### All nether biomes

```json
{ "nbt": "nether_tree", "fungus_type": "any", "biomes": "#minecraft:is_nether" }
```

### Everywhere except oceans

```json
{
  "nbt": "rare_oak",
  "tree_type": "oak",
  "biomes": { "not": "#minecraft:is_ocean" }
}
```
