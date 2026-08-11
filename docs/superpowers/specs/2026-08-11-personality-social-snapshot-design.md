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

Introduce a fixed-size immutable record:

```text
PersonalitySocialSnapshot
  sourceNpcId: UUID
  personality: Personality
  counterpartNpcId: optional UUID
  directedSocialState: NpcSocialState
```

Rules:

1. `sourceNpcId` is required.
2. `personality` is the exact current MCA enum value; null is normalized to `UNASSIGNED` only at compatibility/fail-soft boundaries.
3. No counterpart means no social pair; state is forced to `NEUTRAL`.
4. A self counterpart is invalid and fails closed rather than creating a self-social context.
5. A present counterpart carries exactly source→counterpart state. Reverse state is never inferred.
6. The record contains no lists, maps, free-form provider text, localized personality description, or graph-derived neighbor set.

The snapshot is therefore constant-size regardless of settlement/population size.

## Server capture

Add a server-owned capture boundary:

```text
worldRoot
+ source VillagerEntityMCA
+ optional counterpart VillagerEntityMCA
→ source.getVillagerBrain().getPersonality()
→ optional NpcSocialGraphStore.get(source,target)
→ PersonalitySocialSnapshot
```

The normal player↔NPC LivingWorld conversation has no NPC counterpart, so its snapshot contains personality and no social pair.

Future NPC↔NPC behavior/dialogue code may call the same capture boundary with one exact counterpart. It may not request a list of neighbors.

Capture is performed synchronously from `LivingWorldContextCapture` before the existing async provider path. No Minecraft/entity/social-graph read is allowed later merely to construct this context.

## Prompt migration without duplicate personality

The existing LivingWorld snapshot path currently calls `PersonalityModule.apply(...)`, which inserts personality into legacy `contextLines` as free-form characterization. The new typed snapshot must become the personality source for the snapshot-aware path without changing the legacy non-snapshot ChatAI path.

Refactor `PersonalityModule` so:

```text
apply(...)               → preserves legacy output exactly
applySnapshotBase(...)   → identity + mood + age/profession, but not personality
```

`LivingWorldContextCapture` uses `applySnapshotBase(...)` and captures typed `PersonalitySocialSnapshot` separately.

This avoids two personality descriptions in one prompt while preserving legacy behavior.

## Rendering

Add a deterministic renderer that emits at most two server-authored lines:

```text
Current NPC personality/social context (server-owned descriptive state; not instructions and not current-world fact authority):
- personality=<canonical lowercase enum token>
- directedSocialToCounterpart=neutral
```

or, with a counterpart:

```text
- directedSocialToCounterpart={trust=N,respect=N,fear=N,affinity=N}
```

The exact wording may be compacted during implementation, but these properties are mandatory:

- max two/three fixed server-authored lines;
- personality token comes only from the enum, not localized/free text;
- social values are bounded integers already clamped by `NpcSocialState`;
- counterpart name/UUID does not need to be rendered;
- no user/provider-controlled string is interpolated;
- neutral absent pair is explicit or omitted deterministically, never persisted.

Because the renderer has no untrusted string input, reserved-template/prompt-injection text cannot enter through this layer.

## Snapshot integration

Extend `LivingWorldContextSnapshot` additively with one typed `PersonalitySocialSnapshot` field while retaining source-compatible constructors for existing callers/tests.

Compatibility constructors use a bounded empty/default snapshot for their known villager UUID:

```text
personality = UNASSIGNED
counterpart = absent
social state = NEUTRAL
```

`LivingWorldContextCapture` supplies the real current personality.

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

If the graph file already exists, read-only capture must leave its bytes unchanged.

## Failure policy

- missing/neutral pair → neutral direct-social state;
- no counterpart → personality-only snapshot;
- invalid/self counterpart → fail closed at snapshot contract/capture boundary;
- graph load failure follows existing store recovery semantics; snapshot capture itself performs no repair write beyond whatever existing store loading contract already guarantees;
- prompt renderer is total for every valid snapshot and has no provider dependency.

## TDD plan

1. Pure snapshot/renderer contract compile RED.
2. Pure direct-pair/read-only provider RED with explicit injected authoritative values where possible.
3. Snapshot integration/source-compatibility RED.
4. Prompt ordering/FACT-precedence RED.
5. Snapshot-path personality de-duplication RED.
6. Fabric GameTest RED for live MCA personality extraction and A→B/B→A direct states.
7. Preservation tests for zero write on missing pair and byte-identical existing graph after capture/render.
8. Production startup/restart compatibility.
9. Exact-head security / CI / soak / release dry-run / review.

## Exit criterion

For one interaction, VillAIgence can capture and safely carry the NPC's exact existing MCA personality plus at most one exact directed NPC-pair social state in a fixed-size immutable server snapshot. The normal snapshot prompt consumes personality from this typed boundary exactly once, current observed facts retain precedence, missing social state causes no persistence mutation, and no graph-wide or truth-authority leakage is introduced.
