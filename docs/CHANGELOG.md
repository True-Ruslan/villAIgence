# VillAIgence Changelog

> Human-readable implementation and validation history. For exact current state and next priority, read `docs/PROJECT_STATE.md`. For long-term direction, read `docs/ROADMAP.md`.

## 2026-07-31 — Residual Step 1 security acceptance harness

**Status:** implementation and automated validation prepared in PR #68; controlled release/server execution remains pending.

### Added

- dependency-free Python provider harness restricted to literal loopback;
- deterministic normal, declared-oversize, chunked-oversize, oversized-error, redirect and slow-drip routes for Chat/STT/TTS/verification;
- streamed hostile payloads without whole-body allocation;
- sanitized JSONL evidence containing header-presence booleans and query-key names, never values;
- shared JDK-only account-verification transport preserving fixed production origin policy and disabled redirects;
- exact-release-JAR verification probe restricted to literal loopback;
- exact-release-JAR voice probe covering the 1..120 second clamp, 128 MiB concurrent budget, overflow rejection, release and recovery;
- package smoke checks requiring every probe/transport class in the distributable Fabric JAR;
- standard-library harness tests in read-only repository security CI;
- current approved script inventory expanded from the historical H5 five launchers to seven reviewed scripts.

### Safety boundary

```text
no in-game command
no Minecraft startup hook
no production-provider traffic
no production credential lookup by probes
no config or persistence schema change
```

SEC-003, SEC-004 and SEC-007 remain Open. Tooling alone is not closure evidence. The next candidate, expected `0.1.16+1.21.1`, must complete `docs/security/LOCAL_SECURITY_ACCEPTANCE_HARNESS.md` on the controlled server.

---

## 2026-07-31 — 0.1.15 production and endpoint-policy validation

**Status:** PASS within executed real-server scope; latest production/security live checkpoint.

```text
tag: 0.1.15+1.21.1
commit: 26070c37b806897e37cc3dabe2e4b27af458ac20
JAR: villaigence-fabric-0.1.15+1.21.1.jar
SHA-256: af142be94885541bb4840d0effff73627afe3f0e245dec8307ed665701cc94fb
```

Validated production Text/STT/TTS, Pio/Justino isolation, Pio memory recall, TTS fail-soft behavior, controlled OpenRouter 429 handling, six-file restart hashes, endpoint rejection, byte-identical configuration restoration and log redaction.

