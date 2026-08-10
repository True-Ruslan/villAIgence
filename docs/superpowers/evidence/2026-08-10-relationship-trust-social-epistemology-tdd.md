# Relationship/Trust Social Epistemology — TDD Evidence

Date: 2026-08-10
Base: `d259572124f0a01b400e189e359c960e971cdae6`
PR: #149
Branch: `feat/relationship-trust-social-epistemology`

## Contract

This slice connects the existing server-owned NPC×player relationship state to current prompt treatment of player-origin Semantic BELIEF without making social affinity a truth authority.

```text
retained eligible player-origin BELIEF
+ exact retained source evidence / v2 provenance
+ exact source player
+ current listener NPC × source player relationship trust
→ trustDelta = trust / 10
→ clamp trustDelta to [-10,+10]
→ effectiveBeliefConfidence = clamp(persistedConfidence + trustDelta, 0,100)
→ prompt annotation only
```

Only `trust` participates. `respect`, `fear` and `affinity` do not. Persisted Semantic confidence, ranking, retention, provenance, truth class, contradiction state and settlement routing remain unchanged. FACT is never socially weighted.

## Staged RED / GREEN ledger

### 1. Pure trust policy / truth boundary

RED — tests only:

```text
commit: 7c16a0ce7b1558dda948e8c9739f52bd3adcd4b1
VillAIgence CI #2512
run: 31389219144
result: FAILURE as intended
```

Observed: production compilation completed, then test compilation failed with exactly 16 `cannot find symbol` errors for the absent `SocialEpistemicPolicy` / `SocialEpistemicState` API. This was the intended feature-missing RED rather than setup noise.

The resulting minimal policy keeps the effect bounded to ±10, clamps the derived value to the existing 0..100 confidence domain, uses only `LivingWorldRelationshipState.trust()` and refuses FACT/missing-source-player input.

### 2. Exact source-player evidence resolution

The implementation adds a fail-closed `SocialEpistemicSourceResolver` rather than treating `relatedEntities` as source authority.

Direct `BELIEF / PLAYER_TOLD` resolution:

- refuses social derivation before any event lookup when more than 32 source evidence IDs are attached to one retained claim;
- within that bound, rereads every retained source event under the Semantic owner NPC;
- requires exact `DIALOGUE / PLAYER_TOLD` evidence with dialogue payload;
- derives the player only as the unique non-owner participant;
- requires every retained supporting source event to identify the same player;
- missing, malformed or conflicting evidence resolves to no social state.

Transferred `BELIEF / NPC_TOLD` resolution:

- uses the existing canonical `KnowledgeTransferProvenanceResolver`;
- requires v2 origin `BELIEF / PLAYER_TOLD`;
- rereads the exact origin Semantic entry;
- requires `KnowledgeTransferProvenancePolicy.originMatchesSource(...)`;
- resolves the original player only from the origin entry's retained direct dialogue evidence under the same 32-ID bound;
- FACT-origin and INFERRED-origin rumors receive no player-trust state.

This preserves the existing rule that forgotten provenance is not reconstructed from surviving claim prose or scope.

### 3. Derived resolver / relationship source of truth

`SocialEpistemicResolver` reads exactly:

```text
LivingWorldRelationshipStore.get(currentEntry.ownerNpcId(), resolvedSourcePlayerId)
```

Tests prove that an unrelated player's relationship state is ignored and that changing the retained trust relationship changes the next derived effective confidence without rewriting the Semantic entry.

### 4. Prompt integration after existing rank-to-6 selection

Integrated implementation head before preservation expansion:

```text
commit: 12c276dea921b6a29f2240399b24220e0b4765d0
Repository security policy #2165 / 31391568759 — SUCCESS
VillAIgence CI #2530 / 31391566497 — SUCCESS
VillAIgence Production Soak #357 / 31391566531 — SUCCESS
VillAIgence GitHub Release #690 / 31391566550 — SUCCESS
```

`SemanticMemoryContextProvider` performs existing player eligibility, long-horizon candidate selection and ranking before consulting the relationship store. Therefore trust cannot pull an otherwise unselected BELIEF into the six prompt slots and cannot remove a selected BELIEF.

