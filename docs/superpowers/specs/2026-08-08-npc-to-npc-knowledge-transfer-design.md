# NPC-to-NPC Knowledge Transfer Design

Date: 2026-08-08
Status: approved design; implementation gated by written-spec review and strict TDD
Base branch: `1.21.1`
Base commit: `a20d6d0ebf5688e790fedeb3563f24f69e7e9c95`
Product track: Memory 2.0

## 1. Goal

Add the first trustworthy NPC-to-NPC knowledge-transfer primitive to Memory 2.0.

The slice must prove one exact chain:

```text
speaker NPC A owns persisted Semantic knowledge
→ server authorizes transfer to listener NPC B
→ exact listener-owned NPC_TOLD DIALOGUE evidence persists
→ server rereads and validates that exact evidence
→ listener NPC B receives Semantic BELIEF
```

This is deliberately a low-level server-owned primitive. It does not yet decide when NPCs autonomously talk, generate visible NPC-to-NPC conversations, call an LLM, synthesize voice, propagate rumors, or distort claims.

The purpose is to establish a provenance-safe foundation that later social simulation can invoke without inventing omniscient or provider-authoritative knowledge.

## 2. Existing contracts that remain authoritative

The implementation must preserve all current VillAIgence architecture laws, especially:

- Minecraft/server state is truth; the LLM/provider is never authority;
- `FACT` requires `SYSTEM_OBSERVED` evidence;
- `BELIEF` may use only `PLAYER_TOLD`, `NPC_TOLD` or `INFERRED` provenance;
- confidence, repetition, retention and ranking never promote BELIEF to FACT;
- current observed facts outrank Operator Lore, Semantic Memory and episodic/social recollection;
- player-scoped prompt visibility is an eligibility boundary, not a ranking preference;
- foreign-player memory is excluded before bounded candidate selection;
- NPC-global memory remains eligible;
- shared memory remains eligible when the current player belongs to its semantic scope;
- persistence and replay are deterministic and idempotent;
- no client or provider chooses memory owner, truth class, source identity, visibility or gameplay authority;
- long-horizon retrieval remains hard-bounded at the existing candidate/result limits;
- no memory class becomes immortal under pressure;
- compatibility work requires a supported-data reason.

Official installed-release evidence remains `0.2.0+1.21.1`. This NPC-to-NPC slice is unreleased development until a later release candidate is explicitly accepted.

## 3. Non-goals

This slice does **not** add or authorize:

- autonomous NPC conversation scheduling;
- visible NPC-to-NPC chat bubbles or player-facing conversation UI;
- a second or new LLM/provider call;
- provider selection of transfer source, listener, speaker, provenance, truth class, confidence or importance;
- NPC-to-NPC STT/TTS/voice orchestration;
- rumor propagation or multi-hop `A → B → C` dissemination;
- claim distortion, paraphrase, uncertainty propagation or social trust weighting;
- relationship-driven transfer probability;
- global broadcast of transferred knowledge;
- a new `MemoryEvent.Type`;
- a new persistence file;
- persistence schema/version changes;
- public config fields or config-version changes;
- legacy migration/backfill;
- changes to prompt candidate limits, ranking weights or long-horizon quotas;
- unrelated provider, transport, voice, gameplay or relationship refactoring.

## 4. Chosen architecture

The chosen approach is a **server-owned explicit transfer primitive**.

Rejected alternatives for this slice:

1. **Immediate autonomous/visible NPC↔NPC LLM dialogue** — mixes knowledge transfer with scheduling, provider budgets, presentation, voice and social behavior before provenance is proven.
2. **Direct Semantic Memory copy** — creates knowledge without exact persisted transfer evidence and risks copying truth authority or fabricating provenance.
3. **Implicit proximity/social transfer without dialogue evidence** — weakens source attribution and behaves like magical omniscience.

Required flow:

