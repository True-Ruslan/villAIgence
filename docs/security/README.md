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
- [`APPROVED_SCRIPT_INVENTORY.json`](APPROVED_SCRIPT_INVENTORY.json) — the five approved build/security launchers.
- [`APPROVED_SOURCE_SECURITY_EXCEPTIONS.json`](APPROVED_SOURCE_SECURITY_EXCEPTIONS.json) — exact reviewed source-policy exceptions.
- [`DEPENDENCY_UPDATE_PROCEDURE.md`](DEPENDENCY_UPDATE_PROCEDURE.md) — controlled Gradle, dependency and GitHub Action update procedure.
- [`../livingworld/PROVIDER_ENDPOINT_SECURITY.md`](../livingworld/PROVIDER_ENDPOINT_SECURITY.md) — operator-facing H1 endpoint, credential and redirect policy.

## Current status

- H1 provider endpoint and credential policy: merged and automated-CI validated; real-server smoke remains required before SEC-001/SEC-002 closure.
- H2 bounded network I/O and voice resource controls: merged as `15c56526417ac7dfb76567d51d1aa107f522cda7`; real-server smoke remains required before SEC-003/SEC-004/SEC-007 closure.
- H3 supply-chain verification: merged as `4cf9aef2e5c31a5682a7cad8544219154330e056`; SEC-005 is Closed.
- H4 CI security coverage: merged as `05d105c1f558d5643b8190a88cc744b4d7cbe129`; SEC-006 is Closed.
- H5 legacy-tool cleanup: merged as `6d82b4e4650294a4a42b9ea2113e64d990e08811`; SEC-008 and SEC-009 are Closed.

The audit found no direct evidence of intentionally malicious runtime behavior in the inspected VillAIgence paths. All inherited non-CI utilities, external-maintenance scripts, raw generator inputs and tool-only masks have been removed from the merged default branch. The retained script surface is limited to Gradle and VillAIgence-owned CI/security launchers and is enforced by deterministic policy.

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
