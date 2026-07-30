# VillAIgence 0.1.11+1.21.1 Live-Server Validation

**Validation date:** 2026-07-30  
**Result:** PASS  
**Checkpoint status:** confirmed live-server checkpoint

## Purpose

Validate the merged Working Memory and Semantic Memory foundation on a real Minecraft 1.21.1 server without claiming semantic ingestion that is not implemented yet.

The scenario checked:

- repeated text dialogue beyond Working Memory bounds;
- NPC A / NPC B identity and memory isolation;
- Memory 2.0 completeness and idempotency;
- voice input/output and Opus transport;
- OpenRouter retry recovery;
- restart persistence and byte-level stability;
- expected behavior when `semantic-memory.json` has no producers;
- server, monitor and required network ports.

## Environment

```text
VillAIgence: 0.1.11+1.21.1
Minecraft: 1.21.1
Voice transport: Simple Voice Chat / Opus
Server ports checked:
- 25565/UDP
- 24454/UDP
```

## Executed scenario

```text
1. Run sequential text dialogues with NPC A.
2. Continue beyond the Working Memory prompt-history bound.
3. Interact independently with NPC B.
4. Inspect memory.json and memory2.json.
5. Verify UUID and logical-fingerprint uniqueness.
6. Run three independent voice turns.
7. Verify separate voice profiles for NPC A and NPC B.
8. Record persistence-file hashes.
9. Restart the server.
10. Recheck files, NPC identity, dialogue routing, voice pipeline, monitor and ports.
```

## Results

### NPC identity and isolation

```text
NPC A and NPC B have different UUIDs                         PASS
NPC-owned memory does not mix                                PASS
Post-restart dialogue is recorded under NPC A                PASS
NPC B input remains logically distinct                       PASS
Player-message hashes for A/B interactions differ            PASS
```

### Working Memory and legacy dialogue storage

```text
Sequential NPC A dialogue remains usable after bounds        PASS
memory.json total messages: 86                               OBSERVED
NPC A persisted rolling history: latest 16 messages          PASS
NPC A prompt history: latest 12 messages                     PASS
```

The distinction is expected:

```text
memory.json
→ bounded durable rolling dialogue history
→ latest 16 messages retained for the tested NPC conversation

Working Memory prompt
→ latest 12 messages selected for the AI turn
```

Conversation continuity remained correct after the prompt bound was exceeded.

### Episodic Memory 2.0

```text
memory2.json total events: 21                                OBSERVED
UUID duplicates: 0                                           PASS
Logical fingerprint duplicates: 0                            PASS
Full episodic history retained                               PASS
NPC ownership/isolation preserved                            PASS
```

Working Memory truncation did not truncate the durable Memory 2.0 event history.

### Semantic Memory boundary

```text
semantic-memory.json absent                                  EXPECTED / PASS
```

This is valid for `0.1.11+1.21.1` because the release contains the typed Semantic Memory storage/retrieval foundation but no automatic semantic producers or LLM semantic extraction.

Absence of the file did not affect text dialogue, voice dialogue, restart recovery or server health.

### Voice pipeline

```text
Three independent voice turns completed                     PASS
Each voice turn produced a separate NPC reply                PASS
Simple Voice Chat operational                                PASS
Opus operational                                             PASS
STT errors: none                                             PASS
TTS errors: none                                             PASS
NPC A voice profile created separately                       PASS
NPC B voice profile created separately                       PASS
Existing voice profiles preserved                            PASS
```

### Provider recovery

```text
One OpenRouter retry occurred before restart                 OBSERVED
Retry recovered successfully                                 PASS
Fallback response required: no                               PASS
```

The transient provider anomaly did not cause a failed dialogue, duplicate memory side effect or server failure.

### Restart and persistence

```text
Memory files matched byte-for-byte before/after restart      PASS
NPC identity remained stable                                 PASS
No VillAIgence persistence errors after restart              PASS
No memory errors after restart                               PASS
No voice-pipeline errors after restart                       PASS
```

### Operational health

```text
Minecraft server running                                     PASS
VillAIgence 0.1.11 running                                   PASS
Monitor running                                              PASS
25565/UDP available                                          PASS
24454/UDP available                                          PASS
```

## Final verdict

`0.1.11+1.21.1` fully passed the intended live-server validation.

The test establishes live evidence for:

- Working Memory bounds without dialogue-continuity regression;
- separation between bounded prompt history and complete episodic Memory 2.0 history;
- NPC A / NPC B identity and memory isolation;
- duplicate-safe Memory 2.0 ingestion;
- safe absence of `semantic-memory.json` before controlled producers exist;
- independent voice turns, Opus, STT and TTS;
- successful bounded provider retry;
- restart-safe byte-stable persistence;
- healthy server, monitor and required ports.

This release supersedes `0.1.10+1.21.1` as the latest confirmed live-server checkpoint.

## What this validation does not prove

This checkpoint does not validate:

- semantic FACT/BELIEF ingestion, because no automatic semantic producers exist yet;
- semantic consolidation or duplicate merging;
- forgetting/decay;
- legacy `memory.json` migration;
- NPC-to-NPC knowledge propagation;
- multi-day or large-multiplayer soak behavior;
- long-horizon semantic recall after controlled producers are implemented.

## Development consequence

The next implementation slice may proceed to controlled, provenance-preserving Semantic Memory ingestion:

```text
server-owned evidence
→ FACT / SYSTEM_OBSERVED

explicit told or inferred source
→ BELIEF / preserved provenance
```

Arbitrary LLM prose must not be promoted into an authoritative FACT.
