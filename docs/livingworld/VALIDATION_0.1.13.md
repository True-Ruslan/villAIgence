# VillAIgence 0.1.13+1.21.1 Live-Server Validation

**Validation date:** 2026-07-30  
**Result:** PASS  
**Checkpoint status:** confirmed live-server checkpoint for deterministic Semantic Memory consolidation

## Purpose

Validate the deterministic Semantic Memory consolidation introduced by PR #53 on a real Minecraft 1.21.1 server while preserving NPC isolation, restart-safe persistence, dialogue, voice and operational behavior.

## Release identity

```text
VillAIgence: 0.1.13+1.21.1
Minecraft: 1.21.1
release/tag and tested commit:
b553bf7e83674145bdf42927b9ace7287afa560c
```

Before this documentation update, GitHub comparison confirmed that `0.1.13+1.21.1` and branch `1.21.1` were identical at commit `b553bf7e83674145bdf42927b9ace7287afa560c`.

## Executed scenario

```text
1. Install and start 0.1.13+1.21.1.
2. Produce two authoritative ACTION events representing the same typed knowledge.
3. Confirm the ACTION events have distinct UUIDs.
4. Inspect semantic-memory.json.
5. Verify that one consolidated FACT remains.
6. Verify both source event UUIDs occur exactly once in sourceEventIds.
7. Independently calculate and compare the deterministic consolidation UUID.
8. Retry the operation and verify no new ACTION or semantic-file change.
9. Produce a separate action and FACT for NPC B.
10. Verify owner and related-entity isolation between NPC A and NPC B.
11. Record hashes for all relevant persistence files.
12. Restart the server and verify hashes, version, Chat, voice and operations.
```

## Results

### Distinct authoritative evidence

```text
Authoritative ACTION events: 2                            PASS
ACTION UUIDs distinct                                     PASS
```

Two independent server-owned source events therefore existed before semantic consolidation.

### Consolidated Semantic Memory

```text
Semantic entries for the repeated NPC A knowledge: 1      PASS
Both sourceEventIds present exactly once                  PASS
```

The persisted semantic entry preserved both pieces of source evidence without consuming two semantic retention slots.

### Deterministic UUID

```text
Observed semantic UUID:
093aabb0-e61b-3e62-a5fe-fbb9d15b8494

Independently calculated UUID:
093aabb0-e61b-3e62-a5fe-fbb9d15b8494

Result:                                                    PASS
```

This confirms the provider-independent deterministic consolidation identifier on a real server.

### Retry safety

```text
Retry created a new ACTION event: no                      PASS
Retry changed semantic-memory.json: no                    PASS
Retry introduced duplicate sourceEventIds: no             PASS
```

The retry path remained a persistence no-op.

### NPC and related-entity isolation

NPC B produced a separate authoritative ACTION and semantic FACT.

```text
NPC B ownerNpcId distinct                                 PASS
NPC B relatedEntities set distinct                        PASS
NPC A and NPC B semantic data mixed: no                   PASS
```

Identical or related action text did not bypass the owner and related-entity consolidation boundaries.

### Restart persistence

The following files were byte-identical before and after restart:

```text
memory.json                                               PASS
memory2.json                                              PASS
semantic-memory.json                                      PASS
relationships.json                                        PASS
voices.json                                               PASS
```

The server loaded VillAIgence `0.1.13+1.21.1` again successfully after restart.

### Chat and voice

```text
Chat                                                      PASS
DIALOGUE events created                                   PASS
Simple Voice Chat connected                               PASS
Opus initialized                                          PASS
STT configured and operational                            PASS
TTS configured and operational                            PASS
Voice dialogue completed                                  PASS
```

Semantic consolidation introduced no observed regression in text or voice dialogue.

### Operational health

```text
UDP 24454 listening                                       PASS
UDP 25565 listening                                       PASS
LinuxGSM monitor restored                                 PASS
Server state STARTED                                      PASS
VillAIgence errors                                        none
Memory persistence errors                                 none
OutOfMemory errors                                        none
```

## Final verdict

`0.1.13+1.21.1` fully passed the intended live-server validation for the primary deterministic Semantic Memory consolidation path.

The checkpoint establishes live evidence for:

- distinct authoritative source events;
- one consolidated semantic entry;
- exact preservation of all source event UUIDs;
- independently verified deterministic consolidation UUID;
- retry no-op behavior;
- owner and related-entity isolation;
- byte-stable persistence across restart;
- unchanged Chat, STT, TTS, Voice Chat and Opus behavior;
- healthy monitor, ports and server lifecycle.

`0.1.13+1.21.1` supersedes `0.1.12+1.21.1` as the latest confirmed live-server checkpoint.

## Validation boundary

This live test did not separately seed a pre-existing legacy `semantic-memory.json` containing compatible logical duplicates before startup. Load-time in-memory consolidation of such a prepared historical file remains unit-tested but was not independently exercised in this server scenario.

This checkpoint also does not validate:

- forgetting/decay;
- automatic controlled BELIEF producers;
- contradiction resolution;
- legacy `memory.json` migration;
- NPC-to-NPC knowledge or rumor propagation;
- multi-day or large-multiplayer soak behavior;
- long-horizon recall after days without full raw dialogue.

## Client bundle note

The operator reported the client bundle filename:

```text
villaigence-client-0.1.13+1.21.1.zip
```

It remains a local operator artifact outside the repository. Its absolute workstation path is intentionally not treated as canonical GitHub evidence and the archive was not independently inspected during this documentation update.

## Development consequence

The next implementation slice is explicit Semantic Memory forgetting/decay with deterministic retention rules.
