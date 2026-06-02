# Agent Instructions

- Work in this repository root unless explicitly running the separate test client.
- Do not migrate away from Fabric Minecraft `26.1.2`, Java `25`, Fabric API, Fabric Loader, Loom, or GeckoLib `5.5.1`.
- Keep story/game-state decisions server-authoritative; client code renders and predicts only from synced data.
- Keep story content data-driven through JSON/YAML authoring. Do not hard-code one-off scene logic in Java when data can express it.
- Do not make all entities interactable by default. Demo/release interaction must come from explicit block groups, entity bindings, synced registered targets, tags, names, UUIDs, or entity-type rules.
- Preserve legacy aliases unless a migration and tests prove they are safe to remove.
- For multi-step work, read `.kiro/plan/task_plan.md` and `.kiro/plan/progress.md` first and update the planning files after meaningful phases.
- Before claiming completion, run the strongest practical verification from `GOAL.md` and update `docs/current_status.md` when artifact hashes or runtime evidence changes.
- Do not alter the vanilla `26.1.2` profile in place. Use `26.1.2-Fabric-Ebb-Test` for client testing.
