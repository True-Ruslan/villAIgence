# M11 Phase E Automation Completion Implementation Plan

> **Status:** implementation and independent code review complete at the automation layer on 2026-08-06. PR #114 remains draft and unmerged. No release was requested or published by this plan.

**Goal:** Move every deterministic VillAIgence release risk into repeatable CI and reduce manual acceptance to physical microphone, audible/spatial perception, graphical-client review and exact installed-environment smoke.

**Architecture:** Common tests, source policy, Fabric GameTests, exact production-JAR acceptance, destructive persistence recovery, fail-closed risk selection and constrained-heap soak form one evidence chain. Release validation always selects every mandatory suite. The acceptance catalog remains the canonical risk-to-proof map.

**Tech stack:** Java 21, Gradle 9.6.1, Fabric 1.21.1, Fabric GameTest, JUnit 5, Python 3.12, GitHub Actions and Simple Voice Chat.

## Global constraints

- [x] No `0.1.26+1.21.1` request, tag or publication during Phase E.
- [x] No paid provider or repository-secret dependency in pull-request acceptance.
- [x] No weakening of deadlines, payload limits, redirect restrictions, authorization or exactly-once persistence.
- [x] No sleeps used as correctness oracles.
- [x] Destructive persistence fixtures use isolated generated worlds.
- [x] Every automated scenario has a bounded timeout and explicit terminal oracle.
- [x] Catalog states changed to `AUTOMATED` only after exact-head evidence.

---

## Task 1 — approved design and execution plan

- [x] Write the Phase E design.
- [x] Write the executable task plan.
- [x] Review scope, non-goals and evidence requirements.
- [x] Commit on isolated branch `agent/m11-phase-e-automation-completion`.

## Task 2 — E0 production staging hardening

- [x] Add source-policy coverage for execution-time Gradle Project access.
- [x] Capture immutable provider-backed staging inputs during configuration.
- [x] Remove execution-time `project.*` access from production staging.
- [x] Make production fixture staging configuration-cache safe.
- [x] Close subprocess file handles and remove ResourceWarnings.
- [x] Verify common tests, staging, configuration-cache reuse and production acceptance.

## Task 3 — E1 duplicate-identity prevention

- [x] Add `TombstoneIdentityReplayGameTests` RED boundary.
- [x] Reject replay when the stored UUID already has an authoritative live entity.
- [x] Preserve grave data when a conflicting replay is rejected.
- [x] Verify later non-conflicting restoration preserves UUID, name and inventory once.
- [x] Automate `VAI-LIFE-005`.

## Task 4 — E2 production lifecycle across two JVMs

- [x] Add lifecycle report contract tests.
- [x] Create real MCA villager death/tombstone/portable/resurrection evidence.
- [x] Verify UUID, name, inventory and one live entity after restart.
- [x] Include lifecycle evidence in production acceptance artifacts.
- [x] Automate `VAI-LIFE-002` at production-restart layer.

## Task 5 — E3 corrupt persistence recovery matrix

- [x] Cover truncated JSON, empty file, wrong root, incompatible schema and temp-file variants.
- [x] Preserve exact corrupt bytes in bounded backup paths.
- [x] Regenerate valid canonical JSON.
- [x] Prove five unaffected sibling hashes remain unchanged.
- [x] Prove idempotent second startup.
- [x] Add nightly, selected-main-CI and release-gate execution.
- [x] Automate `VAI-PERSIST-003`.

## Task 6 — E4/E5 authenticated text and Operator Lore transport

- [x] Add authenticated text-turn authority tests.
- [x] Add two authenticated Operator Lore network-session tests.
- [x] Prove player/NPC ownership and one provider/persistence/response effect.
- [x] Prove stale conflict, retained draft and explicit retry exactly once.
- [x] Prove unauthorized requests disclose and mutate nothing.
- [x] Prove response ownership and session isolation.
- [x] Automate `VAI-GAME-005` and `VAI-CONCUR-005`.

## Task 7 — E6 production voice transport

- [x] Add deterministic production codec/transport acceptance.
- [x] Use the real Simple Voice Chat Opus runtime.
- [x] Verify encode/decode, packet loss concealment, duplicate and ordering rejection.
- [x] Verify bounded PCM, cancellation and resource closure.
- [x] Repeat transport validation across production restarts.
- [x] Automate `VAI-AI-005` while retaining physical-device canary `VAI-AI-006`.

## Task 8 — E7/E8 gameplay and navigation matrix

- [x] Add special gift PASS/FAIL/CONSUME GameTests.
- [x] Add deterministic fishing inventory and rod-durability GameTest.
- [x] Add mounted archer stable-controller and NPC-owned projectile GameTest.
- [x] Add obstacle reroute with intact-wall terminal oracle.
- [x] Add ladder ascent/descent with observed production climb control.
- [x] Add real closed-door open/pass/close GameTest.
- [x] Retain water, dry-land and isolated navigation fixtures.
- [x] Run sixteen required server GameTests in normal and release CI.
- [x] Automate `VAI-GAME-002` and `VAI-NAV-004`.

