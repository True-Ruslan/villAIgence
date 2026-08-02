# Risk-Based Automated Acceptance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a risk-driven acceptance catalog and a real Fabric server GameTest suite that broadens regression coverage beyond previously observed defects and runs as a required PR gate.

**Architecture:** A JSON catalog describes risk domains, scenario severity, execution layer and deterministic oracle. A common JUnit validator enforces catalog completeness. An isolated Fabric `gametest` source set contains real-server scenarios for entity lifecycle, tombstone serialization/drop invariants and multi-NPC water navigation; Loom runs these tests during the Fabric build without packaging test code into the release JAR.

**Tech Stack:** Java 21, JUnit 5, Gson, Fabric Loom 1.17.17, Fabric API 0.116.13+1.21.1, Minecraft GameTest, GitHub Actions.

## Global Constraints

- Minecraft remains exactly `1.21.1`.
- Java remains exactly `21` in CI.
- Internal mod ID remains `mca`; the test mod uses the separate ID `mca-acceptance-test`.
- Test-only classes and resources must not enter the production Fabric JAR.
- NeoForge compile compatibility remains required.
- No real Chat/STT/TTS provider call is allowed in merge-blocking CI.
- Every asynchronous GameTest has a deterministic state oracle and a hard tick timeout.
- The first phase must not claim production-JAR restart, mock-provider, client or multi-client automation.

---

### Task 1: Risk scenario catalog contract

**Files:**
- Create: `common/src/test/resources/acceptance/scenarios.json`
- Create: `common/src/test/java/net/conczin/mca/acceptance/AcceptanceScenarioCatalogTest.java`

**Interfaces:**
- Consumes: Gson test runtime already declared by `common/build.gradle`.
- Produces: a validated catalog with stable scenario IDs and the enum-like strings `BOOT_PACKAGE`, `IDENTITY_LIFECYCLE`, `PERSISTENCE_IDEMPOTENCY`, `NAVIGATION_SURVIVAL`, `GAMEPLAY_INTERACTION`, `AI_VOICE_RESILIENCE`, `CONCURRENCY_AUTHORIZATION`.

- [ ] **Step 1: Write the failing catalog validator**

Create a JUnit test that loads `/acceptance/scenarios.json`, rejects duplicate IDs and missing fields, requires all seven domains, and requires every automated scenario to define `gate`, `oracle`, `timeoutSeconds` greater than zero and `evidence`.

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```bash
./gradlew :common:test --tests net.conczin.mca.acceptance.AcceptanceScenarioCatalogTest --no-daemon
```

Expected: FAIL because `/acceptance/scenarios.json` does not exist.

- [ ] **Step 3: Add the initial broad scenario catalog**

Add at least 21 scenarios: at least three per domain. Mark the Phase A entity/tombstone/water cases `AUTOMATED`; mark later phases `PLANNED` with explicit gate names and deterministic oracles. Any `CRITICAL` manual scenario must include `manualRationale`.

- [ ] **Step 4: Re-run the focused test and verify GREEN**

Run the same Gradle command. Expected: PASS with all seven domains represented and no duplicate IDs.

- [ ] **Step 5: Commit**

```bash
git add common/src/test/resources/acceptance/scenarios.json common/src/test/java/net/conczin/mca/acceptance/AcceptanceScenarioCatalogTest.java
git commit -m "test: enforce risk-based acceptance catalog"
```

### Task 2: Isolated Fabric GameTest source set

**Files:**
- Modify: `fabric/build.gradle`
- Create: `fabric/src/gametest/resources/fabric.mod.json`
- Create: `fabric/src/gametest/java/net/conczin/mca/gametest/VillAIgenceGameTests.java`

**Interfaces:**
- Consumes: main Fabric mod `mca`, Fabric API GameTest entrypoint and common MCA classes included by the multiloader plugin.
- Produces: Loom-managed server GameTests under test mod ID `mca-acceptance-test`; `:fabric:build` runs them automatically.

- [ ] **Step 1: Add a minimal failing GameTest entrypoint**

