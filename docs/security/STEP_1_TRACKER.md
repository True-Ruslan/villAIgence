# Step 1 Security Hardening Tracker

**Status:** H1 and H2 implemented and automated-CI validated; real-server smoke pending  
**Plan:** [`STEP_1_SECURITY_SUPPLY_CHAIN_HARDENING.md`](STEP_1_SECURITY_SUPPLY_CHAIN_HARDENING.md)  
**Audit:** [`SECURITY_AUDIT_2026-07-31.md`](SECURITY_AUDIT_2026-07-31.md)  
**H2 evidence:** [`H2_BOUNDED_NETWORK_HARDENING_2026-07-31.md`](H2_BOUNDED_NETWORK_HARDENING_2026-07-31.md)

GitHub Issues are disabled for this repository, so this versioned checklist is the canonical execution tracker.

## Planning and evidence

- [x] Record the dated audit against an exact commit.
- [x] Correct the earlier claim that the repository contained no Python scripts.
- [x] Document audit coverage limitations.
- [x] Define ordered work packages, tests, rollout and closure criteria.
- [x] Validate and merge planning PR #58 at `cd7343fff75bb2ed21a3fa74f63743712388bd7d`.

## H1 — Provider endpoint and credential policy

- [x] Add a centralized validated endpoint/origin model.
- [x] Require HTTPS for non-loopback providers.
- [x] Permit HTTP only for explicit loopback development mode.
- [x] Bind Chat, STT and TTS credentials to their selected endpoints.
- [x] Remove custom-endpoint fallback to an unrelated main API key.
- [x] Replace substring hostname trust checks with normalized exact-host rules.
- [x] Prevent redirects from leaking authorization or trusted metadata.
- [x] Add negative tests for malformed, lookalike, IDN and user-info URIs.
- [x] Record automated implementation evidence for SEC-001 and SEC-002.
- [ ] Complete real-server smoke validation before marking SEC-001 and SEC-002 Closed.

### H1 evidence

```text
endpoint policy RED
8d869863858eca558377e4cbf51024a6004f2a5d
VillAIgence CI #774 — expected FAILURE

endpoint policy GREEN
99a632aa3f855467c94691152d703bcea3967891
VillAIgence CI #776 — SUCCESS
Java Pull Request CI #316 — SUCCESS

credential binding RED
1d9edbc6e8643cc36b96f3bc3ab0a212d08534c7
VillAIgence CI #777 — expected FAILURE

credential binding GREEN
7148eca77edde8f3d1fac1c1b3059eab2ae0d08d
VillAIgence CI #778 — SUCCESS
Java Pull Request CI #318 — SUCCESS

configuration integration GREEN
c7d4c38313f683cb7be282c4f104107d123e0811
VillAIgence CI #783 — SUCCESS
Java Pull Request CI #323 — SUCCESS

final H1 automated GREEN
4fe1a12ed9627afe30573e1ad2ce458699c82105
VillAIgence CI #797 — SUCCESS
Java Pull Request CI #337 — SUCCESS

H1 merge
787f1a781b5970d4bafb851bfb3c7cba7c21fc0a
```

## H2 — Bounded network I/O and SSRF removal

- [x] Introduce shared bounded response readers.
- [x] Add independent Chat, STT, TTS, verification and error-body limits.
- [x] Enforce limits for both declared and chunked/unknown-length responses.
- [x] Retain strict connect and socket-read timeouts.
- [x] Add a hard total provider body-read deadline against slow-drip responses.
- [x] Remove `OpenAIChatAI.verify(String encodedURL)`.
- [x] Replace it with trusted-origin-only `AccountVerificationClient`.
- [x] Confirm and document the active legacy `/mca verify` call site missed by the initial audit.
- [x] Disable redirects for verification and authenticated provider requests.
- [x] Clamp microphone capture to a documented runtime range.
- [x] Add an aggregate active PCM budget.
- [x] Reserve PCM bytes before buffering and release them on all lifecycle paths.
- [x] Release PCM reservation even if decoder cleanup throws.
- [x] Verify oversized TTS fails without changing the valid text-response boundary.
- [x] Preserve retry behavior so provider reads cannot duplicate actions or persistence effects.
- [x] Record automated implementation evidence for SEC-003, SEC-004 and SEC-007.
- [ ] Merge PR #60.
- [ ] Complete controlled real-server validation before marking SEC-003, SEC-004 and SEC-007 Closed.

