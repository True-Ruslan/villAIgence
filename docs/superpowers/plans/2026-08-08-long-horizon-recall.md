# Long-Horizon Recall Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make important Semantic and episodic/social Memory 2.0 remain recallable across multi-day game time, bounded capacity pressure, multiple sessions and restart without weakening player isolation, provenance, current-truth precedence or hard storage/prompt bounds.

**Architecture:** Keep persistence bounded and deterministic. Semantic persistence keeps its existing retention policy; episodic persistence gains a deterministic durability policy only after an observed FIFO-pressure RED. Prompt candidate selection becomes bounded dual-tier (approximately 75% recent / 25% durable) after exact current-player/NPC-global eligibility, while existing rankers and snapshot authority layering remain authoritative.

**Tech Stack:** Java 21, JUnit 5, Gradle multi-project build, Fabric 1.21.1 primary distribution, NeoForge compile compatibility, Gson-backed world-local persistence, GitHub Actions production/server acceptance workflows.

## Global Constraints

- Base branch is `1.21.1`; implementation branch is `feat/long-horizon-recall`.
- Approved design is `docs/superpowers/specs/2026-08-08-long-horizon-recall-design.md`.
- Strict TDD: no production code for a behavior before an observed failing test for that behavior.
- Minecraft `gameTime`, never wall-clock age, controls memory-time reasoning.
- Candidate limit remains `32`; final prompt result limit remains `6` unless a separate RED proves a bound defective.
- Normal dual-tier budget is `24` recent + `8` durable; generic positive limits reserve `max(1, floor(limit / 4))` durable slots when `limit >= 2`.
- Exact current-player/NPC-global/shared eligibility happens before both recent and durable selection.
- Foreign-player memory consumes zero recent or durable candidate slots.
- Current observations → Operator Lore → Semantic Memory → episodic/social history remains the snapshot authority order.
- FACT remains `SYSTEM_OBSERVED`; told/inferred knowledge remains BELIEF. Survival/ranking never promotes BELIEF to FACT.
- No new persistence file, persistence format version, config field/version, legacy `memory.json` migration, embeddings, vector DB, background summarizer or extra LLM memory-management call.
- Official installed-release evidence remains `0.2.0+1.21.1` until a later explicit release/install acceptance.

---

## File Structure

### New production files expected only after their RED gates

