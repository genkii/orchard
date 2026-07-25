# Spacing and Rarity Reference

## Minimum Spacing (`min_spacing`)

The `min_spacing` field sets a minimum block radius between two world-generation placements of the same type. This prevents trees from overlapping and creates more natural-looking forests.

### How It Works

When the game tries to place a custom tree, it checks:
1. **In-session check:** Has another custom tree been placed within this radius during the current session?
2. **Cross-session check:** Is there any `#minecraft:logs` block within this radius in the world?

If either check finds a nearby tree, the placement is skipped entirely (no vanilla fallback either).

### Spacing Guide

| Value | Effect |
|-------|--------|
| `0` | No extra spacing. Vanilla density applies. |
| `6-8` | Slightly less dense than vanilla. |
| `10-14` | Noticeably more open forests. |
| `16-24` | Sparse / parkland feel. |
| `32+` | Very isolated trees. |

### Examples

Dense forest (vanilla density):
```json
{ "nbt": "dense_oak", "tree_type": "oak", "min_spacing": 0 }
```

Open woodland:
```json
{ "nbt": "open_oak", "tree_type": "oak", "min_spacing": 14 }
```

Sparse ancient forest:
```json
{ "nbt": "ancient_oak", "tree_type": "fancy_oak", "min_spacing": 24 }
```

### Important Notes

- Spacing only affects **world generation**, not player-planted saplings. Players can always grow trees wherever they like.
- The cross-session check uses the `#minecraft:logs` tag, so it catches all wood variants (logs, wood, stripped variants).
- Different `tree_type` values do NOT share spacing. An `oak` tree and a `fancy_oak` tree have separate spacing pools.
- Spacing IS shared across all definitions of the same `tree_type`. If you register 10 oak variants, they all count against each other's spacing.

---

## Weight (`weight`)

The `weight` field controls the relative probability of a definition being selected when multiple definitions match the same tree type and biome.

### How It Works

When multiple definitions match, one is selected using a weighted random draw. A definition with `weight: 3` is three times as likely to be chosen as one with `weight: 1`.

### Examples

Equal 50/50 split:
```json
[
  { "nbt": "oak_a", "tree_type": "oak", "weight": 1 },
  { "nbt": "oak_b", "tree_type": "oak", "weight": 1 }
]
```

75%/25% split:
```json
[
  { "nbt": "oak_common", "tree_type": "oak", "weight": 3 },
  { "nbt": "oak_rare", "tree_type": "oak", "weight": 1 }
]
```

Three-tier rarity:
```json
[
  { "nbt": "oak_common", "tree_type": "oak", "weight": 5 },
  { "nbt": "oak_uncommon", "tree_type": "oak", "weight": 2 },
  { "nbt": "oak_rare", "tree_type": "oak", "weight": 1 }
]
```

This gives common ~62.5%, uncommon ~25%, rare ~12.5%.

### Notes

- Weight must be >= 1.
- Default weight is 1.
- Weight only applies within the same pool (normal or rare).

---

## Rare Pool (`rare`)

The `rare` field places a definition into a special **rare pool** that is selected only 2.5% of the time by default.

### How It Works

When multiple definitions match, the selection uses a two-phase process:

1. **Phase 1 - Pool gate:** If both rare and normal definitions exist, a random float is drawn. If it's below the rare pool probability (default 0.025 = 2.5%), the rare pool is used. Otherwise, the normal pool is used.
2. **Phase 2 - Weighted draw:** Within the chosen pool, one definition is selected using the `weight` values.

If only one type of pool exists (all rare or all normal), the gate roll is skipped.

### Configurable Probability

The rare pool probability can be inspected via `/orchard status` and defaults to 2.5%. The value is read at runtime and can be changed programmatically via `OrchardRegistry.setRarePoolProbability(float)`.

### Examples

A rare variant that appears 2.5% of the time:
```json
[
  { "nbt": "oak_common", "tree_type": "oak", "weight": 1 },
  { "nbt": "oak_common2", "tree_type": "oak", "weight": 1 },
  { "nbt": "oak_legendary", "tree_type": "oak", "weight": 1, "rare": true }
]
```

The gate rolls: 2.5% chance to enter the rare pool, 97.5% chance for normal pool.
- Within the normal pool: 50/50 between `oak_common` and `oak_common2`.
- Within the rare pool: only `oak_legendary`.

Effective probabilities:
- `oak_common`: ~48.75%
- `oak_common2`: ~48.75%
- `oak_legendary`: ~2.5%

### Multiple Rare Variants

You can have multiple rare variants with their own weights:
```json
[
  { "nbt": "oak", "tree_type": "oak", "weight": 1 },
  { "nbt": "oak_rare_a", "tree_type": "oak", "weight": 3, "rare": true },
  { "nbt": "oak_rare_b", "tree_type": "oak", "weight": 1, "rare": true }
]
```

When the rare pool is selected (2.5% chance):
- `oak_rare_a`: 75% of rare pool = 1.875% overall
- `oak_rare_b`: 25% of rare pool = 0.625% overall
