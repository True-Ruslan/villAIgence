# Step 1 Security Hardening Tracker

**Status:** not started  
**Plan:** [`STEP_1_SECURITY_SUPPLY_CHAIN_HARDENING.md`](STEP_1_SECURITY_SUPPLY_CHAIN_HARDENING.md)  
**Audit:** [`SECURITY_AUDIT_2026-07-31.md`](SECURITY_AUDIT_2026-07-31.md)

GitHub Issues are disabled for this repository, so this versioned checklist is the canonical execution tracker.

## Planning and evidence

- [x] Record the dated audit against an exact commit.
- [x] Correct the earlier claim that the repository contained no Python scripts.
- [x] Document audit coverage limitations.
- [x] Define ordered work packages, tests, rollout and closure criteria.
- [ ] Merge the planning PR.

## H1 — Provider endpoint and credential policy

- [ ] Add a centralized validated endpoint/origin model.
- [ ] Require HTTPS for non-loopback providers.
- [ ] Permit HTTP only for explicit loopback development mode.
- [ ] Bind Chat, STT and TTS credentials to their selected endpoints.
- [ ] Remove custom-endpoint fallback to an unrelated main API key.
- [ ] Replace substring hostname trust checks with normalized exact-host rules.
- [ ] Prevent redirects from leaking authorization or trusted metadata.
- [ ] Add negative tests for malformed, lookalike, IDN and user-info URIs.
- [ ] Record closing commits and evidence for SEC-001 and SEC-002.

## H2 — Bounded network I/O and SSRF removal

- [ ] Introduce shared bounded response readers.
- [ ] Add independent Chat, STT, TTS and error-body limits.
- [ ] Enforce limits for both declared and chunked responses.
- [ ] Add strict connect, read and total-operation timeouts.
- [ ] Delete `OpenAIChatAI.verify(String encodedURL)` if unused.
- [ ] Otherwise constrain it with scheme, host, address and redirect policy.
- [ ] Clamp `voiceMaxSeconds` to a documented range.
- [ ] Add an aggregate active PCM budget.
- [ ] Verify oversized TTS preserves the valid text reply.
- [ ] Verify retries cannot duplicate actions or persistence effects.
- [ ] Record closing commits and evidence for SEC-003, SEC-004 and SEC-007.

## H3 — Supply-chain verification

- [ ] Move Fabric Loom from snapshot to a stable compatible release.
- [ ] Add the official Gradle wrapper distribution SHA-256.
- [ ] Add Gradle wrapper validation.
- [ ] Add dependency verification metadata.
- [ ] Add dependency locking where compatible.
- [ ] Restrict Maven repositories with content filters.
- [ ] Pin third-party GitHub Actions to full commit SHAs.
- [ ] Generate a release dependency inventory or SBOM.
- [ ] Document controlled dependency updates.
- [ ] Record closing commits and evidence for SEC-005.

## H4 — CI security coverage and supported build matrix

- [ ] Add NeoForge build coverage to required CI.
- [ ] Retain common tests and Fabric package verification.
- [ ] Add secret scanning.
- [ ] Add maintained Java/Gradle static analysis.
- [ ] Add deterministic complete-tree script/workflow inventory checks.
- [ ] Keep CI permissions least-privilege.
- [ ] Keep release write permission isolated to the release job.
- [ ] Confirm documentation-only PR performance remains acceptable.
- [ ] Record closing commits and evidence for SEC-006 and automated SEC-009 controls.

## H5 — Legacy tools cleanup and audit closure

- [ ] Generate a recursive manifest for the exact repository commit.
- [ ] Classify all executable and network-capable scripts.
- [ ] Remove `scripts/pirate_translator.py` if it is not required.
- [ ] Otherwise isolate and harden it with timeout, size limit, bounded concurrency and a disclosure warning.
- [ ] Confirm no unexpected Python invocation in build, CI or release paths.
- [ ] Rerun complete-tree secret and dependency scans.
- [ ] Update the dated audit with exact closing commits and residual risks.
- [ ] Record final states for SEC-008 and SEC-009.

## Cross-cutting validation

- [ ] Common tests pass.
- [ ] Fabric build and release-package checks pass.
- [ ] NeoForge build passes.
- [ ] Wrapper and dependency verification pass from a fresh cache.
- [ ] Hostile-response integration tests pass.
- [ ] No secrets or authorization data appear in logs.
- [ ] Release JAR, checksum and dependency inventory are generated together.
- [ ] Controlled Minecraft 1.21.1 server smoke test passes.
- [ ] Runtime configuration migration notes are complete.
- [ ] `docs/PROJECT_STATE.md` is updated.
- [ ] `docs/CHANGELOG.md` is updated.
- [ ] The security audit is updated with evidence and residual risk.

## Completion

- [ ] SEC-001 has a final evidence-backed state.
- [ ] SEC-002 has a final evidence-backed state.
- [ ] SEC-003 has a final evidence-backed state.
- [ ] SEC-004 has a final evidence-backed state.
- [ ] SEC-005 has a final evidence-backed state.
- [ ] SEC-006 has a final evidence-backed state.
- [ ] SEC-007 has a final evidence-backed state.
- [ ] SEC-008 has a final evidence-backed state.
- [ ] SEC-009 has a final evidence-backed state.
- [ ] Step 1 is marked complete in the project state only after live validation.

## Separate existing obligation

The pending live validation of deterministic Semantic Memory forgetting/decay remains separate. This tracker must not mark that release checkpoint complete.
