# VillAIgence Changelog

> Human-readable implementation and validation history. For exact current state and next priority, read `docs/PROJECT_STATE.md`. For long-term direction, read `docs/ROADMAP.md`.

## 2026-07-31 — Step 1 security and supply-chain hardening

**Status:** repository-side H1–H5 merged and automated-CI validated; controlled H1/H2 real-server validation remains.

### Merge sequence

```text
PR #59 H1 endpoint/credential policy       787f1a781b5970d4bafb851bfb3c7cba7c21fc0a
PR #60 H2 bounded provider/voice resources 15c56526417ac7dfb76567d51d1aa107f522cda7
PR #61 H3 verified supply chain            4cf9aef2e5c31a5682a7cad8544219154330e056
PR #62 H4 CI security coverage             05d105c1f558d5643b8190a88cc744b4d7cbe129
PR #63 H5 legacy-tool closure              6d82b4e4650294a4a42b9ea2113e64d990e08811
```

### Provider and runtime hardening

- provider URLs are parsed and normalized before credentials are selected;
- remote plaintext endpoints are rejected; HTTP is limited to explicit lexical loopback development mode;
- OpenAI/OpenRouter/custom Chat, STT and TTS credentials cannot cross endpoint-family boundaries;
- authenticated redirects are not followed;
- Chat, STT, TTS, provider-error and verification bodies are byte-bounded;
- slow-drip responses have a hard total deadline;
- arbitrary-URL account verification was replaced with a fixed trusted-origin client;
- voice capture is clamped to `1..120` seconds and aggregate active PCM is bounded to 128 MiB.

### Supply-chain and CI hardening

- Fabric Loom moved from snapshot to stable `1.17.17`;
- Gradle wrapper distribution checksum and wrapper validation are enforced;
- external GitHub Actions are pinned to immutable commit SHAs;
- dependency verification metadata and per-project lockfiles are committed;
- third-party Maven content is restricted;
- release packages contain a deterministic lockfile-based dependency manifest;
- primary CI requires common tests, Fabric and NeoForge;
- a dependency-free policy scans high-confidence secrets, dangerous Java/Gradle APIs, workflow permissions and tracked scripts;
- exact-head script and whole-tree SHA-256 artifacts are retained.

### Whole-tree cleanup

- all inherited non-CI utilities were semantically reviewed;
- deprecated Google/AWS TTS, Crowdin/patron fetch, external-LLM localization, pirate translation, name generation and skin generation utilities were removed;
- the unmanaged Python requirements and 4.3 MiB raw name dataset were removed;
- generated game resources remain unchanged;
- the approved executable/script surface is now exactly five Gradle/CI/security launchers.

### Finding status

```text
Closed: SEC-005, SEC-006, SEC-008, SEC-009
Pending controlled server validation: SEC-001, SEC-002, SEC-003, SEC-004, SEC-007
```

`0.1.13+1.21.1` remains the latest live-validated checkpoint. The expected validation candidate is `0.1.15+1.21.1`; its test plan is `docs/security/H1_H2_CONTROLLED_SERVER_VALIDATION.md`.

## Post-0.1.13 — Deterministic Semantic Memory forgetting and decay

**Status:** merged and automated-CI validated in PR #56; real-server validation pending.

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

Entries are retained by:

```text
effective score descending
importance descending
confidence descending
source count descending
gameTime descending
createdAtEpochMillis descending
UUID ascending
```

The selected list is persisted in the existing oldest-first stable order.

### Behavior

- entries are never removed by this feature while under capacity;
- older strong knowledge can survive newer weak knowledge;
- confidence is not mutated;
- consolidation occurs before forgetting;
- corroboration adds bounded source durability and keeps all source UUIDs;
- a rejected weak append does not rewrite `semantic-memory.json`;
- exact UUID replay remains a no-op;
- NPC retention pressure remains isolated;
- no timer, background task, wall-clock TTL or world-tick hook was added;
- JSON format remains version 1;
- no config fields were added.

