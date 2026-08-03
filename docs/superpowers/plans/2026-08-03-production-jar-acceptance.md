# Production-JAR Acceptance Implementation Plan

**Goal:** Run the exact remapped Fabric candidate in an isolated production server twice and produce deterministic startup, shutdown and persistence evidence.

**Architecture:** Gradle stages a verified runtime bundle. A Python standard-library harness installs a pinned Fabric server, launches the candidate in a separate JVM, evaluates bounded log oracles, performs controlled shutdown/restart and compares persistent JSON evidence. A separately remapped test-only Fabric fixture invokes public VillAIgence store APIs so persistence is exercised without changing the production candidate.

## Task 1 — Unit-tested harness contracts

**Files:**

- `scripts/ci/production_server_acceptance.py`
- `scripts/ci/test_production_server_acceptance.py`
- `scripts/ci/test_production_server_process.py`
- `.github/workflows/livingworld-ci.yml`
- `docs/security/APPROVED_SCRIPT_INVENTORY.json`

- [x] Add unit tests for staging-manifest schema and path confinement.
- [x] Add unit tests for required/forbidden startup signatures.
- [x] Add unit tests for duplicate/missing persistent stores and invalid JSON.
- [x] Add unit tests for unchanged and mutated restart hashes.
- [x] Add subprocess tests for ready/stop/clean-exit, early exit and startup timeout.
- [x] Capture canonical helper RED (`ModuleNotFoundError`) on head `a45da15d1416d8e077ea671b13396cffae57fc20`.
- [x] Capture canonical process-boundary RED (`ImportError: run_server_process`) on head `1f3d70c3ba575b96a693e8636b9eddad35b0424e`.
- [x] Implement the standard-library helpers, process lifecycle and staging-verification CLI.
- [x] Verify 15 pure helper tests and 3 subprocess lifecycle tests GREEN.

## Task 2 — Deterministic runtime staging

**Files:**

- `gradle.properties`
- `fabric/build.gradle`
- `fabric/production-acceptance-fixture.gradle`
- `fabric/gradle.lockfile`
- `gradle/verification-metadata.xml`

- [x] Pin Fabric Installer `1.1.1`.
- [x] Add isolated resolvable configurations for the installer and mandatory runtime mods.
- [x] Stage installer, remapped VillAIgence, Fabric API and Simple Voice Chat.
- [x] Write a deterministic manifest with versions, relative paths, sizes and hashes.
- [x] Reject duplicate/unexpected artifacts and GameTest/fixture leakage into the production candidate.
- [x] Capture canonical dependency-verification RED for `fabric-installer-1.1.1.jar/.pom` on CI #1362.
- [x] Generate lock and verification metadata through Gradle and commit only the approved supply-chain delta.
- [x] Verify the clean staging bundle in permanent CI.
- [x] Add a separate source set and remap task for `mca-production-acceptance-fixture`.
- [ ] Verify custom fixture compilation, remapping, manifest inclusion and candidate exclusion in permanent CI.

## Task 3 — First production startup

- [x] Install an isolated Fabric server with explicit Minecraft/Loader versions.
- [x] Generate EULA and bounded low-resource server properties.
- [x] Start the separate JVM without dev classpath.
- [x] Require loaded `mca` candidate and Minecraft ready marker.
- [x] Reject forbidden Mixin/refmap/mod-resolution/JVM signatures.
- [x] Send `stop`, require clean save and exit code `0`.
- [x] Preserve bounded first-run logs and machine-readable evidence.
- [x] Capture canonical integration RED: production startup/shutdown passed, but an empty server created no application stores.
- [ ] Require the application-owned fixture ready marker after all six store APIs succeed.

## Task 4 — Restart and persistence

- [x] Discover exactly one of each canonical persistent basename.
- [x] Parse every store as UTF-8 JSON and record root type, size and SHA-256.
- [x] Add an idempotent fixture using only public `append`, `applyDelta`, `resolve` and `put` APIs with fixed IDs/timestamps.
- [ ] Verify first run creates all stores through product code.
- [ ] Start the same world in a second JVM with the same candidate and fixture.
- [ ] Require second clean startup/shutdown and fixture marker.
- [ ] Require stable relative paths and unchanged hashes.

## Task 5 — CI and merge boundary

- [x] Upload acceptance JSON and bounded logs on success/failure.
- [x] Keep existing unit, GameTest, package, security and supply-chain gates mandatory.
- [ ] Record exact-head RED/GREEN evidence in the PR.
- [ ] Confirm no provider credentials or external AI calls are used.
- [ ] Require repeated independent GREEN before merge.
- [ ] Update the scenario catalog only after real production evidence passes.
