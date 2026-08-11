# Causal NPC Social Mutation — Design

Date: 2026-08-11
Base: `a178336a9346c21429adb25eca580e60adec3b94`
Track: 0.3 — Personality + NPC↔NPC Social Graph

## Goal

Make directed NPC→NPC social changes server-authoritative, causally auditable and exactly-once under replay/restart before social state is allowed to influence dialogue or autonomous behavior.

This slice starts from PR #151:

```text
MCA Personality                         → canonical persistent personality authority
relationships.json                      → NPC × player state
npc-social-graph.json                   → directed NPC × NPC current state
```

It adds a causal mutation lifecycle. It does not add provider-authored psychology, personality mutation, social prompt weighting or autonomous social evolution.

## Why Memory 2.0 alone cannot be the replay ledger

`memory2.json` and `npc-social-graph.json` are separate atomic files. Using only a Memory 2.0 audit event for deduplication leaves a crash window:

```text
apply graph delta
→ crash before audit append
→ retry
→ delta applied twice
```

Writing the audit first merely inverts the inconsistency risk.

Therefore exactly-once state mutation is guarded inside `npc-social-graph.json` itself. Graph state and the mutation frontier are written atomically by one `JsonStoreRecovery.writeAtomic(...)`. Memory 2.0 remains bounded historical process evidence, not the transaction ledger.

## Causal frontier

`npc-social-graph.json` format remains version `1` with one additive nullable/default-empty field:

```text
causalFrontiers: sourceNpcUuid → latest NpcSocialMutationCursor
```

Keeping format v1 is deliberate and safe:

- PR #151 graph files could contain directed edges but no causal mutations because no causal API existed;
- an older v1 file therefore has a semantically correct empty causal frontier;
- Gson may deserialize the new field as null, which is normalized to an empty map;
- no released/installed 0.2 persistence contract is changed.

One frontier per source NPC keeps replay metadata O(number of source NPCs), not O(total mutation history).

### Canonical cause order

For one source NPC, authoritative causes are ordered by:

```text
cause.gameTime ASC
then cause.id UUID string ASC
```

The runtime lifecycle must process a source NPC's mutations in that authoritative order. This is compatible with server-thread/event-driven social evolution and prevents an unbounded mutation-ID ledger.

### One social mutation per source event

The deterministic mutation identity is:

```text
UUIDv3/nameUUID("npc-social-causal-mutation-v1\n" + sourceNpcId + "\n" + causeEventId)
```

Target UUID and proposed delta are deliberately not part of the ID. The same source event therefore has one social-mutation identity. Reusing it with another target or bounded delta is an explicit conflict, not a second valid mutation.

## Cursor contents

The latest cursor stores only structured process state:

- mutation ID;
- source NPC UUID;
- target NPC UUID;
- source/cause event UUID;
- cause `gameTime`;
- bounded requested `NpcSocialDelta`;
- actual applied delta derived from exact before/after state;
- exact before `NpcSocialState`;
- exact after `NpcSocialState`;
- terminal outcome: `APPLIED | NO_CHANGE | CAPACITY_REACHED`.

No generated psychological explanation is stored as authority.

## Atomic graph mutation semantics

New causal store API accepts exact source, target, cause ID/time, proposed delta and a hard per-mutation delta limit.

For a valid new cause:

1. clamp proposed delta using existing `NpcSocialDelta.sanitized(...)`;
2. compare the cause against the source frontier;
3. reject stale or conflicting causes before edge mutation;
4. compute exact before/after state;
5. apply existing 64-edge capacity policy;
6. write both edge state and new cursor in one atomic graph save.

Terminal outcomes advance the frontier even when no edge changes:

- `APPLIED` — edge changed;
- `NO_CHANGE` — valid cause consumed but bounded delta changes nothing;
- `CAPACITY_REACHED` — valid cause consumed but new edge could not be admitted.

This prevents a historical cause from becoming effective later merely because capacity or state changed.

Exact same cause + same target + same bounded request returns `REPLAYED` with the existing cursor and never writes or applies a second delta.

Same cause identity with different target/request returns `CONFLICTING_CAUSE`.

An older authoritative cause returns `STALE_CAUSE`.

## Frontier corruption policy

Exactly-once metadata is more safety-sensitive than an ordinary malformed edge.

Load normalization:

- absent/null frontier map → empty map;
- canonical source keys are required;
- cursor source must match its map key;
- mutation/cause/target IDs must be non-null and source != target;
- states/deltas are rebuilt through bounded constructors;
- duplicate canonical source keys fail closed for that source;
- malformed attributable cursor state blocks causal mutation for that source until the file is repaired/recovered rather than silently discarding replay protection.

Ordinary edge sanitation from PR #151 remains unchanged.

## Source evidence authority

The public lifecycle accepts an exact persisted cause event ID, not arbitrary prose.

First-slice eligible causes are intentionally narrow:

```text
ownerNpcId == sourceNpcId
provenance == SYSTEM_OBSERVED
type in {OBSERVATION, ACTION}
participants contains targetNpcId
```

