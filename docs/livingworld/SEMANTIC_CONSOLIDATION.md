# Semantic Memory Consolidation

## Purpose

Semantic consolidation keeps repeated evidence from consuming one retention slot per occurrence while preserving every source event that supports the knowledge.

```text
same typed knowledge
+ different authoritative source events
→ one SemanticMemoryEntry
→ all sourceEventIds preserved
```

## Consolidation key

Entries consolidate only when these fields match after deterministic normalization:

```text
ownerNpcId
kind
provenance
statement
relatedEntities
```

Statement normalization is intentionally narrow:

```text
Unicode NFKC
whitespace/control collapse
trim
Locale.ROOT lowercase
```

Related entities are compared as sorted UUID sets.

## Boundaries

The policy never merges:

- different NPC owners;
- FACT with BELIEF;
- BELIEF entries with different provenance;
- identical text about different related entities;
- entries without source event IDs;
- text that is merely similar but not canonically identical.

No provider call, LLM comparison, embedding or vector database is used.

## Replay versus corroboration

### Replay

The same semantic UUID remains an idempotent no-op. The persistent file is not rewritten.

### Independent corroboration

Different semantic UUIDs with the same consolidation key are combined. The result uses:

```text
sourceEventIds       sorted union
relatedEntities      sorted union
gameTime             maximum
createdAtEpochMillis maximum
importance           maximum
confidence           maximum
statement            deterministic representative
id                   deterministic consolidation UUID
```

Source count is evidence metadata only. Consolidation does not automatically increase confidence and never upgrades BELIEF to FACT.

## Retention and existing files

Consolidation occurs before per-NPC retention trimming. Repeated evidence therefore occupies one retention slot.

On load, compatible entries already present in `semantic-memory.json` are consolidated in memory. Loading alone does not rewrite the file. The compacted representation is persisted during the next normal semantic append.

The JSON format and format version remain unchanged.

## Live-server validation scenario

1. Start with a known `semantic-memory.json` hash and entry count.
2. Produce two distinct authoritative events that generate the same normalized statement and related-entity set.
3. Confirm `memory2.json` contains two distinct source events.
4. Confirm `semantic-memory.json` contains one consolidated semantic entry.
5. Confirm its `sourceEventIds` contains both source UUIDs exactly once.
6. Confirm its semantic UUID matches the deterministic consolidation key.
7. Replay one original event and confirm no file change or duplicate source ID.
8. Produce an otherwise identical statement for a different related entity and confirm a separate entry.
9. Confirm FACT and BELIEF or different BELIEF provenance remain separate.
10. Restart and confirm byte-stable persistence, retrieval, Chat, STT, TTS and voice behavior.
