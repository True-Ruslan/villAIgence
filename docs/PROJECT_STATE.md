# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work.
>
> Last major state update: **2026-08-01**, after selective synchronization packages S1–S10b were merged and the server-authoritative operator-lore API passed all automated gates.
>
> Always reconcile this document with newer PRs, tags/releases, CI and live-server evidence before starting development.

## Repository visibility update — 2026-08-01

The public README now exposes the canonical author and engineering-portfolio origin `https://trueruslan.ru/`.

```text
feature PR:                  #90
accepted head:               19e397ea3250384d7063e5df46ea3f05f66c9514
VillAIgence CI:              30714996167 / #1154 — SUCCESS
Java Pull Request CI:        30714996171 / #654 — SUCCESS
Repository security policy:  30714996231 / #505 — SUCCESS
squash merge:                62091309f61667ce38a6c60aa9477309093392c5
changed file:                README.md only
```

This is repository-discovery maintenance only. It changes no gameplay, provider, memory, persistence, packet, security, compatibility, release or live-validation claim. **S10c operator-lore client editor UI remains the next development step.**


## Executive snapshot

VillAIgence is a Minecraft 1.21.1 MCA-derived mod evolving from AI-assisted villager dialogue into a persistent living-society simulation layer.

```text
repository: True-Ruslan/villAIgence
primary branch: 1.21.1
current canonical HEAD: 3cc3137b09629ab348cf0d3e79c821a524259d56
Java: 21
primary package: Fabric
NeoForge: compile compatibility required
```

Current engineering state:

```text
Step 1 Security and supply-chain hardening     complete
Memory 2.0 foundation                         implemented and substantially advanced
MCA 7.7.32 selective core synchronization     S1–S8 complete, automated PASS
operator-authored lore foundation             S9 complete, automated PASS
immutable AI-context integration               S10a complete, automated PASS
server-authoritative editor API                S10b complete, automated PASS
client editor UI                               S10c next
cumulative installed-server acceptance         pending
new synchronized release promotion             not yet claimed
```

Latest installed/live server checkpoint:

```text
release: 0.1.16+1.21.1
commit: 521568f903078b91dd5817cdc9a551bd2392e663
JAR SHA-256: 036cbacc657ceb676813f41ee293024690b981e971e7c6037fc5d3ecbe3ee062
status: hostile-provider, PCM, production restoration and restart acceptance PASS
```

Latest exact-artifact security checkpoint:

```text
release: 0.1.17+1.21.1
commit: 88a20d86e8b08e4b5eaf60da943a63e750f2b545
JAR SHA-256: b33af40f7a2696dc679c49e0fc544f6b5df99e0aa600ea5c767bc5a9747da1ab
marker: V0117_SEC004_ARTIFACT_AND_EVIDENCE_PASS
status: focused SEC-004 exact-release-JAR acceptance PASS
```

**Validation boundary:** S1–S10b are implemented and automated-validated, but have not yet been promoted as an installed-server or official-release checkpoint. Do not describe them as live-proven until the cumulative acceptance procedure passes on an exact candidate JAR.

---

# Architecture laws

1. **LLM is never authoritative.** Minecraft/server-owned state is truth.
2. Mutable state used by asynchronous AI is captured into immutable bounded context first.
3. The LLM may propose dialogue, actions and relationship deltas; server policy validates, revalidates and executes mutations.
4. Provider/model changes must not redefine persistent NPC identity.
5. External AI and auxiliary persistence failures fail soft whenever safe.
6. Credentials remain server-side and endpoint-bound.
7. Persistent formats remain explicit, inspectable, world-local and backup-safe.
8. Retry/replay paths must not duplicate persistent or gameplay effects.
9. FACT requires server-owned evidence; dialogue does not automatically become FACT.
10. Confidence never upgrades BELIEF into FACT.
11. Consolidation preserves provenance and independent source-event IDs.
12. Forgetting is deterministic storage policy, never an LLM decision.
13. Time alone does not remove semantic knowledge while the NPC remains under capacity.
14. Retention pressure remains isolated per NPC owner.
15. Operator-authored lore is background context, not an observed current-world fact.
16. Current observed world facts override conflicting operator lore and recalled memory.
17. Client tools never receive file-system or authority ownership; all writes are server-authoritative.
18. Migration remains additive, deterministic, idempotent and reversible until explicit cutover evidence exists.

Canonical AI flow:

```text
Minecraft/server state
→ immutable bounded snapshot
→ observed facts + operator lore + episodic/semantic memory
→ provider/LLM proposal
→ server validation/revalidation
→ server-owned mutation
→ persistent authoritative evidence
```

Operator editor flow:

```text
client selects scope/nearby NPC
→ bounded C2S request
→ server permission check
→ server resolves trusted identity/scope
→ SHA-256 revision check
→ atomic operator-lore store mutation
→ canonical S2C value/status/revision
```

---

# Identity and compatibility

