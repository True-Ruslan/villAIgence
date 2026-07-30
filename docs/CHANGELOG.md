# VillAIgence Changelog

> Human-readable implementation and validation history. For exact current state and next priority, read `docs/PROJECT_STATE.md`. For long-term direction, read `docs/ROADMAP.md`.

## Post-0.1.12 — Deterministic Semantic Memory consolidation

**Status:** merged and automated-CI validated in PR #53; real-server validation pending.

### What changed

Added `SemanticMemoryConsolidator` and integrated it into `SemanticMemoryStore`.

```text
same NPC
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

### Replay and corroboration

Exact semantic UUID replay remains a no-op and does not rewrite `semantic-memory.json`.

Different entries with independent source events and the same consolidation key become one entry containing every source UUID exactly once.

A single sourced entry keeps its existing UUID until corroborating evidence appears.

### Authority boundary

Consolidation never merges:

- different NPC owners;
- FACT with BELIEF;
- BELIEF entries with different provenance;
- identical text about different related entities;
- unsourced entries;
- text that is only approximately or semantically similar.

Confidence is not automatically increased. BELIEF is never promoted to FACT.

No provider call, LLM comparison, embedding, vector database, stemming or synonym matching was introduced.

### Persistence and retention

Consolidation runs before per-NPC retention trimming, so repeated evidence consumes one retention slot.

Existing compatible entries are consolidated in memory during load. Loading alone does not rewrite the world file; the compacted representation is persisted on the next normal semantic append.

The JSON schema and format version remain unchanged.

### TDD and CI anchors

```text
PR #53:
https://github.com/True-Ruslan/villAIgence/pull/53

RED head:
ad393c71d1f414eff0a0c65cb5f326700617ca0d
VillAIgence CI #732 / 30559795912 → expected FAILURE
reason: SemanticMemoryConsolidator did not yet exist

final exact feature head:
19c3d3e840431cc2b1b34e1841e2075f56e99f71

VillAIgence CI #746 / 30561015885 → SUCCESS
Java Pull Request CI #300 / 30561015985 → SUCCESS

merge:
f85879d254f37d7f860380362b296e047bbbb781
```

### Documentation

```text
docs/livingworld/SEMANTIC_CONSOLIDATION.md
docs/superpowers/specs/2026-07-30-memory2-semantic-consolidation-design.md
```

### Validation boundary

A live checkpoint must still prove:

- two distinct real events consolidate into one semantic entry;
- both source UUIDs survive exactly once;
- the consolidation UUID is stable across restart;
- different related entities remain separate;
- replay creates no rewrite or duplicate;
- Chat and voice behavior remain unchanged.

`0.1.12+1.21.1` remains the latest live-validated release.

---

## 0.1.12+1.21.1 — Controlled Semantic Memory live-server checkpoint

**Status:** live-tested successfully on a real Minecraft 1.21.1 server after restart on 2026-07-30.

```text
release/tag and tested commit:
746fa75ab4b5f4bee385efa0c8ae51009c1aec58
```

### Live evidence

```text
Successful NPC actions: 2                                PASS
ACTION MemoryEvents: 2                                   PASS
Relationship transition: trust +1, affinity +1           PASS
RELATIONSHIP_CHANGE MemoryEvents: 1                       PASS
Semantic FACT entries: 3                                 PASS
```

Every FACT had:

```text
kind = FACT                                               PASS
provenance = SYSTEM_OBSERVED                              PASS
ownerNpcId correct                                       PASS
matching episodic UUID in sourceEventIds                 PASS
deterministic semantic UUID correct                      PASS
```

Authority and retry evidence:

```text
Semantic UUID duplicates: 0                              PASS
Retry-created semantic duplicates: 0                     PASS
Ordinary DIALOGUE interactions: 9                        OBSERVED
DIALOGUE-derived semantic entries: 0                     PASS
```

Byte-identical after restart:

```text
memory.json                                               PASS
memory2.json                                              PASS
semantic-memory.json                                      PASS
relationships.json                                        PASS
voices.json                                               PASS
```

Operations:

```text
Chat                                                      SUCCESS
STT                                                       SUCCESS / 1059 ms
TTS                                                       SUCCESS / 1663 ms
Simple Voice Chat / Opus                                  PASS
Monitor                                                   PASS
UDP 24454 / 25565                                         PASS
VillAIgence or persistence errors                         none
```

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.12.md
```

`0.1.12+1.21.1` superseded `0.1.11+1.21.1` as the latest live-server checkpoint.

---

## Post-0.1.11 — Controlled Semantic Memory ingestion

**Status:** merged in PR #49, automated-CI validated, and live-validated by `0.1.12+1.21.1`.

Implemented:

```text
successful safe action
→ ACTION / SYSTEM_OBSERVED
→ semantic FACT

persisted relationship transition
→ RELATIONSHIP_CHANGE / SYSTEM_OBSERVED
→ semantic FACT
```

Added:

- `SemanticMemoryIngestionAdapter`;
- `SemanticBeliefSource`;
- `ControlledSemanticMemoryIngestor`;
- deterministic semantic IDs from source evidence;
- source-event linkage;
- explicit sourced BELIEF API foundation.

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

**Status:** live-tested successfully on 2026-07-30.

Validated:

```text
NPC A / NPC B isolation                                  PASS
memory.json total messages: 86                           OBSERVED
NPC A durable history: 16                                PASS
NPC A prompt history: 12                                 PASS
memory2.json events: 21                                  OBSERVED
UUID duplicates: 0                                       PASS
logical fingerprint duplicates: 0                        PASS
three independent voice turns                            PASS
OpenRouter retry recovery                                PASS
restart persistence                                      PASS
server / monitor / ports                                 PASS
```

At this checkpoint `semantic-memory.json` was absent, which was expected before PR #49.

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.11.md
```

---

## Post-0.1.10 — Working Memory and Semantic Memory foundation

**Status:** PR #46 merged, CI validated, later live-validated by `0.1.11` and `0.1.12`.

Working Memory introduced bounded turn-local composition:

```text
recent dialogue = 12
episodic context = 6
semantic context = 6
```

Semantic foundation introduced:

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

- text and snapshot/voice dialogue share one post-success ingestion lifecycle;
- both create bounded/idempotent DIALOGUE events;
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
