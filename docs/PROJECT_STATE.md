# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work.
>
> Last major state update: **2026-07-30**, after merged PR #56.
>
> Reconcile this state with newer PRs, tags/releases, CI and live-server evidence before starting development.

## Executive snapshot

VillAIgence is a Minecraft 1.21.1 MCA-derived mod evolving from AI-assisted villager dialogue into a persistent living-society simulation layer.

```text
primary branch: 1.21.1
Java: 21
primary package: Fabric
NeoForge: compile compatibility required

latest implementation:
PR #56 — deterministic Semantic Memory forgetting and decay
merge: 73145dd0925d403af7ef343521eb3ae27f68804d
exact verified feature head: c08b47431b6a121deae4be8410be1e4fe4c5126a

exact-head CI:
VillAIgence CI #764 / 30573965448 — SUCCESS
Java Pull Request CI #307 / 30573965439 — SUCCESS

latest live-validated release checkpoint:
0.1.13+1.21.1 — PASS
validation date: 2026-07-30
tested release commit: b553bf7e83674145bdf42927b9ace7287afa560c
```

**Status boundary:** deterministic forgetting/decay is merged and automated-CI validated, but not yet validated on a real server. `0.1.13+1.21.1` remains the latest live-server checkpoint.

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.13.md
docs/livingworld/SEMANTIC_FORGETTING_DECAY.md
docs/superpowers/specs/2026-07-30-memory2-semantic-forgetting-decay-design.md
docs/superpowers/plans/2026-07-30-memory2-semantic-forgetting-decay.md
```

## Release metadata status

```text
0.1.11+1.21.1 → 60236524e37b60c639b93405f809ade883be253f
0.1.12+1.21.1 → 746fa75ab4b5f4bee385efa0c8ae51009c1aec58
0.1.13+1.21.1 → b553bf7e83674145bdf42927b9ace7287afa560c
```

`0.1.13` points at the exact consolidation payload tested on the server. Branch `1.21.1` subsequently advanced through validation documentation and PR #56. No release containing PR #56 has yet been live-tested.

---

# Identity and compatibility

```text
public name: VillAIgence
short name: VAI
tagline: Giving villagers a mind of their own.
repository: True-Ruslan/villAIgence
Minecraft: 1.21.1
Java: 21
```

Compatibility-sensitive identifiers:

```text
mod id: mca
Java package root: net.conczin.mca
config: config/livingworld.json
world data root: <world>/livingworld/
internal engine/data naming: LivingWorld / livingworld
```

Do not rename these without a dedicated migration design.

---

# Architecture laws

1. **LLM is never authoritative.** Minecraft/server-owned state is truth.
2. Mutable state used by async AI must be captured into immutable bounded context before provider work.
3. LLM may propose dialogue, actions and relationship deltas; server policy validates, revalidates and executes mutations.
4. Provider/model changes must not redefine persistent NPC identity.
5. External AI and auxiliary persistence failures fail soft whenever safe.
6. API credentials remain server-side.
7. Persistent formats remain explicit, inspectable and backed up with the world.
8. Retry/replay paths must not duplicate persistent or gameplay side effects.
9. Claims and beliefs remain non-authoritative unless server-owned evidence makes them factual.
10. Confidence never upgrades BELIEF into FACT.
11. Consolidation preserves provenance and every independent source event.
12. Forgetting is a deterministic storage decision, not an LLM decision or a confidence mutation.
13. Time alone must not delete semantic knowledge while the NPC is under capacity.
14. Autonomous AI must eventually be event-driven and budgeted rather than “LLM every tick.”

Canonical authority and retention flow:

```text
Minecraft/server state
→ immutable bounded context
→ provider/LLM proposal
→ server validation/revalidation
→ server-owned mutation
→ persisted authoritative evidence
→ bounded Memory 2.0 ingestion
→ deterministic consolidation
→ deterministic retention under capacity pressure
```

Semantic truth boundary:

```text
FACT   → SYSTEM_OBSERVED only
BELIEF → PLAYER_TOLD / NPC_TOLD / INFERRED only
```

Current server-observed `worldFacts` win when recalled memory conflicts with current state.

---

# Persistent world-local data

```text
<world>/livingworld/memory.json          bounded rolling dialogue history
<world>/livingworld/memory2.json         episodic MemoryEvent store
<world>/livingworld/semantic-memory.json typed Semantic FACT/BELIEF store
<world>/livingworld/events.json          server-owned world events
<world>/livingworld/relationships.json   player↔NPC relationship state
<world>/livingworld/voices.json          persistent NPC voice identity
```

All files belong with world backup/restore procedures.

`memory.json` remains active. Memory 2.0 is additive; legacy migration has not started.

---

# Reliability foundation

Implemented and retained:

- OpenAI-compatible/OpenRouter server-side configuration;
- bounded timeouts and controlled provider failures;
- safe `content:null`, empty-response and retry handling;
- Chat/STT/TTS admission limits and provider cooldown;
- Simple Voice Chat → PCM → STT → targeted NPC → AI;
- optional spatial TTS, PCM/WAV compatibility and resampling;
- persistent per-NPC voice identity;
- safe whitelisted actions with server validation/revalidation;
- authoritative world events and relationship persistence;
- immutable context snapshots;
- structured-response sanitation;
- fail-open malformed auxiliary JSON recovery;
- diagnostics without secrets, prompts, transcripts or hidden reasoning.

Reliability policy:

> Fix concrete regressions reproduced by CI or live use. Do not mix speculative provider or voice refactors into Memory 2.0 work.

---

# Memory 2.0 — implemented state

## Episodic Memory

Implemented through PRs #31, #33, #35, #37, #39, #41 and #43:

- immutable NPC-owned `MemoryEvent`;
- `DIALOGUE`, `OBSERVATION`, `ACTION`, `RELATIONSHIP_CHANGE`;
- explicit provenance;
- bounded per-NPC persistence;
- UUID idempotency and deterministic ordering;
- atomic writes and fail-open recovery;
- deterministic retrieval;
- authoritative ACTION ingestion;
- server-observed RELATIONSHIP_CHANGE ingestion;
- text and voice DIALOGUE parity.

Ranking:

```text
relevance  40%
importance 25%
recency    20%
confidence 15%

