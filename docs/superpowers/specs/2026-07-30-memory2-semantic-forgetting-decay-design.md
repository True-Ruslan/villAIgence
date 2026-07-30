# Memory 2.0 Deterministic Semantic Forgetting and Decay Design

## Goal

Add explicit, deterministic forgetting rules to Semantic Memory without allowing time alone, an LLM, a provider, or a restart to delete knowledge unpredictably.

## Context

Semantic Memory already has:

- typed FACT and BELIEF entries;
- explicit provenance;
- importance and confidence values;
- source-event evidence;
- deterministic consolidation;
- a bounded per-NPC entry limit.

The current store removes the oldest entry whenever the limit is exceeded. That policy ignores importance, confidence, provenance, and corroboration.

## Considered approaches

### 1. Absolute time-to-live deletion

Delete entries after a fixed or calculated age.

Rejected for this slice because it is destructive even without memory pressure, difficult to calibrate for servers with different uptime patterns, and can make NPCs forget while no replacement knowledge exists.

### 2. Mutating confidence over time

Periodically lower the persisted confidence field and delete entries below a threshold.

Rejected because confidence describes evidential certainty, not storage priority. Mutating it would conflate truth confidence with forgetting and would rewrite persistent data merely because time passed.

### 3. Pressure-based deterministic forgetting — selected

Keep all valid entries while capacity remains. When the per-NPC limit is exceeded, rank entries by a pure retention score that includes age, importance, confidence, provenance, and independent source evidence. Forget only the lowest-ranked entries.

This preserves long-term memory when the store is not under pressure and makes replacement behavior explicit and testable.

## Architecture

Introduce one pure provider-independent component:

```text
SemanticMemoryRetentionPolicy
```

Responsibilities:

1. calculate static durability from entry metadata;
2. calculate an effective retention score at an authoritative game time;
3. select the best `maxEntriesPerNpc` entries deterministically;
4. expose no persistence, Minecraft, provider, or LLM dependencies.

`SemanticMemoryStore.append` remains the only production integration point:

```text
append
→ exact UUID replay check
→ add candidate
→ deterministic consolidation
→ deterministic retention selection
→ save only when stored state changed
```

The persistent JSON schema remains version 1 with no new fields.

## Retention formula

### Static durability

```text
importance contribution = importance × 4                 // 0..400
confidence contribution = confidence × 5 / 2             // 0..250
provenance contribution:
  SYSTEM_OBSERVED = 200
  PLAYER_TOLD     = 100
  NPC_TOLD        = 75
  INFERRED        = 25
source contribution = min(sourceEventIds count, 6) × 25   // 0..150
```

Maximum durability is 1000.

The provenance table preserves the authority boundary without changing entry kind or confidence. FACT remains FACT and BELIEF remains BELIEF.

### Decay

```text
DECAY_STEP_TICKS = 36_000
```

At 20 TPS this is approximately 30 minutes of server runtime. One durability point offsets one decay step.

```text
ageTicks = max(0, nowGameTime - entry.gameTime)
effectiveRetentionScore = durability × DECAY_STEP_TICKS - ageTicks
```

This is a storage-priority score only. It is never written into `semantic-memory.json` and never exposed to the model as truth confidence.

Using authoritative world game time means:

- restart does not reset decay;
- offline wall-clock time does not cause forgetting;
- `/time set` or a lower observed game time cannot produce negative age;
- provider latency and system clock changes cannot affect retention.

## Deterministic tie-breaking

Entries are retained by this order:

1. higher effective retention score;
2. higher importance;
3. higher confidence;
4. more independent `sourceEventIds`;
5. newer game time;
6. newer creation epoch;
7. lexicographically smaller UUID.

The retained list is returned in the store's existing oldest-first persistence order. Therefore JSON output remains stable for the same logical input set.

## Store behavior

### Under capacity

No entry is forgotten. Existing behavior and UUIDs remain unchanged.

### At capacity

A new weak entry may be rejected immediately if every stored entry has a better retention rank. In that case the semantic file must not be rewritten.

### Over capacity

The lowest-ranked entries are removed until the configured per-NPC maximum is satisfied.

### Consolidation interaction

Consolidation runs before retention selection. Independent corroboration therefore:

- consumes one retention slot;
- increases source evidence contribution;
- preserves all source UUIDs;
- can make the consolidated knowledge more durable without mutating confidence.

### NPC isolation

Retention is calculated independently inside each NPC owner's list. An entry for NPC B cannot evict or reinforce an entry for NPC A.

## Safety boundaries

The implementation must not:

- delete entries solely because time passed while under capacity;
- mutate `importance`, `confidence`, `provenance`, `kind`, statement, or source IDs;
- promote BELIEF to FACT;
- merge different provenance or related entities;
- use LLMs, embeddings, vector databases, semantic similarity, or provider calls;
- add a timer, background thread, scheduled task, or world-tick hook;
- change JSON format version or configuration format.

## Compatibility

- Minecraft: 1.21.1
- Java: 21
- Fabric package remains primary
- NeoForge compile compatibility remains required
- `semantic-memory.json` format remains version 1
- existing `maxEntriesPerNpc` remains the capacity control
- no new user configuration is introduced in this slice

## Testing strategy

### Pure policy tests

Prove that:

1. higher importance can preserve an older entry over a newer weak entry;
2. higher confidence affects retention independently of importance;
3. SYSTEM_OBSERVED outranks equally scored told or inferred beliefs;
4. additional independent sources increase durability with a six-source cap;
5. age lowers effective retention score;
6. future timestamps are treated as age zero;
7. selection is independent of input order;
8. tie-breaking is stable by UUID;
9. under-capacity selection keeps every entry.

### Store integration tests

Prove that:

1. consolidation occurs before retention selection;
2. retention is isolated per NPC;
3. a rejected weak append does not rewrite the file;
4. retained entries survive reload unchanged;
5. the old newest-only retention expectation is replaced by policy-based retention;
6. exact UUID replay remains a no-op.

### Full validation

Run the standard repository gates:

```text
:common:test + Fabric build/package
NeoForge build
Fabric build
```

A later live-server checkpoint must verify real retention pressure, persistence, restart stability, Chat, STT, TTS, Voice Chat, Opus, monitor, and ports.

## Non-goals

- absolute TTL expiration;
- scheduled cleanup;
- wall-clock decay;
- confidence mutation;
- contradiction resolution;
- automatic BELIEF production;
- legacy `memory.json` migration;
- NPC-to-NPC rumor propagation;
- configurable weights before live calibration.

## Acceptance criteria

1. Retention pressure considers age, importance, confidence, provenance, and source evidence.
2. Entries are never removed by this feature while the NPC is under capacity.
3. Selection is deterministic and input-order independent.
4. Consolidation precedes forgetting.
5. A rejected append does not rewrite the file.
6. FACT/BELIEF and provenance boundaries remain unchanged.
7. NPC data remains isolated.
8. Persistent format and configuration remain unchanged.
9. Full Fabric and NeoForge CI is green.
