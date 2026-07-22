# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work in a new session.
>
> Last major state update: **2026-07-22**.
>
> This file describes implemented repository state, not aspirational roadmap items. Before active development, verify recent PRs, tags/releases and CI because repository activity may have advanced since the last edit.

## Project identity

- **Public name:** VillAIgence
- **Wordplay:** Vill-AI-gence
- **Short name:** VAI
- **Tagline:** `Giving villagers a mind of their own.`
- **Repository:** `True-Ruslan/villAIgence`
- **Primary development/base branch:** `1.21.1`
- **Target game:** Minecraft 1.21.1
- **Primary loader:** Fabric
- **Compatibility build:** NeoForge remains compiling in CI where applicable
- **Java:** 21
- **Technical mod id:** `mca` — intentionally preserved for MCA compatibility
- **Internal Java namespace:** `net.conczin.mca` — intentionally preserved
- **Internal engine/data namespace:** `LivingWorld` / `livingworld` — intentionally preserved for compatibility

### Compatibility paths that must not be casually renamed

```text
config/livingworld.json
<world>/livingworld/
mod id: mca
Java package root: net.conczin.mca
```

Public branding is VillAIgence; compatibility-sensitive MCA/LivingWorld identifiers remain unchanged unless a dedicated migration design exists.

---

# Product direction

VillAIgence is not treated as merely “MCA Reborn with an AI chat feature.”

The intended product is a **persistent living-society simulation layer for Minecraft** built on the MCA-derived NPC/social body.

```text
NPC identity
+ personality
+ memory
+ relationships
+ knowledge
+ voice
+ goals
+ autonomous actions
+ settlements
+ factions
+ emergent history
= VillAIgence
```

The full versioned vision and exit criteria are in `docs/ROADMAP.md`.

---

# Current implementation status

## 1. Server-side AI foundation — implemented

VillAIgence has a server-owned AI provider layer with OpenAI-compatible/OpenRouter routing.

Key guarantees:

- API keys remain server-side;
- provider settings are server configuration;
- provider failure degrades through controlled paths rather than blindly mutating the world;
- LLM output is not authoritative Minecraft state.

---

## 2. Voice input pipeline — implemented

```text
Player microphone
→ Simple Voice Chat packets
→ decoded PCM
→ STT
→ selected/targeted NPC
→ AI response
→ text conversation
```

Implemented behavior includes:

- optional microphone-driven interaction;
- Simple Voice Chat integration;
- STT through OpenAI-compatible/OpenRouter paths;
- separate `voiceInputEnabled` and `voiceOutputEnabled` switches;
- new installs default to `voiceOutputEnabled=false`;
- normal player-to-player Simple Voice Chat traffic remains separate.

Raw microphone audio is processed in memory and is not intentionally persisted.

---

## 3. Text-to-speech and spatial NPC voice — implemented

VillAIgence supports optional NPC TTS output with spatial playback through Simple Voice Chat.

PR #24 added OpenRouter raw PCM and resilient structured response handling.

Implemented:

- `ttsResponseFormat`: `auto`, `wav`, `pcm`;
- OpenRouter `auto` → raw PCM, other OpenAI-compatible endpoints → WAV;
- signed PCM16 little-endian mono decoding;
- MIME `rate`/`channels` parsing;
- configured fallback sample rate;
- 48 kHz resampling before Simple Voice Chat playback;
- WAV path retained;
- text reply is published before/independently from TTS failure or local TTS backpressure.

A TTS problem must not remove a valid text response.

---

## 4. Persistent per-NPC voice identity — implemented

PR #23 added persistent gender/age-aware NPC voices.

Storage:

```text
<world>/livingworld/voices.json
```

Model:

```text
NPC UUID
+ MCA gender
+ MCA age bucket
→ deterministic compatible voice selection
→ persisted voice profile
```

Age mapping:

```text
BABY / TODDLER / CHILD → child
TEEN                   → teen
ADULT / UNASSIGNED     → adult
```

MCA 1.21.1 has no distinct elder state, so VillAIgence does not fabricate one.

Properties:

- stable across restarts;
- independent from the selected chat/LLM model;
- configurable male/female/neutral child/teen/adult pools;
- re-resolution on real age/gender transitions or incompatible pool changes.

### Mood-aware delivery — implemented foundation

