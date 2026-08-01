# S10c Operator Lore Editor Validation

## Status

```text
S10c implementation: complete on PR branch
S10c automated validation: PASS
S1-S10c cumulative installed-server acceptance: PENDING
release promotion: not claimed
```

This document records repository-side evidence for the dedicated operator-only client editor over the S9 persistence, S10a immutable AI context and S10b server-authoritative API.

## Baseline

```text
VillAIgence implementation base: 6abd841952e50ac74717073cef2f69061b6c095f
S9 merge:                     f4a0d369c6e787d9ed91501fc87323aded9b4cbc
S10a merge:                   b700e14636e3370774173978b0b2519941dafed0
S10b merge:                   3cc3137b09629ab348cf0d3e79c821a524259d56
S10c design merge:            3c52f891be850a8b1973057e1345cda7ddb28032
implementation PR:            #88
```

Approved design and executable plan:

```text
docs/superpowers/specs/2026-08-01-s10c-operator-lore-editor-design.md
docs/superpowers/plans/2026-08-01-s10c-operator-lore-editor.md
```

## Implemented behavior

### Dedicated screen

- standalone `OperatorLoreEditorScreen`, separate from Villager Editor;
- stable scope order: WORLD, PLAYER, VILLAGER, VILLAGE;
- targetless opening starts on WORLD and disables target scopes;
- targeted opening starts on VILLAGER and enables all scopes;
- responsive multiline editor with bounded presentation layout;
- Unicode code-point and UTF-8-byte counters;
- explicit IDLE, LOADING, LOADED, DIRTY, SAVING, CONFLICT, FORBIDDEN, NOT_FOUND, INVALID, ERROR and CLOSED states;
- Reload, Clear, Save and Close controls;
- dirty-state confirmation for scope change, reload and close;
- saving-close warning;
- Ctrl/Cmd+S and Ctrl/Cmd+R shortcuts that respect busy-state controls;
- explicit conflict choices: use server version or retain local draft;
- no implicit write on close, reload, clear or conflict;
- confirmation modals continue forwarding in-flight lore responses to the owning editor.

### Pure state model

`OperatorLoreEditorModel` owns editor transitions without Minecraft, networking, rendering or filesystem dependencies.

Automated coverage proves:

- initial scope and scope availability;
- monotonic request generations;
- canonical CRLF/LF dirty comparison;
- Clear behavior;
- 4096-code-point boundary;
- 12288-byte UTF-8 boundary;
- forbidden-control rejection;
- Save eligibility and SAVING transition;
- stale request, scope and entity response rejection;
- late response rejection after close;
- OK and UNCHANGED canonical adoption;
- conflict preservation of local and server versions;
- explicit conflict-resolution transitions;
- explicit server failure states with local draft preservation.

### Request correlation

Read, write and response payloads now carry an integer `requestId` as their first codec field.

```text
client allocates positive generation
server echoes integer unchanged
client applies only current matching generation
request ID has no permission meaning
request ID has no target meaning
request ID has no revision meaning
request ID has no persistence meaning
```

### Server-authoritative opening

New commands:

```text
/mca lore editor
/mca lore editor <target>
```

Rules:

- command tree requires permission level 2;
- global opening uses target sentinel `-1`;
- targeted opening resolves exactly the selected entity;
- target must be a live MCA villager;
- target must remain in the same level;
- target must be within the existing S10b 64-block bound;
- no nearest-entity substitution or client-computed identity;
- every subsequent read and write repeats S10b authority checks.

`OPERATOR_LORE_EDITOR` was appended to `OpenGuiRequest.Type`; existing GUI ordinals remain unchanged.

### Client dispatch and classloading boundary

- `OperatorLoreResponse` dispatches through `ClientHandler`;
- an active editor or its client-only `OperatorLoreConfirmScreen` receives the response;
- the modal delegates the response to its owning editor, preventing a cancelled close-during-save flow from becoming permanently stuck in SAVING;
- the temporary global `OperatorLoreClientState` mailbox was removed;
- controller state is screen-local and in-memory only;
- screen/controller/modal classes live under the client GUI package;
- no common/server initialization path directly references the screen;
- both Fabric and NeoForge builds compile the client integration.

### Localization

Visible S10c strings are stored in the additive language resource:

```text
assets/villaigence/lang/en_us.json
```

Minecraft merges language resources across resource namespaces. The existing large `assets/mca/lang/en_us.json` is left byte-for-byte untouched.

## Authority and security boundary

Unchanged S10b authority:

```text
permission check              server
authenticated PLAYER UUID     server
villager UUID                 server
villager same-level/liveness  server
villager distance             server
village dimension and ID      server
payload validity              server
current revision              server
persistence                   server
```

C2S packets still contain no arbitrary:

```text
player UUID
villager UUID
dimension
village ID
filesystem path
JSON document
provider credential
```

Client code has no reference to `WorldOperatorLoreStore` or `operator-lore.json`.

S10c does not change:

```text
operator-lore.json schema
AI provider transport/parser/retry
immutable context capture
worldFacts precedence
Memory 2.0
Semantic Memory
relationships/events/voices persistence
STT/TTS
Gradle dependencies or locks
workflows or scripts
```

## TDD evidence

### Request-correlation RED

```text
head: d159267b23def8e12baa16021cc35a0c41ff5086
VillAIgence CI #1125 / 30707811447 — expected FAILURE
reason: OperatorLoreProtocolPolicy absent
Java Pull Request CI #627 / 30707811422 — SUCCESS
Repository security policy #450 / 30707811419 — SUCCESS
```

A provisional earlier test imported loader-bound payload classes into the pure common-test boundary and was rejected before production changes. The canonical RED above is loader-independent.

