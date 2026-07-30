# VillAIgence 0.1.12+1.21.1 Live-Server Validation

**Validation date:** 2026-07-30  
**Result:** PASS  
**Checkpoint status:** confirmed live-server checkpoint for controlled Semantic Memory ingestion

## Purpose

Validate the controlled Semantic Memory producers introduced by PR #49 on a real Minecraft 1.21.1 server while preserving the existing dialogue, voice, persistence and operational behavior.

The scenario covered:

- successful authoritative NPC actions;
- a real persisted relationship transition;
- ACTION and RELATIONSHIP_CHANGE episodic events;
- linked semantic FACT creation;
- deterministic semantic UUIDs and retry idempotency;
- exclusion of ordinary DIALOGUE from automatic semantic ingestion;
- restart persistence across all relevant world-local files;
- Chat, STT, TTS, Simple Voice Chat and Opus;
- monitor and network-port health.

## Release identity

```text
VillAIgence: 0.1.12+1.21.1
Minecraft: 1.21.1
release/tag commit: 746fa75ab4b5f4bee385efa0c8ae51009c1aec58
```

The `0.1.12+1.21.1` tag points at the current `1.21.1` head. It is distinct from `0.1.11+1.21.1`, resolving the earlier ambiguous-tag attempt.

## Executed scenario

```text
1. Start the server with 0.1.12+1.21.1.
2. Execute two successful allowed NPC actions.
3. Produce one real persisted relationship change.
4. Inspect memory2.json.
5. Inspect semantic-memory.json.
6. Verify FACT kind, provenance, owner and source linkage.
7. Verify deterministic semantic UUIDs and retry idempotency.
8. Perform nine ordinary DIALOGUE interactions.
9. Confirm DIALOGUE events do not create semantic entries.
10. Record hashes for all relevant persistence files.
11. Restart the server.
12. Recheck files, Chat, voice pipeline, monitor, ports and logs.
```

## Results

### Authoritative episodic events

```text
Successful NPC actions: 2                                PASS
ACTION MemoryEvents created: 2                            PASS
Persisted relationship transition: trust +1, affinity +1 PASS
RELATIONSHIP_CHANGE MemoryEvents created: 1               PASS
```

The relationship FACT records the actual persisted numeric transition rather than an untrusted free-form psychological explanation.

### Semantic FACT production

```text
Semantic FACT entries created: 3                          PASS
FACT entries linked to ACTION events: 2                   PASS
FACT entries linked to RELATIONSHIP_CHANGE events: 1      PASS
```

Every semantic entry satisfied:

```text
kind = FACT                                                PASS
provenance = SYSTEM_OBSERVED                               PASS
ownerNpcId = expected NPC UUID                             PASS
sourceEventIds contains matching memory2 event UUID        PASS
deterministic semantic UUID is correct                     PASS
```

The observed production flow matched the intended architecture:

```text
server-confirmed action or relationship transition
→ SYSTEM_OBSERVED MemoryEvent
→ memory2.json
→ controlled SemanticMemoryIngestionAdapter
→ FACT
→ semantic-memory.json
```

### Duplicate and retry safety

```text
Semantic UUIDs unique                                      PASS
Retry created no duplicate semantic entry                  PASS
Source-event linkage remained stable                       PASS
```

The same source evidence therefore remained idempotent across replay/retry.

### Dialogue exclusion

```text
Ordinary DIALOGUE interactions: 9                          OBSERVED
DIALOGUE-derived semantic entries: 0                       PASS
```

This confirms the critical authority boundary:

```text
DIALOGUE
→ episodic memory only
→ no automatic semantic FACT or BELIEF
```

No arbitrary player/NPC prose was promoted into authoritative knowledge.

### Restart and persistence

The following files matched byte-for-byte before and after restart:

```text
memory.json                                                PASS
memory2.json                                               PASS
semantic-memory.json                                       PASS
relationships.json                                         PASS
voices.json                                                PASS
```

This proves restart-safe persistence for the new semantic layer together with the existing dialogue, episodic, relationship and voice stores.

### Chat and voice pipeline

```text
Chat                                                       SUCCESS
STT                                                        SUCCESS / 1059 ms
TTS                                                        SUCCESS / 1663 ms
Simple Voice Chat connected                                PASS
Opus operational                                           PASS
```

Controlled semantic ingestion introduced no regression in Chat, STT, TTS, Voice Chat or Opus.

### Operational health

```text
Monitor restored and running                               PASS
UDP 24454 listening                                        PASS
UDP 25565 listening                                        PASS
VillAIgence errors after startup: none                     PASS
Memory/persistence errors after startup: none              PASS
```

## Final verdict

`0.1.12+1.21.1` fully passed the intended controlled Semantic Memory live-server validation.

The checkpoint establishes live evidence for:

- ACTION → semantic FACT;
- RELATIONSHIP_CHANGE → semantic FACT;
- `SYSTEM_OBSERVED` truth provenance;
- correct per-NPC ownership;
- source-event linkage;
- deterministic semantic UUIDs;
- retry/replay idempotency;
- DIALOGUE exclusion;
- byte-stable semantic persistence across restart;
- unchanged Chat/STT/TTS and voice behavior;
- healthy monitor, ports and server logs.

`0.1.12+1.21.1` supersedes `0.1.11+1.21.1` as the latest confirmed live-server checkpoint.

## What this validation does not prove

This checkpoint does not validate:

- automatic BELIEF producers from told or inferred sources;
- semantic consolidation across distinct but logically equivalent source events;
- forgetting/decay;
- migration from legacy `memory.json`;
- NPC-to-NPC knowledge or rumor propagation;
- multi-day or large-multiplayer soak behavior;
- long-horizon recall after days without full raw dialogue.

## Development consequence

The next implementation slice is deterministic Semantic Memory duplicate/consolidation policy.

The policy must:

- preserve provenance and sourceEventIds;
- never merge FACT and BELIEF into one authoritative entry;
- remain deterministic and provider-independent;
- distinguish replay duplicates from distinct corroborating evidence;
- avoid embeddings, vector databases and LLM truth classification.

After consolidation, the next planned slice is explicit forgetting/decay with tested retention rules.
