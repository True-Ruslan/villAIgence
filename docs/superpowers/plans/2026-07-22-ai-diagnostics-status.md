# VillAIgence AI Diagnostics Status Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a safe read-only `/villaigence ai status` command that reports non-secret configuration readiness and the latest runtime outcome for CHAT, STT and TTS.

**Architecture:** A pure common-module diagnostics registry stores only the latest immutable status per AI operation. Provider boundaries update that registry after logical operations complete. A safe configuration snapshot and pure formatter produce command output without ever receiving credential values or user/NPC content.

**Tech Stack:** Java 21, JUnit 5, Brigadier/Minecraft 1.21.1 command APIs, Fabric command registration, existing OpenAI-compatible chat/audio providers.

## Global Constraints

- Preserve `mod id: mca`.
- Preserve Java package root `net.conczin.mca`.
- Preserve `config/livingworld.json` and `<world>/livingworld/` paths.
- Do not perform network I/O from `/villaigence ai status`.
- Never expose API keys, Authorization headers, prompts, transcripts, NPC answers, reasoning text or raw provider payloads.
- Diagnostics writes must not alter provider/gameplay success or failure.
- Fabric is the primary runtime target; shared/common code must keep NeoForge compiling.

---

### Task 1: Pure diagnostics state core

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/diagnostics/AiOperation.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/diagnostics/AiOperationState.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/diagnostics/AiOperationStatus.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/diagnostics/AiDiagnosticsSnapshot.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/diagnostics/AiDiagnostics.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/diagnostics/AiDiagnosticsTest.java`

**Interfaces:**
- Produces: `AiDiagnostics.snapshot(): AiDiagnosticsSnapshot`
- Produces: `AiDiagnostics.recordSuccess(AiOperation,long,String,String,String,String,String,String): void`
- Produces: `AiDiagnostics.recordFailure(AiOperation,long,String,String,String,String,String,String): void`
- Produces package-private: `AiDiagnostics.resetForTests(): void`

- [ ] **Step 1: Write failing tests**

```java
@Test
void startsWithNeverForEveryOperation() {
    AiDiagnostics.resetForTests();
    AiDiagnosticsSnapshot snapshot = AiDiagnostics.snapshot();
    assertEquals(AiOperationState.NEVER, snapshot.chat().state());
    assertEquals(AiOperationState.NEVER, snapshot.stt().state());
    assertEquals(AiOperationState.NEVER, snapshot.tts().state());
}

@Test
void storesOnlyBoundedSingleLineMetadata() {
    AiDiagnostics.resetForTests();
    AiDiagnostics.recordFailure(
            AiOperation.CHAT, 12L, "openrouter", "model", null, "provider_error", "gen-1",
            "line1\n" + "x".repeat(400));
    AiOperationStatus status = AiDiagnostics.snapshot().chat();
    assertEquals(AiOperationState.FAILURE, status.state());
    assertFalse(status.detail().contains("\n"));
    assertTrue(status.detail().length() <= 160);
}
```

- [ ] **Step 2: Commit the RED tests and verify CI fails for missing diagnostics types**

Expected failure: Java compilation errors for missing `AiDiagnostics*` symbols.

- [ ] **Step 3: Implement minimal immutable records/enums and atomic registry**

```java
public enum AiOperation { CHAT, STT, TTS }
public enum AiOperationState { NEVER, SUCCESS, FAILURE }

public record AiOperationStatus(
        AiOperationState state,
        long completedAtEpochMillis,
        long durationMillis,
        String provider,
        String model,
        String finishReason,
        String errorType,
        String generationId,
        String detail
) { }
```

Use one `AtomicReference<AiOperationStatus>` per operation, initialized to `NEVER`.

- [ ] **Step 4: Verify focused tests and full `:common:test` are green in CI**

- [ ] **Step 5: Commit**

```text
feat: add AI diagnostics state core
```

---

### Task 2: Safe configuration snapshot and deterministic status report

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/diagnostics/AiDiagnosticsConfigSnapshot.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/diagnostics/AiStageConfig.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/diagnostics/AiConfigState.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/diagnostics/AiStatusReport.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/diagnostics/AiDiagnosticsConfigSnapshotTest.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/diagnostics/AiStatusReportTest.java`

**Interfaces:**
- Produces: `AiDiagnosticsConfigSnapshot.from(LivingWorldConfig): AiDiagnosticsConfigSnapshot`
- Produces: `AiStatusReport.format(AiDiagnosticsConfigSnapshot,AiDiagnosticsSnapshot): List<String>`

- [ ] **Step 1: Write failing config/report tests**

Cover:

```java
assertEquals("openrouter.ai", snapshot.chat().endpointHost());
assertTrue(snapshot.chat().credentialConfigured());
assertFalse(reportText.contains("SECRET_SENTINEL"));
assertTrue(reportText.contains("Chat: CONFIGURED"));
assertTrue(reportText.contains("TTS: DISABLED"));
assertTrue(reportText.contains("last: NEVER"));
```

Use a config with a sentinel credential and assert the sentinel never appears in any snapshot field or report line.

- [ ] **Step 2: Verify RED**

Expected failure: missing snapshot/report types.

- [ ] **Step 3: Implement safe snapshot**

`AiStageConfig` must contain only:

```java
public record AiStageConfig(
        AiConfigState state,
        boolean enabled,
        boolean credentialConfigured,
        String provider,
        String model,
        String endpointHost,
        String format
) { }
```

