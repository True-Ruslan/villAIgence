# VillAIgence Roadmap

> **Canonical product roadmap.** This document defines the long-term direction of VillAIgence and should be updated whenever a milestone materially changes. For the latest implementation status, see `docs/PROJECT_STATE.md`.

## Product vision

**VillAIgence** (`Vill-AI-gence`) is evolving from an MCA-derived AI conversation mod into a **living-society simulation layer for Minecraft**.

The end goal is not merely “NPCs that can chat with an LLM.” The goal is a world where NPCs:

- have stable identities, personalities, memories, relationships, goals and fears;
- know only what they have observed, learned or been told;
- talk naturally by text and voice;
- remember meaningful interactions across sessions;
- form relationships with players and with each other;
- spread facts, rumors and distorted information socially;
- make autonomous, server-authoritative decisions;
- participate in families, settlements, economies, factions and politics;
- create emergent stories that were not authored as fixed quests.

The guiding product idea is:

> **VillAIgence — Giving villagers a mind of their own.**

The internal `LivingWorld` name remains the compatible engine/data namespace for existing configuration and world data. Public product branding is **VillAIgence**.

---

## Architecture principles

These principles apply to every roadmap stage.

### 1. The LLM is not the game authority

The LLM may propose dialogue, interpretations or intentions. Minecraft/server code remains authoritative for facts, permissions and mutations.

```text
World state / server facts
        ↓
Immutable context snapshot
        ↓
LLM reasoning / proposal
        ↓
Validation + policy
        ↓
Server-side action
```

The LLM must never directly invent authoritative world state or bypass action policy.

### 2. Identity must survive provider/model changes

NPC identity, memory, relationships, voice and long-term state must not depend on a specific OpenAI/OpenRouter/Groq/local model.

Changing an LLM provider should not make an NPC become a different person.

### 3. Fail soft, never corrupt the world

AI/STT/TTS providers are external and unreliable by nature. A malformed or empty response must degrade to a controlled fallback, never crash the conversation pipeline, corrupt persistent state, duplicate actions or expose raw JSON/reasoning.

### 4. No hidden client secrets

API keys and provider credentials remain server-side. Clients receive only gameplay/network data required for the experience.

### 5. Persistence is explicit and inspectable

Important long-term state is stored under the world-local LivingWorld/VillAIgence data area and must be safe to back up, migrate and inspect.

### 6. Simulation before spectacle

Prefer systems that create durable emergent behavior over isolated scripted “AI tricks.” Memory, social state and causality are more important than flashy one-off dialogue.

---

# Versioned development roadmap

## 0.1.x — Reliability and provider hardening

### Goal

Make the current AI/voice foundation safe enough that provider failures are ordinary recoverable events rather than gameplay-breaking exceptions.

### Scope

- harden OpenAI-compatible response envelope parsing;
- handle `content: null`, partial responses, malformed structured JSON and provider errors;
- bounded retry policies with no duplicated memory/actions/relationship effects;
- diagnostics for `finish_reason`, provider errors, generation IDs and response modes;
- robust STT/TTS timeout and error handling;
- rate-limit/cooldown protection;
- multiplayer concurrency validation;
- restart/reconnect/crash recovery tests;
- clear `/villaigence` diagnostics/status commands;
- stable release packaging and migration behavior.

### Exit criteria

- provider failure never crashes the AI conversation path;
- malformed responses never leak raw JSON/reasoning into chat or TTS;
- retry cannot duplicate persistent/game-side effects;
- voice input/output failure never removes a valid text reply;
- CI covers common regression paths;
- a server operator can diagnose STT → LLM → parser → memory → TTS failures from concise logs/status output.

---

## 0.2 — Memory 2.0

### Goal

Move from “stored chat history” to **human-like layered memory**.

### Memory model

```text
Working memory
→ recent conversational context

Episodic memory
→ meaningful events involving this NPC

Semantic memory
→ facts the NPC believes/knows about people and the world

Relationship memory
→ reasons behind trust/respect/fear/affinity changes
```

### Planned capabilities

- importance scoring for memories;
- summarization/consolidation of old dialogue into durable facts/events;
- decay/forgetting by importance and time;
- provenance: observed vs told vs inferred;
- confidence/uncertainty for remembered information;
- duplicate memory merging;
- bounded retrieval by relevance, recency and importance;
- explicit reasons attached to relationship changes;
- migration from current persistent dialogue memory.

### Example

Instead of remembering only:

> Player: I saved your daughter from zombies.

The NPC can retain:

```text
EPISODE:
Ruslan rescued my daughter during a zombie attack.
importance: high
emotion: gratitude/fear
source: witnessed/confirmed

RELATIONSHIP REASON:
trust +2 — protected family
respect +2 — acted bravely
```

