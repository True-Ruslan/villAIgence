# M11 Phase E Automation Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move all deterministic VillAIgence release risks into repeatable CI and reduce manual acceptance to physical microphone, audible/spatial perception, final graphical-client review and one exact installed-JAR smoke.

**Architecture:** Extend the existing common-test, Fabric GameTest, production-JAR acceptance and exact-release gates. Fast deterministic tests remain mandatory on every pull request; destructive, long-running and real-brain scenarios run nightly and are also mandatory for release candidates. The acceptance catalog remains the canonical risk-to-proof map.

**Tech Stack:** Java 21, Gradle 9.6.1, Fabric 1.21.1, Fabric GameTest, JUnit 5, Python 3.12, GitHub Actions, Simple Voice Chat integration.

## Global Constraints

- Do not request, tag or publish `0.1.26+1.21.1` during Phase E.
- Do not require paid providers or repository secrets for pull-request tests.
- Do not weaken deadlines, payload limits, redirect restrictions, authorization or exactly-once persistence.
- Do not use sleeps as correctness oracles.
- Every destructive persistence fixture must use a generated temporary world.
- Every automated scenario must have a bounded timeout and explicit terminal oracle.
- A catalog state may become `AUTOMATED` only after exact-head CI evidence exists.

---

### Task 1: Capture the approved Phase E design and execution plan

**Files:**
- Create: `docs/superpowers/specs/2026-08-05-m11-phase-e-automation-completion-design.md`
- Create: `docs/superpowers/plans/2026-08-05-m11-phase-e-automation-completion.md`

**Interfaces:**
- Produces: canonical scope, non-goals, workstream order and evidence requirements used by all later tasks.

- [x] **Step 1:** Write the approved design.
- [x] **Step 2:** Self-review for placeholders, contradictions and ambiguous completion criteria.
- [x] **Step 3:** Commit the design and plan on an isolated branch.

### Task 2: E0 — remove execution-time Project access from production staging

**Files:**
- Create: `common/src/test/java/net/conczin/mca/security/ProductionAcceptanceConfigurationCachePolicyTest.java`
- Modify: `fabric/build.gradle`
- Modify: `fabric/production-acceptance-fixture.gradle`

**Interfaces:**
- Consumes: task name `stageProductionAcceptanceRuntime` and existing `productionAcceptanceStageDir` provider.
- Produces: configuration-time values `productionAcceptanceMinecraftVersion`, `productionAcceptanceLoaderVersion`, `productionAcceptanceInstallerVersion`, `productionAcceptanceModId`, and a task action containing no `project.` access.

- [ ] **Step 1: Write the failing source-policy test**

```java
@Test
void productionAcceptanceStagingDoesNotQueryProjectDuringExecution() throws IOException {
    String source = Files.readString(repositoryRoot().resolve("fabric/build.gradle"));
    String block = taskBlock(source, "def stageProductionAcceptanceRuntime =", "\n\ntasks.named('check')");
    assertFalse(block.contains("project.delete("));
    assertFalse(block.contains("project.property("));
    assertTrue(block.contains("FileSystemOperations"));
}
```

- [ ] **Step 2: Run RED**

Run: `./gradlew :common:test --tests '*ProductionAcceptanceConfigurationCachePolicyTest' --no-daemon --console=plain`

Expected: FAIL because the task action currently contains `project.delete` and `project.property`.

- [ ] **Step 3: Capture immutable values during configuration**

Use Gradle providers before task registration and declare them as inputs. Inject `FileSystemOperations` into a small task type or use a configuration-time provider-backed `Delete`/`Sync` composition so task execution never reaches `Project`.

- [ ] **Step 4: Make the fixture extension configuration-cache safe**

Capture fixture archive and stage paths as providers and avoid execution-time project lookups.

- [ ] **Step 5: Run GREEN and configuration-cache reuse**

Run:

```bash
./gradlew :common:test --tests '*ProductionAcceptanceConfigurationCachePolicyTest' --no-daemon --console=plain
./gradlew :fabric:stageProductionAcceptanceRuntime --configuration-cache --no-daemon --console=plain
./gradlew :fabric:stageProductionAcceptanceRuntime --configuration-cache --no-daemon --console=plain
```