- `common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryEventRetentionPolicy.java`
  - Pure episodic/social durability and bounded survivor selection.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/LongHorizonCandidateSelector.java`
  - Pure bounded recent+durable candidate union shared by Semantic and episodic domains if the generic interface stays inspectable and type-safe. If implementation proves opaque, use two small domain selectors instead and document that deviation before coding.

### Existing production files expected to change

- `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryStore.java`
  - Expose eligible persisted records to long-horizon selection without newest-first truncation before the selector.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryEventStore.java`
  - Replace FIFO pressure with `MemoryEventRetentionPolicy` after RED 2; expose eligible persisted records without newest-first truncation before long-horizon selection after RED 3.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryContextProvider.java`
  - Apply eligibility, dual-tier candidate selection, unchanged final semantic ranking/formatting.
- `common/src/main/java/net/conczin/mca/livingworld/memory2/Memory2ContextProvider.java`
  - Apply eligibility, dual-tier candidate selection, unchanged final episodic ranking/formatting.
- `CHANGELOG.md`
  - Record only behavior actually implemented and verified.
- `docs/PROJECT_STATE.md`, `docs/ROADMAP.md`
  - Reconcile only after runtime PR is fully verified/merged; keep release-vs-unreleased evidence exact.

### Tests

- `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryRetrieverTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryStoreTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/memory2/MemoryEventRetentionPolicyTest.java` (new after RED 2 contract is expressed)
- `common/src/test/java/net/conczin/mca/livingworld/memory2/MemoryEventStoreTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/memory2/MemoryRetrieverTest.java`
- `common/src/test/java/net/conczin/mca/livingworld/memory2/LongHorizonCandidateSelectorTest.java` (new when selector API is introduced through a failing test)
- existing snapshot/prompt policy tests for authority regressions.

---

### Task 1: Semantic retained-but-starved recall

**Files:**
- Modify test only first: `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryRetrieverTest.java`
- Later modify: `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryStore.java`
- Later create/modify candidate selector and `SemanticMemoryContextProvider.java` only after RED.

**Interfaces:**
- Consumes: `SemanticMemoryStore.forWorld(Path)`, `SemanticMemoryContextProvider.load(Path, UUID, UUID, long)`, `PlayerScopedMemoryEligibility.semantic(...)`, existing semantic ranker.
- Produces after GREEN: bounded semantic candidate selection that can include an old durable eligible record despite more than 32 newer eligible records.

- [ ] **Step 1: Write the failing persistence-round-trip test**

Add a test equivalent to:

```java
@Test
void contextProviderRecallsOldDurableSemanticMemoryAfterNewerEligibleWindowAndReload() throws Exception {
    Path firstWorld = tempDir.resolve("world-a");
    UUID npc = UUID.fromString("00000000-0000-0000-0000-000000000101");
    UUID player = UUID.fromString("00000000-0000-0000-0000-000000000201");
    SemanticMemoryStore store = SemanticMemoryStore.forWorld(firstWorld);

    UUID durableId = UUID.fromString("00000000-0000-0000-0000-000000000301");
    store.append(entry(durableId, npc, player, 1L, "old-durable-semantic", 100, 100), 64);
    for (int i = 0; i < 40; i++) {
        store.append(entry(
                new UUID(0L, 1_000L + i), npc, player, 200_000L + i,
                "new-weak-semantic-" + i, 0, 0
        ), 64);
    }

    assertTrue(store.getRecent(npc, 64).stream().anyMatch(value -> value.id().equals(durableId)));

    Path secondWorld = tempDir.resolve("world-b");
    Path source = firstWorld.resolve("livingworld/semantic-memory.json");
    Path target = secondWorld.resolve("livingworld/semantic-memory.json");
    Files.createDirectories(target.getParent());
    Files.copy(source, target);

    List<String> context = SemanticMemoryContextProvider.load(secondWorld, npc, player, 200_100L);
    assertTrue(context.stream().anyMatch(line -> line.contains("old-durable-semantic")));
}
```

The old record scores above the newer weak records if it reaches `SemanticMemoryRetriever`, so the expected failure isolates newest-only candidate starvation rather than final ranking.

- [ ] **Step 2: Verify RED on CI**

Commit only the test and required test imports. Open/update the draft PR to trigger CI.

Expected: compilation succeeds; `:common:test` fails specifically at `contextProviderRecallsOldDurableSemanticMemoryAfterNewerEligibleWindowAndReload` because the old record is persisted but excluded from the current newest-32 candidate window.

Record exact RED head SHA, workflow run ID, failing test and assertion reason in the PR body before production changes.

- [ ] **Step 3: Add the minimal candidate-selector contract through tests**

Create `LongHorizonCandidateSelectorTest` first. Desired API:

```java
static <T> List<T> select(
        List<T> eligible,
        int candidateLimit,
        Comparator<T> newestFirst,
        Comparator<T> durableFirst,
        Function<T, UUID> idExtractor
)
```

Required test cases before implementation:

```text
32 -> exactly 24 recent + up to 8 non-duplicate durable
limit 1 -> newest only
limit 2 -> 1 recent + 1 durable
union never exceeds limit
durable entries already in recent pool are not duplicated
input order does not change selected IDs
```

If this generic signature makes domain rules less inspectable, replace it before production implementation with two domain-specific pure selectors and keep identical behavioral tests.

- [ ] **Step 4: Verify selector RED**

Expected: test compilation fails because selector production API does not exist yet. This is an allowed second RED for the new pure unit after the behavioral starvation RED has already been observed.

- [ ] **Step 5: Implement minimal pure selector**

Use deterministic copies; never mutate input. For `candidateLimit >= 2`:

```java
int durableQuota = Math.max(1, candidateLimit / 4);
int recentQuota = candidateLimit - durableQuota;
```

Sort eligible records newest-first for the recent pool; exclude selected UUIDs; sort remaining records durable-first; take durable quota; combine and hard-limit to candidate limit.

- [ ] **Step 6: Wire Semantic provider minimally**

Do not change `SemanticMemoryRetriever` weights. Make the store expose eligible persisted entries before candidate truncation, then:

```text
exact semantic eligibility
→ dual-tier select at current gameTime
→ existing SemanticMemoryRetriever.rankCandidates
→ existing formatter
```

Semantic durable comparator must use existing `SemanticMemoryRetentionPolicy.effectiveRetentionScore(entry, gameTime)` descending, then importance descending, confidence descending, source count descending, gameTime descending, createdAt descending, UUID ascending.

- [ ] **Step 7: Verify GREEN**

Run full `:common:test` through CI. Expected: new starvation and selector tests pass, all existing semantic privacy/bounds/ranking tests remain green.

- [ ] **Step 8: Commit GREEN evidence**

Separate implementation commit. Record exact head/run in PR body.

---

### Task 2: Episodic/social pressure retention

**Files:**
- Test first: new `MemoryEventRetentionPolicyTest.java` and/or `MemoryEventStoreTest.java`.
- Production after RED: new `MemoryEventRetentionPolicy.java`, modify `MemoryEventStore.java`.

**Interfaces:**
- Consumes: immutable `MemoryEvent` fields only.
- Produces: `selectRetained(List<MemoryEvent>, int maxEvents, long nowGameTime)`, `durabilityScore(MemoryEvent)`, `effectiveRetentionScore(MemoryEvent,long)` package-visible for focused tests.

- [ ] **Step 1: Write failing store pressure test**

Use capacity `2`: old strong `RELATIONSHIP_CAUSE`, middle weak DIALOGUE, newest weak DIALOGUE. Current FIFO retains the two newest; desired behavior retains the old cause plus the stronger/newer remaining candidate.

Assert exact persisted survivor IDs after reopening `memory2.json`.

- [ ] **Step 2: Verify RED**

Expected: store test fails because current `MemoryEventStore.append()` sorts chronological and removes oldest.

Record exact RED evidence before production code.

- [ ] **Step 3: Write pure policy tests before policy implementation**

Lock these monotonic contracts:

```text
importance up => durability never down
confidence up => durability never down
abs(emotionalWeight) up => durability never down
SYSTEM_OBSERVED >= otherwise-equal told/inferred
RELATIONSHIP_CAUSE > RELATIONSHIP_CHANGE > OBSERVATION == ACTION > DIALOGUE
one decay step reduces effective score deterministically
future gameTime age clamps to zero
weak sufficiently old record can lose to newer weak/strong record
selection independent of input order
exact ties deterministic by UUID
returned persistence order stable: gameTime, createdAt, UUID ascending
```

- [ ] **Step 4: Verify policy RED**

Expected: compile failure because `MemoryEventRetentionPolicy` does not exist.

- [ ] **Step 5: Implement minimal policy**

Use only `importance`, `confidence`, `abs(emotionalWeight)`, `type`, `provenance`, `gameTime`. Numeric coefficients may be chosen minimally to satisfy the approved monotonic relations; add no hidden external state.

Recommended shape matching Semantic retention:

```java
static final long DECAY_STEP_TICKS = 36_000L;

