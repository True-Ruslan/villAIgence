# Bounded Personality + Direct Social Snapshot — TDD Evidence

Date: 2026-08-11
PR: #155
Base: `8ebca09b27051ba1fc95e9f80e8c1fad092156de`

## Scope

This ledger records the staged RED→GREEN implementation of the 0.3 read-only personality/direct-NPC-pair snapshot slice. It deliberately distinguishes valid product REDs from fixture mistakes and rejected architecture experiments.

The accepted boundary is:

```text
live MCA VillagerBrain.getPersonality()
→ server-thread capture
→ closed canonical personality token in loader-independent snapshot DTO
+ optional exact NpcSocialGraphStore.get(source,target)
→ fixed-size renderer
→ centralized snapshot prompt policy
```

No provider call/schema, public config, persistence schema/version, autonomous social mutation, truth-authority mutation, or release publication is added.

## Task 1 — fixed-size DTO and renderer

### RED

- Head: `aa18514df214c4ec02ea1df819e6160ca659a091`
- CI: VillAIgence CI #2677 / run `31471850743`
- Result: production compilation succeeded; `common:compileTestJava` failed with exactly 14 missing-symbol errors for the absent `PersonalitySocialSnapshot` and `PersonalitySocialContextRenderer` APIs.

### First GREEN attempt — rejected runtime boundary

- Head: `50f8159fc583a76797104808420fdbd47fee72fa`
- CI: #2681 / run `31472086106`
- Result: tests compiled, but 781 tests completed with exactly 6 new failures caused by `NoClassDefFoundError` / `ClassNotFoundException` when the pure common DTO directly carried MCA `Personality`.
- Interpretation: Minecraft/MCA enum ownership is correct for live server authority, but the immutable transport DTO must remain loader-independent.

### Corrected GREEN

- Final boundary head: `dac673ff845a4bf5eb4cf4ee69e10b5101b8bad5`
- Result: full common suite passed with a closed canonical token set matching the MCA enum. Invalid/untrusted tokens fail soft to `unassigned`; renderer remains fixed-size and emits no UUID/name.

## Task 2 — read-only direct-pair store view

### RED

- Head: `9bb1d61791d95c2aaa75c03d76ef98f674cdcd22`
- CI: #2691 / run `31472786001`
- Result: production compile green; `common:compileTestJava` failed with exactly 5 missing-symbol errors for absent `PersonalitySocialSnapshotStoreView`.

### GREEN

- Head: `ae153cc93120e28b43ea8b8766d4386ecf40c575`
- Result: common suite passed.
- Guarantees: no counterpart does not instantiate the graph store; a counterpart performs exactly `get(source,target)`; missing pair is neutral; A→B and B→A remain independent; existing graph bytes are unchanged by capture/render.

## Task 3 — immutable snapshot source compatibility

`LivingWorldContextSnapshot` was extended additively with `PersonalitySocialSnapshot` while preserving pre-slice constructor call sites through bounded defaults for the already-known villager UUID. Stable staged delivery was green by `cab08931e48b01f6d7ee0d909b45d4d53d0ff69c` across security, CI, soak and release dry-run.

Compatibility constructors do not create a second personality authority: they use `unassigned`, no counterpart and neutral social state.

## Task 4 — snapshot personality de-duplication

### RED

- Head: `4d9e4a90fc2067b24a02df36e238db8414363fbe`
- CI: #2699
- Result: common remained green; Fabric GameTest compilation failed with exactly one missing-symbol error for absent `PersonalityModule.applySnapshotBase(...)`.

### GREEN

- Head: `8dbee2e18f553bf847564857f31a7af4848eb794`
- CI: #2701
- Result: common, Fabric GameTests and supported loader builds passed.
- Guarantee: legacy `PersonalityModule.apply(...)` retains the original personality+mood wording; snapshot path retains identity/mood/age/profession but omits the legacy personality token so the typed snapshot is rendered exactly once.

## Task 5 — prompt authority layer

### Policy RED

- Head: `292b1d0791b7caa4787882a2b24f548e7c0a8058`
- CI: #2703
- Result: exactly 2 compile errors for the absent six-layer `SnapshotContextPromptPolicy.compose(...)` overload.

### Policy GREEN

- Head: `3c98ba0ad48b7adf17fdc0d8e3b1e52ecd24b5e6`
- CI: #2705
- Result: common suite passed.
- Authority order: current world facts → personality/direct-social descriptive state → operator lore → Semantic Memory → live disagreement context → episodic/social history.
- Old overloads delegate through an empty personality/social layer; empty-layer composition remains byte-for-byte equivalent to the legacy result.

### Wiring RED

- Head: `6673129e1bdde1b4d35a8c274b919e0da9c87ff3`
- CI: #2707
- Result: 789 tests / exactly 1 failure, `SnapshotLayeredPromptWiringPolicyTest`.

### Wiring GREEN

- Head: `ebe3d8300175de5d5e3bfb651f16c08d7b40607c`
- Change: `OpenAIChatAI` delegates rendered personality/social lines into the centralized compose call; no ad-hoc prompt string construction and no provider request/schema change.
- Preservation fixture: `a582f863573d3c21419d4741990d145e7149d75d` updated the older contradiction-wiring source test to the six-layer call without weakening its exact-once/before-structured-instructions guarantee.

## Task 6 — live MCA authority GameTest

### RED

- Tests-only live GameTest: `7bc22e0ad7c7530ccdec4b955df987efbfb1f021`
- After the stale contradiction fixture was corrected, common stayed green and Fabric `compileGametestJava` failed only because `PersonalitySocialSnapshotCapture` did not yet exist.

