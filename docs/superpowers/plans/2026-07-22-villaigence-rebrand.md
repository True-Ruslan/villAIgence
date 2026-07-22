# VillAIgence Rebrand Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebrand the public mod from LivingWorld/MCA-fork wording to VillAIgence without changing compatibility-sensitive mod IDs, Java namespaces, configuration paths, world data paths, or serialized formats.

**Architecture:** Treat VillAIgence as the public product layer and LivingWorld as the internal engine namespace. Change only public-facing copy, packaging/release names, metadata where safe, and documentation. Preserve `mca`, `net.conczin.mca`, `config/livingworld.json`, and `<world>/livingworld/`.

**Tech Stack:** Java 21, Gradle, Fabric/NeoForge 1.21.1, GitHub Actions, Markdown, Bash.

## Global Constraints

- Public product name is `VillAIgence`.
- Wordplay form is `Vill-AI-gence`.
- Short name is `VAI`.
- Tagline is `Giving villagers a mind of their own.`
- Mod id remains exactly `mca`.
- Java namespace remains exactly `net.conczin.mca`.
- Config path remains exactly `config/livingworld.json`.
- World data remains under exactly `<world>/livingworld/`.
- Existing release tags/assets are not mutated.
- New public artifact filename is `villaigence-fabric-<tag>.jar`.

---

### Task 1: Public repository identity

**Files:**
- Modify: `README.md`
- Modify: `docs/livingworld/CONFIGURATION.md`
- Modify: `docs/livingworld/VOICE.md`

**Interfaces:**
- Consumes existing installation/configuration semantics.
- Produces a consistent VillAIgence public vocabulary while explicitly documenting LivingWorld as internal engine naming.

- [ ] Rewrite README title and opening sections around VillAIgence.
- [ ] Preserve upstream MCA attribution and the warning that original MCA Reborn must not be installed simultaneously.
- [ ] Update user-facing docs to say VillAIgence when referring to the mod/product and LivingWorld only when referring to internal paths/classes/config names.
- [ ] Verify every documented path remains unchanged.

### Task 2: Release and artifact branding

**Files:**
- Modify: `.github/workflows/livingworld-release.yml`
- Modify: `scripts/ci/package-livingworld-release.sh`
- Modify: `scripts/ci/verify-livingworld-fabric-package.sh` if public filename assumptions exist.
- Modify: `docs/RELEASING.md`

**Interfaces:**
- Produces `villaigence-fabric-<tag>.jar` and `.sha256` for new releases.
- Keeps tag validation and JAR content verification unchanged.

- [ ] Change workflow display/release copy from LivingWorld product naming to VillAIgence while keeping workflow file path stable.
- [ ] Change only public artifact filename conventions; do not alter internal mod id checks.
- [ ] Update release notes and runbook examples.
- [ ] Keep old releases immutable and document filename transition.

### Task 3: Safe metadata/user-facing strings

**Files:**
- Inspect/modify loader metadata and Gradle-visible display names only where they do not alter mod id or dependency identity.
- Do not rename Java packages/classes or persistent paths.

**Interfaces:**
- Public display name becomes VillAIgence where safely supported.
- Runtime identity remains `mca`.

- [ ] Audit Fabric/NeoForge metadata for public `name`/description fields.
- [ ] Update display-only metadata to VillAIgence.
- [ ] Add/adjust tests or packaging smoke assertions if they depend on product-facing names.

### Task 4: Verification and merge

**Files:**
- No new runtime files unless verification exposes a defect.

- [ ] Run `./gradlew :common:test :fabric:build --no-daemon` via CI.
- [ ] Require official Fabric + NeoForge CI green on exact final head.
- [ ] Require release/package smoke-check green.
- [ ] Review diff for accidental changes to `mca`, `net.conczin.mca`, `config/livingworld.json`, `<world>/livingworld/`, or serialized field names.
- [ ] Merge only after all checks pass.
