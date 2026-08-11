# Causal NPC Social Mutation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an exactly-once, source-evidence-backed, server-authoritative lifecycle for bounded directed NPC→NPC social mutations without introducing provider authority or prompt behavior yet.

**Architecture:** Extend `npc-social-graph.json` v1 additively with one latest causal frontier per source NPC, so graph state and replay guard save atomically. Add structured `NPC_SOCIAL_CHANGE` Memory 2.0 audit evidence after successful graph mutation, but keep Memory 2.0 out of the transactional dedupe path and exclude this event from generic prompt context. A small NPC identity authority validates live MCA villagers before lifecycle mutation.

**Tech Stack:** Java 21, Minecraft 1.21.1, Fabric/NeoForge shared common module, Gson records, existing `JsonStoreRecovery`, JUnit 5, Fabric GameTest/production acceptance, GitHub Actions.

## Global Constraints

- Existing MCA `VillagerBrain.PERSONALITY` / `Personality` remains the canonical persistent personality source.
- `relationships.json` remains NPC×player only; `npc-social-graph.json` remains NPC×NPC only.
- `npc-social-graph.json` stays format v1 with additive default-empty causal frontier state.
- `memory2.json` stays format v1 with additive nullable NPC-social audit payload.
- Mutation ID is deterministic from source NPC + exact cause event ID; one source event authorizes at most one social mutation.
- Eligible first-slice cause events are exact retained `SYSTEM_OBSERVED` `OBSERVATION` or `ACTION` events owned by source NPC and containing target NPC.
- Exact replay never applies a second delta.
- Valid `NO_CHANGE` and `CAPACITY_REACHED` consume the causal frontier.
- No provider schema/call, public config, Semantic authority mutation, settlement routing change or release publication.
- New `NPC_SOCIAL_CHANGE` audit events are excluded from generic Memory 2.0 prompt context.
- Runtime behavior is implemented only after observed tests-only RED.

---

## File structure

**New relationship-domain files**
- `common/src/main/java/net/conczin/mca/livingworld/relationship/NpcSocialMutationIdentity.java` — deterministic mutation UUID.
- `common/src/main/java/net/conczin/mca/livingworld/relationship/NpcSocialMutationCursor.java` — bounded latest source frontier payload.
- `common/src/main/java/net/conczin/mca/livingworld/relationship/NpcSocialCausalMutation.java` — exact store-level result/status.

**Modified relationship persistence**
- `common/src/main/java/net/conczin/mca/livingworld/relationship/NpcSocialGraphStore.java` — additive frontier map, atomic causal mutation API, frontier sanitation/readback.

**New lifecycle/audit files**
- `common/src/main/java/net/conczin/mca/livingworld/memory2/NpcIdentityAuthority.java` — injected identity boundary.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/ServerLevelNpcIdentityAuthority.java` — live `ServerLevel` → `VillagerEntityMCA` validation.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/NpcSocialMutationEvidence.java` — structured Memory 2.0 process payload.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/NpcSocialMutationMemoryAdapter.java` — graph result → audit event.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/NpcSocialMutationLifecycle.java` — exact source-event + identity validation and orchestration.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/NpcSocialMutationLifecycleResult.java` — lifecycle outcome with optional retained audit.

**Modified Memory 2.0**
- `common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryEvent.java` — `NPC_SOCIAL_CHANGE` type + additive nullable structured payload while preserving old constructor signatures.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryEventRetentionPolicy.java` — social change retention contribution equals `RELATIONSHIP_CHANGE`.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/Memory2ContextProvider.java` — exclude `NPC_SOCIAL_CHANGE` from generic prompt candidates.

**Production/GameTest evidence**
- extend `fabric/src/productionAcceptanceFixture/java/net/conczin/mca/acceptancefixture/ProductionAcceptanceNpcSocialGraph.java` to prove causal frontier replay across restart.
- add or extend a server GameTest to prove `ServerLevelNpcIdentityAuthority` accepts MCA villagers and rejects non-NPC/player identities.

**Governance**
- root `CHANGELOG.md` `[Unreleased]`.
- `docs/superpowers/evidence/2026-08-11-causal-npc-social-mutation-tdd.md`.

---

### Task 1: Atomic graph causal frontier

**Interfaces**

Produces:

```java
UUID NpcSocialMutationIdentity.forCause(UUID sourceNpcId, UUID causeEventId)

NpcSocialCausalMutation NpcSocialGraphStore.applyCausalDelta(
    UUID sourceNpcId,
    UUID targetNpcId,
    UUID causeEventId,
    long causeGameTime,
    NpcSocialDelta proposed,
    int maxDeltaPerMutation
)