Current moods:

```text
NEUTRAL
HAPPY
SAD
ANGRY
AFRAID
TIRED
```

Mood affects delivery, not persistent voice identity. It is derived from server-owned state such as panic, health and persistent relationship dimensions.

---

## 5. Persistent player↔NPC dialogue memory — implemented foundation

Storage:

```text
<world>/livingworld/memory.json
```

Current memory is bounded persistent dialogue/context memory. It is a foundation, **not** the planned `Memory 2.0` architecture.

Roadmap `0.2` will separate:

- working memory;
- episodic memory;
- semantic knowledge;
- relationship reasons/history;
- importance/decay/provenance/confidence.

---

## 6. World events / factual knowledge foundation — implemented

Storage:

```text
<world>/livingworld/events.json
```

Implemented:

- server-owned factual events;
- provenance/time/location/dimension context;
- bounded event queries;
- factual context injection into authoritative NPC context snapshots;
- LLM cannot inject arbitrary facts into authoritative event state.

This is the foundation for future knowledge propagation and rumors, not a complete social knowledge network.

---

## 7. Player↔NPC relationship state — implemented foundation

Storage:

```text
<world>/livingworld/relationships.json
```

Dimensions:

```text
trust
respect
fear
affinity
```

Properties:

- bounded values;
- structured relationship deltas;
- server-side clamping/policy;
- relationship state influences behavior/context;
- malformed deltas cannot break or leak structured JSON into visible dialogue.

**Not yet implemented:** general persistent `NPC ↔ NPC` social graph. This is roadmap `0.3`.

---

## 8. Safe actions — implemented foundation

LLM-triggered actions use a constrained/whitelisted path:

```text
LLM proposes
→ policy validates
→ server revalidates entity/player/world state
→ server executes allowed mutation
```

Mutable Minecraft state is accessed on the server thread; immutable snapshots are passed into asynchronous AI work.

---

## 9. Immutable authoritative context snapshot — implemented

Before asynchronous AI processing, VillAIgence captures authoritative server-side context for:

- player/NPC identity;
- world facts;
- available actions;
- relationship context;
- language/context metadata;
- relevant voice state where applicable.

This is the boundary preventing asynchronous provider code from freely reading mutable Minecraft entities.

---

## 10. Structured response sanitization — implemented and hardened

VillAIgence isolates the visible NPC message from optional command/relationship metadata.

Important behavior:

- malformed `relationshipDelta` cannot expose raw JSON;
- malformed optional metadata does not invalidate a recoverable message;
- Markdown/code fences are stripped safely;
- malformed JSON may recover only a safe top-level `message`;
- unrecoverable structured JSON is not displayed/spoken as raw metadata;
- language prompting requests the natural language of the player’s latest message.

This closes the historical JSON-tail bug caused by malformed structured relationship fields.

---

## 11. OpenRouter `content: null` hardening — merged

PR #26 fixed the VillAIgence 0.1.5 crash:

```text
JsonNull cannot be cast to JsonPrimitive
```

Implemented:

- dedicated `ChatCompletionResponseParser`;
- null/missing/blank `content` handled safely;
- `finish_reason`, provider errors/error type and generation ID captured safely;
- reasoning is represented only as a presence boolean and is never substituted as NPC text;
- initial request + at most one retry for retryable empty completion;
- no blind retry for terminal `length`, `content_filter` or explicit provider errors;
- controlled `empty_response` fallback;
- no API keys, Authorization headers, prompts or reasoning text in diagnostics.

Retry remains inside the provider layer before memory/actions/relationships/TTS, preventing duplicate game-side effects.

PR #26 merge commit:

```text
52eed8ba8dede8deeaceffbec723255d4515ac8d
```

---

## 12. Branding — implemented

PR #25 established **VillAIgence** as the public product identity.

Updated surfaces include README, loader metadata, repository links, CI/release naming and public documentation.

Release artifact convention:

```text
villaigence-fabric-<version>.jar
villaigence-fabric-<version>.jar.sha256
```

Compatibility identifiers (`mca`, Java namespace, `livingworld` config/data paths) remain unchanged.

---

## 13. Operator AI diagnostics — implemented

PR #28 added:

```text
/villaigence ai status
```

PR #28 merge commit:

```text
90b32ee1125ad451d2fe9f7242ee903e8680a131
```

