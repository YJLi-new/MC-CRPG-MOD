# Custom Story Pack Tutorial

This tutorial creates one block-group investigation point with a dialogue choice, a d20 check, failure-forward effects, a journal entry, and a clue.

Use it with the current Fabric `26.1.2` Ebb alpha. The runtime registry paths mirror Minecraft data-pack layout under `data/<namespace>/...`; bundled examples live under `src/main/resources/data/ebb/...`.

## 1. Pick a namespace and ids

For a custom pack, choose a namespace that is not `ebb`, for example `mycase`:

```text
mycase:rusty_lock_dialogue
mycase:rusty_lock
mycase:rust_on_keyhole
mycase:rust_on_keyhole_note
```

## 2. Add a journal entry

Create `data/mycase/journal_entries/demo/rust_on_keyhole_note.json` in your data pack or mirrored resource folder:

```json
{
  "title": "Rust on the keyhole",
  "category": "clue",
  "text": "The rust is scraped only on the lower edge, as if someone used the wrong key in a hurry."
}
```

## 3. Add a clue

Create `data/mycase/clues/demo/rust_on_keyhole.json`:

```json
{
  "display_name": "Rust on the keyhole",
  "journal_entry": "mycase:demo/rust_on_keyhole_note",
  "tags": ["door", "lock", "failure_forward"],
  "check_modifiers": {
    "perception": 1,
    "intelligence": 1
  }
}
```

## 4. Add a dialogue with a check

Create `data/mycase/dialogues/demo/rusty_lock_dialogue.json`:

```json
{
  "id": "mycase:demo/rusty_lock_dialogue",
  "start": "start",
  "nodes": {
    "start": {
      "speaker": "narrator",
      "text": "The lock is old, but the fresh scratches around it are not.",
      "choices": [
        {
          "id": "inspect",
          "type": "action",
          "text": "Read the scratches around the keyhole.",
          "check": {
            "attribute": "perception",
            "dc": 12,
            "success": "success",
            "failure": "fail_forward",
            "success_effects": [
              { "type": "reveal_clue", "id": "mycase:demo/rust_on_keyhole" }
            ],
            "failure_effects": [
              { "type": "add_journal_entry", "id": "mycase:demo/rust_on_keyhole_note" }
            ]
          }
        },
        {
          "id": "leave",
          "type": "dialogue",
          "text": "Step back.",
          "next": "end"
        }
      ]
    },
    "success": {
      "speaker": "narrator",
      "text": "You see the wrong-key scrape immediately. This was forced in panic, not opened with care.",
      "choices": [{ "id": "done", "type": "dialogue", "text": "Continue.", "next": "end" }]
    },
    "fail_forward": {
      "speaker": "narrator",
      "text": "You miss the pattern, but the rust stains your thumb. Later, that stain may matter.",
      "choices": [{ "id": "done", "type": "dialogue", "text": "Continue.", "next": "end" }]
    },
    "end": {
      "speaker": "narrator",
      "text": "The door keeps its secret for now.",
      "choices": []
    }
  }
}
```

Rules to preserve:

- High-stakes checks should have a failure branch or failure effects.
- Roll resolution and effects are server-authoritative.
- Use canonical attributes: `strength`, `dexterity`, `constitution`, `intelligence`, `wisdom`, `charisma`, `perception`, `luck`.

## 5. Add a block-group target

Create `data/mycase/interactions/block_groups/demo/rusty_lock.json`:

```json
{
  "display_name": "Rusty lock",
  "dialogue": "mycase:demo/rusty_lock_dialogue",
  "interaction_point": [10, 64, 10],
  "blocks": [[10, 64, 10]],
  "max_distance": 2.5,
  "highlight": {
    "render_mode": "merged",
    "priority": 20,
    "close_color": "#55D6FF",
    "far_color": "#1F6C91",
    "opacity": 0.35
  }
}
```

The client can predict and highlight synced block groups, but the server still validates distance, line-of-sight, and target existence when the player presses **X**.

## 6. Optional: add an entity binding

Entity interactions must be explicit. Do **not** depend on debug fallback for release content.

```json
{
  "display_name": "Named witness",
  "dialogue": "mycase:demo/witness_dialogue",
  "priority": 100,
  "match": {
    "entity_type": "ebb:npc",
    "tags": ["mycase.npc.witness"]
  },
  "highlight_range": 6.0,
  "interaction_range": 2.5
}
```

## 7. Validate

From the repository root:

```bash
scripts/compile_authoring_sources.py --clean
scripts/p24_authoring_validation.py
scripts/gradle-local.sh --no-daemon validateEbbData
scripts/run_smoke_checks.sh
```

Validation catches missing dialogue ids, node ids, quest/feat/chime/journal/clue/routine/relationship/conflict references, and unsafe high-stakes checks.

## 8. Test in game

1. Refresh the separate test profile with `scripts/configure_pcl_test_client.sh` if you changed bundled resources.
2. Relaunch `26.1.2-Fabric-Ebb-Test`.
3. Look at your block group, verify its outline appears, press **X**, and click the checked choice.
4. Confirm the dialogue status area shows roll feedback and journal/clue echoes without overlapping choice buttons.
