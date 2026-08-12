# Personality + Social Dialogue/Behavior Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make canonical MCA Personality affect bounded player↔NPC dialogue style and make the exact directed social state of an already-selected settlement NPC pair conservatively gate that pair's knowledge-sharing behavior, while preserving all server authority boundaries.

**Architecture:** Add pure closed influence types/policy plus a fixed-size guidance renderer. Integrate guidance into the existing snapshot prompt layer, gate settlement transfer only after deterministic pair selection using one exact graph read, and revalidate capture-time player command eligibility against fresh NPC×player relationship state on the server thread. No provider schema/call, persistence format, public config or autonomous scheduler change.

**Tech Stack:** Java 21, JUnit 5, Fabric 1.21.1 GameTest, Gradle, existing VillAIgence persistence/recovery and GitHub Actions gates.

## Global Constraints

- MCA `VillagerBrain.getPersonality()` remains canonical persistent personality authority.
- `npc-social-graph.json` remains canonical NPC↔NPC social authority; `relationships.json` remains NPC×player only.
- No graph enumeration or fallback retargeting.
- No provider-authored NPC social delta.
- Current observed facts, safety, permissions and server target validation outrank personality/social preference.
- No FACT/BELIEF authority, confidence, provenance, importance, eligibility or ranking changes.
- No provider request-count/schema, public config, persistence file/version, migration/backfill or release-identity change.
- Neutral/no-counterpart behavior remains compatible.
- Runtime behavior is tests-first; every production step follows observed intended RED.

---

### Task 1: Closed personality/social influence model

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/context/PersonalityDialogueStyle.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/context/NpcPairDisposition.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/context/PersonalitySocialInfluence.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/context/PersonalitySocialInfluencePolicy.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/context/PersonalitySocialInfluencePolicyTest.java`

**Interfaces:**
- Consumes: `PersonalitySocialSnapshot`, `NpcSocialState`.
- Produces: `PersonalitySocialInfluence evaluate(PersonalitySocialSnapshot)` and `NpcPairDisposition pairDisposition(NpcSocialState)`.

- [ ] **Step 1: Write failing closed-personality mapping tests**

Cover all canonical tokens exactly:

```java
assertEquals(PersonalityDialogueStyle.WARM, evaluate("friendly").personalityStyle());
assertEquals(PersonalityDialogueStyle.CHARMING, evaluate("flirty").personalityStyle());
assertEquals(PersonalityDialogueStyle.PLAYFUL, evaluate("playful").personalityStyle());
assertEquals(PersonalityDialogueStyle.GLOOMY, evaluate("gloomy").personalityStyle());
assertEquals(PersonalityDialogueStyle.GENTLE, evaluate("sensitive").personalityStyle());
assertEquals(PersonalityDialogueStyle.TRANSACTIONAL, evaluate("greedy").personalityStyle());
assertEquals(PersonalityDialogueStyle.ECCENTRIC, evaluate("odd").personalityStyle());
assertEquals(PersonalityDialogueStyle.GRUFF, evaluate("crabby").personalityStyle());
assertEquals(PersonalityDialogueStyle.OUTGOING, evaluate("extroverted").personalityStyle());
assertEquals(PersonalityDialogueStyle.RESERVED, evaluate("introverted").personalityStyle());
assertEquals(PersonalityDialogueStyle.CALM, evaluate("relaxed").personalityStyle());
assertEquals(PersonalityDialogueStyle.ANXIOUS, evaluate("anxious").personalityStyle());
assertEquals(PersonalityDialogueStyle.PEACEFUL, evaluate("peaceful").personalityStyle());
assertEquals(PersonalityDialogueStyle.CHEERFUL, evaluate("upbeat").personalityStyle());
assertEquals(PersonalityDialogueStyle.NEUTRAL, evaluate("unassigned").personalityStyle());
```

Also require `evaluate(null) == NEUTRAL/NEUTRAL` and unknown source token canonicalizes through `PersonalitySocialSnapshot` to NEUTRAL.

- [ ] **Step 2: Write failing pair-disposition boundary/priority tests**

Require exact boundaries:

```java
assertEquals(FEARFUL, pair(new NpcSocialState(100, 100, 75, 100)));
assertEquals(DISTRUSTFUL, pair(new NpcSocialState(-75, 100, 74, 100)));
assertEquals(ANTIPATHETIC, pair(new NpcSocialState(-74, 100, 74, -75)));
assertEquals(AFFILIATIVE, pair(new NpcSocialState(60, 0, 0, 60)));
assertEquals(RESPECTFUL, pair(new NpcSocialState(59, 70, 0, 59)));
assertEquals(NEUTRAL, pair(new NpcSocialState(59, 69, 74, 59)));
```

Include priority combinations proving FEARFUL > DISTRUSTFUL > ANTIPATHETIC > AFFILIATIVE > RESPECTFUL > NEUTRAL and asymmetric A→B/B→A state evaluates independently.

- [ ] **Step 3: Run focused tests and observe intended RED**

Run:

```bash
./gradlew :common:test --tests net.conczin.mca.livingworld.context.PersonalitySocialInfluencePolicyTest
```

Expected: compile RED because influence types/policy do not exist.

- [ ] **Step 4: Implement minimal pure types/policy**

Enums contain only closed constants from the design. `PersonalitySocialInfluence` normalizes null enum fields to NEUTRAL. Policy contains only deterministic switch/threshold logic and no I/O.

- [ ] **Step 5: Re-run focused tests for GREEN**

Run the same Gradle command. Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add common/src/main/java/net/conczin/mca/livingworld/context common/src/test/java/net/conczin/mca/livingworld/context/PersonalitySocialInfluencePolicyTest.java
git commit -m "feat: derive bounded personality social influence"
```

