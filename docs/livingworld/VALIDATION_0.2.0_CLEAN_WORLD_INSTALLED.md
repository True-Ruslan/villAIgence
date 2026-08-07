# VillAIgence 0.2.0 clean-world installed acceptance

Status: **EXACT CANDIDATE READY / INSTALLED ACCEPTANCE PENDING**

Target tag:

```text
0.2.0+1.21.1
```

This document is the canonical operator runbook and evidence template for the first installed Memory 2.0 clean-cutover boundary.

Automated exact-JAR acceptance and installed operator evidence are deliberately separate claims. Do not mark this document PASS from CI alone.

---

## 1. Scope

This installed plan validates only risks that remain meaningful on a real server/client after the automated acceptance matrix:

- clean installation of the exact candidate;
- real provider-backed text dialogue;
- real client/provider voice dialogue;
- Memory 2.0 prompt-history recall;
- exact NPC isolation;
- same-world restart persistence;
- absence of the removed `memory.json` runtime path.

Do **not** manually repeat deterministic grave, navigation, fishing, mounted-combat, codec, corruption-recovery, replay/idempotency or logical-session cases already covered by CI unless a new installed symptom points at them.

`VAI-CONCUR-004` remains separately **NOT TESTED / DEFERRED** because two real graphical clients are not available. This document must not convert it to PASS.

---

## 2. Candidate identity

Exact release-request evidence:

```text
release-request PR:          #120
exact PR head:               0cac5dd0dc80cb72fa1ed2014b9b49e2c848efce
GitHub Release dry-run:      #378 / run 31172619861
candidate artifact id:       8991703892
artifact name:               villaigence-fabric-package
candidate JAR:               villaigence-fabric-dry-run-378.jar
candidate JAR SHA-256:       56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee
dependency manifest SHA-256: b16a7b842776d44ed21cad1b56cee63aadc782ada457c108c5107c483aab5816
embedded version:            0.2.0+1.21.1
embedded mod id:             mca
Minecraft:                   1.21.1
Java:                        21
```

The release dry-run passed exact production startup/save/restart, current five-store destructive recovery, risk catalog and server GameTests, Fabric and NeoForge builds, package smoke and byte-for-byte equality between the production-accepted and packaged JAR. The publication job was correctly skipped on the pull request.

Independent artifact inspection reproduced the checksum contained in the workflow package and confirmed `fabric.mod.json` embeds version `0.2.0+1.21.1` with mod id `mca` and public name `VillAIgence`.

Use only this exact JAR for the installed test. Do not substitute a locally built JAR, another workflow artifact or a later commit. If PR #120 receives any further commit after this candidate identity was recorded, this exact installed candidate is stale and the release dry-run artifact/identity must be regenerated and re-recorded before testing.

---

## 3. Server preparation

### 3.1 Safe test target

Preferred:

```text
dedicated test server
+ new test world
+ clean <world>/livingworld/
```

Acceptable:

```text
offline copy of an existing test world
+ remove <copied-world>/livingworld/
+ keep the original world untouched
```

Do not use the only copy of a valuable world.

### 3.2 Offline backup

With the server stopped, preserve at minimum:

```text
world/
config/
mods/
```

If an older LivingWorld state exists, archive it separately before creating the clean state:

```text
<world>/livingworld/
config/livingworld.json
```

Record the prior known-good server version. Expected current rollback baseline:

```text
0.1.26+1.21.1
```

### 3.3 Runtime stack

Server:

```text
Minecraft 1.21.1
Java 21
Fabric Loader
Fabric API compatible with 1.21.1
Simple Voice Chat 2.6.20+
exact VillAIgence 0.2.0 candidate JAR
```

Graphical client:

```text
Minecraft 1.21.1
Fabric Loader
Fabric API
Simple Voice Chat
same exact VillAIgence candidate JAR
```

Original MCA Reborn must not be installed alongside VillAIgence.

### 3.4 Credentials/configuration

Credentials remain server-only. Never place provider keys in Git, client modpacks, release evidence or logs.

Retain the known-good provider/model configuration from the accepted `0.1.26` server unless provider behavior itself must be investigated. The previously accepted Chat model was:

```text
google/gemini-2.5-flash-lite
```

### 3.5 Clean Memory 2.0 state

Before first candidate startup, the active test world must not contain the old experimental conversation store.

Expected clean boundary:

```text
<world>/livingworld/memory.json    DOES NOT EXIST
```

A fresh `livingworld/` directory may be absent entirely before startup.

---

## 4. Stop conditions

Immediately stop installed acceptance and preserve evidence if any of these occur:

- server crash;
- blocking Mixin/refmap/injection failure;
- Memory 2.0 corruption/recovery loop;
- `memory.json` is recreated;
- one NPC recalls another NPC's private dialogue;
- one successful turn is persisted twice;
- voice produces duplicate persistent dialogue or duplicate audible playback;
- same-world restart loses the tested dialogue history;
- unexpected NPC identity change during ordinary restart.

On stop preserve:

```text
latest.log
crash-report/ if present
candidate JAR SHA-256
<world>/livingworld/
exact last operator action
```

Do not merge or publish while any required installed case is FAIL.

---

# 5. Installed test plan

Use two distinct unique phrases that are unlikely to appear by chance:

```text
TEXT CODE  = blue-cactus-731
VOICE CODE = silver-fox-482
```

Russian or another natural-language wrapper is fine, but keep the exact code tokens unchanged so result evaluation is unambiguous.

## VAI-M2-INST-001 — clean startup

Preconditions:

- clean test world/LivingWorld state;
- exact candidate installed on server and client;
- original MCA Reborn absent.

Steps:

1. Start the server with no player connected.
2. Wait for full startup.
3. Connect one graphical client.
4. Select one ordinary NPC (`NPC A`).

