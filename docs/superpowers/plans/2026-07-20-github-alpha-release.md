# GitHub-only Alpha Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the inherited upstream publisher with a verified GitHub-only Fabric alpha release pipeline for LivingWorld.

**Architecture:** Keep PR CI separate from publishing. The release workflow dry-runs packaging on workflow/documentation PRs and publishes only from a validated tag pointing at the current `1.21.1` head. Build output is smoke-checked, renamed, checksummed, uploaded as an Actions artifact, then attached to a GitHub Release.

**Tech Stack:** GitHub Actions, Bash, GitHub CLI, Gradle, Java 21, Fabric 1.21.1.

## Global Constraints

- First release tag format: `0.1.0-alpha.1+1.21.1`.
- Fabric-only release artifact.
- No Maven publication.
- No Modrinth/CurseForge publication or upstream project IDs.
- No external publishing secrets; only repository `GITHUB_TOKEN`.
- Release tag commit must equal current `1.21.1` branch head.
- `:common:test` and `:fabric:build` must pass before release creation.
- JAR smoke validation and SHA-256 generation are mandatory.

---

### Task 1: Replace inherited release workflow

**Files:**
- Modify: `.github/workflows/gradle.yml`

**Produces:** A GitHub-only release workflow triggered by release tags and dry-run PR changes.

- [ ] Preserve strict tag validation for `<mod_version>+<minecraft_version>`.
- [ ] Add PR dry-run trigger limited to release workflow/documentation paths.
- [ ] Checkout with full tag history and use Java 21.
- [ ] On tag builds, fetch `1.21.1` and fail unless tag commit equals branch head.
- [ ] Run `./gradlew :common:test :fabric:build --stacktrace --no-daemon`.
- [ ] Resolve exact release JAR from tag build and a single non-sources Fabric JAR for PR dry-runs.
- [ ] Verify `fabric.mod.json`, `LivingWorldConfig.class`, and Fabric `VoiceConversationService.class` exist in the archive.
- [ ] Rename public output to `mca-livingworld-fabric-<version>.jar` and generate `.sha256`.
- [ ] Upload JAR/checksum with `actions/upload-artifact`.
- [ ] Add a tag-only release job using `actions/download-artifact` and `gh release create`.
- [ ] Mark alpha/beta/rc versions as GitHub prereleases.
- [ ] Remove Maven, Modrinth and CurseForge IDs/tokens/actions entirely.

### Task 2: Add operator release documentation

**Files:**
- Create: `docs/RELEASING.md`

**Produces:** Exact release checklist for the project owner.

- [ ] Document preconditions: merged `1.21.1`, green CI, no original MCA JAR installed alongside fork.
- [ ] Document exact first tag `0.1.0-alpha.1+1.21.1`.
- [ ] Provide both GitHub UI and Git CLI tag creation paths.
- [ ] Document produced GitHub Release assets and checksum.
- [ ] Document Fabric API + Simple Voice Chat dependencies and Java 21.
- [ ] Document API-key configuration and rollback/tag deletion guidance for failed releases.

### Task 3: Verify through PR CI and merge

**Files:**
- Review: `.github/workflows/gradle.yml`
- Review: `docs/RELEASING.md`

- [ ] Open a focused PR into `1.21.1`.
- [ ] Confirm existing LivingWorld CI passes.
- [ ] Confirm official Fabric/NeoForge Gradle PR CI passes.
- [ ] Confirm release workflow PR dry-run succeeds and produces the Fabric package artifact.
- [ ] Inspect dry-run artifact presence before merge.
- [ ] Merge only after all required checks are green.
- [ ] Do not create the actual release tag automatically; leave tag creation as the explicit release action after merge.
