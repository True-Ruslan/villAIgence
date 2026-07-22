# VillAIgence 0.1.10+1.21.1 Live Validation

Date: 2026-07-22

Status: **PASS — intended text/voice Memory 2.0 parity scenario completed successfully.**

This document records user-supplied real-server validation evidence for the `0.1.10+1.21.1` checkpoint. It is operational evidence, not a substitute for automated CI.

## Purpose

Validate the regression fixed by PR #43:

```text
ordinary text dialogue
and
snapshot/voice dialogue
```

must both persist into Memory 2.0 without duplicate IDs, NPC memory mixing, restart loss, or voice-pipeline regression.

## Scenario

```text
1. Text → NPC A
2. Voice → NPC A
3. Text → NPC B
4. Inspect Memory 2.0 ownership/events
5. Restart server
6. Return to NPC A
7. Verify persistence, identity, duplicate safety and service health
```

## Results

| Check | Result |
|---|---|
| Text → NPC A creates `DIALOGUE` | PASS |
| Voice → NPC A creates `DIALOGUE` | PASS |
| Text → NPC B uses separate NPC memory | PASS |
| NPC A is recognized correctly after restart | PASS |
| Duplicate MemoryEvent IDs | NONE FOUND |
| `memory.json` persistence | PASS — 64 dialogue entries |
| `memory2.json` persistence | PASS — 5 events |
| NPC A Memory 2.0 ownership | PASS — 3 events |
| NPC B Memory 2.0 ownership | PASS — 1 event |
| Voice/STT/TTS pipeline regression | NONE OBSERVED |
| Server health | PASS |
| Monitor health | PASS |
| Required ports | PASS |

## Interpretation

The live test confirms the intended PR #43 behavior:

```text
classic OpenAI text ─┐
                      ├→ shared Memory2DialogueLifecycle
snapshot/voice ──────┘
                      → bounded/idempotent DIALOGUE MemoryEvent
```

The previously observed `0.1.9`-era asymmetry is closed for the tested scenario:

```text
before:
text  → legacy memory only
voice → legacy + Memory 2.0

after 0.1.10 validation:
text  → legacy + Memory 2.0
voice → legacy + Memory 2.0
```

The test also confirms:

- Memory 2.0 remains isolated by NPC identity in the tested two-NPC scenario;
- event replay/idempotency protection produced no duplicate IDs in the observed store;
- legacy `memory.json` and `memory2.json` survive restart;
- returning to NPC A after restart resolves the expected NPC correctly;
- the voice/STT/TTS pipeline remained operational after the parity fix.

## What this test does not prove

This checkpoint does not by itself prove:

- long-duration multiplayer concurrency/soak behavior;
- every provider failure/rate-limit path;
- backup/restore across arbitrary world copies;
- long-horizon Memory 2.0 recall quality over many Minecraft days;
- future Working Memory or Semantic Facts/Beliefs behavior, which is not implemented yet.

Those remain separate validation concerns and should not be inferred from this PASS.

## Git/CI anchors

PR #43:

```text
merge commit:
801a9da73438a6bc01ffd61aef179e45a18c9336

exact final head:
cee822de059d344e27b5b3456fc5eb4c187fff78

VillAIgence CI:
29938941710 — SUCCESS

Java Pull Request CI with Gradle:
29938941839 — SUCCESS
```

Automated gates covered:

- unit tests;
- Fabric build;
- distributable VillAIgence Fabric package verification;
- NeoForge build;
- Fabric compatibility build.

## Release/development decision

Treat `0.1.10+1.21.1` as the validated checkpoint for the implemented Memory 2.0 ingestion foundation.

Unless a concrete blocking reliability regression appears, development can proceed to:

```text
Working Memory orchestration
+ explicit Semantic Facts / Beliefs boundaries
```

Do not make embeddings, vector search, or LLM-driven consolidation prerequisites for that next slice.
