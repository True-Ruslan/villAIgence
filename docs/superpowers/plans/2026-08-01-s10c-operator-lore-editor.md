# S10c Operator Lore Editor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a dedicated operator-only Minecraft client screen for reading and editing WORLD, PLAYER, VILLAGER and VILLAGE operator lore through the existing S10b server-authoritative API.

**Architecture:** Add a correlation-only `requestId` to the existing bounded S10b payloads, keep all editor transitions in a pure Java model, and use a thin client controller to send requests and dispatch responses to one active screen. The client never resolves persistent identities, never accesses `operator-lore.json`, and never weakens permission, target, revision or payload checks.

**Tech Stack:** Java 21, Minecraft 1.21.1, MCA-derived common module, Fabric, NeoForge, Brigadier, Mojang custom payload codecs, JUnit 5, Gradle, GitHub Actions.

## Global Constraints

- Implementation base is `1.21.1` at or after the merged S10c design commit `3c52f891be850a8b1973057e1345cda7ddb28032`.
- Approved design: `docs/superpowers/specs/2026-08-01-s10c-operator-lore-editor-design.md`.
- Permission level remains `ServerPlayer.hasPermissions(2)` on open, read and write.
- Client requests may contain only `requestId`, scope, runtime villager entity ID, expected revision and value.
- Client requests must not contain arbitrary player UUID, villager UUID, dimension or village ID.
- WORLD and PLAYER work without a target; VILLAGER and VILLAGE require one server-selected runtime entity ID.
- The server remains authoritative for permission, target resolution, distance, home village, payload, revision and persistence.
- Maximum value remains 4096 Unicode code points and 12288 UTF-8 bytes.
- `operator-lore.json` schema v1 remains unchanged.
- Operator lore remains separate from `worldFacts`, episodic memory and semantic memory.
- Do not change AI provider, parser, retry, STT, TTS, dependencies, workflows, scripts or generated resources.
- Client screen classes must not be referenced from dedicated-server initialization paths.
- Every production package follows RED → minimal GREEN → focused tests → Fabric/NeoForge/security gate → focused commit.
- Real-server UI acceptance is accumulated into the final S1–S10c release-candidate test and is not an isolated merge blocker.

---

## File and ownership map

### Existing files to modify

```text
common/src/main/java/net/conczin/mca/network/c2s/OperatorLoreReadRequest.java
common/src/main/java/net/conczin/mca/network/c2s/OperatorLoreWriteRequest.java
common/src/main/java/net/conczin/mca/network/s2c/OperatorLoreResponse.java
common/src/main/java/net/conczin/mca/network/ClientHandler.java
common/src/main/java/net/conczin/mca/network/ClientHandlerImpl.java
common/src/main/java/net/conczin/mca/network/s2c/OpenGuiRequest.java
common/src/main/java/net/conczin/mca/server/command/Command.java
common/src/main/resources/assets/mca/lang/en_us.json
```

### New production files

```text
common/src/main/java/net/conczin/mca/livingworld/lore/editor/OperatorLoreEditorModel.java
common/src/main/java/net/conczin/mca/livingworld/lore/editor/OperatorLoreEditorOpenPolicy.java
common/src/main/java/net/conczin/mca/client/gui/lore/OperatorLoreEditorOpenContext.java
common/src/main/java/net/conczin/mca/client/gui/lore/OperatorLoreEditorController.java
common/src/main/java/net/conczin/mca/client/gui/lore/OperatorLoreEditorScreen.java
```

### File to remove after direct dispatch is active

```text
common/src/main/java/net/conczin/mca/livingworld/lore/editor/OperatorLoreClientState.java
```

### New tests and evidence

```text
common/src/test/java/net/conczin/mca/livingworld/lore/editor/OperatorLoreProtocolContractTest.java
common/src/test/java/net/conczin/mca/livingworld/lore/editor/OperatorLoreEditorModelTest.java
common/src/test/java/net/conczin/mca/livingworld/lore/editor/OperatorLoreEditorOpenPolicyTest.java
docs/livingworld/VALIDATION_UPSTREAM_S10C_OPERATOR_LORE_EDITOR.md
```

The model stays beside existing lore policy classes because it is pure Java. Controller and screen stay under a client GUI package and are reached only through `ClientHandlerImpl`.

---

### Task 0: Establish the implementation baseline

**Files:**
- Read: `docs/PROJECT_STATE.md`
- Read: `docs/ROADMAP.md`
- Read: `docs/CHANGELOG.md`
- Read: `docs/superpowers/specs/2026-08-01-s10c-operator-lore-editor-design.md`
- Read: `docs/livingworld/VALIDATION_UPSTREAM_S10B_OPERATOR_LORE_AUTHORITY.md`

**Interfaces:**
- Consumes: merged S9/S10a/S10b state and approved S10c design.
- Produces: one clean implementation branch and recorded audit baseline.