Implemented behavior:

- separate public `/villaigence` command root without renaming compatibility-sensitive `/mca` internals;
- permission level 2+ or integrated single-player server owner;
- read-only status with no provider probe/token spend;
- Chat/STT/TTS configuration readiness: `CONFIGURED`, `MISCONFIGURED`, `DISABLED`;
- process-local runtime state: `NEVER`, `SUCCESS`, `FAILURE`;
- safe Chat metadata: provider/model/duration, `finish_reason`, error type, generation ID, bounded attempts, reasoning-presence boolean;
- safe STT/TTS metadata and controlled error types such as `http_402`, `http_429`, `timeout`, `io_error`, `runtime_error`;
- diagnostics reset on process restart and are not persisted.

Security boundary:

- credentials are never stored/displayed as values;
- endpoints are reduced to hosts;
- prompts, transcripts, NPC answers, TTS input, reasoning content, Authorization headers and raw provider payloads are excluded.

Detailed operator documentation: `docs/livingworld/DIAGNOSTICS.md`.

---

## 14. Non-blocking AI admission / backpressure — implemented

Implemented by PR #29 (`feat: add AI admission backpressure and cooldown controls`).

Purpose: prevent provider storms and uncontrolled concurrent Chat/STT/TTS traffic without blocking the Minecraft server thread or creating an unbounded AI queue.

Default limits:

```text
aiChatMaxConcurrentRequests = 4
aiSttMaxConcurrentRequests = 2
aiTtsMaxConcurrentRequests = 2
aiPerPlayerCooldownMillis = 750
aiProviderRateLimitCooldownMillis = 5000
```

Admission order:

```text
provider cooldown
→ stage concurrency capacity
→ per-player/per-stage cooldown
→ ALLOW with idempotent permit
   or
→ REJECT immediately
```

Implemented guarantees:

- independent bounded concurrency for Chat, STT and TTS;
- per-player cooldown is stage-local, so normal `STT → Chat → TTS` flow does not self-block across stages;
- detected provider `429`/rate-limit signals activate temporary stage cooldown;
- rejected requests do not call the external provider;
- no blocking semaphore/waiting path on the Minecraft server thread;
- permits close via owned try-with-resources paths and cannot decrement active counts below zero;
- STT rejection releases player/villager busy state;
- TTS admission happens after text publication, so local TTS backpressure cannot remove a valid text answer;
- local rejection diagnostics use controlled types:

```text
admission_saturated
admission_player_cooldown
admission_provider_cooldown
```

`/villaigence ai status` also reports process-local admission metrics per stage:

```text
active/max
rejected
providerCooldownMs
```

No player UUIDs, prompts, transcripts, answers, keys or provider payload bodies are exposed in admission diagnostics.

Existing `version=2` configs require no migration; missing admission fields receive safe defaults and values are normalized to bounded ranges.

Detailed configuration/diagnostics:

- `docs/livingworld/CONFIGURATION.md`
- `docs/livingworld/DIAGNOSTICS.md`

---

# Release state

## Known release sequence relevant to current development

Historical published tags include releases through `0.1.5+1.21.1`.

`0.1.5+1.21.1` predates PR #26 and contains the reported `content: null` crash.

The next intended patch/reliability release is:

```text
0.1.6+1.21.1
```

Before publishing it, verify:

1. `1.21.1` HEAD is the exact intended release commit including PR #26, #28 and #29 reliability work;
2. release workflow/tag validation still matches;
3. release/dry-run CI is green;
4. `0.1.6+1.21.1` is still unused;
5. the tag points exactly to the intended `1.21.1` HEAD.

Never move an already-published version tag.

---

# CI / quality gates

Important checks:

```text
VillAIgence CI
→ :common:test
→ Fabric build
→ distributable Fabric JAR smoke-check

Official Gradle PR CI
→ NeoForge build
→ Fabric build
```

Before claiming a feature/bugfix complete:

- verify the exact final PR head;
- require fresh green CI on that head;
- review the final diff;
- merge only after no critical/important unresolved issue remains.

Bugfixes should use TDD where practical: reproduce with RED, implement, verify GREEN.

---

# What is NOT implemented yet

The following roadmap features remain planned, not complete.

## Memory 2.0

