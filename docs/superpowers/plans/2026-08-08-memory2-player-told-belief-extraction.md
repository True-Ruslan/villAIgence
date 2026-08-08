# Memory 2.0 Player-Told BELIEF Extraction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add opt-in, bounded `PLAYER_TOLD` BELIEF candidates to the existing snapshot-aware structured chat response and persist them only after their exact Memory 2.0 DIALOGUE source is successfully stored.

**Architecture:** Reuse the existing OpenAI/OpenRouter chat request and structured response instead of adding a second provider call. The model proposes statement strings only; the server fixes provenance, source event, NPC owner and player identity and routes candidates through `SemanticBeliefAdmissionPolicy` / `ControlledSemanticBeliefProducer`.

**Tech Stack:** Java 21, JUnit 5, Gson, Fabric 1.21.1, existing Memory 2.0 stores and VillAIgence CI/GameTest/production gates.

## Global Constraints

- `LLM != authority`.
- `SYSTEM_OBSERVED` is FACT-only; extraction creates `PLAYER_TOLD` BELIEF only.
- No second provider request in this slice.
- Extraction defaults disabled.
- Candidate count defaults to 3 and normalizes to 1..8.
- Candidate statements are bounded to 240 Unicode code points.
- DIALOGUE source persistence happens before BELIEF persistence.
- Failed/empty/unusable provider responses create no semantic entry.
- Existing action, relationship, voice and provider retry behavior must remain unchanged.
- Root `CHANGELOG.md` is updated in this PR.

---

### Task 1: Bounded candidate parser

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/ai/SemanticBeliefCandidateParser.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/ai/SemanticBeliefCandidateParserTest.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/ai/StructuredAiResponseParser.java`
- Modify: `common/src/test/java/net/conczin/mca/livingworld/ai/StructuredAiResponseParserTest.java`

**Interfaces:**
- Consumes: nullable Gson `JsonElement`, configured max count.
- Produces: ordered immutable `List<String>` of normalized bounded candidates; `ParsedResponse.beliefCandidates()`.

- [ ] **Step 1: Write failing parser tests**
  - valid strings preserve order;
  - missing/null/non-array returns empty;
  - invalid elements are ignored;
  - NFKC/control/whitespace normalize;
  - statements cap at 240 code points;
  - max candidate count enforced;
  - duplicates removed;
  - malformed candidate metadata never invalidates a valid visible message.

- [ ] **Step 2: Run RED**

Run:

```bash
./gradlew :common:test --tests '*SemanticBeliefCandidateParserTest' --tests '*StructuredAiResponseParserTest' --no-daemon --console=plain
```

Expected: compile/test failure because `SemanticBeliefCandidateParser` / `ParsedResponse.beliefCandidates()` do not yet exist.

- [ ] **Step 3: Implement minimal parser**

Required signature:

```java
public final class SemanticBeliefCandidateParser {
    public static List<String> parse(JsonElement element, int maxCandidates);
}
```

`StructuredAiResponseParser.parse(...)` calls it for `beliefCandidates` and returns an immutable list. Optional metadata failure never changes message recovery behavior.

- [ ] **Step 4: Run focused GREEN**

Use the RED command and require PASS.

---

### Task 2: Additive extraction configuration

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/LivingWorldConfig.java`
- Modify: `common/src/test/java/net/conczin/mca/livingworld/LivingWorldConfigTest.java`
- Modify: `docs/livingworld/CONFIGURATION.md`

**Interfaces:**
- Produces: `semanticBeliefExtractionEnabled`, `semanticBeliefMaxCandidatesPerTurn`.

- [ ] **Step 1: Add failing config tests**

Assert:

```text
default enabled = false
default max = 3
max < 1 -> 3 or lower-bound-safe normalized value defined by implementation
max > 8 -> 8
round-trip fields preserved
```

For consistency choose explicit normalization:

```text
<= 0 -> 3
1..8 -> unchanged
> 8 -> 8
```

- [ ] **Step 2: Run RED**

```bash
./gradlew :common:test --tests '*LivingWorldConfigTest' --no-daemon --console=plain
```

Expected compile failure on missing fields.

- [ ] **Step 3: Add fields and normalization**

```java
public boolean semanticBeliefExtractionEnabled = false;
public int semanticBeliefMaxCandidatesPerTurn = 3;
```

No config version bump.

- [ ] **Step 4: Run GREEN**

Require focused config tests PASS.

---

### Task 3: Return the exact persisted dialogue source

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory2/Memory2DialogueIngestor.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory2/Memory2DialogueLifecycle.java`
- Modify: corresponding existing tests.

**Interfaces:**
- `Memory2DialogueIngestor.recordIfEnabled(...) -> Optional<MemoryEvent>`
- `Memory2DialogueIngestor.record(...) -> Optional<MemoryEvent>`
- `Memory2DialogueLifecycle.recordSuccessful(...) -> Optional<MemoryEvent>`

- [ ] **Step 1: Write failing return-value tests**
  - successful write returns the exact deterministic DIALOGUE event;
  - disabled path returns empty;
  - empty answer returns empty;
  - replay returns same event ID and store remains one event.

- [ ] **Step 2: Run RED**

```bash
./gradlew :common:test --tests '*Memory2DialogueIngestorTest' --tests '*Memory2DialogueLifecycleTest' --no-daemon --console=plain
```

Expected compile failure because current methods return `void`.

- [ ] **Step 3: Implement return-valued persistence**
  - construct one event through existing `DialogueMemoryAdapter`;
  - append before returning it;
  - return empty when disabled/world root absent/answer unusable;
  - do not alter deterministic identity.

- [ ] **Step 4: Run GREEN**

Require focused memory tests PASS.

---

### Task 4: Preserve candidates through OpenAI structured response

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java`
- Modify/add focused tests for parser/result/source policy as appropriate.

