# Memory 2.0 Deterministic Semantic Consolidation Design

## Goal

Prevent Semantic Memory from accumulating multiple entries that represent the same typed knowledge while preserving every independent source event and the FACT/BELIEF authority boundary.

## Selected policy

Two entries are consolidation-compatible only when all of the following match:

```text
ownerNpcId
kind
provenance
canonical statement
canonical relatedEntities set
```

Both entries must contain at least one `sourceEventId`. Unsourced legacy/manual entries remain separate because the system cannot prove whether they are replays or independent evidence.

## Canonicalization

Canonical statements use provider-independent Java normalization:

```text
Unicode NFKC
+ control/whitespace collapse
+ trim
+ Locale.ROOT lowercase
```

No stemming, synonym matching, embeddings, vector database or LLM classification is used. The policy is deliberately conservative: semantically similar but textually different statements remain separate.

Related entities are compared as sorted UUID sets. This prevents facts about different players or entities from being merged even when their text is identical.

## Outcomes

### Replay duplicate

The same semantic UUID is ignored as today. It adds no new evidence and does not rewrite the file.

### Corroborating evidence

Different semantic entries with the same consolidation key are replaced by one consolidated entry:

```text
sourceEventIds       = sorted union
relatedEntities      = sorted union
importance           = max
confidence           = max
gameTime             = max
createdAtEpochMillis = max
statement            = deterministic lexical representative
id                   = deterministic UUID from the consolidation key
```

The number of source event IDs records how much independent evidence supports the knowledge. This slice does not invent a confidence bonus.

### Distinct knowledge

Entries remain separate when any key component differs. In particular:

- FACT never merges with BELIEF;
- BELIEF entries with different provenance never merge;
- identical text about different related entities never merges;
- unsourced entries never merge;
- text that requires semantic interpretation remains separate.

## Store integration

`SemanticMemoryStore.append` performs consolidation before retention trimming. Therefore retention limits apply to unique consolidated knowledge rather than raw repeated evidence.

Load sanitization consolidates compatible persisted entries in memory so old files behave correctly immediately. The compacted representation is written on the next normal append; loading alone does not rewrite world data.

## Compatibility

- persistent format version remains 1;
- no new JSON fields;
- existing single-source FACT UUIDs remain unchanged until independent corroborating evidence is observed;
- consolidated UUIDs are deterministic and insertion-order independent;
- Minecraft 1.21.1, Java 21, Fabric and NeoForge requirements remain unchanged.

## Non-goals

- fuzzy semantic similarity;
- contradiction resolution;
- automatic BELIEF producers;
- confidence reinforcement formulas;
- forgetting/decay;
- legacy dialogue migration;
- NPC-to-NPC knowledge propagation.

## Acceptance criteria

1. Replaying the same UUID does not rewrite or duplicate data.
2. Distinct source events with the same key become one entry with all source IDs.
3. Consolidated UUID and fields are identical regardless of insertion order.
4. FACT/BELIEF and provenance boundaries remain intact.
5. Different related-entity sets remain separate.
6. Unsourced entries remain separate.
7. Retention counts consolidated knowledge once.
8. Existing stored logical duplicates are consolidated in memory after reload.
9. Full Fabric and NeoForge CI remains green.
