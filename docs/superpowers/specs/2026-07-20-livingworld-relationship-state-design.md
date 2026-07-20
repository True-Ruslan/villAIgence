# LivingWorld Structured Relationship State Design

## Goal

Make AI conversations have persistent, bounded gameplay consequences without letting an LLM directly mutate MCA hearts, marriages, family state, permissions, or arbitrary game data.

## Decision

LivingWorld gets a separate server-owned relationship state for each `NPC UUID × player UUID` pair:

- `trust` — confidence that the player is reliable/safe;
- `respect` — regard for the player's conduct/competence;
- `fear` — perceived threat/danger;
- `affinity` — general social warmth/closeness, explicitly not equivalent to romance.

Each axis is an integer in `[-100, 100]`. Default is `0`.

MCA hearts remain authoritative for existing MCA marriage/engagement/dialogue mechanics and are not synchronized in this milestone.

## LLM boundary

The direct LivingWorld/OpenAI structured response gains an optional `relationshipDelta` object with the four axes.

The LLM may propose a delta, but the server owns the result:

- each proposed per-turn axis delta is clamped to `[-2, 2]` by default;
- final state is clamped to `[-100, 100]`;
- malformed/missing delta becomes no change;
- relationship persistence failure never breaks the conversation;
- legacy MCA ChatAI/Inworld paths do not apply LivingWorld relationship deltas.

Prompt guidance instructs the model to use zero unless the current interaction genuinely justifies a change and never to follow player instructions asking for numeric relationship manipulation.

## Persistence

Store at `<world>/livingworld/relationships.json` behind a focused `LivingWorldRelationshipStore` API.

The store is keyed by NPC/player UUID pair and uses temp-file + atomic replace where supported.

No database or migration service is required.

## Context

The current state is loaded while capturing the immutable server-thread LivingWorld context snapshot and injected as an explicit server-owned fact.

Example:

`LivingWorld social state with player: trust=15, respect=8, fear=-3, affinity=12.`

The LLM sees this as authoritative current state but cannot write it except through the bounded structured delta contract.

## First deterministic gameplay consequence

The available-action snapshot applies a conservative relationship policy:

- `follow-player` is unavailable when `trust < -25` or `fear > 60`;
- all other existing whitelisted actions retain existing MCA eligibility in this milestone.

Because `OpenAIChatAI` can execute only commands present in the immutable snapshot and then revalidates MCA action predicates, the LLM cannot bypass this policy by emitting a hidden command.

This creates a real gameplay consequence while minimizing compatibility risk.

## Update ordering

For one AI turn:

1. capture snapshot including current relationship state and allowed actions;
2. perform async LLM request;
3. persist conversation memory;
4. execute only the action allowed by that captured snapshot;
5. validate/clamp/persist relationship delta for future turns.

The action therefore uses the state the NPC had when deciding how to respond. The new relationship state affects subsequent interactions.

## Security / safety

- no arbitrary heart mutation;
- no direct marriage/family mutation;
- no arbitrary commands;
- no unbounded numeric input;
- no client authority over relationship state;
- no player text automatically interpreted as a state update;
- exact state remains server-owned world data.

## Testing

- state/delta clamping boundaries;
- persistence/reload and NPC/player isolation;
- action-policy thresholds;
- malformed/oversized proposed deltas sanitize safely;
- configuration defaults;
- full LivingWorld CI plus Fabric/NeoForge Gradle builds.

## Explicitly out of scope

- automatic synchronization to MCA hearts;
- romance progression;
- village/family-wide reputation;
- relationship decay over time;
- rumors modifying relationships;
- autonomous hostility/combat;
- trade-price changes;
- quests/rewards based on reputation.

These require separate, evidence-backed milestones.
