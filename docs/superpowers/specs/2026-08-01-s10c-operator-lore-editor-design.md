# S10c Operator Lore Editor Design

## Status

Approved design for the VillAIgence S10c client editor over the completed S9 persistence, S10a immutable context and S10b server-authoritative API.

Approval date: **2026-08-01**.

Canonical baseline:

```text
branch: 1.21.1
base commit: e39b3eca5ba147d7764449508c6ab84ea7d1333a
S9 merge: f4a0d369c6e787d9ed91501fc87323aded9b4cbc
S10a merge: b700e14636e3370774173978b0b2519941dafed0
S10b merge: 3cc3137b09629ab348cf0d3e79c821a524259d56
```

## Purpose

Provide a dedicated operator-facing Minecraft client screen for reading and editing world-local operator lore in the four supported scopes:

```text
WORLD
PLAYER
VILLAGER
VILLAGE
```

The screen is a client presentation layer only. It must not weaken or duplicate S10b authority. The server remains responsible for permission checks, target resolution, scope identity, payload validation, revision comparison and persistence.

## Goals

1. Give server operators a clear in-game editor for world, authenticated player, nearby villager and that villager's home-village lore.
2. Preserve the S10b optimistic-concurrency contract and display conflicts without silent overwrite.
3. Make all loading, saving and error states explicit.
4. Prevent accidental loss of unsaved edits.
5. Keep the editor isolated from the existing Villager Editor and from AI provider, semantic-memory and gameplay mutation paths.
6. Keep both Fabric and NeoForge client builds compatible while preserving dedicated-server classloading safety.

## Non-goals

S10c does not:

- add or modify server permissions;
- accept arbitrary UUID, dimension or village identifiers from the client;
- write `operator-lore.json` directly from client code;
- expose a file browser or raw JSON editor;
- generate biographies, personality profiles, goals or beliefs;
- ingest operator lore into semantic memory;
- modify AI transport, response parsing, retries, STT or TTS;
- replace the existing Villager Editor;
- add bulk editing, search, history, diff storage or audit-log persistence;
- change the S9 schema or existing world-local files;
- promote the synchronized train to a release before cumulative live acceptance.

## Chosen interaction model

### Accepted: dedicated Operator Lore Editor screen

A standalone screen is used instead of embedding the feature in the existing Villager Editor.

Reasons:

- WORLD and PLAYER scopes are not inherently attached to a villager;
- VILLAGER and VILLAGE share a nearby runtime entity target but resolve to different server-owned keys;
- the editor requires asynchronous load/save/conflict states that should not enlarge an unrelated screen;
- isolation makes classloading, regression testing and rollback easier;
- the same screen can be opened with or without a villager target.

### Rejected: Villager Editor tab

Embedding the feature would couple global/player lore to a villager-centric screen, increase regression surface and make the authority lifecycle harder to reason about.

### Rejected: command-only interface

Commands remain useful for diagnostics, but a command-only solution does not provide usable multiline editing, dirty-state protection or conflict presentation.

## Entry points

S10c supports two server-authoritative opening modes.

### Global opening

An operator command opens the editor without a villager target:

```text
/villaigence lore editor
```

Available scopes:

```text
WORLD
PLAYER
```

VILLAGER and VILLAGE tabs are disabled because no trusted nearby runtime entity target was supplied by the server.

### Villager-targeted opening

An operator-only interaction or command opens the editor with a live villager entity ID chosen by the server:

```text
/villaigence lore editor <nearby MCA villager>
```

Available scopes:

```text
WORLD
PLAYER
VILLAGER
VILLAGE
```

The client receives only the runtime entity ID already allowed by S10b. The server still resolves the entity, validates distance and same-level presence, derives villager UUID, and derives home-village dimension and stable ID.

### GUI payload integration

The existing `OpenGuiRequest` gains one new enum value for `OPERATOR_LORE_EDITOR` and continues to carry an integer villager entity ID. A sentinel value of `-1` means no villager target.

The new GUI value must be appended rather than inserted into the enum to preserve ordinal compatibility for existing values.

Opening the screen never grants access. Every read and write still requires the S10b server permission level and target validation.

## Screen layout

The editor uses a single responsive panel centred on the current Minecraft screen.

