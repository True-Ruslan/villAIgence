# Step 1 Security Hardening Tracker

**Status:** H1–H5 merged; `0.1.15+1.21.1` production validation passed; deterministic tooling prepared for SEC-003/SEC-004/SEC-007 isolated acceptance
**Plan:** [`STEP_1_SECURITY_SUPPLY_CHAIN_HARDENING.md`](STEP_1_SECURITY_SUPPLY_CHAIN_HARDENING.md)  
**Audit:** [`SECURITY_AUDIT_2026-07-31.md`](SECURITY_AUDIT_2026-07-31.md)  
**Security index:** [`README.md`](README.md)  
**Residual acceptance procedure:** [`LOCAL_SECURITY_ACCEPTANCE_HARNESS.md`](LOCAL_SECURITY_ACCEPTANCE_HARNESS.md)

GitHub Issues are disabled, so this versioned checklist is the canonical execution tracker. Detailed TDD runs and artifact identifiers live in the linked dated evidence records.

## H1 — Provider endpoint and credential policy

**Merge:** PR #59 — `787f1a781b5970d4bafb851bfb3c7cba7c21fc0a`

- [x] Centralized validated endpoint/origin model.
- [x] HTTPS required for non-loopback providers.
- [x] HTTP permitted only for explicit loopback development mode.
- [x] Chat/STT/TTS credentials bound to selected endpoints.
- [x] Unrelated main-key fallback removed for custom endpoints.
- [x] Exact normalized host trust and no authenticated redirects.
- [x] Malformed/lookalike/IDN/user-info/redirect regression tests.
- [x] Implementation merged and automated-CI validated.
- [x] Complete production real-server endpoint and credential validation in `0.1.15+1.21.1`.
- [x] Close SEC-001 and SEC-002 in `SECURITY_AUDIT_FOLLOW_UP_2026-07-31_RUNTIME_0.1.15.md`.

## H2 — Bounded network I/O and SSRF removal

**Merge:** PR #60 — `15c56526417ac7dfb76567d51d1aa107f522cda7`  
**Evidence:** [`H2_BOUNDED_NETWORK_HARDENING_2026-07-31.md`](H2_BOUNDED_NETWORK_HARDENING_2026-07-31.md)

- [x] Bounded Chat/STT/TTS/verification/error response readers.
- [x] Declared, chunked and unknown-length enforcement.
- [x] Connect/read timeouts and total body-read deadline.
- [x] Arbitrary-URL verification helper removed.
- [x] Trusted-origin-only account verification and no redirects.
- [x] Voice duration clamp and global PCM budget.
- [x] Exactly-once side-effect and persistence boundaries retained.
- [x] Implementation merged and automated-CI validated.
- [x] Complete production Chat/STT/TTS, TTS fail-soft and restart validation in `0.1.15+1.21.1`.
- [x] Prepare literal-loopback hostile-provider server with sanitized evidence.
- [x] Prepare exact-release-JAR verification transport probe.
- [x] Prepare exact-release-JAR voice clamp and concurrent PCM probe.
- [x] Add package smoke checks for every probe class.
- [x] Add standard-library harness regression tests to read-only security CI.
- [ ] Complete isolated mock-provider acceptance for SEC-003.
- [ ] Complete controlled verification and redirect acceptance for SEC-004.
- [ ] Complete voice clamp, PCM exhaustion/recovery and final microphone smoke for SEC-007.
- [ ] Close SEC-003, SEC-004 and SEC-007 after those scenarios pass.

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

- [x] Stable Fabric Loom and verified Gradle wrapper.
- [x] Immutable GitHub Action SHAs.
- [x] Dependency verification metadata and lockfiles.
- [x] Restricted Maven repository content.
- [x] Deterministic lockfile-based release manifest.
- [x] Narrow trust rules for locally generated Loom JARs.
- [x] Read-only cold-refresh Fabric/NeoForge verification.
- [x] SEC-005 Closed.

## H4 — CI security coverage and build matrix

**Merge:** PR #62 — `05d105c1f558d5643b8190a88cc744b4d7cbe129`  
**Evidence:** [`H4_CI_SECURITY_COVERAGE_2026-07-31.md`](H4_CI_SECURITY_COVERAGE_2026-07-31.md)  
**Audit closure:** [`SECURITY_AUDIT_FOLLOW_UP_2026-07-31_H4.md`](SECURITY_AUDIT_FOLLOW_UP_2026-07-31_H4.md)

- [x] Common tests, Fabric and NeoForge required in primary CI.
- [x] Fabric release-package verification retained.
- [x] High-confidence tracked-file secret scanning.
- [x] Java/Gradle dangerous-API source policy.
- [x] Exact reviewed exception registry with stale-entry rejection.
- [x] Recursive script/executable inventory and SHA-256 artifact.
- [x] Workflow least privilege and isolated tag-only release write access.
- [x] Detector self-tests.
- [x] SEC-006 Closed.
- [x] SEC-009 whole-tree discovery evidence carried into H5.

## H5 — Legacy tools cleanup and audit closure