Create the test `fabric.mod.json` with one `fabric-gametest` entrypoint and a Java class whose initial test intentionally asserts a non-air block at the empty origin.

- [ ] **Step 2: Configure Loom tests**

Add:

```groovy
fabricApi {
    configureTests {
        createSourceSet = true
        modId = 'mca-acceptance-test'
        enableGameTests = true
        enableClientGameTests = false
        eula = true
    }
}
```

Keep the existing data-generation configuration in the same `fabricApi` block.

- [ ] **Step 3: Run the GameTest and verify RED**

Run:

```bash
./gradlew :fabric:runGameTest --stacktrace --no-daemon
```

Expected: the intentionally false assertion fails inside the real Minecraft server.

- [ ] **Step 4: Replace the false assertion with a boot/registry test**

The test creates `EntitiesMCA.MALE_VILLAGER` in the GameTest level, verifies the entity and navigation are non-null, adds it to the world and verifies the world contains exactly that UUID before succeeding.

- [ ] **Step 5: Run GameTest and verify GREEN**

Run the same Gradle command. Expected: PASS and normal GameTest server shutdown.

- [ ] **Step 6: Verify test code is absent from the remapped release JAR**

Run:

```bash
./gradlew :fabric:remapJar --no-daemon
unzip -l fabric/build/libs/mca-fabric-*.jar | grep -F 'net/conczin/mca/gametest/' && exit 1 || true
```

Expected: no GameTest class in the production JAR.

- [ ] **Step 7: Commit**

```bash
git add fabric/build.gradle fabric/src/gametest
git commit -m "test: add isolated Fabric server GameTests"
```

### Task 3: Tombstone lifecycle GameTests

**Files:**
- Modify: `fabric/src/gametest/java/net/conczin/mca/gametest/VillAIgenceGameTests.java`

**Interfaces:**
- Consumes: `BlocksMCA.UPRIGHT_HEADSTONE`, `TombstoneBlock.Data`, `EntitiesMCA.MALE_VILLAGER`, `LootParams.Builder` and `TombstoneItemDataPolicy` through production APIs.
- Produces: deterministic tests `filledTombstoneItemRoundTripPreservesNpcIdentity` and `filledTombstoneSilkTouchDropIsPortableAndUnique`.

- [ ] **Step 1: Add failing identity round-trip test**

Create an MCA villager with fixed UUID, custom name and three inventory fixture items. Store it in a real `TombstoneBlock.Data`, serialize to an `ItemStack`, deserialize into a second data instance and reconstruct the entity. Assert UUID, name and item counts.

- [ ] **Step 2: Run focused GameTest and verify RED if any production boundary is unsupported**

Run:

```bash
./gradlew :fabric:runGameTest --stacktrace --no-daemon
```

Expected before completing helper setup: FAIL at the first missing/incorrect lifecycle assertion, not at class loading.

- [ ] **Step 3: Complete minimal real-server fixture setup**

Use a placed upright headstone block and its registered block entity where level ownership is required. Avoid reflection and direct private-field access.

- [ ] **Step 4: Add Silk Touch evaluated-drop test**

Construct a real `LootParams.Builder` with origin, block entity and a diamond pickaxe carrying Silk Touch. Call the production tombstone drop path. Assert exactly one stack whose item equals `block.asItem()`, then load that stack into new tombstone data and reconstruct the same NPC identity.

- [ ] **Step 5: Add empty-grave control**

Evaluate drops for an empty placed tombstone and assert the test does not synthesize stored NPC data or duplicate tombstone items.

- [ ] **Step 6: Run GameTests and verify GREEN**

Expected: all tombstone tests pass within their tick budget.

- [ ] **Step 7: Commit**

```bash
git add fabric/src/gametest/java/net/conczin/mca/gametest/VillAIgenceGameTests.java
git commit -m "test: automate tombstone lifecycle invariants"
```

### Task 4: Multi-NPC navigation and survival GameTests

**Files:**
- Modify: `fabric/src/gametest/java/net/conczin/mca/gametest/VillAIgenceGameTests.java`