```text
┌──────────────────────────────────────────────────────────────┐
│ Operator Lore                                                │
│ [ World ] [ Player ] [ Villager ] [ Village ]               │
│                                                              │
│ Scope: World                                                 │
│ Target: Current world                                        │
│                                                              │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ Multiline operator-authored lore                         │ │
│ │                                                          │ │
│ └──────────────────────────────────────────────────────────┘ │
│                                                              │
│ 128 / 4096 code points       214 / 12288 UTF-8 bytes         │
│ Status: Loaded                                                │
│                                                              │
│ [Reload] [Clear]                         [Save] [Close]       │
└──────────────────────────────────────────────────────────────┘
```

### Responsive dimensions

- panel width: bounded between 320 and 720 pixels, using available screen width minus safe margins;
- panel height: bounded between 220 pixels and available screen height minus safe margins;
- text area receives remaining vertical space after header, tabs, metadata, counters and buttons;
- narrow screens wrap metadata and counters rather than allowing controls to leave the viewport;
- GUI scale changes must not hide Save, Reload or Close.

### Scope tabs

The four tabs are always visible in a stable order:

```text
WORLD → PLAYER → VILLAGER → VILLAGE
```

Disabled tabs remain visible with a tooltip explaining why the scope cannot currently be resolved.

Selecting a different scope is a navigation action. When the current buffer is dirty, the editor displays a discard confirmation before changing scope.

### Target labels

The client may show only presentation data already available locally, such as the selected villager display name. It must not display a client-computed village ID or claim that a target is valid before the server read succeeds.

Canonical labels:

```text
WORLD     Current world
PLAYER    Your player lore
VILLAGER  Selected villager
VILLAGE   Selected villager's home village
```

If server resolution fails, the target label remains descriptive and the status area displays `NOT_FOUND` rather than retargeting automatically.

## Client state model

The screen is driven by a small explicit state machine rather than scattered booleans.

### States

```text
IDLE
LOADING
LOADED
DIRTY
SAVING
CONFLICT
FORBIDDEN
NOT_FOUND
INVALID
ERROR
```

### Canonical state record

The client-side screen model contains:

```text
scope
villagerEntityId
serverValue
workingValue
revision
status
requestGeneration
lastMessage
```

Properties:

- `serverValue` is the last canonical value returned by the server;
- `workingValue` is the current editable buffer;
- `revision` is the last server-returned SHA-256 revision;
- dirty state is derived from `workingValue != serverValue` after the same newline canonicalization used by S10b;
- `requestGeneration` monotonically increases for every load/save request and prevents an older response from replacing newer screen state;
- no value is persisted locally after the screen closes.

### Response correlation

S10b responses currently identify scope and canonical entity ID but do not carry a request identifier. S10c introduces a bounded integer `requestId` on read request, write request and response payloads.

The server echoes the request ID unchanged after validating the rest of the request. The ID has no authority meaning and is used only for client response correlation.

This is the only S10b protocol extension required by S10c. It must not change permission, target-resolution, revision, payload or persistence decisions.

The screen applies a response only when:

```text
response.requestId == current requestGeneration
response.scope == selected scope
response.entityId matches the server-canonical target expected for that scope
screen is still open
```

Late or unrelated responses are ignored.

## Data flow

### Open

```text
server validates command/interaction permission
→ server sends OpenGuiRequest(OPERATOR_LORE_EDITOR, entityId or -1)
→ client constructs screen
→ client chooses initial scope
→ client sends OperatorLoreReadRequest(requestId, scope, entityId)
→ server repeats permission and target validation
→ server returns canonical status, value and revision
→ client enters LOADED or an explicit error state
```

Initial scope:

```text
with villager target: VILLAGER
without target:       WORLD
```

### Edit

```text
LOADED
→ user changes workingValue
→ client computes code-point and UTF-8 counters
→ state becomes DIRTY
```

Client validation is advisory and mirrors S10b limits:

```text
MAX_CODE_POINTS = 4096
MAX_UTF8_BYTES  = 12288
```

The text widget may temporarily hold invalid content so the user can correct it, but Save is disabled while the client-side mirror reports invalid length or control characters. The server remains authoritative and validates again.

### Save

```text
DIRTY
→ user presses Save
→ client sends requestId, scope, entityId, revision, workingValue
→ state becomes SAVING and editing actions are temporarily disabled
→ server resolves trusted target and compares revision
```

Server outcomes:

```text
OK         replace serverValue, workingValue and revision; state LOADED
UNCHANGED  replace canonical fields; state LOADED
CONFLICT   keep local workingValue separately; expose current server value/revision; state CONFLICT
FORBIDDEN  state FORBIDDEN
NOT_FOUND  state NOT_FOUND
INVALID    state INVALID
ERROR      state ERROR
```

### Conflict handling

A conflict never silently overwrites either side.

The screen presents:

