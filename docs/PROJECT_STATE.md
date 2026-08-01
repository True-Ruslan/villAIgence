# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work.
>
> Last major state update: **2026-08-01**, after the official `0.1.17+1.21.1` release JAR passed the focused SEC-004 acceptance and Step 1 Security and supply-chain hardening reached full closure.
>
> Reconcile this state with newer PRs, tags/releases, CI and live-server evidence before starting development.

## Executive snapshot

VillAIgence is a Minecraft 1.21.1 MCA-derived mod evolving from AI-assisted villager dialogue into a persistent living-society simulation layer.

```text
primary branch: 1.21.1
Java: 21
primary package: Fabric
NeoForge: compile compatibility required

latest completed engineering program:
Step 1 Security and supply-chain hardening — complete
implementation PRs: #59, #60, #61, #62, #63
repository closure: #64
acceptance harness: #68
acceptance reconciliation and SEC-004 fix: #70
latest merged security implementation: 88a20d86e8b08e4b5eaf60da943a63e750f2b545

final Step 1 finding state:
SEC-001 through SEC-009 — Closed

latest gameplay/memory implementation:
PR #56 — deterministic Semantic Memory forgetting and decay
merge: 73145dd0925d403af7ef343521eb3ae27f68804d
exact verified feature head: c08b47431b6a121deae4be8410be1e4fe4c5126a

installed server checkpoint:
0.1.16+1.21.1
commit: 521568f903078b91dd5817cdc9a551bd2392e663
JAR SHA-256: 036cbacc657ceb676813f41ee293024690b981e971e7c6037fc5d3ecbe3ee062
status: full hostile-provider, PCM, production restoration and restart acceptance PASS

final security artifact checkpoint:
0.1.17+1.21.1
commit: 88a20d86e8b08e4b5eaf60da943a63e750f2b545
JAR SHA-256: b33af40f7a2696dc679c49e0fc544f6b5df99e0aa600ea5c767bc5a9747da1ab
marker: V0117_SEC004_ARTIFACT_AND_EVIDENCE_PASS
status: focused SEC-004 exact-release-JAR acceptance PASS
```

**Status boundary:** `0.1.14+1.21.1` live-proves deterministic forgetting/decay, source durability, existing-entry eviction, persistence and NPC isolation. `0.1.15+1.21.1` live-proves normal production Chat/STT/TTS, endpoint rejection, TTS fail-soft behavior and six-file restart durability. `0.1.16+1.21.1` live-proves provider response bounds, oversized error handling, no redirects, the ten-minute slow-drip deadline, stage-specific persistence, voice duration clamps, the 128 MiB PCM budget, production restoration and final restart stability. `0.1.17+1.21.1` exact-artifact validation closes the final verification-probe discrepancy by rejecting HTTPS loopback before connection while retaining HTTP success, redirect and 64 KiB oversize behavior.