Optional<NpcSocialMutationCursor> NpcSocialGraphStore.latestCausalMutation(UUID sourceNpcId)
```

`NpcSocialCausalMutation.Status`:

```text
APPLIED
NO_CHANGE
CAPACITY_REACHED
REPLAYED
STALE_CAUSE
CONFLICTING_CAUSE
INVALID_PAIR
FRONTIER_CORRUPT
```

- [ ] Write tests-only `NpcSocialMutationIdentityTest` and `NpcSocialGraphCausalMutationTest` proving deterministic ID, exact replay, conflicting payload, stale ordering, applied delta, terminal no-change/capacity frontier consumption and fresh-root reload.
- [ ] Push tests-only commit and run `VillAIgence CI`; expected RED is compile failure only for absent causal identity/cursor/result/store API.
- [ ] Add `NpcSocialMutationIdentity`, `NpcSocialMutationCursor`, `NpcSocialCausalMutation` and minimal store causal API.
- [ ] Add additive `causalFrontiers` GraphFile field; old v1 null field normalizes to empty.
- [ ] Save edge + cursor in one `save()` call for every terminal new cause.
- [ ] Run common suite to GREEN and commit.

### Task 2: Frontier load sanitation / fail-closed corruption

**Interfaces**

`latestCausalMutation(source)` returns empty for no history and must not expose malformed state.

- [ ] Add tests-only hostile-load cases: uppercase duplicate source keys, source/key mismatch, self target, null cursor fields, old-v1-no-frontier compatibility.
- [ ] Require attributable malformed/duplicate frontier state to make causal mutation return `FRONTIER_CORRUPT` for that source while unrelated source frontiers remain usable.
- [ ] Observe behavioral RED.
- [ ] Implement deterministic frontier sanitation plus transient blocked-source set; ordinary edge sanitation remains unchanged.
- [ ] Run common suite GREEN and commit.

### Task 3: Structured Memory 2.0 audit type without prompt leakage

**Interfaces**

Add to `MemoryEvent`:

```java
NpcSocialMutationEvidence npcSocialMutation
```

Add type:

```java
NPC_SOCIAL_CHANGE
```

Evidence record fields:

```java
UUID mutationId
UUID targetNpcId
UUID causeEventId
long causeGameTime
NpcSocialDelta boundedRequestedDelta
NpcSocialDelta appliedDelta
NpcSocialState before
NpcSocialState after
```

- [ ] Write tests-only coverage for payload validation/clamping, historical constructor source compatibility, retention contribution and generic prompt exclusion.
- [ ] Observe compile/behavioral RED.
- [ ] Add additive MemoryEvent field and old canonical-shape compatibility constructor.
- [ ] Add `NPC_SOCIAL_CHANGE` to type enum and retention policy at same type contribution as `RELATIONSHIP_CHANGE`.
- [ ] Exclude `NPC_SOCIAL_CHANGE` alongside `SEMANTIC_CONTRADICTION` in `Memory2ContextProvider` before candidate allocation.
- [ ] Run common suite GREEN and commit.

### Task 4: Exact audit adapter

**Interfaces**

```java
Optional<MemoryEvent> NpcSocialMutationMemoryAdapter.toMemoryEvent(
    UUID sourceNpcId,
    NpcSocialCausalMutation mutation,
    long createdAtEpochMillis
)
```

Only original `APPLIED` outcomes produce audit events. Event ID equals mutation ID; owner is source; participants are source + target; provenance is `SYSTEM_OBSERVED`; no Semantic admission occurs.

- [ ] Write tests-only adapter contract including exact structured payload, server-authored summary, replay/non-applied rejection and deterministic ID equality.
- [ ] Observe RED.
- [ ] Implement minimal adapter.
- [ ] Run common suite GREEN and commit.

### Task 5: Source-event and NPC identity authority lifecycle

**Interfaces**

```java
@FunctionalInterface
interface NpcIdentityAuthority {
    boolean isNpc(UUID id);
}

final class ServerLevelNpcIdentityAuthority implements NpcIdentityAuthority {
    ServerLevelNpcIdentityAuthority(ServerLevel level);
}