```text
The lore changed on the server while you were editing.
[Use server version] [Keep my draft] [Close]
```

Behavior:

- **Use server version** replaces `workingValue` and `serverValue` with the returned canonical server value and returns to LOADED;
- **Keep my draft** preserves the local text, adopts the returned server value as the new comparison base and adopts the returned revision, returning to DIRTY; the operator must press Save again deliberately;
- **Close** follows the normal dirty-close confirmation and does not write.

No automatic merge is attempted in S10c.

### Reload

Reload performs a fresh server read.

- when clean, it immediately enters LOADING;
- when dirty, it requires discard confirmation;
- a successful reload replaces value and revision;
- a failed reload preserves the current local buffer but moves to the corresponding explicit status.

### Clear

Clear sets `workingValue` to an empty string and marks the screen DIRTY. It does not send a request until Save is pressed.

### Close

- clean screen: closes immediately;
- dirty screen: displays `Discard unsaved changes?` with `Discard` and `Continue editing`;
- saving screen: Close remains available but warns that the in-flight result will be ignored after closure;
- closing never sends an implicit write.

## Status and error presentation

The status line is always present to avoid layout shifts.

Canonical messages:

```text
LOADING    Loading server lore…
LOADED     Loaded
DIRTY      Unsaved changes
SAVING     Saving…
CONFLICT   Server value changed; review required
FORBIDDEN  Operator permission is required
NOT_FOUND  The selected target is unavailable or has no home village
INVALID    The server rejected the scope or content
ERROR      The server could not read or save operator lore
```

Errors must not include stack traces, filesystem paths, JSON content, prompt content or hidden provider information.

Retry behavior:

- FORBIDDEN: Save disabled; Reload may be used in case permissions changed;
- NOT_FOUND: Save disabled; Reload may be used if the target becomes valid again;
- INVALID: Save remains disabled until the local buffer and scope are valid;
- ERROR: Reload and Save retry are available when otherwise valid;
- no automatic retry loop.

## Widget and rendering boundaries

### Multiline editor

Use the Minecraft 1.21.1 multiline text widget already available in the mapped client API. A thin VillAIgence wrapper may be introduced only to provide:

- value-change callback;
- code-point and UTF-8 counters;
- focus preservation during status updates;
- bounded visible scrolling;
- keyboard shortcuts consistent with Minecraft text controls.

The wrapper must not implement persistence or networking.

### Keyboard behavior

```text
Ctrl/Cmd + S  Save when valid and dirty
Ctrl/Cmd + R  Reload, with dirty confirmation
Escape        Close, with dirty confirmation
Tab           normal widget focus traversal
```

The editor does not capture gameplay keys after it closes.

### Accessibility and localization

- all visible strings use translation keys;
- buttons have narration labels;
- disabled scope tabs expose a narrated reason;
- state is not communicated by colour alone;
- counters use text and change narration when limits are exceeded;
- English translation keys are required; additional translations are optional for S10c and must not block the feature.

## Client/server architecture

### Client-only classes

Client screen and rendering classes live in a client package and are never referenced from common dedicated-server initialization paths.

Recommended boundaries:

```text
OperatorLoreEditorScreen       rendering and user interaction
OperatorLoreEditorModel        pure state transitions and dirty/conflict logic
OperatorLoreEditorController   network requests and response correlation
OperatorLoreEditorOpenContext  immutable scope/target opening data
```

The model should avoid direct Minecraft dependencies where practical so it can be unit-tested in `common` tests or a dedicated pure Java test source.

### Common networking classes

Existing S10b request/response payloads remain common. The request ID extension is added consistently to:

```text
OperatorLoreReadRequest
OperatorLoreWriteRequest
OperatorLoreResponse
```

`OperatorLoreClientState` may remain the network response mailbox, but the final design prefers dispatching a response to the active controller through the existing client network handler. The mailbox must not become permanent hidden storage.

### Server authority

`OperatorLoreServerAuthority` remains the single decision point for reads and writes.

S10c may add only:

- request ID echoing;
- a server command or controlled interaction that sends `OpenGuiRequest`;
- bounded diagnostics for denied or failed opening attempts.

It may not move validation into the screen or allow the client to choose persistent identities.

## Permission model

Opening and using the screen require the existing S10b permission level:

```text
ServerPlayer.hasPermissions(2)
```

Permission is checked:

1. before the server sends the open-screen request;
2. on every read;
3. on every write.

A client opening a screen through a modified client or stale packet gains no data and no write capability without the server checks.

## Security and privacy

