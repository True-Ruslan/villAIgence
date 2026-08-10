# NPC Social Graph Foundation — Design

Date: 2026-08-10
Base: `05e48164b633a4989f95fe3b2ef12a1c7b33f2ea`
Track: 0.3 — Personality + NPC↔NPC Social Graph

## Goal

Create the first explicit server-owned NPC↔NPC social domain without duplicating existing MCA personality persistence or reusing NPC×player relationship storage as a shortcut.

## Existing personality authority

MCA already persists one bounded `Personality` enum through `VillagerBrain.PERSONALITY` tracked entity state. This remains the canonical personality source for this slice.

Therefore this PR adds **no second personality JSON store**, no generated biography/profile and no provider-owned personality state. Later 0.3 slices may derive bounded dialogue/behavior snapshots from the existing persistent enum, but they must not introduce a competing source of truth.

## New NPC↔NPC social domain

The existing `LivingWorldRelationshipStore` is explicitly NPC×player. The new graph is a separate domain with separate types and persistence.

```text
source NPC UUID
+ target NPC UUID
→ directed NPC social edge
→ NpcSocialState(trust,respect,fear,affinity)
```

A→B and B→A are independent. Callers must supply NPC identities; the low-level UUID store rejects null/self pairs but cannot infer entity type from a UUID alone. NPC×player state remains in `relationships.json`, and this slice adds no runtime path that forwards player identities into the NPC social graph.

### State bounds

Each dimension is an integer in `[-100,+100]`:

- trust
- respect
- fear
- affinity

`NEUTRAL = (0,0,0,0)`.

Deltas are sanitized with a caller-provided absolute per-mutation bound. State arithmetic clamps to the global range. This mirrors the safe numeric envelope of current NPC×player relationships while preserving a distinct NPC-domain type.

## Persistence

New world-local auxiliary store:

```text
<world>/livingworld/npc-social-graph.json
```

Format version starts at `1`.

The file stores only non-neutral directed edges. Keys are canonical `sourceUuid/targetUuid` strings.

Load sanitization rejects:

- malformed keys;
- missing/null state;
- self-edges;
- duplicate logical keys after canonicalization;
- neutral persisted entries.

Recovered values are reconstructed through the bounded state constructor.

The store uses the existing atomic `JsonStoreRecovery` infrastructure and enters the canonical production persistence/recovery matrix in the same PR.

## Hard graph bound

`MAX_OUTGOING_EDGES_PER_NPC = 64`.

The bound applies to **stored non-neutral outgoing edges** for one source NPC.

Rules:

1. updating an already-retained edge is always allowed within numeric bounds;
2. transitioning an edge to neutral removes it and frees capacity;
3. creating a new non-neutral edge when the source already has 64 retained non-neutral outgoing edges returns `CAPACITY_REACHED`;
4. capacity overflow never evicts another edge silently;
5. invalid/self pairs return `INVALID_PAIR` and do not write;
6. exact no-op deltas return `NO_CHANGE` and do not write.

This yields O(N × 64) persisted graph growth rather than an unconstrained all-pairs graph. The current flat-map foundation performs a global edge scan when admitting a brand-new edge; this is acceptable before autonomous/runtime mutation exists, but must be replaced or indexed before high-frequency social evolution is introduced.

## Mutation result

The store exposes a deterministic result containing:

- status: `APPLIED | NO_CHANGE | INVALID_PAIR | CAPACITY_REACHED`;
- exact source/target IDs when valid;
- before state;
- after state.

This PR does not yet create autonomous social mutations or provider proposals. The explicit result is the seam for a later server-owned causal lifecycle.

## Recovery / automation

`npc-social-graph.json` becomes a canonical persistent store. The recovery matrix gains one corruption case and expected case count increases from five to six.

Production acceptance must prove:

- file is created as a valid JSON object;
- restart preserves exact state when unchanged;
- corrupt canonical graph recovers independently without modifying sibling stores.

Unit/pressure coverage must prove:

- A→B differs from B→A;
- self-edge rejection;
- state/delta clamps;
- neutral entry compaction;
- 64-edge bound with reject-new/no-eviction semantics;
- capacity is source-local;
- fresh-root reload reproduces retained graph state;
- malformed persisted keys cannot become live edges.

## Explicit non-goals

This slice does **not** add:

- dialogue/prompt rendering of personality or NPC social state;
- social-state influence on Semantic confidence, FACT/BELIEF authority or contradiction;
- settlement routing changes;
- autonomous relationship evolution;
- LLM/provider social scoring;
- public config;
- `relationships.json` migration or reinterpretation;
- NPC/player compatibility changes;
- new release/tag/publication.

## Delivery boundary

A successful slice leaves VillAIgence with two deliberately separate social stores of authority:

```text
existing MCA entity NBT Personality       → persistent NPC personality source
relationships.json                        → NPC × player relationship state
npc-social-graph.json                     → directed NPC × NPC relationship state
```

No layer may silently substitute one for another.
