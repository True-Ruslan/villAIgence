# FACT > BELIEF Retrieval Precedence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make current server-observed truth structurally authoritative over remembered/background context while preventing foreign-player episodic and semantic memory from entering the current player's prompt.

**Architecture:** Keep all persistence formats unchanged. Apply exact current-player-or-NPC-global eligibility before candidate limiting, preserve existing ranking only for eligible candidates, remove snapshot-side duplicate `MemoryModule` loading, and make `SnapshotContextPromptPolicy` the single deterministic renderer for observed facts, Operator Lore, semantic memory, and episodic memory before structured-response instructions.

**Tech Stack:** Java 21, Minecraft 1.21.1, Fabric + NeoForge, JUnit 5, Gson-backed world-local JSON stores, GitHub Actions production acceptance/recovery/soak/release dry-run.

## Global Constraints

- Base branch: `1.21.1`; design base commit: `853739f3e580ebb85538e2fb7febd4fa4b5ddfcf`.
- Runtime behavior follows strict TDD: observe intended RED before production implementation.
- Current `SYSTEM_OBSERVED` world state is the highest-authority factual context for a turn.
- BELIEF is never promoted by confidence, score, repetition, recency, or corroboration count.
- Player isolation is eligibility before candidate limiting/ranking, never only a relevance penalty.
- NPC-global entries/events remain eligible.
- Stored memories are retained; no contradiction-driven delete/rewrite is introduced.
- No new persistence file/schema version/config field/migration/dual reader/embedding/vector DB/second provider call/LLM conflict resolver.
- Legacy/classic `PlayerModule.apply(...)` keeps its existing `MemoryModule` behavior; snapshot capture uses a no-memory player-context path.
- Root `CHANGELOG.md` `[Unreleased]` changes in the runtime PR.
- Final exact PR head must pass common tests, repository security, selected GameTests, Fabric + NeoForge, production startup/restart, persistence recovery, package smoke, Production Soak, GitHub Release dry-run with publication skipped, and independent review with P0/P1/P2 = 0.

---

## File map

### New production file

- `common/src/main/java/net/conczin/mca/livingworld/memory2/PlayerScopedMemoryEligibility.java`
  - Pure semantic/episodic eligibility predicates; no I/O, ranking, persistence, mutation, or provider calls.

### Existing production files

- `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryStore.java`
  - Add predicate filtering before `limit`, mirroring `MemoryEventStore.getRecentMatching(...)`.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryRetriever.java`
  - Rank an already bounded eligible candidate list; weights/ties unchanged.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryContextProvider.java`
  - Enforce semantic player/global eligibility before candidate limit.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryRetriever.java`
  - Rank an already bounded eligible episodic candidate list; weights/ties unchanged.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/Memory2ContextProvider.java`
  - Use existing `MemoryEventStore.getRecentMatching(...)` before candidate limit.
- `common/src/main/java/net/conczin/mca/entity/ai/chatAI/modules/PlayerModule.java`
  - Separate player context from the legacy memory side effect.
- `common/src/main/java/net/conczin/mca/livingworld/context/LivingWorldContextCapture.java`
  - Use the player-context-only snapshot path.
- `common/src/main/java/net/conczin/mca/livingworld/context/SnapshotContextPromptPolicy.java`
  - Render four authority-bearing snapshot layers exactly once.
- `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java`
  - Directly compose the four layers before structured-response instructions.
- `common/src/main/resources/mca.mixins.json`
  - Remove `MixinOpenAIChatAI` registration.
- Delete `common/src/main/java/net/conczin/mca/mixin/MixinOpenAIChatAI.java`
  - Obsolete after direct composition.
- `CHANGELOG.md`
  - Record the privacy/retrieval/prompt-precedence guarantees.

### Tests

- `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryRetrieverTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/memory2/MemoryRetrieverTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/context/SnapshotContextPromptPolicyTest.java`
- Create `common/src/test/java/net/conczin/mca/livingworld/context/SnapshotMemoryWiringPolicyTest.java`
- Create `common/src/test/java/net/conczin/mca/entity/ai/chatAI/SnapshotLayeredPromptWiringPolicyTest.java`

---

## Task 1: Semantic player/global eligibility

**Files:**
- Test first: `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryRetrieverTest.java`
- Then create: `common/src/main/java/net/conczin/mca/livingworld/memory2/PlayerScopedMemoryEligibility.java`
- Then modify: `SemanticMemoryStore.java`, `SemanticMemoryRetriever.java`, `SemanticMemoryContextProvider.java`

**Produces:**

```java
public static boolean PlayerScopedMemoryEligibility.semantic(
        SemanticMemoryEntry entry,
        UUID npcId,
        UUID playerId
)
```

