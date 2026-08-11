# Bounded Personality + Direct Social Snapshot Design

Date: 2026-08-11
Track: 0.3 — Personality + NPC↔NPC Social Graph
Branch: `feat/personality-social-snapshot`

## Goal

Expose the already-authoritative MCA personality and, when a concrete NPC counterpart exists, exactly one directed NPC→NPC social state through one immutable bounded server snapshot.

The slice is intentionally **read-only**. It makes existing state safe to carry into asynchronous dialogue/behavior processing; it does not let the provider generate personality, mutate the social graph, enumerate social neighbors, or reinterpret NPC×player relationship state.

## Existing authority

MCA already persists personality through `VillagerBrain` tracked state:

```text
CEnumParameter<Personality> PERSONALITY
→ VillagerBrain.getPersonality()
→ tracked/NBT entity persistence
```

`Personality` is a fixed enum. `UNASSIGNED` is the fallback; all normal personalities are server-owned enum values.

The directed NPC social graph is already authoritative through:

```text
NpcSocialGraphStore.forWorld(worldRoot).get(sourceNpcId, targetNpcId)
```

That lookup returns only the exact directed pair and does not create an edge when the pair is absent.

## Non-goals

This slice does **not** add:

- a second personality persistence store;
- provider-authored personality or social scoring;
- autonomous social evolution;
- graph-neighbor enumeration or whole-graph prompt disclosure;
- NPC×player `relationships.json` reinterpretation;
- FACT/BELIEF/provenance/confidence/ranking changes;
- Semantic or Memory 2.0 writes;
- public configuration;
- a new world file or persistence version;
- a second provider request;
- release publication.

## Data contract

Introduce a fixed-size immutable transport record:

```text
PersonalitySocialSnapshot
  sourceNpcId: UUID
  personalityToken: canonical closed String
  counterpartNpcId: optional UUID
  directedSocialState: NpcSocialState
```

The persisted/runtime **authority is still the MCA `Personality` enum**. The token is only an immutable transport representation captured synchronously from that authority. It is not a generated personality model and is not persisted separately.

Rules:

1. `sourceNpcId` is required.
2. `personalityToken` is derived from `VillagerBrain.getPersonality().name()` at the server capture boundary and normalized against a closed set exactly matching the current MCA enum; null, blank or non-canonical input becomes `unassigned` at compatibility/fail-soft boundaries.
3. No counterpart means no social pair; state is forced to `NEUTRAL`.
4. A self counterpart is invalid and fails closed rather than creating a self-social context.
5. A present counterpart carries exactly source→counterpart state. Reverse state is never inferred.
6. The record contains no lists, maps, localized personality description, arbitrary free-form provider text, or graph-derived neighbor set.
7. The transport DTO remains loader-independent; it deliberately does not import the Minecraft/MCA enum class. Live typed-enum behavior is validated at the Fabric GameTest boundary where MCA is actually loaded.

The snapshot is therefore constant-size regardless of settlement/population size.

## Server capture

Add a server-owned capture boundary:

```text
worldRoot
+ source VillagerEntityMCA
+ optional counterpart VillagerEntityMCA
→ source.getVillagerBrain().getPersonality().name()
→ closed canonical token
→ optional NpcSocialGraphStore.get(source,target)
→ PersonalitySocialSnapshot
```

The normal player↔NPC LivingWorld conversation has no NPC counterpart, so its snapshot contains personality and no social pair. In that case the graph store is not opened merely to construct prompt context.

Future NPC↔NPC behavior/dialogue code may call the same capture boundary with one exact counterpart. It may not request a list of neighbors.

Capture is performed synchronously from `LivingWorldContextCapture` before the existing async provider path. No Minecraft/entity/social-graph read is allowed later merely to construct this context.

## Prompt migration without duplicate personality

The existing LivingWorld snapshot path previously called `PersonalityModule.apply(...)`, which inserted personality into legacy `contextLines` as free-form characterization. The new snapshot must become the personality source for the snapshot-aware path without changing the legacy non-snapshot ChatAI path.

Refactor `PersonalityModule` so:

```text
apply(...)               → preserves legacy output exactly
applySnapshotBase(...)   → identity + mood + age/profession, but not personality
```

