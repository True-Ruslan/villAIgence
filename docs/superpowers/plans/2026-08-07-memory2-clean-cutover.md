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
- later non-dialogue events do not consume the dialogue retrieval limit before filtering;
- Working Memory caps the result to the current 12-message policy.

Run PR CI and retain the expected missing-contract failure as canonical RED evidence.

## Task 2 — Structured DIALOGUE payload

Update `MemoryEvent` additively with optional `DialogueExchange` and a source-compatible constructor for existing non-dialogue producers/tests.

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

Keep the inherited `OpenAIChatAI` call surface bounded by retaining `PersistentChatMemory` only as a temporary no-storage compatibility adapter:

- reads map directly to `Memory2DialogueHistory` when the inherited persistent-recall toggle and `memory2Enabled` allow it;
- adapter writes are intentionally no-op;
- persistent writes remain solely in the existing post-success `ChatAI → Memory2DialogueLifecycle` boundary;
- no code path resolves or creates `memory.json`.

When persistent recall is disabled, retain only the existing process-local non-persistent fallback.

Keep historical version-2 config fields deserializable in this package to avoid an unrelated config-schema migration. Document that the old sizing fields no longer control a separate persistent store.

## Task 5 — Remove legacy runtime store

Delete:

- `ConversationMemoryStore`;
- `MemoryMessage`;
- dedicated legacy recovery/store tests.

Retain `PersistentChatMemory` only as the bounded no-storage adapter above; deleting/renaming that inherited call-surface wrapper is optional future API cleanup rather than data migration.

Add a source-policy regression test requiring production code to contain no persistent `memory.json` resolution, no references to the removed store class, and no second persistent dialogue writer through the adapter.

## Task 6 — Update current production acceptance/recovery

Change exact current production fixture/recovery expectations from six auxiliary LivingWorld stores to five:

```text
memory2.json
semantic-memory.json
relationships.json
voices.json
operator-lore.json
```

Update persistence-recovery scripts, fixtures, current CI/nightly/release assertions and focused tests so `memory2.json` remains fully covered and unknown/persistence changes still fail closed to the complete required matrix.

Keep immutable `Release Recovery` version-aware: it checks out the target release commit, so it must accept a non-empty all-PASS matrix and rely on that tag commit's own tests for the exact historical store count. This preserves six-case `0.1.26` recovery while allowing future five-store releases.

## Task 7 — Documentation and state

Update:

- `docs/livingworld/MEMORY_2.md`;
- `docs/livingworld/CONFIGURATION.md`;
- `docs/livingworld/VALIDATION_MEMORY2_CLEAN_CUTOVER.md`;
- `docs/PROJECT_STATE.md`;
- `docs/ROADMAP.md`;
- relevant current acceptance/recovery docs and workflow contracts.

Preserve old release/validation records as historical evidence.

## Task 8 — Verification and review

Required exact-head checks:

- common tests;
- production acceptance contract tests;
- exact production startup/stop/restart;
- current five-store persistence recovery matrix;
- Fabric GameTests;
- Fabric build;
- NeoForge build;
- package smoke/identity;
- repository security policy;
- production soak;
- release dry-run where triggered;
- version-aware immutable release-recovery validation where triggered;
- independent full-diff review with no unresolved P0/P1/P2 finding.

Do not create a release/tag in this package. Installed clean-world acceptance follows on an exact candidate after merge/release preparation is explicitly authorized.
