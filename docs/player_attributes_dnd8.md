# Player Attribute Points — DND-8 MVP

The mod now has a DND-like eight-dimension player attribute point system.

## Attributes

| Key | Display | Aliases |
|---|---|---|
| `strength` | 力量 / Strength | `str`, `force` |
| `dexterity` | 敏捷 / Dexterity | `dex` |
| `constitution` | 体质 / Constitution | `con` |
| `intelligence` | 智力 / Intelligence | `int`, `logic` |
| `wisdom` | 感知 / Wisdom | `wis` |
| `charisma` | 魅力 / Charisma | `cha`, `empathy` |
| `perception` | 察觉 / Perception | `per` |
| `luck` | 幸运 / Luck | `luc` |

The old content keys `force`, `logic`, and `empathy` remain valid aliases, but new content should prefer `strength`, `intelligence`, and `charisma`.

## Starting points and storage

- New player narrative state starts with `8` unspent attribute points.
- Attribute scores are stored in `NarrativeSavedData` per player.
- Spending one point increases one attribute score by `+1`.
- Current per-attribute range is `-5` to `10`.

## Commands

```mcfunction
/ebb attributes
/ebb attr
```

Shows current DND-8 scores and unspent points.

```mcfunction
/ebb attributes spend <attribute> <points>
```

Spends unspent points on the invoking player. Examples:

```mcfunction
/ebb attributes spend charisma 1
/ebb attr spend strength 2
```

OP/debug commands:

```mcfunction
/ebb attributes grant <points>
/ebb attributes set <attribute> <score>
/ebb attributes reset
```

These currently operate on the invoking player and are intended for development/testing.

## Dialogue checks

Sample content now uses DND-8 keys:

- Locked door force check: `strength`
- Innkeeper pressure check: `charisma`

Rolls still run server-authoritatively as `d20 + attributeScore vs DC`.
