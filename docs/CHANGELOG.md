# VillAIgence Changelog

> Human-readable development and validation history. For the exact current implementation state and next priority, read `docs/PROJECT_STATE.md`. For long-term direction, read `docs/ROADMAP.md`.

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

`0.1.10+1.21.1` is the validated checkpoint before the next Memory 2.0 architecture slice.

Next development priority:

```text
Working Memory orchestration
+ explicit Semantic Facts / Beliefs boundaries
```

Do not add embeddings/vector search or LLM-driven consolidation as prerequisites for that slice.

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