- [ ] **Step 1: Update the base**

```bash
git fetch origin
git switch 1.21.1
git pull --ff-only origin 1.21.1
git rev-parse HEAD
git status --short
```

Expected: current canonical HEAD, clean working tree.

- [ ] **Step 2: Run the baseline gate**

```bash
./gradlew :common:test :fabric:build :neoforge:build --no-daemon
python3 scripts/ci/repository_security_policy.py --check
```

Expected: all commands exit `0` before S10c runtime work.

- [ ] **Step 3: Create the focused branch**

```bash
git switch -c agent/upstream-s10c-operator-lore-editor
```

- [ ] **Step 4: Open a draft PR with this audit block**

```text
VillAIgence base: <exact 1.21.1 SHA>
S9 merge:         f4a0d369c6e787d9ed91501fc87323aded9b4cbc
S10a merge:       b700e14636e3370774173978b0b2519941dafed0
S10b merge:       3cc3137b09629ab348cf0d3e79c821a524259d56
S10c design:      docs/superpowers/specs/2026-08-01-s10c-operator-lore-editor-design.md
Persistence schema changed: no
AI/provider paths changed: no
Live acceptance: accumulated into final S1-S10c candidate
```

---

### Task 1: Add correlation-only request IDs

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/network/c2s/OperatorLoreReadRequest.java`
- Modify: `common/src/main/java/net/conczin/mca/network/c2s/OperatorLoreWriteRequest.java`
- Modify: `common/src/main/java/net/conczin/mca/network/s2c/OperatorLoreResponse.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/lore/editor/OperatorLoreProtocolContractTest.java`

**Interfaces:**
- Consumes unchanged authority methods:

```java
OperatorLoreServerAuthority.read(ServerPlayer, String, int)
OperatorLoreServerAuthority.write(ServerPlayer, String, int, String, String)
```

- Produces:

```java
record OperatorLoreReadRequest(int requestId, String scope, int villagerEntityId)
record OperatorLoreWriteRequest(int requestId, String scope, int villagerEntityId, String expectedRevision, String value)
record OperatorLoreResponse(int requestId, String scope, int villagerEntityId, String status, String value, String revision)
OperatorLoreResponse(int requestId, OperatorLoreEditorResult result)
```

`requestId` is echoed only and has no authority meaning.

- [ ] **Step 1: Write the failing contract test**

```java
@Test
void requestIdIsPreservedAcrossReadWriteAndResponseModels() {
    OperatorLoreReadRequest read = new OperatorLoreReadRequest(41, "WORLD", -1);
    OperatorLoreWriteRequest write = new OperatorLoreWriteRequest(42, "PLAYER", -1, "a".repeat(64), "Lore");
    OperatorLoreEditorResult result = new OperatorLoreEditorResult(
            OperatorLoreScope.WORLD,
            -1,
            OperatorLoreEditorResult.Status.OK,
            "Lore",
            OperatorLoreEditorPolicy.revision("Lore")
    );
    OperatorLoreResponse response = new OperatorLoreResponse(43, result);

    assertEquals(41, read.requestId());
    assertEquals(42, write.requestId());
    assertEquals(43, response.requestId());
    assertEquals(result, response.toResult());
}
```

- [ ] **Step 2: Run RED**

```bash
./gradlew :common:test --tests '*OperatorLoreProtocolContractTest*' --no-daemon
```

Expected: `compileTestJava` fails because the request-ID constructors/accessors do not exist.

- [ ] **Step 3: Extend the read request**

```java
public record OperatorLoreReadRequest(
        int requestId,
        String scope,
        int villagerEntityId
) implements HandleablePayload {
    public static final StreamCodec<FriendlyByteBuf, OperatorLoreReadRequest> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, OperatorLoreReadRequest::requestId,
            ByteBufCodecs.stringUtf8(16), OperatorLoreReadRequest::scope,
            ByteBufCodecs.INT, OperatorLoreReadRequest::villagerEntityId,
            OperatorLoreReadRequest::new
    );

    @Override
    public void handleServer(ServerPlayer player) {
        OperatorLoreEditorResult result = OperatorLoreServerAuthority.read(player, scope, villagerEntityId);
        Network.sendToPlayer(new OperatorLoreResponse(requestId, result), player);
    }
}
```

- [ ] **Step 4: Extend the write request**

Codec order:

```java
ByteBufCodecs.INT, OperatorLoreWriteRequest::requestId,
ByteBufCodecs.stringUtf8(16), OperatorLoreWriteRequest::scope,
ByteBufCodecs.INT, OperatorLoreWriteRequest::villagerEntityId,
ByteBufCodecs.stringUtf8(OperatorLoreEditorPolicy.REVISION_HEX_LENGTH), OperatorLoreWriteRequest::expectedRevision,
ByteBufCodecs.stringUtf8(OperatorLoreEditorPolicy.MAX_CODE_POINTS), OperatorLoreWriteRequest::value
```

The handler calls the unchanged authority method and returns `new OperatorLoreResponse(requestId, result)`.

- [ ] **Step 5: Extend the response**

Add `requestId` as the first record component and codec field:

```java
public OperatorLoreResponse(int requestId, OperatorLoreEditorResult result) {
    this(requestId, result.scope().name(), result.villagerEntityId(), result.status().name(), result.value(), result.revision());
}
```

- [ ] **Step 6: Run focused GREEN**

```bash
./gradlew :common:test \
  --tests '*OperatorLoreProtocolContractTest*' \
  --tests '*OperatorLoreEditorPolicyTest*' \
  --tests '*OperatorLoreTargetPolicyTest*' \
  --no-daemon