NpcSocialMutationLifecycleResult NpcSocialMutationLifecycle.apply(
    boolean enabled,
    Path worldRoot,
    UUID sourceNpcId,
    UUID targetNpcId,
    UUID causeEventId,
    NpcSocialDelta proposed,
    int maxDeltaPerMutation,
    int maxEventsPerNpc,
    long createdAtEpochMillis,
    NpcIdentityAuthority identities
)
```

Lifecycle statuses:

```text
APPLIED
APPLIED_AUDIT_NOT_RETAINED
NO_CHANGE
CAPACITY_REACHED
REPLAYED
STALE_CAUSE
CONFLICTING_CAUSE
SOURCE_NOT_RETAINED
INVALID_SOURCE_EVENT
INVALID_NPC
INVALID_REQUEST
FRONTIER_CORRUPT
```

- [ ] Write tests-only lifecycle cases using a deterministic identity authority.
- [ ] Prove source lookup is exact via `MemoryEventStore.findById`: correct owner, `SYSTEM_OBSERVED`, type `OBSERVATION|ACTION`, target participant required.
- [ ] Prove DIALOGUE, PLAYER_TOLD/NPC_TOLD/INFERRED, foreign owner, missing target, non-NPC/self and null request reject before graph mutation.
- [ ] Prove APPLIED appends structured audit, rereads it by ID and reports `APPLIED_AUDIT_NOT_RETAINED` if bounded Memory retention rejects it.
- [ ] Observe intended RED.
- [ ] Implement lifecycle and server identity authority using `ServerLevel.getEntity(uuid) instanceof VillagerEntityMCA`.
- [ ] Run common suite GREEN and commit.

### Task 6: Replay, forgetting and compatibility preservation

- [ ] Add tests proving exact lifecycle replay while cause retained returns `REPLAYED` and leaves graph bytes/state unchanged.
- [ ] Add tests proving source-event forgetting returns `SOURCE_NOT_RETAINED`, does not roll graph state back and cannot apply the old cause again through the causal graph API.
- [ ] Add tests proving forgotten audit is not resurrected by replay.
- [ ] Add tests proving NPC×player `relationships.json`, `semantic-memory.json` and unrelated Memory 2.0 events remain unchanged by social mutation except the explicit audit append.
- [ ] Add deterministic multi-source/multi-target pressure with ordered/stale causes and fresh-root reload.
- [ ] If a production defect appears, record RED and implement the smallest correction; otherwise make no production change.
- [ ] Run common suite GREEN and commit preservation evidence.

### Task 7: Real server identity and production restart evidence

- [ ] Add/extend a Fabric GameTest that spawns or uses two `VillagerEntityMCA` entities and proves `ServerLevelNpcIdentityAuthority` accepts them while rejecting a non-MCA/player UUID.
- [ ] Extend `ProductionAcceptanceNpcSocialGraph` so first startup records one low-level causal mutation frontier and second startup replays the exact cause, requiring `REPLAYED` and exact unchanged social state.
- [ ] Keep production fixture free of provider calls and preserve six-store recovery count.
- [ ] Run selected GameTests/loaders + production startup/restart + six-store recovery GREEN.

### Task 8: Changelog, TDD ledger and review

- [ ] Update root `CHANGELOG.md` `[Unreleased]` with causal social mutation semantics, additive v1 schema rationale and no-prompt/no-provider boundary.
- [ ] Add `docs/superpowers/evidence/2026-08-11-causal-npc-social-mutation-tdd.md` with every RED/GREEN commit/run and fixture correction.
- [ ] Base→head review for P0/P1/P2, boundedness, cross-store crash semantics, prompt leakage and scope drift.
- [ ] Resolve any blocker through a new tests-only RED when behavior changes.
- [ ] Freeze exact head.

### Task 9: Exact-head delivery and merge

- [ ] Require exact frozen-head Repository security policy SUCCESS.
- [ ] Require exact frozen-head VillAIgence CI SUCCESS, including common/mock-provider, GameTests/loaders, production acceptance, selected six-store recovery and package verification.
- [ ] Require exact frozen-head Production Soak SUCCESS.
- [ ] Require exact frozen-head GitHub Release dry-run SUCCESS with publication SKIPPED.
- [ ] Confirm zero unresolved review threads/actionable discussion comments.
- [ ] Mark PR ready and squash-merge with expected head SHA.
- [ ] Follow with docs-only `PROJECT_STATE.md` / `ROADMAP.md` reconciliation.

## Manual/local acceptance policy

Do not prepare an installed/local release solely for this causal infrastructure if all exact-head automation is green. The slice is intentionally non-observable to ordinary gameplay. Prepare a local test version when the following bounded personality/social snapshot is actually wired into NPC dialogue/behavior, because that is the first point where human-observable social differentiation can be accepted meaningfully.