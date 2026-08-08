# FACT > BELIEF Retrieval Precedence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make current server-observed truth structurally authoritative over remembered/background context while preventing foreign-player episodic and semantic memory from entering the current player's prompt.

**Architecture:** Keep persistence formats unchanged. Add exact player/global eligibility before candidate limiting, preserve existing ranking only for eligible candidates, remove snapshot-side duplicate MemoryModule loading, and make `SnapshotContextPromptPolicy` the single deterministic renderer for observed facts, Operator Lore, semantic memory, and episodic memory before structured-response instructions.

**Tech Stack:** Java 21, Fabric/NeoForge Minecraft 1.21.1, JUnit 5, Gson-backed world-local JSON stores, GitHub Actions production acceptance/recovery/soak/release dry-run.

## Global Constraints

- Base branch is `1.21.1` at `853739f3e580ebb85538e2fb7febd4fa4b5ddfcf`.
- Runtime behavior follows strict TDD: observe intended RED before production implementation.
- Current `SYSTEM_OBSERVED` world state remains the highest-authority factual context for a turn.
- BELIEF is never promoted by confidence, ranking, repetition, recency, or corroboration count.
- Player isolation is an eligibility decision before candidate limiting and ranking, not merely a relevance penalty.
- NPC-global entries/events remain eligible.
- Existing stored memories are retained; no contradiction-driven delete/rewrite is introduced.
- No new persistence file, schema version, config field, migration, dual-reader, embeddings, vector DB, second provider call, or LLM conflict resolver.
- Existing classic/legacy `PlayerModule.apply(...)` may retain `MemoryModule`; snapshot capture must use a no-memory player-context path.
- Root `CHANGELOG.md` `[Unreleased]` must be updated in the runtime PR.
- Final exact PR head must pass common tests, repository security, selected GameTests, Fabric + NeoForge, production startup/restart, persistence recovery, package smoke, production soak, release dry-run with publication skipped, and independent review with P0/P1/P2=0.

---

## File Structure

### New production unit

- `common/src/main/java/net/conczin/mca/livingworld/memory2/PlayerScopedMemoryEligibility.java`
  - Pure server-side eligibility predicates shared by semantic and episodic prompt retrieval.
  - No I/O, persistence, ranking, mutation, or provider behavior.

### Existing production units to modify

- `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryStore.java`
  - Add bounded predicate filtering before limit, mirroring `MemoryEventStore.getRecentMatching(...)`.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryRetriever.java`
  - Add ranking of an already bounded eligible candidate list without changing ranking weights.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryContextProvider.java`
  - Apply exact player/global eligibility before candidate limit.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryRetriever.java`
  - Add ranking of an already bounded eligible candidate list without changing ranking weights.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/Memory2ContextProvider.java`
  - Use existing `MemoryEventStore.getRecentMatching(...)` to filter before candidate limit.
- `common/src/main/java/net/conczin/mca/entity/ai/chatAI/modules/PlayerModule.java`
  - Split player context capture from legacy `MemoryModule` side effect.
- `common/src/main/java/net/conczin/mca/livingworld/context/LivingWorldContextCapture.java`
  - Use player-context-only path during immutable snapshot capture.
- `common/src/main/java/net/conczin/mca/livingworld/context/SnapshotContextPromptPolicy.java`
  - Render all four authority-bearing snapshot layers exactly once in deterministic order.
- `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java`
  - Replace direct world-fact rendering + mixin lore insertion with direct layered policy composition.
- `common/src/main/resources/mca.mixins.json`
  - Remove `MixinOpenAIChatAI` registration once direct composition is active.
- Delete `common/src/main/java/net/conczin/mca/mixin/MixinOpenAIChatAI.java`
  - Obsolete after direct layered composition.
- `CHANGELOG.md`
  - Record privacy/retrieval/prompt-precedence guarantees in `[Unreleased]`.

### Tests to modify/create

- `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryRetrieverTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/memory2/MemoryRetrieverTest.java`
- Create `common/src/test/java/net/conczin/mca/livingworld/memory2/PlayerScopedMemoryEligibilityTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/context/SnapshotContextPromptPolicyTest.java`
- Create `common/src/test/java/net/conczin/mca/livingworld/context/SnapshotMemoryWiringPolicyTest.java`
- Create `common/src/test/java/net/conczin/mca/entity/ai/chatAI/SnapshotLayeredPromptWiringPolicyTest.java`

---

