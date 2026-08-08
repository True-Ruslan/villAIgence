# Memory 2.0 Player-Told BELIEF Extraction Design

## Status

Approved product direction: continue Memory 2.0 after controlled BELIEF admission with a bounded, inspectable claim-candidate layer.

This slice is deliberately limited to claims explicitly attributed to the current player turn (`PLAYER_TOLD`). It does not implement NPC-to-NPC transfer, inference, rumors, causal relationship reasons, or an independent provider call.

## Goal

Allow one successful snapshot-aware player→NPC dialogue turn to optionally produce a small set of non-authoritative semantic BELIEF candidates, without extra LLM latency/cost and without allowing model output to control provenance, source identity, FACT status, or server actions.

## Core decision: piggyback on the existing structured chat response

Three implementation directions were considered:

1. **Same-response metadata — selected.** Add optional bounded `beliefCandidates` strings to the already structured NPC response.
   - no second provider request;
   - no additional queue/deadline budget;
   - no separate retry surface;
   - candidate metadata is already separated from visible/spoken `message`;
   - server still owns provenance/source identity.
2. **Second extractor provider call — rejected for this slice.** Cleaner model separation, but adds latency, cost, retries, deadline/backpressure complexity, and new failure modes after the visible response has already succeeded.
3. **Local linguistic heuristics — rejected.** Cheap and deterministic, but language-dependent and too brittle for natural multilingual dialogue.

The selected design keeps extraction **advisory**. A model can propose statement text only. It cannot create FACT, choose provenance, choose a source UUID, or mutate persistence directly.

## Activation and rollout

Add server config:

```text
semanticBeliefExtractionEnabled = false
semanticBeliefMaxCandidatesPerTurn = 3
```

`semanticBeliefExtractionEnabled` defaults to **false** for a controlled pre-release rollout. Existing dialogue/provider behavior therefore remains unchanged until explicitly enabled on the test server.

`semanticBeliefMaxCandidatesPerTurn` is normalized to `1..8`; default is `3`.

Extraction is active only when both are true:

```text
memory2Enabled
semanticBeliefExtractionEnabled
```

No configuration version bump is required because Gson-compatible additive fields have safe defaults and current config version 2 remains semantically compatible.

## Structured response contract

When extraction is enabled, snapshot-aware OpenAI/OpenRouter dialogue requests require the existing structured JSON response with one additional field:

```json
{
  "message": "natural NPC reply",
  "optionalCommand": "",
  "relationshipDelta": {
    "trust": 0,
    "respect": 0,
    "fear": 0,
    "affinity": 0
  },
  "beliefCandidates": [
    "The player says the north bridge is unsafe."
  ]
}
```

The actual emitted example may omit unrelated optional metadata when those systems are disabled, but `beliefCandidates` is present whenever extraction is enabled.

### Model instruction

The prompt tells the model:

- candidates describe only durable information **explicitly asserted by the player's latest message**;
- never include claims originating only from the NPC reply;
- never classify a candidate as FACT;
- use `[]` for greetings, questions, commands, non-durable chatter, or when no useful claim exists;
- return at most the configured candidate count;
- each candidate is short and self-contained;
- candidates are advisory and may be rejected by server policy.

The server does not trust the model to obey these instructions for authority. They improve semantic quality only.

## Candidate parsing and bounds

Add a focused provider-independent parser:

```text
SemanticBeliefCandidateParser
```

Input:

```text
JsonElement beliefCandidates
configured maxCandidates
```

Output:

```text
List<String>
```

Rules:

1. missing/null/non-array → `[]`;
2. iterate in provider order and consider at most the configured count;
3. non-string elements are ignored;
4. each string is NFKC-normalized;
5. whitespace/control runs collapse to one space;
6. blank results are ignored;
7. each statement is capped to 240 Unicode code points, matching Semantic Memory bounds;
8. exact normalized duplicates within one response are removed while preserving first occurrence order;
9. no exception from malformed optional candidate metadata may invalidate a valid visible NPC message.

This parser does not infer truth or provenance.

## Internal result flow

`StructuredAiResponseParser.ParsedResponse` gains:

```text
List<String> beliefCandidates
```

`OpenAIChatAI.StructuredResponse` retains those candidates.

The snapshot-aware `OpenAIChatAI` path gains a richer internal result:

```text
SnapshotAnswer(
    Optional<String> message,
    List<String> beliefCandidates
)
```

Existing public/strategy-facing `Optional<String>` behavior remains source-compatible: the current `answer(...)` method delegates to the richer path and returns only `message`.

`ChatAI` uses the richer result only for snapshot-aware OpenAI/OpenRouter dialogue.

Classic/non-snapshot and Inworld paths do not ingest candidates in this slice.

## Source-event ownership and persistence ordering

The semantic entry must never reference a source dialogue that was not successfully persisted.

Therefore `Memory2DialogueLifecycle.recordSuccessful(...)` becomes return-valued:

```text
Optional<MemoryEvent>
```

and returns the exact DIALOGUE event after successful append/admission. Existing callers may ignore the return value.

Order:

