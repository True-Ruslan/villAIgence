# Relationship/Trust Social Epistemology — Design

Date: 2026-08-10
Status: approved for implementation by project owner and canonical roadmap
Base: `1.21.1` at `d259572124f0a01b400e189e359c960e971cdae6`

## Problem

VillAIgence already stores server-owned NPC×player relationship state (`trust`, `respect`, `fear`, `affinity`) and has provenance-aware Semantic BELIEF, rumor lineage, fallibility, contradiction handling and bounded settlement dissemination. The missing connection is social epistemology: an NPC should be able to rely somewhat more or less on a player-origin belief depending on its own current trust in that player, without turning social affinity into objective truth.

The current relationship store is explicitly NPC×player. There is not yet a persistent NPC↔NPC social graph. This slice therefore must not pretend that player relationship state is an NPC↔NPC trust model and must not introduce the future 0.3 graph implicitly.

## Selected product boundary

This slice changes **derived prompt-level BELIEF confidence only**.

It does **not** mutate persisted `SemanticMemoryEntry.confidence`, retention, ranking, contradiction state, FACT/BELIEF class or settlement routing.

```text
retained eligible BELIEF
+ retained source evidence/provenance
+ exact resolvable source player
+ current server-owned listenerNpc × sourcePlayer trust
→ bounded trust delta [-10,+10]
→ derived effectiveBeliefConfidence [0,100]
→ prompt annotation only
```

Why this boundary:

- persisted evidence remains historically auditable and provider-independent;
- changing a relationship later can change current personal reliance without rewriting old memory;
- no persistence/schema migration is required;
- social trust cannot affect Semantic candidate allocation, durability or contradiction existence;
- routing remains untouched until an actual NPC↔NPC social graph exists;
- `respect`, `fear` and `affinity` remain social dimensions but are not silently treated as epistemic credibility.

## Trust arithmetic

Only `LivingWorldRelationshipState.trust()` participates.

```text
trust range:                -100..100
trust delta:                trust / 10
trust delta range:          -10..10
effective confidence:       clamp(baseConfidence + trustDelta, 0, 100)
```

Java integer division toward zero is intentional:

```text
trust -100 -> -10
trust  -55 ->  -5
trust   -9 ->   0
trust    0 ->   0
trust    9 ->   0
trust   55 ->  +5
trust  100 -> +10
```

The effect is deliberately small relative to the existing 0..100 confidence domain. It models the NPC's current willingness to rely on a player-origin BELIEF, not probability of truth.

## Eligible knowledge

Social epistemology applies only to `SemanticMemoryEntry.Kind.BELIEF` whose **source player is exactly resolvable from retained server-owned evidence**.

No annotation is emitted for:

- FACT;
- INFERRED BELIEF;
- NPC_TOLD rumor whose origin is not PLAYER_TOLD;
- missing/forgotten/invalid source evidence;
- conflicting source-player evidence;
- malformed provenance;
- foreign-player memory already excluded by current eligibility.

Failure to resolve social source is fail-soft: the existing Semantic line remains unchanged.

## Exact source-player resolution

### Direct `BELIEF / PLAYER_TOLD`

For every retained `sourceEventId` used by the consolidated Semantic entry:

1. reread the event from `MemoryEventStore` using the Semantic owner NPC;
2. require `DIALOGUE / PLAYER_TOLD`;
3. require the event owner NPC to match the Semantic owner;
4. derive the player as the unique non-owner event participant;
5. require all retained supporting source events to resolve to the same player.

If any retained source event is missing, malformed or identifies a different player, social source resolution is `UNRESOLVED` and no trust effect is emitted.

### `BELIEF / NPC_TOLD` with player origin

1. resolve the current direct rumor branch through existing `KnowledgeTransferProvenanceResolver`;
2. require canonical v2 origin `BELIEF / PLAYER_TOLD`;
3. reread the exact origin Semantic entry by `originNpcId + originSemanticEntryId`;
4. require `KnowledgeTransferProvenancePolicy.originMatchesSource(...)`;
5. resolve the original player from that origin entry's retained direct `PLAYER_TOLD` source events using the same direct resolver above.

If the origin Semantic entry or its source events have been forgotten, do not reconstruct the player from claim prose or `relatedEntities`; emit no social trust metadata.

This preserves the existing rule that missing provenance is explicit and history is never fabricated from surviving prose.

## Derived state

A new non-persistent value object represents only validated derived social treatment:

