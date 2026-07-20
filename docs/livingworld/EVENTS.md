# LivingWorld factual events

LivingWorld keeps a small server-owned journal of recent factual events at:

`<world>/livingworld/events.json`

This is **not** a global NPC memory or rumor database. It is a bounded source-of-truth layer used to give an NPC recent local context without making every NPC omniscient.

## Truth boundary

Only deterministic server-generated events are stored as factual events.

For the first implementation, the only event source is a successfully completed whitelisted MCA/LivingWorld NPC action such as following, staying, going home, equipping armor or opening trade.

The following are **not** automatically factual events:

- arbitrary player statements;
- LLM-generated claims;
- rumors;
- assumptions about unseen parts of the world;
- unknown/custom commands.

Each event records provenance, dimension, position, game time and actor/subject identifiers.

## Context visibility

When a LivingWorld context snapshot is captured on the Minecraft server thread, the NPC receives only recent events that satisfy all configured bounds:

- same dimension;
- within `eventContextRadius` (default `32` blocks);
- no older than `eventMemoryMaxAgeTicks` (default `72000` ticks / three Minecraft days);
- at most `eventContextMaxEvents` (default `8`) newest events.

These appear in the LLM factual context as `Observed recent local event: ...` entries.

## Storage bounds

Default maximum journal size: `512` events.

Old events are filtered/compacted in memory during context queries and persisted in the compacted form on a later event append. Writes use a temporary file and atomic replace where supported.

## Failure behavior

Event storage is fail-open:

- an event read failure does not block an AI conversation;
- an event write failure does not roll back the gameplay action that already succeeded;
- raw voice is never stored in this journal.

Back up `livingworld/events.json` together with the Minecraft world if event history is important.

## Future layers

These are intentionally separate future systems:

- player claims and uncertain beliefs;
- rumors and confidence/provenance propagation;
- NPC-to-NPC knowledge exchange;
- broader combat/death/building/quest events;
- event-driven autonomous planning.

Keeping these separate prevents unverified text from silently becoming world truth.