The server remained on `0.1.16+1.21.1` during the `0.1.17` focused probe test. This is intentional: PR #70 changed only the explicit acceptance-probe URI validator and documentation, not Minecraft runtime behavior.

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.14.md
docs/livingworld/VALIDATION_0.1.15.md
docs/livingworld/VALIDATION_0.1.16.md
docs/livingworld/VALIDATION_0.1.17.md
docs/security/SECURITY_AUDIT_FOLLOW_UP_2026-07-31_RUNTIME_0.1.15.md
docs/security/SECURITY_AUDIT_FOLLOW_UP_2026-08-01_RUNTIME_0.1.16.md
docs/security/SECURITY_AUDIT_FOLLOW_UP_2026-08-01_RUNTIME_0.1.17.md
docs/security/STEP_1_TRACKER.md
docs/security/README.md
```

---

# Release metadata status

```text
0.1.11+1.21.1 → 60236524e37b60c639b93405f809ade883be253f
0.1.12+1.21.1 → 746fa75ab4b5f4bee385efa0c8ae51009c1aec58
0.1.13+1.21.1 → b553bf7e83674145bdf42927b9ace7287afa560c
0.1.14+1.21.1 → c45aea45dd915b24ba236344feef30559c7171bb
0.1.15+1.21.1 → 26070c37b806897e37cc3dabe2e4b27af458ac20
0.1.16+1.21.1 → 521568f903078b91dd5817cdc9a551bd2392e663
0.1.17+1.21.1 → 88a20d86e8b08e4b5eaf60da943a63e750f2b545
```

Release roles:

```text
0.1.14  forgetting/decay retention-pressure checkpoint
0.1.15  normal production and endpoint-policy checkpoint
0.1.16  installed full hostile-provider/PCM server checkpoint
0.1.17  final SEC-004 exact-artifact checkpoint
```

Do not describe `0.1.17` as installed or live-tested inside Minecraft unless that occurs later. Its accepted scope is the exact official JAR probe surface.

---

# Step 1 security and supply-chain hardening — complete

## Merge sequence

```text
H1 provider endpoint and credential policy     PR #59 → 787f1a781b5970d4bafb851bfb3c7cba7c21fc0a
H2 bounded network and voice resources         PR #60 → 15c56526417ac7dfb76567d51d1aa107f522cda7
H3 immutable verified build inputs             PR #61 → 4cf9aef2e5c31a5682a7cad8544219154330e056
H4 primary CI and repository security policy   PR #62 → 05d105c1f558d5643b8190a88cc744b4d7cbe129
H5 legacy utility and whole-tree closure       PR #63 → 6d82b4e4650294a4a42b9ea2113e64d990e08811
canonical repository closure                   PR #64 → 26070c37b806897e37cc3dabe2e4b27af458ac20
residual acceptance tooling                    PR #68 → 521568f903078b91dd5817cdc9a551bd2392e663
acceptance reconciliation / probe fix          PR #70 → 88a20d86e8b08e4b5eaf60da943a63e750f2b545
```

## Implemented controls

- normalized endpoint validation and endpoint-family credential binding;
- remote plaintext provider endpoints rejected except explicit literal-loopback development mode;
- authenticated provider redirects blocked;
- Chat, STT, TTS, provider-error and verification responses byte-bounded;
- hard ten-minute total response-body deadline;
- production account verification fixed to a trusted HTTPS origin;
- acceptance verification probe restricted to HTTP literal loopback;
- voice capture clamped to `1..120` seconds;
- aggregate active PCM memory bounded to 128 MiB;
- stable Fabric Loom and verified Gradle wrapper;
- dependency verification metadata and lockfiles;
- immutable GitHub Actions references;
- required common, Fabric and NeoForge CI;
- deterministic secret, dangerous-source, workflow and script policy;
- exact-head tracked-tree and script manifests;
- removal of inherited non-CI network and generation utilities;
- loopback hostile-provider harness with sanitized evidence;
- exact-release-JAR verification and PCM probes;
- package smoke checks for the acceptance classes.

## Final finding matrix

```text
SEC-001 Closed
SEC-002 Closed
SEC-003 Closed
SEC-004 Closed
SEC-005 Closed
SEC-006 Closed
SEC-007 Closed
SEC-008 Closed
SEC-009 Closed
```

Step 1 is complete within its defined implementation and acceptance scope. Future security-sensitive changes remain subject to the established policy; Step 1 completion is a baseline, not a permanent exemption from review.

## Acceptance tooling boundary

```text
scripts/security/provider_acceptance_harness.py
→ literal loopback bind only
→ declared/chunked/error/redirect/slow-drip routes
→ streamed hostile payloads
→ sanitized manifest and JSONL evidence

AccountVerificationAcceptanceProbe
→ explicit java -cp invocation only
→ HTTP literal-loopback target only
→ shared bounded/no-redirect verification transport

