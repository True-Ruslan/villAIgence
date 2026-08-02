# VillAIgence 0.1.20 Installed Acceptance

## Status

```text
date: 2026-08-02
release: 0.1.20+1.21.1
acceptance result: PARTIAL PASS — RELEASE DEFECTS FOUND
server startup gate: PASS
installed server/client smoke: PASS
S1-S10c cumulative acceptance: PARTIAL PASS
restart durability: PASS for six checked persistent files
rollback-world compatibility: PASS
release promotion as fully accepted checkpoint: BLOCKED
```

The exact `0.1.20+1.21.1` release starts and operates on the installed server, fixing the startup blockers found in `0.1.18` and `0.1.19`. The cumulative acceptance is not a full PASS because two gameplay defects, one packaging defect, one severe latency observation and one unverified concurrency scenario remain.

This document records operator-provided installed evidence. It does not convert unverified scenarios into PASS and does not infer results for files or behaviors that were not explicitly checked.

---

## Passed scenarios

### AI and voice pipeline

- Text dialogue passed.
- STT passed.
- Chat passed functionally.
- TTS passed.
- Simple Voice Chat integration passed.

One Chat request required approximately `272 seconds`. Functional completion therefore passed, but latency is not accepted as healthy production behavior and remains a separate reliability defect/risk.

### Operator Lore S9-S10c

- `/mca lore editor` opened successfully.
- `WORLD` lore passed.
- `PLAYER` lore passed.
- `VILLAGER` lore passed.
- `VILLAGE` lore passed.
- Save and reopen persistence passed.
- An NPC without a home village was handled successfully.

The true optimistic-concurrency conflict scenario with two simultaneously open editors remains unverified because a second player/client was unavailable. This must remain `UNVERIFIED`, not PASS.

### Navigation and inherited gameplay

- ordinary navigation passed;
- ladder/climb behavior passed;
- relationship gifts passed;
- fishing passed for rod use, durability damage, cod and salmon;
- mounted archer passed for mounting and destroying a target while mounted.

### Lifecycle and compatibility

- restart passed;
- a rollback world originating from `0.1.16+1.21.1` loaded successfully under `0.1.20+1.21.1` in isolation;
- the primary world and primary configuration were not replaced during the rollback-world test;
- the primary server remained operational;
- TCP `25565` remained active;
- UDP `24454` remained active.

---

## Persistent-data evidence

After restart, the following six checked files remained valid and retained identical pre/post-restart SHA-256 values:

```text
memory.json
memory2.json
operator-lore.json
relationships.json
semantic-memory.json
voices.json
```

Exact hash values were not supplied in this acceptance report, so this document records equality and validity but does not invent the hashes.

`events.json` was not included in the supplied six-file hash result. Its restart hash status is therefore not claimed here.

No corruption, replacement or rollback mutation of the primary world was reported.

---

# Blocking defects

## DEFECT-1 — Villager becomes trapped in water and drowns

```text
result: FAIL
area: S4/S5/S6 navigation and survival behavior
observed: NPC became trapped in water, failed to escape, drowned and died
```

Ordinary navigation and ladders passed, so the failure is water-specific rather than a general navigation failure.

### Required investigation

- water path-node classification and edge transitions;
- water-tag and entity-AABB collision rules;
- transitions from submerged nodes to adjacent walkable blocks;
- float/swim goal scheduling;
- move-control ownership while submerged;
- path progress watchdog behavior in water;
- bounded recovery when no complete dry-land path is immediately available.

### Required regression boundary

- deterministic reproduction or focused fixture;
- survival and escape from representative shallow and deep water layouts;
- no regression to ordinary navigation or ladders;
- Fabric and NeoForge builds;
- distributable Fabric package verification;
- installed-server retest.

This defect is release-blocking because it can kill persistent NPCs during normal world simulation.

---

## DEFECT-2 — Filled grave disappears when broken with Silk Touch

```text
result: FAIL
area: tombstone/grave item-drop integrity
observed: a filled grave broken with a Silk Touch pickaxe disappeared without dropping an item
```

### Required investigation

