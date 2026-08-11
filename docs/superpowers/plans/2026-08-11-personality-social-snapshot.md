# Bounded Personality + Direct Social Snapshot Implementation Plan

Date: 2026-08-11
Spec: `docs/superpowers/specs/2026-08-11-personality-social-snapshot-design.md`

## Task 1 — Pure fixed-size snapshot + renderer

Tests first:

- add `PersonalitySocialSnapshotTest`;
- add `PersonalitySocialContextRendererTest`;
- require source UUID and exact enum personality;
- absent counterpart forces neutral social state;
- self counterpart fails closed;
- direct state is preserved exactly;
- renderer is deterministic and fixed-size;
- renderer contains only enum-derived token/bounded integer state and no UUID/name/free-form text;
- neutral/no-counterpart rendering is deterministic.

Observe compile RED for absent snapshot/renderer APIs, then implement only those APIs.

## Task 2 — Read-only authoritative capture boundary

Tests first:

- add unit coverage for direct graph lookup semantics where possible;
- add preservation coverage proving missing pair capture causes no graph write;
- add byte-equality coverage proving existing graph bytes are unchanged after capture/render;
- add direct A→B versus B→A state tests.

Implement `PersonalitySocialSnapshotCapture` using only:

```text
source.getVillagerBrain().getPersonality()
NpcSocialGraphStore.forWorld(worldRoot).get(source,target)
```

No enumeration API and no mutation API.

## Task 3 — Snapshot source compatibility

Tests first:

- extend `LivingWorldContextSnapshot` contract tests;
- old constructor signatures must compile and produce `UNASSIGNED + no counterpart + NEUTRAL` compatibility snapshot;
- canonical new constructor preserves the supplied typed snapshot immutably.

Then add one typed `PersonalitySocialSnapshot` field and compatibility constructors.

## Task 4 — Snapshot-path personality de-duplication

Tests first:

- existing legacy `PersonalityModule.apply(...)` output remains unchanged;
- new `applySnapshotBase(...)` includes conversation identity, mood, age/profession as before but omits personality token;
- snapshot path obtains personality through the typed snapshot renderer exactly once.

Then refactor `PersonalityModule` minimally and switch only `LivingWorldContextCapture` snapshot path.

## Task 5 — Prompt authority placement

Tests first:

- add source-compatible overload to `SnapshotContextPromptPolicy` tests;
- required order:
  current world facts → personality/social → operator lore → semantic → contradiction → episodic/social history;
- current observed fact authority statement remains before/louder than lower-authority context;
- empty personality/social context introduces no extra prompt text;
- fixed snapshot layer does not affect memory result bounds.

Then integrate renderer output into `OpenAIChatAI.buildSnapshotSystem(...)` through the existing centralized prompt policy.

## Task 6 — Live MCA GameTest

Tests first:

- add GameTest for a real `VillagerEntityMCA` with an explicitly assigned `Personality`;
- capture must read that exact personality from `VillagerBrain` tracked state;
- create two MCA NPCs and seed A→B and B→A with different states;
- capture(A,B) and capture(B,A) must differ exactly as directed graph state differs;
- capture(A,null) must be personality-only and create no social edge;
- no provider call is involved.

Observe GameTest compile/behavior RED as appropriate, then add only the minimum server capture integration.

## Task 7 — Preservation and mutation-isolation tests

Add tests proving capture/render does not mutate:

- `npc-social-graph.json` bytes;
- NPC×player `relationships.json` bytes;
- `memory2.json`;
- `semantic-memory.json`;
- Personality tracked value before/after capture.

Exercise fresh-root reload and repeated capture for byte-idempotency.

No production correction unless an observed preservation failure requires one.

## Task 8 — Runtime integration and compatibility

- `LivingWorldContextCapture.capture(...)` captures typed personality snapshot synchronously on server thread;
- normal player dialogue supplies no NPC counterpart;
- no new persistence/config/provider schema/call;
- existing production acceptance fixture must remain startup/restart compatible.

## Task 9 — Changelog + TDD evidence

In the same runtime PR:

- update root `CHANGELOG.md` `[Unreleased]`;
- add `docs/superpowers/evidence/2026-08-11-personality-social-snapshot-tdd.md` with exact RED/GREEN heads/runs and any fixture corrections.

## Task 10 — Delivery

Freeze exact head and require:

```text
Repository security policy
VillAIgence CI
  common + deterministic provider tests
  Fabric GameTests
  Fabric build
  NeoForge build
  production acceptance
  selected persistence recovery
  distributable package verification
VillAIgence Production Soak
VillAIgence GitHub Release dry-run
  publication SKIPPED
base→head review
  P0/P1/P2 = 0/0/0
  unresolved threads = 0
```

Then:

- update PR body with exact evidence;
- mark ready;
- squash merge with expected head SHA;
- open docs-only reconciliation PR for `PROJECT_STATE.md` / `ROADMAP.md`;
- advance NEXT only after reconciliation is green and merged.