VoicePcmBudgetAcceptanceProbe
→ explicit java -cp invocation only
→ exact 1..120 second clamp
→ exact 128 MiB contention, rejection, release and recovery
```

No acceptance tool has an in-game command, Minecraft startup hook, production-key lookup or persistence schema effect.

## Dependency-verification maintenance note

During the `0.1.17` validation window, one local environment resolved transitive Fabric dependencies for which the committed verification metadata did not contain matching checksum records. Gradle stopped before compilation.

Security interpretation:

```text
verification disabled or bypassed   no
unverified dependency accepted      no
local artifact used                 no
official release artifact used      yes
exact SHA-256 recorded              yes
```

The failure was fail-closed and does not invalidate the accepted official release JAR. Supporting that additional local resolution graph is a non-blocking build-maintenance task. Any metadata refresh must use `docs/security/DEPENDENCY_UPDATE_PROCEDURE.md`; do not disable dependency verification.

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
6. API credentials remain server-side and endpoint-bound.
7. Persistent formats remain explicit, inspectable and backed up with the world.
8. Retry/replay paths must not duplicate persistent or gameplay side effects.
9. Claims and beliefs remain non-authoritative unless server-owned evidence makes them factual.
10. Confidence never upgrades BELIEF into FACT.
11. Consolidation preserves provenance and every independent source event.
12. Forgetting is a deterministic storage decision, not an LLM decision or confidence mutation.
13. Time alone must not delete semantic knowledge while the NPC is under capacity.
14. Retention pressure must remain isolated per NPC owner.
15. Autonomous AI must eventually be event-driven and budgeted rather than “LLM every tick.”
16. Migration must be additive, deterministic, idempotent and reversible until explicit cutover evidence exists.

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

`memory.json` remains active. Memory 2.0 is additive; legacy migration has not started. Do not delete or rewrite `memory.json` as part of initial migration work.

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
- diagnostics without secrets, prompts, transcripts or hidden reasoning;
- validated provider destinations and endpoint-aware credentials;
- no authenticated provider redirects;
- bounded network bodies and voice memory;
- stable verified Gradle, dependency and action inputs;
- required common, Fabric and NeoForge CI;
- deterministic repository security policy.

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

Safety boundaries:

- both entries require source event IDs;
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
- rejected weak append does not rewrite `semantic-memory.json` in automated tests;
- exact replay remains a no-op;
- retention is isolated per NPC;
- JSON and configuration formats are unchanged.

`0.1.14+1.21.1` live-validates real retention pressure, source durability, decay ordering, weak existing-entry eviction, persistence and NPC isolation.

---

# Validation checkpoints

## 0.1.17+1.21.1 — SEC-004 exact-artifact PASS

```text
commit: 88a20d86e8b08e4b5eaf60da943a63e750f2b545
JAR SHA-256: b33af40f7a2696dc679c49e0fc544f6b5df99e0aa600ea5c767bc5a9747da1ab
marker: V0117_SEC004_ARTIFACT_AND_EVIDENCE_PASS
```

Validated directly from the official release JAR:

```text
verification success                         SUCCESS / 200
verification redirect                        HTTP_ERROR / 307
redirect_target_hits                         0
declared/chunked verification oversize       TOO_LARGE
HTTPS loopback                               rejected before connection
HTTPS rejection exit                         2
SSLException                                 none
sanitized harness HTTP requests              4
```

Minecraft server impact:

```text
server release changed   no
mods changed             no
restart performed        no
world/config changed     no
```

## 0.1.16+1.21.1 — full controlled server acceptance PASS

```text
commit: 521568f903078b91dd5817cdc9a551bd2392e663
JAR SHA-256: 036cbacc657ceb676813f41ee293024690b981e971e7c6037fc5d3ecbe3ee062
```

Live-proven:

```text
Chat JSON limit                                      8 MiB PASS
STT JSON limit                                       4 MiB PASS
TTS audio limit                                     64 MiB PASS
provider error body                                256 KiB PASS
slow-drip total deadline                            600.026 s PASS
Chat/STT/TTS redirects                              not followed
redirect_target_hits                                0
TTS failure preserved visible text/DIALOGUE         PASS
voice duration clamp                                1..120 seconds PASS
PCM peak                                             128 MiB exactly
PCM accepted/rejected                                128 / 128
PCM final bytes                                      0
PCM recovery                                         PASS
production Chat/STT/TTS/Opus                         PASS
production config byte restoration                   PASS
six-file final restart diff                          empty
secrets / OutOfMemoryError                           none
server / ports / monitor                             healthy
```

## 0.1.15+1.21.1 — normal production and endpoint-policy PASS

```text
commit: 26070c37b806897e37cc3dabe2e4b27af458ac20
JAR SHA-256: af142be94885541bb4840d0effff73627afe3f0e245dec8307ed665701cc94fb
```

Live-proven:

```text
Text / STT / TTS                                      PASS
Pio / Justino isolation                               PASS
Pio name and favorite-color recall                    PASS
TTS io_error preserved text and DIALOGUE              PASS
OpenRouter HTTP 429 remained controlled               PASS
six persistent files hash-identical after restart    PASS
LAN HTTP / lookalike / user-info / fragment rejected PASS
production config restored byte-for-byte              PASS
keys or Authorization leaked                          no
server / UDP 24454 / TCP 25565 / monitor              healthy
```

## 0.1.14+1.21.1 — forgetting/decay PASS

Validation marker:

```text
V0114_FINAL_RESTART_VERIFICATION_PASS
```

Controlled test configuration:

```text
semantic capacity during pressure test = 3
semantic capacity after test           = 256
```

Live-proven:

```text
older corroborated Basiliso FACT survived pressure        PASS
Basiliso semantic UUID and sourceEventIds preserved       PASS
source-evidence durability affected retention             PASS
decay ordering among equal entries                        PASS
weak Casimiro RELATIONSHIP_CHANGE FACT evicted            PASS
Basiliso/Casimiro pressure isolated by owner               PASS
```

Explicit remaining boundary:

```text
rejection of a newly appended weak candidate without file rewrite
→ automated-test proven
→ not reached in the live gameplay pipeline because the Chat model did not emit RELATIONSHIP_CHANGE
```

This does not block the forgetting/decay checkpoint or the next product milestone.

## Previous live checkpoints

```text
0.1.13+1.21.1 — semantic consolidation
0.1.12+1.21.1 — controlled semantic ingestion
0.1.11+1.21.1 — Working Memory
0.1.10+1.21.1 — text/voice Memory 2.0 parity
```

Evidence:

```text
docs/livingworld/VALIDATION_0.1.13.md
docs/livingworld/VALIDATION_0.1.12.md
docs/livingworld/VALIDATION_0.1.11.md
docs/livingworld/VALIDATION_0.1.10.md
```

---

# Roadmap status

## 0.1.x Reliability and provider hardening — complete baseline

The defined runtime and security baseline is implemented and accepted across `0.1.15`, `0.1.16` and `0.1.17` evidence. Future provider or voice work should be triggered by concrete regressions or a separately scoped product requirement.

## 0.2 Memory 2.0 — active and substantially advanced

Implemented and live-proven:

```text
Episodic MemoryEvent model and persistence
+ deterministic episodic retrieval
+ ACTION / DIALOGUE / RELATIONSHIP_CHANGE ingestion
+ text / voice parity
+ Working Memory bounds
+ Semantic FACT/BELIEF model and persistence
+ deterministic semantic retrieval
+ layered prompt integration
+ controlled semantic FACT ingestion
+ deterministic semantic consolidation
+ source-union and retry idempotency
+ deterministic pressure-based forgetting/decay
+ source durability
+ existing weak-entry eviction
+ NPC and related-entity isolation
+ restart-safe persistence
```

Still not implemented or not fully proven:

- deterministic live path for rejected-new-append no-rewrite;
- automatic controlled BELIEF producers;
- legacy `memory.json` migration;
- NPC-to-NPC knowledge and rumor propagation;
- trustworthy causal relationship reasons;
- long-horizon recall after days without full raw dialogue;
- large multiplayer and multi-day soak validation;
- live test of manually seeded pre-existing semantic duplicates.

## Next optimal development step

Design and implement **legacy `memory.json` migration — Phase 1**.

Required design properties:

```text
additive, not destructive
world-local backup before mutation
explicit format/version marker
deterministic event IDs
idempotent repeated execution
NPC ownership preserved
bounded import
no automatic FACT creation from dialogue
no deletion of memory.json
atomic Memory 2.0 write
dry-run / diagnostics before commit
rollback path documented
```

Recommended sequence:

```text
1. Define the exact legacy dialogue schema and migration compatibility matrix.
2. Add deterministic conversion from eligible legacy dialogue to MemoryEvent DIALOGUE.
3. Add a persisted migration marker/checkpoint without changing existing semantic truth rules.
4. Prove rerun idempotency, malformed-entry fail-soft behavior and NPC isolation in tests.
5. Package and validate on a copied world before any production cutover.
6. Keep legacy reads active until long-horizon recall and rollback evidence passes.
7. Exercise rejected-new-append live only through a deterministic test path if one is added.
8. Continue to controlled BELIEF producers and relationship reasons.
9. Begin 0.3 Personality + NPC↔NPC social graph after Memory 2.0 exit criteria are met.
```

No embeddings, vector database or LLM truth classification should be prerequisites.

## Non-blocking maintenance follow-up

Reproduce the local transitive Fabric checksum-resolution difference from the `0.1.17` acceptance environment. If support is required, refresh dependency verification metadata through the controlled procedure and retain fail-closed behavior. Do not mix this maintenance task into Memory migration logic unless it blocks the normal CI/release path.

---

# Immediate target

The next implementation target is not another security acceptance release. Step 1 is closed.

Start with a design/specification for additive legacy dialogue migration:

```text
memory.json
→ deterministic bounded DIALOGUE import
→ memory2.json
```

The first implementation must not:

- delete or truncate `memory.json`;
- infer FACT from old dialogue;
- merge NPC identities;
- duplicate events on restart or rerun;
- require an LLM, embeddings or external service;
- change production provider or voice behavior.

---

# New-session handoff protocol

Preferred resume prompt:

> **Open `docs/PROJECT_STATE.md`, `docs/CHANGELOG.md`, `docs/ROADMAP.md`, `docs/livingworld/VALIDATION_0.1.16.md`, `docs/livingworld/VALIDATION_0.1.17.md`, `docs/security/README.md` and `docs/security/STEP_1_TRACKER.md` in `True-Ruslan/villAIgence`. Check recent PRs, releases and CI, then tell me what is implemented, what is live-validated versus exact-artifact validated, whether any new regression exists, and how to begin the additive legacy `memory.json` migration.**

A new session must:

1. read this file;
2. read `docs/CHANGELOG.md`;
3. read `docs/ROADMAP.md`;
4. inspect current `1.21.1` HEAD;
5. inspect recent merged/open PRs;
6. inspect latest tag/release and CI state;
7. distinguish installed server evidence from exact-artifact-only evidence;
8. continue from the first unimplemented or unvalidated priority;
9. update canonical state after material progress.

```text
docs/ROADMAP.md       → where the project is going
docs/PROJECT_STATE.md → what exists, what is proven, and what comes next
docs/CHANGELOG.md     → material implementation and validation history
```