```text
SemanticMemoryStore
  exact speaker-owned entry
          │
          ▼
NpcKnowledgeTransferLifecycle
  exact source lookup + authoritative reread
          │
          ▼
NpcToldDialogueAdapter
  deterministic listener-owned DIALOGUE
  provenance = NPC_TOLD
          │
          ▼
MemoryEventStore.append(...)
          │
          ▼
exact persisted evidence reread
          │
          ▼
NpcKnowledgeTransferPolicy
  exact boundary validation
          │
          ▼
SemanticBeliefAdmissionPolicy
          │
          ▼
ControlledSemanticMemoryIngestor
          │
          ▼
SemanticMemoryStore
  listener-owned BELIEF / NPC_TOLD
```

The server owns every identity and provenance decision in this flow.

## 5. Public lifecycle input and source authority

The transfer lifecycle accepts only server-owned identifiers/state required to prove the transfer:

```text
worldRoot
speakerNpcId
listenerNpcId
speakerSemanticEntryId
authoritativeGameTime
memory2CapacityPerNpc
semanticCapacityPerNpc
```

The caller does **not** provide arbitrary claim text, provenance, semantic kind, importance, confidence, related entities, source-event IDs, event UUIDs or listener Semantic entry IDs.

The lifecycle performs an exact `speakerSemanticEntryId` lookup in the current world-local `SemanticMemoryStore` and requires:

- speaker and listener IDs are present;
- `speakerNpcId != listenerNpcId`;
- the semantic entry exists;
- its owner is exactly `speakerNpcId`;
- its normalized statement is non-blank.

Immediately before evidence construction, the lifecycle performs an authoritative exact reread of the same source ID. The reread must still match the selected immutable source snapshot on identity-critical fields:

- entry UUID;
- owner NPC;
- kind;
- provenance;
- normalized statement;
- canonical semantic subject scope;
- source-event UUID set.

If the source disappeared, was consolidated away, or no longer matches that exact snapshot before evidence construction, the transfer returns `REJECTED` and writes no transfer evidence or listener BELIEF.

No arbitrary statement injection is permitted through this primitive.

## 6. Source knowledge and truth boundary

Both speaker `FACT` and speaker `BELIEF` entries are transferable in the first slice.

Transfer never copies truth authority.

Examples:

```text
A: FACT / SYSTEM_OBSERVED / "The bridge is destroyed"
→ B: BELIEF / NPC_TOLD / "The bridge is destroyed"

A: BELIEF / PLAYER_TOLD / "The bridge is destroyed"
→ B: BELIEF / NPC_TOLD / "The bridge is destroyed"
```

For listener B, the authoritative direct evidence is only:

> speaker A told listener B this statement.

The listener does not inherit the speaker's original `FACT` status or original provenance class. Repetition, corroboration or high confidence cannot upgrade the transferred belief to FACT.

The first slice also does not copy the speaker's upstream `sourceEventIds` into the listener BELIEF. The listener BELIEF points to the exact NPC_TOLD transfer evidence event. Rich origin-chain propagation is reserved for the later provenance-aware rumor design.

## 7. NPC_TOLD evidence event

A transfer is represented with the existing `MemoryEvent.Type.DIALOGUE`; no new event type or persistence schema is introduced.

The event is listener-owned and canonical:

```text
ownerNpcId              = listenerNpcId
type                    = DIALOGUE
participants            = [listenerNpcId, speakerNpcId]
provenance              = NPC_TOLD
gameTime                = authoritativeGameTime
createdAtEpochMillis    = 0
importance              = 50
emotionalWeight         = 0
confidence              = 50
relationshipReasons     = []
dialogue                = null
relationshipTransition  = null
relationshipCause       = null
summary                 = "NPC told: " + normalizedStatement
```

`createdAtEpochMillis = 0` is deliberate for this synthetic server-owned transfer evidence. Transfer chronology is authoritative Minecraft `gameTime`; using wall clock would introduce an unnecessary replay/retention tie-break input. Existing store order still remains deterministic through `gameTime` and UUID.