```text
public name: VillAIgence
short name: VAI
tagline: Giving villagers a mind of their own.
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

# Persistent world-local data

```text
<world>/livingworld/memory.json          bounded rolling legacy dialogue history
<world>/livingworld/memory2.json         episodic MemoryEvent store
<world>/livingworld/semantic-memory.json typed Semantic FACT/BELIEF store
<world>/livingworld/events.json          server-owned world events
<world>/livingworld/relationships.json   player↔NPC relationship state
<world>/livingworld/voices.json          persistent NPC voice identity
<world>/livingworld/operator-lore.json   explicit server/operator-authored lore
```

All files belong with world backup/restore procedures.

`memory.json` remains active. Memory 2.0 is additive; legacy migration has not started. Do not delete or rewrite `memory.json` during unrelated synchronization, editor or release work.

## Operator lore schema

`operator-lore.json` schema version 1 supports:

```text
WORLD
VILLAGER by persistent NPC UUID
PLAYER by authenticated player UUID
VILLAGE by dimension + stable MCA village ID
```

Guarantees:

- maximum 4096 Unicode code points per stored value;
- normalized line endings and forbidden-control rejection;
- deterministic inspectable JSON;
- exact replay does not rewrite the file;
- temporary file plus atomic replace where supported;
- malformed-file fail-open recovery with `.corrupt` backup;
- explicit provenance labels in prompt context;
- no automatic semantic-memory ingestion.

---

# Reliability and security baseline

Step 1 Security and supply-chain hardening is complete. `SEC-001` through `SEC-009` are Closed.

Implemented and retained:

- normalized provider endpoint validation and credential-family binding;
- remote plaintext rejection except explicit literal-loopback development mode;
- authenticated redirects blocked;
- bounded Chat/STT/TTS/error/verification bodies;
- ten-minute total response-body deadline;
- voice capture clamped to `1..120` seconds;
- aggregate active PCM bounded to 128 MiB;
- safe `content:null`, empty-response and malformed-response handling;
- retry without duplicate memory/actions/relationship effects;
- stable Fabric Loom, verified Gradle wrapper and dependency verification;
- pinned GitHub Actions;
- required common, Fabric and NeoForge CI;
- deterministic repository security policy;
- diagnostics without secrets, prompts, transcripts or hidden reasoning.

Canonical security evidence:

```text
docs/security/README.md
docs/security/STEP_1_TRACKER.md
docs/livingworld/VALIDATION_0.1.16.md
docs/livingworld/VALIDATION_0.1.17.md
```

---

# Memory 2.0 implemented state

## Episodic memory

Implemented:

- immutable NPC-owned `MemoryEvent`;
- `DIALOGUE`, `OBSERVATION`, `ACTION`, `RELATIONSHIP_CHANGE`;
- explicit provenance;
- bounded per-NPC persistence;
- deterministic UUID idempotency and ordering;
- atomic writes and fail-open recovery;
- deterministic retrieval;
- text and voice DIALOGUE parity;
- authoritative ACTION and relationship-transition ingestion.

## Working memory

Turn-local prompt context combines:

```text
recent dialogue
+ selected episodic context
+ selected semantic context
```

Hard bounds:

```text
recent dialogue messages: 12
max dialogue message: 1200 Unicode code points
episodic entries: 6
semantic entries: 6
```

## Semantic memory

Implemented:

- typed FACT/BELIEF entries;
- provenance and confidence boundaries;
- deterministic retrieval;
- controlled FACT ingestion from server-observed events;
- deterministic consolidation and source union;
- pressure-based forgetting/decay;
- source-evidence durability;
- existing weak-entry eviction;
- per-NPC isolation;
- restart-safe persistence.

Truth boundary:

```text
FACT   → SYSTEM_OBSERVED only
BELIEF → PLAYER_TOLD / NPC_TOLD / INFERRED only
DIALOGUE → episodic only by default
```

Prompt layers now remain explicitly separate:

```text
worldFacts                 authoritative current state
operatorAuthoredContext    operator background lore
memoryContext              episodic memory
semanticMemoryContext      semantic FACT/BELIEF memory
```

Still pending for Memory 2.0 completion:

- additive legacy `memory.json` migration;
- controlled automatic BELIEF producers;
- trustworthy causal relationship reasons;
- long-horizon and multi-day soak validation;
- NPC-to-NPC knowledge and rumor propagation.

---

# Selective MCA synchronization state

Whole-upstream merge and blind sequential cherry-pick remain prohibited. Changes were adopted as isolated final-state packages while protecting VillAIgence AI/security/persistence boundaries.

## Core correctness and navigation

```text
S1  PR #72  tombstone item/entity-data integrity
S2  PR #73  UUID-preserving villager↔zombie conversion
S3  PR #74  occupied HOME-bed rejection and ticket correctness
S4  PR #75  water-tag and entity-AABB collision navigation
S5  PR #76  stable climbable/ladder navigation
S6  PR #77  staggered pathfinding plus retained progress watchdog
S1–S6 checkpoint  PR #78
```

## Server behavior and compatibility

```text
S7   PR #79  graveyard mourning, reservations, retry and cleanup
S8a  PR #80  InteractionResult relationship-gift contract
S8b  PR #81  fishing loot/rod/AquaCulture compatibility
S8c  PR #82  stable archer movement control while mounted
```

## Operator-authored context train

```text
S9   PR #83  world-local operator-lore store
     merge: f4a0d369c6e787d9ed91501fc87323aded9b4cbc

