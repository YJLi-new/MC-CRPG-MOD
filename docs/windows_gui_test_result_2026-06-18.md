# Windows GUI Test Result — 2026-06-18

Runtime: separate PCL/Fabric profile `26.1.2-Fabric-Ebb-Test`, save `新的世界 (1)`.
Tested at: 2026-06-18 01:22 Asia/Shanghai.

## Runtime under test

```text
9795116871dbac74ab7e34fa6ec602b30d7851f2a794651982544d78c96d2932  build/libs/ebb-0.1.0-dev.jar
01116f0b28668a4dbf832bc39da7ebbd2e24f04f7018283d1d20939f536738d1  build/libs/ebb-0.1.0-dev-sources.jar
```

The profile-local jar was refreshed only after closing the running Minecraft JVM. `runtime-check-report.json` confirms the open client loaded the matching jar and registry counts:

```text
dialogues=20, block_groups=12, entity_bindings=15, npc_routines=7
```

## Actual Windows GUI scenarios

| Scenario | Report | Steps | Failures | Evidence summary |
|---|---:|---:|---:|---|
| Runtime check | `build/gui-e2e/runtime-check-report.json` | 1 | 0 | Matching jar hash and registry counts. |
| Full interaction retest | `build/gui-e2e/gui-retest-report.json` | 282 | 0 | K menu, `/ebb` commands, 6 role NPC interactions, innkeeper choice progression, and all 12 block-group interactions. |
| LLM chat GUI | `build/gui-e2e/llm-chat-report.json` | 131 | 0 | K-menu LLM status, fake streaming chat, citations overlay, suggested-option click, return-to-script. |
| LLM validation GUI | `build/gui-e2e/p43-llm-validation-report.json` | 110 | 0 | auth-disabled, fake-chat, and real-gateway dry-run status routes. |
| P45 memory-proof route | `build/gui-e2e/memory-proof-report.json` | 3 | 0 | Runtime-ready route manifest; visual LLM chat evidence remains `llm_chat`, memory retrieval proof remains covered by gateway smoke/P45 audit. |

Representative screenshots include:

- `build/gui-e2e/k_menu_open.png`
- `build/gui-e2e/role_innkeeper_dialogue.png`
- `build/gui-e2e/block_stairwell_dust_dialogue.png`
- `build/gui-e2e/llm_chat_reply.png`
- `build/gui-e2e/llm_citations_overlay.png`
- `build/gui-e2e/p43_fake_chat_reply.png`

## Issues found and fixed during this GUI pass

1. The profile-local jar was stale at first (`profile=fddca105...`, build=`548d39...`). The client was closed, the profile was refreshed, and later refreshed again after the hotfix build.
2. `stairwell_dust` failed in the first `gui_retest`: the old test view aimed at the cobweb outline, while project policy intentionally uses collider-only raycasts. `scripts/gui_e2e_run.py` now aims at the carpet collider (`pitch=45`), and the rerun passed.
3. `/ebb summon_npc demo/...` parsing rejected routine ids containing `/` during GUI setup. `summon_npc` now uses `StringArgumentType.greedyString()`. Latest runtime log contains no `n_npc demo/` parse errors.
4. `gui_e2e_run.py` previously could return exit code 0 even with failed report steps. It now returns nonzero for failed steps, while still honoring `--allow-stale-runtime` for dry-run/runtime-only smoke flows.
5. `llm_validation` GUI path had an automation-only `log_step(name=...)` collision. The payload field is now `config_name`; actual GUI rerun passed.

## Additional validation after fixes

```text
python3 -m py_compile scripts/gui_e2e_run.py -> pass
scripts/gradle-local.sh --no-daemon build -> BUILD SUCCESSFUL
scripts/run_gui_automation_smoke.sh -> passed
python3 scripts/gui_e2e_run.py --scenario runtime_check -> passed
git diff --check -> passed
```
