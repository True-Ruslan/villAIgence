# VillAIgence Security Documentation

This directory contains the canonical security-review evidence and hardening plans for VillAIgence.

## Canonical documents

- [`SECURITY_AUDIT_2026-07-31.md`](SECURITY_AUDIT_2026-07-31.md) — original source, runtime-boundary and supply-chain audit against `1.21.1` commit `c45aea45dd915b24ba236344feef30559c7171bb`.
- [`STEP_1_SECURITY_SUPPLY_CHAIN_HARDENING.md`](STEP_1_SECURITY_SUPPLY_CHAIN_HARDENING.md) — ordered implementation plan for Step 1.
- [`STEP_1_TRACKER.md`](STEP_1_TRACKER.md) — canonical execution checklist; GitHub Issues are disabled for this repository.
- [`H2_BOUNDED_NETWORK_HARDENING_2026-07-31.md`](H2_BOUNDED_NETWORK_HARDENING_2026-07-31.md) — bounded provider I/O, verification and voice-resource evidence.
- [`H3_SUPPLY_CHAIN_HARDENING_2026-07-31.md`](H3_SUPPLY_CHAIN_HARDENING_2026-07-31.md) — immutable build inputs and SEC-005 closure evidence.
- [`SECURITY_AUDIT_FOLLOW_UP_2026-07-31_H3.md`](SECURITY_AUDIT_FOLLOW_UP_2026-07-31_H3.md) — dated audit reconciliation closing SEC-005 at merge commit `4cf9aef2e5c31a5682a7cad8544219154330e056`.
- [`H4_CI_SECURITY_COVERAGE_2026-07-31.md`](H4_CI_SECURITY_COVERAGE_2026-07-31.md) — complete primary loader matrix, secret/source policy, recursive script inventory and CI evidence.
- [`SECURITY_AUDIT_FOLLOW_UP_2026-07-31_H4.md`](SECURITY_AUDIT_FOLLOW_UP_2026-07-31_H4.md) — dated reconciliation closing SEC-006 at merge commit `05d105c1f558d5643b8190a88cc744b4d7cbe129`.
- [`H5_LEGACY_TOOLS_AUDIT_CLOSURE_2026-07-31.md`](H5_LEGACY_TOOLS_AUDIT_CLOSURE_2026-07-31.md) — semantic retain/remove decisions, exact-head whole-tree manifests and SEC-008/SEC-009 implementation evidence.
- [`SECURITY_AUDIT_FOLLOW_UP_2026-07-31_H5.md`](SECURITY_AUDIT_FOLLOW_UP_2026-07-31_H5.md) — closes SEC-008 and SEC-009 at H5 merge `6d82b4e4650294a4a42b9ea2113e64d990e08811`.
- [`H1_H2_CONTROLLED_SERVER_VALIDATION.md`](H1_H2_CONTROLLED_SERVER_VALIDATION.md) — complete H1/H2 real-server scenario and residual acceptance boundaries.
- [`LOCAL_SECURITY_ACCEPTANCE_HARNESS.md`](LOCAL_SECURITY_ACCEPTANCE_HARNESS.md) — exact operator procedure for loopback hostile-provider, verification and PCM acceptance.
- [`SECURITY_AUDIT_FOLLOW_UP_2026-07-31_RUNTIME_0.1.15.md`](SECURITY_AUDIT_FOLLOW_UP_2026-07-31_RUNTIME_0.1.15.md) — closes SEC-001 and SEC-002 using real-server evidence.
- [`../livingworld/VALIDATION_0.1.15.md`](../livingworld/VALIDATION_0.1.15.md) — production Chat/STT/TTS, endpoint-policy, persistence and restart validation.
- [`APPROVED_SCRIPT_INVENTORY.json`](APPROVED_SCRIPT_INVENTORY.json) — the seven approved Gradle, CI and loopback-acceptance scripts.
- [`APPROVED_SOURCE_SECURITY_EXCEPTIONS.json`](APPROVED_SOURCE_SECURITY_EXCEPTIONS.json) — exact reviewed source-policy exceptions.
- [`DEPENDENCY_UPDATE_PROCEDURE.md`](DEPENDENCY_UPDATE_PROCEDURE.md) — controlled Gradle, dependency and GitHub Action update procedure.
- [`../livingworld/PROVIDER_ENDPOINT_SECURITY.md`](../livingworld/PROVIDER_ENDPOINT_SECURITY.md) — operator-facing H1 endpoint, credential and redirect policy.

## Current status

- H1 provider endpoint and credential policy: merged and live-validated in `0.1.15+1.21.1`; SEC-001 and SEC-002 are Closed.
- H2 bounded network I/O and voice resource controls: normal production and TTS fail-soft behavior passed in `0.1.15+1.21.1`; deterministic loopback acceptance tooling is prepared for SEC-003, SEC-004 and SEC-007, which remain open until a release containing the tooling completes the controlled run.
- H3 supply-chain verification: merged as `4cf9aef2e5c31a5682a7cad8544219154330e056`; SEC-005 is Closed.
- H4 CI security coverage: merged as `05d105c1f558d5643b8190a88cc744b4d7cbe129`; SEC-006 is Closed.
- H5 legacy-tool cleanup: merged as `6d82b4e4650294a4a42b9ea2113e64d990e08811`; SEC-008 and SEC-009 are Closed.

Release `0.1.15+1.21.1` is the latest production/security live checkpoint, while `0.1.14+1.21.1` remains the canonical forgetting/decay retention-pressure checkpoint.

The current acceptance tool surface is deliberately narrow:

```text
scripts/security/provider_acceptance_harness.py
scripts/ci/test_provider_acceptance_harness.py
AccountVerificationAcceptanceProbe
VoicePcmBudgetAcceptanceProbe
```

The Python server is restricted to literal loopback and records sanitized metadata only. The Java probes are present in the release JAR but require explicit `java -cp` invocation and have no Minecraft startup hook.

The audit found no direct evidence of intentionally malicious runtime behavior in the inspected VillAIgence paths. All inherited non-CI utilities, external-maintenance scripts, raw generator inputs and tool-only masks have been removed from the merged default branch. The retained script surface is limited to reviewed Gradle, VillAIgence-owned CI/security launchers and the explicit loopback acceptance harness, all enforced by deterministic policy.

## Security change rule

A documented finding remains open until all applicable closure conditions are met:

1. implementation is merged;
2. targeted negative/regression tests pass;
3. the full supported build matrix passes;
4. build/security verification passes;
5. runtime-sensitive changes pass controlled server validation;
6. a dated record names the exact closing commit and residual risk.

## Resume protocol

When continuing security work:

1. read the latest audit, follow-ups, hardening plan and tracker;
2. reconcile them with current default-branch HEAD, open PRs and CI;
3. inspect new scripts, workflows, dependencies and network-capable code;
4. continue from the first incomplete tracker item;
5. update evidence, tracker, project state and changelog after material progress.