S10a PR #84  immutable context capture and prompt provenance
     merge: b700e14636e3370774173978b0b2519941dafed0

S10b PR #85  server-authoritative editor API
     merge: 3cc3137b09629ab348cf0d3e79c821a524259d56
```

S10b authority guarantees:

- permission level 2 required for READ and WRITE;
- PLAYER always resolves to the authenticated sender;
- VILLAGER/VILLAGE resolve from a live nearby same-level MCA villager;
- arbitrary UUID, dimension and village ID are absent from the C2S format;
- stale revisions return `CONFLICT` with current canonical state;
- exact replay returns `UNCHANGED`;
- payloads are bounded by code points and UTF-8 bytes;
- transport-oversized existing lore cannot cause packet encoding failure;
- no client screen is included yet.

Canonical package evidence:

```text
docs/livingworld/VALIDATION_UPSTREAM_S1_TOMBSTONE.md
...
docs/livingworld/VALIDATION_UPSTREAM_S8C_ARCHER_MOUNTED.md
docs/livingworld/VALIDATION_UPSTREAM_S9_OPERATOR_LORE.md
docs/livingworld/VALIDATION_UPSTREAM_S10A_OPERATOR_LORE_CONTEXT.md
docs/livingworld/VALIDATION_UPSTREAM_S10B_OPERATOR_LORE_AUTHORITY.md
```

---

# Validation status

Automated validation for every S1–S10b package includes:

- focused unit tests;
- Fabric build;
- NeoForge build compatibility;
- Fabric distributable-package verification;
- repository security policy;
- exact RED/GREEN evidence in package validation documents.

Latest exact automated head before merge:

```text
S10b head: de2ab0ad38e6b88999cfb397a8dd42ba88c6b1bb
VillAIgence CI #1116 / 30705048055              SUCCESS
Java Pull Request CI #623 / 30705048075       SUCCESS
Repository security policy #438 / 30705048038 SUCCESS
```

Not yet live-proven:

- S1–S8 gameplay/runtime scenarios;
- S9 persistence through an installed candidate lifecycle;
- S10a prompt behavior against a real provider;
- S10b permission, packet and concurrency behavior on a multiplayer server;
- cumulative restart durability after the complete synchronization train.

No official release after `0.1.17+1.21.1` has been promoted for this train.

---

# Next optimal development step

Implement **S10c — operator lore client editor UI** as a separate client-focused package.

Required behavior:

```text
entry point available only to permitted operators
scope selector: WORLD / VILLAGER / PLAYER / VILLAGE
nearby target selection for VILLAGER/VILLAGE
read current canonical state before editing
multiline bounded text editor
code-point and UTF-8 budget indicators
save with server-provided SHA-256 revision
clear through the same revision-protected write path
visible OK / UNCHANGED / CONFLICT / INVALID / FORBIDDEN / NOT_FOUND / ERROR states
conflict requires reload/review; never blind overwrite
```

S10c must not:

- read or write `operator-lore.json` directly;
- trust client-entered UUID, dimension or village ID;
- duplicate server permission or target authority;
- silently overwrite on conflict;
- ingest lore into semantic memory;
- change provider transport/parser/retry behavior;
- add unbounded payloads;
- mix unrelated gameplay synchronization.

After S10c:

```text
build one exact release candidate
→ execute cumulative backed-up-world acceptance for S1–S10c
→ verify persistent-file hashes and restart
→ verify Text/STT/Chat/TTS regression surface
→ record exact JAR SHA-256 and live evidence
→ promote a release only if the full gate passes
```

Only after the synchronized candidate is accepted should development return to additive legacy `memory.json` migration and the remaining Memory 2.0 exit criteria.

---

# New-session handoff protocol

Preferred resume prompt:

> **Open `docs/PROJECT_STATE.md`, `docs/CHANGELOG.md`, `docs/ROADMAP.md` and the S9/S10a/S10b validation documents in `True-Ruslan/villAIgence`. Check current `1.21.1` HEAD, recent PRs, releases and CI. Tell me what is automated-validated versus live-validated, then continue with S10c operator-lore client editor UI without weakening the S10b server-authority contract.**

A new session must:

1. read this file;
2. read `docs/CHANGELOG.md` and `docs/ROADMAP.md`;
3. inspect current `1.21.1` HEAD;
4. inspect recent merged/open PRs and CI;
5. inspect latest tag/release;
6. distinguish automated package validation from installed-server evidence;
7. continue from S10c unless newer evidence changes priority;
8. update canonical documents after material progress.

```text
docs/ROADMAP.md       → long-term direction and milestone sequence
docs/PROJECT_STATE.md → what exists, what is proven, and what comes next
docs/CHANGELOG.md     → material implementation and validation history
```