PASS requires:

- server reaches normal started state;
- client connects;
- NPC A is alive/mobile;
- no blocking VillAIgence/Mixin/refmap/injection errors;
- `memory2.json` is available after Memory 2.0 is first used;
- `memory.json` does not appear.

Evidence:

```text
result: TBD
notes:  TBD
```

## VAI-M2-INST-002 — first text persistence

With Player A talking to NPC A, send a natural sentence containing exactly:

```text
blue-cactus-731
```

Example intent: ask NPC A to remember that code for this conversation.

Wait for one successful NPC reply.

PASS requires one successful dialogue turn with no duplicate response/effect. The corresponding Memory 2.0 event must belong to the exact NPC A / Player A pair.

Evidence:

```text
result: TBD
notes:  TBD
```

## VAI-M2-INST-003 — immediate text recall

Without repeating the code, ask NPC A which code Player A just provided.

PASS requires NPC A to return `blue-cactus-731` or an unambiguously equivalent answer containing the exact token.

Evidence:

```text
result: TBD
notes:  TBD
```

## VAI-M2-INST-004 — NPC isolation

Select a different NPC (`NPC B`) that has not received the code.

Ask NPC B which code Player A previously provided.

PASS requires NPC B **not** to know or reproduce `blue-cactus-731` from NPC A's private dialogue history.

FAIL if NPC B receives or reproduces NPC A's dialogue memory as if it were its own.

Evidence:

```text
result: TBD
notes:  TBD
```

## VAI-M2-INST-005 — player isolation (optional installed extension)

This case requires a second real player identity. It is optional for the operator-only test environment because exact player isolation is already automated.

If a second player/client becomes available:

1. Player B talks to NPC A.
2. Player B asks for Player A's private code without being told it in that session.

PASS requires no leakage of `blue-cactus-731` through the exact NPC/player dialogue-history path.

If unavailable record:

```text
NOT TESTED / AUTOMATED EVIDENCE ONLY
```

Do not confuse this with `VAI-CONCUR-004`, which is the separate two-client Operator Lore graphical conflict scenario.

## VAI-M2-INST-006 — physical voice uses the same dialogue memory

Return Player A to NPC A.

Using the physical microphone and installed Simple Voice Chat path, speak a natural sentence containing exactly:

```text
silver-fox-482
```

Wait for one audible NPC response. Then use text and ask NPC A for the second code without repeating it.

PASS requires:

- one physical voice turn completes;
- one audible NPC response occurs without duplicate playback;
- NPC A later recalls `silver-fox-482` through normal prompt history;
- no separate/duplicate legacy conversation store appears.

Evidence:

```text
result: TBD
notes:  TBD
```

## VAI-M2-INST-007 — same-world restart recall

After INST-002 through INST-006:

1. Stop the server cleanly.
2. Preserve hashes/copies of the current LivingWorld stores for evidence if convenient.
3. Restart the same server using the same world and candidate JAR.
4. Reconnect Player A.
5. Select the same NPC A.
6. Ask which two codes Player A provided earlier, without repeating either code.

PASS requires NPC A to recall both:

```text
blue-cactus-731
silver-fox-482
```

Also require:

- same NPC identity/UUID;
- no startup persistence corruption;
- no duplicate dialogue generated merely by restart.

Evidence:

```text
result: TBD
notes:  TBD
```

## VAI-M2-INST-008 — no legacy resurrection

After the restart, inspect the active world persistence directory.

PASS requires:

```text
memory2.json    EXISTS
memory.json     DOES NOT EXIST
```

The expected current auxiliary persistence files are:

```text
memory2.json
semantic-memory.json
relationships.json
voices.json
operator-lore.json
```

`events.json` may also exist as authoritative factual event history and is not part of the five-store corruption matrix.

Evidence:

```text
result: TBD
notes:  TBD
```

---

# 6. Acceptance result

Required release-request installed boundary:

```text
VAI-M2-INST-001  TBD
VAI-M2-INST-002  TBD
VAI-M2-INST-003  TBD
VAI-M2-INST-004  TBD
VAI-M2-INST-006  TBD
VAI-M2-INST-007  TBD
VAI-M2-INST-008  TBD
```

Optional/deferred boundaries:

```text
VAI-M2-INST-005  OPTIONAL / automated evidence sufficient when second player unavailable
VAI-CONCUR-004   NOT TESTED / DEFERRED until two graphical clients are available
```

Release-request PR may be merged only when all seven required `VAI-M2-INST-*` cases are PASS and no required case is FAIL.

---

# 7. Evidence to return after testing

A compact operator report is sufficient:

```text
candidate SHA-256: 56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee

VAI-M2-INST-001 — PASS/FAIL
VAI-M2-INST-002 — PASS/FAIL
VAI-M2-INST-003 — PASS/FAIL
VAI-M2-INST-004 — PASS/FAIL
VAI-M2-INST-006 — PASS/FAIL
VAI-M2-INST-007 — PASS/FAIL
VAI-M2-INST-008 — PASS/FAIL
VAI-M2-INST-005 — PASS/FAIL/NOT TESTED
VAI-CONCUR-004  — NOT TESTED / DEFERRED

notes:
<only anomalies or material observations>
```

Attach `latest.log` only if there is a failure, suspicious warning, persistence anomaly or provider/voice issue requiring investigation.

---

# 8. Rollback

If a required case fails:

1. stop the candidate server;
2. keep failed evidence intact;
3. restore the offline test backup or known-good `0.1.26+1.21.1` setup;
4. leave the `0.2.0+1.21.1` tag unused;
5. do not merge the release-request PR;
6. fix under a separate development PR and generate a fresh exact candidate.

Because no public release tag exists at this stage, the candidate remains safely replaceable until installed acceptance succeeds.
