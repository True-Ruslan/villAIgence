# S10a validation — immutable operator lore context

Date: 2026-08-01  
Target branch: `1.21.1`

## Purpose

Connect the S9 world-local operator lore store to the direct LivingWorld AI path without mixing explicit lore into current observations, episodic memory, semantic memory or provider transport.

## Implemented contract

- `LivingWorldContextSnapshot` has a distinct immutable `operatorAuthoredContext` component.
- Legacy snapshot constructors remain source-compatible and default the new component to an empty list.
- Lore is loaded during `LivingWorldContextCapture.capture(...)`, which is required to run on the Minecraft server thread.
- Scope resolution is deterministic:
  - world lore;
  - current villager UUID;
  - current player UUID;
  - home-village ID plus dimension when a home village exists.
- A villager without a home village does not read a synthetic `-1` village scope.
- Store/read failures are logged and fail open to an empty immutable list.
- Prompt provenance is explicit.
- Observed world facts appear before operator lore and remain authoritative for the current turn.
- Operator lore is inserted before structured JSON/command instructions.
- Provider HTTP, parser, retry, memory ingestion, relationship actions and client networking are unchanged.

## TDD evidence

### RED

```text
head: 774c18f1e55a1657f16f699a8ece6be3d541dcf6
VillAIgence CI #1093 / 30703338955 — expected FAILURE
boundary: common:compileTestJava
reason:
  - SnapshotContextPromptPolicy absent
  - 16-component snapshot constructor absent
  - operatorAuthoredContext accessor absent
```

The Java loader workflow and repository security policy remained green because production code had not changed.

### GREEN

```text
head: b0a2e84df62fb6a5bba053c9ef4115e97e2789e7
VillAIgence CI #1100 / 30703655190              SUCCESS
Java Pull Request CI #608 / 30703655301       SUCCESS
Repository security policy #405 / 30703655198 SUCCESS
```

Validated gates:

- common unit tests;
- Fabric build and refmap generation;
- NeoForge build and refmap generation;
- distributable Fabric package verification;
- deterministic repository security policy.

## Automated policy coverage

- observed facts precede operator lore;
- explicit conflict rule gives current observations precedence;
- empty context produces no prompt section;
- blank entries are omitted;
- multiline provenance-labelled lore remains intact;
- lore is inserted before structured-response instructions;
- snapshot defensively copies the operator-authored list.

## Manual release acceptance

Accumulate the following scenarios into the synchronized release candidate:

1. Populate world, villager, player and village lore in `livingworld/operator-lore.json`.
2. Start a dialogue with the matching villager/player pair.
3. Confirm the NPC can use all applicable lore scopes.
4. Create a direct conflict between stored lore and a current observed fact; confirm the current fact wins.
5. Move the villager to another dimension/village and confirm the old village lore is not selected.
6. Test a villager with no home village; confirm no synthetic village lore appears.
7. Restart the server and confirm identical context selection.
8. Corrupt the lore file, restart, and confirm the dialogue path fails open without crashing or altering existing LivingWorld stores.

## Scope review

Expected changed areas:

- `livingworld/context` snapshot, loader and prompt policy;
- one narrow server-side mixin for prompt insertion;
- mixin registration;
- focused unit tests;
- this validation document.

Explicitly excluded:

- client editor UI;
- packets and permissions;
- `OpenAIChatAI` transport/parser/retry implementation;
- semantic-memory ingestion;
- generated personality or biography;
- changes to `memory.json`, `memory2.json`, `semantic-memory.json`, `events.json`, `relationships.json` or `voices.json`.
