# Fungus Type Reference

The `fungus_type` field identifies which nether fungus to replace. It intercepts the `HugeFungusFeature.place()` call, covering both world generation and bone meal growth.

## Available Values

| Value | What It Matches |
|-------|-----------------|
| `"warped"` | Warped huge fungus (`warped_stem`) |
| `"crimson"` | Crimson huge fungus (`crimson_stem`) |
| `"any"` | Both warped and crimson |

## Examples

### Replace warped trees

```json
{
  "nbt": "my_warped_tree",
  "fungus_type": "warped",
  "biomes": "warped_forest",
  "min_spacing": 6
}
```

### Replace crimson trees

```json
{
  "nbt": "my_crimson_tree",
  "fungus_type": "crimson",
  "biomes": "crimson_forest",
  "min_spacing": 6
}
```

### Replace both with the same NBT

```json
{
  "nbt": "generic_nether_tree",
  "fungus_type": "any",
  "biomes": "#minecraft:is_nether",
  "min_spacing": 6
}
```

### Multiple variants with weighted selection

```json
[
  { "nbt": "warped1", "fungus_type": "warped", "biomes": "warped_forest", "weight": 3 },
  { "nbt": "warped2", "fungus_type": "warped", "biomes": "warped_forest", "weight": 1 },
  { "nbt": "warped_rare", "fungus_type": "warped", "biomes": "warped_forest", "weight": 1, "rare": true }
]
```

This gives `warped1` a 75% chance, `warped2` a ~24.4% chance, and `warped_rare` a ~0.6% chance (2.5% gate then weighted).

## Behavior Notes

- **Bone meal is intercepted too.** When a player uses bone meal on a planted warped/crimson fungus, the game calls `HugeFungusFeature.place()` internally. The mod replaces this with the custom NBT as well.
- **Spacing applies.** The `min_spacing` field works for fungus trees the same way as overworld trees.
- **NBT placement.** The structure is centered horizontally on the placement origin. The stem base should be at Y=0 in the NBT.