### Task 1: Semantic exact-player eligibility before candidate limiting

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/PlayerScopedMemoryEligibility.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryStore.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryRetriever.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryContextProvider.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryRetrieverTest.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/memory2/PlayerScopedMemoryEligibilityTest.java`

**Interfaces:**
- Produces: `PlayerScopedMemoryEligibility.semantic(SemanticMemoryEntry entry, UUID npcId, UUID playerId) -> boolean`.
- Produces: package-private `SemanticMemoryStore.getRecentMatching(UUID npcId, int maxResults, Predicate<SemanticMemoryEntry> predicate)`.
- Produces: package-private `SemanticMemoryRetriever.rankCandidates(List<SemanticMemoryEntry> candidates, SemanticMemoryQuery query)`.
- Existing public `SemanticMemoryRetriever.retrieve(store, query)` remains source-compatible.

- [ ] **Step 1: Write semantic isolation RED tests only**

Add provider-level tests that create one NPC with current-player, NPC-global, and foreign-player entries. Include a starvation case where 32 newer foreign entries would occupy all current candidates under the old implementation.

```java
@Test
void contextProviderFiltersForeignPlayerBeforeCandidateLimit() {
    UUID npc = UUID.randomUUID();
    UUID currentPlayer = UUID.randomUUID();
    UUID foreignPlayer = UUID.randomUUID();
    SemanticMemoryStore store = SemanticMemoryStore.forWorld(tempDir);

    SemanticMemoryEntry eligible = new SemanticMemoryEntry(
            UUID.randomUUID(), npc, SemanticMemoryEntry.Kind.BELIEF,
            "eligible-current-player", List.of(currentPlayer), MemoryEvent.Provenance.PLAYER_TOLD,
            1L, 1_700_000_000_001L, 20, 30, List.of(UUID.randomUUID())
    );
    store.append(eligible, 128);

    for (int i = 0; i < 32; i++) {
        store.append(new SemanticMemoryEntry(
                UUID.randomUUID(), npc, SemanticMemoryEntry.Kind.BELIEF,
                "foreign-" + i, List.of(foreignPlayer), MemoryEvent.Provenance.PLAYER_TOLD,
                100L + i, 1_700_000_001_000L + i, 100, 100, List.of(UUID.randomUUID())
        ), 128);
    }

    List<String> context = SemanticMemoryContextProvider.load(tempDir, npc, currentPlayer, 200L);

    assertTrue(context.stream().anyMatch(line -> line.contains("eligible-current-player")));
    assertTrue(context.stream().noneMatch(line -> line.contains("foreign-")));
}
```

Also test NPC-global entry (`relatedEntities = List.of()`) remains eligible and pure predicate owner/player behavior.

- [ ] **Step 2: Run tests and record intended RED**

Run through the PR CI-bearing common test path after committing tests only. Expected failure: the current provider either returns foreign semantic entries or allows 32 foreign entries to starve the eligible entry. Do not change production before this RED is observed.

- [ ] **Step 3: Implement pure semantic eligibility and bounded filtered store read**

```java
public final class PlayerScopedMemoryEligibility {
    private PlayerScopedMemoryEligibility() {}

    public static boolean semantic(SemanticMemoryEntry entry, UUID npcId, UUID playerId) {
        if (entry == null || npcId == null || !npcId.equals(entry.ownerNpcId())) return false;
        if (entry.relatedEntities().isEmpty()) return true;
        return playerId != null && entry.relatedEntities().contains(playerId);
    }
}
```

Mirror `MemoryEventStore.getRecentMatching(...)` in `SemanticMemoryStore` so filtering occurs before `.limit(maxResults)`.

- [ ] **Step 4: Rank only eligible semantic candidates**

Add `rankCandidates(...)` that maps/sorts/limits a supplied bounded candidate list using the existing `RANKING`; do not modify weights or tie breakers. `SemanticMemoryContextProvider.load(...)` must call:

```java
List<SemanticMemoryEntry> candidates = SemanticMemoryStore.forWorld(worldRoot)
        .getRecentMatching(
                npcId,
                CANDIDATE_LIMIT,
                entry -> PlayerScopedMemoryEligibility.semantic(entry, npcId, playerId)
        );
List<RankedSemanticMemory> ranked = SemanticMemoryRetriever.rankCandidates(candidates, query);
```

- [ ] **Step 5: Run focused + full common tests and commit GREEN**

Expected: semantic foreign-player entries are absent, NPC-global/current-player entries remain, ranking of eligible entries is unchanged.

Commit message:

```text
fix: isolate player-scoped semantic retrieval
```

---

### Task 2: Episodic exact-player eligibility before candidate limiting

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory2/PlayerScopedMemoryEligibility.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryRetriever.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory2/Memory2ContextProvider.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/memory2/MemoryRetrieverTest.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/memory2/PlayerScopedMemoryEligibilityTest.java`

