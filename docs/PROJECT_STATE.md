# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md`. Read root `CHANGELOG.md` for product/release history and `docs/superpowers/evidence/` for detailed TDD evidence.
>
> Last reconciled: **2026-08-09**, after deterministic Semantic contradiction representation merged through PR #137.
>
> Always distinguish source/unit evidence, common integration, server GameTests, production-candidate evidence, exact-release evidence, and installed operator server/client evidence.

## Executive snapshot

VillAIgence is a Minecraft 1.21.1 MCA-derived mod evolving from AI-assisted villager dialogue into a persistent living-society simulation layer.

```text
repository:                         True-Ruslan/villAIgence
primary branch:                     1.21.1
Java:                               21
primary distribution:               Fabric
NeoForge:                           compile compatibility required

latest product merge:               PR #137
latest product merge commit:        afcd4f52187e1e419326abf9ae1ae7ac587f2064
latest official release:            0.2.0+1.21.1
latest release commit:              e426f588efefa6aa48a6e536c4a998421bbda241
installed 0.2.0 candidate JAR SHA:   56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee

next product slice:                 contradiction-aware prompt context without truth arbitration
then:                               uncertainty / bounded distortion
```

Current delivery state:

```text
0.1.x reliability/security baseline                    COMPLETE
MCA selective synchronization S1-S8                    COMPLETE
Operator Lore S9-S10c                                  COMPLETE
M11 Phases A-E                                         COMPLETE AT AUTOMATION LAYER
acceptance catalog                                     28 AUTOMATED / 6 MANUAL / 0 PLANNED
release/recovery automation                            COMPLETE / VERSION-AWARE

0.2 Memory 2.0 foundation                              SUBSTANTIALLY IMPLEMENTED
persistent-dialogue clean cutover                       COMPLETE / RELEASED
legacy memory.json migration                           CANCELLED BY DESIGN
0.2.0 clean-world installed acceptance                 7 PASS / 0 FAIL
controlled BELIEF admission                            COMPLETE / PR #123
bounded PLAYER_TOLD extraction                         COMPLETE / PR #125
causal relationship memory                             COMPLETE / PR #127
FACT > BELIEF prompt precedence                        COMPLETE / PR #129
long-horizon recall                                    COMPLETE / PR #131
NPC-to-NPC knowledge transfer                          COMPLETE / PR #133
bounded multi-hop rumor provenance                     COMPLETE / PR #135
Semantic contradiction representation                  COMPLETE / PR #137
contradiction-aware prompt context                     NEXT
```

Installed boundaries that remain explicit:

```text
VAI-M2-INST-005  NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004   NOT TESTED / DEFERRED
```

Neither is represented as PASS.

PRs #127, #129, #131, #133, #135 and #137 are merged, automated source capabilities after the already-installed `0.2.0` release. Their CI/candidate evidence must **not** be described as installed `0.2.0` acceptance until a later exact candidate is built and explicitly accepted.

---

# Product and architecture

Target product:

```text
NPC Identity
+ Personality
+ Memory
+ Relationships
+ Knowledge
+ Voice
+ Goals
+ Server-authoritative Actions
+ Settlements
+ Factions
+ Emergent History
= Persistent Living Society
```

## Architecture laws