**Merge:** PR #63 — `6d82b4e4650294a4a42b9ea2113e64d990e08811`  
**Evidence:** [`H5_LEGACY_TOOLS_AUDIT_CLOSURE_2026-07-31.md`](H5_LEGACY_TOOLS_AUDIT_CLOSURE_2026-07-31.md)

- [x] Generate exact-head pre-cleanup whole-tree manifest.
- [x] Semantically review every inherited utility and tool-only resource.
- [x] Remove deprecated Google/AWS static TTS bundle.
- [x] Remove credentialed Crowdin/patron contributor fetcher.
- [x] Remove obsolete external-LLM localization generator.
- [x] Remove `scripts/pirate_translator.py`.
- [x] Remove inherited name generator and raw dataset.
- [x] Remove inherited skin generators and masks.
- [x] Remove umbrella launcher, tool README, tool ignore and unmanaged Python requirements.
- [x] Reduce approved script inventory from 17 to exactly 5 launchers.
- [x] Add build/CI/release invocation guard for removed utilities.
- [x] Retain exact-head SHA-256 whole-tree manifest evidence.
- [x] Complete common/Fabric/NeoForge/package/security validation on `ae26a9445b646c02e53b9fe8a557204fd703c7ff`.
- [x] Merge PR #63 and record exact squash commit.
- [x] Close SEC-008 and SEC-009 in `SECURITY_AUDIT_FOLLOW_UP_2026-07-31_H5.md`.

### H5 final automated evidence

```text
VillAIgence CI #922 / 30636167806 — SUCCESS
Java Pull Request CI #458 / 30636168112 — SUCCESS
Repository security policy #79 / 30636168870 — SUCCESS

script inventory artifact 8795396094
sha256:f92b9dffb43da32cf6be4b39506c1502dbcb20f85dac8c1a00d7fa4e8d54a54b
items: 5

tracked-tree artifact 8795396369
sha256:fa0868462479b85c16027f989ab44693dbd0e39a0d3d90fbe6b48cde77d40175
tracked files: 3458
```

The H5 five-script result is historical exact-head evidence. The current approved inventory intentionally contains seven scripts after adding the reviewed loopback harness and its CI test.

## Current acceptance tool boundary

```text
scripts/security/provider_acceptance_harness.py
→ literal loopback bind only
→ declared/chunked/error/redirect/slow-drip routes
→ streamed hostile bodies
→ sanitized manifest and JSONL evidence

AccountVerificationAcceptanceProbe
→ explicit java -cp only
→ literal loopback target only
→ same JDK-only bounded/no-redirect transport as production verification

VoicePcmBudgetAcceptanceProbe
→ explicit java -cp only
→ exact 1..120 second clamp
→ exact 128 MiB production budget
→ synchronized contention, rejection, release and recovery
```

None of these tools has an in-game command, Minecraft startup hook or production-provider credential lookup.

## Remaining controlled real-server validation

- [x] Standard OpenRouter configuration works with merged H1/H2 in `0.1.15+1.21.1`.
- [x] LAN HTTP, lookalike, user-info and fragment endpoints fail safely without persistence mutation.
- [ ] Explicit loopback development mode works only with opt-in on the acceptance candidate.
- [ ] Oversized declared/chunked responses fail safely through the loopback harness.
- [ ] Oversized non-2xx bodies remain bounded and sanitized.
- [ ] Chat/STT/TTS redirects are not followed and target hits remain zero.
- [ ] Slow-drip responses terminate at the ten-minute total deadline.
- [ ] Verification success/redirect/oversize behavior matches the exact-JAR probe output.
- [ ] Voice clamp reports exactly 1 and 120 seconds.
- [ ] Concurrent PCM probe reaches but never exceeds 128 MiB, rejects overflow and fully recovers.
- [ ] A normal real microphone capture succeeds after the PCM probe.
- [x] Text Chat and Memory 2.0 DIALOGUE persistence remain operational.
- [x] Voice STT/TTS remains operational.
- [x] TTS failure preserves text output and DIALOGUE.
- [x] Logs contain no credentials or authorization headers in the reviewed `0.1.15` run.
- [x] All six persistent world files remain hash-identical across restart in the reviewed `0.1.15` run.
- [x] Release JAR filename, tag, commit and SHA-256 are retained as evidence.

## Documentation closure

- [x] H2, H3, H4 and H5 dated implementation evidence exists.
- [x] SEC-005 and SEC-006 closing follow-ups exist.
- [x] Record H5 merge and close SEC-008/SEC-009.
- [x] Add `H1_H2_CONTROLLED_SERVER_VALIDATION.md`.
- [x] Record production validation and close SEC-001/SEC-002.
- [x] Add the deterministic local residual-acceptance procedure.
- [ ] Record the acceptance-harness merge and candidate identity.
- [ ] Add a dated SEC-003/SEC-004/SEC-007 validation follow-up after the controlled run.
- [ ] Mark Step 1 fully complete only after SEC-003, SEC-004 and SEC-007 isolated acceptance exists.
