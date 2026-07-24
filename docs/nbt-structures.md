# NBT Structure Files

## Where to Place Them

All NBT structure files go in `config/orchard/nbt/`. The mod reads them from this directory at runtime (not from the mod jar).

```
config/
  orchard/
    nbt/
      oak1.nbt
      oak2.nbt
      spruce1.nbt
      ...
```

## How to Create NBT Files

### Method 1: In-Game Structure Block

1. Build your tree in creative mode.
2. Place a Structure Block and set it to **Save** mode.
3. Adjust the bounding box to encompass the entire tree (including roots and canopy).
4. Set the structure name (e.g., `orchard:oak1`).
5. Save the structure.
6. The `.nbt` file will be in your world's `generated/orchard/structures/` folder.
7. Copy it to `config/orchard/nbt/oak1.nbt`.

### Method 2: Third-Party Tools

Tools like **Axiom**, **WorldEdit**, or **Litematica** can export selections as NBT structure files.

### Method 3: From Another Mod

If you're migrating from the Fabric CustomTree mod, copy the `.nbt` files from `src/main/resources/data/custom-tree/structures/` directly into `config/orchard/nbt/`.

## NBT Authoring Guidelines

### Trunk Base Position

The trunk or stem base should be at **Y=0** in NBT space. When the mod places the structure, it centers it horizontally on the placement origin and places Y=0 at the ground level.

```
Y=0  → trunk/stem base (sits on ground)
Y=1+ → trunk continues upward, branches, leaves
```

### Roots and Underground Placement

By default, Y=0 in the NBT aligns with the ground level where the tree spawns. If your tree has roots, caves, or underground portions that should extend below ground, use `origin_y_offset` in your JSON config.

`origin_y_offset` shifts the entire NBT downward by the specified number of blocks. Use a negative value:

```json
{ "nbt": "oak_with_roots", "tree_type": "oak", "origin_y_offset": -3 }
```

With `origin_y_offset: -3`, the NBT is placed 3 blocks lower than normal:

```
Y=0 in NBT  → placed at ground level - 3 (underground)
Y=3 in NBT  → placed at ground level (where trunk normally starts)
Y=6 in NBT  → placed at ground level + 3 (canopy region)
```

**How to author a tree with roots:**

1. Build your tree in a structure block with roots included.
2. Position the structure so the trunk base (where roots meet trunk) is at Y=0 in NBT space.
3. Roots extend into negative Y values in the structure.
4. Set `origin_y_offset` to the depth of your roots. For example, if your roots extend 4 blocks below the trunk base, use `"origin_y_offset": -4`.
5. The mod will place the trunk base 4 blocks underground, so roots appear in the terrain while the canopy sits at the correct height.

**Negative vs positive values:**

| Value | Effect |
|-------|--------|
| `0` (default) | Y=0 in NBT = ground level |
| `-3` | Entire NBT shifted down 3 blocks; roots go underground |
| `+2` | Entire NBT shifted up 2 blocks; base floats above ground |

**Important notes:**

- The `TerrainPreservingProcessor` still applies: air blocks in the NBT won't erase existing terrain, so underground roots will only replace solid blocks where they need to.
- Bedrock and lava are never overwritten, even with negative offsets.
- The spacing check and wall-overlap detection use the original ground-level origin, not the offset position.

### Centering

The structure is centered horizontally: the mod calculates `corner = origin - (sizeX/2, 0, sizeZ/2)`. Make sure your tree is centered within the structure block's bounding box.

### Air Blocks

Air blocks in the NBT are **never written** to the world. This prevents the bounding-box padding from deleting surrounding terrain. You can safely have air padding around your tree.

### TerrainPreservingProcessor

The mod applies a terrain-preserving processor to every placement:

| Block Type | Behavior |
|------------|----------|
| Air | Never written (prevents craters) |
| Bedrock | Never overwritten |
| Lava | Never overwritten (prevents fire) |
| Water | Preserved (allows waterlogged placement) |
| Everything else | Placed normally |

This means:
- Your tree will not destroy surrounding terrain.
- Lava lakes in the Nether remain intact.
- Mangrove roots and swamp trunks can be waterlogged correctly.

### Tree Type Specific Tips

#### Overworld Trees

- Use the appropriate log block for the tree type (oak_log for oak, birch_log for birch, etc.).
- Place leaves using the correct leaf block for the tree type.
- The ground block below Y=0 is NOT part of the NBT - the mod handles ground alignment automatically.

#### Nether Fungi

- Use `warped_stem` or `crimson_stem` for the trunk.
- Use `warped_wart_block` or `nether_wart_block` for the hat.
- Use `shroomlight` for decoration if desired.
- The nylium base should be at Y=0.

#### Mangrove Trees

- Use `mangrove_log` for the trunk.
- Include mangrove roots in the NBT.
- The structure can extend over water - the mod will not overwrite water blocks.
- Do NOT include a hanging propagule at Y=0.

#### Azalea Trees

- Use `oak_log` for the trunk.
- Use a mix of `azalea_leaves` and `flowering_azalea_leaves` for the canopy.
- Include `rooted_dirt` roots below the tree.

#### Large Mushrooms

- Use `red_mushroom_block` or `brown_mushroom_block` for the cap.
- Use `mushroom_stem` for the stem.
- The stem base should be at Y=0.
- The small mushroom block at the base is removed before placement, so do NOT include one in the NBT.

## File Naming

- The filename in the JSON `nbt` field should match the `.nbt` file name (without extension).
- Example: `"nbt": "oak1"` looks for `config/orchard/nbt/oak1.nbt`.
- The `.nbt` extension is added automatically if omitted in the JSON.
- File names are case-sensitive on Linux.