## Task 9 — E9 risk selection, soak and documentation closure

- [x] Write RED selector API tests.
- [x] Implement deterministic path-to-risk classification.
- [x] Write RED CLI and workflow-policy tests.
- [x] Emit deterministic GitHub Actions outputs.
- [x] Collect authoritative PR/push changed paths.
- [x] Guard fast, server, production, recovery and package groups independently.
- [x] Fail closed for release, empty, unsafe, unknown, workflow, build and CI-script changes.
- [x] Require `all=true` in release validation.
- [x] Add selected recovery execution to normal CI.
- [x] Write RED bounded-soak contracts.
- [x] Add explicit Gradle fork-heap override.
- [x] Repeat authenticated text and Operator Lore scenarios three times with `--rerun-tasks` at 512 MiB.
- [x] Stage the exact remapped candidate at constrained workers/heap.
- [x] Run five production JVM cycles at 512 MiB.
- [x] Verify strict startup/stop/save/exit, one live NPC, voice PASS and six stable store hashes after every cycle.
- [x] Upload and inspect machine-readable soak evidence.
- [x] Make soak workflow/harness/test changes trigger release dry-run.
- [x] Run soak contracts inside release validation.
- [x] Reconcile `PROJECT_STATE.md`, `ROADMAP.md` and E9 validation evidence.
- [x] Complete independent review, fix the canonical-store selector P2 and prepare the final documentation-only head for workflow verification.

---

# Independent review result

Review scope covered the complete PR #114 diff, with focused inspection of CI/release/soak, persistence, server authority, tombstone lifecycle, voice, concurrency and package identity.

One P2 was found:

```text
canonical LivingWorld Store.java changes selected runtime suites
but did not directly select the destructive recovery suite in main CI
```

The release dry-run already protected these paths, so published-release safety was not bypassed. The main-CI fail-closed contract was nevertheless incomplete.

Resolution:

- [x] Add six current canonical store regression cases.
- [x] Add a future LivingWorld `*Store.java` regression case.
- [x] Observe seven focused RED failures.
- [x] Classify persistence infrastructure and every production LivingWorld `*Store.java` as recovery-sensitive.
- [x] Preserve the narrower runtime suite for ordinary voice classes.
- [x] Rerun selector, main recovery, release recovery and five-cycle soak.

Open findings after correction:

```text
P0: 0
P1: 0
P2: 0
P3: 0
```

---

# Exact implementation evidence

Runtime implementation head:

```text
78d7961632501b038d233dd662c62384d81a7c3b
```

Validated runs:

```text
VillAIgence CI                 1721 / 31083451312 — PASS
Java Pull Request CI           1107 / 31083451053 — PASS
Repository security policy    1347 / 31083451124 — PASS
Supply-chain verification     167  / 31083451252 — PASS
Production Soak               14   / 31083451193 — PASS
GitHub Release dry-run         333  / 31083451015 — PASS
release publication                                SKIPPED
```

Inspected soak evidence:

```text
production-soak-14
artifact id: 8960538615
digest: sha256:6567b4a8cad895945a204a8a64b64e7a9078ab989b6d6e7db3c242a56fbd1c83
5 clean JVM cycles at 512 MiB
one live NPC per cycle
voice PASS per cycle
six stable persistent-store hashes
```

Release dry-run evidence:

```text
production-server-acceptance-333  artifact 8960504586
persistence-recovery-333          artifact 8960600598
villaigence-fabric-package        artifact 8960636786
production-tested/package JAR     byte-identical
```

Canonical detailed evidence:

```text
docs/livingworld/VALIDATION_M11_PHASE_E_E7.md
docs/livingworld/VALIDATION_M11_PHASE_E_E8.md
docs/livingworld/VALIDATION_M11_PHASE_E_E9.md
```

Acceptance catalog after Phase E:

```text
34 total
28 AUTOMATED
6 MANUAL_CANARY
0 PLANNED
```

---

# Remaining delivery boundary outside Phase E automation

The following are deliberately not marked complete by this plan:

- [ ] exact candidate/released JAR startup in the operator environment;
- [ ] two ordinary MCA brains visibly escape water in the installed world;
- [ ] selected-NPC visible text response on an installed client;
- [ ] real-player Silk Touch grave pickup/placement/restart interaction;
- [ ] physical microphone, client UDP routing and audible spatial response;
- [ ] two graphical clients visibly review and resolve Operator Lore conflict;
- [ ] release-request merge, tag and publication;
- [ ] published-asset identity and post-release restart verification.

These are the six catalog `MANUAL_CANARY` scenarios plus release governance. They must remain small and must not repeat deterministic internals already covered by automation.
