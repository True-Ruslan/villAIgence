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
- [`APPROVED_SCRIPT_INVENTORY.json`](APPROVED_SCRIPT_INVENTORY.json) — approved tracked scripts/executables and their network/CI classifications.
- [`APPROVED_SOURCE_SECURITY_EXCEPTIONS.json`](APPROVED_SOURCE_SECURITY_EXCEPTIONS.json) — exact reviewed source-policy exceptions.
- [`DEPENDENCY_UPDATE_PROCEDURE.md`](DEPENDENCY_UPDATE_PROCEDURE.md) — controlled Gradle, dependency and GitHub Action update procedure.
- [`../livingworld/PROVIDER_ENDPOINT_SECURITY.md`](../livingworld/PROVIDER_ENDPOINT_SECURITY.md) — operator-facing H1 endpoint, credential and redirect policy.

## Current status

- H1 provider endpoint and credential policy: merged and automated-CI validated; real-server smoke remains required before SEC-001/SEC-002 closure.
- H2 bounded network I/O and voice resource controls: merged as `15c56526417ac7dfb76567d51d1aa107f522cda7`; real-server smoke remains required before runtime-sensitive finding closure.
- H3 supply-chain verification: merged as `4cf9aef2e5c31a5682a7cad8544219154330e056`; SEC-005 is Closed.
- H4 CI security coverage: implementation and automated validation are complete in PR #62; SEC-006 remains open until merge evidence is recorded.
- H5 legacy-tool cleanup and final whole-tree audit: not started.

The audit found no direct evidence of intentionally malicious runtime behavior in the inspected VillAIgence paths. Inherited manual utilities remain outside normal mod runtime and CI. They are now recursively inventoried and blocked from changing or expanding without an explicit reviewed classification.

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