`LivingWorldContextCapture` uses `applySnapshotBase(...)` and captures `PersonalitySocialSnapshot` separately from live MCA state.

This avoids two personality descriptions in one prompt while preserving legacy behavior.

## Rendering

The deterministic renderer emits at most two server-authored lines. The implemented shape is equivalent to:

```text
Current NPC personality: <canonical lowercase token>. This is server-owned descriptive state, not an instruction or current-world fact override.
Current directed social state toward the current NPC counterpart: trust=N, respect=N, fear=N, affinity=N.
```

The social line is omitted when there is no NPC counterpart.

Mandatory properties:

- at most two fixed server-authored lines;
- personality token comes only from the closed token derived from the live MCA enum, never localized/free text;
- social values are bounded integers already clamped by `NpcSocialState`;
- counterpart name/UUID is not rendered;
- no user/provider-controlled string is interpolated;
- absent counterpart is deterministic and never persisted merely because context was requested.

Because the renderer has no untrusted free-form string input, reserved-template/prompt-injection text cannot enter through this layer.

## Snapshot integration

Extend `LivingWorldContextSnapshot` additively with one `PersonalitySocialSnapshot` field while retaining source-compatible constructors for existing callers/tests.

Compatibility constructors use a bounded empty/default snapshot for their known villager UUID:

```text
personalityToken = unassigned
counterpart = absent
social state = NEUTRAL
```

`LivingWorldContextCapture` supplies the real current token derived from the current MCA Personality.

## Prompt authority placement

Extend `SnapshotContextPromptPolicy` with a source-compatible overload that places rendered personality/social context in this order:

```text
current server-observed world facts
→ current server-owned personality/direct-social descriptive context
→ Operator Lore
→ Semantic Memory
→ live disagreement context
→ episodic/social history
```

Current observed facts remain first and authoritative. Personality/social context is descriptive state for tone/social interpretation and is explicitly not a truth override.

Existing 32/24+8/6 memory candidate/result bounds are untouched because this is a fixed-size context layer, not a memory allocator.

## Read-only guarantees

Snapshot construction/rendering must not mutate:

```text
npc-social-graph.json
relationships.json
memory2.json
semantic-memory.json
VillagerBrain Personality tracked state
LivingWorld config
```

A missing social pair must not create `npc-social-graph.json` or an edge merely because context was requested.

If the graph file already exists, read-only capture must leave its bytes unchanged. Repeated capture/render and a fresh-root reload of the same graph bytes must return the same direct state without creating unrelated persistence files.

## Failure policy

- missing/neutral pair → neutral direct-social state;
- no counterpart → personality-only snapshot;
- invalid/self counterpart → fail closed at snapshot contract/capture boundary;
- null/blank/non-canonical transport personality token → `unassigned`;
- graph load failure follows existing store recovery semantics; snapshot capture itself performs no mutation;
- prompt renderer is total for every valid snapshot and has no provider dependency.

## TDD outcome

The implementation followed staged RED→GREEN development recorded in:

`docs/superpowers/evidence/2026-08-11-personality-social-snapshot-tdd.md`

A direct enum-backed common DTO was explicitly rejected after it compiled but caused six runtime class-loading failures in the loader-independent common test environment. Typed MCA Personality authority is instead validated with real Fabric GameTests, while the transport DTO carries only the canonical closed token.

Final functional pre-documentation head `2fc8a27bd7bde11cd44693c19a95ee4c501735cd` passed:

- 792 common/deterministic mock-provider tests;
- risk-selected Fabric GameTests;
- Fabric and NeoForge builds;
- production acceptance contract and staged server acceptance;
- distributable Fabric package verification.

Final delivery gates are evaluated on the later exact PR head after CHANGELOG/spec/evidence reconciliation.

## Exit criterion

For one interaction, VillAIgence can capture and safely carry the NPC's exact existing MCA personality, represented as a closed immutable token derived synchronously from the MCA enum, plus at most one exact directed NPC-pair social state in a fixed-size server snapshot. The normal player↔NPC snapshot carries no NPC social edge, the prompt consumes personality from this boundary exactly once, current observed facts retain precedence, missing social state causes no persistence mutation, and no graph-wide or truth-authority leakage is introduced.