```text
Closed: SEC-001, SEC-002
Pending isolated acceptance: SEC-003, SEC-004, SEC-007
```

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.15.md
docs/security/SECURITY_AUDIT_FOLLOW_UP_2026-07-31_RUNTIME_0.1.15.md
```

---

## 2026-07-31 — Step 1 security and supply-chain hardening

**Status:** repository-side H1–H5 merged and automated-CI validated; production endpoint-policy validation passed in `0.1.15+1.21.1`; isolated SEC-003/SEC-004/SEC-007 acceptance remains.

### Merge sequence

```text
PR #59 H1 endpoint/credential policy       787f1a781b5970d4bafb851bfb3c7cba7c21fc0a
PR #60 H2 bounded provider/voice resources 15c56526417ac7dfb76567d51d1aa107f522cda7
PR #61 H3 verified supply chain            4cf9aef2e5c31a5682a7cad8544219154330e056
PR #62 H4 CI security coverage             05d105c1f558d5643b8190a88cc744b4d7cbe129
PR #63 H5 legacy-tool closure              6d82b4e4650294a4a42b9ea2113e64d990e08811
PR #64 canonical closure                  26070c37b806897e37cc3dabe2e4b27af458ac20
```

### Provider and runtime hardening

- provider URLs are parsed and normalized before credentials are selected;
- remote plaintext endpoints are rejected;
- OpenAI/OpenRouter/custom credentials cannot cross endpoint-family boundaries;
- authenticated redirects are not followed;
- Chat, STT, TTS, provider-error and verification bodies are byte-bounded;
- slow-drip responses have a hard total deadline;
- account verification uses a fixed trusted origin;
- voice capture is clamped to `1..120` seconds;
- aggregate active PCM is bounded to 128 MiB.

### Supply-chain and CI hardening

- Fabric Loom uses stable `1.17.17`;
- Gradle wrapper checksum and wrapper validation are enforced;
- external GitHub Actions are pinned to immutable commit SHAs;
- dependency verification metadata and lockfiles are committed;
- release packages contain a deterministic dependency manifest;
- primary CI requires common tests, Fabric and NeoForge;
- repository policy scans secrets, dangerous APIs, workflow permissions and scripts;
- exact-head tracked-tree evidence is retained.

### Whole-tree cleanup

- inherited non-CI utilities were reviewed;
- deprecated Google/AWS TTS, external localization, translation, name and skin generation utilities were removed;
- unmanaged Python requirements and the raw name dataset were removed;
- generated game resources remain unchanged;
- H5 reduced the executable/script surface to five Gradle/CI/security launchers; the later reviewed acceptance harness and its CI test intentionally bring the current approved inventory to seven scripts.

### Finding status

```text
Closed: SEC-001, SEC-002, SEC-005, SEC-006, SEC-008, SEC-009
Pending isolated acceptance: SEC-003, SEC-004, SEC-007
```

`0.1.14+1.21.1` remains the forgetting/decay checkpoint. `0.1.15+1.21.1` is the latest production/security live checkpoint; remaining isolated acceptance uses `docs/security/LOCAL_SECURITY_ACCEPTANCE_HARNESS.md` in a release containing PR #68.

---

## 0.1.14+1.21.1 — Deterministic Semantic Memory forgetting/decay live-server checkpoint

**Status:** live-tested successfully on a real Minecraft 1.21.1 server after controlled retention pressure and final restart on 2026-07-31.

Validation marker:

```text
V0114_FINAL_RESTART_VERIFICATION_PASS
```

Release identity:

```text
release/tag and tested commit:
c45aea45dd915b24ba236344feef30559c7171bb
```

The current `1.21.1` branch is a descendant of this tag. Later H1–H5 security commits are not part of the tested payload.

### Controlled capacity

```text
capacity during pressure test: 3
capacity restored after test: 256
```

The final restart and hash comparison were performed after restoring the normal capacity.

### Forgetting and durability evidence

```text
older corroborated Basiliso FACT survived pressure        PASS
Basiliso semantic UUID preserved                          PASS
Basiliso sourceEventIds preserved                         PASS
source-evidence durability affected retention             PASS
decay ordering among equal entries                        PASS
weak Casimiro RELATIONSHIP_CHANGE FACT evicted            PASS
```

The live scenario confirms that the policy no longer behaves as newest-only trimming. Older strong and corroborated knowledge can survive while a weak existing semantic entry is removed.

### NPC isolation

```text
Basiliso pressure isolated                                PASS
Casimiro pressure isolated                                PASS
cross-NPC eviction, merge or reinforcement                none
```

### Restart persistence

Byte-identical after restart:

```text
memory.json                                                PASS
memory2.json                                               PASS
semantic-memory.json                                       PASS
relationships.json                                         PASS
voices.json                                                PASS
```

Semantic UUIDs and `sourceEventIds` for the tested Basiliso and Casimiro entries remained stable.

### Chat, voice and operations

```text
Chat                                                       SUCCESS
STT                                                        SUCCESS
TTS                                                        SUCCESS
Simple Voice Chat connection                               PASS
Opus initialization                                        PASS
UDP 24454 / 25565                                          PASS
LinuxGSM monitor                                           PASS
server STARTED                                             PASS
VillAIgence / persistence / OutOfMemory errors             none
```

### Explicit validation boundary

The normal gameplay pipeline did not exercise rejection of a **new** weak candidate:

- three different social scenarios were attempted;
- the active Chat model produced no `RELATIONSHIP_CHANGE`;
- no new weak semantic candidate reached the store;
- therefore byte-identical file behavior for rejected-new-append was not live-observed.

Current evidence:

```text
existing weak FACT eviction under pressure                live-proven
rejected-new-append no-rewrite                             automated-proven
rejected-new-append no-rewrite                             not live-proven
```

Automated anchor:

```text
rejectedWeakAppendDoesNotRewriteSemanticFile
```

Exact feature head and CI:

```text
c08b47431b6a121deae4be8410be1e4fe4c5126a
VillAIgence CI #764 / 30573965448 — SUCCESS
Java Pull Request CI #307 / 30573965439 — SUCCESS
```

This boundary does not invalidate the live forgetting/decay checkpoint. It prevents claiming that this one branch was reached on a real server.

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.14.md
```

