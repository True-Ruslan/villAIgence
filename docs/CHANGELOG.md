# VillAIgence Changelog

> Human-readable development and validation history. For the exact current implementation state and next priority, read `docs/PROJECT_STATE.md`. For long-term direction, read `docs/ROADMAP.md`.

## Post-0.1.10 — Working Memory + Semantic Memory foundation

**Status:** merged and automated-CI validated in PR #46; dedicated real-server validation is still pending.

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

Working Memory now provides hard turn-local bounds for persistent dialogue and long-term context:

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

Semantic storage/retrieval now supports:

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
- LLM prose cannot silently become a `FACT`;
- `semantic-memory.json` may legitimately remain absent/empty until controlled producers are implemented;
- embeddings/vector DB, decay, consolidation, legacy migration and rumor propagation remain future work.

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

The TDD RED contract was previously confirmed by `VillAIgence CI #651 / 29949058071`, which failed because the new production semantic/working-memory types did not yet exist.

### Development consequence

The immediate next checkpoint is **real-server validation of PR #46**. After that, the next implementation slice should add controlled provenance-preserving Semantic Memory ingestion, beginning with server-owned evidence for `FACT`, before duplicate/consolidation and forgetting/decay.

---

## 0.1.10+1.21.1 — Memory 2.0 text/voice parity checkpoint

**Status:** live-tested successfully on a real server after restart.

### What changed

- ordinary OpenAI text dialogue and snapshot/voice dialogue now share one post-success Memory 2.0 ingestion lifecycle;
- both successful text and voice turns create bounded/idempotent `DIALOGUE` MemoryEvents;
- NPC memory remains isolated by NPC UUID;
- classic text provider/prompt/tools/relationship semantics were not rerouted through the snapshot-aware provider path;
- voice/STT/TTS behavior was intentionally left unchanged;
- legacy `memory.json` remains active alongside `memory2.json`.

### Live validation evidence

The release was tested with the intended parity scenario:

```text
Text  → NPC A → DIALOGUE Memory 2.0 event
Voice → NPC A → DIALOGUE Memory 2.0 event
Text  → NPC B → separate NPC-owned Memory 2.0 event
Restart server
Return to NPC A
```

Observed result:

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

This closes the text/voice Memory 2.0 ingestion parity regression discovered during the preceding `0.1.9`-era live test.

### Architecture preserved

```text
classic OpenAI text ─┐
                      ├→ Memory2DialogueLifecycle
snapshot/voice ──────┘
                      → Memory2DialogueIngestor
                      → bounded/idempotent DIALOGUE MemoryEvent
```

The patch did not change:

- `OpenAIChatAI` provider/parser/retry behavior;
- STT/TTS pipeline;
- persistent schemas or config version;
- relationship/action semantics;
- Inworld/non-OpenAI classic path.

### Git/CI anchors

```text
PR #43 merge:
801a9da73438a6bc01ffd61aef179e45a18c9336

PR #43 exact final head:
cee822de059d344e27b5b3456fc5eb4c187fff78

VillAIgence CI 29938941710 → SUCCESS
Java Pull Request CI 29938941839 → SUCCESS
```

Validated CI scope included unit tests, Fabric build, distributable Fabric package verification, NeoForge build and Fabric compatibility build.

### Development consequence

`0.1.10+1.21.1` remains the latest **live-validated** checkpoint. PR #46 is newer code, but is currently only merged + automated-CI validated until a dedicated real-server test is completed.

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
