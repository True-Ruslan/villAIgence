# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work.
>
> Last major state update: **2026-07-31**, after preparing the residual Step 1 security acceptance harness on top of live-validated `0.1.15+1.21.1`; `0.1.14+1.21.1` remains the forgetting/decay checkpoint.
>
> Reconcile this state with newer PRs, tags/releases, CI and live-server evidence before starting development.

## Executive snapshot

VillAIgence is a Minecraft 1.21.1 MCA-derived mod evolving from AI-assisted villager dialogue into a persistent living-society simulation layer.

```text
primary branch: 1.21.1
Java: 21
primary package: Fabric
NeoForge: compile compatibility required

latest merged engineering program:
Step 1 Security and supply-chain hardening — H1 through H5
PRs: #59, #60, #61, #62, #63, documentation closure #64
latest canonical security closure: 26070c37b806897e37cc3dabe2e4b27af458ac20
final H5 validated code head: ae26a9445b646c02e53b9fe8a557204fd703c7ff

final H5 exact-head CI:
VillAIgence CI #922 / 30636167806 — SUCCESS
Java Pull Request CI #458 / 30636168112 — SUCCESS
Repository security policy #79 / 30636168870 — SUCCESS

latest gameplay/memory implementation:
PR #56 — deterministic Semantic Memory forgetting and decay
merge: 73145dd0925d403af7ef343521eb3ae27f68804d
exact verified feature head: c08b47431b6a121deae4be8410be1e4fe4c5126a

latest live-validated release checkpoint:
0.1.15+1.21.1 — PASS within executed production/security scope
validation date: 2026-07-31
tested release commit: 26070c37b806897e37cc3dabe2e4b27af458ac20
JAR: villaigence-fabric-0.1.15+1.21.1.jar
JAR SHA-256: af142be94885541bb4840d0effff73627afe3f0e245dec8307ed665701cc94fb
```

**Status boundary:** `0.1.14+1.21.1` live-proves forgetting/decay, source durability, existing-entry eviction, persistence and NPC isolation; the rejected-new-append branch remains automated-test proven only. `0.1.15+1.21.1` live-proves production Chat/STT/TTS, endpoint rejection, fail-soft TTS and six-file restart durability. SEC-001 and SEC-002 are Closed. Deterministic literal-loopback provider tooling and exact-release-JAR verification/PCM probes are prepared for SEC-003, SEC-004 and SEC-007; those findings remain open until the controlled candidate run passes.

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.15.md
docs/security/SECURITY_AUDIT_FOLLOW_UP_2026-07-31_RUNTIME_0.1.15.md
docs/livingworld/VALIDATION_0.1.14.md
docs/livingworld/VALIDATION_0.1.13.md
docs/livingworld/SEMANTIC_FORGETTING_DECAY.md
docs/security/H1_H2_CONTROLLED_SERVER_VALIDATION.md
docs/security/LOCAL_SECURITY_ACCEPTANCE_HARNESS.md
```

---

# Release metadata status

```text
0.1.11+1.21.1 → 60236524e37b60c639b93405f809ade883be253f
0.1.12+1.21.1 → 746fa75ab4b5f4bee385efa0c8ae51009c1aec58
0.1.13+1.21.1 → b553bf7e83674145bdf42927b9ace7287afa560c
0.1.14+1.21.1 → c45aea45dd915b24ba236344feef30559c7171bb
0.1.15+1.21.1 → 26070c37b806897e37cc3dabe2e4b27af458ac20
```

`0.1.14` remains the exact forgetting/decay payload tested under retention pressure. `0.1.15` identifies the later H1–H5 security payload and is the latest production/security live checkpoint.

---

# Step 1 security hardening

## Repository implementation — complete

Merged sequence:

```text
H1 provider endpoint and credential policy     PR #59 → 787f1a781b5970d4bafb851bfb3c7cba7c21fc0a
H2 bounded network and voice resources         PR #60 → 15c56526417ac7dfb76567d51d1aa107f522cda7
H3 immutable verified build inputs             PR #61 → 4cf9aef2e5c31a5682a7cad8544219154330e056
H4 primary CI and repository security policy   PR #62 → 05d105c1f558d5643b8190a88cc744b4d7cbe129
H5 legacy utility and whole-tree closure       PR #63 → 6d82b4e4650294a4a42b9ea2113e64d990e08811
canonical closure                              PR #64 → 26070c37b806897e37cc3dabe2e4b27af458ac20
```

Implemented controls:

- normalized endpoint validation and endpoint-family credential binding;
- authenticated provider redirects blocked;
- byte-bounded Chat, STT, TTS, provider-error and verification responses;
- hard total response-body deadline;
- fixed trusted-origin account verification;
- voice capture clamped to `1..120` seconds;
- aggregate active PCM memory bounded to 128 MiB;
- stable Fabric Loom and verified Gradle wrapper;
- dependency verification metadata and lockfiles;
- immutable GitHub Actions references;
- required common, Fabric and NeoForge CI;
- deterministic secret, dangerous-source, workflow and script policy;
- exact-head tracked-tree manifests;
- removal of inherited non-CI network and generation utilities.

Finding status:

```text
Closed:
SEC-001
SEC-002
SEC-005
SEC-006
SEC-008
SEC-009