Participant orientation is exact and intentional: listener first, speaker second. Any differently oriented or differently scoped event is invalid transfer evidence even if it contains the same UUIDs.

`dialogue == null` is deliberate. `DialogueExchange` is player-oriented (`playerMessage` / `npcReply`) and must not be reused by pretending one NPC is a player. Existing player Working Memory requires structured dialogue, so this server-side transfer event stays out of player user/assistant dialogue reconstruction.

The summary is evidence text, not a parsing authority. Admission uses the normalized statement already resolved from the exact speaker Semantic entry; it must never recover authority by parsing arbitrary summary prose.

### 7.1 Statement normalization and bound

The transferred statement uses the same semantic normalization/boundary as current Semantic BELIEF ingestion: at most `240` Unicode code points after normalization.

The canonical evidence summary is therefore at most `250` Unicode code points: the fixed ten-code-point prefix `NPC told: ` plus the bounded statement.

No provider paraphrase occurs. If a persisted speaker statement is longer because of older/internal construction, the transfer adapter applies the current semantic bound deterministically before both evidence summary construction and listener BELIEF admission.

## 8. Deterministic evidence identity and replay

Evidence UUID is deterministic under a dedicated versioned namespace:

```text
npc-knowledge-transfer-v1
listenerNpcId
speakerNpcId
speakerSemanticEntryId
authoritativeGameTime
```

`createdAtEpochMillis` is not part of evidence identity or authority.

`MemoryEventStore.append(...)` already de-duplicates by exact event UUID. Exact retry therefore resolves to the same event UUID, appends no duplicate, and rereads the retained persisted event as downstream authority.

Replay validation compares all canonical transfer fields from section 7, including exact event UUID, owner, type, participant orientation, provenance, game time, fixed policy values and canonical summary.

Required behavior:

- exact same transfer retry → same evidence UUID;
- exact same transfer retry → no duplicate `MemoryEvent`;
- exact same transfer retry → no duplicate logical Semantic BELIEF/source;
- same source transferred at a different authoritative `gameTime` → a new evidence UUID/source event;
- equivalent claims from distinct source events may consolidate through the existing Semantic consolidation/source-union behavior.

## 9. Evidence validation boundary

Current generic `SemanticBeliefAdmissionPolicy` correctly requires `NPC_TOLD` to originate from `NPC_TOLD DIALOGUE`, but that check alone does not prove exact speaker/listener orientation or source ownership.

The new lifecycle/policy must strengthen the boundary before calling generic admission.

The exact persisted evidence must satisfy all of the following:

- `ownerNpcId == listenerNpcId`;
- `speakerNpcId != listenerNpcId`;
- `type == DIALOGUE`;
- `provenance == NPC_TOLD`;
- participants are exactly `[listenerNpcId, speakerNpcId]` in that order;
- `gameTime == authoritativeGameTime`;
- `createdAtEpochMillis == 0`;
- `importance == 50`;
- `emotionalWeight == 0`;
- `confidence == 50`;
- structured dialogue/relationship payloads are absent;
- evidence UUID equals the deterministic UUID for listener/speaker/source-entry/game-time;
- summary equals the exact canonical `NPC told: <normalizedStatement>` representation;
- the authoritative source Semantic entry belongs to `speakerNpcId`.

Only after exact persisted evidence passes this boundary may `SemanticBeliefAdmissionPolicy` produce a `SemanticBeliefSource`.

The resulting source remains listener-owned because generic admission derives BELIEF owner/time/source identity from the persisted evidence event.

## 10. Semantic scope and player visibility

The listener BELIEF preserves the **knowledge subject scope** of the speaker Semantic entry.

Examples:

```text
speaker relatedEntities = []
→ listener relatedEntities = []
→ NPC-global

speaker relatedEntities = [playerP]
→ listener relatedEntities = [playerP]
→ player-scoped

speaker relatedEntities = [playerP, npcC]
→ listener relatedEntities = [playerP, npcC]
→ shared scope preserved
```

