# VillAIgence 0.3.0 clean-world installed acceptance

Status: **PENDING POST-PUBLICATION INSTALLED ACCEPTANCE**

Target release:

```text
0.3.0+1.21.1
```

This document is the canonical operator record for installed acceptance of the published VillAIgence 0.3.0 release.

Automated request-validation, production acceptance, recovery, GameTests, loader builds, soak and package verification are separate evidence. This file must never convert those automated results into installed/manual PASS.

---

## 1. Supported rollout boundary

The supported pre-1.0 rollout for this release is:

```text
private test server
+ Minecraft 1.21.1
+ clean LivingWorld state
+ exact official 0.3.0+1.21.1 GitHub Release JAR
```

Do not use the only copy of a valuable world. Keep an offline backup and retain the last installed-accepted `0.2.0+1.21.1` JAR for rollback.

The clean-state policy means no migration/backfill claim is being tested for unsupported experimental pre-release `memory.json` or NPC social-graph state.

---

## 2. Official release identity — fill after publication

Installed acceptance starts only after the immutable Git tag and GitHub Release exist.

Record the official identity before installing:

```text
release tag:                  0.3.0+1.21.1
release commit:               PENDING
GitHub Release workflow/run:  PENDING
release asset id:             PENDING
JAR filename:                 villaigence-fabric-0.3.0+1.21.1.jar
JAR SHA-256:                  PENDING
dependency manifest:          villaigence-dependencies-0.3.0+1.21.1.txt
embedded version:             0.3.0+1.21.1
embedded mod id:              mca
embedded name:                VillAIgence
```

The SHA-256 recorded here must be the checksum of the official release asset. The server and every graphical client used for acceptance must run those exact JAR bytes.

A PR dry-run artifact, local build or snapshot build is not a substitute for the official published release asset in this record.

---

## 3. Pre-test backup and clean-state preparation

Before installing 0.3.0:

1. stop the test server cleanly;
2. record the currently installed known-good release and its JAR SHA-256;
3. back up the full test world, `config/` and `mods/` offline;
4. preserve the old `<world>/livingworld/` directory separately if it exists;
5. prepare a clean LivingWorld state for the test world;
6. remove the previous VillAIgence/MCA JAR and install the exact official 0.3.0 JAR;
7. install the same exact JAR on every graphical client used by a manual canary;
8. keep original MCA Reborn removed; VillAIgence intentionally owns internal mod id `mca`;
9. retain a known-working provider/model configuration unless provider behavior is itself under test.

Current world-local state after normal use may include:

```text
memory2.json
semantic-memory.json
events.json
relationships.json
voices.json
operator-lore.json
npc-social-graph.json
```

Do not require all files to exist before the subsystem that owns them has produced state.

---

## 4. Automated evidence that is not repeated manually

The release pipeline already owns deterministic verification of:

- exact version/tag/embedded-metadata identity;
- repository security policy;
- common/unit and deterministic mock-provider tests;
- required Fabric server GameTests;
- Fabric and NeoForge builds;
- production Fabric startup, clean stop/save and same-world restart;
- six-store destructive auxiliary persistence recovery;
- replay/idempotency and bounded persistence contracts;
- Memory 2.0 provenance, contradiction, fallibility and transformation rules;
- directed NPC social persistence and causal exactly-once mutation contracts;
- bounded Personality/direct-pair snapshot and deterministic behavior-policy contracts;
- package smoke verification;
- production-accepted JAR == packaged JAR byte identity;
- constrained Production Soak and repeated restart cycles.

Do not manually rerun those internals unless an installed symptom specifically points at a release defect.

---

## 5. Canonical manual-canary boundary

The machine-readable 0.3 convergence contract defines exactly six unavoidable `MANUAL_CANARY` cases for the current acceptance catalog:

```text
VAI-PCM-E2E-001
VAI-PCM-MULTI-001
VAI-PROX-MULTI-001
VAI-SEC-001
VAI-RESET-001
VAI-STT-001
```

This repository currently fixes these six IDs as the canonical manual boundary but does not provide a separate current 0.3 document with more detailed case prose. Therefore this release record does not invent new semantics for the IDs.

For each case, execute the established installed/manual procedure used by the project and record privacy-safe evidence sufficient to support one of:

```text
PASS
FAIL
NOT TESTED
```

Do not infer PASS from CI, server logs alone, logical clients or mocked hardware when the case requires graphical, physical-device or subjective installed evidence.

### Results

```text
VAI-PCM-E2E-001    PENDING
VAI-PCM-MULTI-001  PENDING
VAI-PROX-MULTI-001 PENDING
VAI-SEC-001        PENDING
VAI-RESET-001      PENDING
VAI-STT-001        PENDING
```

For every executed case record:

```text
case id:
result:
server/client JAR SHA-256:
Minecraft/Fabric/Voice Chat versions:
provider/model when relevant:
privacy-safe observation:
relevant log marker or screenshot reference when useful:
```

Do not store API keys, Authorization headers, private prompts/transcripts, raw provider payloads or model reasoning in the evidence.

---

## 6. Explicit deferred installed evidence

These existing boundaries remain outside the six required manual canaries and must remain honest:

```text
VAI-M2-INST-005  NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004   NOT TESTED / DEFERRED
```

`VAI-M2-INST-005` must not become installed PASS without the missing real second-player installed isolation evidence.

`VAI-CONCUR-004` must not become PASS without two real graphical clients exercising the Operator Lore conflict presentation boundary.

Green automation does not change either status.

---

## 7. Release decision

Installed acceptance is complete only when:

1. the official release identity fields in section 2 are filled from the published immutable release;
2. the installed server/client JAR SHA-256 matches the official asset checksum;
3. every required manual canary has an explicit PASS/FAIL/NOT TESTED result supported by real installed evidence;
4. no required executed canary is FAIL;
5. any NOT TESTED result is reported honestly and evaluated against the release policy rather than silently treated as PASS;
6. the two explicit deferred boundaries remain labelled exactly as their evidence supports;
7. no release-blocking crash, corruption or authority regression is observed.

Until those conditions are recorded, project documentation must say **0.3.0 published / installed acceptance pending**, not installed accepted.

---

## 8. Failure and rollback

If an installed case reveals a release-blocking defect:

1. stop the server;
2. preserve privacy-safe logs and the failed LivingWorld evidence;
3. restore the offline test-world/config/mod backup or a clean test state;
4. reinstall the known-good `0.2.0+1.21.1` release;
5. reproduce/fix the defect in a narrow TDD development PR;
6. do not move or overwrite the immutable `0.3.0+1.21.1` tag;
7. if runtime bytes must change after publication, ship a new release version.

Release recovery may reconstruct missing metadata/assets only from the immutable tag commit; it never reassigns the tag to different code.

---

## 9. Post-acceptance reconciliation

After installed evidence is complete, update canonical state separately:

```text
docs/PROJECT_STATE.md
docs/ROADMAP.md
CHANGELOG.md validation evidence if the result changes release history
```

Only then may the roadmap advance from 0.3 release closure into the next 0.4 product slice.
