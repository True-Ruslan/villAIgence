# Settlement-Scale Information Flow Without Omniscience — Design

Date: 2026-08-10
Status: implemented in PR #147 pending final exact-head delivery gates
Base: `1.21.1` at `455d2ea36a393b2521346107fa6351f0a89ee0cd`

## Problem

VillAIgence already has exact source-backed NPC-to-NPC Semantic transfer, bounded multi-hop provenance, fallibility, bounded transformation, contradiction representation/prompting, and automatic bounded contradiction production. The missing capability is a server-owned policy for deciding when retained knowledge may move through a settlement population without giving every resident a shared omniscient memory.

## Selected architecture

Reuse MCA home-village membership as the only settlement boundary and reuse the existing staggered loaded-village update cadence. Do not create a second settlement model, per-NPC gossip tick, settlement-global knowledge store, provider routing call, or new scheduler ledger.

```text
existing MCA Village loaded update
+ home-village resident UUID snapshot
+ retained NPC-local Semantic Memory
→ bounded deterministic opportunity selector
→ exact NpcKnowledgeTransferLifecycle.transfer(...)
→ listener DIALOGUE / NPC_TOLD evidence
→ listener BELIEF / NPC_TOLD
→ existing v2 provenance / transformation / contradiction machinery
```

Every listener learns through the existing exact source-backed transfer lifecycle. Settlement membership is routing context, never knowledge authority.

## Runtime integration

`MixinVillage` injects immediately before the existing `VillageGuardsManager.spawnGuards(ServerLevel)` invocation inside `Village.tick(...)`.

That call is already behind MCA's:
- village-ID-staggered 1200-tick update cadence;
- move-in cooldown gate;
- loaded-village-chunk gate.

The mixin therefore does not duplicate or replace MCA scheduling. Because `Village.tick(...)` mutates its local `time` parameter with `time += villageId` for staggered scheduling, transfer evidence deliberately uses `world.getGameTime()` as the authoritative unshifted server clock rather than trusting the late-injection method argument.

Server-owned runtime inputs:
- `Village.getId()`;
- `Village.getResidentsUUIDs()` snapshot;
- `world.getGameTime()` authoritative game clock;
- `world.getServer().getWorldPath(LevelResource.ROOT)`;
- `LivingWorldConfig.memory2MaxEventsPerNpc`;
- existing `enabled && memory2Enabled` gate.

`SettlementKnowledgeFlowRuntime` catches auxiliary runtime failures so settlement dissemination cannot corrupt the village tick.

## Hard bounded policy

Internal implementation policy, not public configuration:

```text
CYCLE_TICKS = 1200
MAX_RESIDENTS_PER_CYCLE = 16
MAX_SPEAKERS_PER_CYCLE = 4
MAX_SOURCE_CANDIDATES_PER_SPEAKER = 2
MAX_OPPORTUNITIES_PER_CYCLE = 4
MAX_FANOUT_PER_SOURCE_PER_CYCLE = 1
```

### Resident window

Resident UUID input is server-owned MCA village membership. Nulls are removed, IDs are deduplicated and UUID-sorted, then a deterministic `(villageId, cycleIndex)` rotation chooses at most 16 residents. Only the first four rotated residents are considered speakers.

Total membership canonicalization may inspect the village's resident set, but pair/source/target work is hard bounded and there is no resident×resident all-pairs scan.

### Source candidates

Each selected speaker reads at most two newest eligible retained Semantic entries and deterministically selects one.

A source is eligible only when:
- entry/kind/provenance are non-null;
- normalized statement is non-empty;
- `entry.gameTime() < cycleStart`.

The strict cycle-start rule is an anti-cascade boundary: a rumor received in the current 1200-tick settlement cycle cannot immediately become another settlement-flow source until a later cycle.

### Knowledge-level fan-out

A settlement-cycle knowledge key is:

```text
canonical normalized statement
+ exact canonical relatedEntities scope
```

The first deterministic carrier of that key consumes it before target-equivalence evaluation. Another carrier of equivalent scoped knowledge cannot become a fallback and cannot allocate a second opportunity in the same cycle.

This enforces `MAX_FANOUT_PER_SOURCE_PER_CYCLE = 1` at the semantic knowledge level rather than merely per carrier entry UUID.

### Deterministic listener / no fallback