The event is retrieved with `MemoryEventStore.findById(sourceNpcId, causeEventId)`. No `Integer.MAX_VALUE` history traversal is used by the new lifecycle.

DIALOGUE, PLAYER_TOLD, NPC_TOLD, INFERRED and free-form explanations are not social mutation authority in this slice. Later dialogue-specific social effects require their own explicit validation policy.

## NPC identity authority

The low-level graph store cannot infer entity type from a UUID.

The lifecycle therefore depends on a small `NpcIdentityAuthority` boundary:

```text
boolean isNpc(UUID id)
```

Production/server implementation resolves the UUID in the current `ServerLevel` and accepts only `VillagerEntityMCA`. Unit tests may inject a deterministic authority implementation.

Both source and target must resolve as NPCs before the graph API is called. Self pairs fail closed.

## Memory 2.0 audit evidence

A successful `APPLIED` graph mutation may append one structured `NPC_SOCIAL_CHANGE / SYSTEM_OBSERVED` Memory 2.0 event owned by the source NPC.

The event ID equals the deterministic mutation ID, making audit append replay-idempotent.

Structured payload records:

- mutation ID;
- target NPC UUID;
- cause event UUID/time;
- bounded requested delta;
- actual applied delta;
- exact before/after social state.

The summary is server-authored and informational. The payload is the auditable data.

Audit retention is best-effort under existing bounded Memory 2.0 pressure. Loss or immediate rejection of the audit event does not roll back graph state and cannot weaken the graph frontier replay guard.

`NPC_SOCIAL_CHANGE` is excluded from the generic episodic prompt path in this slice. The next 0.3 slice will render current bounded social/personality state from a dedicated snapshot instead of replaying stale mutation logs into prompts.

`NPC_SOCIAL_CHANGE` is never admitted as Semantic FACT automatically.

## Source evidence loss after application

A successful graph mutation remains authoritative current social state even if its original source event or Memory 2.0 audit event is later forgotten.

On a later lifecycle retry:

- if exact source evidence is no longer retained, return `SOURCE_NOT_RETAINED` before any new mutation;
- graph state is unchanged;
- the persisted frontier still prevents the old cause from being reapplied through the low-level causal API;
- no prose or stale audit data reconstructs missing source evidence.

## Memory retention

`NPC_SOCIAL_CHANGE` receives the same retention type contribution as existing `RELATIONSHIP_CHANGE`: significant bounded social process history, but not immortal.

It remains subject to existing Memory 2.0 capacity and game-time decay.

## Compatibility and truth boundaries

This slice changes no:

- provider request/response schema or call count;
- public configuration;
- Semantic FACT/BELIEF schema, confidence, ranking, provenance or contradiction semantics;
- NPC×player `relationships.json` behavior;
- MCA Personality source;
- settlement knowledge routing;
- voice/UI/client authority;
- official release identity.

`memory2.json` remains format version 1 with an additive nullable structured field. Historical records deserialize with no NPC-social payload.

`npc-social-graph.json` remains format version 1 with an additive default-empty frontier because pre-feature files contain no causal-mutation history to migrate.

## Performance boundary

The lifecycle is event-driven and performs:

- two NPC identity lookups;
- one exact source-event lookup by ID in one NPC's bounded Memory 2.0 list;
- one source-frontier lookup;
- one exact pair lookup;
- existing new-edge capacity scan only when a genuinely new edge is requested.

No all-pairs prompt allocation and no provider call are added.

The PR #151 flat-map new-edge scan remains a documented P3 and must be indexed/partitioned before high-frequency autonomous social evolution.

## Production / test evidence

TDD must prove:

1. deterministic mutation identity;
2. exact replay does not double-apply;
3. same-cause conflicting target/delta fails closed;
4. stale/out-of-order cause fails closed;
5. valid `NO_CHANGE` and `CAPACITY_REACHED` consume the cause frontier;
6. graph state + frontier survive fresh-root reload;
7. source evidence validation uses exact `findById` semantics;
8. non-NPC/self/foreign-owner/non-system/non-action-observation causes reject before graph mutation;
9. applied audit event is structured, bounded, replay-idempotent and excluded from generic prompt context;
10. audit/source forgetting does not roll back or duplicate graph state;
11. NPC×player relationship and Semantic stores remain unchanged;
12. multi-source/multi-target pressure remains deterministic;
13. production startup/restart preserves causal frontier and exact graph state;
14. current six-store recovery remains green;
15. GameTest/server identity authority accepts MCA villagers and rejects a player/non-MCA entity.

## Slice exit criterion

An exact retained server-observed event can cause exactly one bounded directed NPC social mutation. Exact replay/restart cannot duplicate the effect; conflicting/stale causes fail closed; current graph state and the latest causal frontier are atomically consistent; bounded historical audit evidence is inspectable while retained; source/audit forgetting is honest; and no social value becomes FACT, Semantic authority or provider-owned state.

The next slice may then build a bounded read-only dialogue/behavior snapshot from existing MCA Personality plus only the directly relevant NPC pair edge.