# JSON Configuration Reference

Every `.json` file in `config/orchard/data/` defines one or more tree replacements. Each file can contain a single JSON object or an array of objects.

## Full Option Reference

```json
{
  "nbt": "my_tree",
  "tree_type": "oak",
  "fungus_type": null,
  "mushroom_type": null,
  "biomes": null,
  "min_spacing": 0,
  "weight": 1,
  "rare": false,
  "origin_y_offset": 0
}
```

### Options

| Option | Type | Required | Default | Description |
|--------|------|----------|---------|-------------|
| `nbt` | string | **Yes** | - | Filename (without `.nbt` extension) of the structure file in `config/orchard/nbt/`. |
| `tree_type` | string, object, or array | No | `null` | Which overworld tree type to replace. See [Tree Type Reference](tree-types.md). |
| `fungus_type` | string or array | No | `null` | Which nether fungus type to replace. See [Fungus Type Reference](fungus-types.md). |
| `mushroom_type` | string or array | No | `null` | Which huge mushroom type to replace. See [Mushroom Type Reference](mushroom-types.md). |
| `biomes` | string, object, or array | No | `null` | Biome filter. `null` = all biomes. See [Biome Filter Reference](biome-filters.md). |
| `min_spacing` | integer | No | `0` | Minimum block radius between two trees of this type during world generation. `0` = vanilla density. |
| `weight` | integer | No | `1` | Relative selection weight when multiple definitions match. Must be >= 1. |
| `rare` | boolean | No | `false` | Place this definition in the rare pool (2.5% selection chance). |
| `origin_y_offset` | integer | No | `0` | Shift the placement origin down by this many blocks. Use negative values to place roots underground. See [NBT Structures](nbt-structures.md#roots-and-underground-placement). |

### Rule: At Least One Type Required

Each definition must have at least one of `tree_type`, `fungus_type`, or `mushroom_type`. Without one of these, the definition will never match anything.

### JSON Format

A file can contain a single definition:
```json
{ "nbt": "oak1", "tree_type": "oak" }
```

Or an array of definitions:
```json
[
  { "nbt": "oak1", "tree_type": "oak" },
  { "nbt": "oak2", "tree_type": "oak" }
]
```

Multiple files are allowed. All definitions from all `.json` files are merged into a single pool at startup.
