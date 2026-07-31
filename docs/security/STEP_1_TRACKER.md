# Step 1 Security Hardening Tracker

**Status:** H1–H3 merged; H4 implementation and automated validation complete in PR #62; H1/H2 real-server validation and H5 legacy-tool closure remain  
**Plan:** [`STEP_1_SECURITY_SUPPLY_CHAIN_HARDENING.md`](STEP_1_SECURITY_SUPPLY_CHAIN_HARDENING.md)  
**Audit:** [`SECURITY_AUDIT_2026-07-31.md`](SECURITY_AUDIT_2026-07-31.md)  
**Security index:** [`README.md`](README.md)

GitHub Issues are disabled for this repository, so this versioned checklist is the canonical execution tracker. Detailed TDD logs and run identifiers live in the linked dated evidence documents rather than being duplicated here.

## Planning and evidence

- [x] Record the dated audit against an exact commit.
- [x] Correct the earlier claim that the repository contained no Python scripts.
- [x] Document audit coverage limitations.
- [x] Define ordered work packages, tests, rollout and closure criteria.
- [x] Merge planning PR #58 at `cd7343fff75bb2ed21a3fa74f63743712388bd7d`.

## H1 — Provider endpoint and credential policy

**Merge:** PR #59 — `787f1a781b5970d4bafb851bfb3c7cba7c21fc0a`  
**Operator guide:** [`../livingworld/PROVIDER_ENDPOINT_SECURITY.md`](../livingworld/PROVIDER_ENDPOINT_SECURITY.md)

- [x] Add a centralized validated endpoint/origin model.
- [x] Require HTTPS for non-loopback providers.
- [x] Permit HTTP only for explicit loopback development mode.
- [x] Bind Chat, STT and TTS credentials to their selected endpoints.
- [x] Remove custom-endpoint fallback to an unrelated main API key.
- [x] Replace substring hostname trust checks with normalized exact-host rules.
- [x] Prevent redirects from leaking authorization or trusted metadata.
- [x] Add malformed, lookalike, IDN, user-info and redirect regression tests.
- [x] Merge implementation and automated evidence for SEC-001/SEC-002.
- [ ] Complete controlled real-server smoke validation.
- [ ] Close SEC-001 and SEC-002 in a dated audit follow-up.

## H2 — Bounded network I/O and SSRF removal

**Merge:** PR #60 — `15c56526417ac7dfb76567d51d1aa107f522cda7`  
**Evidence:** [`H2_BOUNDED_NETWORK_HARDENING_2026-07-31.md`](H2_BOUNDED_NETWORK_HARDENING_2026-07-31.md)

- [x] Introduce shared bounded response readers.
- [x] Add independent Chat, STT, TTS, verification and error-body limits.
- [x] Enforce limits for declared, chunked and unknown-length responses.
- [x] Retain strict connect/socket timeouts and add total body-read deadline.
- [x] Remove arbitrary-URL verification helper.
- [x] Replace it with trusted-origin-only account verification.
- [x] Confirm and document the active legacy `/mca verify` call site.
- [x] Disable redirects for verification and authenticated provider requests.
- [x] Clamp microphone capture duration.
- [x] Add aggregate active PCM budget and atomic reservation/release.
- [x] Preserve text output and exactly-once side-effect boundaries on failures/retries.
- [x] Merge automated evidence for SEC-003, SEC-004 and SEC-007.
- [ ] Complete controlled real-server validation.
- [ ] Close runtime-sensitive findings in a dated audit follow-up.

### H2 enforced limits

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

## H3 — Supply-chain verification

**Merge:** PR #61 — `4cf9aef2e5c31a5682a7cad8544219154330e056`  
**Evidence:** [`H3_SUPPLY_CHAIN_HARDENING_2026-07-31.md`](H3_SUPPLY_CHAIN_HARDENING_2026-07-31.md)  
**Audit closure:** [`SECURITY_AUDIT_FOLLOW_UP_2026-07-31_H3.md`](SECURITY_AUDIT_FOLLOW_UP_2026-07-31_H3.md)

- [x] Move Fabric Loom from snapshot to stable `1.17.17`.
- [x] Add official Gradle 9.6.1 wrapper distribution checksum.
- [x] Enable Gradle wrapper validation in CI/release workflows.
- [x] Commit dependency verification metadata with SHA-256 checksums.
- [x] Enable dependency locking and commit all project lockfiles.
- [x] Restrict third-party Maven repository content.
- [x] Pin external GitHub Actions to immutable commit SHAs.
- [x] Produce deterministic lockfile-based release dependency manifest.
- [x] Document controlled dependency-update procedure.
- [x] Constrain trust to three reviewed classes of locally generated Loom JARs.
- [x] Add regression policy against broad trust and synthetic dependency declarations.
- [x] Merge PR #61 and record exact closing commit.
- [x] Close SEC-005.

