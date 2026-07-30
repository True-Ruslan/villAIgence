# VillAIgence Security Documentation

This directory contains the canonical security-review evidence and hardening plans for VillAIgence.

## Current documents

- [`SECURITY_AUDIT_2026-07-31.md`](SECURITY_AUDIT_2026-07-31.md) — source, runtime-boundary and supply-chain audit recorded against branch `1.21.1` at commit `c45aea45dd915b24ba236344feef30559c7171bb`.
- [`STEP_1_SECURITY_SUPPLY_CHAIN_HARDENING.md`](STEP_1_SECURITY_SUPPLY_CHAIN_HARDENING.md) — ordered implementation plan for the first security and supply-chain hardening workstream.
- [`STEP_1_TRACKER.md`](STEP_1_TRACKER.md) — versioned execution checklist. GitHub Issues are disabled for this repository, so progress is tracked here.

## Status

The audit found no direct evidence of intentionally malicious runtime behavior in the inspected VillAIgence paths. It did identify several medium-priority hardening gaps around configurable provider endpoints, bounded network reads, build reproducibility and CI coverage.

The repository also contains at least one inherited Python network utility, `scripts/pirate_translator.py`. It is not part of normal mod runtime, Gradle build, release packaging or the inspected GitHub Actions workflows, but it sends localization strings to an external translation service when run manually. Its presence and limitations are now explicitly recorded rather than treated as absent.

## Security change rule

A documented finding remains open until all of the following are true:

1. the relevant code or build configuration has changed;
2. targeted regression tests exist and pass;
3. the full supported build matrix passes;
4. operator-visible behavior and compatibility are documented;
5. runtime-sensitive changes are validated on a real server where applicable.

Documentation alone does not close a finding.

## Resume protocol

When continuing security work:

1. read the latest audit, hardening plan and tracker;
2. reconcile them with current branch HEAD, open PRs, CI and releases;
3. inspect any new scripts, workflows, dependencies and network-capable code;
4. continue from the first incomplete tracker item;
5. update the tracker, audit status and project state after material progress.
