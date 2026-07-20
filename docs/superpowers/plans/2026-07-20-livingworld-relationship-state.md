# LivingWorld Structured Relationship State Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add persistent bounded `trust/respect/fear/affinity` state per NPC/player pair and one conservative gameplay consequence without mutating MCA hearts.

**Architecture:** Persist relationship state in a world-local JSON store. Capture it into immutable LivingWorld context, allow the LLM to propose only bounded structured deltas, apply sanitized deltas after successful turns, and filter `follow-player` eligibility using a deterministic server-owned policy.

**Tech Stack:** Java 21, Gson, MCA ChatAI/ContextSnapshot, JUnit 5.

## Global Constraints

- Axes range `[-100,100]`.
- Per-turn proposed delta default max magnitude `2` per axis.
- MCA hearts/family/marriage are untouched.
- Legacy MCA/Inworld does not apply LivingWorld deltas.
- No new required user configuration.
- Full LivingWorld CI and official Fabric/NeoForge CI before merge.

---

### Task 1: Pure relationship model and persistence

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/relationship/LivingWorldRelationshipState.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/relationship/LivingWorldRelationshipDelta.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/relationship/LivingWorldRelationshipStore.java`
- Test corresponding pure/store tests.

- [ ] Test state clamp and delta clamp.
- [ ] Test persistence/reload and NPC/player isolation.
- [ ] Implement atomic JSON persistence keyed by NPC/player UUID pair.

### Task 2: Configuration, context and action policy

**Files:**
- Modify: `LivingWorldConfig.java` + config tests.
- Create: `LivingWorldRelationshipActionPolicy.java` + tests.
- Modify: `LivingWorldContextCapture.java`.

- [ ] Add `relationshipStateEnabled=true`, `relationshipMaxDeltaPerTurn=2` defaults.
- [ ] Load relationship state during server-thread snapshot capture fail-open.
- [ ] Add server-owned relationship fact to `worldFacts`.
- [ ] Filter `follow-player` when trust < -25 or fear > 60; leave other actions unchanged.

### Task 3: Structured LLM delta contract

**Files:**
- Modify: `OpenAIChatAI.java`.

- [ ] Extend `StructuredResponse` with optional relationship delta while preserving two-argument legacy constructor behavior.
- [ ] Direct snapshot prompt always documents JSON response schema and delta limits.
- [ ] Missing/malformed delta means no change.
- [ ] Persist sanitized delta only for configured direct LivingWorld after successful response.
- [ ] Apply action based on captured snapshot before new state influences later turns.

### Task 4: Documentation and verification

**Files:**
- Create: `docs/livingworld/RELATIONSHIPS.md`
- Modify: `docs/livingworld/CONFIGURATION.md`
- Modify: `docs/UPSTREAM_ISSUE_AUDIT.md`

- [ ] Document separation from MCA hearts and exact security boundary.
- [ ] Update #977 overflow status and #1292 implementation status.
- [ ] Run both CI workflows and merge only when green.