The speaker NPC is **not** automatically added to listener `relatedEntities`.

Semantic scope equivalence is defined as the same normalized UUID set, not list insertion order. This matches existing semantic normalization/consolidation behavior and prevents consolidation order from changing visibility semantics.

`relatedEntities` describes what/who the semantic knowledge concerns for prompt eligibility. Provenance identity belongs in the exact transfer evidence event. Mixing speaker identity into semantic subject scope would incorrectly alter player visibility.

The raw transfer evidence event has participants `[listener, speaker]`. Under current player-scoped episodic eligibility it is not ordinary player dialogue/history because the current player is not an external participant. Player prompts consume the resulting scoped Semantic BELIEF, not a fabricated player conversation turn.

Foreign-player semantic memory remains excluded before bounded prompt candidate allocation exactly as before this slice.

## 11. Transfer importance and confidence policy

The first slice uses fixed deterministic server-owned values:

```text
importance = 50
confidence = 50
```

These values characterize listener B's memory of being told the statement. They do not inherit or restate the authority/confidence of speaker A's source entry.

The first slice intentionally does not incorporate:

- source importance/confidence;
- relationship level;
- trust/reputation;
- number of previous repetitions;
- provider confidence;
- speaker personality;
- rumor-chain depth.

Those dimensions require separate designs because they change social epistemology rather than merely proving transfer provenance.

## 12. Semantic admission and consolidation

After evidence persistence and exact reread/validation, the lifecycle calls the existing controlled BELIEF path with:

```text
owner        = listenerNpcId
kind         = BELIEF
provenance   = NPC_TOLD
statement    = normalized transferred statement
related      = copied canonical subject scope
importance   = 50
confidence   = 50
sourceEvents = [exact NPC_TOLD evidence UUID]
```

The lifecycle must account for existing `SemanticMemoryConsolidator` behavior: an append can consolidate an equivalent listener BELIEF with existing compatible sources and may therefore change the retained Semantic entry UUID.

Success must **not** be defined as "candidate BELIEF UUID exists after append".

Instead, after append the lifecycle rereads listener Semantic Memory and proves a retained compatible BELIEF exists whose:

- owner is the listener;
- kind is `BELIEF`;
- provenance is `NPC_TOLD`;
- normalized statement matches;
- canonical semantic subject UUID set matches;
- `sourceEventIds` contains the exact new NPC_TOLD evidence UUID.

This supports both first admission and deterministic corroborating source union.

## 13. Exact store lookup requirements

The current stores expose bounded recent queries but no dedicated exact-ID read API. Implementation may add minimal pure read methods such as:

```text
MemoryEventStore.findById(ownerNpcId, eventId)
SemanticMemoryStore.findById(ownerNpcId, entryId)
```

or an equivalently strict internal API.

Requirements:

- exact owner + UUID lookup;
- no mutation;
- no newest-window approximation;
- no prompt ranking;
- no persistence-format change;
- deterministic result;
- safe empty result for missing/mismatched owner.

The lifecycle must not use bounded `getRecent(...)` queries as authority lookups because valid evidence/source may sit outside a recent window.

For post-admission consolidation validation, an internal exact compatible-entry query may scan the bounded listener store by semantic consolidation key plus exact source UUID containment. It must not rely on newest-window position or prompt ranking.

## 14. Cross-store write order and failure semantics

No distributed transaction is introduced between `memory2.json` and `semantic-memory.json`.

Required order:

```text
1. resolve exact speaker Semantic source
2. authoritative exact reread + source snapshot validation
3. construct deterministic NPC_TOLD evidence
4. append evidence to MemoryEventStore
5. reread exact evidence by listener owner + UUID
6. validate exact persisted evidence contract
7. run NPC_TOLD semantic admission
8. append listener BELIEF to SemanticMemoryStore
9. reread compatible retained listener BELIEF containing evidence UUID
10. return explicit result
```

