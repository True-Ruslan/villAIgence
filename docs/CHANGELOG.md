# VillAIgence Changelog

> Human-readable implementation and validation history. For exact current state and next priority, read `docs/PROJECT_STATE.md`. For long-term direction, read `docs/ROADMAP.md`.

## Post-0.1.11 — Controlled Semantic Memory ingestion

**Status:** merged and automated-CI validated in PR #49; real-server validation pending.

### What changed

The existing Semantic Memory foundation now has controlled producers.

```text
successful safe NPC action
→ ACTION MemoryEvent
→ memory2.json
→ FACT / SYSTEM_OBSERVED
→ semantic-memory.json
```

```text
persisted relationship transition
→ RELATIONSHIP_CHANGE MemoryEvent
→ memory2.json
→ FACT / SYSTEM_OBSERVED
→ semantic-memory.json
```

Added:

- `SemanticMemoryIngestionAdapter`;
- `SemanticBeliefSource`;
- `ControlledSemanticMemoryIngestor`;
- deterministic semantic IDs derived from source evidence;
- normalized semantic statements capped at 240 Unicode code points;
- source-event linkage through `sourceEventIds`;
- action and relationship semantic FACT producers;
- explicit provenance-safe BELIEF API for future controlled producers.

### Authority boundary

Automatic FACT conversion accepts only:

```text
provenance = SYSTEM_OBSERVED
AND type in {ACTION, OBSERVATION, RELATIONSHIP_CHANGE}
```

Automatic conversion rejects:

```text
DIALOGUE
PLAYER_TOLD
NPC_TOLD
INFERRED
```

Ordinary dialogue remains episodic-only. No LLM extraction, truth classification, embeddings or vector database was introduced.

BELIEF creation requires:

- `PLAYER_TOLD`, `NPC_TOLD` or `INFERRED` provenance;
- an owning NPC UUID;
- a non-empty statement;
- at least one source event UUID.

The BELIEF API is not automatically wired to dialogue in this slice.

### Persistence and compatibility

The episodic MemoryEvent is written before its semantic FACT. A semantic write failure cannot roll back the already persisted episodic evidence or authoritative Minecraft mutation.

Semantic ingestion remains under existing:

```text
memory2Enabled
memory2MaxEventsPerNpc
```

No new configuration keys, persistent-format versions or provider dependencies were added.

### TDD and CI anchors

```text
PR #49:
https://github.com/True-Ruslan/villAIgence/pull/49

RED head:
f47369f624263884fe906ee91ee31ac91c1e2696
VillAIgence CI #701 / 30547268997 → expected FAILURE
Reason: controlled-ingestion production API did not yet exist

final exact feature head:
7f9916e510a2ca70245d93c5b308ee31758fed0f

VillAIgence CI #721 / 30548196801 → SUCCESS
Java Pull Request CI #289 / 30548198746 → SUCCESS

merge:
c6a7a17aa5bd7806667ff3b8b502b852640e606c
```

### Validation boundary

This implementation has not yet been tested on a real server. The next checkpoint must verify:

- ACTION and RELATIONSHIP_CHANGE produce linked semantic FACT entries;
- FACT provenance and NPC ownership are correct;
- replay/retry creates no duplicate semantic UUIDs;
- text/voice DIALOGUE alone creates no semantic entry;
- semantic and episodic files survive restart;
- voice/STT/TTS and server operations remain unchanged.

`0.1.11+1.21.1` therefore remains the latest live-validated release.

---

## 0.1.11+1.21.1 — Working Memory live-server checkpoint

**Status:** live-tested successfully on a real Minecraft 1.21.1 server after restart on 2026-07-30.

### Live evidence

