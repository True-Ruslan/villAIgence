# Security Audit Follow-Up — H4 CI Coverage Closure

**Date:** 2026-07-31  
**Repository:** `True-Ruslan/villAIgence`  
**Default branch:** `1.21.1`  
**Closing merge commit:** `05d105c1f558d5643b8190a88cc744b4d7cbe129`  
**Pull request:** #62

## Finding closed

### SEC-006 — Primary CI omitted NeoForge despite supported compatibility requirement

**Status: Closed**

The primary `VillAIgence CI` workflow now requires, in one read-only job:

```text
:common:test
:fabric:build
:neoforge:build
```

The job also runs the deterministic repository security policy before compilation and retains the Fabric distributable-package smoke check after both loader targets succeed.

## Additional H4 controls

The H4 merge also added:

- high-confidence tracked-file secret scanning;
- Java/Gradle dangerous-API source policy;
- exact reviewed source exceptions with stale-entry rejection;
- recursive tracked script/executable discovery;
- versioned script classifications;
- SHA-256 inventory artifacts;
- workflow permission enforcement;
- detector self-tests;
- normalization of an accidental executable bit on NeoForge pack metadata.

## Validation evidence

Validated code head:

```text
afcef79a761f5b3f96e02c419a4ba63bf83e890b
```

Successful checks:

```text
VillAIgence CI #893 / 30633864131
Java Pull Request CI #430 / 30633864150
Repository security policy #20 / 30633864188
```

Inventory artifact:

```text
id: 8794446800
digest: sha256:5a26629ec24f28eaef6c2899b6f3505cf104ee038ed84443e65d8b7f6893c887
```

The complete change was squash-merged as `05d105c1f558d5643b8190a88cc744b4d7cbe129`.

## Residual boundary

SEC-009 remains open. H4 automated discovery and blocked undocumented additions, but H5 must still make a semantic retain/remove decision for inherited utilities, remove obsolete network-capable tools, generate a closing whole-tree manifest and record residual risk.