A selected source maps to one listener from the bounded resident window using `(villageId, cycleIndex, source logical claim identity)`.

If the chosen listener:
- is unavailable;
- already retains exact normalized statement + exact canonical scope;
- or the downstream transfer lifecycle rejects the transfer;

then that knowledge does not retarget to another listener in the same cycle.

A later authoritative cycle may select another listener, enabling gradual bounded spread without a new durable routing ledger.

## Transfer authority

`SettlementKnowledgeFlowLifecycle` is orchestration only. It may mutate knowledge solely through:

```text
NpcKnowledgeTransferLifecycle.transfer(...)
```

It does not append listener Semantic Memory directly.

Existing downstream guarantees therefore remain authoritative:
- exact source reread;
- listener DIALOGUE/NPC_TOLD process evidence;
- listener always receives `BELIEF/NPC_TOLD`;
- FACT is never copied as FACT authority;
- v2 origin/ancestry is retained and extended;
- provenance remains acyclic and capped at eight hops;
- an existing transformation snapshot propagates unchanged and the one-transform budget is not reset;
- player-private/shared/global `relatedEntities` scope is preserved exactly;
- `ControlledSemanticMemoryIngestor` invokes the existing bounded contradiction producer after listener admission;
- current server-observed FACT remains authoritative.

## Replay / restart semantics

Same-cycle replay cannot silently fan the claim out farther because:
1. resident/speaker/source/target selection is deterministic;
2. the selected target has no fallback;
3. the existing transfer evidence identity is replay-idempotent;
4. listener equivalence suppression sees already retained knowledge;
5. current-cycle received claims are ineligible as sources until the next cycle;
6. equivalent carriers share one cycle knowledge key.

Fresh-root tests copying `memory2.json` and `semantic-memory.json` confirm same-cycle replay does not expand fan-out. A later cycle may progress the same knowledge to another deterministic target.

## Privacy / no omniscience

There is no settlement-global knowledge record. Each resident has only its own Semantic Memory.

Home-village membership does not broaden claim subject scope. Player-private claims remain player-private under existing retrieval eligibility even after another NPC receives the same scoped BELIEF.

Cross-village residents are never supplied to the same cycle selector. Cross-village dissemination, travel gossip, alliances and social-topology routing are outside this slice.

## Failure behavior

Invalid/null/undersized settlement input is a no-op. Individual transfer failures are diagnostics-only statuses and never trigger fallback routing or rollback of unrelated valid transfers. Runtime adapter failures fail soft and cannot abort the MCA village update.

## Persistence / provider compatibility

No new:
- provider request, response field or call;
- public config field/version;
- world file;
- `memory2.json` or `semantic-memory.json` schema/version;
- migration/backfill;
- durable scheduling ledger;
- settlement-global memory;
- release identity.

The existing Memory 2.0 stores and exact transfer evidence remain the sole persistence model.

## Deferred by design

Not included in this slice:
- relationship/trust weighting of beliefs or routing;
- cross-village dissemination;
- physical conversational proximity;
- autonomous generated NPC↔NPC dialogue/voice spectacle;
- provider-authored recipient selection;
- settlement-global summaries or shared truth state.

Relationship/trust social epistemology is the next separate product slice so social affinity cannot accidentally become truth authority.

## Verification contract

The implementation is accepted only with:
- observed test-only RED for selector, lifecycle and runtime adapter APIs;
- behavioral RED proving multi-carrier scoped knowledge could over-fan-out before the knowledge-key correction;
- deterministic private-scope, provenance, transformation and contradiction preservation;
- fresh-root same-cycle replay and later-cycle gradual-spread tests;
- 12 settlements × 24 residents with hundreds of retained claims while 16/4/4 bounds remain constant per settlement;
- Fabric/NeoForge loader builds, GameTests and production startup with `MixinVillage`;
- exact-head repository security, main CI, Production Soak and release dry-run; publication remains skipped.

## Exit criterion

Information propagates through an MCA home-village population via explicit bounded source-backed transfers: at most 16 residents, four speakers and four opportunities are allocated per cycle; equivalent scoped knowledge has fan-out one per cycle across all carriers; no same-cycle cascade or fallback retargeting exists; replay/restart are deterministic; local BELIEF/provenance/privacy semantics remain intact; and no shared omniscient settlement knowledge state exists.