```

- [ ] **Step 7: Run loader builds and commit**

```bash
./gradlew :fabric:build :neoforge:build --no-daemon
git add common/src/main/java/net/conczin/mca/network/c2s/OperatorLoreReadRequest.java \
        common/src/main/java/net/conczin/mca/network/c2s/OperatorLoreWriteRequest.java \
        common/src/main/java/net/conczin/mca/network/s2c/OperatorLoreResponse.java \
        common/src/test/java/net/conczin/mca/livingworld/lore/editor/OperatorLoreProtocolContractTest.java
git commit -m "feat: correlate operator lore requests"
```

---

### Task 2: Implement the pure editor model

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/lore/editor/OperatorLoreEditorModel.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/lore/editor/OperatorLoreEditorModelTest.java`

**Interfaces:**

```java
public final class OperatorLoreEditorModel {
    public enum State { IDLE, LOADING, LOADED, DIRTY, SAVING, CONFLICT, FORBIDDEN, NOT_FOUND, INVALID, ERROR, CLOSED }
    public record LoadCommand(int requestId, OperatorLoreScope scope, int villagerEntityId) {}
    public record SaveCommand(int requestId, OperatorLoreScope scope, int villagerEntityId, String revision, String value) {}

    public static OperatorLoreEditorModel open(int villagerEntityId)
    public LoadCommand beginLoad(OperatorLoreScope scope)
    public Optional<SaveCommand> beginSave()
    public void edit(String value)
    public void clear()
    public boolean applyResponse(int requestId, OperatorLoreEditorResult result)
    public void useServerVersion()
    public void keepDraft()
    public void close()
    public boolean isScopeAvailable(OperatorLoreScope scope)
    public boolean isDirty()
    public boolean isPayloadValid()
    public boolean canSave()
    public int codePointCount()
    public int utf8ByteCount()
}
```

- [ ] **Step 1: Write RED opening tests**

```java
@Test
void targetlessEditorStartsOnWorldAndDisablesTargetScopes() {
    OperatorLoreEditorModel model = OperatorLoreEditorModel.open(-1);
    assertEquals(OperatorLoreScope.WORLD, model.scope());
    assertTrue(model.isScopeAvailable(OperatorLoreScope.WORLD));
    assertTrue(model.isScopeAvailable(OperatorLoreScope.PLAYER));
    assertFalse(model.isScopeAvailable(OperatorLoreScope.VILLAGER));
    assertFalse(model.isScopeAvailable(OperatorLoreScope.VILLAGE));
}

@Test
void targetedEditorStartsOnVillagerAndEnablesAllScopes() {
    OperatorLoreEditorModel model = OperatorLoreEditorModel.open(77);
    assertEquals(OperatorLoreScope.VILLAGER, model.scope());
    for (OperatorLoreScope scope : OperatorLoreScope.values()) {
        assertTrue(model.isScopeAvailable(scope));
    }
}
```

- [ ] **Step 2: Write RED loading/editing/limit tests**

Cover exactly:

```text
beginLoad increments request ID and enters LOADING
matching OK enters LOADED and adopts value/revision
different canonical value enters DIRTY
CRLF and LF compare equal after canonicalization
Clear produces a DIRTY empty value
4096 ASCII code points valid
4097 code points invalid
4096 four-byte emoji exceed 12288 bytes and are invalid
NUL/control content invalid
Save enabled only while DIRTY and valid
```

- [ ] **Step 3: Write stale-response and close tests**

```java
@Test
void staleOrMismatchedResponsesAreIgnored() {
    OperatorLoreEditorModel model = OperatorLoreEditorModel.open(-1);
    OperatorLoreEditorModel.LoadCommand current = model.beginLoad(OperatorLoreScope.WORLD);
    OperatorLoreEditorResult ok = result(OperatorLoreScope.WORLD, -1, OK, "server");

    assertFalse(model.applyResponse(current.requestId() - 1, ok));
    assertEquals(OperatorLoreEditorModel.State.LOADING, model.state());
    assertFalse(model.applyResponse(current.requestId(), result(OperatorLoreScope.PLAYER, -1, OK, "wrong")));

    model.close();
    assertFalse(model.applyResponse(current.requestId(), ok));
    assertEquals(OperatorLoreEditorModel.State.CLOSED, model.state());
}
```