candidateLimit = 32
maxResults = 6
recencyHorizonTicks = 168000
```

## Working Memory — PR #46

```text
recent dialogue
+ selected episodic context
+ selected semantic context
→ WorkingMemoryContext
```

Hard prompt bounds:

```text
recent dialogue messages = 12
max dialogue message = 1200 Unicode code points
episodic entries = 6
semantic entries = 6
```

Working Memory is turn-local and not persisted.

## Semantic Memory foundation — PR #46

Implemented:

```text
SemanticMemoryEntry
SemanticMemoryStore
SemanticMemoryQuery
SemanticMemoryRetriever
SemanticMemoryContextFormatter
SemanticMemoryContextProvider
```

Store guarantees:

- NPC isolation;
- bounded retention;
- UUID idempotency;
- deterministic ordering and retrieval;
- atomic persistence;
- fail-open malformed-file recovery.

Semantic retrieval ranking:

```text
related-entity relevance 40%
importance               30%
confidence               20%
recency                  10%
```

Prompt layers remain separate:

```text
worldFacts               authoritative current state
memoryContext            episodic memory
semanticMemoryContext    semantic FACT/BELIEF memory
```

## Controlled semantic ingestion — PR #49

```text
successful safe action
→ ACTION / SYSTEM_OBSERVED
→ semantic FACT