Development consequence:

- forgetting/decay, source durability, persistence and NPC isolation move from CI-only to live-proven;
- the next release-validation priority is the controlled H1/H2 security scenario in a build containing PRs #59–#63;
- rejected-new-append may be exercised later through a deterministic test path, but should not block progress on model randomness.

---

## Post-0.1.13 — Deterministic Semantic Memory forgetting and decay

**Status:** merged and automated-CI validated in PR #56; subsequently live-validated by `0.1.14+1.21.1`, except for the rejected-new-append branch described above.

### What changed

Added `SemanticMemoryRetentionPolicy` and integrated it into `SemanticMemoryStore` after consolidation.

The previous policy removed the oldest entry whenever the per-NPC limit was exceeded. The new policy keeps all valid knowledge while under capacity and forgets only under retention pressure.

```text
exact UUID replay check
→ append candidate
→ deterministic consolidation
→ deterministic retention selection
→ atomic save only when retained state changed
```

### Durability and decay

```text
importance contribution = importance × 4                 // 0..400
confidence contribution = confidence × 5 / 2             // 0..250

provenance contribution:
SYSTEM_OBSERVED = 200
PLAYER_TOLD     = 100
NPC_TOLD        = 75
INFERRED        = 25

source contribution = min(sourceEventIds count, 6) × 25   // 0..150
```

```text
DECAY_STEP_TICKS = 36000
ageTicks = max(0, nowGameTime - entry.gameTime)
effectiveRetentionScore = durability × 36000 - ageTicks
```

At 20 TPS, one decay step is approximately 30 minutes of active server runtime.

### Deterministic selection

```text
effective score descending
importance descending
confidence descending
source count descending
gameTime descending
createdAtEpochMillis descending
UUID ascending
```

### Safety and persistence

- entries are never removed while under capacity;
- confidence is not mutated;
- consolidation occurs before forgetting;
- corroboration adds bounded source durability and preserves every source UUID;
- exact UUID replay remains a no-op;
- NPC pressure remains isolated;
- no timer, background task, wall-clock TTL or world-tick hook exists;
- JSON remains format version 1;
- no config fields were added.

### TDD anchors

```text
policy RED:
1ea407ab2cd16eb74ea86dacb1aa04e476341e34
VillAIgence CI #756 / 30572701052 → expected FAILURE

policy GREEN:
0410b8c4b9bcbe604effca2154d092ef6a2af1a5
VillAIgence CI #758 / 30572959755 → SUCCESS
Java Pull Request CI #304 / 30572959844 → SUCCESS

store RED:
4c42a3f13f73657fade630fa6fd212e0a7677657
VillAIgence CI #760 / 30573293522 → expected FAILURE

final exact head:
c08b47431b6a121deae4be8410be1e4fe4c5126a
VillAIgence CI #764 / 30573965448 → SUCCESS
Java Pull Request CI #307 / 30573965439 → SUCCESS

merge:
73145dd0925d403af7ef343521eb3ae27f68804d
```

Documentation:

```text
docs/livingworld/SEMANTIC_FORGETTING_DECAY.md
docs/superpowers/specs/2026-07-30-memory2-semantic-forgetting-decay-design.md
docs/superpowers/plans/2026-07-30-memory2-semantic-forgetting-decay.md
```

---