Result surface:

```text
ADMITTED
REJECTED
SOURCE_NOT_RETAINED
BELIEF_NOT_RETAINED
```

### 14.1 `REJECTED`

Expected fail-closed input/authority failures return `REJECTED` without creating a listener BELIEF, including:

- missing speaker or listener;
- `speaker == listener`;
- missing source Semantic entry;
- source entry owned by another NPC;
- source disappeared or changed before the authoritative pre-write reread;
- blank/invalid normalized statement;
- mismatched persisted evidence identity/shape;
- mismatched owner/participants/provenance/type/gameTime/policy fields.

If invalidity is discovered before step 4, no transfer evidence is written. If storage corruption or an impossible mismatch is discovered only after an evidence append/reread, the lifecycle fails closed and creates no BELIEF; it must not fabricate a replacement source.

Expected rejection is not an uncaught exception path.

### 14.2 `SOURCE_NOT_RETAINED`

If `MemoryEventStore` pressure immediately rejects/evicts the new transfer evidence, exact reread by UUID fails and Semantic BELIEF admission does not occur.

No source is fabricated or reconstructed from summary text.

### 14.3 `BELIEF_NOT_RETAINED`

If exact evidence persists and the transfer legitimately occurred, but Semantic Memory pressure does not retain a compatible listener BELIEF containing that evidence UUID, the result is `BELIEF_NOT_RETAINED`.

The evidence event is **not rolled back**. The historical event "A told B this" occurred; B simply did not retain it as semantic long-term knowledge.

### 14.4 `ADMITTED`

Return `ADMITTED` only after exact persisted evidence is validated and the listener store contains a compatible retained `NPC_TOLD` BELIEF with the exact evidence UUID among its sources.

## 15. Retention and long-horizon behavior

The slice adds no special retention privileges.

- transfer evidence uses existing `MemoryEventRetentionPolicy`;
- listener BELIEF uses existing `SemanticMemoryRetentionPolicy`;
- retrieval uses existing hard-bounded long-horizon selection;
- prompt candidate limit remains `32`;
- normal long-horizon split remains `24 recent + 8 durable`;
- final result bound remains `6` per current memory domain;
- no transferred event or BELIEF is pinned or immortal.

A transferred BELIEF may survive weaker newer knowledge, may be recalled over long game-time distance, and may eventually be forgotten under deterministic pressure.

If a source evidence event is later evicted after BELIEF admission, the retained BELIEF may keep the historical source UUID. The system must not fabricate the missing source or reinterpret the belief as FACT.

## 16. Current-truth precedence

Transferred knowledge never changes prompt authority ordering.

Required contradiction example:

```text
listener BELIEF / NPC_TOLD: "The bridge is intact"
current server observation: "The bridge is destroyed"
```

The current observed state remains authoritative and structurally precedes the transferred BELIEF in prompt composition.

Multiple NPC_TOLD sources, fixed confidence, semantic retention or long-horizon recall cannot override current `SYSTEM_OBSERVED` truth.

## 17. Component boundaries

### 17.1 `NpcToldDialogueAdapter`

Pure deterministic construction only.

Responsibilities:

- normalize/bound statement;
- construct exact canonical server-authored transfer summary;
- construct exact listener-owned DIALOGUE shape;
- compute versioned deterministic evidence UUID;
- set deterministic metadata/policy fields from section 7;
- never access stores, provider, client or world mutation APIs.

### 17.2 `NpcKnowledgeTransferPolicy`

Pure fail-closed validation only.

Responsibilities:

- validate speaker/listener/source ownership contract;
- validate exact persisted transfer evidence shape;
- expose fixed policy values/constants if kept centrally;
- compare semantic scopes canonically as UUID sets;
- never persist data;
- never call an LLM;
- never upgrade truth class.

### 17.3 `NpcKnowledgeTransferLifecycle`