- [ ] **Step 4: Write conflict tests**

Prove:

```text
CONFLICT preserves local draft and returned server value/revision
Use server version -> LOADED with returned canonical value
Keep my draft -> DIRTY with returned server base/revision and local draft preserved
OK and UNCHANGED -> LOADED
FORBIDDEN, NOT_FOUND, INVALID and ERROR -> matching explicit states
```

- [ ] **Step 5: Run RED**

```bash
./gradlew :common:test --tests '*OperatorLoreEditorModelTest*' --no-daemon
```

- [ ] **Step 6: Implement minimal transitions**

Use a monotonic positive request counter:

```java
private int nextRequestId() {
    requestGeneration = requestGeneration == Integer.MAX_VALUE ? 1 : requestGeneration + 1;
    return requestGeneration;
}
```

A response applies only when:

```java
state != State.CLOSED
requestId == requestGeneration
result.scope() == scope
result.villagerEntityId() == expectedEntityIdForScope()
```

WORLD and PLAYER expect `-1`; VILLAGER and VILLAGE expect the opening entity ID.

Dirty comparison uses `OperatorLoreEditorPolicy.canonicalize` on both values.

- [ ] **Step 7: Run GREEN and commit**

```bash
./gradlew :common:test \
  --tests '*OperatorLoreEditorModelTest*' \
  --tests '*OperatorLoreEditorPolicyTest*' \
  --no-daemon
git add common/src/main/java/net/conczin/mca/livingworld/lore/editor/OperatorLoreEditorModel.java \
        common/src/test/java/net/conczin/mca/livingworld/lore/editor/OperatorLoreEditorModelTest.java
git commit -m "feat: add operator lore editor state model"
```

---

### Task 3: Replace the mailbox with direct active-screen dispatch

**Files:**
- Create: `common/src/main/java/net/conczin/mca/client/gui/lore/OperatorLoreEditorOpenContext.java`
- Create: `common/src/main/java/net/conczin/mca/client/gui/lore/OperatorLoreEditorController.java`
- Create compile shell: `common/src/main/java/net/conczin/mca/client/gui/lore/OperatorLoreEditorScreen.java`
- Modify: `common/src/main/java/net/conczin/mca/network/ClientHandler.java`
- Modify: `common/src/main/java/net/conczin/mca/network/ClientHandlerImpl.java`
- Modify: `common/src/main/java/net/conczin/mca/network/s2c/OperatorLoreResponse.java`
- Delete: `common/src/main/java/net/conczin/mca/livingworld/lore/editor/OperatorLoreClientState.java`

**Interfaces:**

```java
public record OperatorLoreEditorOpenContext(int villagerEntityId) {
    public boolean hasVillagerTarget() { return villagerEntityId >= 0; }
}

public final class OperatorLoreEditorController {
    public OperatorLoreEditorController(OperatorLoreEditorModel model)
    public void load(OperatorLoreScope scope)
    public void save()
    public void accept(OperatorLoreResponse response)
    public OperatorLoreEditorModel model()
}

void ClientHandler.handleOperatorLoreResponse(OperatorLoreResponse response)
```

- [ ] **Step 1: Create the compile RED**

Change `OperatorLoreResponse.handle` to:

```java
@Override
public void handle(Player player) {
    ClientProxy.getNetworkHandler().handleOperatorLoreResponse(this);
}
```

Run:

```bash
./gradlew :common:compileJava --no-daemon
```

Expected: missing `handleOperatorLoreResponse`.

- [ ] **Step 2: Add the ClientHandler method**

```java
void handleOperatorLoreResponse(OperatorLoreResponse response);
```

- [ ] **Step 3: Implement controller sends**

```java
public void load(OperatorLoreScope scope) {
    OperatorLoreEditorModel.LoadCommand command = model.beginLoad(scope);
    Network.sendToServer(new OperatorLoreReadRequest(
            command.requestId(), command.scope().name(), command.villagerEntityId()
    ));
}

public void save() {
    model.beginSave().ifPresent(command -> Network.sendToServer(new OperatorLoreWriteRequest(
            command.requestId(), command.scope().name(), command.villagerEntityId(),
            command.revision(), command.value()
    )));
}

public void accept(OperatorLoreResponse response) {
    model.applyResponse(response.requestId(), response.toResult());
}
```

- [ ] **Step 4: Dispatch only to the active screen**

```java
@Override
public void handleOperatorLoreResponse(OperatorLoreResponse response) {
    Screen screen = client.screen;
    if (screen instanceof OperatorLoreEditorScreen editor) {
        editor.accept(response);
    }
}
```

