# Production-JAR Acceptance Implementation Plan

**Goal:** Run the exact remapped Fabric candidate in an isolated production server twice and produce deterministic startup, shutdown and persistence evidence.

**Architecture:** Gradle stages a verified runtime bundle. A Python standard-library harness installs a pinned Fabric server, launches the candidate in a separate JVM, evaluates bounded log oracles, performs controlled shutdown/restart and compares persistent JSON evidence.

## Task 1 — Unit-tested harness contracts

**Files:**

- `scripts/ci/production_server_acceptance.py`
- `scripts/ci/test_production_server_acceptance.py`
- `.github/workflows/livingworld-ci.yml`
- `docs/security/APPROVED_SCRIPT_INVENTORY.json`

- [x] Add unit tests for staging-manifest schema and path confinement.
- [x] Add unit tests for required/forbidden startup signatures.
- [x] Add unit tests for duplicate/missing persistent stores and invalid JSON.
- [x] Add unit tests for unchanged and mutated restart hashes.
- [x] Add the unit command to CI and capture canonical RED before implementation (`ModuleNotFoundError` on head `a45da15d1416d8e077ea671b13396cffae57fc20`).
- [x] Implement the standard-library helpers and staging-verification CLI.
- [x] Verify 11-test unit GREEN and repository security policy on the staging RED run.

## Task 2 — Deterministic runtime staging

**Files:**

- `gradle.properties`
- `fabric/build.gradle`
- `fabric/gradle.lockfile`
- `gradle/verification-metadata.xml`

- [x] Pin Fabric Installer `1.1.1`.
- [x] Add isolated resolvable configurations for the installer and mandatory runtime mods.
- [x] Stage only installer, remapped VillAIgence, Fabric API and Simple Voice Chat.
- [x] Write a deterministic manifest with versions, relative paths, sizes and hashes.
- [x] Reject duplicate/unexpected artifacts and any GameTest class leakage.
- [x] Capture the canonical dependency-verification RED for `fabric-installer-1.1.1.jar/.pom` on CI #1362.
- [x] Generate lock and verification metadata through Gradle and commit only the approved supply-chain delta.
- [x] Remove all one-shot metadata workflows from the branch.
- [ ] Verify the clean staging bundle in permanent CI on an ordinary API-authored head.

## Task 3 — First production startup

- [ ] Install an isolated Fabric server with explicit Minecraft/Loader versions.
- [ ] Generate EULA and bounded low-resource server properties.
- [ ] Start the separate JVM without dev classpath.
- [ ] Require loaded `mca` candidate and Minecraft ready marker.
- [ ] Reject forbidden Mixin/refmap/mod-resolution/JVM signatures.
- [ ] Send `stop`, require clean save and exit code `0`.
- [ ] Preserve bounded first-run logs and machine-readable evidence.

## Task 4 — Restart and persistence

- [ ] Discover exactly one of each canonical persistent basename.
- [ ] Parse every store as UTF-8 JSON and record root type, size and SHA-256.
- [ ] Start the same world in a second JVM with the same candidate.
- [ ] Require second clean startup/shutdown.
- [ ] Require stable relative paths and unchanged hashes.
- [ ] If startup does not create all stores, add a deterministic application-owned fixture rather than fabricating files in the harness.

## Task 5 — CI and merge boundary

- [ ] Upload acceptance JSON and bounded logs on success/failure.
- [ ] Keep existing unit, GameTest, package, security and supply-chain gates mandatory.
- [ ] Record exact-head RED/GREEN evidence in the PR.
- [ ] Confirm no provider credentials or external AI calls are used.
- [ ] Require repeated independent GREEN before merge.
- [ ] Update the scenario catalog only after real production evidence passes.
