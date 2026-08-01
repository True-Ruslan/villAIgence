# VillAIgence Security Documentation

This directory contains the canonical security-review evidence and hardening plans for VillAIgence.

## Canonical documents

- [`SECURITY_AUDIT_2026-07-31.md`](SECURITY_AUDIT_2026-07-31.md) — original source, runtime-boundary and supply-chain audit against `1.21.1` commit `c45aea45dd915b24ba236344feef30559c7171bb`.
- [`STEP_1_SECURITY_SUPPLY_CHAIN_HARDENING.md`](STEP_1_SECURITY_SUPPLY_CHAIN_HARDENING.md) — ordered implementation plan for Step 1.
- [`STEP_1_TRACKER.md`](STEP_1_TRACKER.md) — completed canonical execution checklist; GitHub Issues are disabled for this repository.
- [`H2_BOUNDED_NETWORK_HARDENING_2026-07-31.md`](H2_BOUNDED_NETWORK_HARDENING_2026-07-31.md) — bounded provider I/O, verification and voice-resource evidence.
- [`H3_SUPPLY_CHAIN_HARDENING_2026-07-31.md`](H3_SUPPLY_CHAIN_HARDENING_2026-07-31.md) — immutable build inputs and SEC-005 closure evidence.
- [`SECURITY_AUDIT_FOLLOW_UP_2026-07-31_H3.md`](SECURITY_AUDIT_FOLLOW_UP_2026-07-31_H3.md) — dated audit reconciliation closing SEC-005 at merge commit `4cf9aef2e5c31a5682a7cad8544219154330e056`.
- [`H4_CI_SECURITY_COVERAGE_2026-07-31.md`](H4_CI_SECURITY_COVERAGE_2026-07-31.md) — complete primary loader matrix, secret/source policy, recursive script inventory and CI evidence.
- [`SECURITY_AUDIT_FOLLOW_UP_2026-07-31_H4.md`](SECURITY_AUDIT_FOLLOW_UP_2026-07-31_H4.md) — dated reconciliation closing SEC-006 at merge commit `05d105c1f558d5643b8190a88cc744b4d7cbe129`.
- [`H5_LEGACY_TOOLS_AUDIT_CLOSURE_2026-07-31.md`](H5_LEGACY_TOOLS_AUDIT_CLOSURE_2026-07-31.md) — semantic retain/remove decisions, exact-head whole-tree manifests and SEC-008/SEC-009 implementation evidence.
- [`SECURITY_AUDIT_FOLLOW_UP_2026-07-31_H5.md`](SECURITY_AUDIT_FOLLOW_UP_2026-07-31_H5.md) — closes SEC-008 and SEC-009 at H5 merge `6d82b4e4650294a4a42b9ea2113e64d990e08811`.
- [`H1_H2_CONTROLLED_SERVER_VALIDATION.md`](H1_H2_CONTROLLED_SERVER_VALIDATION.md) — complete H1/H2 real-server scenario and residual acceptance boundaries.
- [`LOCAL_SECURITY_ACCEPTANCE_HARNESS.md`](LOCAL_SECURITY_ACCEPTANCE_HARNESS.md) — operator procedure for loopback hostile-provider, verification and PCM acceptance, including stage-specific persistence rules.
- [`SECURITY_AUDIT_FOLLOW_UP_2026-07-31_RUNTIME_0.1.15.md`](SECURITY_AUDIT_FOLLOW_UP_2026-07-31_RUNTIME_0.1.15.md) — closes SEC-001 and SEC-002 using real-server evidence.
- [`SECURITY_AUDIT_FOLLOW_UP_2026-08-01_RUNTIME_0.1.16.md`](SECURITY_AUDIT_FOLLOW_UP_2026-08-01_RUNTIME_0.1.16.md) — closes SEC-003 and SEC-007 using controlled hostile-provider, deadline and PCM evidence.
- [`SECURITY_AUDIT_FOLLOW_UP_2026-08-01_RUNTIME_0.1.17.md`](SECURITY_AUDIT_FOLLOW_UP_2026-08-01_RUNTIME_0.1.17.md) — closes SEC-004 using the exact official release JAR and marks Step 1 complete.
- [`../livingworld/VALIDATION_0.1.15.md`](../livingworld/VALIDATION_0.1.15.md) — production Chat/STT/TTS, endpoint-policy, persistence and restart validation.
- [`../livingworld/VALIDATION_0.1.16.md`](../livingworld/VALIDATION_0.1.16.md) — hostile response, redirect, deadline, PCM and production-restoration acceptance.
- [`../livingworld/VALIDATION_0.1.17.md`](../livingworld/VALIDATION_0.1.17.md) — focused SEC-004 exact-release-JAR success, redirect, oversize and HTTPS-rejection acceptance.
- [`APPROVED_SCRIPT_INVENTORY.json`](APPROVED_SCRIPT_INVENTORY.json) — the seven approved Gradle, CI and loopback-acceptance scripts.
- [`APPROVED_SOURCE_SECURITY_EXCEPTIONS.json`](APPROVED_SOURCE_SECURITY_EXCEPTIONS.json) — exact reviewed source-policy exceptions.
- [`DEPENDENCY_UPDATE_PROCEDURE.md`](DEPENDENCY_UPDATE_PROCEDURE.md) — controlled Gradle, dependency and GitHub Action update procedure.
- [`../livingworld/PROVIDER_ENDPOINT_SECURITY.md`](../livingworld/PROVIDER_ENDPOINT_SECURITY.md) — operator-facing H1 endpoint, credential and redirect policy.

