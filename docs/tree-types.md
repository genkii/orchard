# Tree Type Reference

The `tree_type` field identifies which overworld tree to replace during world generation. It intercepts the `TreeFeature.place()` call.

## Shorthand Strings

Use a single string to match a vanilla tree type by name:

| Value | What It Matches | How It Identifies |
|-------|-----------------|-------------------|
| `"oak"` | Regular (short) oak | `BlobFoliagePlacer` + `oak_log` trunk + `ignoreVines=true` |
| `"fancy_oak"` | Fancy / tall oak | `FancyFoliagePlacer` (unique to fancy oak) |
| `"birch"` | Birch | `BlobFoliagePlacer` + `birch_log` trunk |
| `"spruce"` | All spruce (including mega) | `SpruceFoliagePlacer` |
| `"spruce_only"` | Single-trunk spruce only | `SpruceFoliagePlacer` but NOT `GiantTrunkPlacer` |
| `"pine"` | Pine | `PineFoliagePlacer` |
| `"mega_pine"` | Mega pine (2x2 spruce) | `MegaPineFoliagePlacer` |
| `"mega_spruce"` | Mega spruce (2x2 old growth) | `GiantTrunkPlacer` + `SpruceFoliagePlacer` |
| `"jungle_small"` | Small jungle tree | `BlobFoliagePlacer` + `jungle_log` trunk |
| `"jungle_mega"` | Big jungle tree (2x2) | `MegaJungleFoliagePlacer` |
| `"jungle_bush"` | Jungle bush | `BushFoliagePlacer` |
| `"acacia"` | Acacia | `AcaciaFoliagePlacer` |
| `"dark_oak"` | Dark oak | `DarkOakFoliagePlacer` + `dark_oak_log` trunk |
| `"cherry"` | Cherry | `CherryFoliagePlacer` |
| `"swamp"` | Swamp oak | `BlobFoliagePlacer` + `oak_log` trunk + `ignoreVines=false` |
| `"azalea"` | Azalea tree | `RandomSpreadFoliagePlacer` + `oak_log` trunk |
| `"mangrove"` | Mangrove | `UpwardsBranchingTrunkPlacer` + `mangrove_log` trunk |

### Example - Simple oak replacement

```json
{ "nbt": "my_oak", "tree_type": "oak" }
```

## Object Format (Advanced)

Use an object to build custom matchers from individual components:

```json
{
  "tree_type": {
    "foliage": "blob",
    "trunk": null,
    "trunk_block": "minecraft:oak_log"
  }
}
```

### Object Fields

| Field | Type | Description |
|-------|------|-------------|
| `foliage` | string | Foliage placer type (see table below) |
| `trunk` | string | Trunk placer type (see table below) |
| `trunk_block` | string | Block ID of the trunk (e.g. `"minecraft:birch_log"`) |

All specified fields are combined with AND logic. Omit any field to skip that check.

### Foliage Values

| Value | Matches |
|-------|---------|
| `"blob"` | `BlobFoliagePlacer` - oak, birch, small jungle |
| `"fancy"` | `FancyFoliagePlacer` - fancy oak |
| `"spruce"` | `SpruceFoliagePlacer` - spruce, mega spruce |
| `"pine"` | `PineFoliagePlacer` - pine |
| `"mega_pine"` | `MegaPineFoliagePlacer` - mega pine |
| `"mega_jungle"` | `MegaJungleFoliagePlacer` - big jungle |
| `"bush"` | `BushFoliagePlacer` - jungle bush |
| `"acacia"` | `AcaciaFoliagePlacer` - acacia |
| `"dark_oak"` | `DarkOakFoliagePlacer` - dark oak, pale oak |
| `"cherry"` | `CherryFoliagePlacer` - cherry |
| `"random_spread"` | `RandomSpreadFoliagePlacer` - azalea, mangrove canopy |

### Trunk Values

| Value | Matches |
|-------|---------|
| `"dark_oak"` | `DarkOakTrunkPlacer` |
| `"forking"` | `ForkingTrunkPlacer` - acacia |
| `"giant"` | `GiantTrunkPlacer` - mega spruce |
| `"mega_jungle"` | `MegaJungleTrunkPlacer` |
| `"upwards_branching"` | `UpwardsBranchingTrunkPlacer` - mangrove |

### Custom trunk_block Examples

Match any tree using a specific log block:

```json
{ "tree_type": { "trunk_block": "minecraft:birch_log" } }
```

Match a custom modded tree:
```json
{ "tree_type": { "foliage": "blob", "trunk_block": "mymod:my_log" } }
```

## Array Format (Multiple Types)

Use an array to match several tree types with the same NBT. This is OR logic - any match counts:

```json
{
  "nbt": "big_oak",
  "tree_type": ["oak", "fancy_oak"]
}
```

You can mix shorthand strings and objects in the same array:
```json
{
  "nbt": "big_conifer",
  "tree_type": ["pine", "mega_pine", "mega_spruce"]
}
```

## Why Trunk Block Discrimination Is Needed

In Minecraft 1.21, oak, birch, and small jungle trees all share `BlobFoliagePlacer`. The only way to tell them apart is by checking the trunk block (`oak_log` vs `birch_log` vs `jungle_log`). The shorthand strings handle this automatically, but when building custom object matchers, use `trunk_block` to disambiguate.

Similarly, dark oak and pale oak share `DarkOakFoliagePlacer` and `DarkOakTrunkPlacer`. Use `trunk_block: "minecraft:dark_oak_log"` vs `"minecraft:pale_oak_log"` to distinguish them.

## Important Notes

- Each JSON definition should have exactly ONE type: `tree_type`, `fungus_type`, or `mushroom_type`. Mixing tree types with fungus types in the same definition has no effect (both matchers are set independently, but only one is checked by the corresponding mixin).
- If `tree_type` is omitted, the definition will not replace any overworld trees.
- If a tree type doesn't match anything in your config, vanilla generation proceeds normally as a fallback.