```text
usable provider result
→ visible message/validated command/relationship behavior
→ Memory 2.0 DIALOGUE persistence
→ obtain persisted source MemoryEvent
→ for each bounded candidate:
     server fixes provenance = PLAYER_TOLD
     server fixes related player UUID
     server fixes source event UUID/time/owner from MemoryEvent
     ControlledSemanticBeliefProducer
→ semantic-memory.json
```

If DIALOGUE persistence fails or returns empty, **no semantic BELIEF is written**.

If semantic persistence fails, it fails soft and cannot remove the already successful dialogue reply or source DIALOGUE event.

## Authority boundary

The model controls only candidate statement text.

The server controls:

```text
kind       = BELIEF
provenance = PLAYER_TOLD
owner      = sourceEvent.ownerNpcId
source     = sourceEvent.id
speaker    = current player UUID
time       = sourceEvent time
```

`SYSTEM_OBSERVED` never enters this path.

The existing `SemanticBeliefAdmissionPolicy` is still mandatory; no direct write to `SemanticMemoryStore` is permitted from the AI parser/path.

Current observed world facts remain authoritative over conflicting beliefs during retrieval/prompt construction.

## Idempotency and retry

Provider retry occurs before a usable structured answer is accepted. Candidate persistence occurs only after the final usable answer and after DIALOGUE persistence.

The source DIALOGUE event has deterministic identity. BELIEF identity and existing consolidation are deterministic. Therefore replay of the same accepted turn must not multiply semantic entries.

Required tests prove:

- exact lifecycle replay creates one BELIEF;
- two genuinely different source dialogue events with the same normalized claim consolidate source IDs rather than multiply equivalent semantic entries;
- failed/empty provider result never reaches candidate persistence;
- candidate metadata on an unusable response never creates memory.

## Prompt compatibility

Today the snapshot path requires JSON only when safe actions or relationship state is active. With extraction enabled, JSON becomes required even if both of those systems are disabled.

The prompt example must preserve all existing fields and add `beliefCandidates`; it must not weaken command or relationship rules.

When extraction is disabled, existing prompt behavior remains unchanged.

## Failure handling

Fail soft:

- missing candidate field → normal NPC reply, no BELIEF;
- malformed candidate field → normal NPC reply, no/partial valid candidates according to parser rules;
- overlong candidate → bounded to 240 code points;
- duplicate candidate → one candidate;
- semantic store failure → log bounded diagnostic, keep dialogue response/source event;
- disabled Memory 2.0/extraction → no candidate prompt/persistence.

Fail closed for authority:

- model-provided provenance/source IDs are not part of the schema and are ignored if present as unknown fields;
- no candidate can become FACT through this path;
- no candidate can trigger Minecraft actions.

## Security and privacy

- no new endpoint, key, header, or network call;
- no provider reasoning is persisted;
- candidates are bounded before persistence;
- existing provider response byte limits remain authoritative;
- diagnostics must not log full prompts/transcripts/candidate collections;
- client owns no semantic identity or persistence mutation.

## TDD acceptance matrix

### Parser RED/GREEN

- valid array parsed in order;
- null/missing/non-array → empty;
- invalid elements ignored without losing visible message;
- NFKC/whitespace/control normalization;
- 240-code-point cap;
- configured count bound;
- duplicate removal;
- malformed optional metadata cannot invalidate `message`.

### Config RED/GREEN

- default extraction disabled;
- default max candidates = 3;
- configured max normalized to `1..8`;
- additive config round-trip preserves fields.

### Lifecycle RED/GREEN

- successful DIALOGUE persistence returns exact source event;
- disabled/empty result returns empty source;
- replay returns the same deterministic source event identity.

### End-to-end memory orchestration RED/GREEN

A focused pure orchestration seam must prove:

- source event + candidate → one `PLAYER_TOLD` BELIEF;
- source event is mandatory;
- current player UUID is the related speaker;
- disabled extraction writes nothing;
- replay does not duplicate;
- equivalent claims from separate source events consolidate source IDs.

### Prompt/source policy

Source-level or focused helper tests prove:

- extraction enabled forces structured-response instructions;
- prompt says candidates come only from latest player assertion;
- model cannot set provenance/source IDs through the documented schema;
- classic/Inworld paths are not silently wired.

## Non-goals

This slice does **not** implement:

- separate extractor API/provider request;
- `NPC_TOLD` production;
- `INFERRED` production;
- LLM truth classification;
- FACT promotion;
- entity-name→UUID resolution beyond current player association;
- relationship causal reasons;
- rumor propagation;
- embeddings/vector search;
- legacy `memory.json` migration.

## Exit criteria

The slice is complete when:

1. extraction is explicit opt-in and bounded;
2. one successful snapshot-aware player dialogue can carry bounded candidate strings in its existing structured response;
3. candidate parsing cannot corrupt visible message handling;
4. DIALOGUE source is persisted before BELIEF;
5. server fixes `PLAYER_TOLD`, player UUID, NPC owner and source event identity;
6. retries/replay are idempotent;
7. failed/empty/unusable responses create no semantic entry;
8. existing action/relationship/provider/voice behavior remains regression-green;
9. root `CHANGELOG.md`, Semantic Memory docs, config docs and canonical state are updated;
10. full selected CI, production, persistence, soak/release-dry-run gates are green where selected.
