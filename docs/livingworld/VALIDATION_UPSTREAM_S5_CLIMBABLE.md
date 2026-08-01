# Upstream S5 Climbable Navigation Validation

**Implementation date:** 2026-08-01  
**Package:** S5 — stable ladder and tagged-climbable navigation  
**Pull request:** #76  
**VillAIgence base:** `846979a2f5aef6775ee3ae1471eab12b50246aab`  
**Upstream source state:** `c65806b469fbea79d9d7033f7b114796bd1a3b13`, `c70af35f14c87c4d12dcaaa787e42e0345b6492c`, final state at `7ba76165dcf8aa9aa79070bdbac141b65519b3e7`

## Imported final-state behavior

S5 extends the S4 water/collision foundation with climbable graph and motion control.

```text
path graph
→ #minecraft:climbable blocks are WALKABLE path nodes
→ climbable nodes connect vertically one block at a time
→ large vertical vanilla transitions are removed on the climbable
→ upper-floor exits are searched within a bounded two-block rise
→ start-node recovery searches at most two blocks below an entity already attached to a climbable
→ climbable lookups are cached for the evaluator lifetime

motion
→ approach the climbable anchor with bounded horizontal velocity
→ ladder anchors include the block-facing offset
→ ascent velocity cannot become negative
→ descent velocity cannot become positive
→ node completion uses direction-sensitive tolerances
→ exit handoff applies a directional vertical bias
→ vanilla jump is suppressed only while MCA navigation controls the climb
```

Player-follow behavior now accounts for vertical separation:

```text
same or adjacent vertical level → close-enough distance 2
large vertical separation       → close-enough distance 0
currently on climbable          → close-enough distance 1
```

Existing panic and guard-combat yielding remain authoritative.

## VillAIgence adaptations

- S4 tagged-water positioning is retained unchanged.
- S4 actual-mob AABB and partial-collision policy are retained unchanged.
- No access widener, dependency lock or verification metadata is changed.
- `setJumping` is adapted through the existing `MixinMob`, avoiding a broad edit to `VillagerEntityMCA`.
- The mixin affects only MCA villagers whose `MCAGroundPathNavigation` currently owns a climb context.
- AI provider, Memory 2.0, relationships, voices and security code are untouched.

## TDD evidence

### Canonical RED

```text
head: 79213ab51232406767dff0a01b53a1affe06e1e6
VillAIgence CI #1026 / 30697363964
result: expected FAILURE
boundary: common:compileTestJava
reason: ClimbNavigationPolicy was absent at the policy call sites
```

### GREEN

```text
head: 5c14756421a7a70017fa4cafa15010003871a7d9
VillAIgence CI #1032 / 30697556485              SUCCESS
Java Pull Request CI #548 / 30697556501       SUCCESS
Repository security policy #268 / 30697556499 SUCCESS
```

The GREEN gate executes:

```text
:common:test
:fabric:build
:neoforge:build
Fabric distributable-package verification
repository security policy
```

## Automated regression coverage

`ClimbNavigationPolicyTest` proves:

1. the following path node has priority when determining vertical direction;
2. the previous node is the deterministic fallback;
3. ascent and descent use direction-sensitive reached-height checks;
4. controlled vertical velocity cannot reverse the intended direction;
5. exit bias points out of the climbable;
6. horizontal attraction is proportional and bounded;
7. jumping is suppressed only while navigation controls a climb;
8. player-follow distance responds to vertical separation and climbable state.

Both loader builds compile the actual climbable graph, motion controller, mixin and follow-task integration.

## Deferred cumulative server acceptance

Per operator decision, S5 is accumulated with S1–S6.

Required cumulative S5 segment:

```text
vanilla ladder:
→ command adult NPC to ascend and exit at the top
→ command the same NPC to descend and exit at the bottom
→ reverse direction repeatedly near the middle and both exits
→ verify no vertical oscillation, jump fighting or permanent stuck state

tagged non-ladder climbable:
→ repeat ascent/descent using another block included in #minecraft:climbable
→ verify center anchor fallback and stable exit

child NPC:
→ repeat representative ascent/descent and exit
→ verify S4 entity-sized collision handling remains valid

player following:
→ follow player across a vertical ladder route
→ verify the NPC does not stop two blocks away on the wrong level

guard behavior:
→ verify active combat still takes precedence over player following

restart:
→ repeat representative ladder and tagged-climbable routes
→ verify no path corruption, oscillation loop or crash
```

## Acceptance boundary

```text
repository implementation: PASS
automated directional-motion tests: PASS
Fabric build/package verification: PASS
NeoForge compile compatibility: PASS
repository security policy: PASS
isolated live S5 validation: intentionally deferred
cumulative S1–S6 server validation: PENDING
release promotion based on live climbable evidence: NOT YET CLAIMED
```