```text
SocialEpistemicState
- sourcePlayerId       server-resolved, not rendered to provider
- trust                -100..100
- trustDelta           -10..10
- effectiveConfidence  0..100
```

No JSON field or new world file is added.

## Prompt rendering

Existing Semantic lines preserve their current base field:

```text
confidence=<persisted confidence>
```

When validated social epistemic state exists, append before the statement:

```text
socialEpistemics={trustDelta=-5, effectiveBeliefConfidence=45}
```

The source player's UUID is deliberately not rendered.

Semantic prompt guidance becomes conditional, like rumor fallibility guidance:

```text
Social epistemic metadata is the NPC's personal trust adjustment for a player-origin BELIEF only.
It does not change persisted confidence, ranking, provenance or truth class and never turns BELIEF into FACT.
```

Current server-observed facts continue to win on conflict.

## Retrieval/ranking boundary

`SemanticMemoryContextProvider` must preserve the existing order:

```text
player/NPC eligibility
→ long-horizon 32 candidate allocation
→ existing ranking
→ max 6 results
→ derived fallibility/social metadata
→ formatting
```

Relationship state is read **only after** Semantic selection/ranking. Therefore:

- high trust cannot make an otherwise unselected belief consume a slot;
- low trust cannot remove a selected belief;
- foreign-player claims consume zero slots before social weighting;
- trust does not alter retention/durability;
- trust does not select contradiction winners.

## Relationship source of truth

Use only:

```text
LivingWorldRelationshipStore.forWorld(worldRoot).get(listenerNpcId, sourcePlayerId)
```

Only `trust` participates. `respect`, `fear`, and `affinity` remain unchanged and available for later behavioral/personality work.

No provider/model output may set or propose trust for this calculation.

## Replay/restart semantics

Derived social epistemics contains no stored mutable state. Same:

```text
semantic-memory.json
+ memory2.json
+ relationships.json
```

must yield the same rendered social epistemic metadata after fresh-root reload.

Changing persisted relationship trust intentionally changes the next prompt's derived effective confidence without rewriting Semantic memory.

## Contradictions

Both contradictory beliefs remain retained and renderable regardless of trust.

Social trust may change the derived effective confidence shown on a selected BELIEF line, but it may not:

- create/delete contradiction evidence;
- select a contradiction winner;
- hide a claim;
- promote FACT;
- change contradiction candidate/producer policy.

## Settlement flow

Settlement routing remains exactly unchanged in this slice.

Reason: current persistent relationship state is NPC×player, while settlement dissemination selects NPC→NPC opportunities. Using player trust as an NPC-pair routing score would be semantically wrong; introducing NPC↔NPC relationship persistence belongs to the following Personality + Social Graph track.

## Persistence / provider / release compatibility

No new:

- provider request, response field or call;
- public config field/version;
- world file;
- persistence schema/version;
- migration/backfill;
- Semantic entry field;
- relationship field;
- transfer evidence field;
- release identity.

## TDD acceptance contract

Required staged evidence:

1. RED: pure bounded trust arithmetic / truth boundary API absent.
2. GREEN: trust affects BELIEF effective confidence by at most ±10; FACT is ineligible.
3. RED: exact source-player resolver API absent.
4. GREEN: direct PLAYER_TOLD resolves only from valid retained DIALOGUE evidence; missing/conflicting evidence fails closed.
5. GREEN: NPC_TOLD player-origin rumor resolves through existing v2 provenance + exact origin Semantic evidence; non-player origins do not.
6. RED: formatter/provider social metadata integration absent.
7. GREEN: social metadata is applied only after existing rank-to-6 selection; persisted confidence remains displayed unchanged.
8. Preservation: current FACT authority, contradiction coexistence, rumor fallibility/transformation rendering, prompt escaping and privacy-before-allocation remain unchanged.
9. Restart: copied `memory2.json`, `semantic-memory.json`, `relationships.json` produces identical social metadata.
10. Pressure: many foreign/private claims and many relationship records cannot expand the existing 32/6 Semantic bounds.
11. Exact-head security, CI, soak and release dry-run; publication skipped.

## Exit criterion

A selected player-origin BELIEF may carry a small deterministic current-trust adjustment in prompt context while its persisted confidence/provenance/truth class remain unchanged; source player must be recoverable from exact retained evidence; missing evidence fails closed; trust cannot affect FACT, ranking, contradiction existence or settlement routing; results are deterministic across restart and require no provider/persistence/release expansion.