1. **The LLM is never authority.** Minecraft/server-owned state is truth.
2. Mutable game state used asynchronously is captured into immutable bounded context first.
3. Provider/model changes must not redefine persistent NPC identity, memory, relationships or voice.
4. Provider and auxiliary-persistence failures fail soft whenever safe.
5. Retry/replay paths must not duplicate dialogue, memory, relationship or gameplay effects.
6. **FACT requires `SYSTEM_OBSERVED` server-owned evidence.**
7. **BELIEF remains non-authoritative** and may use only `PLAYER_TOLD`, `NPC_TOLD` or `INFERRED` provenance.
8. Confidence, repetition and corroboration never upgrade BELIEF into FACT.
9. Candidate extraction is not admission, and admission is not authority.
10. Operator Lore is explicit background context, not an observed current-world fact.
11. Current observed world facts override conflicting lore or recalled beliefs.
12. Clients never own permissions, target identity, file access, revisions or persistence mutations.
13. Compatibility work requires a supported-data reason; experimental pre-1.0 data is not automatically entitled to migration code.
14. Exact release identity must match tag, filename, embedded metadata and manifest.
15. Published artifacts must be byte-identical to the exact artifact accepted by the release gate.
16. Automated logical-client evidence never silently becomes installed multi-client evidence.
17. Unknown, unsafe, protected and persistence-store CI changes fail closed to the complete mandatory matrix.
18. Release recovery may repair metadata/assets only from an existing immutable release tag commit and never moves the tag.
19. A relationship cause records server-proven process linkage, not generated psychological truth.
20. Player-scoped prompt retrieval is an eligibility boundary: foreign-player memory is excluded before bounded allocation/ranking.
21. Snapshot prompt authority is structurally ordered: current observations → Operator Lore → Semantic Memory → episodic/social history.
22. Long-horizon recall remains hard-bounded and deterministic; durability never makes memory immortal.
23. NPC-to-NPC transfer is evidence-backed and local to the listener; it never creates implicit omniscience.
24. Multi-hop rumor provenance is immutable, server-backed, acyclic and capped at eight hops.
25. Canonical rumor ancestry is selected independently of the proposed listener; a cycle/limit rejection never falls back to a more convenient lower branch.
26. **Semantic contradiction is process metadata, not truth resolution.** Recording disagreement does not promote, rewrite, rank or delete either claim.
27. Historical contradiction evidence cannot resurrect forgotten claim prose; live contradiction resolution requires both logical claims to remain retained and eligible.

Canonical AI/state flow:

```text
Minecraft/server state
→ immutable bounded snapshot
→ deterministic context/authority layers
→ provider/LLM proposal
→ server validation/revalidation
→ server-owned mutation
→ persistent evidence
```

Semantic knowledge flow:

```text
SYSTEM_OBSERVED evidence
→ controlled FACT ingestion
→ FACT

persisted DIALOGUE/source evidence
→ bounded candidate text
→ server-owned provenance/source binding
→ BELIEF admission
→ BELIEF

speaker-owned retained Semantic FACT/BELIEF
→ exact source lookup + authoritative reread
→ listener-owned NPC_TOLD transfer evidence
→ exact evidence validation
→ listener BELIEF / NPC_TOLD

retained listener NPC_TOLD BELIEF
+ retained canonical v2 direct evidence
→ listener-independent ancestry resolution
→ cycle check → hop-limit check
→ next v2 direct evidence with immutable ancestry
→ downstream BELIEF / NPC_TOLD

retained Semantic claim A
+ retained Semantic claim B
→ exact server-owned A/B IDs + authoritative reread
→ deterministic SEMANTIC_CONTRADICTION process evidence
→ live resolved contradiction only while A and B remain retained/eligible
```

The provider may suggest bounded dialogue/claim text and relationship deltas where explicitly designed. It never chooses persistent truth class, source identity, rumor ancestry, contradiction identity/winner, visibility, retention or gameplay authority.

---

# Identity, configuration and persistence

```text
public name:      VillAIgence
short name:       VAI
Minecraft:        1.21.1
Java:             21
internal mod id:  mca
package root:     net.conczin.mca
config:           config/livingworld.json
world data root:  <world>/livingworld/
```

Compatibility-sensitive `mca`, `LivingWorld` and `livingworld` identifiers remain unchanged.

Current world-local stores:

```text
<world>/livingworld/memory2.json
<world>/livingworld/semantic-memory.json
<world>/livingworld/events.json
<world>/livingworld/relationships.json
<world>/livingworld/voices.json
<world>/livingworld/operator-lore.json
```

Current auxiliary corruption/recovery matrix:

```text
memory2.json
semantic-memory.json
relationships.json
voices.json
operator-lore.json
```

`events.json` remains authoritative factual event history with its own validation path.

The experimental pre-0.2 `memory.json` conversation store is no longer current runtime/recovery state. No importer, dual reader, checkpoint ledger or destructive migration is planned for the accepted pre-1.0 clean-state boundary.

Current Semantic BELIEF extraction config:

```json
{
  "semanticBeliefExtractionEnabled": false,
  "semanticBeliefMaxCandidatesPerTurn": 3
}
```

Extraction remains opt-in. Hard candidate count is `8`; statements are bounded to `240` Unicode code points. Config version remains `2`.

PR #137 adds no world file or public configuration. `SEMANTIC_CONTRADICTION` is an optional structured payload in the existing `memory2.json`; `memory2.json` and `semantic-memory.json` remain format version `1`. There is no backfill or persistence migration.

---

# Implemented Memory 2.0 systems

## Persistent dialogue and Working Memory

Released in `0.2.0+1.21.1`:

```text
usable text/voice result
→ Memory2DialogueLifecycle
→ structured DIALOGUE MemoryEvent
→ memory2.json

next turn
→ exact NPC/player DIALOGUE eligibility before limit
→ chronological user/assistant reconstruction
→ bounded Working Memory
```

`ConversationMemoryStore` and `MemoryMessage` are removed. `PersistentChatMemory` remains a no-storage compatibility façade only.

## Controlled BELIEF admission — PR #123

```text
PLAYER_TOLD → matching PLAYER_TOLD DIALOGUE → BELIEF
NPC_TOLD    → matching NPC_TOLD DIALOGUE    → BELIEF
INFERRED    → explicit persisted source     → BELIEF
SYSTEM_OBSERVED through BELIEF API          → REJECT
```

Owner/time/source identity comes from persisted evidence. Replay is idempotent; equivalent corroborating claims use deterministic source-union consolidation.

## Bounded PLAYER_TOLD extraction — PR #125

One structured OpenAI/OpenRouter response may carry bounded statement candidates. The server owns NPC/player/source/provenance/kind, requires the exact DIALOGUE to persist first, bounds/deduplicates candidate text, and never creates FACT through this path.

## Causal relationship memory — PR #127

`RELATIONSHIP_CHANGE` stores exact server-applied before/after relationship state. `RELATIONSHIP_CAUSE(DIALOGUE_TURN)` links that transition to exact persisted same-NPC/player/game-time DIALOGUE evidence. Generated retrospective prose is not authoritative cause and is not promoted to Semantic FACT.

## FACT > BELIEF prompt precedence — PR #129

Snapshot context renders exactly once in deterministic authority order:

```text
current observed world facts
→ Operator Lore
→ Semantic Memory
→ episodic / relationship social history
→ structured-response/tool instructions
```

Foreign-player Semantic/episodic/social memory is excluded before the candidate window. Shared current-player and NPC-global data remain eligible.

## Long-horizon recall — PR #131

At the normal hard candidate bound:

```text
24 newest eligible
+ 8 strongest durable eligible
→ deterministic de-duplication
→ existing domain ranker
→ at most 6 prompt records
```

Episodic/social retention uses server-owned importance, confidence, absolute emotional weight, provenance, type and authoritative Minecraft game time. No class is immortal.

## NPC-to-NPC knowledge transfer — PR #133

```text
exact persisted speaker Semantic source
→ authoritative reread
→ exact listener-owned DIALOGUE / NPC_TOLD evidence
→ exact reread/validation
→ listener BELIEF / NPC_TOLD
```

The caller cannot inject claim text, provenance, truth class, semantic scope, source-event IDs, importance or confidence. FACT authority is never copied to the listener. Replay and partial-retention outcomes are explicit and deterministic.

## Provenance-aware bounded multi-hop rumors — PR #135

Every new v2 direct transfer evidence may carry one immutable ancestry:

```text
Origin
  origin NPC / Semantic entry / kind / provenance / normalized statement / scope

Hop[]
  speaker / listener / speaker Semantic entry / evidence UUID / gameTime
```

Guarantees:

- first-hop origin: `FACT/SYSTEM_OBSERVED`, `BELIEF/PLAYER_TOLD`, or `BELIEF/INFERRED` only;
- `BELIEF/NPC_TOLD` cannot reset origin;
- downstream knowledge always remains `BELIEF/NPC_TOLD`;
- max depth exactly 8;
- cycles rejected;
- cycle precedes hop-limit for selected ancestry;
- canonical branch: `gameTime DESC → evidence UUID ASC`;
- resolver has no proposed-listener input;
- no listener-dependent fallback;
- Semantic sourceEventIds remain direct-only;
- ancestry remains on the direct evidence event;
- current observed FACT remains authoritative.