### TDD anchors

```text
policy RED head:
1ea407ab2cd16eb74ea86dacb1aa04e476341e34
VillAIgence CI #756 / 30572701052 → expected FAILURE
reason: SemanticMemoryRetentionPolicy did not exist

policy GREEN head:
0410b8c4b9bcbe604effca2154d092ef6a2af1a5
VillAIgence CI #758 / 30572959755 → SUCCESS
Java Pull Request CI #304 / 30572959844 → SUCCESS

store RED head:
4c42a3f13f73657fade630fa6fd212e0a7677657
VillAIgence CI #760 / 30573293522 → expected FAILURE
reason: four targeted tests exposed oldest-first trimming and rewrite behavior

final exact head:
c08b47431b6a121deae4be8410be1e4fe4c5126a
VillAIgence CI #764 / 30573965448 → SUCCESS
Java Pull Request CI #307 / 30573965439 → SUCCESS

merge:
73145dd0925d403af7ef343521eb3ae27f68804d
```

### Documentation

```text
docs/livingworld/SEMANTIC_FORGETTING_DECAY.md
docs/superpowers/specs/2026-07-30-memory2-semantic-forgetting-decay-design.md
docs/superpowers/plans/2026-07-30-memory2-semantic-forgetting-decay.md
```

### Validation boundary

A live checkpoint must still prove:

- real retention pressure;
- predicted retention of older strong knowledge;
- predicted rejection or eviction of weak knowledge;
- source corroboration affecting a real capacity decision;
- byte-stable file behavior after rejected append and restart;
- NPC isolation under pressure;
- unchanged Chat, STT, TTS, Voice Chat, Opus, monitor and port health.

`0.1.13+1.21.1` remains the latest live-validated release.

---

## 0.1.13+1.21.1 — Deterministic Semantic Memory consolidation live-server checkpoint

**Status:** live-tested successfully on a real Minecraft 1.21.1 server after restart on 2026-07-30.

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
NPC A / NPC B owner isolation                              PASS
NPC A / NPC B relatedEntities isolation                    PASS
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
0.1.13 loaded after restart                                PASS
Chat and DIALOGUE                                          PASS
Simple Voice Chat / Opus                                   PASS
STT / TTS voice dialogue                                   PASS
UDP 24454 / 25565                                          PASS
LinuxGSM monitor                                           PASS
server STARTED                                             PASS
VillAIgence / persistence / OutOfMemory errors             none
```

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.13.md
```

The primary append-time consolidation path is live-proven. A manually seeded old file containing compatible logical duplicates was not separately tested live; that path remains unit-tested.

---

## Post-0.1.12 — Deterministic Semantic Memory consolidation

**Status:** merged in PR #53, automated-CI validated, and live-validated by `0.1.13+1.21.1`.

```text
same ownerNpcId
+ same FACT/BELIEF kind
+ same provenance
+ same canonical statement
+ same related-entity UUID set
+ sourced evidence
→ one consolidated SemanticMemoryEntry
```

Canonical statement normalization:

```text
Unicode NFKC
+ whitespace/control collapse
+ trim
+ Locale.ROOT lowercase
```

Consolidated fields:

```text
sourceEventIds       sorted union
relatedEntities      sorted union
gameTime             max
createdAtEpochMillis max
importance           max
confidence           max
statement            deterministic representative
id                   deterministic consolidation UUID
```

Authority boundaries:

- different NPC owners remain separate;
- FACT never merges with BELIEF;
- different BELIEF provenance remains separate;
- different related entities remain separate;
- unsourced entries remain separate;
- confidence is not automatically increased;
- no LLM, embeddings or semantic similarity matching.

```text
PR #53 final head:
19c3d3e840431cc2b1b34e1841e2075f56e99f71

VillAIgence CI #746 / 30561015885 → SUCCESS
Java Pull Request CI #300 / 30561015985 → SUCCESS

merge:
f85879d254f37d7f860380362b296e047bbbb781
```

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