persisted relationship transition
→ RELATIONSHIP_CHANGE / SYSTEM_OBSERVED
→ semantic FACT
```

Automatic FACT requires:

```text
provenance = SYSTEM_OBSERVED
type in {ACTION, OBSERVATION, RELATIONSHIP_CHANGE}
```

Critical exclusion:

```text
DIALOGUE
→ episodic only
→ no automatic semantic entry
```

An explicit sourced BELIEF API exists, but ordinary dialogue is not automatically converted to BELIEF.

PR #49 was live-validated by `0.1.12+1.21.1`.

## Deterministic semantic consolidation — PR #53

```text
merge: f85879d254f37d7f860380362b296e047bbbb781
verified head: 19c3d3e840431cc2b1b34e1841e2075f56e99f71
VillAIgence CI #746 / 30561015885 — SUCCESS
Java Pull Request CI #300 / 30561015985 — SUCCESS
```

Consolidation key:

```text
ownerNpcId
kind
provenance
canonical statement
canonical relatedEntities set
```

Both entries require source event IDs. Compatible entries produce one deterministic UUID with sorted source union, related-entity union and maximum time/importance/confidence fields.

Safety boundaries:

- exact UUID replay is a no-op;
- FACT never merges with BELIEF;
- different BELIEF provenance never merges;
- different related entities never merge;
- unsourced entries never merge;
- confidence is not artificially increased;
- no fuzzy matching, LLM, embeddings or vector database;
- JSON format remains version 1.

`0.1.13+1.21.1` live-validates the primary append-time consolidation path.

## Deterministic semantic forgetting/decay — PR #56

```text
merge: 73145dd0925d403af7ef343521eb3ae27f68804d
verified head: c08b47431b6a121deae4be8410be1e4fe4c5126a
VillAIgence CI #764 / 30573965448 — SUCCESS
Java Pull Request CI #307 / 30573965439 — SUCCESS
```

Implemented production component:

```text
SemanticMemoryRetentionPolicy
```

Forgetting is pressure-based:

```text
entries <= maxEntriesPerNpc
→ retain every valid unique entry

entries > maxEntriesPerNpc
→ consolidate first
→ rank retention deterministically
→ retain best bounded set
```

Durability formula:

```text
importance × 4
+ confidence × 5 / 2
+ provenance contribution
+ min(sourceEventIds count, 6) × 25
```

Provenance contribution:

```text
SYSTEM_OBSERVED = 200
PLAYER_TOLD     = 100
NPC_TOLD        = 75
INFERRED        = 25
```

Decay formula:

```text
DECAY_STEP_TICKS = 36000
ageTicks = max(0, nowGameTime - entry.gameTime)
effectiveRetentionScore = durability × 36000 - ageTicks
```

Deterministic tie-breakers:

```text
effective score descending
importance descending
confidence descending
source count descending
gameTime descending
createdAtEpochMillis descending
UUID ascending
```

Safety properties:

- no deletion while under capacity;
- no timer, background thread, wall-clock TTL or world-tick hook;
- no mutation of confidence, importance, provenance, kind or evidence;
- consolidation occurs before forgetting;
- corroboration adds bounded durability without confidence inflation;
- rejected weak append does not rewrite `semantic-memory.json`;
- exact replay remains a no-op;
- retention is isolated per NPC;
- JSON and configuration formats are unchanged.

TDD evidence:

```text
policy RED:
1ea407ab2cd16eb74ea86dacb1aa04e476341e34
VillAIgence CI #756 / 30572701052 — expected FAILURE

policy GREEN:
0410b8c4b9bcbe604effca2154d092ef6a2af1a5
VillAIgence CI #758 / 30572959755 — SUCCESS
Java Pull Request CI #304 / 30572959844 — SUCCESS

store RED:
4c42a3f13f73657fade630fa6fd212e0a7677657
VillAIgence CI #760 / 30573293522 — expected FAILURE

final GREEN:
c08b47431b6a121deae4be8410be1e4fe4c5126a
VillAIgence CI #764 / 30573965448 — SUCCESS
Java Pull Request CI #307 / 30573965439 — SUCCESS
```

---

# Live validation status

## 0.1.13+1.21.1 — PASS

Validated on a real server:

```text
two same-knowledge authoritative ACTION events             PASS
distinct ACTION UUIDs                                      PASS
one consolidated Semantic Memory entry                     PASS
both sourceEventIds present exactly once                   PASS
deterministic UUID independently reproduced                PASS
retry created new ACTION                                   no
retry changed semantic file                                no
NPC A / NPC B owner isolation                              PASS
NPC A / NPC B relatedEntities isolation                    PASS
```

Byte-identical after restart:

```text
memory.json
memory2.json
semantic-memory.json
relationships.json
voices.json
```

Operations:

```text
0.1.13 reloaded                                            PASS
Chat and DIALOGUE                                          PASS
Voice Chat / Opus                                          PASS
STT / TTS voice dialogue                                   PASS
UDP 24454 / 25565                                          PASS
LinuxGSM monitor                                           PASS
server STARTED                                             PASS
VillAIgence / persistence / OutOfMemory errors             none
```

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.13.md
```

## PR #56 validation boundary

Not yet live-proven:

- actual retention pressure on a real server;
- older strong semantic knowledge surviving newer weak knowledge;
- predicted rejection/eviction using the exact score formula;
- corroboration durability affecting a real capacity decision;
- byte-identical `semantic-memory.json` after a rejected append;
- retention persistence across restart;
- voice/chat operational stability in a build containing PR #56.

Do not call forgetting/decay live-validated until the scenario below passes.

---

# Roadmap status

## 0.1.x Reliability

Stable and live-validated through `0.1.13+1.21.1`. Continue only for concrete defects or explicit soak, backup/restore or provider-failure goals.

## 0.2 Memory 2.0 — active and substantially advanced

Implemented and live-proven through `0.1.13`:

```text
Episodic MemoryEvent model and persistence
+ deterministic episodic retrieval
+ ACTION / DIALOGUE / RELATIONSHIP_CHANGE ingestion
+ text / voice parity
+ Working Memory bounds
+ Semantic FACT/BELIEF model and persistence
+ deterministic semantic retrieval
+ shared layered prompt integration
+ controlled semantic FACT ingestion
+ deterministic semantic consolidation
+ source-union and retry idempotency
+ NPC and related-entity isolation
```

Implemented and CI-proven, pending live validation:

```text
deterministic pressure-based forgetting/decay
```

Still not implemented or not proven:

- live-server forgetting/decay validation;
- automatic controlled BELIEF producers;
- legacy `memory.json` migration;
- NPC-to-NPC knowledge and rumor propagation;
- trustworthy causal relationship reasons;
- long-horizon recall after days without full raw dialogue;
- large multiplayer and multi-day soak validation;
- live test of manually seeded pre-existing semantic duplicates.

## Next sequence

```text
1. Live-validate PR #56 forgetting/decay
2. Calibrate only if live evidence exposes a concrete retention defect
3. Design legacy memory.json migration after semantic layers stabilize
4. Run long-horizon Memory 2.0 exit-criterion validation
5. Begin 0.3 Personality + NPC↔NPC social graph
```

No embeddings, vector DB or LLM truth classification should be prerequisites.

---

# Immediate live-test scenario

```text
1. Install a build containing PR #56.
2. Back up memory.json, memory2.json, semantic-memory.json, relationships.json and voices.json.
3. Reach semantic capacity for NPC A using a controlled small limit or enough eligible events.
4. Create an older high-importance/high-confidence authoritative FACT.
5. Create newer low-importance/low-confidence entries until pressure occurs.
6. Calculate durability and effective retention scores independently.
7. Confirm the predicted stronger UUID remains and the weakest candidate is rejected or evicted.
8. Add corroborating source evidence for one consolidation key.
9. Confirm sourceEventIds contains every source exactly once and the evidence contribution changes durability as predicted.
10. Repeat a weak rejected append and confirm semantic-memory.json remains byte-identical.
11. Apply equivalent pressure to NPC B and confirm owner/related-entity isolation.
12. Restart the server and compare all five file hashes.
13. Confirm retained semantic UUIDs and evidence survive restart.
14. Confirm Chat, DIALOGUE, STT, TTS, Voice Chat, Opus, monitor, UDP 24454 and 25565 remain healthy.
15. Confirm no VillAIgence persistence or OutOfMemory errors.
```

After success, create a validation document and promote the tested build to the latest live-server checkpoint.

---

# New-session handoff protocol

Preferred resume prompt:

> **Open `docs/PROJECT_STATE.md`, `docs/CHANGELOG.md`, `docs/ROADMAP.md` and the latest validation evidence in `True-Ruslan/villAIgence`. Check recent PRs, tags/releases and CI, then tell me what is implemented, what is live-validated, what changed since the state file, and what should be built next.**

A new session must:

1. read this file;
2. read `docs/CHANGELOG.md`;
3. read `docs/ROADMAP.md`;
4. inspect current `1.21.1` HEAD;
5. inspect recent merged/open PRs;
6. inspect latest tag/release and CI state;
7. reconcile newer live-test evidence;
8. continue from the first unimplemented priority;
9. update canonical state after material progress.

```text
docs/ROADMAP.md       → where the project is going
docs/PROJECT_STATE.md → what exists, what is proven, and what comes next
docs/CHANGELOG.md     → material implementation and validation history
```
