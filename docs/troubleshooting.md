# Troubleshooting

## Common Issues

### Trees are not being replaced

**Check the log for:**
- `[Orchard] Mod initialised - X definition(s) registered.` - If X is 0, no definitions were loaded.
- `[Orchard] Loaded X definition(s) from filename.json` - Confirms a file was parsed.

**Possible causes:**
1. No JSON files in `config/orchard/data/`. Create at least one `.json` file there.
2. JSON syntax error. Check the log for `Failed to load` messages.
3. The `tree_type` doesn't match the vanilla tree at that location. Try `"tree_type": ["oak", "fancy_oak"]` to match both.
4. The biome filter excludes the current biome. Remove the `biomes` field to test.
5. Mixin not loading. Check that `orchard.mixins.json` lists all three mixin classes.

### NBT file not found

**Check the log for:**
- `[Orchard] MISSING filename.nbt`

**Fix:** Place the `.nbt` file in `config/orchard/nbt/filename.nbt`. Make sure the filename matches the `nbt` field in your JSON (without the `.nbt` extension).

### Trees appear but look wrong

**Possible causes:**
1. The NBT structure is not centered. Ensure the tree is centered within the structure block bounding box.
2. The trunk base is not at Y=0 in the NBT. The mod places Y=0 at ground level.
3. Air padding is eating terrain. This shouldn't happen due to TerrainPreservingProcessor, but verify your NBT doesn't have unexpected blocks.
4. Roots are not showing. If your tree has underground roots, set `"origin_y_offset"` to a negative value (e.g. `-3`) to shift the structure downward.

### Performance issues during world generation

The mod caches NBT templates after first load. If you see stutter on the first chunk generation but not after, this is normal. The cache is pre-warmed when the server starts.

If you have many definitions (100+), consider:
- Reducing the number of variants per tree type.
- Using the same NBT file for multiple definitions (the cache shares them).
- Using `dimensions` to prevent definitions from matching in dimensions where they aren't needed.
- Using `min_y`/`max_y` to narrow the Y range where replacements occur.

The placement index is pruned automatically every 4096 placements to prevent memory growth on long-running servers. Chunks that haven't been accessed in 30 minutes are evicted.

### Server crashes on startup

Check the log for the specific error. Common causes:
- Invalid JSON syntax in a config file.
- The config directory is not writable.
- Java version mismatch (requires Java 21+).

## Log Messages Reference

| Message | Meaning |
|---------|---------|
| `[Orchard] Mod initialised - X definition(s) registered.` | Startup summary. X = total definitions loaded from all JSON files. |
| `[Orchard] Loaded X definition(s) from filename.json` | A specific JSON file was parsed successfully. |
| `[Orchard] NBT file not found: path` | An NBT file referenced by a definition doesn't exist. |
| `[Orchard] Loaded filename.nbt (size: XxYxZ)` | An NBT template was loaded and cached. |
| `[Orchard] FAILED to load filename.nbt: error` | An NBT file exists but couldn't be parsed. |
| `[Orchard] TreeFeatureMixin is active` | First tree interception confirmed (mixin is alive). |
| `[Orchard] Placing filename.nbt at BlockPos` | Debug: a custom tree was placed (at DEBUG log level). |
| `[Orchard] Blocked placement at BlockPos` | Debug: placement was rejected due to wall overlap (at DEBUG log level). |
| `[Orchard] Template null for filename` | Debug: NBT failed to load, falling back to vanilla. |
| `[Orchard] Created directory: path` | A config subdirectory was created on first launch. |
| `[Orchard] Cache pre-warmed: X loaded, Y missing` | Summary of template cache at server start. |
| `[Orchard] Pruned X stale chunk(s) from placement index.` | Debug: old chunks removed from the placement index during periodic pruning. |
