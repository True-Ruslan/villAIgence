# Production-JAR Acceptance Implementation Plan

**Goal:** Run the exact remapped Fabric candidate in an isolated production server twice and produce deterministic startup, shutdown and persistence evidence.

**Architecture:** Gradle stages a verified runtime bundle. A Python standard-library harness installs a pinned Fabric server, launches the candidate in a separate JVM, evaluates bounded log oracles, performs controlled shutdown/restart and compares persistent JSON evidence.

## Task 1 — Unit-tested harness contracts

**Files:**

- `scripts/ci/production_server_acceptance.py`
- `scripts/ci/test_production_server_acceptance.py`
- `.github/workflows/livingworld-ci.yml`
- `docs/security/APPROVED_SCRIPT_INVENTORY.json`

- [ ] Add unit tests for staging-manifest schema and path confinement.
- [ ] Add unit tests for required/forbidden startup signatures.
- [ ] Add unit tests for duplicate/missing persistent stores and invalid JSON.
- [ ] Add unit tests for unchanged and mutated restart hashes.
- [ ] Add the unit command to CI and capture canonical RED before implementation.
- [ ] Implement the standard-library helpers and CLI.
- [ ] Verify unit GREEN and repository security policy.

## Task 2 — Deterministic runtime staging

**Files:**

- `gradle.properties`
- `fabric/build.gradle`
- `fabric/gradle.lockfile`
- `gradle/verification-metadata.xml`

- [ ] Pin an explicit Fabric Installer version.
- [ ] Add isolated resolvable configurations for the installer and mandatory runtime mods.
- [ ] Stage only installer, remapped VillAIgence, Fabric API and Simple Voice Chat.
- [ ] Write a deterministic manifest with versions, relative paths, sizes and hashes.
- [ ] Reject duplicate/unexpected artifacts and any GameTest class leakage.
- [ ] Capture lock/verification RED and update generated supply-chain metadata.
- [ ] Verify the staging bundle in CI.

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
