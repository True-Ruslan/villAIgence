# Causal NPC Social Mutation — TDD Evidence

Date: 2026-08-11
PR: #153
Base: `a178336a9346c21429adb25eca580e60adec3b94`
Track: 0.3 — Personality + NPC↔NPC Social Graph

## Scope

This ledger records staged RED→GREEN evidence for the server-owned causal NPC↔NPC social mutation lifecycle. The slice deliberately does not add provider-authored social scoring, prompt weighting, autonomous relationship evolution, public configuration, Semantic truth mutation, or a release publication.

## TDD stages

### 1. Atomic graph causal frontier

RED head: `d39e80d3…`

- production sources compiled;
- `common:compileTestJava` failed with exactly 54 missing-symbol errors for the not-yet-existing causal mutation identity/cursor/result/store APIs.

GREEN head: `efe5085d…`

- introduced deterministic mutation identity and one latest causal frontier per source NPC;
- graph edge state and frontier are persisted by one atomic graph-store write;
- exact replay, conflict, stale ordering, `NO_CHANGE`, and `CAPACITY_REACHED` semantics passed the common suite.

### 2. Frontier corruption and old-v1 compatibility

RED head: `6864330e…`

- 754 tests executed;
- exactly 3 behavioral failures demonstrated missing fail-closed handling for duplicate canonical frontier sources, key/source mismatch, and inconsistent applied delta;
- old format-v1 files without a causal frontier already remained compatible.

GREEN head: `c9138d82…`

- source-local frontier corruption became fail-closed;
- ambiguity markers survive unrelated saves/reloads instead of silently restoring replay authority;
- common suite passed.

### 3. Memory 2.0 audit contract

RED:

- production compilation remained green;
- test compilation failed only on the absent `NpcSocialMutationEvidence`, `NPC_SOCIAL_CHANGE` event type, and new structured accessor (6 missing-symbol errors).

GREEN head: `346e7cb6…`

- `memory2.json` stayed format v1 with additive nullable structured evidence;
- the pre-feature canonical `MemoryEvent` constructor shape remained source-compatible;
- `NPC_SOCIAL_CHANGE` received bounded retention parity with `RELATIONSHIP_CHANGE`;
- generic Memory 2.0 prompt loading excludes the new audit event.

### 4. Applied-mutation audit adapter

RED:

- test compilation failed only on the absent `NpcSocialMutationMemoryAdapter` (3 missing-symbol errors).

GREEN head: `8f7b6cd5…`

- only a primary graph `APPLIED` result can become one structured audit event;
- audit identity equals the deterministic social mutation identity;
- replay/no-op/capacity results do not fabricate new audit history.

### 5. Server-owned lifecycle authority

RED head: `5c944d69…`

- production compilation passed;
- test compilation failed with exactly 48 errors for the absent `NpcIdentityAuthority`, lifecycle, and lifecycle-result APIs.

GREEN head: `358112a0…`

- lifecycle uses exact `MemoryEventStore.findById(sourceNpcId, causeEventId)` lookup;
- only retained `SYSTEM_OBSERVED` `OBSERVATION|ACTION` events that include the target are mutation authority;
- source and target NPC identity are validated before graph mutation;
- audit append is bounded and best-effort after a primary graph commit.

### 6. Preservation / crash-window semantics

Preservation-only head: `e9044707…`

No production correction was required. Tests proved:

- exact replay is byte-idempotent;
- graph state remains authoritative after source/audit forgetting;
- a graph-only crash window returns `REPLAYED` and does not backfill or duplicate audit history;
- no Semantic state is created by the social mutation lifecycle;
- NPC×player `relationships.json` remains byte-independent;
- multiple source frontiers remain isolated and deterministic after fresh-root reload.

### 7. Real server NPC identity authority

RED head: `64fb0a37…`

- common suite passed;
- `:fabric:compileGametestJava` failed only because `ServerLevelNpcIdentityAuthority` did not yet exist.

GREEN head: `6f9a0ac2…`

- real Fabric GameTest accepts a live `VillagerEntityMCA` UUID;
- the same authority rejects a non-MCA entity and an unknown UUID;
- GameTest and both supported loader builds passed.

### 8. Attributable malformed-key hardening

RED head: `26ba25915ba56f8c0e74fbd783f3c91bb1bc12bc`

- CI #2652 executed 774 tests;
- exactly 1 behavioral failure showed that a malformed map key with a valid cursor `sourceNpcId` was preserved but did not block that attributable source.

GREEN head: `d42f14447b1478079240f0076c0ea60a581da092`

- malformed-key cursor payloads now block their attributable source;
- the fail-closed marker survives unrelated save/reload.

### 9. Production restart fixture

Tests-only head: `5d1adba29e653b038a14582b6ab5f6c0c2d2fea6`

The existing production acceptance fixture now exercises the causal lifecycle across real server startup/restart:

- first start creates exact retained `SYSTEM_OBSERVED` source evidence and admits one social mutation;
- later start accepts only `REPLAYED`, not another delta;
- exact directed graph state and structured `NPC_SOCIAL_CHANGE` evidence must remain present.

### 10. Malformed cursor decode isolation

RED head: `9acbdccd12de71677a0a4eb1e8100d2ce5ac70f2`

- CI #2658 executed 775 tests;
- exactly 1 behavioral failure proved that a frontier missing required `mutationId` could throw during Gson record construction before source-local sanitation;
- shared `JsonStoreRecovery` then treated the entire graph file as corrupt, which could discard otherwise-valid edges and replay metadata.

GREEN head: `8033d051ff74c523cb381d893c3cabc684902e4a`

- runtime `NpcSocialMutationCursor` invariants remain strict;
- a persistence-only tolerant decoder converts malformed cursor payloads into a deliberately invalid bounded sentinel;
- source-local sanitation can therefore block the attributable source without escalating one malformed cursor into whole-file recovery;
- the 775-test common suite passed, including preservation of valid directed graph state.

## Verified architectural invariants

- Exactly-once mutation authority is the atomic graph frontier, not the bounded Memory 2.0 audit log.
- One cause event has one deterministic mutation identity for one source NPC; target/delta reuse is a conflict.
- `NO_CHANGE` and `CAPACITY_REACHED` consume authoritative cause order so an old cause cannot become effective later.
- Malformed attributable frontier state fails closed per source and cannot silently regain mutation authority after an unrelated save/restart.
- Source/audit forgetting never rolls back current graph state and never permits a historical cause to be applied twice.
- Social audit evidence is process history, not Semantic FACT/BELIEF authority and not generic prompt context.
- MCA `Personality` remains the canonical persistent personality authority; NPC×player `relationships.json` remains a separate domain.

## Delivery evidence policy

The final exact-head Repository security policy, VillAIgence CI, Production Soak, and GitHub Release dry-run identifiers are recorded in PR #153 after the final documentation/changelog commits. Release publication must remain skipped; no official release identity changes in this slice.