## 0.1.13+1.21.1 — Deterministic Semantic Memory consolidation live-server checkpoint

**Status:** live-tested successfully on 2026-07-30.

```text
release/tag and tested commit:
b553bf7e83674145bdf42927b9ace7287afa560c
```

Validated:

```text
same-knowledge authoritative ACTION events: 2              PASS
ACTION UUIDs distinct                                      PASS
consolidated semantic entries: 1                           PASS
both sourceEventIds present exactly once                   PASS
deterministic UUID independently reproduced                PASS
retry created another ACTION                               no
retry changed semantic-memory.json                         no
NPC A / NPC B isolation                                    PASS
```

Deterministic UUID:

```text
093aabb0-e61b-3e62-a5fe-fbb9d15b8494
```

Byte-identical after restart:

```text
memory.json
memory2.json
semantic-memory.json
relationships.json
voices.json
```

Operations:

```text
Chat / DIALOGUE                                             PASS
Simple Voice Chat / Opus                                   PASS
STT / TTS                                                  PASS
UDP 24454 / 25565                                          PASS
LinuxGSM monitor                                           PASS
server STARTED                                             PASS
```

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.13.md
```

---

## Post-0.1.12 — Deterministic Semantic Memory consolidation

**Status:** PR #53 merged, automated-CI validated and live-validated by `0.1.13`.

```text
merge: f85879d254f37d7f860380362b296e047bbbb781
verified head: 19c3d3e840431cc2b1b34e1841e2075f56e99f71
VillAIgence CI #746 / 30561015885 → SUCCESS
Java Pull Request CI #300 / 30561015985 → SUCCESS
```

Consolidation preserves provenance and all independent `sourceEventIds`, keeps FACT and BELIEF separate, isolates related entities and uses deterministic UUIDs without fuzzy matching, embeddings or LLM classification.

Documentation:

```text
docs/livingworld/SEMANTIC_CONSOLIDATION.md
docs/superpowers/specs/2026-07-30-memory2-semantic-consolidation-design.md
```

---

## 0.1.12+1.21.1 — Controlled Semantic Memory live-server checkpoint

**Status:** live-tested successfully on 2026-07-30.

```text
release/tag and tested commit:
746fa75ab4b5f4bee385efa0c8ae51009c1aec58
```

Validated ACTION and RELATIONSHIP_CHANGE FACT ingestion, deterministic source-linked semantic UUIDs, retry idempotency, DIALOGUE exclusion, restart persistence and Chat/STT/TTS/Voice Chat health.

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.12.md
```

---

## Post-0.1.11 — Controlled Semantic Memory ingestion

**Status:** PR #49 merged, automated-CI validated and live-validated by `0.1.12`.

```text
merge: c6a7a17aa5bd7806667ff3b8b502b852640e606c
verified head: 7f9916e510a2ca70245d93c5b308ee31758fed0f
VillAIgence CI #721 / 30548196801 → SUCCESS
Java Pull Request CI #289 / 30548198746 → SUCCESS
```

Automatic conversion accepts server-observed ACTION, OBSERVATION and RELATIONSHIP_CHANGE events and rejects ordinary DIALOGUE, told provenance and inferred provenance.

---

## 0.1.11+1.21.1 — Working Memory checkpoint

Validated bounded persistent and prompt history, NPC isolation, retry recovery, three voice turns, restart persistence and operational health.

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.11.md
```

---

## 0.1.10+1.21.1 — Text/voice Memory 2.0 parity checkpoint

Validated shared text/voice post-success ingestion, bounded DIALOGUE events, NPC isolation, legacy plus Memory 2.0 persistence and unchanged voice behavior.

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.10.md
```

---

## 0.1.8+1.21.1 — Reliability foundation

Established safe provider response parsing, `content:null` handling, diagnostics, admission/backpressure, persistent JSON recovery and stable voice/memory persistence.

```text
release commit:
23fba1ee373e932c0b17aba3755f8ac478c26941

workflow:
29918008438 → SUCCESS
```