Expected: policy PASS; first Gradle run stores configuration cache; second reuses it; no `Task.project` warning.

### Task 3: E1 — automate duplicate-identity prevention

**Files:**
- Create: `fabric/src/gametest/java/net/conczin/mca/gametest/TombstoneIdentityReplayGameTests.java`
- Modify: `common/src/test/resources/acceptance/scenarios.tsv`

**Interfaces:**
- Consumes: `TombstoneBlock.Data.setEntity`, `writeToStack`, `readFromStack`, `createEntity` and real server entity lookup.
- Produces: `VAI-LIFE-005` evidence that replaying one stored UUID leaves exactly one live entity and one inventory multiset.

- [ ] **Step 1: Write the failing GameTest**

Create one stored villager with a fixed UUID and inventory, reconstruct it twice from cloned portable tombstone data, add both attempted entities through the production server API, and assert exactly one live entity with the fixture UUID.

- [ ] **Step 2: Run RED**

Run: `./gradlew :fabric:runGameTest --no-daemon --console=plain`

Expected: the new test must fail if the second replay can create a second live identity or if no explicit replay guard exists.

- [ ] **Step 3: Implement the smallest replay guard at the tombstone resurrection boundary**

Before adding a recreated entity, resolve the UUID in the target server level. Return the existing authoritative entity or reject the replay without consuming/duplicating inventory. Do not change ordinary first resurrection.

- [ ] **Step 4: Add negative and restart-aware assertions**

Verify different UUIDs remain independent and the stored inventory appears once.

- [ ] **Step 5: Run GREEN**

Run:

```bash
./gradlew :common:test :fabric:runGameTest :fabric:build :neoforge:build --no-daemon --console=plain
```

Expected: all tests and loader builds PASS.

### Task 4: E2 — production lifecycle evidence across two JVMs

**Files:**
- Modify: `fabric/src/productionAcceptanceFixture/java/net/conczin/mca/acceptancefixture/ProductionAcceptanceFixture.java`
- Create: `fabric/src/productionAcceptanceFixture/java/net/conczin/mca/acceptancefixture/LifecycleAcceptanceState.java`
- Modify: `scripts/ci/production_server_acceptance.py`
- Modify: `scripts/ci/test_production_server_acceptance.py`
- Modify: `common/src/test/resources/acceptance/scenarios.tsv`

**Interfaces:**
- Produces: `world/livingworld/acceptance-lifecycle.json` with schema, UUID, name, expected inventory, phase, live entity count and exact assertions from both JVM runs.

- [ ] **Step 1:** Add failing Python report tests requiring lifecycle evidence and exact before/after restart equality.
- [ ] **Step 2:** Add fixture phase one: create real MCA villager, perform death/tombstone/portable/restoration/resurrection, persist evidence.
- [ ] **Step 3:** Add fixture phase two: on second JVM, load authoritative entity/evidence and verify UUID, name, inventory and count.
- [ ] **Step 4:** Include lifecycle evidence in `acceptance-report.json` and fail on missing or inconsistent fields.
- [ ] **Step 5:** Run Python unit tests and exact production acceptance twice.

### Task 5: E3 — corrupt persistence recovery matrix

**Files:**
- Create: `scripts/ci/persistence_recovery_acceptance.py`
- Create: `scripts/ci/test_persistence_recovery_acceptance.py`
- Modify: `.github/workflows/livingworld-ci.yml`
- Create: `.github/workflows/livingworld-nightly.yml`
- Modify: `common/src/test/resources/acceptance/scenarios.tsv`

**Interfaces:**
- Consumes: staged production runtime and generated server world.
- Produces: machine-readable recovery report for each canonical auxiliary store and corruption variant.

