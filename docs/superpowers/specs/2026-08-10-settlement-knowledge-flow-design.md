# Settlement-Scale Information Flow Without Omniscience — Design

Date: 2026-08-10
Status: approved for implementation by project owner and canonical roadmap
Base: `1.21.1` at `455d2ea36a393b2521346107fa6351f0a89ee0cd`

## Problem

VillAIgence already has exact source-backed NPC-to-NPC Semantic transfer, bounded multi-hop provenance, fallibility, one bounded transformation primitive, contradiction representation/prompting, and automatic bounded contradiction production. The missing capability is a server-owned policy for deciding when retained knowledge may move through a settlement population without giving every resident a shared omniscient memory.

This slice must add autonomous settlement-scale dissemination while preserving local NPC knowledge, existing source provenance, player privacy, hard work bounds, deterministic replay, and the rule that the LLM is never authority.

## Options considered

### A. Village-cycle bounded transfer opportunities — selected

Use MCA's existing server-owned home-village membership (`Village` / `Residency`) as the settlement boundary. On the existing staggered village update cadence, deterministically select a small bounded resident window, a bounded speaker subset, one bounded source candidate per speaker, and one deterministic listener target per source. Delegate each selected opportunity to `NpcKnowledgeTransferLifecycle`.

Advantages:
- reuses existing MCA settlement state instead of inventing a second settlement model;
- reuses exact source-backed transfer, provenance, transformation and contradiction machinery;
- no provider call or shared settlement knowledge store;
- no per-NPC-per-tick scheduling;
- hard deterministic work bound independent of total settlement population;
- replay can be stable because one source/cycle maps to one listener with no fallback retargeting.

Trade-off: this first slice models settlement membership, not physical conversational proximity or social preference. Relationship/trust-aware routing is deliberately deferred to the following social-epistemology slice.

### B. Per-villager autonomous gossip tick — rejected

Running dissemination from every villager tick would duplicate coordination work, make global work harder to bound, and couple knowledge propagation to entity tick frequency. It also creates an easy path to O(N²) scans or per-NPC scheduling pressure.

### C. Settlement knowledge bus / shared village memory — rejected

A shared settlement knowledge store would make information distribution cheap but would violate the product's local-knowledge/no-omniscience architecture. It would also create a second admission authority beside existing NPC-local Semantic Memory.

## Architecture

```text
existing MCA Village staggered update cycle
+ server-owned home-village resident UUID set
+ retained speaker Semantic Memory
→ SettlementKnowledgeFlowSelector
→ bounded deterministic transfer opportunities
→ SettlementKnowledgeFlowLifecycle
→ NpcKnowledgeTransferLifecycle.transfer(...)
→ listener DIALOGUE/NPC_TOLD evidence
→ listener BELIEF/NPC_TOLD
→ existing provenance / transformation / contradiction producer
```

No settlement-global knowledge is persisted. Every listener must receive knowledge through an ordinary exact source-backed transfer.

## Settlement boundary

The only settlement identity in this slice is the existing MCA home village:

- `Village.getId()` is the server-owned settlement identity;
- `Village.getResidents()` is the server-owned resident membership snapshot;
- `Residency.getHomeVillage()` remains the existing ownership mechanism;
- cross-village residents are never supplied to the same selector invocation.

The selector accepts explicit `villageId` plus the resident UUID collection so the domain policy is testable without Minecraft world/entity mocks.

This slice does not add settlement alliances, travel gossip, physical-distance routing or cross-village transfer.

## Cadence

Reuse the existing `Village.isVillageUpdateTime(time)` cadence (1200 ticks with village-ID staggering). Dissemination is evaluated once from that village-cycle branch rather than from every NPC tick.

No new scheduler state or timer persistence is introduced.

## Hard bounded work policy

Internal policy constants, not public config:

```text
CYCLE_TICKS = 1200
MAX_RESIDENTS_PER_CYCLE = 16
MAX_SPEAKERS_PER_CYCLE = 4
MAX_SOURCE_CANDIDATES_PER_SPEAKER = 2
MAX_OPPORTUNITIES_PER_CYCLE = 4
MAX_FANOUT_PER_SOURCE_PER_CYCLE = 1
```

The policy first canonicalizes resident UUIDs (non-null, distinct, stable UUID order), rotates the resident window deterministically from `(villageId, cycleIndex)`, and materializes at most 16 residents. Only the first four rotated residents are considered speakers.

For each selected speaker:
- read at most two recent retained Semantic candidates;
- select exactly one source deterministically for the current cycle;
- derive exactly one listener target from the bounded resident window and `(villageId, cycleIndex, source logical identity)`;
- never fall back to a second listener in the same source/cycle when the selected target is ineligible or already knows an equivalent claim;
- stop after four opportunities globally.

This makes pair-selection work O(1) with respect to total settlement population after the bounded resident window is materialized and prevents cascade/all-pairs growth.

## Deterministic no-fallback target rule

A source/cycle maps to one target. If that target:
- is the speaker;
- is unavailable from the bounded resident window;
- already has the same normalized statement under the exact same canonical Semantic scope;
- otherwise fails exact transfer lifecycle validation;

then that source/cycle does not retarget to another listener.

This rule is important for replay safety. Re-running the same cycle after the first successful transfer cannot discover a second listener merely because the first target now knows the claim.