- packets contain no arbitrary UUID, dimension or village ID;
- packet strings remain bounded by codecs before allocation where supported;
- server values remain subject to safe network view handling;
- revision remains SHA-256 of canonical server content;
- logs do not contain lore values;
- client drafts remain in memory only and are discarded on close/disconnect;
- no clipboard automation, file export or external network access is added;
- no provider credential or AI request is involved.

## Testing strategy

### Pure model tests

Test the state model independently:

- initial scope with and without villager target;
- dirty detection after canonical newline normalization;
- Save enabled only when dirty, valid and not busy;
- Clear creates an explicit dirty empty value;
- stale request IDs are ignored;
- scope-mismatched responses are ignored;
- successful save adopts canonical value and revision;
- unchanged save returns clean state;
- conflict retains both local draft and server value;
- Use server version and Keep my draft transitions;
- dirty scope-change confirmation;
- dirty close confirmation;
- late response after close is ignored;
- exact code-point and UTF-8 boundary behavior;
- invalid control characters disable Save.

### Common networking tests

- request/response codec round trips with request ID;
- maximum accepted scope, revision and lore lengths;
- oversized payload rejection remains fail-closed;
- server echoes request ID without using it for authority;
- existing S10b permission, target and revision tests remain green.

### Client integration tests

Where automated client harness coverage is practical:

- screen opens for global and targeted contexts;
- disabled tabs are not selectable;
- resizing and GUI-scale changes keep controls visible;
- button and keyboard actions invoke expected controller methods;
- status messages and counters update without losing text focus;
- conflict dialog exposes all three explicit choices;
- dedicated server starts without loading client screen classes.

### Build and repository gates

Every S10c head must pass:

```text
:common:test
:fabric:build
:neoforge:build
VillAIgence Fabric package verification
repository security policy
```

No dependency, workflow, script, lockfile or verification-metadata change is expected.

## Live acceptance

S10c live acceptance is accumulated into the synchronized release-candidate scenario rather than requiring a separate production installation.

Required editor scenarios:

1. Non-operator cannot open, read or write.
2. Operator opens WORLD and PLAYER without a villager target.
3. VILLAGER and VILLAGE are disabled without a target.
4. Operator opens a nearby villager and reads all four scopes.
5. VILLAGE returns NOT_FOUND for a villager without a home village.
6. WORLD, PLAYER, VILLAGER and VILLAGE values survive restart in `operator-lore.json`.
7. Loaded lore appears in the next AI snapshot at the correct scope.
8. Contradictory current observed world facts still win over operator lore.
9. Two editors create a stale-write conflict; neither value is silently lost.
10. Boundary payloads at 4096 code points and 12288 UTF-8 bytes behave correctly.
11. An oversized manually seeded store value returns a safe view and can be replaced using its revision.
12. Dirty close, scope switch, Reload and Clear confirmations work.
13. Fabric client, NeoForge client compatibility and dedicated server startup remain healthy.
14. Text, STT, Chat, TTS, Memory 2.0 and all persistent-file restart checks remain green.

## Rollback

S10c adds only client presentation, a request-correlation field and an opening route over the existing additive `operator-lore.json` store.

Rollback rules:

- retain a world backup and the previous JAR;
- S10c must not rewrite the S9 schema;
- removing S10c leaves existing operator lore readable by S9/S10a/S10b-capable builds;
- protocol changes are shipped in the same client/server release candidate;
- mixed client/server versions are not guaranteed for the editor and must fail without persistence mutation;
- rollback does not require deleting `operator-lore.json` or any Memory 2.0 file.

## Implementation sequence

```text
1. Add pure OperatorLoreEditorModel RED tests.
2. Implement model state transitions and validation mirrors.
3. Add requestId codec RED tests and extend S10b payloads without changing authority.
4. Add server-authorized open command and append OpenGuiRequest enum value.
5. Implement client controller and response dispatch/correlation.
6. Implement responsive dedicated screen and confirmations.
7. Add translation keys and accessibility labels.
8. Run common, Fabric, NeoForge, package and security gates.
9. Record exact RED/GREEN evidence and scope review.
10. Merge S10c only after final documented head is green.
11. Build one synchronized release candidate and execute cumulative S1–S10c live acceptance.
```

## Decision summary

S10c is a dedicated, operator-only, server-authoritative multiline editor. The client manages presentation, local drafts and response correlation; the server continues to own every persistent identity and mutation decision. Conflicts require explicit operator action, unsaved changes are protected, and the implementation remains isolated from AI transport, semantic memory and existing villager editing workflows.