- filled versus empty grave drop policy;
- Silk Touch loot-table and block-drop path;
- preservation of tombstone/entity data when an item drop is allowed;
- interaction between direct tombstone logic, loot generation and inherited MCA behavior;
- prevention of silent data/item loss.

### Required regression boundary

- filled grave broken with Silk Touch cannot disappear without an explicit safe outcome;
- empty grave behavior remains correct;
- normal non-Silk-Touch break behavior remains correct;
- resurrection and mourning cleanup remain unaffected;
- exact item/data semantics are documented and tested;
- installed-server retest.

This defect is release-blocking because it causes destructive loss of a filled grave.

---

# Packaging and reliability defects

## DEFECT-3 — Runtime version reports `1.21.1-SNAPSHOT`

```text
expected: 0.1.20+1.21.1
observed: 1.21.1-SNAPSHOT
area: release metadata / packaging identity
```

The official asset name, tag and checksum identify `0.1.20+1.21.1`, but the installed mod reports the wrong runtime version. Release metadata must be derived from the exact release identity and verified inside the packaged JAR.

### Required regression boundary

- packaged metadata reports the exact release version;
- development builds may retain an explicit development identity, but release builds must not report `SNAPSHOT`;
- CI inspects the distributable JAR metadata and fails on mismatch;
- tag, asset filename, checksum manifest and embedded runtime version agree.

The existing `0.1.20` tag and release must not be overwritten. A corrected artifact requires a new version.

---

## DEFECT-4 — Chat request took approximately 272 seconds

```text
functional result: PASS
reliability result: DEFECT / UNACCEPTABLE LATENCY OBSERVATION
observed duration: approximately 272 seconds
```

The request eventually completed, so this is not recorded as a functional pipeline failure. It does show that current provider/retry/deadline behavior can leave a user waiting for several minutes.

### Required investigation

- per-attempt connect, request and response-body timing;
- retry count and provider fallback sequence;
- distinction between bounded response-body deadline and end-to-end interaction deadline;
- cancellation when the player disconnects or starts a newer request;
- user-visible progress/failure status;
- safe upper bound that does not duplicate memory, actions or relationship effects.

### Required regression boundary

- diagnostics identify where time was spent without leaking prompts or transcripts;
- end-to-end latency policy is explicit;
- timeout/cancellation remains fail-soft;
- retries remain idempotent;
- Text/STT/Chat/TTS pipeline remains operational.

---

# Unverified acceptance item

## Concurrent Operator Lore conflict

```text
status: UNVERIFIED
reason: second player/client unavailable
scenario: two editors load the same revision, one saves, the second attempts a stale save
expected: second save returns CONFLICT with canonical server state; no blind overwrite
```

Repository tests and automated S10b/S10c evidence remain valid, but installed multiplayer proof is still missing. The next acceptance cycle should use either two real clients or a deterministic integration harness that exercises two authenticated sessions.

---

# Release decision

`0.1.20+1.21.1` is suitable as evidence that the startup hotfixes work and that most synchronized functionality operates on the installed server. It is not a fully accepted synchronized release checkpoint.

```text
startup replacement for 0.1.18/0.1.19: PASS
functional smoke and most S1-S10c scenarios: PASS
full cumulative acceptance: PARTIAL PASS
release defects: PRESENT
new feature development gate: BLOCKED BY DEFECT TRIAGE
```

The correct next sequence is:

```text
1. reproduce and fix water trapping/drowning
2. reproduce and fix filled-grave Silk Touch loss
3. correct embedded runtime version metadata
4. add end-to-end Chat latency policy and diagnostics
5. verify true two-editor conflict
6. build a new exact release candidate under the next free version
7. repeat focused regressions plus cumulative restart/hash acceptance
8. update PROJECT_STATE.md, ROADMAP.md and CHANGELOG.md with final live evidence
9. only then resume additive memory.json migration and remaining Memory 2.0 work
```

Suggested current checkpoint marker:

```text
V0120_S1_S10C_PARTIAL_ACCEPTANCE_DEFECTS_FOUND
```

A full-PASS marker must not be used for `0.1.20+1.21.1`.