```java
synchronized List<SemanticMemoryEntry> SemanticMemoryStore.getRecentMatching(
        UUID npcId,
        int maxResults,
        Predicate<SemanticMemoryEntry> predicate
)
```

```java
static List<RankedSemanticMemory> SemanticMemoryRetriever.rankCandidates(
        List<SemanticMemoryEntry> candidates,
        SemanticMemoryQuery query
)
```

- [ ] **1.1 Write provider-level RED tests using only existing APIs**

Do not reference any new production class in the tests-only commit. Add cases for current player, NPC-global, foreign player, and pre-limit starvation.

```java
@Test
void contextProviderFiltersForeignPlayerBeforeCandidateLimit() {
    UUID npc = UUID.randomUUID();
    UUID currentPlayer = UUID.randomUUID();
    UUID foreignPlayer = UUID.randomUUID();
    SemanticMemoryStore store = SemanticMemoryStore.forWorld(tempDir);

    store.append(new SemanticMemoryEntry(
            UUID.randomUUID(), npc, SemanticMemoryEntry.Kind.BELIEF,
            "eligible-current-player", List.of(currentPlayer), MemoryEvent.Provenance.PLAYER_TOLD,
            1L, 1_700_000_000_001L, 20, 30, List.of(UUID.randomUUID())
    ), 128);

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

Add a second test with `relatedEntities = List.of()` proving NPC-global semantic memory remains visible.

- [ ] **1.2 Commit tests only and observe RED in CI**

Expected RED: current provider either returns foreign semantic entries or lets the 32 newer foreign entries consume the candidate window and starve the eligible entry. Reject infrastructure/policy failures as TDD evidence.

- [ ] **1.3 Implement minimal semantic eligibility**

```java
public final class PlayerScopedMemoryEligibility {
    private PlayerScopedMemoryEligibility() {
    }

    public static boolean semantic(SemanticMemoryEntry entry, UUID npcId, UUID playerId) {
        if (entry == null || npcId == null || !npcId.equals(entry.ownerNpcId())) return false;
        if (entry.relatedEntities().isEmpty()) return true;
        return playerId != null && entry.relatedEntities().contains(playerId);
    }
}
```

Mirror `MemoryEventStore.getRecentMatching(...)` in `SemanticMemoryStore`, with `.filter(predicate)` before `.limit(maxResults)`.

- [ ] **1.4 Rank only the bounded eligible candidates**

`SemanticMemoryRetriever.rankCandidates(...)` uses the existing `rank(...)`, `RANKING`, and `query.maxResults()` unchanged. `SemanticMemoryContextProvider.load(...)` becomes:

```java
SemanticMemoryStore store = SemanticMemoryStore.forWorld(worldRoot);
List<SemanticMemoryEntry> candidates = store.getRecentMatching(
        npcId,
        CANDIDATE_LIMIT,
        entry -> PlayerScopedMemoryEligibility.semantic(entry, npcId, playerId)
);
List<RankedSemanticMemory> ranked = SemanticMemoryRetriever.rankCandidates(candidates, query);
return SemanticMemoryContextFormatter.format(ranked);
```

- [ ] **1.5 Run focused/full common tests and commit GREEN**

Expected: foreign semantic memory is absent, current-player/NPC-global memory remains, ranking among eligible entries is unchanged.

Commit: `fix: isolate player-scoped semantic retrieval`

---

## Task 2: Episodic player/global eligibility

**Files:**
- Test first: `common/src/test/java/net/conczin/mca/livingworld/memory2/MemoryRetrieverTest.java`
- Then modify: `PlayerScopedMemoryEligibility.java`, `MemoryRetriever.java`, `Memory2ContextProvider.java`

**Produces:**

```java
public static boolean PlayerScopedMemoryEligibility.episodic(
        MemoryEvent event,
        UUID npcId,
        UUID playerId
)
```

```java
static List<RankedMemory> MemoryRetriever.rankCandidates(
        List<MemoryEvent> candidates,
        MemoryQuery query
)
```

- [ ] **2.1 Write episodic RED tests using only existing APIs**

Use deterministic summaries, not foreign UUID text, for assertions. Add current-player, NPC-global, foreign-player `RELATIONSHIP_CHANGE`, foreign-player `RELATIONSHIP_CAUSE`, and 32-foreign-entry starvation cases.

```java
@Test
void contextProviderFiltersForeignPlayerEventsBeforeCandidateLimit() {
    UUID npc = UUID.randomUUID();
    UUID currentPlayer = UUID.randomUUID();
    UUID foreignPlayer = UUID.randomUUID();
    MemoryEventStore store = MemoryEventStore.forWorld(tempDir);

    store.append(eventWithSummary(
            UUID.randomUUID(), npc, MemoryEvent.Type.ACTION,
            Set.of(npc, currentPlayer), 1L, 20, 30, "eligible-current-player"
    ), 128);

    for (int i = 0; i < 32; i++) {
        store.append(eventWithSummary(
                UUID.randomUUID(), npc, MemoryEvent.Type.RELATIONSHIP_CHANGE,
                Set.of(npc, foreignPlayer), 100L + i, 100, 100, "foreign-" + i
        ), 128);
    }

    List<String> context = Memory2ContextProvider.load(tempDir, npc, currentPlayer, 200L);

    assertTrue(context.stream().anyMatch(line -> line.contains("eligible-current-player")));
    assertTrue(context.stream().noneMatch(line -> line.contains("foreign-")));
}
```

- [ ] **2.2 Commit tests only and observe RED**

Expected RED: current provider admits/starves on foreign-player events because participant mismatch is only a relevance penalty.

- [ ] **2.3 Implement episodic eligibility**

```java
public static boolean episodic(MemoryEvent event, UUID npcId, UUID playerId) {
    if (event == null || npcId == null || !npcId.equals(event.ownerNpcId())) return false;
    boolean hasExternalParticipant = event.participants().stream()
            .anyMatch(id -> !npcId.equals(id));
    if (!hasExternalParticipant) return true;
    return playerId != null && event.participants().contains(playerId);
}
```

- [ ] **2.4 Filter before the existing candidate limit and rank**

```java
MemoryEventStore store = MemoryEventStore.forWorld(worldRoot);
List<MemoryEvent> candidates = store.getRecentMatching(
        npcId,
        CANDIDATE_LIMIT,
        event -> PlayerScopedMemoryEligibility.episodic(event, npcId, playerId)
);
List<RankedMemory> ranked = MemoryRetriever.rankCandidates(candidates, query);
return MemoryContextFormatter.format(ranked);
```

Do not change relevance weights, recency, confidence, importance, or tie ordering.

- [ ] **2.5 Run focused/full common tests and commit GREEN**

Commit: `fix: isolate player-scoped episodic retrieval`

---

## Task 3: Remove snapshot duplicate memory loading

**Files:**
- Test first: create `common/src/test/java/net/conczin/mca/livingworld/context/SnapshotMemoryWiringPolicyTest.java`
- Then modify: `PlayerModule.java`, `LivingWorldContextCapture.java`

**Produces:**

```java
public static void PlayerModule.applySnapshotContext(
        List<String> input,
        VillagerEntityMCA villager,
        ServerPlayer player
)
```

- [ ] **3.1 Write source-wiring RED test**

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
}
```