Orchestration only.

Responsibilities:

- exact source lookup and authoritative pre-write reread;
- invoke adapter/policy;
- evidence append and exact reread;
- controlled BELIEF admission;
- Semantic append and retained-compatible-result reread;
- map expected outcomes to explicit result status;
- preserve idempotency and cross-NPC isolation.

### 17.4 `NpcKnowledgeTransferResult`

A small explicit result type/status containing at minimum the outcome status and, when available, exact retained evidence/semantic identifiers needed for deterministic tests/diagnostics.

It must not expose hidden prompts, provider reasoning or mutable store internals.

## 18. Strict TDD implementation sequence

Production behavior must be implemented only after observing the intended failure from tests-only commits.

### RED 1 — source-backed transfer primitive

Prove failure before implementation for:

- exact speaker-owned Semantic source selection;
- listener-owned `NPC_TOLD DIALOGUE` evidence;
- exact canonical evidence fields from section 7;
- listener receives BELIEF, never FACT;
- `FACT → BELIEF` and `BELIEF → BELIEF` cases;
- source semantic scope preserved.

### RED 2 — fail-closed identity/evidence

Prove rejection for:

- wrong source owner;
- unknown source ID;
- source disappearance/change before authoritative reread;
- speaker/listener inversion;
- same speaker/listener;
- wrong participant orientation;
- wrong provenance;
- wrong event type;
- wrong gameTime;
- wrong fixed policy/canonical summary fields;
- forged/prebuilt evidence not produced from the exact speaker source;
- no partial listener BELIEF after rejection.

### RED 3 — idempotency and consolidation

Prove:

- exact retry → same evidence UUID;
- exact retry → one evidence event;
- exact retry → one logical Semantic BELIEF/source;
- later transfer at a new gameTime → distinct evidence UUID;
- equivalent later claim consolidates deterministically and unions exact source evidence;
- result validation remains correct when consolidation changes retained Semantic entry UUID;
- semantic scope comparison is stable regardless of UUID list insertion order.

### RED 4 — pressure and partial-retention semantics

Prove:

- transfer evidence rejected by Memory 2.0 pressure → `SOURCE_NOT_RETAINED` and no BELIEF;
- evidence retained but listener BELIEF rejected by semantic pressure → `BELIEF_NOT_RETAINED`;
- evidence is not rolled back in the second case;
- unrelated NPC memory is unchanged;
- weak rejected append does not cause unrelated persistence rewrite/corruption;
- wall-clock time cannot alter transfer-evidence retention tie behavior because transfer evidence uses deterministic metadata.

### RED 5 — restart/reload

Prove:

- evidence and BELIEF survive world-local store reload when retained;
- exact source UUID survives reload;
- exact transfer retry after reload remains idempotent;
- no provenance/truth-class mutation after reload;
- store order/lookup is deterministic.

### RED 6 — scope/privacy isolation

Prove:

- NPC-global source remains NPC-global for listener;
- player-scoped source remains scoped to that exact player;
- shared subject scope is preserved as the same canonical UUID set;
- speaker is not injected into semantic `relatedEntities`;
- transfer A→B creates nothing for C;
- independent D→C transfer cannot affect B;
- foreign-player listener memory consumes zero prompt candidate slots for another player;
- raw NPC→NPC transfer evidence is not reconstructed as player Working Memory dialogue.

### RED 7 — long-horizon integration

Prove under bounded pressure and multiple sessions:

- transferred BELIEF can enter the existing durable candidate tier when retained;
- it can survive weaker newer knowledge according to existing policies;
- it remains evictable;
- no candidate/result limits change;
- selection remains deterministic across input order and reload.

### RED 8 — truth-authority preservation

Prove:

- transferred NPC_TOLD BELIEF conflicting with a current observed FACT does not override current truth;
- repeated NPC_TOLD corroboration remains BELIEF;
- speaker FACT does not become listener FACT;
- speaker BELIEF does not change provenance into the speaker's original provenance at listener side.