### H2 final limits

```text
Chat JSON:                 8 MiB
STT JSON:                  4 MiB
TTS audio:                64 MiB
provider error body:     256 KiB
account verification:     64 KiB
provider body-read time:  10 minutes
voice capture/session:     1..120 seconds
global active PCM:        128 MiB
```

### H2 TDD and CI evidence

```text
bounded-reader RED
85a88b9fceb7553c3a04f9c1e54f19ad020c3c2d
VillAIgence CI #801 / 30593290408 — expected FAILURE
reason: BoundedResponseReader did not exist

bounded-reader GREEN
94fe1c03a05c7c85fa0a112b963b4cfe96754496
VillAIgence CI #803 / 30593479219 — SUCCESS
Java Pull Request CI #342 / 30593479217 — SUCCESS

widened-limit GREEN checkpoint
8a2116191b484810b8b20ef0a476ecaede1c0dc7
VillAIgence CI #821 / 30618378645 — SUCCESS
Java Pull Request CI #360 / 30618378582 — SUCCESS

PCM release hardening
cb5ed05c66b1015abc2b951c4fd1987742d651cd
source regression guard
19c4a667a47fba63694410e8c0cdb6899228c615

total body-read deadline RED
5d49462fa81a2c12c7e8d2894d18eeedb9c9331c
VillAIgence CI #825 / 30625004367 — expected FAILURE
reason: deadline API and exception did not exist

total body-read deadline GREEN
d7291d277e9bfe9745974abdcdc69569567e3a96
VillAIgence CI #826 / 30625132186 — SUCCESS
Java Pull Request CI #365 / 30625131764 — SUCCESS
```

Automated H2 coverage includes:

- declared-length rejection before reading;
- chunked and unknown-length rejection on the first excess byte;
- total body-read deadline for slow-drip streams;
- safe exceptions without provider payload content;
- oversized STT and TTS local-server integration cases;
- removal of unbounded Chat/Audio whole-stream helpers;
- trusted Conczin HTTPS verification boundary with fixed path and no redirects;
- atomic PCM reservation, exhaustion, release and concurrency;
- release in `finally` when decoder cleanup fails;
- common tests, Fabric package verification, Fabric compilation and NeoForge compilation.

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
- [x] Retain common tests and Fabric packaging checks during H1/H2.
- [ ] Add secret scanning.
- [ ] Add Java/Gradle static security analysis.
- [ ] Add deterministic recursive script inventory.
- [ ] Block undocumented executable or network-capable utilities.
- [x] Keep CI permissions least-privilege during H1/H2.
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

- [x] Common unit tests pass for H1/H2.
- [x] Fabric build/package passes for H1/H2.
- [x] NeoForge and Fabric compilation pass through Java Pull Request CI for H1/H2.
- [ ] Wrapper and dependency verification pass after H3.
- [ ] Secret/static/script inventory checks pass after H4/H5.
- [ ] Standard OpenRouter/OpenAI configuration works on a real server with H1/H2.
- [ ] Invalid HTTP/lookalike endpoints fail safely.
- [ ] Explicit loopback development mode works only with opt-in.
- [ ] Oversized declared and chunked provider responses fail safely.
- [ ] Slow-drip provider responses terminate at the total deadline.
- [ ] Text Chat persists exactly once.
- [ ] Voice STT/TTS pipeline remains operational.
- [ ] TTS failure preserves text output.
- [ ] Concurrent voice capture remains stable under the global PCM budget.
- [ ] Logs contain no credentials, authorization headers, prompts or transcripts.
- [ ] Persistent world files remain stable across restart where no mutation is expected.
- [ ] Release JAR, checksum and dependency manifest are retained as validation evidence.

## Documentation closure

- [x] Add the dated H2 implementation/evidence record.
- [x] Correct the audit record through the H2 follow-up for the active `/mca verify` path.
- [ ] Update finding statuses after H1/H2 merge and live smoke.
- [ ] Update `docs/PROJECT_STATE.md` after implementation and validation boundaries are reconciled.
- [ ] Update `docs/CHANGELOG.md` after implementation and validation.
- [ ] Add the combined H1/H2 real-server validation document.
- [ ] Mark Step 1 complete only after code, CI and live evidence all exist.
