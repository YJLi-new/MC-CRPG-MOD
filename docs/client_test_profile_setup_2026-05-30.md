# Playable Test Client Profile — 2026-05-30

Configured a separate PCL/Fabric Minecraft Java Edition `26.1.2` test profile for Ebb CRPG.

## Profile

- PCL profile name: `26.1.2-Fabric-Ebb-Test`
- Windows path: `E:\MC\PCL\.minecraft\versions\26.1.2-Fabric-Ebb-Test`
- WSL path: `/mnt/e/MC/PCL/.minecraft/versions/26.1.2-Fabric-Ebb-Test`
- Base vanilla profile preserved: `E:\MC\PCL\.minecraft\versions\26.1.2`

The vanilla `26.1.2` profile was not modified in place. The test profile has its own version JSON, copied vanilla client jar, copied initial options/natives, profile-local `mods/`, and PCL metadata.

## Installed runtime

- Minecraft: `26.1.2`
- Fabric Loader: `0.19.2`
- Fabric API: `0.150.0+26.1.2`
- GeckoLib: `5.5.1`
- Ebb mod jar: `ebb-0.1.0-dev.jar`

Installed mods in the profile-local `mods/` directory:

```text
ebb-0.1.0-dev.jar
fabric-api-0.150.0+26.1.2.jar
geckolib-fabric-26.1.2-5.5.1.jar
```

## Verification

- Version JSON exists and uses `mainClass = net.fabricmc.loader.impl.launch.knot.KnotClient`.
- PCL `Setup.ini` exists with `VersionFabric:0.19.2` and independent-version flags.
- Windows-applicable libraries referenced by the profile are present in `.minecraft/libraries`.
- Mod jar hashes:
  - `ebb-0.1.0-dev.jar`: `9f6a09ceb005aea6148aaa3d8e1545212c9ca6aaca9305aba2a1b8ca3a85624e`
  - `fabric-api-0.150.0+26.1.2.jar`: `43bdfc59a21ace202345bc4c42c751fa36b80617a61cf7b2f8c3698b806305d8`
  - `geckolib-fabric-26.1.2-5.5.1.jar`: `63d2519dc13e302da52911727f11ecb7b6bbecc79968751a90bf607273d5f8bc`

## How to launch

1. Open PCL.
2. Select version/profile `26.1.2-Fabric-Ebb-Test`.
3. Launch normally.
4. Create/open a single-player world with cheats enabled for the first test pass.

Useful commands were written into the profile `command_history.txt` for convenience:

```mcfunction
/ebb status
/ebb data
/ebb dev
/ebb summon_npc ebb:demo/innkeeper_day
/setblock 0 64 4 minecraft:oak_door[half=lower,facing=south]
/setblock 0 65 4 minecraft:oak_door[half=upper,facing=south]
/tag @e[type=minecraft:villager,limit=1,sort=nearest] add ebb.npc.innkeeper
```

## Re-run/update command

After rebuilding the mod jar, refresh the playable PCL profile with:

```bash
scripts/configure_pcl_test_client.sh
```

The script updates the dedicated test profile only; if profile JSON/PCL metadata already exist it writes timestamped backups before replacing them.
