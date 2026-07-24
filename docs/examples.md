# Examples

## Minimal Tree

Replace all vanilla oak trees with a single NBT:

```json
{ "nbt": "oak1", "tree_type": "oak" }
```

## Multiple Variants

Randomly pick from several NBTs for the same tree type:

```json
[
  { "nbt": "oak1", "tree_type": "oak" },
  { "nbt": "oak2", "tree_type": "oak" },
  { "nbt": "oak3", "tree_type": "oak" }
]
```

## Biome-Restricted

Only replace trees in specific biomes:

```json
[
  { "nbt": "darkoak1", "tree_type": "dark_oak", "biomes": "dark_forest" },
  { "nbt": "jungle1",  "tree_type": "jungle_small", "biomes": "#minecraft:is_jungle" }
]
```

## Rare Variant

A special tree that only appears 2.5% of the time:

```json
{ "nbt": "rare_oak", "tree_type": "oak", "rare": true, "weight": 5 }
```

## Trees with Custom Roots

Place roots underground by offsetting the origin downward:

```json
{ "nbt": "oak_with_roots", "tree_type": "oak", "origin_y_offset": -3 }
```

See [NBT Structures](nbt-structures.md) for details on authoring structures with underground roots.

## Fungi and Mushrooms

```json
[
  { "nbt": "warped1", "fungus_type": "warped", "biomes": "warped_forest" },
  { "nbt": "brown1",  "mushroom_type": "brown", "biomes": "mushroom_fields" }
]
```

## Weighted Selection

Give certain variants higher spawn chances:

```json
[
  { "nbt": "common_oak", "tree_type": "oak", "weight": 5 },
  { "nbt": "uncommon_oak", "tree_type": "oak", "weight": 2 },
  { "nbt": "rare_oak", "tree_type": "oak", "weight": 1, "rare": true }
]
```

## Spacing Control

Prevent trees from spawning too close together:

```json
{ "nbt": "big_oak", "tree_type": "fancy_oak", "min_spacing": 12 }
```