The temporary screen shell must expose constructor, `accept`, and `isPauseScreen`; Task 5 replaces it before merge.

- [ ] **Step 5: Remove the mailbox**

```bash
git rm common/src/main/java/net/conczin/mca/livingworld/lore/editor/OperatorLoreClientState.java
git grep -n "OperatorLoreClientState" -- ':!docs'
```

Expected: no remaining reference.

- [ ] **Step 6: Run builds and commit**

```bash
./gradlew :common:test :fabric:build :neoforge:build --no-daemon
git add common/src/main/java/net/conczin/mca/client/gui/lore \
        common/src/main/java/net/conczin/mca/network/ClientHandler.java \
        common/src/main/java/net/conczin/mca/network/ClientHandlerImpl.java \
        common/src/main/java/net/conczin/mca/network/s2c/OperatorLoreResponse.java
git add -u
git commit -m "feat: dispatch operator lore responses to the active editor"
```

---

### Task 4: Add server-authoritative opening commands

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/lore/editor/OperatorLoreEditorOpenPolicy.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/lore/editor/OperatorLoreEditorOpenPolicyTest.java`
- Modify: `common/src/main/java/net/conczin/mca/network/s2c/OpenGuiRequest.java`
- Modify: `common/src/main/java/net/conczin/mca/server/command/Command.java`

**Interfaces:**

```java
public static boolean canOpenGlobal(boolean hasPermission)
public static boolean canOpenTargeted(boolean hasPermission, boolean villagerPresent, boolean sameLevel, double distanceSquared)
```

Commands:

```text
/mca lore editor
/mca lore editor <target>
```

- [ ] **Step 1: Write RED policy tests**

```java
@Test
void globalOpeningRequiresOperatorPermission() {
    assertFalse(OperatorLoreEditorOpenPolicy.canOpenGlobal(false));
    assertTrue(OperatorLoreEditorOpenPolicy.canOpenGlobal(true));
}

@Test
void targetedOpeningRequiresLiveSameLevelNearbyVillager() {
    assertFalse(OperatorLoreEditorOpenPolicy.canOpenTargeted(true, false, false, Double.POSITIVE_INFINITY));
    assertFalse(OperatorLoreEditorOpenPolicy.canOpenTargeted(true, true, false, 1.0));
    assertFalse(OperatorLoreEditorOpenPolicy.canOpenTargeted(true, true, true, OperatorLoreTargetPolicy.MAX_DISTANCE_SQUARED + 1));
    assertTrue(OperatorLoreEditorOpenPolicy.canOpenTargeted(true, true, true, OperatorLoreTargetPolicy.MAX_DISTANCE_SQUARED));
}
```

- [ ] **Step 2: Run RED**

```bash
./gradlew :common:test --tests '*OperatorLoreEditorOpenPolicyTest*' --no-daemon
```

- [ ] **Step 3: Implement policy**

```java
return hasPermission
        && villagerPresent
        && sameLevel
        && Double.isFinite(distanceSquared)
        && distanceSquared >= 0.0
        && distanceSquared <= OperatorLoreTargetPolicy.MAX_DISTANCE_SQUARED;
```

- [ ] **Step 4: Append the GUI enum value**

Preserve all existing ordinals:

```java
COMB,
CLOSE,
OPERATOR_LORE_EDITOR,
```

- [ ] **Step 5: Register the command tree**

```java
.then(Commands.literal("lore")
        .requires(source -> source.hasPermission(2))
        .then(Commands.literal("editor")
                .executes(Command::openGlobalLoreEditor)
                .then(Commands.argument("target", EntityArgument.entity())
                        .executes(Command::openTargetedLoreEditor))))
```

- [ ] **Step 6: Implement global opening**

```java
ServerPlayer player = ctx.getSource().getPlayerOrException();
Network.sendToPlayer(new OpenGuiRequest(OpenGuiRequest.Type.OPERATOR_LORE_EDITOR), player);
return 1;
```

- [ ] **Step 7: Implement exact targeted opening**

Resolve only `EntityArgument.getEntity(ctx, "target")`. Require `VillagerEntityMCA`, alive/not removed, same level and within `OperatorLoreTargetPolicy.MAX_DISTANCE_SQUARED`. Never substitute a nearest entity. On failure send `command.lore_editor.invalid_target`; on success send `new OpenGuiRequest(Type.OPERATOR_LORE_EDITOR, villager)`.

- [ ] **Step 8: Run tests/builds and commit**

```bash
./gradlew :common:test \
  --tests '*OperatorLoreEditorOpenPolicyTest*' \
  --tests '*OperatorLoreTargetPolicyTest*' \
  --no-daemon
