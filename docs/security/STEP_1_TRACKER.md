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
- [x] Validate the planning PR with both required CI workflows.
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
- [ ] Add the official Gradle wrapper distribution checksum.
- [ ] Add Gradle wrapper validation to CI.
- [ ] Commit dependency verification metadata.
- [ ] Enable dependency locking where compatible.
- [ ] Restrict third-party Maven repository content.
- [ ] Pin GitHub Actions to full commit SHAs.
- [ ] Produce an SBOM or equivalent dependency manifest for releases.
- [ ] Document dependency-update and metadata-refresh procedure.
- [ ] Record closing commits and evidence for SEC-005.

## H4 — CI security coverage and build matrix

- [ ] Add NeoForge build to required CI.
- [ ] Retain common tests and Fabric packaging checks.
- [ ] Add secret scanning.
- [ ] Add Java/Gradle static security analysis.
- [ ] Add deterministic recursive script inventory.
- [ ] Block undocumented executable or network-capable utilities.
- [ ] Keep CI permissions least-privilege.
- [ ] Keep release write access isolated to the release job.
- [ ] Record closing commits and evidence for SEC-006 and part of SEC-009.

## H5 — Legacy tools cleanup and audit closure

- [ ] Generate the recursive manifest for the closing commit.
- [ ] Classify every executable and network-capable script.
- [ ] Remove `scripts/pirate_translator.py`, unless a documented maintenance need is proven.
- [ ] If retained, harden and isolate the translator with mocked-network tests.
- [ ] Confirm no unexpected Python invocation from build, CI or release paths.
- [ ] Add an approved script inventory document.
- [ ] Rerun whole-tree secret and dependency scans.
- [ ] Update the audit with exact closing commits and residual risk.
- [ ] Record closing evidence for SEC-008 and SEC-009.

## Final CI and live validation

- [ ] Common unit tests pass.
- [ ] Fabric build/package passes.
- [ ] NeoForge build passes.
- [ ] Wrapper and dependency verification pass.
- [ ] Secret/static/script inventory checks pass.
- [ ] Standard OpenRouter/OpenAI configuration works.
- [ ] Invalid HTTP/lookalike endpoints fail safely.
- [ ] Explicit loopback development mode works as documented.
- [ ] Text Chat persists exactly once.
- [ ] Voice STT/TTS pipeline remains operational.
- [ ] TTS failure preserves text output.
- [ ] Logs contain no credentials, authorization headers, prompts or transcripts.
- [ ] Persistent world files remain stable across restart where no mutation is expected.
- [ ] Release JAR, checksum and dependency manifest are retained as validation evidence.

## Documentation closure

- [ ] Update `docs/security/SECURITY_AUDIT_2026-07-31.md` finding statuses.
- [ ] Update `docs/PROJECT_STATE.md`.
- [ ] Update `docs/CHANGELOG.md`.
- [ ] Add the hardening validation document.
- [ ] Mark Step 1 complete only after code, CI and live evidence all exist.
