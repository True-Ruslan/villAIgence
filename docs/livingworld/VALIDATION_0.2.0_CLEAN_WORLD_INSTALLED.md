# VillAIgence 0.2.0 clean-world installed acceptance

Status: **INSTALLED ACCEPTANCE PASS FOR TESTED JAR BYTES / FINAL PR-HEAD BYTE-IDENTITY RECHECK REQUIRED**

Target tag:

```text
0.2.0+1.21.1
```

This document is the canonical operator runbook and evidence record for the first installed Memory 2.0 clean-cutover boundary.

Automated exact-JAR acceptance and installed operator evidence are deliberately separate claims. Installed evidence is bound to the tested JAR bytes by SHA-256. A later documentation-only release-request commit may reuse the installed evidence only when a fresh exact release dry-run proves that its packaged JAR has the **same SHA-256** as the installed candidate. Any runtime-byte change requires a fresh installed test.

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

## 2. Exact candidate identity rule

The concrete candidate identity is recorded in PR #120 after the latest successful non-publishing release dry-run for the current PR head.

Required identity fields:

```text
release-request PR
exact PR head
GitHub Release dry-run number / run id
candidate artifact id
candidate JAR filename
candidate JAR SHA-256
dependency manifest SHA-256
embedded version
```

Do not hard-code a self-referential current PR head into this file. The PR description is the authoritative mutable handoff for the exact candidate while the release request is open.

The installed operator run reported against these tested bytes:

```text
candidate JAR SHA-256: 56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee
embedded version:      0.2.0+1.21.1
mod id:                mca
name:                  VillAIgence
```

The tested bytes came from GitHub Release dry-run #380 / run `31173200381` before this evidence/oracle-only documentation correction.

A subsequent current-head dry-run may carry this installed evidence forward **only if** its packaged JAR SHA-256 is still exactly:

```text
56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee
```

If the SHA differs, this installed result is stale and the new JAR must be retested.

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

Record the prior known-good server version. Expected rollback baseline:

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
- same-world restart loses the **accepted STT transcript** from the tested voice turn;
- unexpected NPC identity change during ordinary restart.

A speech recognizer changing punctuation, hyphenation, capitalization, or numeral formatting is **not by itself a Memory 2.0 failure**. It becomes a voice/STT failure only when the transcript loses or changes the intended identifying content enough that the spoken information is no longer recognizable.

On stop preserve:

```text
latest.log
crash-report/ if present
candidate JAR SHA-256
<world>/livingworld/
exact last operator action
```

Do not merge or publish while any required installed case is a genuine product/runtime FAIL.

---

# 5. Installed test plan

Use distinct unique markers that are unlikely to appear by chance.

For text, exact spelling is under player control and may be asserted literally:

```text
TEXT MARKER = blue-cactus-731
```

For physical voice, the spoken seed is only an input to STT:

```text
VOICE SEED = silver-fox-482
```

The voice oracle must **not** require STT to preserve punctuation, hyphens, capitalization or digit formatting. Record the actual accepted STT transcript as `VOICE_TRANSCRIPT = T`, then validate Memory 2.0 against `T`.

Example acceptable normalization:

```text
spoken seed:       silver-fox-482
accepted STT text: SilverFox482
```

That example is not a failure if `SilverFox482` remains the unique recognizable content and is what Memory 2.0 persists and recalls.

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

## VAI-M2-INST-002 — first text persistence

With Player A talking to NPC A, send a natural sentence containing exactly:

```text
blue-cactus-731
```

Wait for one successful NPC reply.

PASS requires one successful dialogue turn with no duplicate response/effect. The corresponding Memory 2.0 event must belong to the exact NPC A / Player A pair.

## VAI-M2-INST-003 — immediate text recall

Without repeating the marker, ask NPC A which marker Player A just provided.

PASS requires NPC A to return `blue-cactus-731` or an unambiguously equivalent answer containing the exact token.

## VAI-M2-INST-004 — NPC isolation

Select a different NPC (`NPC B`) that has not received the marker and ask which marker Player A previously provided.

PASS requires NPC B **not** to know or reproduce `blue-cactus-731` from NPC A's private dialogue history.

FAIL if NPC B receives or reproduces NPC A's dialogue memory as if it were its own.

## VAI-M2-INST-005 — player isolation (optional installed extension)

This case requires a second real player identity. It is optional for the operator-only test environment because exact player isolation is already automated.

If a second player/client becomes available, Player B talks to NPC A and asks for Player A's private marker without being told it in that session.

PASS requires no leakage of `blue-cactus-731` through the exact NPC/player dialogue-history path.

If unavailable record:

```text
NOT TESTED / AUTOMATED EVIDENCE ONLY
```

Do not confuse this with `VAI-CONCUR-004`, the separate two-client Operator Lore graphical conflict scenario.

## VAI-M2-INST-006 — physical voice uses the same dialogue memory