### Exit criteria

NPCs can correctly recall important events days later without requiring full raw conversation history.

---

## 0.3 — Personality + NPC↔NPC social graph

### Goal

Make every NPC a persistent individual and extend relationships beyond `Player ↔ NPC` to `NPC ↔ NPC`.

### Personality profile

Planned stable traits include:

```text
temperament
values
goals
fears
likes / dislikes
speech style
morality
ambition
curiosity
sociability
aggression
loyalty
```

Traits should be mostly deterministic/persistent, with limited evolution caused by major experiences rather than random regeneration.

### Social graph

Each relevant NPC pair may track dimensions such as:

```text
friendship
trust
respect
fear
family
rivalry
romance
grudge
```

The exact schema should remain compact and simulation-friendly rather than becoming an unbounded LLM-generated profile.

### Why this milestone matters

This is the point where NPC statements such as:

> “I do not trust Rex.”

can be grounded in actual persistent social state instead of invented dialogue.

### Exit criteria

Two NPCs can have a persistent relationship history that affects dialogue, behavior and information exchange.

---

## 0.4 — Knowledge, information propagation and rumors

### Goal

Create an actual **information ecosystem** inside the world.

### Knowledge provenance

NPC knowledge should distinguish:

```text
OBSERVED
TOLD_BY_SOMEONE
OFFICIAL / PUBLIC
INFERRED
RUMOR
UNKNOWN
```

An NPC must not automatically know everything stored on the server.

### Rumor propagation

Example:

```text
FACT
Ruslan stole one diamond from Rex.
        ↓
Rex tells Ara
        ↓
RUMOR
Ruslan stole something valuable from Rex.
        ↓
Ara tells another villager
        ↓
DISTORTED RUMOR
Ruslan robs villagers.
```

Propagation should depend on:

- relationship/trust between speaker and listener;
- sociability/gossip tendency;
- importance/emotional intensity;
- distance/community membership;
- time and memory decay.

### Exit criteria

Information can move through a settlement without the player directly talking to every NPC, while preserving source/provenance and allowing uncertainty/distortion.

---

## 0.5 — Autonomous NPC agents

### Goal

Move from purely reactive NPCs to **server-authoritative autonomous behavior**.

Current pattern:

```text
Player speaks
→ AI responds
→ optional action
```

Target pattern:

```text
NPC perceives event/state
→ evaluates needs/goals/social context
→ chooses intention
→ validated server action
→ observes result
→ memory/state update
```

### Agent loop

```text
Perceive
↓
Evaluate
↓
Plan / choose intent
↓
Policy validation
↓
Act
↓
Observe result
↓
Remember
```

### Important safety rule

The LLM proposes **intent**, not arbitrary Minecraft commands.

Example:

```json
{
  "intent": "seek_help",
  "targetRole": "guard"
}
```

Server-side code resolves whether this is possible, selects a valid target and executes allowed behavior.

### Candidate autonomous behaviors

- flee danger;
- seek food/shelter/help;
- avoid disliked or feared characters;
- report threats;
- visit family/friends;
- investigate important events;
- ask the player/NPCs for help;
- pursue role-specific tasks.

### Exit criteria

NPCs produce meaningful behavior while no player is actively talking to them, without uncontrolled LLM calls or unsafe world mutation.

---

## 0.6 — Settlement simulation

### Goal

Treat villages as social/economic systems rather than collections of independent NPCs.

### Settlement state

```text
Settlement
├── population
├── families
├── roles/professions
├── resources
├── food/economy
├── security
├── leadership
├── reputation
└── current problems/events
```

### Example systemic consequences

```text
food shortage
→ dissatisfaction rises
→ food prices/value rise
→ farmers gain social importance
→ theft/conflict risk increases
```

```text
frequent hostile attacks
→ fear rises
→ demand for guards grows
→ leadership is judged by security
```

### Exit criteria

NPC decisions and dialogue can be grounded in persistent settlement-level conditions with visible gameplay consequences.

---

## 0.7 — Factions and politics

### Goal

Scale social simulation beyond one settlement.

### Possible entities

```text
villages / towns
merchant groups
bandits
kingdoms
religious orders
military groups
player-created alliances
```

### Relationship states

```text
ALLY
FRIENDLY
NEUTRAL
RIVAL
HOSTILE
WAR
```

### Planned systems

- faction membership and reputation;
- diplomatic relations;
- leadership and succession;
- alliances/conflicts;
- political goals;
- player alignment and consequences;
- local knowledge of faction events rather than omniscience.

### Exit criteria

A player’s actions in one community can create persistent political/social consequences elsewhere through believable information channels.

---

## 0.8 — Emergent stories