`AiConfigState` values:

```java
CONFIGURED, DISABLED, MISCONFIGURED
```

Endpoint normalization returns host only; malformed URLs return `<invalid>`.

- [ ] **Step 4: Implement deterministic formatter**

Each stage emits a summary line and one indented `last:` line. Never display raw credential fields.

- [ ] **Step 5: Verify focused tests and full common tests green**

- [ ] **Step 6: Commit**

```text
feat: add safe AI status report
```

---

### Task 3: Instrument final chat outcomes

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java`
- Test: extend diagnostics tests if helper behavior is extracted.

**Interfaces consumed:**
- `AiDiagnostics.recordSuccess(...)`
- `AiDiagnostics.recordFailure(...)`

- [ ] **Step 1: Add a failing regression test for final-result recording through a small package-private pure helper if needed**

The test must prove intermediate retry state is not treated as the final logical result.

- [ ] **Step 2: Verify RED**

- [ ] **Step 3: Instrument `post(...)`**

Capture `System.nanoTime()` once per logical request. Record exactly once when returning:

```text
usable answer      -> SUCCESS
explicit error     -> FAILURE
empty after retry  -> FAILURE detail=empty_response
transport exception-> FAILURE
```

Safe metadata:

```text
model
endpoint host/provider label
finish_reason
error_type
generation_id
final attempt count / retry exhausted marker
```

Do not record requestBody/token/content/reasoning.

- [ ] **Step 4: Run existing parser/retry tests plus full common tests**

- [ ] **Step 5: Commit**

```text
feat: record chat provider diagnostics
```

---

### Task 4: Instrument STT and TTS outcomes

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/voice/OpenAIAudioProvider.java`
- Test: `common/src/test/java/net/conczin/mca/livingworld/voice/OpenAIAudioProviderTest.java`

- [ ] **Step 1: Add failing tests around extracted recording helpers / safe failure metadata**

The tests must prove failure diagnostics do not contain transcript/TTS input or credential sentinels.

- [ ] **Step 2: Verify RED**

- [ ] **Step 3: Instrument operation boundaries**

`transcribe(...)` and `synthesize(...)` measure total logical duration and record one final result.

Success metadata may include model, endpoint host, resolved format and generation ID where already available.

Failure metadata is bounded/single-line; never store audio, transcription result or TTS input text.

- [ ] **Step 4: Verify existing audio tests and full common tests**

- [ ] **Step 5: Commit**

```text
feat: record voice provider diagnostics
```

---

### Task 5: Add `/villaigence ai status`

**Files:**
- Create: `common/src/main/java/net/conczin/mca/server/command/VillAIgenceCommand.java`
- Modify: `fabric/src/main/java/net/conczin/mca/fabric/MCAFabric.java`

**Command tree:**

```text
/villaigence ai status
```

**Permission:** `hasPermission(2) || server.isSingleplayer()`.

- [ ] **Step 1: Implement command using only snapshot/report APIs**

Core execution:

```java
AiDiagnosticsConfigSnapshot config = AiDiagnosticsConfigSnapshot.from(LivingWorldConfig.getInstance());
for (String line : AiStatusReport.format(config, AiDiagnostics.snapshot())) {
    ctx.getSource().sendSuccess(() -> Component.literal(line), false);
}
return 1;
```

No HTTP call is permitted.

- [ ] **Step 2: Register from Fabric next to existing MCA/Admin command registration**

```java
VillAIgenceCommand.register(dispatcher);
AdminCommand.register(dispatcher);
Command.register(dispatcher);
```

- [ ] **Step 3: Verify Fabric and NeoForge compile gates**

Expected: common + Fabric command compile; NeoForge shared/common compile remains unaffected.

- [ ] **Step 4: Commit**

```text
feat: add VillAIgence AI status command
```

---

### Task 6: Documentation and project-state handoff

**Files:**
- Modify: `README.md`
- Modify: `docs/livingworld/CONFIGURATION.md`
- Modify: `docs/PROJECT_STATE.md`

- [ ] **Step 1: Document operator command**

Document that `/villaigence ai status` is read-only, operator-only, performs no paid/API probe and never prints credentials or conversation content.

- [ ] **Step 2: Update project state**

Move diagnostics/status from immediate pending work into implemented reliability work. Keep rate limits/backpressure, multiplayer concurrency, soak/restart testing and `0.1.6` release verification as remaining `0.1.x` work.

- [ ] **Step 3: Commit**

```text
docs: document VillAIgence AI diagnostics
```

---

### Task 7: Final verification and PR

- [ ] **Step 1: Require fresh final-head CI**

Required green checks:

```text
VillAIgence CI
- :common:test
- Fabric build
- distributable Fabric JAR smoke-check

Java Pull Request CI with Gradle
- NeoForge build
- Fabric build
```

- [ ] **Step 2: Review final diff for security/privacy regressions**

Search changed code for accidental use of:

```text
apiKey
resolvedApiKey()
requestBody
transcript
reasoning
Authorization
```

Any occurrence in diagnostics/report code must be justified and must not expose a raw value.

- [ ] **Step 3: Open PR with exact scope, tests and security guarantees**

Suggested title:

```text
feat: add safe VillAIgence AI diagnostics status
```

- [ ] **Step 4: Merge only after green final-head CI and no unresolved critical/important review findings**