**Interfaces:**
- Consumes: `VillagerEntityMCA.getNavigation()`, fixed GameTest geometry and vanilla water/stone blocks.
- Produces: deterministic tests `twoNpcsEscapeIndependentWaterLanes` and `waterEscapePreservesLandNavigation`.

- [ ] **Step 1: Build fixed two-lane water geometry**

Create two separated stone-bottom lanes, source water blocks, one-block shore ramps and dry targets. Spawn one MCA villager in each lane with unique UUIDs.

- [ ] **Step 2: Add bounded escape oracle**

On each assertion attempt, renew each NPC's navigation target. The test succeeds only when both NPCs are alive, not in water and on their own dry target side. Timeout at 240 ticks.

- [ ] **Step 3: Add post-water land-navigation phase**

After the first NPC exits, assign a second dry target at least four blocks farther. Succeed only when it reaches the second target while alive and out of water. Timeout at 320 ticks.

- [ ] **Step 4: Run GameTests repeatedly**

Run three independent invocations:

```bash
./gradlew :fabric:runGameTest --no-daemon
./gradlew :fabric:runGameTest --no-daemon
./gradlew :fabric:runGameTest --no-daemon
```

Expected: three PASS results with no retry wrapper and no random-seed-specific failure.

- [ ] **Step 5: Commit**

```bash
git add fabric/src/gametest/java/net/conczin/mca/gametest/VillAIgenceGameTests.java
git commit -m "test: automate multi-NPC water navigation"
```

### Task 5: CI and package evidence

**Files:**
- Modify: `.github/workflows/livingworld-ci.yml`
- Modify: `fabric/build.gradle`
- Modify: `docs/livingworld/VALIDATION_0.1.22_TOMBSTONE_STARTUP_FIX.md`

**Interfaces:**
- Consumes: Gradle `:common:test`, `:fabric:build`, `:neoforge:build`, `:fabric:runGameTest` or the Loom-created build dependency, and remapped JAR.
- Produces: required PR gate output and explicit partial installed acceptance markers.

- [ ] **Step 1: Make GameTest execution explicit in CI**

Change the build step to run the catalog test, server GameTests, Fabric build and NeoForge build with a single captured log. Do not rely only on an implicit task dependency.

- [ ] **Step 2: Add package exclusion gate**

Extend `verifyFabricRefmap` to reject any production JAR entry under `net/conczin/mca/gametest/` and reject the test mod ID string `mca-acceptance-test` in `fabric.mod.json`.

- [ ] **Step 3: Record current installed evidence accurately**

Update the validation document to mark startup, two-NPC water escape and filled-grave round trip as PASS, while cumulative gameplay remains PENDING.

- [ ] **Step 4: Run complete verification**

Run:

```bash
./gradlew :common:test :fabric:runGameTest :fabric:build :neoforge:build --stacktrace --no-daemon
scripts/ci/package-livingworld-release.sh ci-m11 false ci-dist
```

Expected: all commands PASS; production JAR contains no GameTest classes/resources.

- [ ] **Step 5: Commit**

```bash
git add .github/workflows/livingworld-ci.yml fabric/build.gradle docs/livingworld/VALIDATION_0.1.22_TOMBSTONE_STARTUP_FIX.md
git commit -m "ci: require risk-based server acceptance"
```

### Task 6: Final review and merge boundary

**Files:**
- Review all changed files.

**Interfaces:**
- Consumes: exact branch head and GitHub Actions results.
- Produces: a reviewed PR whose claims are limited to Phase A.

- [ ] **Step 1: Inspect exact diff and security inventory**

Confirm no new executable script, dependency, production entrypoint, persistent schema or release metadata was added.

- [ ] **Step 2: Verify CI on exact head**

Require success from VillAIgence CI, Java PR CI, repository security policy and supply-chain verification when triggered.

- [ ] **Step 3: Verify no unresolved review threads**

List PR review threads and resolve only after applying or explicitly rejecting actionable feedback with evidence.

- [ ] **Step 4: Merge only if Phase A evidence is complete**

Use squash merge. Do not publish a new release solely for test infrastructure. Production restart and mock-provider phases remain follow-up work.