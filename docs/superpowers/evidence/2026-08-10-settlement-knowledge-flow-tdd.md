# Settlement-Scale Information Flow Without Omniscience — TDD Evidence

Date: 2026-08-10
Base: `455d2ea36a393b2521346107fa6351f0a89ee0cd`
PR: #147
Branch: `feat/settlement-knowledge-flow`

## Contract

The slice adds autonomous but bounded settlement-scale knowledge propagation by reusing MCA home-village membership and the existing exact `NpcKnowledgeTransferLifecycle`. It deliberately does not create a settlement-global knowledge store.

Hard internal bounds:

```text
CYCLE_TICKS = 1200
MAX_RESIDENTS_PER_CYCLE = 16
MAX_SPEAKERS_PER_CYCLE = 4
MAX_SOURCE_CANDIDATES_PER_SPEAKER = 2
MAX_OPPORTUNITIES_PER_CYCLE = 4
MAX_FANOUT_PER_SOURCE_PER_CYCLE = 1
```

A current-cycle transfer cannot immediately become another source because eligible sources must have `entry.gameTime() < cycleStart`. Equivalent knowledge carried by multiple NPCs is keyed by normalized statement + exact canonical Semantic scope and can consume at most one fan-out opportunity per settlement cycle. One selected source/cycle has one deterministic target and no fallback retargeting.

Every successful mutation delegates to the existing source-backed NPC transfer lifecycle, so listener knowledge remains local `BELIEF/NPC_TOLD` and existing v2 provenance, transformation, privacy and contradiction policies remain authoritative.

## Staged RED / GREEN ledger

### 1. Bounded settlement selector

RED — tests only:

```text
commit: e2e0f79cf1cf3015975ee815b17b0fec3744e720
VillAIgence CI #2449
run: 31379579060
result: FAILURE as intended
```

Observed: exactly 18 compile errors for the absent `SettlementKnowledgeFlowSelector` API. No production selector existed.

GREEN:

```text
commit: 82cf5181225bc93442353213ee1f9b46cbd234a2
VillAIgence CI #2451
run: 31379822397
common + deterministic mock-provider tests: SUCCESS
```

The selector established deterministic resident rotation, the 16-resident / 4-speaker / 2-source / 4-opportunity hard bounds, one target per source/cycle, exact scope-aware listener equivalence suppression and supplied-settlement membership isolation.

### 2. Settlement lifecycle / exact transfer delegation

RED — tests only:

```text
commit: 49b648f7a79c062a753bce3e6a031712e5763ff5
VillAIgence CI #2453
run: 31380048563
result: FAILURE as intended
```

Observed: exactly 6 compile errors for the absent `SettlementKnowledgeFlowLifecycle` API.

GREEN:

```text
commit: 5774458e9514507c6f0db87e226a0d43ce4afb15
VillAIgence CI #2457
run: 31380538643
common + deterministic mock-provider tests: SUCCESS
```

Coverage proves that a selected opportunity invokes the real `NpcKnowledgeTransferLifecycle`, creates listener DIALOGUE/NPC_TOLD evidence and listener `BELIEF/NPC_TOLD`, preserves the source FACT unchanged, and exact same-cycle replay cannot create another target/evidence.

### 3. Same-cycle cascade prevention

The lifecycle replay contract exposed an authority/boundedness requirement before runtime integration: a claim admitted during a settlement cycle must not become a new speaker source when the exact cycle is evaluated again.

Production correction:

```text
commit: 0d7ab010b6b472737645417993e8f045db3494e9
```

The selector now accepts only retained sources whose authoritative `gameTime` is strictly earlier than the current 1200-tick cycle start. This prevents intra-cycle rumor cascades without adding a scheduler ledger or persistence field.

### 4. Runtime adapter boundary

RED — tests only:

```text
commit: 4336d9332219ffc1e1ca5aa5bcb49eca15585bbb
VillAIgence CI #2463
run: 31380844904
result: FAILURE as intended
```

Observed: exactly 2 compile errors for the absent `SettlementKnowledgeFlowRuntime` API.

GREEN implementation:

```text
commit: b4ac1b16410ec1135723bbef1588395c6e64ab35
```

The public adapter is strict no-op when disabled, passes the existing Memory 2.0 capacity to the domain lifecycle and catches runtime failures so the MCA village tick cannot be corrupted by auxiliary knowledge propagation.

### 5. Preservation investigation and fixture corrections

On the runtime-adapter head:

```text
VillAIgence CI #2465
run: 31381087820
699 tests / 2 behavioral failures
```

The preservation suite already compiled. It found:
- one later-cycle fan-out discrepancy;
- one contradiction fixture that initially used a direct `PLAYER_TOLD` BELIEF without the required persisted source DIALOGUE.