Deterministic coverage includes 10 NPCs, 8 admitted hops, rejected ninth hop, cycles, corroboration, >200 Semantic noise entries, >200 episodic/social noise entries, forward/reverse pressure order and fresh-root reload equality.

## Deterministic Semantic contradiction representation — PR #137

Merged as:

```text
PR:                                      #137
verified head:                           c20354e2cfa34b01cbcb8ea9da0b7edd68cadc1f
merge commit:                            afcd4f52187e1e419326abf9ae1ae7ac587f2064
```

### Stable logical claim identity

`SemanticMemoryIdentity` exposes the existing logical consolidation dimensions:

```text
owner NPC
+ kind
+ provenance
+ canonical NFKC/lowercase/whitespace statement
+ canonical sorted unique semantic subject scope
```

Source-event IDs are deliberately excluded, so identity survives deterministic source-union consolidation. Existing `semantic-consolidated-v1` entry IDs remain byte-compatible.

### Structured contradiction process evidence

`MemoryEvent.Type.SEMANTIC_CONTRADICTION` stores an optional structured payload in existing `memory2.json`:

```text
ClaimSnapshot A/B
  logicalClaimId
  exact detectedSemanticEntryId
  original kind
  original provenance
  canonical relatedEntities
```

Claim prose is deliberately **not duplicated** in contradiction evidence.

Deterministic `semantic-contradiction-v1` identity binds:

```text
owner NPC
+ both canonical ordered complete snapshots
+ authoritative gameTime
```

A/B and B/A therefore produce the same identity at the same time, while mutation of detected entry/kind/provenance/scope invalidates canonical evidence.

### Exact lifecycle

```text
exact source A/B IDs
→ exact owner-scoped reads
→ authoritative rereads
→ SAME_CLAIM / SCOPE_MISMATCH / SOURCE_NOT_RETAINED validation
→ canonical event append
→ exact reread + integrity validation
→ RECORDED or EVENT_NOT_RETAINED
```

The lifecycle changes neither Semantic claim. Exact replay is byte-idempotent; later detection time creates distinct bounded process evidence.

### Live resolved history

`SemanticContradictionHistory` resolves current claims by stable logical identity. It:

- survives source-union consolidation even if concrete Semantic entry ID changes;
- requires current kind/provenance/scope to match stored snapshots;
- applies global/private/shared player eligibility before limiting;
- ignores malformed evidence fail-closed;
- stops resolving a relation when either logical claim is forgotten;
- never parses event summary or uses historical contradiction evidence to recover forgotten claim text.

### Prompt isolation and authority

`SEMANTIC_CONTRADICTION` is explicitly excluded from generic `Memory2ContextProvider`. This prevents its `SYSTEM_OBSERVED` process provenance from being rendered as a generic `VERIFIED` factual memory before a dedicated contradiction prompt contract exists.

`SemanticMemoryIngestionAdapter.toFact(...)` does not accept contradiction events. Recording disagreement never promotes, rewrites, ranks or resolves either source claim.

### Preservation coverage

Tests exercise:

```text
fresh-root memory2.json + semantic-memory.json reload
source-union consolidation with concrete-ID replacement
forgetting without historical-text resurrection
malformed persisted contradiction input
exact replay / later-time identity
bounded event rejection without Semantic mutation
global/private/shared privacy before limit
240 unrelated Semantic records
240 unrelated episodic records
forward/reverse deterministic pressure snapshots
FACT/BELIEF kind/provenance/confidence preservation
existing current-FACT prompt authority
existing eight-hop rumor regressions
```

One suspected malformed-persistence NPE was investigated during review. The first test attempt was invalid because it introduced an unavailable Gson test dependency and was not counted as behavioral RED. The corrected real `memory2.json` production-path test passed, disproving the suspected defect; no production change was made.

### Exact-head delivery evidence