---

### Task 2: Fixed-size dialogue guidance renderer

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/context/PersonalitySocialDialogueGuidanceRenderer.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/context/PersonalitySocialDialogueGuidanceRendererTest.java`

**Interfaces:**
- Consumes: `PersonalitySocialInfluence`.
- Produces: `List<String> render(PersonalitySocialInfluence)` with size 0..2.

- [ ] **Step 1: Write failing renderer tests**

Require:

```text
NEUTRAL/NEUTRAL                         -> 0 lines
WARM/NEUTRAL                            -> 1 closed personality line
NEUTRAL/DISTRUSTFUL                     -> 1 closed pair line
CHEERFUL/AFFILIATIVE                    -> 2 lines
```

Assert every style/disposition renders only known server-authored text, no UUID, name, arbitrary token or numeric state. Require the personality line to contain: `This affects tone only; current-world facts, safety rules, permissions, and structured action validation take precedence.` Require pair line to state it does not change factual truth, memory authority or server action validation.

- [ ] **Step 2: Run renderer tests for compile RED**

```bash
./gradlew :common:test --tests net.conczin.mca.livingworld.context.PersonalitySocialDialogueGuidanceRendererTest
```

- [ ] **Step 3: Implement minimal deterministic renderer**

Use exhaustive enum switches. Do not accept free-form text parameters.

- [ ] **Step 4: Re-run focused tests for GREEN and commit**

```bash
./gradlew :common:test --tests net.conczin.mca.livingworld.context.PersonalitySocialDialogueGuidanceRendererTest
git add common/src/main/java/net/conczin/mca/livingworld/context/PersonalitySocialDialogueGuidanceRenderer.java common/src/test/java/net/conczin/mca/livingworld/context/PersonalitySocialDialogueGuidanceRendererTest.java
git commit -m "feat: render bounded personality dialogue guidance"
```

---

### Task 3: Centralized prompt integration without schema changes

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/context/SnapshotContextPromptPolicy.java`
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java`
- Modify: `common/src/test/java/net/conczin/mca/livingworld/context/SnapshotContextPromptPolicyTest.java`
- Modify: `common/src/test/java/net/conczin/mca/entity/ai/chatAI/SnapshotLayeredPromptWiringPolicyTest.java`

**Interfaces:**
- Add source-compatible overload:

```java
compose(
  List<String> worldFacts,
  List<String> personalitySocialContext,
  List<String> personalitySocialGuidance,
  List<String> operatorAuthoredContext,
  List<String> semanticMemoryContext,
  List<String> contradictionContext,
  List<String> episodicMemoryContext)
