# Memory 2.0 Working + Semantic Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add bounded Working Memory orchestration and an explicit persistent Semantic FACT/BELIEF layer while preserving server authority and text/voice parity.

**Architecture:** Keep episodic `MemoryEvent` storage unchanged, add a separate typed semantic store with strict provenance invariants, then compose recent dialogue + selected episodic + selected semantic context into one immutable turn-local Working Memory. Both OpenAI dialogue routes use the same bounds; snapshot/voice captures long-term context on the server thread.

**Tech Stack:** Java 21, JUnit 5, Gson, Gradle, Minecraft 1.21.1, Fabric + NeoForge CI.

## Global Constraints

- `LLM != authority`; server-owned world facts always win.
- `FACT` requires `SYSTEM_OBSERVED`; told/inferred provenance can only be `BELIEF`.
- No embeddings, vector DB, LLM semantic extraction, decay, consolidation, rumor propagation, or legacy `memory.json` migration in this slice.
- `memory.json` and `memory2.json` formats remain unchanged.
- Working Memory is bounded and non-persistent.
- Auxiliary memory failures fail soft and must not suppress a valid dialogue reply or authoritative gameplay mutation.

---

### Task 1: Define semantic memory invariants and persistence

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryEntry.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryStore.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryEntryTest.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryStoreTest.java`

**Interfaces:**
- Produces: `SemanticMemoryEntry`, `SemanticMemoryEntry.Kind`, `SemanticMemoryStore.forWorld(Path)`, `append(...)`, `getRecent(...)`.

- [ ] **Step 1: Write failing tests** proving FACT/BELIEF provenance invariants, normalization/clamping, defensive copies, persistence, per-NPC isolation, UUID idempotency, bounded retention, and malformed-file fail-open recovery.
- [ ] **Step 2: Run targeted tests** with `./gradlew :common:test --tests '*SemanticMemoryEntryTest' --tests '*SemanticMemoryStoreTest'`; expected RED because production types do not exist.
- [ ] **Step 3: Implement minimal immutable model and store** mirroring the existing Memory 2.0 atomic/fail-open persistence pattern, using `<world>/livingworld/semantic-memory.json`.
- [ ] **Step 4: Re-run targeted tests**; expected PASS.
- [ ] **Step 5: Commit** `feat: add typed semantic memory persistence`.

### Task 2: Add deterministic semantic retrieval and prompt formatting

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryQuery.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/RankedSemanticMemory.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryRetriever.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryContextFormatter.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/SemanticMemoryContextProvider.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryRetrieverTest.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryContextFormatterTest.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/memory2/SemanticMemoryContextProviderTest.java`

**Interfaces:**
- Produces: deterministic bounded semantic retrieval and `SemanticMemoryContextProvider.load(Path, UUID, UUID, long)` returning prompt-safe formatted lines.

- [ ] **Step 1: Write failing tests** for ranking weights, deterministic ties, hard candidate/result bounds, current-player relevance, escaping/control-character normalization, FACT/BELIEF labels, and prompt truth/instruction language.
- [ ] **Step 2: Run targeted tests**; expected RED because retrieval/formatter/provider types do not exist.
- [ ] **Step 3: Implement minimal deterministic query/ranking/formatting/provider layer** with weights `40 relevance / 30 importance / 20 confidence / 10 recency`, candidate limit `32`, result limit `6`, horizon `168000` ticks.
- [ ] **Step 4: Re-run targeted tests**; expected PASS.
- [ ] **Step 5: Commit** `feat: add bounded semantic memory retrieval`.

### Task 3: Add Working Memory orchestration

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/WorkingMemoryMessage.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/WorkingMemoryContext.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/WorkingMemoryOrchestrator.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/WorkingMemoryPromptFormatter.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/memory2/WorkingMemoryOrchestratorTest.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/memory2/WorkingMemoryPromptFormatterTest.java`

**Interfaces:**
- Produces: `WorkingMemoryOrchestrator.compose(...)` returning immutable `WorkingMemoryContext` with max 12 dialogue messages, 1200 Unicode code points per message, 6 episodic entries, and 6 semantic entries.

- [ ] **Step 1: Write failing tests** for latest-message retention, code-point-safe truncation, null/blank filtering, independent episodic/semantic bounds, defensive copies, and layered prompt rendering.
- [ ] **Step 2: Run targeted tests**; expected RED because Working Memory types do not exist.
- [ ] **Step 3: Implement minimal pure orchestration and formatting classes** without persistence or provider calls.
- [ ] **Step 4: Re-run targeted tests**; expected PASS.
- [ ] **Step 5: Commit** `feat: add bounded working memory orchestration`.

### Task 4: Integrate both OpenAI dialogue routes without changing authority semantics

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/context/LivingWorldContextSnapshot.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/context/LivingWorldContextCapture.java`
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java`
- Modify: `common/src/test/java/net/conczin/mca/livingworld/context/LivingWorldContextSnapshotMemoryTest.java`
- Add targeted request/prompt tests under the existing `chatAI` or `memory2` test package as needed.

**Interfaces:**
- Consumes: `SemanticMemoryContextProvider`, `WorkingMemoryOrchestrator`, `WorkingMemoryPromptFormatter`.
- Produces: source-compatible snapshot semantic context and shared bounded Working Memory request construction for classic text and snapshot/voice OpenAI paths.

- [ ] **Step 1: Write failing integration tests** showing semantic context is separate/defensively copied, memory is rendered once, recent dialogue is bounded, and both routes preserve the same Working Memory limits.
- [ ] **Step 2: Run targeted tests**; expected RED against the old snapshot/request construction.
- [ ] **Step 3: Extend snapshot source-compatibly** with a separate semantic-context list and capture it fail-soft when `memory2Enabled`.
- [ ] **Step 4: Remove pre-rendered episodic prompt injection from generic `contextLines`** so memory sections cannot be duplicated.
- [ ] **Step 5: Integrate Working Memory in both OpenAI routes** while leaving provider selection, tool/action validation, relationship persistence, dialogue persistence, and Memory 2.0 ingestion lifecycles unchanged.
- [ ] **Step 6: Re-run targeted tests**; expected PASS.
- [ ] **Step 7: Commit** `feat: integrate layered working memory context`.

### Task 5: Document and verify the slice

**Files:**
- Modify: `docs/livingworld/MEMORY_2.md`
- Modify: `docs/PROJECT_STATE.md`
- Modify: `docs/CHANGELOG.md`

- [ ] **Step 1: Update Memory 2.0 architecture docs** with Working Memory, semantic storage, invariants, bounds, and explicit non-goals.
- [ ] **Step 2: Run full unit/build verification**: `./gradlew :common:test :fabric:build :neoforge:build` plus repository package verification workflow.
- [ ] **Step 3: Confirm CI** for both `VillAIgence CI` and `Java Pull Request CI with Gradle` is SUCCESS on the exact final PR head.
- [ ] **Step 4: Update `PROJECT_STATE.md` and `CHANGELOG.md`** only with claims supported by the exact final CI evidence; live-game behavior remains unvalidated until a separate real-server checkpoint.
- [ ] **Step 5: Commit** `docs: record working and semantic memory foundation`.
