# Risk-Based Automated Acceptance Implementation Plan

> **Execution model:** Phase A is implemented in PR #103. Later phases remain explicit follow-up work and are intentionally not represented as complete.

**Goal:** Add a risk-driven acceptance catalog and a real Fabric server GameTest suite that broadens regression coverage beyond previously observed defects and runs as a required PR gate.

**Architecture:** A dependency-free TSV catalog describes risk domains, scenario severity, execution layer and deterministic oracle. A common JUnit validator enforces catalog completeness. An isolated Fabric `gametest` source set contains real-server scenarios for entity lifecycle, tombstone serialization/drop invariants and multi-NPC water navigation. Production-JAR verification rejects test metadata or classes.

**Tech Stack:** Java 21, JUnit 5, Fabric Loom 1.17.17, Fabric API 0.116.13+1.21.1, Minecraft GameTest, GitHub Actions.

## Global constraints

- Minecraft remains exactly `1.21.1`.
- Java remains exactly `21` in CI.
- Internal mod ID remains `mca`; test mod ID is `mca-acceptance-test`.
- Test-only classes and resources must not enter the production Fabric JAR.
- NeoForge compile compatibility remains required.
- No real Chat/STT/TTS provider call is allowed in merge-blocking CI.
- Every asynchronous GameTest has a deterministic state oracle and hard tick timeout.
- GameTest evidence is not production-JAR startup/restart evidence.

## Task 1 — Risk scenario catalog contract

**Files:**

- `common/src/test/resources/acceptance/scenarios.tsv`
- `common/src/test/java/net/conczin/mca/acceptance/AcceptanceScenarioCatalogTest.java`

- [x] Write the dependency-free validator before the catalog.
- [x] Verify canonical RED on head `6eef21d8167eb1f465dae391a679ab512588395d`.
- [x] Confirm RED reached `:common:test` and failed because `/acceptance/scenarios.tsv` was absent.
- [x] Add at least three scenarios for each of the seven risk domains.
- [x] Enforce stable IDs, valid severity/state, deterministic oracle, timeout, gate and evidence fields.
- [x] Require explicit manual rationale for manual canaries and non-automated critical risks.
- [x] Verify GREEN on head `22bca3c1c9084de094f5842879a671f5cc6ebdd8` with CI #1314 and Java PR CI #735.
- [x] Preserve dependency locks by parsing TSV with the Java standard library instead of adding Gson to test compile classpath.

## Task 2 — Isolated Fabric GameTest source set

**Files:**

- `fabric/build.gradle`
- `fabric/src/gametest/resources/fabric.mod.json`
- `fabric/src/gametest/java/net/conczin/mca/gametest/VillAIgenceGameTests.java`
- `fabric/src/gametest/java/net/conczin/mca/gametest/TombstoneControlGameTests.java`

- [x] Configure Loom `configureTests` with `createSourceSet = true` and test mod ID `mca-acceptance-test`.
- [x] Use the Minecraft 1.21.1-compatible API: vanilla `GameTest` annotation plus `FabricGameTest.EMPTY_STRUCTURE`.
- [x] Verify canonical runtime RED on head `713ab4cfee4092651811073973e44f6013b4a1c6`.
- [x] Confirm the real Fabric server loaded the test mod and failed exactly on the intentional `RED: Fabric server GameTest harness is active` assertion.
- [x] Replace the intentional failure with a real MCA entity/registry/navigation boot test.
- [x] Verify boot GREEN in Java PR CI #740.

## Task 3 — Tombstone lifecycle and controls

- [x] Create fixed-UUID MCA NPC fixtures with a custom name and three inventory item types.
- [x] Automate `NPC -> TombstoneBlock.Data -> ItemStack -> TombstoneBlock.Data -> NPC`.
- [x] Assert UUID and name continuity.
- [x] Assert exact inventory item totals and exact non-empty stack count.
- [x] Reject slot-position assertions because MCA's list serialization does not promise preservation of empty gaps.
- [x] Verify the corrected lifecycle oracle GREEN in Java PR CI #743.
- [x] Construct a real Silk Touch pickaxe from the server enchantment registry.
- [x] Call the public production `TombstoneBlock.getDrops(...)` path with real `LootParams`.
- [x] Assert exactly one portable tombstone, block-entity data presence and full NPC reconstruction.
- [x] Verify Silk Touch drop GREEN on head `f730620c67e9db0404d02588135005a4b14775b7` with CI #1324 and Java PR CI #745.
- [x] Add an empty-grave negative control that rejects synthesized NPC data and duplicate tombstone items.

## Task 4 — Multi-NPC navigation and survival

- [x] Build two separated fixed-geometry water lanes with independent dry targets.
- [x] Spawn two MCA NPCs of different registered entity types.
- [x] Require both NPCs to remain alive, become dry and reach their own target within 240 ticks.
- [x] Build a second scenario requiring the same NPC to leave water and then reach a dry target five blocks farther.
- [x] Bound the second scenario to 320 ticks.
- [x] Verify the first combined water GREEN in Java PR CI #746.
- [ ] Confirm at least two additional independent GREEN executions on later exact heads before merge.

## Task 5 — CI and production-package boundary

- [x] Make `:fabric:runGameTest` explicit in `livingworld-ci.yml`.
- [x] Run catalog, GameTests, Fabric build and NeoForge build in one fail-closed Gradle invocation.
- [x] Improve failure extraction for GameTest diagnostics.
- [x] Reject production `fabric.mod.json` if it contains test mod ID or `fabric-gametest` entrypoint.
- [x] Reject any production JAR entry under `net/conczin/mca/gametest/`.
- [x] Preserve existing release identity, Mixin/refmap, navigation, tombstone and resurrection package checks.
- [x] Record installed `0.1.22` startup, water and grave PASS without claiming cumulative acceptance.
- [ ] Verify the complete exact-head CI matrix after removing the temporary focused workflow.

## Task 6 — Documentation and merge boundary

- [x] Record the layered architecture and seven risk domains.
- [x] Update the scenario catalog from planned to automated only after actual GameTest GREEN evidence.
- [x] Keep installed canaries distinct from GameTest evidence.
- [x] Document remaining cumulative `0.1.22` checks.
- [ ] Remove `.github/workflows/ops-m11-focused-gametest.yml` before merge.
- [ ] Inspect the final exact diff and confirm no dependency, production entrypoint, persistent schema or release metadata change.
- [ ] Require exact-head success from VillAIgence CI, Java PR CI, repository security policy and supply-chain verification.
- [ ] Confirm no unresolved review threads.
- [ ] Squash merge PR #103 only after all Phase A evidence is complete.

## Phase B — Installed production acceptance

- [ ] Add a production Fabric server launcher using the remapped candidate JAR.
- [ ] Add startup log forbidden-signature oracle.
- [ ] Add controlled shutdown and second-JVM restart.
- [ ] Add persistent JSON validity and hash report.
- [ ] Add official downloaded release-asset startup smoke.

## Phase C — Deterministic AI and voice integration

- [ ] Add local mock Chat, STT and TTS endpoints.
- [ ] Cover success, null/malformed responses, timeout, retry and body limits.
- [ ] Enforce one global Chat deadline across all attempts and body reads.
- [ ] Keep real provider canaries non-blocking.

## Phase D — Concurrency and client acceptance

- [ ] Add two logical clients for stale operator-lore conflict.
- [ ] Add client GameTests for editor open/edit/save/reopen behavior.
- [ ] Add true multi-client Voice Chat canary where CI infrastructure permits.