static int durabilityScore(MemoryEvent event) {
    return importanceContribution
            + confidenceContribution
            + emotionalContribution
            + provenanceContribution
            + typeContribution;
}

static long effectiveRetentionScore(MemoryEvent event, long nowGameTime) {
    long age = Math.max(0L, Math.max(0L, nowGameTime) - event.gameTime());
    return (long) durabilityScore(event) * DECAY_STEP_TICKS - age;
}
```

Exact coefficients are accepted only after all relational tests pass and no ordinary event type is immortal.

- [ ] **Step 6: Wire store**

On append: duplicate check → copy previous state → add → compute `nowGameTime` as max persisted game time → `selectRetained` → replace list → save only when retained state changed.

Do not change JSON format/version.

- [ ] **Step 7: Add rejected-weak-no-rewrite regression**

Like Semantic store, capture file bytes before appending a weak immediately-evicted event and assert bytes remain identical.

- [ ] **Step 8: Verify GREEN**

Run common suite; exact old-important survivor test and all prior idempotency/recovery tests must pass.

---

### Task 3: Episodic retained-but-starved recall

**Files:**
- Test first: `MemoryRetrieverTest.java`.
- Production after RED: `MemoryEventStore.java`, `Memory2ContextProvider.java`, existing candidate selector.

**Interfaces:**
- Produces episodic dual-tier candidate selection using `MemoryEventRetentionPolicy.effectiveRetentionScore(event, currentGameTime)` for durable order.

- [ ] **Step 1: Write failing recall test**

Persist one old high-durability current-player or NPC-global `RELATIONSHIP_CAUSE`/`RELATIONSHIP_CHANGE`, then more than 32 newer eligible weak DIALOGUE events while store capacity is high enough to keep the old event. Perform a real persistence round-trip via copied world root and assert `Memory2ContextProvider.load(...)` includes the old summary.

- [ ] **Step 2: Verify RED**

Expected: old event remains persisted after Task 2 GREEN but is absent from prompt context because current provider truncates newest eligible events before ranking.

- [ ] **Step 3: Wire dual-tier episodic selection**

Eligibility first; recent comparator = existing newest-first event order; durable comparator = effective episodic retention score descending followed by importance, confidence, absolute emotional weight, type durability, provenance durability, gameTime descending, createdAt descending, UUID ascending.

Do not change `MemoryRetriever` ranking weights.

- [ ] **Step 4: Verify GREEN**

Existing hard bounds, relevance, privacy, NPC-global/shared visibility and stable ranking tests must remain green.

---

### Task 4: Multi-session and restart determinism

**Files:**
- Extend Semantic/episodic store and retriever tests.

- [ ] **Step 1: Add multi-day fixtures**

Use deterministic UUIDs and game times spanning several multiples of `36_000` ticks. Persist, reopen directly, copy to a second world root, retrieve again.

- [ ] **Step 2: Assert exact IDs**

Assert exact retained IDs, exact candidate IDs (through selector-focused tests), and exact ranked IDs before/after reload.

- [ ] **Step 3: Verify tests**

If existing GREEN implementation already passes, no production change. If a deterministic/reload defect appears, treat that failing test as RED and make the smallest fix.

---

### Task 5: Privacy under long-horizon pressure

**Files:**
- Extend `SemanticMemoryRetrieverTest.java` and `MemoryRetrieverTest.java`.

- [ ] **Step 1: Construct mixed-scope pressure fixtures**

Each domain must include two NPCs, current player, foreign player, NPC-global memory, current-player private memory, foreign-only private memory and shared current-player-plus-other-entity memory. Add enough data to exercise both recent and durable pools.

- [ ] **Step 2: Assert eligibility before both pools**

For current player context:

```text
foreign-only summaries absent
current-player summaries present when competitively ranked
NPC-global present when competitively ranked
shared current-player scope remains eligible
other NPC data absent
```

Use deliberately high durability on foreign-only entries so a selector that filters after durable selection would fail.

- [ ] **Step 3: Verify GREEN/no-change or RED→minimal fix**

No privacy fix may alter FACT/BELIEF provenance or ranking weights.

---

### Task 6: Current-truth authority regressions

**Files:**
- Extend existing snapshot prompt policy tests, not provider/model tests unless wiring evidence is needed.

- [ ] **Step 1: Preserve conflict ordering**

Add/extend cases where stale long-horizon Semantic BELIEF and stale relationship/social history conflict with current observed world/relationship facts.

- [ ] **Step 2: Assert structural order**

Observed facts must appear before lore, semantic and episodic sections; BELIEF labels remain BELIEF; causal history does not factualize source dialogue.

- [ ] **Step 3: Verify**

Expected likely GREEN from PR #129. If already green, make no production change and retain the tests as regression evidence.

---

### Task 7: Deterministic long-running simulation

**Files:**
- Add focused common test class under `memory2`, e.g. `LongHorizonMemorySimulationTest.java`.

- [ ] **Step 1: Generate deterministic fixture**

Generate hundreds of Semantic and episodic records over multiple Minecraft days using deterministic UUIDs, no sleeps and no wall-clock-dependent assertions.

- [ ] **Step 2: Exercise pressure**

Keep stores bounded below generated volume; include strong old records, weak old/new records, several NPC/player scopes, relationship causes and ordinary dialogue.

- [ ] **Step 3: Repeat and reload**

Run logically identical generation into two world roots and compare exact survivor IDs and prompt context ordering. Reopen/copy stores and compare again.

- [ ] **Step 4: Verify**

Any nondeterminism is a new RED and must be fixed minimally before proceeding.

---

### Task 8: Changelog, CI, production acceptance and handoff

**Files:**
- Modify `CHANGELOG.md` only after runtime behavior is GREEN.
- Update PR body continuously with RED/GREEN evidence.
- Reconcile `docs/PROJECT_STATE.md` and `docs/ROADMAP.md` only after runtime PR is merged, via a separate docs handoff if following repository precedent.

- [ ] **Step 1: Update Unreleased changelog**

Document bounded dual-tier recall, episodic durability-aware pressure, preserved privacy/authority and unchanged formats/config/providers.

- [ ] **Step 2: Run exact-head repository gates**

Required success:

```text
repository security policy
full common/mock-provider tests
risk selector + required server GameTests
Fabric build
NeoForge compile/build
production startup/restart acceptance
selected persistence recovery
package verification
constrained production soak/restart cycles
release dry-run with publication skipped
```

- [ ] **Step 3: Independent exact-head review**

Review base→head for P0/P1/P2 issues, privacy ordering, boundedness, no persistence/config migration, no ranking/authority scope creep and accurate tests.

- [ ] **Step 4: Merge only after exact-head GREEN**

Do not convert automated candidate evidence into installed-release claims.

- [ ] **Step 5: Canonical docs reconciliation**

After merge, update `docs/PROJECT_STATE.md` / `docs/ROADMAP.md` with exact merge SHA and gate evidence; advance next product slice to NPC-to-NPC knowledge transfer only if long-horizon exit criteria are actually met.

---

## Self-Review Result

- Spec coverage: retention durability, recall durability, Semantic existing policy preservation, episodic pressure policy, 75/25 bounded selection, eligibility-before-selection, restart, privacy, truth precedence, deterministic simulation, CI/soak/release evidence and release-boundary documentation are all mapped to tasks.
- Placeholder scan: no TBD/TODO/"implement later" instructions remain; production steps have concrete APIs/ordering and verification expectations.
- Type consistency: existing `SemanticMemoryContextProvider`, `Memory2ContextProvider`, stores, rankers and `PlayerScopedMemoryEligibility` names match current source; proposed policy/selector names are introduced before later tasks consume them.
- Scope check: the work spans Semantic and episodic subsystems but they are intentionally one product slice because the approved long-horizon contract and shared candidate-selection/privacy semantics require both to be green before the feature has a meaningful exit criterion. Each subsystem still has its own independent RED→GREEN gate.
