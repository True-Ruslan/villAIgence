# Step 1 Security Hardening Tracker

**Status:** H1–H5 merged; repository-side hardening is complete; controlled H1/H2 real-server validation remains
**Plan:** [`STEP_1_SECURITY_SUPPLY_CHAIN_HARDENING.md`](STEP_1_SECURITY_SUPPLY_CHAIN_HARDENING.md)  
**Audit:** [`SECURITY_AUDIT_2026-07-31.md`](SECURITY_AUDIT_2026-07-31.md)  
**Security index:** [`README.md`](README.md)

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
- [ ] Complete controlled real-server smoke validation.
- [ ] Close SEC-001 and SEC-002 in a dated audit follow-up.

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
- [ ] Complete controlled real-server validation.
- [ ] Close SEC-003, SEC-004 and SEC-007 in a dated follow-up.

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

## Remaining controlled real-server validation

- [ ] Standard OpenRouter/OpenAI configuration works with merged H1/H2.
- [ ] Invalid HTTP/lookalike endpoints fail safely.
- [ ] Explicit loopback development mode works only with opt-in.
- [ ] Oversized declared/chunked responses fail safely.
- [ ] Slow-drip responses terminate at the total deadline.
- [ ] Text Chat persists exactly once.
- [ ] Voice STT/TTS remains operational.
- [ ] TTS failure preserves text output.
- [ ] Concurrent voice capture remains stable under the PCM budget.
- [ ] Logs contain no credentials, authorization headers, prompts or transcripts.
- [ ] Persistent world files remain stable across restart when no mutation is expected.
- [ ] Release JAR, checksum and dependency manifest are retained as evidence.

## Documentation closure

- [x] H2, H3, H4 and H5 dated implementation evidence exists.
- [x] SEC-005 and SEC-006 closing follow-ups exist.
- [x] Record H5 merge and close SEC-008/SEC-009.
- [x] Add `H1_H2_CONTROLLED_SERVER_VALIDATION.md`; execution evidence remains pending.
- [x] Reconcile `docs/PROJECT_STATE.md` and `docs/CHANGELOG.md` after H5 merge.
- [ ] Mark Step 1 fully complete only after applicable live validation exists.
