# Exact Release Identity and Corrective Candidate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Guarantee that a VillAIgence release JAR embeds the exact release tag in both `fabric.mod.json` and `META-INF/MANIFEST.MF`, then publish the next corrective candidate containing the merged water-navigation and filled-grave fixes.

**Architecture:** Gradle will accept a validated explicit `release_version` property for tag builds while preserving tag discovery and snapshot behavior for ordinary development builds. The packaging boundary will inspect the built JAR and fail closed when Fabric metadata and manifest versions are missing, disagree, or do not equal the requested release tag. A standalone shell regression test will exercise both the Gradle version input and valid/invalid package cases.

**Tech Stack:** Gradle/Groovy, Bash, Python 3 JSON parsing, Java `jar`/`unzip`, GitHub Actions, Fabric 1.21.1, Java 21.

## Global Constraints

- Base branch: `1.21.1` after merged PR #100.
- Corrective release version: `0.1.21+1.21.1`.
- Preserve internal mod ID `mca` and compatibility-sensitive LivingWorld paths.
- Include both merged fixes: water-navigation PR #99 and filled-grave PR #100.
- Do not change gameplay, memory, persistence schemas, providers, packets, dependencies or loader requirements in this package.
- Development builds without a release tag remain `<minecraft_version>-SNAPSHOT`.
- Release builds must not derive identity solely from ambient local Git state.
- Release packaging must reject mismatched or snapshot embedded versions.
- Installed acceptance remains pending until the exact released JAR is tested.

---

### Task 1: Establish the executable RED contract

**Files:**
- Create: `scripts/ci/test-release-version-contract.sh`
- Modify: `.github/workflows/livingworld-ci.yml`

**Interfaces:**
- Consumes: Gradle property `release_version`, `scripts/ci/package-livingworld-release.sh`.
- Produces: executable regression proof for explicit Gradle identity and embedded package identity.

- [ ] **Step 1: Add a Gradle identity assertion**

Run `./gradlew -q properties -Prelease_version=<test-version>` and require the root project `version:` value to equal the supplied version exactly.

- [ ] **Step 2: Add an invalid release JAR fixture**

Build a temporary JAR named for the requested release but containing `1.21.1-SNAPSHOT` in both `fabric.mod.json` and `Implementation-Version`. Require packaging to fail and identify an embedded-version mismatch.

- [ ] **Step 3: Add a valid release JAR fixture**

Build the same temporary structure with exact embedded release identity. Require packaging to succeed and preserve the exact version in both metadata locations.

- [ ] **Step 4: Wire the script into VillAIgence CI**

Execute it before the main build so release-identity failures are isolated from loader compilation failures.

- [ ] **Step 5: Run the RED gate**

Expected initial failure: Gradle ignores `release_version` and reports `1.21.1-SNAPSHOT`.

### Task 2: Add explicit validated Gradle release identity

**Files:**
- Modify: `build.gradle`

**Interfaces:**
- Consumes: optional `-Prelease_version=<mod-version>+<minecraft-version>`.
- Produces: exact `project.version` for all subprojects and resource expansion.

- [ ] **Step 1: Prefer explicit release identity**

Resolve a nonblank `release_version` before Git tag discovery.

- [ ] **Step 2: Validate format and Minecraft suffix**

Require the existing release-tag format and require the suffix after `+` to equal `minecraft_version`.

- [ ] **Step 3: Preserve development fallback**

When no explicit value and no unique tag exist, keep `<minecraft_version>-SNAPSHOT`.

### Task 3: Make packaging fail closed on embedded identity

**Files:**
- Modify: `scripts/ci/package-livingworld-release.sh`

**Interfaces:**
- Consumes: packaged JAR and requested `artifact_label`.
- Produces: verified release package only when Fabric metadata and manifest identity agree.

- [ ] **Step 1: Extract Fabric metadata version**

Read `fabric.mod.json` from the copied output JAR and parse `.version` with Python 3.

- [ ] **Step 2: Extract manifest version**

Read `Implementation-Version` from `META-INF/MANIFEST.MF` with CRLF-safe parsing.

- [ ] **Step 3: Enforce common identity**

Require both values to be nonblank and equal for every packaged JAR.

- [ ] **Step 4: Enforce exact release tag**

For `is_release=true`, additionally require both values to equal `artifact_label` exactly.

### Task 4: Pass release identity explicitly in GitHub Actions

**Files:**
- Modify: `.github/workflows/livingworld-release.yml`

**Interfaces:**
- Consumes: validated `GITHUB_REF_NAME` on tag pushes.
- Produces: Gradle build invoked with `-Prelease_version=${GITHUB_REF_NAME}`.

- [ ] **Step 1: Run the regression contract in release CI**

Execute `scripts/ci/test-release-version-contract.sh` after Gradle setup.

- [ ] **Step 2: Supply the exact tag only for tag pushes**

Append `-Prelease_version=${GITHUB_REF_NAME}` to the Gradle invocation when `github.event_name == 'push'`.

- [ ] **Step 3: Preserve PR dry-run behavior**

Do not supply a release identity to pull-request or manual dry-run builds unless they run on a release tag.

### Task 5: Verify, merge and publish the corrective candidate

**Files:**
- Create: `docs/livingworld/VALIDATION_0.1.21_CORRECTIVE_CANDIDATE.md`

**Interfaces:**
- Consumes: exact merged commit, CI runs, generated release assets.
- Produces: canonical identity and installed-test procedure for `0.1.21+1.21.1`.

- [ ] **Step 1: Require exact-head CI success**

Require VillAIgence CI, Java PR CI, repository security policy and release dry-run/package checks.

- [ ] **Step 2: Merge only the scoped package**

Confirm the diff contains release-identity code/tests/workflows/documentation only.

- [ ] **Step 3: Create tag `0.1.21+1.21.1` on current `1.21.1` head**

Do not reuse or overwrite `0.1.20+1.21.1`.

- [ ] **Step 4: Verify official assets**

Record commit, JAR filename, byte size, SHA-256, embedded Fabric version and manifest version.

- [ ] **Step 5: Keep live status pending**

Do not mark water escape, grave round-trip, restart or cumulative gameplay as PASS until the operator tests the exact official JAR.
