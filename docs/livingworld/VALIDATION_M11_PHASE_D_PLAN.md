# M11 Phase D — Concurrency and client acceptance plan

## Purpose

Automate the remaining Operator Lore concurrency and client-state risks after M11 Phases A-C, without weakening the existing S10b server-authority or S10c client-editor contracts.

Phase D is not a visual redesign. It is a deterministic acceptance package for stale revisions, logical multi-client behavior, request ownership and editor state transitions.

## Baseline

```text
canonical base: 66f5372f10914bcd9daec894962b0383084726fa
S10b server-authoritative API: complete
S10c client editor: complete
M11 Phase A: complete
M11 Phase B: complete
M11 Phase C: complete
```

Relevant existing guarantees:

- read and write require permission level 2;
- PLAYER is bound to the authenticated sender;
- VILLAGER and VILLAGE are resolved from a live nearby same-level MCA villager;
- C2S contains no arbitrary UUID, dimension or village ID;
- writes use SHA-256 optimistic revisions;
- stale revisions return `CONFLICT` with current canonical state;
- exact replay returns `UNCHANGED`;
- client responses are correlated by request generation;
- the client has no file or persistence ownership.

## Primary acceptance scenario

Two logical clients operate on one server-owned Operator Lore scope.

```text
1. client A reads canonical value V0 and revision R0
2. client B reads canonical value V0 and revision R0
3. client A writes V1 with R0
4. server returns OK and revision R1
5. client B writes V2 with stale R0
6. server returns CONFLICT with canonical V1 and R1
7. store remains V1/R1
8. client B reloads or accepts the conflict state
9. client B writes V2 with R1
10. server returns OK and revision R2
11. store becomes V2/R2 exactly once
```

Required oracle:

- the first write mutates the store once;
- the stale write performs no mutation;
- the conflict response contains the current canonical value and revision;
- the retry with the current revision mutates the store once;
- no silent overwrite, duplicate write or lost update occurs;
- persistence remains valid and deterministic.

## Test architecture

Prefer the smallest deterministic seam that exercises production authority code.

Recommended layers:

```text
common integration test
  logical clients + production Operator Lore service/store

client model/controller test
  request generation + editor state transitions

source/package policy
  no client authority fields or direct store access

optional client GameTest/headless client
  only if the existing Fabric test runtime can execute it deterministically
```

Do not introduce a large UI framework or a special production-only abstraction solely for testing. Extract a narrow package-private/public test seam only where production ownership is already clear.

## RED boundaries

At least one canonical RED must prove each missing behavior rather than failing from setup noise.

### RED A — logical stale writer

Expected initial failure:

```text
missing production-level logical two-client orchestration harness
or
conflict response/store mutation assertion not satisfied
```

The RED must reach the actual revision-protected write path.

### RED B — stale client response

Expected initial failure:

```text
response from an old request generation changes the active editor state
```

The test must model a real scope/target or request-generation transition.

### RED C — modal/in-flight save ownership

Expected initial failure:

```text
valid Save response is lost or applied to the wrong state while a confirmation modal is open
```

If existing tests already prove this contract strongly, record the evidence instead of creating a duplicate RED.

## Required deterministic scenarios

### D1 — two logical clients, one stale revision

Layer: common integration.

Assertions:

- both clients receive R0;
- A receives `OK` with R1;
- B receives `CONFLICT` with V1/R1;
- store remains V1/R1;
- B can later save with R1;
- final revision differs from R1;
- write count is exactly two successful mutations.

### D2 — exact replay

Layer: common integration.

Assertions:

- repeat of the same canonical value with the current revision returns `UNCHANGED`;
- file/store is not rewritten;
- revision remains stable.

### D3 — permission isolation

Layer: common integration.

Assertions:

- authorized logical client can read/write;
- unauthorized client receives `FORBIDDEN`;
- unauthorized operation does not disclose arbitrary scope identity or mutate state.

### D4 — target isolation

Layer: common integration.

Assertions:

- target is resolved by the server from live state;
- missing/out-of-range target returns `NOT_FOUND` or the existing canonical status;
- arbitrary UUID/dimension/village ID cannot be supplied.

### D5 — request-generation correlation

Layer: client state/model.

Assertions:

- response generation N applies only while generation N is active;
- after scope/target switch to N+1, response N is ignored;
- ignored response cannot clear dirty state or replace canonical text.

### D6 — conflict editor state

Layer: client state/model.

Assertions:

- `CONFLICT` preserves the user's draft for review;
- canonical server value/revision is available;
- no automatic overwrite is issued;
- Reload and review paths are explicit.

### D7 — in-flight Save and modal

Layer: client state/model.

Assertions:

- valid Save response is retained while a modal is open;
- modal close applies or exposes the correct result according to the existing S10c contract;
- response cannot be consumed by an unrelated screen/global mailbox.

### D8 — Clear uses the same revision path

Layer: common integration + client model.

Assertions:

- Clear sends the current revision;
- stale Clear receives `CONFLICT`;
- current Clear succeeds;
- no bypass or unconditional delete exists.

## Source and package policies

Add or retain policy checks that reject:

- client file access to `operator-lore.json`;
- client-side permission authority;
- C2S player/villager UUID fields;
- C2S dimension or village ID fields;
- unconditional overwrite/retry after `CONFLICT`;
- unbounded text or packet payloads;
- a global response mailbox not owned by the active editor/request generation;
- Phase D fixture classes/resources in the distributable JAR.

## CI placement

Recommended order:

```text
repository security policy
→ fail-fast common/client state tests
→ production acceptance contract tests
→ two-JVM exact candidate startup/restart
→ risk catalog validation
→ Fabric server/client tests where applicable
→ Fabric build
→ NeoForge build
→ distributable package verification
```

No public AI provider, real API key, physical microphone or real multiplayer infrastructure is required for merge-blocking Phase D tests.

## Manual boundary

Phase D automation does not by itself claim:

- visual pixel/layout quality on every resolution;
- real mouse/keyboard behavior on every operating system;
- two physical Minecraft clients connected over a real network;
- Simple Voice Chat UDP/Opus multi-client behavior;
- compatibility with a specific operator modpack.

Retain one short installed two-client canary:

1. open the same scope on two clients;
2. save on client A;
3. save stale content on client B;
4. require explicit conflict;
5. reload/review on B;
6. save again with current revision;
7. reopen and verify the canonical final value.

## Documentation updates on completion

Update:

- `docs/PROJECT_STATE.md`;
- `docs/ROADMAP.md`;
- `common/src/test/resources/acceptance/scenarios.tsv`;
- this file with exact RED/GREEN evidence;
- a focused implementation validation document if Phase D grows beyond this plan.

`VAI-CONCUR-003` moves from `MANUAL_CANARY` to an automated logical-client scenario only after the deterministic harness passes. A separate installed multi-client scenario must remain represented explicitly.

## Exit criteria

Phase D is complete only when:

- meaningful RED evidence exists;
- D1-D8 are implemented or an explicitly justified equivalent set covers the same invariants;
- stale write cannot mutate state;
- stale response cannot mutate active editor state;
- permissions and target resolution remain server-owned;
- no distribution leakage occurs;
- all required exact-head checks pass;
- no unresolved P0/P1/P2 review finding remains;
- canonical documentation is synchronized.

## Out of scope

- Memory 2.0 migration;
- BELIEF producers;
- NPC-to-NPC social graph;
- rumor propagation;
- provider/model changes;
- voice codec or spatial audio redesign;
- unrelated MCA gameplay synchronization;
- release publication before the acceptance package is reviewed and green.