```text
Repository security policy #1893:       SUCCESS / run 31311225992
VillAIgence CI #2258:                   SUCCESS / run 31311225966
VillAIgence Production Soak #240:       SUCCESS / run 31311225982
VillAIgence GitHub Release #574:        SUCCESS / run 31311225980
release publication job:                SKIPPED
independent review P0/P1/P2:            0 / 0 / 0
open review threads:                    0
```

Main CI passed common/mock-provider, risk-selected GameTests/loaders, production acceptance/startup, persistence recovery and package verification. Soak passed constrained-heap concurrency, exact staging and five restart cycles. Release dry-run passed exact production acceptance/recovery, GameTests/loaders, package smoke and accepted-JAR/package identity without publishing a release.

Canonical implementation/TDD evidence:

```text
docs/superpowers/specs/2026-08-09-semantic-contradictions-design.md
docs/superpowers/plans/2026-08-09-semantic-contradictions.md
docs/superpowers/evidence/2026-08-09-semantic-contradictions-tdd.md
```

---

# Provider, voice and selective MCA systems

Provider/security baseline remains intact:

- OpenAI-compatible Chat/STT/TTS and OpenRouter-compatible Chat;
- endpoint/credential/redirect policy;
- bounded response/error bodies, retries/deadlines/backpressure;
- deterministic repository security policy and pinned Actions;
- no secrets/prompts/transcripts in diagnostics.

Voice automation retains exactly-once orchestration, mock-provider STT/Chat/TTS, real Simple Voice Chat Opus transport, cancellation/resource cleanup and constrained-heap soak. Physical microphone/UDP/audible-spatial checks remain installed/manual evidence categories.

Selective MCA corrections remain implemented for tombstone/inventory/resurrection integrity, HOME-bed safety, water/ladder/obstacle/door navigation, pathfinding watchdog, graveyard mourning, gifts, fishing/AquaCulture, mounted archer control and portable-grave/drop fallback.

---

# Automated acceptance and CI

Canonical acceptance catalog remains:

```text
34 total scenarios
28 AUTOMATED
6 MANUAL_CANARY
0 PLANNED
```

Permanent GitHub Actions remain fail-closed to the canonical workflow surface. Runtime/product changes exercise selected combinations of common/provider tests, risk-selected server GameTests, Fabric + NeoForge builds, production startup/restart, persistence recovery, package verification, repository security, constrained soak and release dry-run.

Recent product merges:

```text
PR #127  causal relationship memory
PR #129  FACT > BELIEF prompt precedence
PR #131  long-horizon recall
PR #133  NPC-to-NPC knowledge transfer
PR #135  bounded multi-hop rumor provenance
PR #137  Semantic contradiction representation
```

Detailed historical run IDs remain in root `CHANGELOG.md`, PR bodies and `docs/superpowers/evidence/`.

---

# Current official release boundary — 0.2.0+1.21.1

```text
tag:                     0.2.0+1.21.1
release commit:          e426f588efefa6aa48a6e536c4a998421bbda241
installed candidate SHA: 56293f86634b50b2def044429aac6f2cf0d197eb16ac1e60224708f7b3333aee
```

Installed clean-world Memory 2.0 result:

```text
VAI-M2-INST-001  PASS
VAI-M2-INST-002  PASS
VAI-M2-INST-003  PASS
VAI-M2-INST-004  PASS
VAI-M2-INST-006  PASS
VAI-M2-INST-007  PASS
VAI-M2-INST-008  PASS

Required total: 7 PASS / 0 FAIL
VAI-M2-INST-005: NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004:  NOT TESTED / DEFERRED
```

Canonical installed evidence:

```text
docs/livingworld/VALIDATION_0.2.0_CLEAN_WORLD_INSTALLED.md
```

PR #137 does **not** alter this installed-release claim.

---

# Changelog governance

Root `CHANGELOG.md` is the canonical product/release changelog. Notable runtime, persistence, public-config, release, security and permanent-CI changes update `[Unreleased]` in the same PR. `docs/CHANGELOG.md` remains historical engineering detail only.