**Interfaces:**
- Consumes: `MemoryEventStore.getRecentMatching(...)` already exists.
- Produces: `PlayerScopedMemoryEligibility.episodic(MemoryEvent event, UUID npcId, UUID playerId) -> boolean`.
- Produces: package-private `MemoryRetriever.rankCandidates(List<MemoryEvent> candidates, MemoryQuery query)`.

- [ ] **Step 1: Write episodic isolation RED tests only**

Create current-player, NPC-global, and foreign-player events. Include `RELATIONSHIP_CHANGE`/`RELATIONSHIP_CAUSE` foreign-player cases and candidate starvation.

```java
@Test
void contextProviderFiltersForeignPlayerEventsBeforeCandidateLimit() {
    UUID npc = UUID.randomUUID();
    UUID currentPlayer = UUID.randomUUID();
    UUID foreignPlayer = UUID.randomUUID();
    MemoryEventStore store = new MemoryEventStore(tempDir.resolve("livingworld").resolve("memory2.json"));

    MemoryEvent eligible = event(
            UUID.randomUUID(), npc, MemoryEvent.Type.ACTION,
            Set.of(npc, currentPlayer), 1L, 20, 30
    );
    store.append(eligible, 128);

    for (int i = 0; i < 32; i++) {
        store.append(event(
                UUID.randomUUID(), npc, MemoryEvent.Type.RELATIONSHIP_CHANGE,
                Set.of(npc, foreignPlayer), 100L + i, 100, 100
        ), 128);
    }

    List<String> context = Memory2ContextProvider.load(tempDir, npc, currentPlayer, 200L);

    assertTrue(context.stream().anyMatch(line -> line.contains(eligible.summary())));
    assertTrue(context.stream().noneMatch(line -> line.contains(foreignPlayer.toString())));
}
```

Use deterministic summaries in fixtures so assertions do not depend on UUID text accidentally omitted from formatter output.

- [ ] **Step 2: Run tests and record intended RED**

Expected: old provider admits or is starved by foreign-player events. No production edit before observed RED.

- [ ] **Step 3: Implement episodic eligibility**

```java
public static boolean episodic(MemoryEvent event, UUID npcId, UUID playerId) {
    if (event == null || npcId == null || !npcId.equals(event.ownerNpcId())) return false;
    boolean hasExternalParticipant = event.participants().stream().anyMatch(id -> !npcId.equals(id));
    if (!hasExternalParticipant) return true;
    return playerId != null && event.participants().contains(playerId);
}
```

This treats no-external-participant events as NPC-global and fails closed when externally scoped events do not include the current player.

- [ ] **Step 4: Rank only bounded eligible episodic candidates**

`Memory2ContextProvider.load(...)` obtains candidates through existing `MemoryEventStore.getRecentMatching(...)` before `CANDIDATE_LIMIT`, then calls `MemoryRetriever.rankCandidates(...)` with unchanged weights/tie breakers.

- [ ] **Step 5: Run focused + full common tests and commit GREEN**

Expected: current-player/NPC-global events remain; foreign relationship/social history cannot enter another player's prompt; owner isolation remains unchanged.

Commit message:

```text
fix: isolate player-scoped episodic retrieval
```

---

### Task 3: Remove snapshot-side duplicate memory loading

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/modules/PlayerModule.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/context/LivingWorldContextCapture.java`
- Create test: `common/src/test/java/net/conczin/mca/livingworld/context/SnapshotMemoryWiringPolicyTest.java`

**Interfaces:**
- Produces: `PlayerModule.applySnapshotContext(List<String> input, VillagerEntityMCA villager, ServerPlayer player)`.
- Existing `PlayerModule.apply(...)` remains source-compatible and still calls `MemoryModule.apply(...)` after applying non-memory player context.

- [ ] **Step 1: Write structural wiring RED test only**

Use the repository's established source-policy-test pattern (`Files.readString(...)`). Require snapshot capture to call the new no-memory entrypoint and require legacy `apply(...)` to keep MemoryModule.

```java
@Test
void snapshotCaptureUsesPlayerContextWithoutMemorySideEffect() throws IOException {
    String capture = Files.readString(Path.of(
            "src/main/java/net/conczin/mca/livingworld/context/LivingWorldContextCapture.java"));
    String playerModule = Files.readString(Path.of(
            "src/main/java/net/conczin/mca/entity/ai/chatAI/modules/PlayerModule.java"));

    assertTrue(capture.contains("PlayerModule.applySnapshotContext(context, villager, player)"));
    assertFalse(capture.contains("PlayerModule.apply(context, villager, player)"));
    assertTrue(playerModule.contains("public static void applySnapshotContext"));
    assertTrue(playerModule.contains("MemoryModule.apply(input, villager, player)"));
}
```

- [ ] **Step 2: Run test and record compile/assertion RED**

Expected: `applySnapshotContext` does not yet exist and snapshot capture still calls `PlayerModule.apply(...)`.

- [ ] **Step 3: Extract no-memory player context**

Refactor without behavior change to classic callers:

```java
public static void apply(List<String> input, VillagerEntityMCA villager, ServerPlayer player) {
    applySnapshotContext(input, villager, player);
    MemoryModule.apply(input, villager, player);
}