./gradlew :fabric:build :neoforge:build --no-daemon
git add common/src/main/java/net/conczin/mca/livingworld/lore/editor/OperatorLoreEditorOpenPolicy.java \
        common/src/test/java/net/conczin/mca/livingworld/lore/editor/OperatorLoreEditorOpenPolicyTest.java \
        common/src/main/java/net/conczin/mca/network/s2c/OpenGuiRequest.java \
        common/src/main/java/net/conczin/mca/server/command/Command.java
git commit -m "feat: add operator lore editor commands"
```

---

### Task 5: Implement the responsive editor screen

**Files:**
- Replace shell: `common/src/main/java/net/conczin/mca/client/gui/lore/OperatorLoreEditorScreen.java`
- Modify: `common/src/main/java/net/conczin/mca/network/ClientHandlerImpl.java`

**Interfaces:**

```java
public final class OperatorLoreEditorScreen extends Screen {
    public OperatorLoreEditorScreen(OperatorLoreEditorOpenContext context)
    public void accept(OperatorLoreResponse response)
}
```

Use:

```text
OperatorLoreEditorModel
OperatorLoreEditorController
MultiLineEditBox
ButtonWidget
ConfirmScreen
```

- [ ] **Step 1: Open the screen from `ClientHandlerImpl`**

```java
case OPERATOR_LORE_EDITOR:
    client.setScreen(new OperatorLoreEditorScreen(
            new OperatorLoreEditorOpenContext(message.villager())
    ));
    break;
```

Use `-1` as targetless sentinel. Do not validate the target client-side.

- [ ] **Step 2: Initialize model/controller and initial load**

```java
public OperatorLoreEditorScreen(OperatorLoreEditorOpenContext context) {
    super(Component.translatable("gui.operator_lore.title"));
    this.model = OperatorLoreEditorModel.open(context.villagerEntityId());
    this.controller = new OperatorLoreEditorController(model);
}

