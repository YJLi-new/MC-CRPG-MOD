# Esoteric Ebb CRPG Installation Guide

This guide is for the current alpha prototype of **Esoteric Ebb CRPG** (`ebb`) on **Minecraft Java Edition 26.1.2**.

## Supported stack

Use these exact versions for the current build:

| Component | Version | Notes |
| --- | --- | --- |
| Minecraft Java Edition | `26.1.2` | Do not use NeoForge or Minecraft 1.21.x for this branch. |
| Java | `25` | Client, server, Gradle, and GameTest verification use Java 25. |
| Fabric Loader | `0.19.2` | Loader for both client and dedicated server. |
| Fabric API | `0.150.0+26.1.2` | Required runtime dependency. |
| GeckoLib | `5.5.1` | Required runtime dependency for `ebb:npc` rendering/animation. |
| Ebb mod jar | `build/libs/ebb-0.1.0-dev.jar` | Current verified jar hash is listed in `docs/current_status.md`. |

Official/source channels to use when refreshing dependencies:

- Fabric installer / server bootstrap: `https://fabricmc.net/use/installer/`
- Fabric API release page or Maven coordinates for `0.150.0+26.1.2`
- GeckoLib Fabric release page or Maven coordinates for `5.5.1`

Do not mix NeoForge GeckoLib jars with this Fabric build.

## Client install

1. Install or select **Minecraft Java Edition 26.1.2**.
2. Install **Fabric Loader 0.19.2** for Minecraft `26.1.2`.
3. Create a new profile instead of altering the vanilla profile in place.
4. Put these jars in the profile/game directory `mods/` folder:
   - `ebb-0.1.0-dev.jar`
   - `fabric-api-0.150.0+26.1.2.jar`
   - `geckolib-fabric-26.1.2-5.5.1.jar`
5. Launch once and check the log for lines like:

```text
Loading Minecraft 26.1.2 with Fabric Loader 0.19.2
Loaded ... mods: ebb 0.1.0-dev, fabric-api 0.150.0+26.1.2, geckolib 5.5.1
Dialogue registry rebuilt: 19 valid dialogue(s)
Built block group index: 12 group(s)
Entity binding registry rebuilt: 14 binding(s), debug fallback=false
NPC routine registry rebuilt: 7 routine(s)
```

### Local PCL test profile

For this repository, use the separate Plain Craft Launcher profile:

```text
/mnt/e/MC/PCL/.minecraft/versions/26.1.2-Fabric-Ebb-Test
```

Refresh it from the repository root:

```bash
scripts/configure_pcl_test_client.sh
```

The script installs the current Ebb jar plus the pinned Fabric API and GeckoLib jars into the profile-local `mods/` directory. It intentionally does **not** modify the vanilla `26.1.2` profile.

After launching `26.1.2-Fabric-Ebb-Test`, the recommended test world is:

```text
新的世界 (1)
```

Optional runtime checks:

```bash
scripts/check_pcl_runtime_loaded.py
scripts/run_gui_automation_smoke.sh
python3 scripts/gui_e2e_run.py --scenario runtime_check
python3 scripts/gui_e2e_run.py --scenario gui_retest --gui --gui-wait 1.4
```

Only treat GUI verification as passed after the client has relaunched into the refreshed jar.

## Dedicated server install

1. Create a Fabric dedicated server for Minecraft `26.1.2` with Fabric Loader `0.19.2`.
2. Run the server with Java `25`.
3. Put these jars in the server `mods/` folder:
   - `ebb-0.1.0-dev.jar`
   - `fabric-api-0.150.0+26.1.2.jar`
   - `geckolib-fabric-26.1.2-5.5.1.jar`
4. Start the server once and verify the Ebb registries load with zero validation messages.
5. Require players to install the same client-side mod/dependencies. Ebb has dedicated-server missing-client diagnostics, but the intended alpha experience needs the Ebb client for highlights, prompts, dialogue UI, journal/quest UI, and NPC rendering.

Example server launch shape:

```bash
java -Xmx4G -jar fabric-server-launch.jar nogui
```

Run server-side validation from the repository before distributing a jar:

```bash
scripts/gradle-local.sh --no-daemon validateEbbData
scripts/gradle-local.sh --no-daemon runGametestServer --args nogui
scripts/run_smoke_checks.sh
```

## Common troubleshooting

- **No outlines or `按 [X] 互动` prompt:** verify the client relaunched into the current jar and that the target is explicitly bound/tagged. Ebb does not make all entities interactable by default.
- **All NPCs show the same dialogue:** verify role tags/bindings are synced and that the current jar reports `entity_bindings=14` or higher.
- **Client can join but UI is missing:** install Ebb, Fabric API, and GeckoLib on the client. Dedicated server can warn about missing Ebb client sync payloads.
- **Data pack or JSON fails:** run `scripts/p24_authoring_validation.py` and read the file/line diagnostic.
