# VillAIgence Project State

> **Canonical current-state handoff.** Read this file before `docs/ROADMAP.md` when resuming work in a new session.
>
> Last major state update: **2026-07-22**.
>
> This file must describe what is actually implemented/merged, not aspirational roadmap items. Before relying on it for active development, verify recent GitHub PRs, tags/releases and CI because repository activity may have advanced since the last edit.

## Project identity

- **Public name:** VillAIgence
- **Wordplay:** Vill-AI-gence
- **Short name:** VAI
- **Tagline:** `Giving villagers a mind of their own.`
- **Repository:** `True-Ruslan/villAIgence`
- **Primary development/base branch:** `1.21.1`
- **Target game:** Minecraft 1.21.1
- **Primary loader:** Fabric
- **Compatibility build:** NeoForge is kept compiling in CI where applicable
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

Public branding is VillAIgence; internal compatibility identifiers remain MCA/LivingWorld where changing them would break worlds, configs, loaders or upstream-derived code.

---

# Product direction

VillAIgence is no longer treated as merely “MCA Reborn with an AI chat feature.”

The intended product is a **persistent living-society simulation layer for Minecraft** built on the MCA-derived NPC/social body.

Core idea:

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

The full versioned vision is in `docs/ROADMAP.md`.

---

# Current implementation status

## 1. Server-side AI foundation — implemented

VillAIgence has a server-owned AI provider layer with OpenAI-compatible/OpenRouter routing.

Key guarantees:

- API keys remain server-side;
- provider settings are server configuration;
- external provider failure should degrade safely rather than mutate the world blindly;
- LLM output is not authoritative Minecraft state.

Relevant historical work includes the original LivingWorld AI foundation and later provider hardening.

---

## 2. Voice input pipeline — implemented

Current conceptual pipeline:

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
- STT support through OpenAI-compatible/OpenRouter paths;
- text-only voice-input mode;
- `voiceInputEnabled` and `voiceOutputEnabled` split;
- new installations default to `voiceOutputEnabled=false`;
- normal player-to-player Simple Voice Chat traffic remains separate.

No raw microphone audio is intended to be persisted.

---

## 3. Text-to-speech and spatial NPC voice — implemented

VillAIgence supports optional NPC TTS output with spatial playback through Simple Voice Chat.

### OpenRouter raw PCM support

Merged in PR #24 (`feat(livingworld): add OpenRouter PCM TTS and resilient structured response parsing`).

Implemented:

- `ttsResponseFormat`: `auto`, `wav`, `pcm`;
- `auto` resolves OpenRouter TTS to raw PCM and other OpenAI-compatible endpoints to WAV;
- raw signed PCM16 little-endian mono decoding;
- MIME `rate`/`channels` parsing;
- configured fallback sample rate;
- 48 kHz resampling before Simple Voice Chat playback;
- WAV path retained;
- text reply is published before/independently from TTS failure.

TTS failure must not remove a valid text response.

---

## 4. Persistent per-NPC voice identity — implemented

Merged in PR #23 (`feat: add persistent gender and age aware NPC voices`).

Persistent storage:

```text
<world>/livingworld/voices.json
```

Implemented model:

```text
NPC UUID
+ MCA gender
+ MCA age bucket
→ deterministic compatible voice selection
→ persisted voice profile
```

Age mapping follows actual MCA states:

```text
BABY / TODDLER / CHILD → child
TEEN                   → teen
ADULT / UNASSIGNED     → adult
```

MCA 1.21.1 currently has no distinct elder state, so VillAIgence does not fabricate one.

Properties:

- voice remains stable across restarts;
- voice identity is independent from the chat/LLM model;
- configurable male/female/neutral child/teen/adult voice pools;
- persisted profile can be re-resolved on a real age/gender transition or incompatible pool change;
- different NPCs can receive different voices.

### Mood-aware delivery — implemented foundation

Current moods include:

```text
NEUTRAL
HAPPY
SAD
ANGRY
AFRAID
TIRED
```

Mood changes delivery/style, not the NPC’s persistent base voice.

Mood is derived from server-owned state such as panic, health and persistent relationship dimensions rather than freely invented by the LLM.

---

## 5. Persistent player↔NPC dialogue memory — implemented foundation

Persistent memory exists and is world-local.

Current storage includes:

```text
<world>/livingworld/memory.json
```

The current system is primarily bounded persistent dialogue/context memory.