Pending isolated acceptance:
SEC-003
SEC-004
SEC-007
```

Canonical security evidence starts at:

```text
docs/security/README.md
```

Required runtime scenario:

```text
docs/security/H1_H2_CONTROLLED_SERVER_VALIDATION.md
```

`0.1.15` contains H1–H5 and closes SEC-001/SEC-002 with live evidence. SEC-003/SEC-004/SEC-007 remain pending isolated acceptance.

## Residual acceptance tooling — prepared

PR #68 adds the controlled execution surface required for the remaining findings:

```text
scripts/security/provider_acceptance_harness.py
→ literal loopback bind only
→ normal / declared / chunked / error / redirect / slow-drip routes
→ streamed hostile payloads without whole-body allocation
→ sanitized manifest and JSONL evidence

AccountVerificationAcceptanceProbe
→ explicit java -cp invocation only
→ literal loopback target validation
→ shared JDK-only bounded/no-redirect production transport

VoicePcmBudgetAcceptanceProbe
→ explicit java -cp invocation only
→ exact 1..120 second clamp
→ exact 128 MiB budget contention, rejection, release and recovery
```

The distributable Fabric package is required to contain both probes and the shared verification transport. Security CI runs the Python harness contract tests and deterministic seven-script inventory. No probe or harness has an in-game command, startup hook, production-key lookup or persistent schema effect.

Tooling does not close SEC-003, SEC-004 or SEC-007. A release containing PR #68 must still complete `docs/security/LOCAL_SECURITY_ACCEPTANCE_HARNESS.md` on the controlled server.

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

# Live validation status

## 0.1.15+1.21.1 — PASS within executed scope

```text
commit: 26070c37b806897e37cc3dabe2e4b27af458ac20
JAR: villaigence-fabric-0.1.15+1.21.1.jar
SHA-256: af142be94885541bb4840d0effff73627afe3f0e245dec8307ed665701cc94fb
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

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.15.md
docs/security/SECURITY_AUDIT_FOLLOW_UP_2026-07-31_RUNTIME_0.1.15.md
```

## 0.1.14+1.21.1 — PASS

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

Byte-identical after the final restart:

```text
memory.json
memory2.json
semantic-memory.json
relationships.json
voices.json
```

Operations:

```text
Chat                                                       SUCCESS
STT                                                        SUCCESS
TTS                                                        SUCCESS
Voice Chat / Opus                                          PASS
UDP 24454 / 25565                                          PASS
LinuxGSM monitor                                           PASS
server STARTED                                             PASS
VillAIgence / persistence / OutOfMemory errors             none
```

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.14.md
```

