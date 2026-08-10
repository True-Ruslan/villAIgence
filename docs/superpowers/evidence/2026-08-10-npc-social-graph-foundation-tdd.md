# NPC Social Graph Foundation — TDD Evidence

Date: 2026-08-10
Base: `05e48164b633a4989f95fe3b2ef12a1c7b33f2ea`
PR: #151
Branch: `feat/npc-social-graph-foundation`

## Authority boundary

This is the first 0.3 persistence foundation. Existing MCA `VillagerBrain.PERSONALITY` / `Personality` tracked entity state remains the canonical persistent personality source. The feature adds a separate directed NPC→NPC relationship domain and does not reinterpret the existing NPC×player `relationships.json` store.

```text
MCA entity NBT Personality    -> NPC personality source
relationships.json            -> NPC × player relationship state
npc-social-graph.json         -> directed NPC × NPC social state
```

No prompt/dialogue/behavior weighting, provider scoring, Semantic authority change, autonomous social evolution or release publication is introduced.

## Stage 1 — pure bounded state/delta

### RED

Tests-only commit:

```text
0084bbab298c27379099adf014d97125b14421ba
VillAIgence CI #2553 / 31406243906
```

Production compilation succeeded; test compilation failed with exactly 20 missing-symbol errors for the absent `NpcSocialState` / `NpcSocialDelta` API.

### GREEN

Implementation head:

```text
8574e40b8bce8f862ec68de6ba1e7b5ae972c802
VillAIgence CI #2557 / 31406602406 — SUCCESS
```

The first CI job attempt on this exact SHA was interrupted before tests by an external `plugins.gradle.org` HTTP 503 while resolving `moddev-gradle`. The same job was rerun without changing the SHA and the common suite passed. This infrastructure incident is not counted as a product RED.

Result:
- all state dimensions clamp to `[-100,+100]`;
- delta magnitude is sanitized before application;
- negative mutation limits are interpreted by absolute magnitude;
- `Integer.MIN_VALUE` is handled through `long` before absolute-value conversion;
- null delta is a no-op.

## Stage 2 — directed store identity and persistence

### RED

Tests-only commit:

```text
cc467e9f3048e33a569a751a69181e1d050dd3e5
VillAIgence CI #2559 / 31407033292
```

Production compilation succeeded; test compilation failed with exactly 25 missing-symbol errors for absent `NpcSocialGraphStore` / `NpcSocialGraphMutation` APIs.

### GREEN

Implementation head:

```text
2b6492f4857720995780684e187301d35e705033
VillAIgence CI #2563 / 31407340646 — SUCCESS
```

Result:
- A→B and B→A persist independently;
- self/null pairs return `INVALID_PAIR`;
- exact before/after state is returned;
- exact no-op returns `NO_CHANGE` without a write;
- a transition back to neutral removes the persisted edge;
- storage uses world-local `npc-social-graph.json`, format version 1, through existing atomic `JsonStoreRecovery`.

## Stage 3 — hard outgoing-edge capacity

### RED

Tests-only commit:

```text
bedf3a4402684ead876a1cb13335f5164c3202f8
VillAIgence CI #2565 / 31407649548
736 tests / exactly 2 failures
```

Only the two intended overflow assertions failed:
- sixty-fifth outgoing edge was not yet rejected;
- after neutralizing and refilling one slot, the next overflow was not yet rejected.

Existing-edge updates at capacity and per-source capacity isolation already passed.

### GREEN

Implementation head:

```text
5acb5f2ecdc3543795dd056ab67f84d4db3aa656
VillAIgence CI #2567 / 31407979973 — SUCCESS
```

Result:
- max 64 non-neutral outgoing edges per source NPC;
- edge 65 returns `CAPACITY_REACHED`;
- no retained edge is silently evicted;
- an existing edge may still change at capacity;
- neutralization frees exactly one slot;
- one source NPC cannot consume another source NPC's capacity.

## Stage 4 — hostile/corrupt load sanitation

### Initial RED

Tests-only commit:

```text
8d914cdecedc9afe1f2892df75b481305db62351
VillAIgence CI #2569 / 31408308734
739 tests / exactly 2 failures
```

Failures proved that:
- alternate UUID casing could create two raw keys for one logical pair;
- malformed/self/neutral persisted entries could still consume outgoing-capacity work.

The out-of-range reconstruction assertion already passed because the bounded record constructor clamps deserialized values.

### Strengthened RED before implementation

Additional tests-only commit:

