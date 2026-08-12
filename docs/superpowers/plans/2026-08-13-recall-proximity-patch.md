# 0.3.1 Recall and Proximity Acceptance Patch Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix bounded same-owner recall of older relevant dialogue and correct the installed `VAI-PROX-MULTI-001` acceptance contract without changing proximity-target runtime behavior.

**Architecture:** Keep exact NPC/player owner filtering before any relevance ranking. Reserve bounded dialogue capacity for query-relevant older same-pair exchanges while retaining recent conversational continuity, pass the current player message into the retrieval path, and present selected exchanges chronologically. Acceptance documentation must reflect explicit target selection rather than movement-based retargeting.

**Tech Stack:** Java 21, JUnit 5, Gradle, Minecraft 1.21.1, Fabric/NeoForge compatibility, GitHub Actions.

## Global Constraints

- Base exactly on released `0.3.0+1.21.1` commit `d42141511c0c61f10256fd06576f977f2a784d1c`.
- Do not enlarge `WorkingMemoryOrchestrator.MAX_RECENT_DIALOGUE_MESSAGES`.
- Never rank across NPC owners or player identities; exact-pair filtering precedes selection.
- No proximity-based automatic target stealing may be introduced.
- Runtime changes follow RED -> GREEN TDD and full CI validation.
- Installed/manual evidence remains distinct from automated evidence.

---

### Task 1: Lock the recall regression with a failing test

**Files:**
- Modify: `common/src/test/java/net/conczin/mca/livingworld/memory2/Memory2DialogueHistoryTest.java`

**Interfaces:**
- Consumes: existing Memory 2.0 event store and dialogue ingestor.
- Produces: regression contract for bounded recent + relevant older same-pair recall.

- [ ] Add a test with more than the current exchange limit, an old same-pair `amber-orchid-731` exchange, newer unrelated same-pair turns, and a second player's private marker.
- [ ] Query with `What is my calibration phrase?` and require the old relevant exchange inside the same bounded message count.
- [ ] Require the other player's marker to remain absent.
- [ ] Commit test-only RED state and verify the targeted CI/test fails for the missing query-aware retrieval API/behavior.

### Task 2: Implement bounded query-aware same-pair retrieval

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory2/MemoryEventStore.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory2/Memory2DialogueHistory.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory/PersistentChatMemory.java`
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/memory2/Memory2DialogueHistoryTest.java`

**Interfaces:**
- Consumes: current player message plus exact NPC/player identifiers.
- Produces: at most `MAX_RECENT_DIALOGUE_MESSAGES`, with recent continuity and relevant older same-pair recall.

- [ ] Add an owner-scoped matching read that exposes eligible candidates to the selector without increasing prompt output.
- [ ] Add deterministic lexical relevance scoring and reserve at most one exchange slot for the best older relevant same-pair exchange when it is not already recent.
- [ ] Keep final selected exchanges chronological and bounded.
- [ ] Thread current `msg` through `OpenAIChatAI -> PersistentChatMemory -> Memory2DialogueHistory` for direct and snapshot paths.
- [ ] Preserve compatibility overloads where required.
- [ ] Run targeted tests and require GREEN, including exact-pair isolation.

### Task 3: Correct `VAI-PROX-MULTI-001` acceptance semantics

**Files:**
- Modify the canonical acceptance/test documentation that defines `VAI-PROX-MULTI-001`.
- Modify: `docs/livingworld/VALIDATION_0.3.0_CLEAN_WORLD_INSTALLED.md` if needed to record the discovered blocker without inventing PASS evidence.

**Interfaces:**
- Consumes: existing explicit voice/NPC selection behavior.
- Produces: manual contract: select A -> A remains target while moving near B -> explicitly select B -> B becomes target; never dual response/stale target/cross-busy.

- [ ] Remove/replace any movement-near-B expectation that implies automatic retargeting.
- [ ] Explicitly state that proximity alone must not steal an already selected target.
- [ ] Do not change runtime target-selection behavior.

### Task 4: Patch release documentation and verification

**Files:**
- Modify: `CHANGELOG.md`
- Modify/add release/validation documentation required by `docs/RELEASING.md` for `0.3.1+1.21.1`.

**Interfaces:**
- Consumes: verified GREEN implementation and corrected acceptance contract.
- Produces: auditable patch-release candidate.

- [ ] Record the recall fix and acceptance correction without claiming installed PASS.
- [ ] Run full CI, production acceptance, recovery, loader builds and release dry-run required by repository policy.
- [ ] Perform an independent read-only diff review; P0/P1/P2 must be zero before merge.
- [ ] Publish `0.3.1+1.21.1` only from the exact merged commit if all release gates pass.
- [ ] Leave post-publication installed acceptance pending until the official asset is actually tested.