**Interfaces:**
- `StructuredResponse(..., List<String> beliefCandidates)`
- `SnapshotAnswer(Optional<String> message, List<String> beliefCandidates)`
- package-visible `answerDetailed(...)` used by `ChatAI` snapshot path.

- [ ] **Step 1: Write failing result-shape/source tests**
  - parsed candidates survive provider-envelope → structured response;
  - existing `answer(...)` still returns message-only behavior;
  - error/no-message result carries no persistable candidates.

- [ ] **Step 2: Observe RED**

Run the relevant common tests; failure must be missing richer result/candidate fields, not infrastructure.

- [ ] **Step 3: Implement minimal richer internal result**
  - preserve `ChatAIStrategy` signature;
  - make existing snapshot `answer(...)` delegate to `answerDetailed(...)`;
  - do not change legacy/Inworld strategy contracts.

- [ ] **Step 4: Run GREEN**

Require focused tests PASS.

---

### Task 5: Prompt contract for opt-in extraction

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java`
- Add focused prompt/source-policy tests.

**Interfaces:**
- structured response required when actions OR relationships OR extraction are active.

- [ ] **Step 1: Write failing prompt/source-policy tests**

Required text/behavior:

```text
beliefCandidates are only durable claims explicitly asserted by latest player message
[] when no useful claim exists
at most configured max
never include NPC-only claims
non-authoritative BELIEF candidates
```

Verify disabled extraction does not add candidate instructions.

- [ ] **Step 2: Observe RED**
- [ ] **Step 3: Implement prompt changes without weakening action/relationship instructions**
- [ ] **Step 4: Run GREEN**

---

### Task 6: Post-dialogue BELIEF orchestration

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/PlayerToldBeliefLifecycle.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/PlayerToldBeliefLifecycleTest.java`
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/ChatAI.java`

**Interfaces:**

```java
PlayerToldBeliefLifecycle.recordCandidatesIfEnabled(
    boolean enabled,
    Path worldRoot,
    MemoryEvent sourceDialogue,
    UUID playerId,
    List<String> candidates,
    int maxCandidates,
    int maxEntriesPerNpc
)
```

- [ ] **Step 1: Write failing lifecycle tests**
  - valid source + candidate creates `PLAYER_TOLD` BELIEF;
  - source must be DIALOGUE / PLAYER_TOLD through existing admission policy;
  - player UUID appears as related speaker;
  - disabled/null source/empty candidates write nothing;
  - max candidate count respected;
  - replay is idempotent;
  - equivalent candidates from different source dialogues consolidate source IDs.

- [ ] **Step 2: Observe RED**
- [ ] **Step 3: Implement lifecycle using `ControlledSemanticBeliefProducer` only**
  - no direct SemanticMemoryStore write;
  - importance/confidence inherit from source dialogue event;
  - fail soft per candidate/persistence operation as appropriate.
- [ ] **Step 4: Run focused GREEN**

Then wire snapshot `ChatAI`:

```text
SnapshotAnswer
→ persist Memory 2.0 DIALOGUE and obtain Optional<MemoryEvent>
→ only when source exists: PlayerToldBeliefLifecycle
→ return original message unchanged
```

Classic/non-snapshot and Inworld paths remain unchanged.

---

### Task 7: Regression, docs, changelog and exact gates

**Files:**
- Modify: `CHANGELOG.md`
- Modify: `docs/livingworld/SEMANTIC_INGESTION.md`
- Modify: `docs/livingworld/CONFIGURATION.md`
- Modify: `docs/PROJECT_STATE.md` / `docs/ROADMAP.md` only after final delivery boundary is known.

- [ ] **Step 1: Update `[Unreleased]`**

Record opt-in player-told extraction, authority boundary, default disabled state and validation evidence.

- [ ] **Step 2: Run focused common tests**

```bash
./gradlew :common:test --no-daemon --console=plain
```

- [ ] **Step 3: Push exact head and require selected GitHub gates**

At minimum for the runtime/persistence change:

```text
Repository security policy
VillAIgence CI
Production Soak
GitHub Release dry-run (publication skipped)
```

The fail-closed selector decides whether additional suites are mandatory.

- [ ] **Step 4: Independent diff review**

Check:

- no direct AI→FACT path;
- no direct parser→store path;
- DIALOGUE persisted before BELIEF;
- no extra provider call;
- no duplicate persistence on retry/replay;
- no action/relationship authority weakening;
- no transcript/reasoning leakage into diagnostics;
- no open review threads.

- [ ] **Step 5: Merge only after exact-head PASS**

Use squash merge with expected head SHA.