**Important:** This is the foundation, not the planned `Memory 2.0` architecture.

The next major memory milestone will separate:

- working memory;
- episodic memory;
- semantic knowledge;
- relationship reasons/history;
- importance/decay/provenance.

See roadmap milestone `0.2`.

---

## 6. World events / factual knowledge foundation — implemented

Current storage includes:

```text
<world>/livingworld/events.json
```

Implemented foundation:

- server-owned factual events;
- provenance/time/location/dimension context;
- bounded event queries;
- factual context can be injected into the NPC context snapshot;
- LLM is not allowed to inject arbitrary facts into authoritative event state.

This is the base for future knowledge propagation and rumors, not yet a complete social knowledge network.

---

## 7. Player↔NPC relationship state — implemented foundation

Current storage includes:

```text
<world>/livingworld/relationships.json
```

Current dimensions:

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
- malformed deltas must not break or leak into visible dialogue.

**Not yet implemented:** general persistent `NPC ↔ NPC` social graph. This is a major target for roadmap `0.3`.

---

## 8. Safe actions — implemented foundation

LLM-triggered actions use a constrained/whitelisted path rather than arbitrary command execution.

Design principle:

```text
LLM proposes
→ policy validates
→ server revalidates entity/player/world state
→ server executes allowed mutation
```

Mutable Minecraft state is intended to be accessed on the server thread, with immutable snapshots passed into asynchronous AI work.

---

## 9. Immutable authoritative context snapshot — implemented

Before asynchronous AI processing, VillAIgence captures an authoritative server-side context snapshot.

This prevents asynchronous AI/provider code from freely reading mutable Minecraft entities after leaving the server thread.

The snapshot is the boundary for:

- player/NPC identity;
- world facts;
- available actions;
- relationship context;
- language/context metadata;
- relevant voice-state capture where applicable.

---

## 10. Structured response sanitization — implemented and hardened

VillAIgence has a dedicated structured-response parser that isolates the visible NPC message from optional command/relationship metadata.

Important behavior:

- malformed `relationshipDelta` cannot expose raw JSON;
- malformed optional metadata should not invalidate a recoverable message;
- Markdown/code fences are stripped safely;
- malformed JSON attempts safe `message` recovery only;
- unrecoverable structured JSON is not spoken/displayed as raw metadata;
- language prompt explicitly requests the language of the player’s latest message.

This addresses the historical JSON-tail bug where malformed numeric relationship fields could leak structured metadata into visible dialogue.

---

## 11. OpenRouter `content: null` hardening — merged

Merged in PR #26 (`fix: handle empty OpenRouter chat completions safely`).

Root cause in VillAIgence 0.1.5:

```java
getAsJsonPrimitive("content").getAsString()
```

assumed `choices[0].message.content` was always a string. A valid OpenAI-compatible envelope with `content: null` caused:

```text
JsonNull cannot be cast to JsonPrimitive
```

Implemented fix:

- dedicated `ChatCompletionResponseParser` for the external provider envelope;
- null/missing/blank `content` handled safely;
- captures `finish_reason`;
- captures root/choice provider errors and `error_type` where available;
- captures generation ID;
- records only whether reasoning metadata is present — reasoning content is **never** substituted as NPC-visible text;
- initial request + at most one retry for retryable empty completions;
- no blind retry for terminal `length`, `content_filter` or explicit provider errors;
- controlled `empty_response` fallback after exhaustion;
- diagnostics avoid logging API keys, Authorization headers, prompts or reasoning text.

Retry occurs inside the provider HTTP layer, before memory/actions/relationship application/TTS, so an empty first attempt cannot duplicate those game-side effects.

Final merge commit for PR #26:

```text
52eed8ba8dede8deeaceffbec723255d4515ac8d
```

---

## 12. Branding — implemented

Merged in PR #25.

Public-facing project is now **VillAIgence**.

Updated surfaces include:

- README;
- loader display metadata;
- GitHub/repository links;
- CI/release naming;
- release artifact naming;
- public documentation.

New release artifact convention:

```text
villaigence-fabric-<version>.jar
villaigence-fabric-<version>.jar.sha256
```

Compatibility identifiers (`mca`, Java namespace, livingworld config/data paths) intentionally remain unchanged.

---

# Release state

## Known release sequence relevant to current development

Existing historical tags include releases through `0.1.5+1.21.1`.