```text
NPC A / NPC B UUID isolation                       PASS
NPC-owned memory isolation                         PASS
Post-restart routing to NPC A                      PASS

memory.json total messages: 86                     OBSERVED
NPC A durable rolling history: 16                  PASS
NPC A prompt history: 12                           PASS
Dialogue continuity after prompt bound             PASS

memory2.json events: 21                            OBSERVED
UUID duplicates: 0                                 PASS
Logical fingerprint duplicates: 0                  PASS
Full episodic history retained                     PASS

semantic-memory.json absent                        EXPECTED / PASS

Three independent voice turns                      PASS
Simple Voice Chat / Opus                           PASS
STT/TTS errors: none                               PASS
Separate and preserved voice profiles              PASS

One OpenRouter retry recovered                     PASS
Fallback required: no                              PASS

Memory files byte-identical after restart          PASS
Server / monitor / required ports                  PASS
```

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.11.md
```

This checkpoint live-validates the Working Memory and Semantic Memory foundation from PR #46, but predates the semantic producers introduced in PR #49.

### Release metadata note

A later attempted `0.1.12+1.21.1` release run `30540119567` failed before Gradle because `0.1.11+1.21.1` and `0.1.12+1.21.1` pointed at the same commit. `0.1.12` is not a successful release checkpoint.

---

## Post-0.1.10 — Working Memory + Semantic Memory foundation

**Status:** merged in PR #46, automated-CI validated, and later live-validated by `0.1.11+1.21.1`.

PR #46 introduced:

```text
Recent dialogue
→ bounded Working Memory

MemoryEvent experiences
→ Episodic Memory

Typed knowledge
→ Semantic Memory
   ├── FACT
   └── BELIEF
```

Working Memory bounds:

```text
recent persistent dialogue messages = 12
max dialogue message = 1200 Unicode code points
episodic entries = 6
semantic entries = 6
```

Semantic foundation:

- typed `FACT` and `BELIEF` entries;
- per-NPC bounded persistence;
- deterministic retrieval;
- UUID idempotency;
- atomic writes;
- fail-open malformed-file recovery;
- explicit prompt truth and instruction boundaries.

Truth invariant:

```text
FACT   → SYSTEM_OBSERVED only
BELIEF → PLAYER_TOLD / NPC_TOLD / INFERRED only
```

CI anchors:

```text
PR #46 merge:
f82248ac79734200add0652fca663b93a71f2f18

exact verified head:
f8338bcf5371f062a31b6a50c8dbc4d992251bda

VillAIgence CI #687 / 29950014730 → SUCCESS
Java Pull Request CI #276 / 29950015077 → SUCCESS
```

---

## 0.1.10+1.21.1 — Memory 2.0 text/voice parity checkpoint

**Status:** live-tested successfully.

Implemented and validated:

- ordinary text and snapshot/voice dialogue share one post-success Memory 2.0 ingestion lifecycle;
- both paths create bounded/idempotent `DIALOGUE` MemoryEvents;
- NPC memory remains isolated by NPC UUID;
- legacy `memory.json` remains active alongside `memory2.json`;
- restart preserves NPC identity and memory;
- Voice/STT/TTS behavior remains unchanged.

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.10.md
```

Git/CI anchors:

```text
PR #43 merge:
801a9da73438a6bc01ffd61aef179e45a18c9336

exact final head:
cee822de059d344e27b5b3456fc5eb4c187fff78

VillAIgence CI 29938941710 → SUCCESS
Java Pull Request CI 29938941839 → SUCCESS
```

---

## 0.1.9-era Memory 2.0 checkpoint

A real-server test confirmed:

- voice STT → Chat → TTS completed;
- legacy memory and voice profiles survived restart;
- `memory2.json` persisted;
- snapshot/voice dialogue created a DIALOGUE MemoryEvent;
- ordinary text dialogue still wrote only to `memory.json`.

That asymmetry was fixed by PR #43 and validated in `0.1.10+1.21.1`.

---

## 0.1.8+1.21.1 — Reliability foundation

```text
release commit:
23fba1ee373e932c0b17aba3755f8ac478c26941

workflow:
29918008438 → SUCCESS
```

This checkpoint established:

- provider response hardening;
- safe `content:null` handling;
- diagnostics;
- admission/backpressure;
- persistent auxiliary JSON recovery;
- stable voice and memory persistence foundations.