@Override
protected void init() {
    rebuildWidgets();
    if (model.state() == OperatorLoreEditorModel.State.IDLE) {
        controller.load(model.scope());
    }
}
```

Re-init after resize must preserve model value and focus.

- [ ] **Step 3: Implement responsive panel bounds**

```java
int margin = 16;
int panelWidth = Math.max(320, Math.min(720, width - margin * 2));
if (width < 352) panelWidth = width - 8;
int panelHeight = Math.max(220, height - margin * 2);
int left = width < 352 ? 4 : (width - panelWidth) / 2;
int top = Math.max(8, (height - panelHeight) / 2);
int contentWidth = panelWidth - 24;
int textTop = top + 72;
int footerHeight = 72;
int textHeight = Math.max(72, panelHeight - 72 - footerHeight);
```

No control may leave the viewport.

- [ ] **Step 4: Add four stable scope buttons**

Order:

```text
WORLD, PLAYER, VILLAGER, VILLAGE
```

Disable VILLAGER/VILLAGE when no target. Dirty scope change opens a `ConfirmScreen`; confirmed change calls `controller.load(requestedScope)`.

- [ ] **Step 5: Add the multiline editor**

Use Minecraft 1.21.1 `MultiLineEditBox` with:

```text
initial value = model.workingValue()
UI safety ceiling = 16384 UTF-16 code units
value listener = model.edit(value), then refresh controls/counters
editable states = LOADED, DIRTY, INVALID, ERROR, CONFLICT
```

The UI ceiling intentionally exceeds server limits so users can correct invalid content. Save stays governed by `model.isPayloadValid()`.

- [ ] **Step 6: Add Reload, Clear, Save and Close**

```text
Reload -> direct load if clean; dirty confirmation otherwise
Clear -> model.clear(), update widget, no network request
Save -> controller.save()
Close -> onClose(), never implicit write
```

Button state:

```java
saveButton.active = model.canSave();
reloadButton.active = model.state() != SAVING && model.state() != CLOSED;
clearButton.active = model.state() != LOADING && model.state() != SAVING && model.state() != CLOSED;
```

- [ ] **Step 7: Add conflict controls**

In `CONFLICT`, show:

```text
Use server version
Keep my draft
Close
```

`Use server version` calls `model.useServerVersion()`. `Keep my draft` calls `model.keepDraft()` and does not save automatically.

- [ ] **Step 8: Render labels, counters and statuses**

Always render title, selected scope, target label, code-point counter, UTF-8 counter and status. Exhaustive status keys:

```text
gui.operator_lore.status.idle
gui.operator_lore.status.loading
gui.operator_lore.status.loaded
gui.operator_lore.status.dirty
gui.operator_lore.status.saving
gui.operator_lore.status.conflict
gui.operator_lore.status.forbidden
gui.operator_lore.status.not_found
gui.operator_lore.status.invalid
gui.operator_lore.status.error
gui.operator_lore.status.closed
```

Do not render response values in error messages or logs.

- [ ] **Step 9: Implement keyboard and close behavior**

```java
if (hasControlDown() && keyCode == GLFW.GLFW_KEY_S && model.canSave()) {
    controller.save();
    return true;
}
if (hasControlDown() && keyCode == GLFW.GLFW_KEY_R) {
    requestReload();
    return true;
}
```

Close behavior:

```text
clean -> close immediately
dirty -> discard confirmation
saving -> warning that in-flight result will be ignored
```

`isPauseScreen()` returns `false`.

- [ ] **Step 10: Accept responses only through controller**

```java
public void accept(OperatorLoreResponse response) {
    controller.accept(response);
    syncWidgetFromModelWhenCanonicalStateChanged();
    refreshControls();
}
```

- [ ] **Step 11: Run builds/package check and commit**

```bash
./gradlew :common:test :fabric:build :neoforge:build --no-daemon
jar tf fabric/build/libs/*.jar | grep 'OperatorLoreEditorScreen.class'
git add common/src/main/java/net/conczin/mca/client/gui/lore/OperatorLoreEditorScreen.java \
        common/src/main/java/net/conczin/mca/network/ClientHandlerImpl.java
git commit -m "feat: add operator lore editor screen"
```

---

### Task 6: Add localization and narration

**Files:**
- Modify: `common/src/main/resources/assets/mca/lang/en_us.json`
- Modify as needed: `common/src/main/java/net/conczin/mca/client/gui/lore/OperatorLoreEditorScreen.java`
- Modify: `common/src/main/java/net/conczin/mca/server/command/Command.java`

**Interfaces:**
- Produces translation keys for every visible label, button, state, confirmation and command error.

- [ ] **Step 1: Add exact key families**

```text
gui.operator_lore.title
gui.operator_lore.scope.world|player|villager|village
gui.operator_lore.target.world|player|villager|village
gui.operator_lore.disabled.no_target
gui.operator_lore.button.reload|clear|save|close|use_server|keep_draft
gui.operator_lore.status.idle|loading|loaded|dirty|saving|conflict|forbidden|not_found|invalid|error|closed
gui.operator_lore.counter.code_points
gui.operator_lore.counter.utf8
gui.operator_lore.discard_title
gui.operator_lore.discard_scope_message
gui.operator_lore.discard_reload_message
gui.operator_lore.discard_close_message
gui.operator_lore.saving_close_message
command.lore_editor.invalid_target
```

Required English wording follows the approved spec; do not replace unrelated keys.

- [ ] **Step 2: Add narration**

Disabled target tabs narrate `gui.operator_lore.disabled.no_target`. Counters and current status must be included in screen/widget narration; state must not be communicated by color alone.

- [ ] **Step 3: Validate resources**

```bash
python3 -m json.tool common/src/main/resources/assets/mca/lang/en_us.json >/dev/null
./gradlew :fabric:processResources :neoforge:processResources --no-daemon
```

- [ ] **Step 4: Commit localization**

```bash
git add common/src/main/resources/assets/mca/lang/en_us.json \
        common/src/main/java/net/conczin/mca/client/gui/lore/OperatorLoreEditorScreen.java \
        common/src/main/java/net/conczin/mca/server/command/Command.java
git commit -m "feat: localize operator lore editor"
```

---

### Task 7: Produce automated and live-acceptance evidence

**Files:**
- Create: `docs/livingworld/VALIDATION_UPSTREAM_S10C_OPERATOR_LORE_EDITOR.md`
- Update: `docs/CHANGELOG.md`
- Update `docs/PROJECT_STATE.md` only if status materially changes.

- [ ] **Step 1: Run focused tests**

```bash
./gradlew :common:test \
  --tests '*OperatorLoreProtocolContractTest*' \
  --tests '*OperatorLoreEditorModelTest*' \
  --tests '*OperatorLoreEditorOpenPolicyTest*' \
  --tests '*OperatorLoreEditorPolicyTest*' \
  --tests '*OperatorLoreTargetPolicyTest*' \
  --no-daemon
```

- [ ] **Step 2: Run complete gate**

```bash
./gradlew :common:test :fabric:build :neoforge:build --stacktrace --no-daemon
python3 scripts/ci/repository_security_policy.py --check
```

- [ ] **Step 3: Verify package and server isolation**

```bash
FABRIC_JAR=$(find fabric/build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-sources.jar' | sort | tail -n 1)
jar tf "$FABRIC_JAR" | grep -E 'OperatorLore(EditorScreen|EditorController|ReadRequest|WriteRequest|Response)\.class'
git grep -n 'OperatorLoreEditorScreen' -- \
  'common/src/main/java/net/conczin/mca/**' \
  ':!common/src/main/java/net/conczin/mca/network/ClientHandlerImpl.java' \
  ':!common/src/main/java/net/conczin/mca/client/**'
```

Expected: classes packaged; no common/server initialization reference.

- [ ] **Step 4: Verify protected paths remain unchanged**

```bash
git diff origin/1.21.1...HEAD -- \
  common/src/main/java/net/conczin/mca/entity/ai/chatAI \
  common/src/main/java/net/conczin/mca/livingworld/context \
  common/src/main/java/net/conczin/mca/livingworld/memory \
  common/src/main/java/net/conczin/mca/livingworld/semantic \
  .github scripts gradle
```

Expected: empty diff.

- [ ] **Step 5: Write evidence document**

Record exact base, RED head/failure, GREEN head, three workflow IDs/results, changed files, protocol correlation, permission/target boundary, limits, mailbox removal, classloading boundary and pending live matrix.

Required cumulative live matrix:

```text
operator global opening
operator targeted opening
non-operator denied
WORLD/PLAYER/VILLAGER/VILLAGE read-edit-save-reopen
no-home VILLAGE -> NOT_FOUND
beyond 64 blocks -> NOT_FOUND
4096 code-point and UTF-8 boundaries
invalid control character
Clear then Save
Reload clean and dirty confirmation
scope-switch dirty confirmation
close dirty confirmation
second-editor conflict
Use server version
Keep my draft then deliberate Save
disconnect during load/save
GUI scale and resize
restart persistence/hash
no semantic-memory mutation
Text/STT/Chat/TTS smoke
final persistent-file hash comparison
```

- [ ] **Step 6: Update changelog without live claims**

```text
S10c implementation: complete after merge
S10c automated verification: PASS at exact head
S1-S10c cumulative installed-server acceptance: PENDING
release promotion: not claimed
```

- [ ] **Step 7: Commit evidence**

```bash
git add docs/livingworld/VALIDATION_UPSTREAM_S10C_OPERATOR_LORE_EDITOR.md docs/CHANGELOG.md
git add docs/PROJECT_STATE.md 2>/dev/null || true
git commit -m "docs: record S10c automated validation"
```

---

### Task 8: Final PR gate and merge

- [ ] **Step 1: Review changed-file scope**

```bash
git diff --stat origin/1.21.1...HEAD
git diff --name-only origin/1.21.1...HEAD
```

Allowed areas only:

```text
operator lore editor model/policies/tests
operator lore payloads
ClientHandler/ClientHandlerImpl
OpenGuiRequest
Command
client GUI lore package
en_us.json
S10c validation/changelog docs
```

- [ ] **Step 2: Search for forbidden client authority**

```bash
git grep -nE 'UUID|dimension|villageId' -- \
  common/src/main/java/net/conczin/mca/network/c2s/OperatorLoreReadRequest.java \
  common/src/main/java/net/conczin/mca/network/c2s/OperatorLoreWriteRequest.java

git grep -nE 'operator-lore\.json|WorldOperatorLoreStore' -- common/src/main/java/net/conczin/mca/client
```

Expected: no arbitrary identity fields and no client persistence access.

- [ ] **Step 3: Complete PR body**

Include user-visible behavior, authority/persistence boundaries, canonical RED, final GREEN, exact workflows, changed-file scope, pending manual acceptance and rollback statement.

- [ ] **Step 4: Require exact-head workflows**

```text
VillAIgence CI                        SUCCESS
Java Pull Request CI with Gradle     SUCCESS
Repository security policy           SUCCESS
```

Do not merge pending, failed or stale-head runs.

- [ ] **Step 5: Mark ready and squash-merge**

```bash
gh pr ready <number>
gh pr merge <number> --squash --delete-branch
```

Record merge SHA in the next canonical-state reconciliation.

---

## Plan self-review result

### Spec coverage

```text
request correlation                         Task 1
pure explicit state machine                 Task 2
direct active-screen dispatch               Task 3
operator-only global/target opening         Task 4
four scopes and responsive editor           Task 5
dirty/reload/close/conflict behavior        Task 5
code-point/UTF-8 validation                 Tasks 2 and 5
localization/narration                      Task 6
dedicated-server classloading boundary      Tasks 5 and 7
automated/security evidence                 Task 7
controlled merge gate                       Task 8
cumulative live acceptance                  Task 7
```

### Placeholder scan

No `TBD`, `TODO`, unnamed validation step or undefined interface remains.

### Type consistency

```java
OperatorLoreReadRequest(int requestId, String scope, int villagerEntityId)
OperatorLoreWriteRequest(int requestId, String scope, int villagerEntityId, String expectedRevision, String value)
OperatorLoreResponse(int requestId, String scope, int villagerEntityId, String status, String value, String revision)
OperatorLoreEditorModel.LoadCommand
OperatorLoreEditorModel.SaveCommand
ClientHandler.handleOperatorLoreResponse(OperatorLoreResponse)
```

### Scope result

One independently reviewable UI package. Release creation, live execution, legacy memory migration, generated personality, semantic ingestion and optional compatibility trains remain outside this plan.
