# H4 CI Security Coverage Evidence — 2026-07-31

## Scope

This record covers Step 1 H4: complete supported build coverage and deterministic repository security policy enforcement.

Implementation branch:

```text
agent/h4-ci-security-coverage
```

Final automated-validation code head:

```text
afcef79a761f5b3f96e02c419a4ba63bf83e890b
```

The work changes CI, security policy, tracked-file metadata and documentation only. It does not change gameplay, provider, Memory 2.0 or persistent world behavior.

## Implemented controls

### Primary supported build matrix

The primary `VillAIgence CI` workflow now runs:

```text
:common:test
:fabric:build
:neoforge:build
```

Fabric release-package smoke verification remains enabled after both loader builds succeed.

### Repository security policy

A dependency-free Python 3 policy runner now checks every tracked repository file and fails closed on:

- high-confidence private-key and credential formats;
- unapproved Java process execution, native loading, scripting engines, unsafe deserialization, URL class loading, reflection access and class-definition APIs;
- unapproved Gradle/Groovy process execution;
- undocumented tracked scripts or executable files;
- network-indicator mismatches in approved script classifications;
- stale script inventory entries;
- stale source-security exceptions;
- workflow files without explicit permission boundaries;
- `contents: write` outside the dedicated tag-only release job.

The scanner uses only the Python standard library and Git metadata. It does not download rules, execute repository utilities or contact external services.

### Deterministic recursive script inventory

The policy derives candidates from:

- every tracked executable mode;
- tracked shell, Python, PowerShell, batch and JavaScript/TypeScript script extensions;
- the Gradle wrapper launcher.

Every discovered item must exist in:

```text
docs/security/APPROVED_SCRIPT_INVENTORY.json
```

The generated artifact records, in stable path order:

- repository path;
- tracked/executable mode;
- SHA-256;
- detected network indicators;
- CI workflow references.

The final approved inventory contains 17 tracked scripts/executables. Inherited maintenance utilities are explicitly classified as manual and not CI-invoked. The legacy pirate translator remains identified as network-capable and is reserved for the H5 removal decision.

An accidental executable bit on `neoforge/src/main/resources/pack.mcmeta` was discovered by the policy and normalized to a regular tracked file rather than approved as an executable.

### Reviewed source exceptions

Only three source-policy exceptions are currently approved:

1. the fixed `git tag --points-at HEAD` Gradle version lookup;
2. its fixed `commandLine` invocation;
3. canonical record-constructor reflection in `RecordTypeAdapterFactory` for a statically selected Gson type.

Unused exceptions fail the policy, preventing suppressions from becoming permanent after the underlying code disappears.

### Detector self-tests

Five standard-library self-tests verify:

- each high-confidence secret signature;
- placeholder non-matches;
- representative Java/Gradle dangerous-API signatures;
- script candidate discovery by extension and executable mode;
- conservative network indicators.

## TDD evidence

### Initial policy contract RED

```text
76a88e19b26edfaed8ad4bffc4a94122b9c600b6
VillAIgence CI #879 / 30632794809
EXPECTED FAILURE
```

The contract required NeoForge in primary CI, a committed policy runner and an approved recursive inventory before those controls existed.

### Full-tree inventory discovery RED

```text
d3a5c1cc6bb46ab2b4536cefb47120af474a2348
Repository security policy #2 / 30633294384
EXPECTED FAILURE
```

The scanner reported:

- eleven inherited scripts absent from the initial inventory;
- the accidental executable `pack.mcmeta` mode;
- network-classification mismatches;
- the reviewed Gson reflection call requiring an explicit exception.

### Final GREEN

```text
afcef79a761f5b3f96e02c419a4ba63bf83e890b

VillAIgence CI #893 / 30633864131 — SUCCESS
Java Pull Request CI with Gradle #430 / 30633864150 — SUCCESS
Repository security policy #20 / 30633864188 — SUCCESS
```

Final inventory artifact:

```text
artifact id: 8794446800
digest: sha256:5a26629ec24f28eaef6c2899b6f3505cf104ee038ed84443e65d8b7f6893c887
retention: 14 days
```

## Finding status

SEC-006 implementation and automated validation are complete on the PR branch. It must remain open until PR #62 is merged and the exact merge commit is recorded in a dated follow-up.

SEC-009 is partially remediated:

- complete tracked-script/executable discovery is automated;
- undocumented additions are blocked;
- deterministic SHA-256 inventory evidence is retained;
- workflow invocation and network indicators are classified.

SEC-009 remains open until H5 completes the semantic review/removal decision for every inherited utility and records the closing whole-tree audit.