### Goal

Allow stories to emerge from interacting systems rather than authored quest scripts.

Example chain:

```text
Rex and Ara become rivals
↓
Ara spreads a damaging rumor
↓
Rex loses social support
↓
Rex suspects the player helped Ara
↓
player chooses a side
↓
settlement opinion splits
↓
leadership dispute develops
↓
new alliances and grudges form
```

No single fixed quest script needs to define that chain. The story emerges from:

- memory;
- personality;
- relationships;
- knowledge propagation;
- goals;
- settlement state;
- autonomous choices.

### Supporting systems

- event significance classification;
- story-thread tracking for debugging/UX, not scripting outcomes;
- optional journals/history summaries;
- anti-chaos constraints so the simulation remains coherent.

### Exit criteria

Playtests consistently produce understandable, causally connected stories unique to each world.

---

## 0.9 — Scale, performance and local AI

### Goal

Make the simulation practical for large persistent servers.

### Provider support target

```text
OpenRouter
OpenAI
Groq where applicable
Ollama
LM Studio
vLLM
other OpenAI-compatible endpoints
```

### Performance architecture

- event-driven AI activation rather than polling every NPC;
- tiered simulation detail based on relevance/distance;
- deterministic server logic for simple decisions;
- LLM only where language/high-level reasoning adds value;
- batching/caching where safe;
- per-server token/cost budgets;
- rate-limit queues and backpressure;
- local-model support for high-volume NPC reasoning;
- profiling for hundreds/thousands of NPC state records.

### Exit criteria

VillAIgence can support a substantial persistent population without requiring one expensive LLM call per NPC per tick or destabilizing server TPS.

---

## 1.0 — Living world milestone

### Product promise

A mature VillAIgence world should support this kind of experience:

> You return to a village after several in-game weeks. A villager remembers that you rescued his daughter. His wife knows because he told her. A neighbor heard a distorted version. Someone who hates that family now distrusts you by association. The local leadership has changed because of events that happened while you were away. NPCs discuss those events, act according to their own goals, and react differently based on what they personally know.

At 1.0, VillAIgence should feel less like **“Minecraft + chatbot NPCs”** and more like a **persistent simulation of people, communities and history inside Minecraft**.

---

# Voice roadmap across milestones

Voice is a cross-cutting system rather than a separate end goal.

Already-established direction:

- microphone/STT input;
- text always remains authoritative fallback;
- persistent per-NPC voice identities;
- gender/age-aware configurable voice pools;
- mood-aware delivery without changing NPC identity;
- spatial playback from the NPC;
- provider-independent voice profile state.

Future voice capabilities:

### Group-addressed speech

```text
Player: “Has anyone seen the blacksmith?”
↓
Nearby NPCs hear the utterance
↓
Only NPCs with relevant knowledge/attention respond
```

### NPC↔NPC audible conversations

NPCs may initiate short contextual conversations that nearby players can overhear.

This must be rate-limited, relevance-driven and simulation-grounded to avoid constant noisy AI chatter.

---

# Recommended immediate sequence

The preferred development order from the current foundation is:

```text
0.1.x  Reliability / provider hardening
        ↓
0.2    Memory 2.0
        ↓
0.3    Personality + NPC↔NPC social graph
        ↓
0.4    Knowledge + rumors
        ↓
0.5    Autonomous agents
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

The highest-value next major product milestone after `0.1.x` stabilization is **Memory 2.0**, followed immediately by **Personality + NPC↔NPC social relationships**. These systems unlock nearly every later feature.

---

# Scope discipline

To prevent feature creep:

1. Every major feature should map to one roadmap milestone.
2. Reliability/security/data-integrity fixes may interrupt any milestone.
3. A milestone should establish a reusable system, not a one-off demo.
4. LLM prompts are not a substitute for persistent simulation state.
5. New persistent schemas require migration/backward-compatibility planning.
6. Every external-provider path must have deterministic fallback behavior.
7. Expensive autonomous behavior must be event-driven and budget-aware.

---

# How to resume this project in a new ChatGPT/Codex session

When starting a fresh session, use the repository as the source of truth and ask:

> **“Open `docs/PROJECT_STATE.md` and `docs/ROADMAP.md` in `True-Ruslan/villAIgence`. Tell me the current VillAIgence status, what has been completed, what is in progress, what the next milestone is, and continue development from there.”**

The assistant/agent should:

1. read `docs/PROJECT_STATE.md` first;
2. read this roadmap;
3. inspect recent merged/open PRs and CI before assuming the state is current;
4. update `docs/PROJECT_STATE.md` whenever a milestone or release materially advances;
5. never infer completion solely from this roadmap — roadmap describes intent, project state describes progress.