public static void applySnapshotContext(List<String> input, VillagerEntityMCA villager, ServerPlayer player) {
    // existing advancement/player-context logic only
}
```

Then change only snapshot capture to `PlayerModule.applySnapshotContext(...)`.

- [ ] **Step 4: Run focused/full common tests and commit GREEN**

Confirm `LivingWorldContextSnapshot.memoryContext()` and `.semanticMemoryContext()` remain populated by their dedicated loaders and classic `PlayerModule.apply(...)` still carries MemoryModule.

Commit message:

```text
refactor: isolate snapshot memory capture
```

---

### Task 4: Centralize deterministic layered snapshot prompt composition

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/context/SnapshotContextPromptPolicy.java`
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java`
- Modify: `common/src/main/resources/mca.mixins.json`
- Delete: `common/src/main/java/net/conczin/mca/mixin/MixinOpenAIChatAI.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/context/SnapshotContextPromptPolicyTest.java`
- Create: `common/src/test/java/net/conczin/mca/entity/ai/chatAI/SnapshotLayeredPromptWiringPolicyTest.java`

**Interfaces:**
- Produces overload:

```java
SnapshotContextPromptPolicy.compose(
        List<String> worldFacts,
        List<String> operatorAuthoredContext,
        List<String> semanticMemoryContext,
        List<String> episodicMemoryContext
) -> String
```

- Existing two-argument `compose(worldFacts, operatorAuthoredContext)` delegates with empty memory lists for source compatibility.

- [ ] **Step 1: Write prompt-order RED tests only**

Pure policy test requires exact ordering and labels:

```java
@Test
void layeredPromptUsesFixedAuthorityOrderExactlyOnce() {
    String prompt = SnapshotContextPromptPolicy.compose(
            List.of("Observed weather: rain."),
            List.of("Server-authored world lore:\nUsually sunny."),
            List.of("BELIEF | provenance=PLAYER_TOLD | confidence=100 | statement=\"It is sunny.\""),
            List.of("VERIFIED | provenance=SYSTEM_OBSERVED | type=RELATIONSHIP_CHANGE | confidence=100 | summary=\"Old trust state.\"")
    );

    int fact = prompt.indexOf("Observed weather: rain.");
    int lore = prompt.indexOf("Usually sunny.");
    int semantic = prompt.indexOf("It is sunny.");
    int episodic = prompt.indexOf("Old trust state.");

    assertTrue(fact >= 0);
    assertTrue(lore > fact);
    assertTrue(semantic > lore);
    assertTrue(episodic > semantic);
    assertEquals(fact, prompt.lastIndexOf("Observed weather: rain."));
    assertEquals(semantic, prompt.lastIndexOf("It is sunny."));
}
```

Structural test requires `OpenAIChatAI.buildSnapshotSystem(...)` to call the four-layer policy before `The reply MUST be in this JSON format`, and requires no `MixinOpenAIChatAI` registration.

- [ ] **Step 2: Run tests and record intended RED**

Expected: four-argument composition does not exist; OpenAI still renders world facts itself and lore is inserted by mixin.

- [ ] **Step 3: Implement four-layer policy**

`SnapshotContextPromptPolicy` appends:

1. observed facts using the existing authoritative wording;
2. Operator Lore using existing background/current-facts-win wording;
3. `SemanticMemoryContextFormatter.promptSection(semanticMemoryContext)`;
4. `MemoryContextFormatter.promptSection(episodicMemoryContext)`.

Do not alter the semantic/episodic formatter truth labels.

- [ ] **Step 4: Wire direct policy composition into OpenAIChatAI**

After stable `contextLines`, child/relative/language instructions and before structured-response instructions:

```java
systemBuilder.append(SnapshotContextPromptPolicy.compose(
        snapshot.worldFacts(),
        snapshot.operatorAuthoredContext(),
        snapshot.semanticMemoryContext(),
        snapshot.memoryContext()
));
```

Remove the existing direct `worldFacts` block so each layer is rendered once.

- [ ] **Step 5: Remove obsolete lore mixin active path**

Delete `MixinOpenAIChatAI.java` and remove only `"MixinOpenAIChatAI",` from `mca.mixins.json`. Do not touch unrelated mixins/refmap behavior.

- [ ] **Step 6: Run focused/full common tests and commit GREEN**

Expected: exact order observed facts → lore → semantic → episodic → structured instructions; no duplicate memory/lore sections.

Commit message:

```text
feat: enforce layered prompt precedence
```

---

### Task 5: Conflict regression package, changelog, and delivery gates

**Files:**
- Test: `common/src/test/java/net/conczin/mca/livingworld/context/SnapshotContextPromptPolicyTest.java`
- Test: `common/src/test/java/net/conczin/mca/entity/ai/chatAI/SnapshotLayeredPromptWiringPolicyTest.java`
- Test: semantic/episodic retrieval tests from Tasks 1-2
- Modify: `CHANGELOG.md`

**Interfaces:**
- No new persistence or provider interfaces.
- This task freezes the behavior delivered by Tasks 1-4.

- [ ] **Step 1: Add conflict regression tests**

Cover all approved conflicts:

```text
current world FACT vs PLAYER_TOLD BELIEF
current relationship worldFact vs stale RELATIONSHIP_CHANGE/RELATIONSHIP_CAUSE
current observation vs conflicting Operator Lore
BELIEF vs BELIEF remains two BELIEF lines
foreign player semantic + episodic records absent
```

Assertions must verify structure and truth labels, not model-generated prose.

- [ ] **Step 2: Add request-wiring regression**

Use structural source-policy evidence to prove the actual snapshot system string is composed from the dedicated fields and inserted before the JSON response marker. If an existing deterministic mock-provider harness exposes the posted request body directly, add an integration assertion there; otherwise do not introduce a second HTTP harness solely for this slice.

- [ ] **Step 3: Run full common suite**

Expected: all existing tests plus new privacy/precedence regressions PASS; no baseline assertion weakened.

- [ ] **Step 4: Update canonical root changelog**

Under `[Unreleased]`, record:

```text
- Enforce exact current-player-or-NPC-global eligibility before bounded episodic/Semantic Memory retrieval so foreign-player memories cannot consume candidate slots or enter another player's AI prompt.
- Compose snapshot context exactly once in deterministic authority order: current observed facts, Operator Lore, Semantic Memory, then episodic/social history; current observations remain authoritative and conflicting BELIEFs remain non-authoritative.
```

Do not edit `docs/CHANGELOG.md` for new product history.

- [ ] **Step 5: Commit stabilization**

Commit message:

```text
test: lock FACT-over-BELIEF prompt precedence
```

- [ ] **Step 6: Open/update draft PR and run exact-head mandatory gates**

Require on the same immutable head:

```text
Repository security policy          SUCCESS
VillAIgence CI                       SUCCESS
Production Soak                      SUCCESS
GitHub Release dry-run               SUCCESS
github-release publication           SKIPPED
```

Main CI must include common tests, selected server GameTests, Fabric + NeoForge, production startup/restart, persistence recovery, and package verification.

- [ ] **Step 7: Independent diff review**

Review base `853739f3e580ebb85538e2fb7febd4fa4b5ddfcf` → exact PR head for:

- privacy leakage via player-scoped retrieval;
- scope ambiguity with empty/non-empty related entities/participants;
- candidate filtering happening after limit;
- duplicated snapshot memory/lore rendering;
- authority wording accidentally promoting BELIEF/lore;
- provider/schema/action/relationship mutation regressions;
- persistence/schema/config changes outside scope;
- stale `MixinOpenAIChatAI` registration/refmap failure.

Any behavioral blocker requires a new tests-only RED before production fix.

- [ ] **Step 8: Merge only exact verified head**

Require zero unresolved review threads and P0/P1/P2 = 0. Squash merge with `expected_head_sha` equal to the exact fully-green reviewed head.

- [ ] **Step 9: Documentation-only state handoff after merge**

Create a separate docs-only PR updating `docs/PROJECT_STATE.md` and `docs/ROADMAP.md` with the actual merge SHA, exact gate evidence, and next product slice. Do not promote unreleased automation to installed `0.2.0` evidence.