## H4 — CI security coverage and build matrix

**PR:** #62  
**Evidence:** [`H4_CI_SECURITY_COVERAGE_2026-07-31.md`](H4_CI_SECURITY_COVERAGE_2026-07-31.md)

- [x] Add NeoForge build to primary VillAIgence CI.
- [x] Retain common tests, Fabric build and release-package verification.
- [x] Add dependency-free high-confidence secret scanning.
- [x] Add deterministic Java/Gradle dangerous-API source policy.
- [x] Add exact reviewed source-security exception registry.
- [x] Add deterministic recursive tracked script/executable inventory.
- [x] Record SHA-256, mode, network indicators and CI references in inventory artifact.
- [x] Block undocumented scripts/executables and stale inventory entries.
- [x] Normalize accidental executable bit on NeoForge `pack.mcmeta`.
- [x] Add detector self-tests.
- [x] Enforce explicit workflow permission boundaries.
- [x] Keep all non-release workflows read-only.
- [x] Keep `contents: write` isolated to the tag-only `github-release` job.
- [x] Complete automated validation on code head `afcef79a761f5b3f96e02c419a4ba63bf83e890b`.
- [ ] Merge PR #62 and record the closing squash commit.
- [ ] Close SEC-006 after merge.
- [ ] Carry the partially remediated SEC-009 inventory evidence into H5.

### H4 final automated evidence

```text
VillAIgence CI #893 / 30633864131 — SUCCESS
Java Pull Request CI #430 / 30633864150 — SUCCESS
Repository security policy #20 / 30633864188 — SUCCESS
inventory artifact 8794446800
artifact digest sha256:5a26629ec24f28eaef6c2899b6f3505cf104ee038ed84443e65d8b7f6893c887
```

## H5 — Legacy tools cleanup and audit closure

- [ ] Review the generated recursive inventory at the H4/H5 boundary.
- [ ] Classify each inherited utility by actual behavior and maintenance need.
- [ ] Remove `scripts/pirate_translator.py`, unless a documented need is proven.
- [ ] Review inherited TTS, contributor, localization, name and skin utilities.
- [ ] Remove obsolete utilities or isolate retained tools under explicit developer documentation.
- [ ] If network utilities remain, add bounded timeouts, response limits and mocked-network tests where appropriate.
- [ ] Confirm no unexpected Python/shell invocation from build, CI or release paths.
- [ ] Rerun whole-tree secret, source-policy, dependency and script-inventory checks.
- [ ] Record inherited versus VillAIgence-owned origin where practical.
- [ ] Update audit with exact closing commits and residual risk.
- [ ] Close SEC-008 and SEC-009, or explicitly document accepted residual risk.

## Final CI and live validation

- [x] Common unit tests pass through H4.
- [x] Fabric build/package passes through H4.
- [x] NeoForge build passes in primary and independent PR CI.
- [x] Wrapper and dependency verification pass after H3.
- [x] Secret/source/script/workflow policy checks pass after H4.
- [ ] Standard OpenRouter/OpenAI configuration works on a real server with H1/H2.
- [ ] Invalid HTTP/lookalike endpoints fail safely.
- [ ] Explicit loopback development mode works only with opt-in.
- [ ] Oversized declared/chunked responses fail safely.
- [ ] Slow-drip provider responses terminate at the total deadline.
- [ ] Text Chat persists exactly once.
- [ ] Voice STT/TTS pipeline remains operational.
- [ ] TTS failure preserves text output.
- [ ] Concurrent voice capture remains stable under the global PCM budget.
- [ ] Logs contain no credentials, authorization headers, prompts or transcripts.
- [ ] Persistent world files remain stable across restart where no mutation is expected.
- [ ] Release JAR, checksum and dependency manifest are retained as evidence.

## Documentation closure

- [x] Add dated H2 evidence.
- [x] Add dated H3 evidence and SEC-005 closure follow-up.
- [x] Add dated H4 evidence.
- [ ] Record H4 merge and SEC-006 closure follow-up.
- [ ] Add combined H1/H2 real-server validation document.
- [ ] Update `docs/PROJECT_STATE.md` and `docs/CHANGELOG.md` after validation boundaries are reconciled.
- [ ] Mark Step 1 complete only after H5 and applicable live evidence exist.