### GREEN

- Head: `da4b18afa0b4ccf028a4981bf64b9cfe622d7284`
- Result: Fabric GameTests and Fabric/NeoForge builds passed.
- Live test uses two real MCA villagers with `Personality.FRIENDLY` and `Personality.CRABBY`, seeds distinct A→B and B→A states, and proves:
  - live MCA personality becomes the canonical snapshot token;
  - directed graph state is exact and never reversed/inferred;
  - personality-only capture has no social edge;
  - MCA tracked personality is unchanged;
  - `npc-social-graph.json` remains byte-identical;
  - renderer remains fixed-size and does not expose UUIDs.

## Task 7 — preservation / fresh-root reload

- Tests-only head: `733ce39a9720fb772d25cf149ca87c46a3d1ac4f`
- Final common GREEN evidence: CI #2735 / run `31524966816` on `2fc8a27bd7bde11cd44693c19a95ee4c501735cd`.
- Repeated capture/render, then fresh-root graph-file reload, preserves exact directed state and graph bytes.
- Snapshot reads do not create `relationships.json`, `memory2.json`, or `semantic-memory.json`.

## Rejected enum-backed common experiments

These are **not** product REDs and must not be interpreted as required product behavior:

- `3d6b8cc90cc2ded1a2ec2006993c528b30a160d0` introduced a common test requiring an MCA-enum DTO API; it produced 6 compile errors.
- A repeated enum-backed test experiment was later staged again.
- `0b77e0c8cb02981633910f5d7391543c0644bf99` changed the production DTO itself to carry MCA `Personality`.
- That direction contradicted already-observed runtime evidence from #2681, where the enum-backed common DTO compiled but caused exactly 6 class-loading failures.
- Test boundary was restored by `fcfdd550d752af3c676f272121d40075be37b98a` and production boundary by `31ca3225352f312f7ddf2a94f50ceb47daa1f9ea`.

Conclusion: the server authority remains the exact MCA enum; the immutable transport snapshot carries only a closed canonical token derived synchronously from that authority. This is transport representation, not a second persisted personality model.

## Task 8 — real LivingWorld server-thread wiring

### RED

- Clean RED head: `31ca3225352f312f7ddf2a94f50ceb47daa1f9ea`
- CI: #2733 / run `31524555257`
- Result: 792 tests / exactly 1 failure, `LivingWorldContextCapturePersonalitySocialWiringPolicyTest`.
- All enum-backed drift had been removed before interpreting this as a product RED.

### GREEN

- Head: `2fc8a27bd7bde11cd44693c19a95ee4c501735cd`
- CI: #2735 / run `31524966816`
- Result:
  - common + deterministic mock-provider tests: SUCCESS (792 tests);
  - risk-selected Fabric GameTests + Fabric/NeoForge loader builds: SUCCESS;
  - production acceptance contract: SUCCESS;
  - staged production server acceptance: SUCCESS;
  - distributable Fabric package verification: SUCCESS.
- Wiring changes are intentionally limited to:
  1. `LivingWorldContextCapture` uses `PersonalityModule.applySnapshotBase(...)`;
  2. it captures `PersonalitySocialSnapshotCapture.capture(worldRoot, villager, null)` synchronously on the server thread;
  3. it passes the immutable snapshot into `LivingWorldContextSnapshot`.

Because the player↔NPC path supplies no NPC counterpart, it performs no social-graph lookup merely to answer a player. Future exact NPC↔NPC callers may supply one explicit counterpart through the same bounded capture adapter.

## Review hardening — snapshot owner binding

Manual base→head readiness review found one P2 integrity gap before merge: the primary `LivingWorldContextSnapshot` constructor could be called with `villagerId=A` while carrying a `PersonalitySocialSnapshot` owned by `sourceNpcId=B`. The normal server capture path already supplied matching IDs, but the immutable boundary itself did not enforce that invariant.

### RED

- Tests-only head: `194f1994c3c1271fc7b1313f8653e8e84dd56c43`
- CI: #2745 / run `31526373610`
- Result: 793 tests / exactly 1 failure, `canonicalConstructorRejectsPersonalitySocialSnapshotOwnedByAnotherNpc`.
- Security/release-identity preflight remained green; later CI stages were skipped because the intended common regression failed first.

### GREEN

- Head: `cb489c37a3aa0662b9dc953ce1caa367de66df0c`
- CI: #2747 / run `31526699758`
- Result at evidence update time:
  - common + deterministic mock-provider tests: SUCCESS (793 tests);
  - risk-selected Fabric GameTests + Fabric/NeoForge loader builds: SUCCESS;
  - production acceptance contract: SUCCESS.
- Fix: the canonical immutable snapshot constructor now fails closed when a non-null `personalitySocialSnapshot.sourceNpcId()` differs from the non-null `villagerId`; compatibility/default construction is unchanged.

This closes cross-NPC descriptive/social snapshot attachment at the immutable boundary without changing runtime capture behavior, prompt semantics, persistence, provider APIs, or feature scope.

## Preserved invariants

The completed slice does not:

- mutate MCA Personality;
- mutate or enumerate the NPC social graph during capture;
- read or write NPC×player relationship state as NPC↔NPC social state;
- write Memory 2.0 or Semantic Memory;
- add a provider request or provider schema field;
- add public config;
- add or version a persistence file;
- promote BELIEF to FACT or change contradiction truth semantics;
- add autonomous social deltas or trust weighting.

Final delivery evidence belongs to the exact PR head after CHANGELOG/spec/evidence reconciliation and review hardening; release publication must remain skipped in release-dry execution.