Validated:

```text
successful NPC actions: 2                                 PASS
ACTION MemoryEvents: 2                                    PASS
relationship transition: trust +1, affinity +1            PASS
RELATIONSHIP_CHANGE events: 1                             PASS
semantic FACT entries: 3                                  PASS
semantic UUID duplicates: 0                               PASS
retry-created semantic duplicates: 0                      PASS
ordinary DIALOGUE interactions: 9                         OBSERVED
DIALOGUE-derived semantic entries: 0                      PASS
```

Every FACT had correct kind, `SYSTEM_OBSERVED` provenance, NPC owner, source-event linkage and deterministic UUID.

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
Chat SUCCESS
STT SUCCESS / 1059 ms
TTS SUCCESS / 1663 ms
Voice Chat / Opus PASS
monitor PASS
UDP 24454 / 25565 PASS
startup persistence errors none
```

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.12.md
```

---

## Post-0.1.11 — Controlled Semantic Memory ingestion

**Status:** merged in PR #49, automated-CI validated, and live-validated by `0.1.12+1.21.1`.

```text
successful safe action
→ ACTION / SYSTEM_OBSERVED
→ semantic FACT

persisted relationship transition
→ RELATIONSHIP_CHANGE / SYSTEM_OBSERVED
→ semantic FACT
```

Automatic conversion rejects `DIALOGUE`, told provenance and inferred provenance. No arbitrary dialogue or LLM prose becomes FACT.

```text
PR #49 merge:
c6a7a17aa5bd7806667ff3b8b502b852640e606c
verified head:
7f9916e510a2ca70245d93c5b308ee31758fed0f
VillAIgence CI #721 / 30548196801 → SUCCESS
Java Pull Request CI #289 / 30548198746 → SUCCESS
```

---

## 0.1.11+1.21.1 — Working Memory live-server checkpoint

Validated:

```text
NPC A / NPC B isolation                                  PASS
NPC A durable history: 16                                PASS
NPC A prompt history: 12                                 PASS
memory2 UUID duplicates: 0                               PASS
logical fingerprint duplicates: 0                        PASS
three independent voice turns                            PASS
OpenRouter retry recovery                                PASS
restart persistence                                      PASS
server / monitor / ports                                 PASS
```

At this checkpoint `semantic-memory.json` was absent, which was expected before controlled semantic ingestion.

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.11.md
```

---

## Post-0.1.10 — Working Memory and Semantic Memory foundation

PR #46 introduced:

```text
recent dialogue = 12
episodic context = 6
semantic context = 6
```

And:

- FACT/BELIEF truth invariant;
- per-NPC semantic persistence;
- deterministic retrieval;
- UUID idempotency;
- atomic writes and fail-open recovery;
- layered prompt truth and instruction boundaries.

```text
PR #46 merge:
f82248ac79734200add0652fca663b93a71f2f18
verified head:
f8338bcf5371f062a31b6a50c8dbc4d992251bda
```

---

## 0.1.10+1.21.1 — Text/voice Memory 2.0 parity checkpoint

Validated:

- text and voice dialogue share one post-success ingestion lifecycle;
- both create bounded and idempotent DIALOGUE events;
- NPC memory remains isolated;
- legacy and Memory 2.0 persistence survive restart;
- Voice/STT/TTS behavior remains unchanged.

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.10.md
```

---

## 0.1.9-era checkpoint

Voice dialogue created Memory 2.0 DIALOGUE events while ordinary text still wrote only to legacy memory. PR #43 removed this asymmetry.

---

## 0.1.8+1.21.1 — Reliability foundation

Established:

- provider response hardening;
- safe `content:null` handling;
- diagnostics;
- admission/backpressure;
- persistent JSON recovery;
- stable voice and memory persistence.

```text
release commit:
23fba1ee373e932c0b17aba3755f8ac478c26941
workflow:
29918008438 → SUCCESS
```