The latter was an invalid fixture assumption, not a product failure. It was corrected to a valid server-observed FACT source.

After that fixture correction/diagnostic pass:

```text
VillAIgence CI #2469
run: 31381458364
result: 2 remaining behavioral failures
```

Further isolation showed that contradiction relation construction also requires exact canonical Semantic scope equality. The final fixture therefore uses the same `[listenerNpc, player]` scope as the listener's server-observed FACT rather than weakening contradiction policy.

### 6. Multi-carrier fan-out RED and production correction

A first single-cycle duplicate-carrier fixture accidentally selected only one effective carrier and was therefore already green. The test was strengthened to scan deterministic cycles and assert the actual contract: equivalent normalized statement + exact scope carried by multiple NPCs must never produce more than one opportunity in one settlement cycle.

Robust RED — tests only:

```text
commit: bc36d1a8f4f9d4ddb1c4cdbfea7b2559d121d389
VillAIgence CI #2477
run: 31382255372
700 tests / exactly 3 failures
```

One failure was the new carrier fan-out assertion, proving a real production defect: two carriers of the same scoped knowledge could each allocate an opportunity in one cycle. The existing later-cycle test failed for the same reason.

Production correction:

```text
commit: 36ee459e3efbf1eab79a84fd89d2f73f722909ff
```

The selector now allocates a per-cycle knowledge key defined as:

```text
canonical normalized statement
+ exact canonical relatedEntities scope
```

The first deterministic carrier consumes that key before target-equivalence evaluation. A second carrier cannot become a fallback and cannot consume a second fan-out opportunity for the same scoped knowledge.

Verification on that production correction:

```text
VillAIgence CI #2479
run: 31382550847
fan-out test: PASS
later-cycle progression test: PASS
one remaining contradiction fixture-scope failure
```

The remaining failure was resolved by the exact-scope fixture correction described above; no contradiction production code was changed.

### 7. Full domain/preservation GREEN

Final domain/runtime-adapter head before Minecraft mixin wiring:

```text
commit: b662f65ba5f1d0c2fa30a5508b578532ad175a37
VillAIgence CI #2481
run: 31382862171
common + deterministic mock-provider tests: SUCCESS
```

The suite covers:
- deterministic 16/4/2/4 bounds;
- one fan-out per normalized claim+scope per cycle across multiple carriers;
- no fallback retargeting;
- no same-cycle cascade;
- supplied home-village membership isolation;
- player-private scope preservation and foreign-player retrieval exclusion;
- exact v2 provenance extension;
- existing `OMIT_TRAILING_SENTENCE` snapshot propagation without transformation-budget reset;
- automatic truth-neutral contradiction production after settlement delivery;
- fresh-root same-cycle replay without fan-out expansion;
- later-cycle gradual progression to another target;
- 12 settlements × 24 residents with hundreds of retained Semantic claims while each settlement remains at constant 16/4/4 allocation bounds.

### 8. Minecraft runtime integration

Runtime wiring uses a minimal `MixinVillage` rather than rewriting the large upstream `Village.java`. The mixin injects at the existing `VillageGuardsManager.spawnGuards(ServerLevel)` call inside `Village.tick`. That invocation is already behind MCA's staggered 1200-tick village update, move-in cooldown and loaded-chunk checks.

The mixin passes:
- existing server-owned `Village.getId()`;
- `Village.getResidentsUUIDs()` snapshot;
- the original authoritative `gameTime` argument (not the local `time + villageId` scheduling offset);
- the canonical server world root;
- existing `memory2MaxEventsPerNpc` capacity.

Mixin commits:

```text
35b76f83c9d97ae28e0d2908c9f12815a6bd0ba4  add MixinVillage
00eb3a277ad4d113c0a577c5885516d7706197d9  register MixinVillage
```

Verification:

```text
VillAIgence CI #2485
run: 31383129185
common/mock-provider: SUCCESS
risk-selected GameTests + Fabric/NeoForge loader builds: SUCCESS
production acceptance contracts: SUCCESS
production server acceptance/startup: SUCCESS
package verification: SUCCESS
```

The new injection therefore resolves on supported loaders and boots in the production acceptance runtime.

## Preservation / authority result

No new:

- provider request, response field or call;
- public configuration field/version;
- world file or persistence schema/version;
- migration/backfill;
- settlement-global/shared knowledge store;
- relationship/trust weighting;
- cross-village dissemination;
- direct listener Semantic write path;
- FACT promotion or confidence/provenance mutation;
- release identity.

The existing source-backed transfer lifecycle, v2 provenance, transformation budget, player eligibility and bounded contradiction producer remain the only mutation/authority paths.

Final exact-head delivery workflow IDs belong in PR #147 after the head is frozen so recording them does not invalidate the verified SHA.