Explicit remaining boundary:

```text
rejection of a newly appended weak candidate without file rewrite
→ automated-test proven
→ not reached in the live gameplay pipeline because the Chat model did not emit RELATIONSHIP_CHANGE
```

This does not block promotion of `0.1.14` as the live forgetting/decay checkpoint. It prevents claiming that the rejected-new-append branch itself was exercised on the server.

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

## 0.1.x Reliability

Gameplay and Memory 2.0 retention behavior is live-validated by `0.1.14+1.21.1`. Production Chat/STT/TTS and endpoint-policy behavior is live-validated by `0.1.15+1.21.1`.

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

- rejected-new-append no-rewrite behavior on a real server;
- automatic controlled BELIEF producers;
- legacy `memory.json` migration;
- NPC-to-NPC knowledge and rumor propagation;
- trustworthy causal relationship reasons;
- long-horizon recall after days without full raw dialogue;
- large multiplayer and multi-day soak validation;
- live test of manually seeded pre-existing semantic duplicates.

## Next sequence

```text
1. Merge PR #68 and build the first release containing the acceptance harness, expected 0.1.16+1.21.1
2. Run docs/security/LOCAL_SECURITY_ACCEPTANCE_HARNESS.md for SEC-003
3. Run the exact-JAR verification probe and redirect checks for SEC-004
4. Run the exact-JAR voice clamp/PCM probe plus final microphone smoke for SEC-007
5. Close the remaining Step 1 findings only if persistence, redaction and recovery evidence passes
6. Exercise rejected-new-append live only if a deterministic test path becomes available
7. Design legacy memory.json migration
8. Run long-horizon Memory 2.0 exit-criterion validation
9. Begin 0.3 Personality + NPC↔NPC social graph
```

No embeddings, vector DB or LLM truth classification should be prerequisites.

---

# Immediate validation target

Preserve `0.1.15+1.21.1` as the latest production/security live checkpoint.

Build the first candidate containing PR #68, expected `0.1.16+1.21.1`, then execute:

```text
docs/security/LOCAL_SECURITY_ACCEPTANCE_HARNESS.md
```

Required remaining evidence:

- declared, chunked, error and slow-drip Chat/STT/TTS behavior from literal loopback;
- zero redirect-target hits for provider and verification redirects;
- 64 KiB verification response bound through the exact-JAR transport probe;
- exact 1/120 second clamp and 128 MiB PCM rejection/recovery output;
- normal microphone operation after the PCM probe;
- byte-identical rejected-operation persistence and production configuration restoration;
- no secrets or unredacted provider bodies in evidence.

After SEC-003/SEC-004/SEC-007 closure, continue with legacy `memory.json` migration unless live evidence exposes a concrete defect.

---

# New-session handoff protocol

Preferred resume prompt:

> **Open `docs/PROJECT_STATE.md`, `docs/CHANGELOG.md`, `docs/ROADMAP.md`, `docs/livingworld/VALIDATION_0.1.15.md`, `docs/livingworld/VALIDATION_0.1.14.md`, `docs/security/README.md` and `docs/security/LOCAL_SECURITY_ACCEPTANCE_HARNESS.md` in `True-Ruslan/villAIgence`. Check recent PRs, tags/releases and CI, then tell me what is implemented, what is live-validated, what changed since the state file, and what should happen next.**

A new session must:

1. read this file;
2. read `docs/CHANGELOG.md`;
3. read `docs/ROADMAP.md`;
4. inspect current `1.21.1` HEAD;
5. inspect recent merged/open PRs;
6. inspect latest tag/release and CI state;
7. reconcile newer live-test evidence;
8. continue from the first unimplemented or unvalidated priority;
9. update canonical state after material progress.

```text
docs/ROADMAP.md       → where the project is going
docs/PROJECT_STATE.md → what exists, what is proven, and what comes next
docs/CHANGELOG.md     → material implementation and validation history
```