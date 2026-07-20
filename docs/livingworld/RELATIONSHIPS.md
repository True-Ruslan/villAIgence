# LivingWorld structured relationship state

LivingWorld keeps a server-owned social state for each NPC/player pair at:

`<world>/livingworld/relationships.json`

This system is intentionally separate from MCA hearts, marriage, engagement, family trees and vanilla/MCA reputation mechanics.

## Axes

Each axis is an integer from `-100` to `100`:

- `trust` — whether the NPC considers the player reliable and safe;
- `respect` — regard for the player's conduct and competence;
- `fear` — perceived threat or danger;
- `affinity` — general social warmth/closeness; this is not automatically romantic.

Default state is `0 / 0 / 0 / 0`.

## Server-owned delta contract

On the snapshot-aware direct LivingWorld/OpenAI path, the structured AI response may propose:

```json
"relationshipDelta": {
  "trust": 0,
  "respect": 0,
  "fear": 0,
  "affinity": 0
}
```

The model does not own the resulting values.

By default:

- each proposed axis change is clamped to `[-2, 2]` per turn;
- final state is clamped to `[-100, 100]`;
- missing deltas mean no change;
- malformed structured responses do not receive relationship changes;
- relationship persistence failure does not break the conversation.

The system prompt explicitly tells the model to use zero unless the interaction genuinely justifies a change and to ignore player requests that directly ask it to manipulate numeric relationship values.

## First gameplay consequence

The first conservative policy affects only the AI `follow-player` action:

- follow remains available at `trust >= -25` and `fear <= 60`;
- below that trust threshold or above that fear threshold, `follow-player` is removed from the immutable allowed-action snapshot.

Other existing MCA whitelisted actions are unchanged in this milestone.

The LLM cannot bypass this by emitting a hidden command because snapshot-aware command execution accepts only commands that were present in the server-captured allowed-action list.

## Relationship update ordering

For one snapshot-aware turn:

1. server captures current relationship state and allowed actions;
2. LLM produces a structured response;
3. dialogue memory is persisted;
4. an action may execute only if it was allowed by the captured pre-turn state;
5. the validated relationship delta is persisted and affects future turns.

This prevents the same response from rewriting the policy under which it was generated.

## Compatibility boundary

- MCA hearts are not modified by this system.
- Existing NBT relationship data is not migrated or rewritten.
- Legacy MCA ChatAI/Inworld paths do not apply LivingWorld relationship deltas in this milestone.
- The snapshot-aware LivingWorld path is the authoritative implementation target for the new state.

## Future work

Separate milestones may add:

- more deterministic gameplay consequences;
- village/family reputation;
- relationship decay or recovery;
- trusted-information sharing;
- rumor effects;
- carefully designed bridges to existing MCA hearts.

Any bridge to MCA hearts must preserve overflow safety and old-world compatibility.