- [ ] **Step 1:** Write failing Python tests for truncated JSON, empty file, wrong root type, incompatible schema and leftover temporary file.
- [ ] **Step 2:** Implement isolated matrix setup, startup, backup detection and canonical JSON validation.
- [ ] **Step 3:** Verify unaffected store hashes and idempotent second startup.
- [ ] **Step 4:** Add nightly workflow and release-gate invocation.
- [ ] **Step 5:** Run Python tests and one complete matrix in CI.

### Task 6: E4/E5 — authenticated text and two-session Operator Lore transport

**Files:**
- Create: `common/src/test/java/net/conczin/mca/network/AuthenticatedTextTurnAcceptanceTest.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/lore/OperatorLoreNetworkSessionAcceptanceTest.java`
- Modify only the narrow package-private transport seams required by those tests.
- Modify: `common/src/test/resources/acceptance/scenarios.tsv`

**Interfaces:**
- Produces: deterministic authenticated session harness with player UUID, scope, target NPC UUID, request ID and revision.

- [ ] **Step 1:** Write failing tests around production packet-handler authority boundaries.
- [ ] **Step 2:** Add the smallest package-private session adapters without exposing test-only public APIs.
- [ ] **Step 3:** Prove exactly one dialogue effect and explicit stale conflict/retry.
- [ ] **Step 4:** Prove unauthorized identity/scope rejection and response ownership.
- [ ] **Step 5:** Run all common and provider integration tests.

### Task 7: E6 — voice codec and loopback transport

**Files:**
- Create: `common/src/test/java/net/conczin/mca/voice/VoiceTransportLoopbackAcceptanceTest.java`
- Modify narrow voice transport seams only where required.
- Modify: `common/src/test/resources/acceptance/scenarios.tsv`

**Interfaces:**
- Produces: bounded local PCM/Opus loopback fixture with sequence numbers, deadlines and cancellation token.

- [ ] **Step 1:** Write failing encode/decode, ordering, loss and disconnect tests.
- [ ] **Step 2:** Implement deterministic local loopback adapter around production codec/queue boundaries.
- [ ] **Step 3:** Verify shared deadline, bounded buffers and cancellation on disconnect.
- [ ] **Step 4:** Run common tests and existing mock-provider integration.

### Task 8: E7/E8 — nightly gameplay and real-brain navigation

**Files:**
- Create focused GameTest classes under `fabric/src/gametest/java/net/conczin/mca/gametest/`.
- Modify: `.github/workflows/livingworld-nightly.yml`
- Modify: `common/src/test/resources/acceptance/scenarios.tsv`

**Interfaces:**
- Produces: bounded scenarios for ladders, doors, obstacle replan, mounts, ranged combat, gifts, fishing and two real MCA villagers escaping independent water lanes.

- [ ] **Step 1:** Add one failing scenario per behavior with explicit inventory/entity/position oracle.
- [ ] **Step 2:** Implement only production fixes exposed by meaningful RED failures.
- [ ] **Step 3:** Group long scenarios in a nightly GameTest tag/suite.
- [ ] **Step 4:** Require nightly suite in exact release validation.

### Task 9: E9 — soak, risk selection and documentation closure

**Files:**
- Create: `scripts/ci/select_acceptance_suites.py`
- Create: `scripts/ci/test_select_acceptance_suites.py`
- Create: `.github/workflows/livingworld-soak.yml`
- Modify: `.github/workflows/livingworld-ci.yml`
- Modify: `.github/workflows/livingworld-release.yml`
- Modify: `docs/PROJECT_STATE.md`
- Modify: `docs/ROADMAP.md`
- Modify: `common/src/test/resources/acceptance/scenarios.tsv`

**Interfaces:**
- Produces: deterministic path-to-risk suite selection for PR optimization; release validation always selects all mandatory suites.

- [ ] **Step 1:** Write failing selector tests for runtime, persistence, network, voice, navigation and release-only changes.
- [ ] **Step 2:** Implement fail-closed suite selection with an `all` mode for release.
- [ ] **Step 3:** Add bounded repeated restart, concurrent turns and memory-pressure soak workflow.
- [ ] **Step 4:** Reconcile catalog, state and roadmap with exact CI evidence.
- [ ] **Step 5:** Run all mandatory workflows and perform final change review before any merge or release request.