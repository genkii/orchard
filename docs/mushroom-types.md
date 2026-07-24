# Mushroom Type Reference

The `mushroom_type` field identifies which huge mushroom to replace. It intercepts the `AbstractHugeMushroomFeature.place()` call, covering world generation in Mushroom Fields and Dark Forest biomes.

## Available Values

| Value | What It Matches |
|-------|-----------------|
| `"red"` | Large red mushroom (`red_mushroom_block` cap) |
| `"brown"` | Large brown mushroom (`brown_mushroom_block` cap) |
| `"any"` | Both red and brown |

## Examples

### Replace red mushrooms

```json
{
  "nbt": "my_red_mushroom",
  "mushroom_type": "red",
  "biomes": ["mushroom_fields", "dark_forest"]
}
```

### Replace brown mushrooms

```json
{
  "nbt": "my_brown_mushroom",
  "mushroom_type": "brown",
  "biomes": ["mushroom_fields", "dark_forest"]
}
```

### Replace both with the same NBT

```json
{
  "nbt": "generic_mushroom",
  "mushroom_type": "any",
  "biomes": ["mushroom_fields", "dark_forest"]
}
```

### Multiple variants with weight

```json
[
  { "nbt": "red_mushroom1", "mushroom_type": "red", "biomes": ["mushroom_fields", "dark_forest"], "weight": 2 },
  { "nbt": "red_mushroom2", "mushroom_type": "red", "biomes": ["mushroom_fields", "dark_forest"], "weight": 1 },
  { "nbt": "red_rare", "mushroom_type": "red", "biomes": ["mushroom_fields", "dark_forest"], "weight": 1, "rare": true }
]
```

## Behavior Notes

- **Bone meal on small mushrooms is NOT intercepted.** If a player bone meals a small red or brown mushroom, vanilla huge mushroom generation is used. Only world generation is replaced.
- **NBT placement.** The structure is centered horizontally on the placement origin. The stem base should be at Y=0 in the NBT.
- **Ground validation.** The mod checks that the ground block is dirt-family or mycelium. Mushrooms will not generate on treetops or artificial structures.
