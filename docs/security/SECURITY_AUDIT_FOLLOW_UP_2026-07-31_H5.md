# Security Audit Follow-Up — H5 Legacy Tool and Whole-Tree Closure

**Date:** 2026-07-31  
**Repository:** `True-Ruslan/villAIgence`  
**Default branch:** `1.21.1`  
**Closing merge commit:** `6d82b4e4650294a4a42b9ea2113e64d990e08811`  
**Pull request:** #63

## Findings closed

### SEC-008 — Legacy Python translator sent localization data to an external service

**Status: Closed**

`scripts/pirate_translator.py` was removed together with the inherited unmanaged Python requirements and all other obsolete non-CI utility launchers.

Closure evidence:

- exact semantic review recorded in `H5_LEGACY_TOOLS_AUDIT_CLOSURE_2026-07-31.md`;
- the translator and its external request path are absent from the merged tree;
- permanent tests require the path to remain absent;
- build, buildSrc source, CI, release and retained CI scripts are checked for removed utility references;
- exact-head secret/source/workflow/script policy succeeded;
- common tests, Fabric, NeoForge and distributable-package checks succeeded.

There is no accepted residual risk for this finding because the utility was removed rather than retained or suppressed.

### SEC-009 — Delta-focused audit missed inherited scripts and lacked a deterministic whole-tree inventory

**Status: Closed**

Closure evidence:

- H4 introduced recursive tracked script/executable discovery and blocked undocumented additions;
- H5 generated an exact-head SHA-256 manifest for all tracked files before cleanup;
- every inherited utility and tool-only resource received a semantic retain/remove decision;
- all 19 non-CI tool files were removed;
- the approved script inventory was reduced from 17 launchers to exactly 5 reviewed build/security launchers;
- a second exact-head whole-tree manifest was retained after cleanup;
- removed utility invocation references are blocked with path/line diagnostics;
- all non-release workflows remain read-only;
- the tag-only release job remains the sole `contents: write` boundary.

## Final implementation evidence

Validated code head:

```text
ae26a9445b646c02e53b9fe8a557204fd703c7ff
```

Successful workflows:

```text
VillAIgence CI #922 / 30636167806
Java Pull Request CI with Gradle #458 / 30636168112
Repository security policy #79 / 30636168870
```

Final exact-head artifacts:

```text
script inventory
id: 8795396094
sha256:f92b9dffb43da32cf6be4b39506c1502dbcb20f85dac8c1a00d7fa4e8d54a54b
items: 5

tracked-tree manifest
id: 8795396369
sha256:fa0868462479b85c16027f989ab44693dbd0e39a0d3d90fbe6b48cde77d40175
tracked files: 3458
```

The complete change was squash-merged as:

```text
6d82b4e4650294a4a42b9ea2113e64d990e08811
```

## Step 1 boundary after H5

The repository-side H1–H5 implementation program is complete:

```text
H1 provider endpoint and credential policy     merged
H2 bounded network and voice resources         merged
H3 immutable verified build inputs             merged
H4 primary CI and repository security policy   merged
H5 legacy utility and whole-tree closure       merged
```

Closed findings:

```text
SEC-005
SEC-006
SEC-008
SEC-009
```

Runtime-sensitive findings remain implemented but open until controlled server validation:

```text
SEC-001
SEC-002
SEC-003
SEC-004
SEC-007
```

No direct evidence of intentionally malicious runtime behavior was found. The remaining work is operational validation of the merged endpoint, response-bound and voice-resource controls, not additional repository cleanup.