PR #137 already updated root `[Unreleased]`; this docs-only reconciliation therefore changes only `docs/PROJECT_STATE.md` and `docs/ROADMAP.md`.

---

# Known gaps and technical debt

1. `VAI-CONCUR-004` real two-graphical-client Operator Lore presentation remains deferred.
2. `VAI-M2-INST-005` real second-player installed isolation remains untested; automated isolation exists.
3. Structured contradiction relations now exist and are queryable, but are intentionally **not yet rendered into provider prompt context**. A dedicated bounded prompt contract is the next product gap.
4. This slice provides exact contradiction representation/lifecycle/query but no automatic natural-language contradiction detector. Any future producer must remain candidate-only/server-bound and may not choose truth authority.
5. Uncertainty/confidence evolution and bounded distortion across rumor hops are not implemented.
6. Multi-hop knowledge transfer remains server-invoked; autonomous initiation and visible NPC↔NPC conversation/voice are later product slices.
7. Causal relationship history records deterministic `DIALOGUE_TURN` process linkage; richer psychological/told/inferred causal narratives remain out of scope.
8. `PersistentChatMemory` remains a no-storage compatibility façade.
9. Historical config fields for the removed raw conversation store remain deserializable compatibility baggage.
10. Historical Javadoc/deprecation warnings remain non-blocking.

---

# Next optimal delivery step

The next product slice is **contradiction-aware prompt context without truth arbitration**.

PR #137 deliberately kept `SEMANTIC_CONTRADICTION` out of generic episodic prompt retrieval. That was necessary to prevent a `SYSTEM_OBSERVED` process event from being mislabeled as a generic verified fact. Now that exact live contradiction resolution exists, the next safe step is a dedicated bounded context layer that tells the model two retained claims disagree **without** asking the model to decide server truth.

Required boundary:

```text
live resolved contradiction
→ current-player/NPC-global/shared eligibility
→ bounded deterministic contradiction context allocation
→ dedicated non-authoritative prompt wording
→ current SYSTEM_OBSERVED facts remain earlier/higher authority
→ no winner, no FACT promotion, no confidence mutation
```

Recommended TDD progression:

```text
contradiction prompt semantics/spec
→ RED: generic episodic path still excludes contradiction events
→ RED: dedicated context contains both currently retained claims only
→ RED: current FACT is rendered before contradiction context and declared authoritative
→ RED: foreign-player contradictions consume zero slots
→ RED: forgotten/malformed relation contributes zero context
→ RED: contradiction context has a hard candidate/result bound independent of all-pairs growth
→ RED: provider prompt cannot reinterpret contradiction metadata as instructions/tool authority
→ restart/pressure/multi-NPC deterministic simulation
→ full CI / GameTests / production / soak / release dry-run
```

Required invariants:

- contradiction remains process metadata, not truth resolution;
- neither side wins by confidence, recency, repetition or corroboration count;
- current `SYSTEM_OBSERVED` FACT remains structurally authoritative;
- prompt context uses live resolved Semantic text, never historical duplicated prose;
- player visibility is enforced before bounded allocation;
- existing Semantic `32` candidate / `24+8` / `6` result bounds are unchanged unless a separate measured design explicitly changes them;
- existing eight-hop rumor provenance is unchanged;
- no new store/config/provider request/migration;
- no legacy `memory.json` reader returns.

After contradiction-aware prompt context:

```text
uncertainty / bounded distortion design
→ bounded producer/detector policy for contradiction candidates where justified
→ settlement-scale information flow without omniscience
→ relationship/trust effects on belief confidence as a separate social-epistemology slice
```

---

# Session handoff protocol

For a new development session:

1. read `docs/PROJECT_STATE.md`;
2. read `docs/ROADMAP.md`;
3. read root `CHANGELOG.md`;
4. inspect current `1.21.1` HEAD;
5. inspect open/recent PRs, tags/releases and current CI;
6. reconcile live GitHub state against these documents before changing code;
7. use TDD for runtime behavior: specification → RED → minimal GREEN → focused regression → complete selected gates;
8. keep root changelog and canonical state docs synchronized with delivery boundaries.

Do not infer PASS from stale documentation. GitHub state and exact evidence must be checked each session.