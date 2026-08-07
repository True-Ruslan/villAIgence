# Memory 2.0 Clean Cutover Implementation Plan

Date: 2026-08-07

## Goal

Make `memory2.json` the sole persistent dialogue-memory source and remove the runtime/recovery dependency on legacy `memory.json`, without building a legacy importer.

## Task 1 — RED dialogue-history contract

Add focused common tests that require:

- structured `MemoryEvent.DialogueExchange` payload;
- exact player/NPC isolation;
- chronological `user`/`assistant` reconstruction;
- delimiter-like text round-trips without parsing `summary`;
- later non-dialogue events do not starve dialogue history;
- Working Memory caps the result to the current 12-message policy.

Run PR CI and retain the expected missing-contract failure as canonical RED evidence.

## Task 2 — Structured DIALOGUE payload

Update `MemoryEvent` additively with optional `DialogueExchange` and a source-compatible legacy constructor for existing event producers.

Update `DialogueMemoryAdapter` so both `summary` and payload are built from the same normalized, 240-code-point-bounded player/NPC utterances. Preserve the existing deterministic turn ID and provenance/scores.

## Task 3 — Deterministic Memory 2.0 dialogue retrieval

Extend `MemoryEventStore` with a bounded filtered retrieval seam that filters before limiting results.

Add `Memory2DialogueHistory`:

```text
MemoryEventStore
→ DIALOGUE + exact player + structured payload
→ newest eligible exchanges
→ chronological user/assistant messages
→ WorkingMemoryOrchestrator
```

Malformed/legacy DIALOGUE events without structured payload are ignored.

## Task 4 — Production prompt cutover

Change `OpenAIChatAI` persistent dialogue reads to `Memory2DialogueHistory` when `memory2Enabled=true`.

Remove persistent dialogue writes from `OpenAIChatAI`; persistent writes remain solely in the existing post-success `ChatAI → Memory2DialogueLifecycle` boundary.

When Memory 2.0 is disabled, retain only the existing process-local non-persistent fallback.

Remove retired legacy persistent-memory configuration fields and update configuration docs/tests as needed without changing the config version solely for ignored historical JSON fields.

## Task 5 — Remove legacy runtime store

Delete:

- `PersistentChatMemory`;
- `ConversationMemoryStore`;
- dedicated legacy recovery/store tests.

Add a source-policy regression test requiring production code to contain no persistent `memory.json` path and no references to the removed store classes.

## Task 6 — Update production acceptance/recovery

Change exact production fixture/recovery expectations from six canonical LivingWorld stores to five:

```text
memory2.json
semantic-memory.json
relationships.json
voices.json
operator-lore.json
```

Update persistence-recovery scripts, fixtures, selectors and focused tests so `memory2.json` remains fully covered and unknown/persistence changes still fail closed to the complete required matrix.

## Task 7 — Documentation and state

Update:

- `docs/livingworld/MEMORY_2.md`;
- `docs/livingworld/CONFIGURATION.md`;
- `docs/PROJECT_STATE.md`;
- `docs/ROADMAP.md`;
- relevant acceptance/recovery docs whose current contract says six stores.

Preserve old release/validation records as historical evidence.

## Task 8 — Verification and review

Required exact-head checks:

- common tests;
- production acceptance contract tests;
- exact production startup/stop/restart;
- persistence recovery matrix;
- Fabric GameTests;
- Fabric build;
- NeoForge build;
- package smoke/identity;
- repository security policy;
- applicable supply-chain/release dry-run workflows;
- independent full-diff review with no unresolved P0/P1/P2 finding.

Do not create a release/tag in this package. Installed clean-world acceptance follows on an exact candidate after merge/release preparation is explicitly authorized.