### Editor-model RED

```text
head: 0857097ae19c4eee983b3f5bfb658822fe2981ea
VillAIgence CI #1130 / 30708165162 — expected FAILURE
reason: OperatorLoreEditorModel absent
Java Pull Request CI #632 / 30708165164 — SUCCESS
Repository security policy #460 / 30708165182 — SUCCESS
```

### Direct-dispatch RED

```text
head: 4fc8c20fdf3c1dfdb169619444cb3fbf8c149864
VillAIgence CI #1132 / 30708485495 — expected FAILURE
Java Pull Request CI #634 / 30708485529 — expected FAILURE
reason: ClientHandler.handleOperatorLoreResponse absent
Repository security policy #464 / 30708485513 — SUCCESS
```

### Opening-policy RED

```text
head: 1c54dedc6b8a7cf749fba6fa63549da89e1eca85
VillAIgence CI #1139 / 30708795976 — expected FAILURE
reason: OperatorLoreEditorOpenPolicy absent
Java Pull Request CI #641 / 30708795986 — SUCCESS
Repository security policy #478 / 30708795981 — SUCCESS
```

## Initial implementation GREEN

```text
head: 1d383f66531d8cdf8518a387cdffc9a34fbeac74
VillAIgence CI #1144 / 30709370857              SUCCESS
Java Pull Request CI #646 / 30709370847       SUCCESS
Repository security policy #489 / 30709370842 SUCCESS
```

## Post-GREEN UI review hardening

Manual patch review found three presentation-only edge cases after the initial GREEN:

```text
enum declaration order differed from approved tab order
Ctrl/Cmd+R could bypass the disabled Reload button while busy
a vanilla ConfirmScreen temporarily replaced the active response receiver during close-while-saving
```

Corrections:

```text
explicit WORLD, PLAYER, VILLAGER, VILLAGE order
keyboard shortcuts require the corresponding active button
client-only response-receiving confirmation modal delegates in-flight responses to the editor
```

Final implementation GREEN:

```text
head: 20ffbaf4cf6a138767f1458cddfe8d014bb5c8ee
VillAIgence CI #1149 / 30709770608              SUCCESS
Java Pull Request CI #651 / 30709770646       SUCCESS
Repository security policy #499 / 30709770642 SUCCESS
```

Validated automatically at the final implementation head:

```text
common unit tests                          PASS
Fabric build                               PASS
NeoForge build                             PASS
Fabric distributable package verification PASS
repository security policy                PASS
```

## Changed runtime scope

```text
client/gui/lore/OperatorLoreConfirmScreen.java
client/gui/lore/OperatorLoreEditorController.java
client/gui/lore/OperatorLoreEditorOpenContext.java
client/gui/lore/OperatorLoreEditorScreen.java
client/gui/lore/OperatorLoreResponseReceiver.java
livingworld/lore/editor/OperatorLoreEditorModel.java
livingworld/lore/editor/OperatorLoreEditorOpenPolicy.java
livingworld/lore/editor/OperatorLoreProtocolPolicy.java
network/ClientHandler.java
network/ClientHandlerImpl.java
network/c2s/OperatorLoreReadRequest.java
network/c2s/OperatorLoreWriteRequest.java
network/s2c/OpenGuiRequest.java
network/s2c/OperatorLoreResponse.java
server/command/Command.java
assets/villaigence/lang/en_us.json
```

Removed:

```text
livingworld/lore/editor/OperatorLoreClientState.java
```

## Cumulative installed-server acceptance matrix

Run only on the backed-up final synchronized release candidate containing S1-S10c.

### Opening and authority

```text
operator global opening
operator targeted opening with nearby MCA villager
non-operator open denied
non-operator forged read denied
non-operator forged write denied
invalid non-villager target rejected
removed/dead target rejected
cross-level target rejected
beyond-64-block target rejected
```

### Scope persistence

```text
WORLD read/edit/save/reopen
PLAYER read/edit/save/reopen
VILLAGER read/edit/save/reopen
VILLAGE read/edit/save/reopen
villager without home village -> NOT_FOUND
restart and reopen all populated scopes
operator-lore.json deterministic persistence/hash
```

### Bounds and editor behavior

```text
4096 ASCII code-point boundary
4097 code-point rejection
UTF-8 byte boundary with emoji
invalid control character blocked
Clear then deliberate Save
Reload clean
Reload dirty confirmation
scope-switch dirty confirmation
close dirty confirmation
saving-close warning
response arrives while close confirmation is open
cancel close after successful in-flight response
GUI scale changes
window resize and narrow layout
keyboard Save/Reload
```

### Concurrency and failure

```text
two editors load same revision
editor A saves
editor B receives CONFLICT
Use server version
repeat conflict and Keep my draft
Keep my draft then deliberate Save
late response after scope change ignored
disconnect while loading
disconnect while saving
permission revoked while screen open
server persistence error remains fail-soft
```

### Regression and durability

```text
no semantic-memory mutation from operator lore writes
observed worldFacts still win prompt conflicts
Text dialogue smoke
Voice/STT smoke
Chat provider smoke
TTS smoke
S1-S8 synchronized behavior matrix
all world-local persistent files hash/restart comparison
final rollback-copy load
```

## Validation boundary

The screen is compiled and its pure state/authority contracts are automated, but no claim is made yet that mouse layout, narration, GUI-scale behavior or end-to-end network interaction has been observed inside a running Minecraft client.

```text
repository implementation complete   yes
automated validation PASS             yes
installed-client acceptance           pending
cumulative release acceptance         pending
release promotion                     no
```