The formatter keeps the historical field:

```text
confidence=<persisted confidence>
```

and, only after exact validation, appends:

```text
socialEpistemics={trustDelta=..., effectiveBeliefConfidence=...}
```

The source player UUID is never rendered. Conditional prompt guidance explicitly states that social epistemic metadata is personal trust treatment only and never changes truth class or creates FACT authority.

## Preservation-only evidence

Additional tests were added without production changes:

```text
commit: 868ffd5b1acf52a103f7fc2991cf9822f3818376
VillAIgence CI #2532 / 31397163335 — SUCCESS
Repository security policy #2167 / 31397164031 — SUCCESS
VillAIgence Production Soak #358 / 31397165601 — SUCCESS
VillAIgence GitHub Release #691 / 31397163263 — SUCCESS
```

Preservation coverage proves:

1. **fresh-root determinism** — copying `memory2.json`, `semantic-memory.json` and `relationships.json` to a fresh world root reproduces identical rendered social metadata and does not change persisted Semantic confidence;
2. **privacy before allocation** — 24 higher-confidence foreign-player private claims plus relationship records consume zero slots for the current player, while the current player's eligible claim retains the existing prompt bound;
3. **contradiction coexistence** — two retained opposing player-origin BELIEFs may derive different trust-adjusted effective confidence while the existing contradiction relation remains live and neither persisted claim is changed/deleted;
4. **transformation/fallibility coexistence** — a real player-origin rumor transferred with `OMIT_TRAILING_SENTENCE` retains `sourceDistanceHops=1`, `transformationsUsed=1` and its transformed statement while adding only the derived trust annotation;
5. **no UUID disclosure** — source player identity remains server-side and is not included in prompt rendering.

No preservation test required a production correction.

## Review hardening — bounded source-evidence work

Base→head review found one material boundedness issue before feature freeze: Semantic consolidation unions source event IDs, so a long-lived repeated claim can accumulate a source list larger than the bounded event store. The initial social resolver walked that whole list on the prompt path, and each `MemoryEventStore.findById(...)` itself scans the retained per-NPC event list. That made social derivation potentially scale with accumulated claim history rather than a fixed prompt-time budget.

Behavioral RED — tests only:

```text
commit: f0d6ae69d4680141d29909d55ad56d4ffab3cd0d
VillAIgence CI #2538
run: 31398108016
result: FAILURE as intended
722 tests / exactly 1 failure
```

The sole failure was `SocialEpistemicBoundednessTest.excessiveDirectSourceEvidenceFailsClosedBeforeSocialDerivation()`: the pre-fix resolver accepted 33 valid direct source events, proving the work was not hard-bounded.

Minimal production correction:

```text
commit: 1d7dac0f9c06d29ebf85e35889b56bbee1b79dd7
MAX_SOURCE_EVIDENCE_IDS = 32
```

The resolver now checks source-list size before its first event lookup. More than 32 source IDs makes social source resolution `UNRESOLVED` for prompt purposes and falls back to the existing unannotated Semantic line. No Semantic evidence is deleted or rewritten, and normal claims at or below the cap retain exact all-source consistency validation.

VillAIgence CI #2540 confirmed `common + deterministic mock-provider tests: SUCCESS` on the correction head before documentation synchronization. Final delivery gates are rerun after the source head is frozen.

## Authority / compatibility result

The implementation adds no provider request/response field/call, public configuration, world file, persistence schema/version, migration/backfill, Semantic field, relationship field, transfer evidence field, settlement routing rule, NPC↔NPC social graph or release identity change.

The feature does not mutate or rank by social trust. It computes a current non-persistent interpretation after the existing Semantic selection boundary. Current server-observed facts remain authoritative, BELIEF remains BELIEF, contradiction remains disagreement rather than verdict, and missing/excessive evidence fails soft to the pre-feature Semantic rendering.

Final exact-head delivery workflow IDs belong in PR #149 after changelog/evidence/review freeze so recording them does not invalidate the verified source SHA.