```text
d519323de034697479c9c48f4ee85e1102c22401
VillAIgence CI #2571 / 31408766658
```

Added a 65-valid-edge persisted source. The required behavior is fail-closed for the whole overloaded source rather than selecting an order-dependent set of 64 implicit survivors. An independent source in the same file must remain live.

### GREEN

Implementation head:

```text
d3086c2d811fddf2808ac093f4da32ab3f09208f
VillAIgence CI #2572 / 31408892854 — SUCCESS
```

Result:
- malformed/self/null/neutral records do not enter live state;
- UUID pair keys are canonicalized;
- duplicate canonical logical pairs fail closed rather than choosing a raw-key winner;
- values are rebuilt through bounded state construction;
- a source with more than 64 valid edges is dropped as one local corrupted set;
- unrelated source NPC edges remain available;
- sanitation itself does not rewrite disk; a later legitimate mutation atomically saves canonical live state.

## Stage 5 — sixth canonical persistence/recovery store

### RED 1 — recovery matrix contract

Tests-only commit:

```text
573f93d40f1d4a1fc7ec36eb9e51a07d3b5e91c7
VillAIgence CI #2575 / 31409289304
```

Java/common and loader/GameTest stages were green. Production acceptance contract tests then failed exactly one Python assertion because the implementation still exposed five canonical stores while the new contract required six (`AssertionError: 6 != 5`).

### RED 2 — workflow governance

Tests-only commit:

```text
e2b3ac1114fbe4200e2b7fa961aae908574fde5e
VillAIgence CI #2579 / 31409988341
740 tests / exactly 1 failure
```

Only `PersistenceRecoveryGatePolicyTest.nightlyAndReleaseGatesExecuteTheSameCurrentSixStoreRecoveryMatrix` failed because current nightly/release gates still required `len(cases) != 5`. The immutable release-recovery workflow remained intentionally version-aware and was not changed to hard-code the current six-store count.

### GREEN

Integrated Stage-5 head:

```text
af54b747d5f0ddd5a0c0bb8d62dadcf7824e2682
VillAIgence CI #2593 / 31411514871 — SUCCESS
VillAIgence Production Soak #385 / 31411514868 — SUCCESS
```

Changes:
- `npc-social-graph.json` became the sixth current canonical persistent store;
- recovery matrix gained an incompatible-schema canonical corruption case with exact `.corrupt` preservation;
- recovery fixture creates and verifies a real directed NPC edge;
- regular production fixture creates one exact graph edge on first startup and verifies that exact value after restart;
- current CI/nightly/release gates require six recovery cases;
- immutable historical release recovery remains target-version-aware.

Main CI proved:
- common/mock-provider tests;
- Fabric GameTests and Fabric/NeoForge builds;
- production acceptance contract suite;
- real Fabric production startup and clean restart with all six canonical stores;
- destructive six-store recovery with sibling-store independence and idempotent second startup;
- distributable package verification.

## Stage 6 — preservation / pressure

Tests-only pressure commit:

```text
39129d6e74fa68fbf5accdc516d2200be30b3b2b
VillAIgence CI #2595 / 31412448655
741 tests / exactly 1 failure
```

This was a fixture mistake, not a product failure. The first generated pressure state at `(sourceIndex=0,targetIndex=0)` was exactly `NEUTRAL`, so the correct runtime outcome was `NO_CHANGE` while the test incorrectly expected `APPLIED`.

Fixture-only correction:

```text
040dda3704ce432a29a501714f324654e8f7fc49
VillAIgence CI #2597 / 31412768783
common + deterministic mock-provider tests — SUCCESS
```

The corrected preservation test requires every generated edge to be non-neutral and proves without production changes:
- 12 independent source NPCs × 8 directed edges;
- reverse direction remains neutral unless explicitly written;
- all 96 expected edges reproduce on a fresh-root reload;
- graph writes leave the existing NPC×player `relationships.json` byte-for-byte unchanged;
- the existing NPC×player relationship value remains unchanged after graph pressure/reload.

## Persistence and authority result

The current source capability adds one new auxiliary world file and one current recovery case. It does not alter the existing installed `0.2.0+1.21.1` acceptance record, which historically covered five stores. Immutable release recovery remains version-aware so historical tags retain their own matrix contract.

No provider request/response field/call, public configuration, Semantic memory field/version, NPC×player relationship schema, settlement routing rule, prompt layer, client authority or release identity change is introduced.

Final exact-head delivery workflow IDs belong in PR #151 after changelog/evidence/review freeze so recording them does not invalidate the verified source SHA.
