# VillAIgence Changelog

> Human-readable development and validation history. For exact current implementation state and next priority, read `docs/PROJECT_STATE.md`. For long-term direction, read `docs/ROADMAP.md`.

## 0.1.11+1.21.1 — Working Memory live-server checkpoint

**Status:** live-tested successfully on a real Minecraft 1.21.1 server after restart.

**Validation date:** 2026-07-30

### What this checkpoint validates

`0.1.11+1.21.1` is the first live checkpoint covering the Working Memory and Semantic Memory foundation introduced after `0.1.10`.

The test covered:

- repeated text dialogue beyond Working Memory bounds;
- NPC A / NPC B identity and memory isolation;
- distinction between durable rolling dialogue and bounded prompt history;
- complete episodic Memory 2.0 retention;
- UUID and logical duplicate safety;
- voice dialogue, Simple Voice Chat and Opus;
- bounded OpenRouter retry recovery;
- restart-safe byte-stable persistence;
- expected absence of `semantic-memory.json` before semantic producers exist;
- server, monitor and required ports.

### Observed results

```text
NPC A / NPC B UUID isolation                            PASS
NPC-owned memory isolation                              PASS
Post-restart routing to NPC A                           PASS

memory.json total messages: 86                          OBSERVED
NPC A durable rolling history: latest 16                PASS
NPC A prompt history: latest 12                         PASS
Dialogue continuity after prompt bound                  PASS

memory2.json events: 21                                 OBSERVED
UUID duplicates: 0                                      PASS
Logical fingerprint duplicates: 0                       PASS
Full episodic history retained                          PASS

semantic-memory.json absent                             EXPECTED / PASS

Three independent voice turns                           PASS
Simple Voice Chat / Opus                                PASS
STT errors: none                                        PASS
TTS errors: none                                        PASS
Separate NPC A / NPC B voice profiles                   PASS
Existing voice profiles preserved                       PASS

One OpenRouter retry recovered                          PASS
Fallback required: no                                   PASS

Memory files byte-identical before/after restart        PASS
Server running 0.1.11                                   PASS
Monitor running                                         PASS
25565/UDP healthy                                       PASS
24454/UDP healthy                                       PASS
```

### Working Memory evidence

The release demonstrated the intended separation:

```text
memory.json
→ durable rolling conversation history
→ latest 16 messages retained for tested NPC A conversation

Working Memory prompt
→ latest 12 messages selected for the current AI turn
```

The prompt bound did not break conversation continuity.

Working Memory truncation also did not truncate `memory2.json`; all 21 episodic events remained present and unique.

### Semantic Memory boundary

`semantic-memory.json` was absent and this was accepted as correct behavior.

The release contains the typed Semantic Memory model, storage and retrieval foundation, but does not yet contain automatic semantic producers or LLM semantic extraction. Therefore absence of the file is expected until controlled ingestion is implemented.

### Provider and voice reliability

One transient OpenRouter condition required a retry. The bounded retry recovered successfully without fallback, duplicate memory side effects or server failure.

Three voice turns each produced an independent NPC response. Voice Chat, Opus, STT and TTS remained healthy before and after restart.

### Canonical evidence

```text
docs/livingworld/VALIDATION_0.1.11.md
```

### Development consequence

`0.1.11+1.21.1` supersedes `0.1.10+1.21.1` as the latest confirmed live-server checkpoint.

The next implementation slice is controlled, provenance-preserving Semantic Memory ingestion:

```text
server-owned evidence
→ FACT / SYSTEM_OBSERVED

explicit told or inferred source
→ BELIEF / preserved provenance
```

Arbitrary LLM prose must never be promoted into an authoritative FACT.

### Release metadata note

A later attempted `0.1.12+1.21.1` release workflow run failed before Gradle because `0.1.11+1.21.1` and `0.1.12+1.21.1` both pointed at the same commit. `0.1.12` is not a successful release checkpoint and its tag must be corrected before another release attempt.

---

## Post-0.1.10 — Working Memory + Semantic Memory foundation

**Status:** merged and automated-CI validated in PR #46; subsequently live-validated by `0.1.11+1.21.1`.

### What changed

PR #46 introduced the next layered Memory 2.0 architecture slice:

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

Working Memory provides hard turn-local bounds:

```text
recent persistent dialogue messages = 12
max dialogue message = 1200 Unicode code points
episodic entries = 6
semantic entries = 6
```

A new world-local semantic foundation was added:

```text
<world>/livingworld/semantic-memory.json
```

Hard truth invariant:

```text
FACT   → SYSTEM_OBSERVED only
BELIEF → PLAYER_TOLD / NPC_TOLD / INFERRED only
```

Confidence never converts a BELIEF into a FACT.

Semantic storage/retrieval supports:

- per-NPC isolation;
- bounded retention;
- UUID idempotency;
- deterministic ordering;
- atomic persistence;
- fail-open malformed-file recovery;
- deterministic relevance/importance/confidence/recency ranking;
- explicit prompt boundaries between authoritative facts and remembered beliefs.

### Important scope boundary

PR #46 intentionally does **not** add automatic semantic producers.

Therefore:

- arbitrary dialogue is not automatically converted into semantic knowledge;
- LLM prose cannot silently become a FACT;
- `semantic-memory.json` may legitimately remain absent or empty;
- embeddings, vector DB, decay, consolidation, legacy migration and rumor propagation remain future work.

Existing provider parsing/retry, action execution, relationship persistence and post-success Memory 2.0 dialogue ingestion semantics were not changed by this slice.

### Git/CI anchors

```text
PR #46 merge:
f82248ac79734200add0652fca663b93a71f2f18

PR #46 exact verified head:
f8338bcf5371f062a31b6a50c8dbc4d992251bda

VillAIgence CI #687 / 29950014730 → SUCCESS
Java Pull Request CI #276 / 29950015077 → SUCCESS
```

The TDD RED contract was confirmed by `VillAIgence CI #651 / 29949058071`, which failed because the new production semantic and working-memory types did not yet exist.

### Development consequence

The architecture foundation is now both CI-validated and live-server validated. The next priority is controlled semantic ingestion, followed by deterministic consolidation and forgetting/decay.

---

## 0.1.10+1.21.1 — Memory 2.0 text/voice parity checkpoint

**Status:** live-tested successfully on a real server after restart.

### What changed

- ordinary OpenAI text dialogue and snapshot/voice dialogue share one post-success Memory 2.0 ingestion lifecycle;
- both successful text and voice turns create bounded/idempotent `DIALOGUE` MemoryEvents;
- NPC memory remains isolated by NPC UUID;
- classic text provider/prompt/tools/relationship semantics were not rerouted through the snapshot-aware provider path;
- voice/STT/TTS behavior was intentionally left unchanged;
- legacy `memory.json` remains active alongside `memory2.json`.

### Live validation evidence

```text
Text → NPC A: DIALOGUE                              PASS
Voice → NPC A: DIALOGUE                             PASS
Text → NPC B: separate memory                       PASS
NPC A correctly identified after restart            PASS
No duplicate MemoryEvent IDs                        PASS
memory.json persisted: 64 dialogue entries          PASS
memory2.json persisted: 5 events                    PASS
NPC A: 3 Memory 2.0 events                          PASS
NPC B: 1 Memory 2.0 event                           PASS
Voice/STT/TTS pipeline unchanged                    PASS
Server / monitor / required ports healthy           PASS
```

This closed the text/voice Memory 2.0 ingestion parity regression discovered during the preceding `0.1.9`-era live test.

### Git/CI anchors

```text
PR #43 merge:
801a9da73438a6bc01ffd61aef179e45a18c9336

PR #43 exact final head:
cee822de059d344e27b5b3456fc5eb4c187fff78

VillAIgence CI 29938941710 → SUCCESS
Java Pull Request CI 29938941839 → SUCCESS
```

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.10.md
```

`0.1.10+1.21.1` remains an important historical parity checkpoint but is superseded by the broader `0.1.11` live validation.

---

## 0.1.9-era Memory 2.0 checkpoint

A real-server test confirmed:

- voice STT → Chat → TTS completed successfully;
- legacy memory and voice profiles survived restart;
- `memory2.json` persisted correctly;
- snapshot/voice dialogue created a `DIALOGUE` MemoryEvent;
- ordinary text dialogue still wrote only to legacy `memory.json`.

That last asymmetry became the explicit defect fixed by PR #43 and validated in `0.1.10+1.21.1`.

---

## 0.1.8+1.21.1 — reliability foundation

Release anchor:

```text
commit: 23fba1ee373e932c0b17aba3755f8ac478c26941
workflow: 29918008438 — SUCCESS
```

This release established the hardened `0.1.x` foundation through PR #30, including provider response hardening, diagnostics, admission/backpressure and persistent auxiliary JSON recovery.
