# Memory 2.0 Clean Cutover Design

Date: 2026-08-07

## Context

VillAIgence is pre-1.0 and its current deployment boundary is an operator-only test environment where a clean world/server reset is an accepted rollout constraint. There is no supported external production population whose legacy conversation history must be preserved.

`0.1.26+1.21.1` still uses `<world>/livingworld/memory.json` as the persistent recent-dialogue read/write path, while successful text and voice turns are also written into `memory2.json`. Maintaining a migration/importer and dual-read period would add transitional complexity without preserving required production data.

## Decision

Perform a clean cutover: Memory 2.0 becomes the only persistent dialogue-memory source. No `memory.json` importer, migration checkpoint, dual reader, destructive converter, or rollback-to-legacy path will be built.

A clean LivingWorld state is required for installed acceptance of the cutover.

## Target flow

```text
successful text/voice turn
→ existing Memory2DialogueLifecycle
→ structured DIALOGUE MemoryEvent
→ memory2.json

next AI turn
→ Memory 2.0 dialogue-history retrieval
→ exact NPC/player filtering
→ bounded Working Memory
→ prompt messages
```

## Structured DIALOGUE contract

DIALOGUE events retain the existing human-readable `summary` for episodic retrieval and diagnostics, but also carry an optional structured dialogue payload:

```text
DialogueExchange
  playerMessage
  npcReply
```

The payload contains the same normalized, bounded utterances used by the summary. Prompt reconstruction MUST read the structured payload and MUST NOT parse the summary string.

Existing non-DIALOGUE `MemoryEvent` construction remains source-compatible through an overload that supplies no dialogue payload. Existing/old DIALOGUE records lacking the payload are ignored by the new dialogue-history reader; no migration is attempted.

## Retrieval rules

Persistent dialogue history is selected from the bounded per-NPC Memory 2.0 store with these invariants:

- exact `ownerNpcId` match;
- `type == DIALOGUE`;
- exact current player participant match;
- structured dialogue payload required;
- intervening ACTION/OBSERVATION/RELATIONSHIP_CHANGE events cannot starve dialogue retrieval;
- newest eligible exchanges are selected, then rendered chronologically as alternating `user` / `assistant` messages;
- final prompt messages pass through `WorkingMemoryOrchestrator` hard bounds.

## Configuration

`memory2Enabled` owns persistent Memory 2.0 behavior. The legacy `persistentMemoryEnabled`, `persistentMemoryMaxMessages`, and `persistentMemoryMaxCharsPerMessage` controls are retired from production configuration/runtime use. When Memory 2.0 is disabled, the existing process-local non-persistent conversation fallback may continue to serve compatibility behavior, but no persistent legacy store may be read or written.

## Legacy removal

After the production prompt path is switched:

- remove `PersistentChatMemory`;
- remove `ConversationMemoryStore` and its tests;
- remove canonical/recovery acceptance expectations for `memory.json`;
- reduce the current LivingWorld canonical store set from six to five;
- add source-policy coverage preventing reintroduction of the legacy persistent path.

Historical validation documents remain historical and are not rewritten to pretend `memory.json` never existed.

## Safety and truth boundaries

The cutover MUST preserve:

- DIALOGUE remains episodic and non-authoritative;
- dialogue never auto-promotes to FACT;
- FACT still requires `SYSTEM_OBSERVED` evidence;
- exactly-once dialogue persistence remains outside provider retry loops;
- a Memory 2.0 read failure fails soft to empty persistent dialogue context, never to another persistent format;
- provider, credential, deadline, action-authority, relationship and Operator Lore boundaries remain unchanged.

## Verification

Required automated evidence:

1. structured dialogue round-trip including delimiter-like text without summary parsing;
2. exact NPC and player isolation;
3. chronological user/assistant reconstruction;
4. non-dialogue events cannot starve recent dialogue;
5. bounded Working Memory behavior;
6. replay/idempotency unchanged;
7. text/voice shared post-success lifecycle remains single-writer;
8. no production source reads/writes `memory.json`;
9. five-store corruption/recovery and production restart acceptance;
10. Fabric + NeoForge + package + repository-security gates.

Installed acceptance is performed on a clean test world/server and is reported separately from automated evidence.
