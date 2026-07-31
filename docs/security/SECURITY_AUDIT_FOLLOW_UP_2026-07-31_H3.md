# Security Audit Follow-Up — H3 Supply-Chain Closure

**Date:** 2026-07-31  
**Repository:** `True-Ruslan/villAIgence`  
**Default branch:** `1.21.1`  
**Closing merge commit:** `4cf9aef2e5c31a5682a7cad8544219154330e056`  
**Pull request:** #61

## Finding closed

### SEC-005 — Build and workflow supply-chain inputs were not fully immutable or verified

**Status: Closed**

The merged implementation provides:

- stable Fabric Loom `1.17.17` instead of a snapshot;
- official Gradle 9.6.1 wrapper distribution SHA-256;
- Gradle wrapper validation;
- external GitHub Actions pinned to immutable commit SHAs;
- Gradle dependency verification metadata with SHA-256 checksums;
- committed dependency lockfiles for all three projects;
- Maven repository content filtering and removal of an unused broad resolver;
- deterministic release dependency manifests sourced from committed lockfiles;
- a controlled dependency-update procedure;
- read-only cold-refresh CI covering common tests, Fabric and NeoForge;
- regression controls around the three narrowly reviewed classes of locally generated Loom development JARs.

## Validation evidence

Validated code head:

```text
4d00ff296819196bd12fd5e3f16fd93820b5cf9c
```

Successful checks:

```text
VillAIgence CI #875 / 30631724664
Java Pull Request CI #413 / 30631724636
VillAIgence GitHub Release #69 / 30631724672
Supply-chain verification #29 / 30631724653
```

The two later PR commits only added the dated H3 evidence and tracker reconciliation. The complete change was squash-merged as `4cf9aef2e5c31a5682a7cad8544219154330e056`.

## Residual boundaries

This closure does not close:

- SEC-006, because NeoForge is not yet enforced in the primary VillAIgence CI workflow;
- SEC-008, because the legacy network-capable Python translator remains present;
- SEC-009, because deterministic whole-tree executable/script inventory and security scanning are not yet mandatory.

Those controls are assigned to H4 and H5.