- [ ] **3.2 Commit tests only and observe RED**

Expected RED: snapshot capture still calls `PlayerModule.apply(...)` and no `applySnapshotContext(...)` exists.

- [ ] **3.3 Extract the non-memory player context without changing classic behavior**

```java
public static void apply(List<String> input, VillagerEntityMCA villager, ServerPlayer player) {
    applySnapshotContext(input, villager, player);
    MemoryModule.apply(input, villager, player);
}

public static void applySnapshotContext(List<String> input, VillagerEntityMCA villager, ServerPlayer player) {
    List<String> list = advancements.entrySet().stream()
            .filter(entry -> {
                AdvancementHolder advancement = Objects.requireNonNull(player.getServer())
                        .getAdvancements().get(entry.getKey());
                if (advancement == null) {
                    MCA.LOGGER.warn("Advancement {} not found.", entry.getKey());
                    return false;
                }
                return player.getAdvancements().getOrStartProgress(advancement).isDone();
            })
            .map(Map.Entry::getValue)
            .toList();

    if (!list.isEmpty()) {
        input.add("Player has completed the following advancements: ");
        for (String advancement : list) input.add(advancement + " ");
    }
}
```

Change snapshot capture only to `PlayerModule.applySnapshotContext(context, villager, player)`.

- [ ] **3.4 Run focused/full common tests and commit GREEN**

Verify dedicated snapshot `memoryContext` and `semanticMemoryContext` loaders are unchanged.

Commit: `refactor: isolate snapshot memory capture`

---

## Task 4: Centralize layered snapshot prompt composition

**Files:**
- Test first: modify `SnapshotContextPromptPolicyTest.java`; create `SnapshotLayeredPromptWiringPolicyTest.java`
- Then modify: `SnapshotContextPromptPolicy.java`, `OpenAIChatAI.java`, `mca.mixins.json`
- Then delete: `common/src/main/java/net/conczin/mca/mixin/MixinOpenAIChatAI.java`

**Produces:**

```java
public static String SnapshotContextPromptPolicy.compose(
        List<String> worldFacts,
        List<String> operatorAuthoredContext,
        List<String> semanticMemoryContext,
        List<String> episodicMemoryContext
)
```

