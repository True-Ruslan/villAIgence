# Step 1 Security Hardening Tracker

**Status:** H1 implemented and automated-CI validated; real-server smoke pending  
**Plan:** [`STEP_1_SECURITY_SUPPLY_CHAIN_HARDENING.md`](STEP_1_SECURITY_SUPPLY_CHAIN_HARDENING.md)  
**Audit:** [`SECURITY_AUDIT_2026-07-31.md`](SECURITY_AUDIT_2026-07-31.md)

GitHub Issues are disabled for this repository, so this versioned checklist is the canonical execution tracker.

## Planning and evidence

- [x] Record the dated audit against an exact commit.
- [x] Correct the earlier claim that the repository contained no Python scripts.
- [x] Document audit coverage limitations.
- [x] Define ordered work packages, tests, rollout and closure criteria.
- [x] Validate the planning PR with both required CI workflows.
- [x] Merge planning PR #58 at `cd7343fff75bb2ed21a3fa74f63743712388bd7d`.

## H1 — Provider endpoint and credential policy

- [x] Add a centralized validated endpoint/origin model.
- [x] Require HTTPS for non-loopback providers.
- [x] Permit HTTP only for explicit loopback development mode.
- [x] Bind Chat, STT and TTS credentials to their selected endpoints.
- [x] Remove custom-endpoint fallback to an unrelated main API key.
- [x] Replace substring hostname trust checks with normalized exact-host rules.
- [x] Prevent redirects from leaking authorization or trusted metadata.
- [x] Add negative tests for malformed, lookalike, IDN and user-info URIs.
- [x] Record automated closing evidence for SEC-001 and SEC-002.
- [ ] Complete real-server smoke validation before marking SEC-001 and SEC-002 closed.

### H1 TDD and CI evidence

```text
endpoint policy RED:
8d869863858eca558377e4cbf51024a6004f2a5d
VillAIgence CI #774 / 30589948913 — expected FAILURE
reason: ProviderEndpoint and ProviderEndpointPolicy did not exist

endpoint policy GREEN:
99a632aa3f855467c94691152d703bcea3967891
VillAIgence CI #776 / 30590173445 — SUCCESS
Java Pull Request CI #316 / 30590173506 — SUCCESS

credential binding RED:
1d9edbc6e8643cc36b96f3bc3ab0a212d08534c7
VillAIgence CI #777 / 30590326551 — expected FAILURE
reason: ProviderCredentialBinding did not exist

credential binding GREEN:
7148eca77edde8f3d1fac1c1b3059eab2ae0d08d
VillAIgence CI #778 / 30590518187 — SUCCESS
Java Pull Request CI #318 / 30590518186 — SUCCESS

configuration integration RED:
3c6ee0f5e7366c1fb1b91f917b200a27d238ea15
VillAIgence CI #779 — expected FAILURE
reason: secure endpoint bindings and loopback opt-in were absent

configuration integration GREEN checkpoint:
c7d4c38313f683cb7be282c4f104107d123e0811
VillAIgence CI #783 / 30591313740 — SUCCESS
Java Pull Request CI #323 / 30591313750 — SUCCESS

HTTP/trusted-context RED:
4a370c636df591c4ae25d6dc77bb41728426ea7a
VillAIgence CI #787 / 30591587389 — expected FAILURE
reason: callers still accepted raw endpoint strings and lacked the new no-redirect/test contracts

final H1 automated GREEN:
4fe1a12ed9627afe30573e1ad2ce458699c82105
VillAIgence CI #797 / 30592651839 — SUCCESS
Java Pull Request CI #337 / 30592651847 — SUCCESS
```

Automated H1 coverage includes:

- normalized URI and provider-family classification;
- remote HTTP rejection;
- explicit lexical-loopback HTTP opt-in;
- exact/subdomain host boundaries and lookalike rejection;
- Chat/STT/TTS credential-family binding;
- custom audio endpoint dedicated-key requirement;
- authenticated Audio redirect leakage integration test;
- Chat source-boundary guard for validated endpoint use, exact trust and disabled redirects;
- common tests and Fabric packaging;
- NeoForge and Fabric compilation through the Java PR workflow.

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

- [ ] Add NeoForge build to required primary CI.
- [x] Retain common tests and Fabric packaging checks during H1.
- [ ] Add secret scanning.
- [ ] Add Java/Gradle static security analysis.
- [ ] Add deterministic recursive script inventory.
- [ ] Block undocumented executable or network-capable utilities.
- [x] Keep CI permissions least-privilege during H1.
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

- [x] Common unit tests pass for H1.
- [x] Fabric build/package passes for H1.
- [x] NeoForge build passes for H1 through Java Pull Request CI.
- [ ] Wrapper and dependency verification pass after H3.
- [ ] Secret/static/script inventory checks pass after H4/H5.
- [ ] Standard OpenRouter/OpenAI configuration works on a real server with H1.
- [ ] Invalid HTTP/lookalike endpoints fail safely in an operator smoke test.
- [ ] Explicit loopback development mode works as documented in an operator smoke test.
- [ ] Text Chat persists exactly once.
- [ ] Voice STT/TTS pipeline remains operational.
- [ ] TTS failure preserves text output.
- [ ] Logs contain no credentials, authorization headers, prompts or transcripts.
- [ ] Persistent world files remain stable across restart where no mutation is expected.
- [ ] Release JAR, checksum and dependency manifest are retained as validation evidence.

## Documentation closure

- [ ] Update `docs/security/SECURITY_AUDIT_2026-07-31.md` finding statuses after H1 merge and live smoke.
- [ ] Update `docs/PROJECT_STATE.md` after H1 merge and validation boundary is reconciled.
- [ ] Update `docs/CHANGELOG.md` after H1 merge.
- [ ] Add the H1 hardening validation document after real-server smoke.
- [ ] Mark Step 1 complete only after code, CI and live evidence all exist.