Return Player A to NPC A. Using the physical microphone and installed Simple Voice Chat path, speak a natural sentence containing the unique voice seed.

After STT completes, record the actual accepted transcript as `T`. Then use text and ask NPC A for the voice marker without repeating it.

PASS requires:

- one physical voice turn completes;
- STT produces one nonblank accepted transcript `T` whose unique intended content remains recognizable;
- one audible NPC response occurs without duplicate playback;
- the Memory 2.0 DIALOGUE player message corresponds to `T` rather than the pre-STT spoken spelling;
- NPC A later recalls the identifying content from `T` through normal prompt history;
- no separate/duplicate legacy conversation store appears.

Do not fail this case solely because `silver-fox-482` became `SilverFox482`, `silver fox 482`, a localized capitalization variant, or another orthographic normalization that preserves the unique content.

## VAI-M2-INST-007 — same-world restart recall

After INST-002 through INST-006:

1. Stop the server cleanly.
2. Restart the same server using the same world and candidate JAR.
3. Reconnect Player A.
4. Select the same NPC A.
5. Ask which text marker and voice marker Player A provided earlier, without repeating either one.

PASS requires:

- NPC A recalls `blue-cactus-731` from the text turn;
- NPC A recalls the same unique identifying content represented by the accepted voice transcript `T`;
- the restart does not introduce a new transformation beyond ordinary LLM presentation of the already persisted `T`;
- same NPC identity/UUID;
- no startup persistence corruption;
- no duplicate dialogue generated merely by restart.

Exact equality to the pre-STT spelling `silver-fox-482` is **not** required. The persistence assertion starts at the accepted STT transcript boundary.

## VAI-M2-INST-008 — no legacy resurrection

After the restart, inspect the active world persistence directory.

PASS requires:

```text
memory2.json    EXISTS
memory.json     DOES NOT EXIST
```

Expected current auxiliary persistence files are:

```text
memory2.json
semantic-memory.json
relationships.json
voices.json
operator-lore.json
```

`events.json` may also exist as authoritative factual event history and is not part of the five-store corruption matrix.

---

# 6. Installed result — 2026-08-07

Operator initially reported the literal-token oracle as:

```text
5 PASS / 2 FAIL

VAI-M2-INST-001 — PASS
VAI-M2-INST-002 — PASS
VAI-M2-INST-003 — PASS
VAI-M2-INST-004 — PASS
VAI-M2-INST-006 — FAIL
VAI-M2-INST-007 — FAIL
VAI-M2-INST-008 — PASS
```

Observed reason for both reported FAILs:

```text
spoken seed:                 silver-fox-482
accepted/persisted STT text: SilverFox482
after restart:               NPC reproduced the same STT-normalized form
```

The physical microphone path, STT completion, AI response, TTS and spatial audio otherwise worked normally. The restart retained the recognized voice content.

### Root-cause classification

The two reported FAILs were **false negatives caused by an invalid acceptance oracle**, not Memory 2.0 runtime failures:

1. physical STT is not an exact punctuation/case transcription contract;
2. the old test compared post-STT memory to the pre-STT spelling;
3. the actual Memory 2.0 boundary begins at the accepted STT transcript;
4. `SilverFox482` was persisted and survived restart as the same recognized content.

Therefore, under the corrected contract, the installed Memory 2.0 result for the tested JAR bytes is:

```text
VAI-M2-INST-001 — PASS
VAI-M2-INST-002 — PASS
VAI-M2-INST-003 — PASS
VAI-M2-INST-004 — PASS
VAI-M2-INST-006 — PASS
VAI-M2-INST-007 — PASS
VAI-M2-INST-008 — PASS

Required total: 7 PASS / 0 FAIL

VAI-M2-INST-005 — NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004  — NOT TESTED / DEFERRED
```

### Non-blocking STT quality observation

```text
STT normalized silver-fox-482 -> SilverFox482
```

This is retained as a provider/transcription-quality observation. It does not indicate memory corruption or restart loss. Exact punctuation fidelity from free-form physical speech is not a 0.2.0 Memory 2.0 release requirement.

---

# 7. Final release-request carry-forward rule

The release-request PR may treat installed acceptance as complete without repeating the manual server test only when the **latest current-head non-publishing release dry-run** produces a packaged JAR whose SHA-256 exactly equals the tested SHA:

```text
56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee
```

This permits documentation/evidence corrections without pretending a different runtime binary was installed. If the JAR SHA changes for any reason, installed acceptance becomes pending again.

---

# 8. Rollback

If a genuine required runtime case fails:

1. stop the candidate server;
2. keep failed evidence intact;
3. restore the offline test backup or known-good `0.1.26+1.21.1` setup;
4. leave the `0.2.0+1.21.1` tag unused;
5. do not merge the release-request PR;
6. fix under a separate development PR and generate a fresh exact candidate.

Because no public release tag exists at this stage, the candidate remains safely replaceable until installed acceptance and final byte-identity verification succeed.