The existing two-argument `compose(worldFacts, operatorAuthoredContext)` remains and delegates with empty memory lists.

- [ ] **4.1 Write four-layer policy RED test**

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

- [ ] **4.2 Write actual OpenAI wiring RED test**

Use the established `Files.readString(...)` structural-test pattern. Require the direct four-field policy call to occur before the structured-response schema marker, and require no active `MixinOpenAIChatAI` registration.

- [ ] **4.3 Commit tests only and observe RED**

Expected RED: four-argument policy API is missing; OpenAI still renders world facts directly and lore still arrives through a mixin.

- [ ] **4.4 Implement the four-layer renderer**

The four-argument policy appends in this exact order:

```java
appendObservedFacts(builder, worldFacts);
appendOperatorLore(builder, operatorAuthoredContext);
builder.append(SemanticMemoryContextFormatter.promptSection(semanticMemoryContext));
builder.append(MemoryContextFormatter.promptSection(episodicMemoryContext));
```

Do not change the formatter truth labels or instructions.

- [ ] **4.5 Wire direct composition in `OpenAIChatAI.buildSnapshotSystem(...)`**

After stable context lines + age/relationship/language instructions, and before structured-response instructions:

```java
systemBuilder.append(SnapshotContextPromptPolicy.compose(
        snapshot.worldFacts(),
        snapshot.operatorAuthoredContext(),
        snapshot.semanticMemoryContext(),
        snapshot.memoryContext()
));
```

Remove only the old direct `worldFacts` block. The request JSON schema/provider/retry path remains unchanged.

- [ ] **4.6 Remove the obsolete lore mixin path**

Delete `MixinOpenAIChatAI.java` and remove only `"MixinOpenAIChatAI",` from `mca.mixins.json`.

- [ ] **4.7 Run focused/full common tests and commit GREEN**

Expected order: observed facts → Operator Lore → Semantic Memory → episodic/social history → structured response instructions, each durable layer exactly once.

Commit: `feat: enforce layered prompt precedence`

---

## Task 5: Conflict regressions and delivery

**Files:**
- Tests from Tasks 1–4
- Modify `CHANGELOG.md`

- [ ] **5.1 Add regression tests for all approved conflict boundaries**

Assert structure/truth labels rather than generated model prose:

```text
current FACT vs PLAYER_TOLD BELIEF → both visible; FACT section first
current relationship factual summary vs stale relationship history → current state first
current observation vs Operator Lore → observation first
BELIEF vs BELIEF → both remain BELIEF; no FACT label appears
foreign player semantic/episodic memory → absent
RELATIONSHIP_CAUSE → historical process evidence only; no dialogue-to-FACT promotion
```

- [ ] **5.2 Verify the provider-facing snapshot string, not only isolated formatters**

Prefer an existing deterministic mock-provider request-capture harness if one already exists in the test suite. If none exists, use the source-wiring policy test plus the pure four-layer policy test; do not create a second network harness solely for this slice.

- [ ] **5.3 Run full common suite**

No existing assertion may be weakened to obtain GREEN.

- [ ] **5.4 Update root `[Unreleased]`**

Add concise entries equivalent to:

```text
- Enforce exact current-player-or-NPC-global eligibility before bounded episodic/Semantic Memory retrieval so foreign-player memories cannot consume candidate slots or enter another player's AI prompt.
- Compose snapshot context exactly once in deterministic authority order: current observed facts, Operator Lore, Semantic Memory, then episodic/social history; current observations remain authoritative and conflicting BELIEFs remain non-authoritative.
```

- [ ] **5.5 Commit stabilization**

Commit: `test: lock FACT-over-BELIEF prompt precedence`

- [ ] **5.6 Open/update draft PR and run the mandatory exact-head matrix**

Require on one immutable head:

```text
Repository security policy     SUCCESS
VillAIgence CI                  SUCCESS
Production Soak                 SUCCESS
GitHub Release dry-run          SUCCESS
github-release publication      SKIPPED
```

- [ ] **5.7 Independent changed-file review**

Review for: foreign-player leakage, scope ambiguity, filtering after limit, duplicate memory/lore rendering, accidental BELIEF/lore promotion, provider/schema/action/relationship regressions, persistence/config scope creep, and stale mixin/refmap registration. Any behavioral blocker gets a new tests-only RED before a production fix.

- [ ] **5.8 Merge only the exact fully green reviewed head**

Require zero unresolved review threads and P0/P1/P2 = 0. Squash merge using `expected_head_sha`.

- [ ] **5.9 Separate docs-only handoff**

After merge, update `docs/PROJECT_STATE.md` and `docs/ROADMAP.md` in a docs-only PR with the actual merge SHA and exact evidence. Keep `0.2.0+1.21.1` installed evidence unchanged until a later release candidate is explicitly accepted.