### Preservation simulation

Add a deterministic multi-NPC simulation with at least:

- two independent speaker/listener pairs;
- NPC-global, player-scoped and shared Semantic sources;
- source FACT and source BELIEF cases;
- repeated transfers across multiple authoritative game times;
- hundreds of unrelated pressure records;
- multiple fresh store reloads/restarts;
- forward and reversed fixture insertion/iteration where applicable;
- exact survivor/evidence/source IDs asserted;
- no sleeps and no wall-clock-based expected behavior.

## 19. Validation gates

The implementation PR must satisfy the repository's selected mandatory matrix for runtime/persistence changes, including at minimum:

- common unit/integration tests;
- deterministic mock-provider coverage where the existing matrix selects it;
- Fabric GameTests for server/runtime behavior selected by repository policy;
- Fabric and NeoForge compatibility/build checks;
- production startup/restart evidence;
- persistence recovery checks;
- constrained-heap/soak coverage when selected;
- package/release-identity verification;
- security workflow;
- release dry-run with publication skipped unless explicitly authorized.

Unknown CI classification must continue to fail closed.

Exact RED→GREEN evidence must be recorded in a dedicated `docs/superpowers/evidence/...` document before merge.

## 20. Documentation and release boundary

The implementation PR must update root `CHANGELOG.md` because this is notable runtime/persistence behavior.

After the product PR merges, reconcile `docs/PROJECT_STATE.md` and `docs/ROADMAP.md` in the same delivery flow or a dedicated documentation-only reconciliation PR according to the repository's established pattern.

Do not describe this slice as installed `0.2.0` acceptance. Installed evidence remains tied to the immutable accepted 0.2.0 artifact until a later release candidate is built and explicitly tested.

## 21. Acceptance criteria

The slice is complete only when all of the following are true:

1. A listener BELIEF can be created only from an exact persisted Semantic entry owned by the claimed speaker.
2. The speaker source passes an exact authoritative pre-write reread before any transfer evidence is written.
3. The transfer event is persisted before BELIEF admission and is exact listener-owned `DIALOGUE / NPC_TOLD` evidence.
4. Speaker `FACT` or `BELIEF` always becomes listener `BELIEF / NPC_TOLD`, never FACT.
5. The caller cannot inject arbitrary claim text, source-event IDs, provenance, owner, importance or confidence.
6. Exact participant orientation and canonical evidence fields are proven fail-closed.
7. Listener BELIEF preserves the source semantic subject UUID set without adding speaker to `relatedEntities`.
8. Exact retry is idempotent across evidence and Semantic persistence, including after reload.
9. Distinct later transfer creates distinct evidence and existing Semantic consolidation may deterministically union sources.
10. Pressure returns explicit `SOURCE_NOT_RETAINED` / `BELIEF_NOT_RETAINED` outcomes without corrupting unrelated state.
11. No rollback fabricates or deletes a legitimate persisted transfer event when Semantic retention rejects the BELIEF.
12. Transferred memory participates in existing bounded retention/long-horizon recall and remains evictable.
13. Foreign-player privacy and NPC isolation remain intact before candidate allocation.
14. Current observed truth still outranks conflicting transferred BELIEF.
15. No new provider call, persistence file, schema version, config field, client authority or legacy migration is introduced.
16. Strict staged TDD evidence and the selected full validation gates are green on the exact implementation head before merge.

## 22. Follow-up slices

Only after this primitive is accepted should later design cycles consider:

1. autonomous NPC-to-NPC conversation initiation;
2. visible/social dialogue presentation and optional voice;
3. provenance-aware multi-hop rumors;
4. bounded rumor distortion/uncertainty;
5. relationship/trust/reputation effects on belief confidence;
6. contradiction handling between multiple social sources;
7. settlement/faction-level information diffusion.

Those are separate product/authority decisions and must not be pulled into this implementation opportunistically.