`0.1.5+1.21.1` points to the pre-PR-26 state and contains the reported `content: null` crash.

The next patch version intended for the merged null-content/provider-hardening fix is:

```text
0.1.6+1.21.1
```

At the time of this state file creation, `0.1.6+1.21.1` had **not** been found as an existing tag. Before publishing it, verify that:

1. `1.21.1` HEAD is the exact intended release commit;
2. the release workflow/tag validation rules still match;
3. CI/dry-run is green;
4. the tag is still unused;
5. the tag points exactly to the intended current `1.21.1` HEAD.

Do not move already-published version tags.

---

# CI / quality gates

The project uses at least these important checks:

```text
VillAIgence CI
→ :common:test
→ Fabric build
→ distributable Fabric JAR smoke-check

Official Gradle PR CI
→ NeoForge build
→ Fabric build
```

Before claiming a feature/bugfix is complete:

- verify the exact final PR head;
- require fresh green CI on that head;
- review the final diff;
- merge only after no critical/important unresolved issue remains.

Bugfixes should use TDD where practical: reproduce with RED, implement, verify GREEN.

---

# What is NOT implemented yet

The following roadmap features are **planned, not complete**:

### Memory 2.0

- episodic/semantic/relationship-specific layered memory;
- memory importance and forgetting;
- confidence/provenance-aware knowledge consolidation;
- durable summaries beyond raw chat history.

### Full persistent personality system

- stable values/goals/fears/likes/dislikes;
- personality-driven autonomous decisions;
- controlled long-term personality evolution.

### NPC↔NPC social graph

- friendship/trust/respect/fear between arbitrary NPC pairs;
- rivalry/grudges/social history;
- family/romance integration as a unified social graph.

### Rumor/knowledge propagation

- NPCs telling other NPCs facts;
- source chains;
- trust-dependent belief;
- distortion over repeated transmission.

### Autonomous agent loop

- perceive → evaluate → choose intent → validate → act → observe → remember;
- meaningful autonomous behavior while the player is not actively conversing.

### Settlement simulation

- settlement resources/economy/security/leadership/social conditions as persistent causal systems.

### Factions/politics

- diplomacy, alliances, wars, leadership, reputation and cross-settlement consequences.

### Emergent story system

- persistent causal story threads arising from interacting systems rather than fixed scripted quests.

### Large-scale/local AI architecture

- Ollama/LM Studio/vLLM/local model support as first-class targets;
- event-driven/budgeted autonomous AI at large NPC counts;
- simulation LOD/performance architecture.

---

# Immediate next development priorities

## Priority A — finish the `0.1.x` reliability phase

Recommended next tasks:

1. publish/test the `0.1.6+1.21.1` hotfix release containing PR #26;
2. add a clear `/villaigence ai status` / diagnostics surface;
3. improve rate-limit/backpressure/cooldown handling;
4. run multiplayer concurrency and repeated voice-dialogue soak tests;
5. validate restart/reconnect/world-backup behavior for all persistent JSON stores;
6. collect real server feedback on STT/LLM/TTS provider failure modes;
7. decide whether the default/recommended free LLM should change from unstable free-provider choices.

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

Then add bounded retrieval/consolidation before introducing autonomous social propagation.

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

When starting a new ChatGPT/Codex session, the preferred user prompt is:

> **Open `docs/PROJECT_STATE.md` and `docs/ROADMAP.md` in `True-Ruslan/villAIgence`. Check recent PRs/releases/CI, then tell me how VillAIgence development is going, what is complete, what changed since the state file, and what we should build next.**

The new session should not rely only on memory. It should:

1. read this file;
2. read `docs/ROADMAP.md`;
3. inspect current `1.21.1` HEAD;
4. inspect recent merged/open PRs;
5. inspect latest tags/releases and CI;
6. reconcile discrepancies;
7. update this file after material progress.

This protocol is intentionally designed so project continuity survives a completely new chat/thread.

---

# Maintenance rule for future development

Any PR/release that materially changes one of the following should update this file in the same PR or immediately after merge:

- current release version/state;
- completed roadmap milestone/subsystem;
- persistent storage/schema;
- architecture boundary;
- provider behavior;
- compatibility requirement;
- next immediate priority.

`docs/ROADMAP.md` answers **“Where are we going?”**

`docs/PROJECT_STATE.md` answers **“Where are we now, and what should happen next?”**