```

- [ ] **Step 1: Add failing authority-order tests**

Require exact order:

```text
Observed fact
< descriptive personality/social
< dialogue guidance
< Operator Lore
< Semantic Memory
< disagreement
< episodic/social history
```

Require old overload output to remain byte-identical when guidance is empty.

- [ ] **Step 2: Add failing wiring test for `OpenAIChatAI.buildSnapshotSystem`**

Require source wiring to evaluate and render `snapshot.personalitySocialSnapshot()` and pass guidance through centralized `SnapshotContextPromptPolicy`, not append ad hoc after structured response instructions.

- [ ] **Step 3: Run focused tests for RED**

```bash
./gradlew :common:test --tests net.conczin.mca.livingworld.context.SnapshotContextPromptPolicyTest --tests net.conczin.mca.entity.ai.chatAI.SnapshotLayeredPromptWiringPolicyTest
```

- [ ] **Step 4: Implement overload + OpenAI wiring**

`buildSnapshotSystem` computes:

```java
PersonalitySocialInfluence influence = PersonalitySocialInfluencePolicy.evaluate(snapshot.personalitySocialSnapshot());
List<String> guidance = PersonalitySocialDialogueGuidanceRenderer.render(influence);
```

and passes it into the new compose overload. Do not modify `StructuredResponse`, provider body shape, request count or response parser.

- [ ] **Step 5: GREEN + deterministic provider regression**

```bash
./gradlew :common:test --tests net.conczin.mca.livingworld.context.SnapshotContextPromptPolicyTest --tests net.conczin.mca.entity.ai.chatAI.SnapshotLayeredPromptWiringPolicyTest
./gradlew :common:test
```

- [ ] **Step 6: Commit**

```bash
git add common/src/main/java/net/conczin/mca/livingworld/context/SnapshotContextPromptPolicy.java common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java common/src/test/java/net/conczin/mca/livingworld/context/SnapshotContextPromptPolicyTest.java common/src/test/java/net/conczin/mca/entity/ai/chatAI/SnapshotLayeredPromptWiringPolicyTest.java
git commit -m "feat: apply personality influence to dialogue prompt"
```

---

### Task 4: Exact-pair settlement social gate

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/memory2/SettlementSocialKnowledgeSharingPolicy.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/memory2/SettlementSocialKnowledgeSharingPolicyTest.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/memory2/SettlementKnowledgeFlowLifecycle.java`
- Modify: `common/src/test/java/net/conczin/mca/livingworld/memory2/SettlementKnowledgeFlowLifecycleTest.java`

**Interfaces:**
- `SettlementSocialKnowledgeSharingPolicy.isAllowed(NpcPairDisposition)` returns false only for FEARFUL/DISTRUSTFUL/ANTIPATHETIC.
- Extend package-private `CycleResult` with `int sociallySuppressedTransfers`.

- [ ] **Step 1: Write pure policy RED tests**

Require block for exactly three adverse dispositions and allow for NEUTRAL/AFFILIATIVE/RESPECTFUL/null-as-neutral.

- [ ] **Step 2: Write lifecycle RED tests**

Build the same two-resident FACT fixture as `cycleDelegatesToExactNpcTransfer...`, seed `NpcSocialGraphStore` speaker→listener with each adverse state, then require:

```text
opportunities = 1
sociallySuppressedTransfers = 1
attemptedTransfers = 0
successfulTransfers = 0
statuses = []
listener semantic memory = []
listener NPC_TOLD dialogue evidence = absent
```

Neutral state must preserve current `1 attempted / 1 admitted`. Reverse-only listener→speaker hostility must not block speaker→listener, proving directionality.

- [ ] **Step 3: Run focused tests for RED**

```bash
./gradlew :common:test --tests net.conczin.mca.livingworld.memory2.SettlementSocialKnowledgeSharingPolicyTest --tests net.conczin.mca.livingworld.memory2.SettlementKnowledgeFlowLifecycleTest
```

- [ ] **Step 4: Implement exact-pair gate after selector**

For each already-selected `Opportunity`:

```java
NpcSocialState social;
try {
    social = NpcSocialGraphStore.forWorld(worldRoot).get(opportunity.speakerNpcId(), opportunity.listenerNpcId());
} catch (RuntimeException e) {
    sociallySuppressedTransfers++;
    continue;
}
NpcPairDisposition disposition = PersonalitySocialInfluencePolicy.pairDisposition(social);
if (!SettlementSocialKnowledgeSharingPolicy.isAllowed(disposition)) {
    sociallySuppressedTransfers++;
    continue;
}
```

Only then increment `attempted` and invoke existing transfer lifecycle. Do not change selector or retarget.

Require `attemptedTransfers + sociallySuppressedTransfers == opportunities` for current lifecycle flow.

- [ ] **Step 5: GREEN + selector regression**

```bash
./gradlew :common:test --tests net.conczin.mca.livingworld.memory2.SettlementSocialKnowledgeSharingPolicyTest --tests net.conczin.mca.livingworld.memory2.SettlementKnowledgeFlowLifecycleTest --tests net.conczin.mca.livingworld.memory2.SettlementKnowledgeFlowSelectorTest
```

- [ ] **Step 6: Commit**

```bash
git add common/src/main/java/net/conczin/mca/livingworld/memory2 common/src/test/java/net/conczin/mca/livingworld/memory2
git commit -m "feat: gate settlement sharing by direct social state"
```

---

### Task 5: Settlement preservation and no-enumeration proof

**Files:**
- Modify: `common/src/test/java/net/conczin/mca/livingworld/memory2/SettlementKnowledgeFlowPreservationTest.java`
- Modify: `common/src/test/java/net/conczin/mca/livingworld/memory2/SettlementKnowledgeFlowPersistenceTest.java`
- Add/modify source-wiring test if needed to prove selector does not import/read `NpcSocialGraphStore`.

- [ ] **Step 1: Add preservation tests**

Prove:

1. missing `npc-social-graph.json` behaves neutral and remains absent after exact pair read + allowed transfer;
2. an existing adverse graph file remains byte-identical after suppressed cycle;
3. suppressed cycle leaves `memory2.json` and `semantic-memory.json` listener state unchanged from pre-cycle bytes/state;
4. fresh-root reload repeats the same suppression;
5. positive social state does not increase `MAX_OPPORTUNITIES_PER_CYCLE`, fan-out or fallback behavior;
6. `SettlementKnowledgeFlowSelector` remains independent of graph state.

- [ ] **Step 2: Run preservation tests**

```bash
./gradlew :common:test --tests net.conczin.mca.livingworld.memory2.SettlementKnowledgeFlowPreservationTest --tests net.conczin.mca.livingworld.memory2.SettlementKnowledgeFlowPersistenceTest --tests net.conczin.mca.livingworld.memory2.SettlementKnowledgeFlowSelectorTest
```

Expected initial RED on new social assertions, then GREEN after Task 4 implementation; no additional production correction unless evidence exposes a defect.

- [ ] **Step 3: Commit test-only hardening**

```bash
git add common/src/test/java/net/conczin/mca/livingworld/memory2
git commit -m "test: prove social sharing gate preserves authority"
```

---

### Task 6: Fresh relationship policy revalidation for player commands

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/relationship/SnapshotCommandRelationshipPolicy.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/relationship/SnapshotCommandRelationshipPolicyTest.java`
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java`
- Add source-wiring test under `common/src/test/java/net/conczin/mca/entity/ai/chatAI/`.

**Interfaces:**

```java
public static boolean isAllowed(
    boolean relationshipStateEnabled,
    Path worldRoot,
    UUID villagerId,
    UUID playerId,
    String commandName)
```

- [ ] **Step 1: Write policy RED**

Seed `relationships.json` and require:

- relationship disabled → true without needing store;
- neutral follow-player → true;
- trust below `FOLLOW_MIN_TRUST` → false;
- fear above `FOLLOW_MAX_FEAR` → false;
- unrelated safe command remains true under normal state;
- invalid/unreadable inputs while relationship gating is enabled fail closed.

- [ ] **Step 2: Write OpenAI execution-wiring RED**

Require `applySnapshotCommand` to keep capture-time `snapshot.availableActions()` check, then inside `server.execute` invoke `SnapshotCommandRelationshipPolicy.isAllowed(...)` before `TriggerCommandInfos.findCommand(...).call`.

