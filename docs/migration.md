# Migration Guide: Fabric CustomTree to NeoForge Orchard

This guide helps you migrate from the Fabric CustomTree mod (where trees are registered in Java code) to the NeoForge Orchard mod (where trees are configured via JSON files).

## Step 1: Copy NBT Files

Copy all `.nbt` structure files from the Fabric mod into the NeoForge config directory:

**From:**
```
CustomTree/src/main/resources/data/custom-tree/structures/*.nbt
```

**To:**
```
<minecraft>/config/orchard/nbt/*.nbt
```

The file names stay exactly the same. No renaming is needed.

## Step 2: Create JSON Configs

For each `registerTree()`, `registerFungus()`, or `registerMushroom()` call in the Fabric mod's `CustomTree.onInitialize()`, create a corresponding JSON entry in `config/orchard/data/`.

### Conversion Table

| Fabric Java Call | NeoForge JSON |
|-----------------|---------------|
| `registerTree("oak1", Blocks.OAK_SAPLING, TreeMatchers.OAK, 10)` | `{ "nbt": "oak1", "tree_type": "oak", "min_spacing": 10 }` |
| `registerTree("oak1", Blocks.OAK_SAPLING, TreeMatchers.OAK, 10, BiomeMatchers.FOREST)` | `{ "nbt": "oak1", "tree_type": "oak", "min_spacing": 10, "biomes": "forest" }` |
| `registerFungus("warped1", TreeMatchers.WARPED_FUNGUS, 6, BiomeMatchers.WARPED_FOREST)` | `{ "nbt": "warped1", "fungus_type": "warped", "min_spacing": 6, "biomes": "warped_forest" }` |
| `registerMushroom("redmushroom1", TreeMatchers.RED_MUSHROOM, BiomeMatchers.any(MUSHROOM_FIELDS, DARK_FOREST))` | `{ "nbt": "redmushroom1", "mushroom_type": "red", "biomes": ["mushroom_fields", "dark_forest"] }` |

### Biome Matcher Conversion

| Fabric `BiomeMatchers` | NeoForge JSON |
|------------------------|---------------|
| `BiomeMatchers.FOREST` | `"forest"` |
| `BiomeMatchers.IS_FOREST` | `"is_forest"` or `"#minecraft:is_forest"` |
| `BiomeMatchers.any(A, B)` | `["a", "b"]` or `{ "any_of": ["a", "b"] }` |
| `BiomeMatchers.all(A, B)` | `{ "all_of": ["a", "b"] }` |
| `A.negate()` | `{ "not": "a" }` |

### Complex Biome Filter Examples

**Fabric:**
```java
BiomeMatchers.any(
    BiomeMatchers.OLD_GROWTH_BIRCH_FOREST,
    BiomeMatchers.FOREST,
    BiomeMatchers.FLOWER_FOREST
).negate()
```

**NeoForge JSON:**
```json
{ "not": { "any_of": ["old_growth_birch_forest", "forest", "flower_forest"] } }
```

Or equivalently:
```json
{ "all_of": [
    { "not": "old_growth_birch_forest" },
    { "not": "forest" },
    { "not": "flower_forest" }
]}
```

### Rare Weighted Registration

**Fabric:**
```java
CustomTreeRegistry.register(
    CustomTreeDefinition.forTree("birchtree3")
        .sapling(Blocks.BIRCH_SAPLING)
        .worldGen(TreeMatchers.BIRCH)
        .biomes(...)
        .minSpacing(9)
        .rare()
        .build()
);
```

**NeoForge JSON:**
```json
{ "nbt": "birchtree3", "tree_type": "birch", "min_spacing": 9, "rare": true, "biomes": "..." }
```

### Multiple Tree Types in One Registration

**Fabric:**
```java
registerTree("bigspruce1", Blocks.SPRUCE_SAPLING,
    TreeMatchers.any(TreeMatchers.PINE, TreeMatchers.MEGA_PINE, TreeMatchers.MEGA_SPRUCE),
    8, BiomeMatchers.PINE_BIOMES);
```

**NeoForge JSON:**
```json
{ "nbt": "bigspruce1", "tree_type": ["pine", "mega_pine", "mega_spruce"], "min_spacing": 8, "biomes": "pine_biomes" }
```

## Step 3: Verify

1. Start the server/client.
2. Check the log for `[Orchard] Mod initialised - X definition(s) registered.`
3. Check for any `MISSING` warnings about NBT files.
4. Enter a world and verify trees are replaced.

## Key Differences

| Feature | Fabric | NeoForge |
|---------|--------|----------|
| Registration | Java code in `onInitialize()` | JSON files in `config/orchard/data/` |
| NBT location | Mod jar resources | `config/orchard/nbt/` |
| Sapling growth | Intercepts via world-gen matcher (not separate) | Same - world-gen matcher handles saplings too |
| Bone meal (fungi) | Intercepted by HugeFungusFeatureMixin | Same |
| Bone meal (mushrooms) | NOT intercepted (vanilla used) | Same |
| Debug command | `/customtrees status` | Not implemented (check logs for `[Orchard]` messages instead) |
| Config reload | Requires restart | Hot reload with `/orchard reload` (and `/orchard clearcache` for NBT changes) |