A later cycle changes the deterministic cycle index and may select another target, allowing bounded gradual spread without persisted scheduler bookkeeping.

## Source eligibility

The selector never invents a claim. It obtains exact retained speaker Semantic entry IDs from `SemanticMemoryStore`.

Candidate source rules:
- kind must be FACT or BELIEF;
- normalized statement must be non-empty;
- source remains speaker-owned;
- exact statement/scope/provenance authority is not rewritten by the selector.

The selector does not attempt to reimplement provenance validation. `NpcKnowledgeTransferLifecycle` remains the final authority for source retention, valid provenance, cycle/limit checks and listener admission.

## Listener equivalent suppression

Before allocating an opportunity, the selector checks the chosen listener's existing Semantic Memory for exact:
- normalized statement equality;
- canonical `relatedEntities` scope equality.

Kind/provenance are deliberately not required for this suppression: if the listener already retains the same scoped claim text through any allowed source, another settlement retelling is unnecessary.

This suppression is not truth arbitration and does not merge claims across different scopes.

## Privacy and scope

The selected transfer preserves the source's existing Semantic subject scope unchanged through the existing transfer lifecycle.

A player-private claim does not become public because two NPCs share a village. The listener may receive the same scoped BELIEF, but retrieval still applies existing current-player/NPC-global/shared eligibility. The settlement selector never adds player IDs, speaker IDs or listener IDs to `relatedEntities`.

Cross-village residents consume zero selector slots because each cycle receives only one server-owned village membership set.

## Transfer authority

`SettlementKnowledgeFlowLifecycle` may only call:

```text
NpcKnowledgeTransferLifecycle.transfer(...)
```

It must not directly append listener Semantic entries or synthesize transfer evidence.

Therefore existing guarantees remain authoritative:
- exact source reread;
- listener DIALOGUE/NPC_TOLD evidence;
- listener knowledge always `BELIEF/NPC_TOLD`;
- v2 origin/ancestry preservation;
- max eight provenance hops;
- one transformation budget carried forward unchanged;
- cycle/limit failures fail closed;
- `ControlledSemanticMemoryIngestor` automatically invokes the bounded contradiction producer after listener admission.

## Replay and restart

No new durable scheduler ledger is added.

Replay safety derives from:
1. deterministic village-cycle resident/speaker/source/target selection;
2. no-fallback target selection;
3. existing exact transfer evidence identity and replay-idempotent lifecycle;
4. listener equivalent suppression;
5. persistent listener Semantic Memory and transfer evidence surviving restart.

An exact repeated cycle does not fan the same source out to a new target. A later authoritative cycle may select another target, which is intended gradual propagation rather than replay duplication.

## Runtime integration

The runtime hook belongs in the existing `Village.tick(ServerLevel world, long time)` village-update branch. It is gated by existing `LivingWorldConfig.memory2Enabled` and uses existing `memory2MaxEventsPerNpc` as both current Memory 2.0 event and Semantic retention capacity, matching current controlled-transfer call sites.

The hook derives the world root from the server's existing world-storage root and passes:
- village id;
- `getResidents()` snapshot;
- authoritative Minecraft `gameTime`;
- existing Memory 2.0 capacity.

Any invalid/empty settlement state returns a no-op cycle result. Provider/network calls are not involved.

## Observability

The cycle returns an immutable diagnostics-only result containing:
- bounded resident count;
- speaker count considered;
- opportunity count;
- attempted transfer count;
- successful transfer count;
- transfer result statuses.

No new diagnostics persistence format is added.

## Compatibility / non-goals

No new:
- provider request, response field or call;
- public config field/version;
- world file or persistence schema/version;
- migration/backfill;
- settlement-global knowledge store;
- relationship/trust weighting;
- cross-village propagation;
- physical-distance/social-preference routing;
- autonomous NPC voice/dialogue spectacle;
- release identity.

Official installed `0.2.0+1.21.1` acceptance remains unchanged.

## TDD / acceptance requirements

1. RED — deterministic resident/speaker/source/target selector contract.
2. RED — resident/window and opportunity hard bounds.
3. RED — cross-settlement input isolation and no broadcast.
4. RED — exact statement/scope listener-equivalent suppression before opportunity allocation.
5. RED — same-cycle replay cannot retarget after first listener receives a claim.
6. RED — lifecycle delegates only to existing exact transfer path and listener remains BELIEF/NPC_TOLD.
7. RED — player-private scope remains unchanged and foreign-player retrieval remains excluded.
8. RED — v2 provenance, existing transformation snapshot and automatic contradiction behavior survive propagation.
9. RED — fresh-root restart preserves no-duplicate behavior.
10. Deterministic multi-settlement pressure simulation with hundreds of retained claims and residents while all hard bounds remain constant.
11. Exact-head security, main CI, production soak and release dry-run; publication remains skipped.

## Exit criterion

Information can propagate through an eligible MCA home-village population via explicit bounded source-backed transfers. Each cycle considers at most 16 residents, four speakers and four transfer opportunities; one source/cycle has one deterministic no-fallback target; replay/restart cannot silently expand fan-out; listener knowledge remains local BELIEF with exact existing provenance/scope semantics; and no settlement-global omniscient state or provider routing call exists.