- [ ] **Step 3: Run focused RED**

```bash
./gradlew :common:test --tests net.conczin.mca.livingworld.relationship.SnapshotCommandRelationshipPolicyTest --tests '*SnapshotCommand*'
```

- [ ] **Step 4: Implement minimal fresh recheck**

Policy loads exact NPC×player current relationship only when enabled and catches runtime read failures to false. OpenAI uses it on the server thread immediately before TriggerCommand active-state revalidation.

- [ ] **Step 5: GREEN + relationship regression**

```bash
./gradlew :common:test --tests net.conczin.mca.livingworld.relationship.SnapshotCommandRelationshipPolicyTest --tests '*RelationshipActionPolicy*' --tests '*SnapshotCommand*'
```

- [ ] **Step 6: Commit**

```bash
git add common/src/main/java/net/conczin/mca/livingworld/relationship common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java common/src/test/java/net/conczin/mca
git commit -m "fix: revalidate relationship policy before command execution"
```

---

### Task 7: Live MCA GameTest for dialogue influence

**Files:**
- Create or modify: `fabric/src/gametest/java/net/conczin/mca/gametest/PersonalitySocialDialogueInfluenceGameTests.java`

**Interfaces:**
- Uses live `VillagerEntityMCA`, `VillagerBrain.setPersonality`, `PersonalitySocialSnapshotCapture`, influence policy and guidance renderer.

- [ ] **Step 1: Add GameTest first**

Spawn a real MCA villager; for representative live values `FRIENDLY`, `CRABBY`, `ANXIOUS`, set tracked Personality, capture with no counterpart, and require exact `WARM`, `GRUFF`, `ANXIOUS` styles plus one guidance line and unchanged tracked Personality after evaluation/render.

Also spawn a second MCA villager with seeded A→B and B→A states and prove disposition asymmetry without graph mutation.

- [ ] **Step 2: Run Fabric GameTests**

Use the repository's existing GameTest task selected by CI (the exact Gradle command must match current workflow scripts); expected GREEN after Tasks 1–4. If the test reveals a runtime-only defect, add a focused RED reproducer before production correction.

- [ ] **Step 3: Commit**

```bash
git add fabric/src/gametest/java/net/conczin/mca/gametest/PersonalitySocialDialogueInfluenceGameTests.java
git commit -m "test: validate live personality social influence"
```

---

### Task 8: Changelog, TDD evidence and full delivery gates

**Files:**
- Modify: `CHANGELOG.md`
- Create: `docs/superpowers/evidence/2026-08-11-personality-social-dialogue-behavior-tdd.md`

- [ ] **Step 1: Update `[Unreleased]`**

Document:

- closed personality dialogue-style guidance;
- exact-pair settlement social suppression after existing deterministic selection;
- no fallback/graph enumeration/truth scoring;
- fresh player relationship revalidation before command execution;
- unchanged provider schema/call count, persistence/config/release identity.

- [ ] **Step 2: Record staged evidence**

Evidence doc must list exact RED heads/runs, GREEN heads/runs, any fixture-only corrections, preservation results and final frozen-head gate IDs.

- [ ] **Step 3: Run complete selected verification**

Require one frozen final head with:

```text
Repository security policy                     PASS
VillAIgence CI                                  PASS
  common + deterministic provider tests        PASS
  Fabric GameTests                             PASS
  Fabric + NeoForge builds                     PASS
  production acceptance                        PASS
  selected persistence recovery                PASS
  distributable package verification           PASS
VillAIgence Production Soak                    PASS
VillAIgence GitHub Release dry-run             PASS
  publication                                  SKIPPED
base→head review P0/P1/P2/P3                   0/0/0/0
unresolved review threads                      0
```

- [ ] **Step 4: Open/update PR and deliver**

PR body must state exact non-scope and evidence. Mark ready only after frozen-head checks. Squash merge with `expected_head_sha` after gates pass.

- [ ] **Step 5: Reconcile canonical docs after merge**

Open a docs-only PR updating `PROJECT_STATE.md` / `ROADMAP.md`; only after that reconciliation is green+merged may NEXT advance to 0.3 convergence / release-candidate planning.