## Current status

Step 1 Security and supply-chain hardening is complete within its defined scope.

```text
SEC-001 Closed
SEC-002 Closed
SEC-003 Closed
SEC-004 Closed
SEC-005 Closed
SEC-006 Closed
SEC-007 Closed
SEC-008 Closed
SEC-009 Closed
```

Runtime and artifact evidence is layered:

- `0.1.15+1.21.1` live-validates normal production Chat/STT/TTS, endpoint rejection, TTS fail-soft persistence and restart durability; SEC-001 and SEC-002 are Closed.
- `0.1.16+1.21.1` live-validates Chat/STT/TTS response limits, provider-error bounds, ten-minute slow-drip deadline, no-redirect behavior, stage-specific TTS persistence, voice duration clamp, 128 MiB PCM exhaustion/recovery and production restoration; SEC-003 and SEC-007 are Closed.
- `0.1.17+1.21.1` exact-release-JAR validation confirms verification success, 64 KiB declared/chunked bounds, no redirect following and pre-connection rejection of HTTPS loopback by the acceptance-only probe; SEC-004 is Closed.
- H3 supply-chain verification is merged as `4cf9aef2e5c31a5682a7cad8544219154330e056`; SEC-005 is Closed.
- H4 CI security coverage is merged as `05d105c1f558d5643b8190a88cc744b4d7cbe129`; SEC-006 is Closed.
- H5 legacy-tool cleanup is merged as `6d82b4e4650294a4a42b9ea2113e64d990e08811`; SEC-008 and SEC-009 are Closed.

Release roles:

```text
0.1.14+1.21.1  forgetting/decay retention-pressure checkpoint
0.1.15+1.21.1  normal production and endpoint-policy checkpoint
0.1.16+1.21.1  installed full hostile-provider/PCM server checkpoint
0.1.17+1.21.1  final SEC-004 exact-artifact checkpoint
```

The production server remained on `0.1.16+1.21.1` during the focused `0.1.17` probe test. This is intentional: the final change was confined to the explicit `java -cp` acceptance probe and did not require Minecraft installation or server restart.

## Acceptance tool boundary

The retained acceptance surface is deliberately narrow:

```text
scripts/security/provider_acceptance_harness.py
scripts/ci/test_provider_acceptance_harness.py
AccountVerificationAcceptanceProbe
VoicePcmBudgetAcceptanceProbe
```

The Python server is restricted to literal loopback and records sanitized metadata only. The Java probes require explicit `java -cp` invocation and have no Minecraft startup hook, in-game command or production credential lookup.

`AccountVerificationAcceptanceProbe` accepts HTTP literal-loopback targets only. Production `/mca verify` remains independently fixed to its trusted HTTPS provider origin.

## Dependency-verification note

A local `0.1.17` source build in one validation environment stopped before compilation because checksum records were absent for transitive Fabric artifacts resolved there. Verification was not disabled, no unverified dependency was accepted and the local output was not used. The exact official release JAR was tested by recorded SHA-256.

This is retained as a non-blocking build-maintenance follow-up. Any checksum metadata refresh must use [`DEPENDENCY_UPDATE_PROCEDURE.md`](DEPENDENCY_UPDATE_PROCEDURE.md); verification must not be bypassed.

## Security conclusion

The audit found no direct evidence of intentionally malicious runtime behavior in the inspected VillAIgence paths. All inherited non-CI utilities, external-maintenance scripts, raw generator inputs and tool-only masks were removed from the merged default branch. The retained script surface is limited to reviewed Gradle, VillAIgence-owned CI/security launchers and the explicit loopback acceptance harness, all enforced by deterministic policy.

Step 1 completion does not mean future code is automatically trusted. New scripts, workflows, dependencies, network paths, persistence formats and privileged actions remain subject to the same repository policy and evidence rules.

## Security change rule

A documented finding remains open until all applicable closure conditions are met:

1. implementation is merged;
2. targeted negative/regression tests pass;
3. the full supported build matrix passes;
4. build/security verification passes;
5. runtime-sensitive changes pass controlled server or exact-release-artifact validation;
6. a dated record names the exact closing commit and residual risk.

## Resume protocol

When continuing security-sensitive work:

1. read the latest audit, follow-ups, hardening plan and completed tracker;
2. reconcile them with current default-branch HEAD, open PRs and CI;
3. inspect new scripts, workflows, dependencies and network-capable code;
4. treat Step 1 as the established baseline rather than reopening it without new evidence;
5. create a new scoped finding or hardening phase for newly discovered risk;
6. update evidence, project state and changelog after material progress.