- layered working/episodic/semantic/relationship memory;
- importance and forgetting;
- confidence/provenance-aware consolidation;
- durable summaries beyond raw chat history.

## Full persistent personality system

- stable values/goals/fears/likes/dislikes;
- personality-driven autonomous decisions;
- controlled long-term personality evolution.

## NPC↔NPC social graph

- friendship/trust/respect/fear between arbitrary NPC pairs;
- rivalry/grudges/social history;
- family/romance integration as a unified social graph.

## Rumor/knowledge propagation

- NPCs telling other NPCs facts;
- source chains;
- trust-dependent belief;
- distortion over repeated transmission.

## Autonomous agent loop

- perceive → evaluate → choose intent → validate → act → observe → remember;
- meaningful autonomous behavior while the player is not actively conversing.

## Settlement simulation

- settlement resources/economy/security/leadership/social conditions as persistent causal systems.

## Factions/politics

- diplomacy, alliances, wars, leadership, reputation and cross-settlement consequences.

## Emergent story system

- persistent causal story threads arising from interacting systems rather than fixed scripted quests.

## Large-scale/local AI architecture

- Ollama/LM Studio/vLLM/local model support as first-class targets;
- event-driven/budgeted autonomous AI at large NPC counts;
- simulation LOD/performance architecture.

---

# Immediate next development priorities

## Priority A — finish the `0.1.x` reliability/release phase

Recommended next tasks:

1. prepare, verify, publish and playtest `0.1.6+1.21.1` from the exact intended `1.21.1` HEAD;
2. run multiplayer concurrency and repeated voice-dialogue soak tests under the new admission limits;
3. validate restart/reconnect/world-backup behavior for all persistent JSON stores;
4. collect real server feedback on STT/LLM/TTS/admission failure modes using `/villaigence ai status` plus bounded logs;
5. decide whether the default/recommended free LLM should change from unstable free-provider choices.

Rate-limit/backpressure/cooldown implementation is no longer a pending roadmap item; it is part of the implemented reliability foundation.

## Priority B — begin `0.2 Memory 2.0`

Once `0.1.x` is stable enough for normal playtesting, the next major feature branch should design and implement Memory 2.0.

Suggested first architecture slice:

```text
MemoryEvent
├── id
├── type
├── summary
├── participants
├── source/provenance
├── game time / real time metadata
├── importance
├── emotional weight
├── confidence
└── related relationship reasons
```

Then add bounded retrieval/consolidation before autonomous social propagation.

## Priority C — `0.3 Personality + NPC↔NPC social graph`

This should follow Memory 2.0 closely because later rumors, autonomous behavior and settlements need durable identities and social relationships.

---

# Recommended milestone order

```text
0.1.x  Reliability / provider hardening
        ↓
0.2    Memory 2.0
        ↓
0.3    Personality + NPC↔NPC social graph
        ↓
0.4    Knowledge + rumors
        ↓
0.5    Autonomous NPC agents
        ↓
0.6    Settlement simulation
        ↓
0.7    Factions + politics
        ↓
0.8    Emergent stories
        ↓
0.9    Performance + local LLM + large servers
        ↓
1.0    Persistent living society
```

See `docs/ROADMAP.md` for rationale, details and milestone exit criteria.

---

# Session handoff protocol

Preferred prompt for a new ChatGPT/Codex session:

> **Open `docs/PROJECT_STATE.md` and `docs/ROADMAP.md` in `True-Ruslan/villAIgence`. Check recent PRs/releases/CI, then tell me how VillAIgence development is going, what is complete, what changed since the state file, and what we should build next.**

The new session should:

1. read this file;
2. read `docs/ROADMAP.md`;
3. inspect current `1.21.1` HEAD;
4. inspect recent merged/open PRs;
5. inspect latest tags/releases and CI;
6. reconcile discrepancies;
7. update this file after material progress.

This protocol is designed so project continuity survives a completely new chat/thread.

---

# Maintenance rule for future development

Any PR/release materially changing one of these must update this file in the same PR or immediately after merge:

- current release version/state;
- completed roadmap milestone/subsystem;
- persistent storage/schema;
- architecture boundary;
- provider behavior;
- compatibility requirement;
- next immediate priority.

`docs/ROADMAP.md` answers **“Where are we going?”**
`docs/PROJECT_STATE.md` answers **“Where are we now, and what should happen next?”**
