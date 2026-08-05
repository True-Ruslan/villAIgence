# M11 Phase E — E3 persistence recovery validation

## Status

PASS on 2026-08-05.

This document records exact-head evidence for `VAI-PERSIST-003`. It does not request a version, create a tag or publish a release.

## Exact validated head

```text
d674cfa86b01b3bccf97c62432f04d0285c6043d
```

## Mandatory workflows

| Gate | Run | Result |
| --- | ---: | --- |
| VillAIgence CI | 1619 / `31036942464` | PASS |
| Java Pull Request CI with Gradle | 1005 / `31036942450` | PASS |
| Repository security policy | 1143 / `31036942602` | PASS |
| VillAIgence GitHub Release dry-run | 231 / `31036942552` | PASS |

The release workflow ran in dry-run mode. The `github-release` job was intentionally skipped.

## Production recovery contract

The exact remapped candidate was staged with the normal production manifest. A derived test-only acceptance fixture changed only its main entrypoint to `ProductionAcceptanceRecoveryMode`; the candidate JAR was not modified.

Candidate evidence:

```text
version: 1.21.1-SNAPSHOT
SHA-256: 1036fa1e9aebdba6b6b8f0ab9a96596e9f6b8855f664e5edf90255a02535924c
Minecraft: 1.21.1
Fabric Loader: 0.19.3
```

The baseline server initialized all six canonical stores and exited cleanly. The matrix then copied that generated world into six isolated cases. Every case performed a first recovery startup and a second idempotence startup.

## Cases

| Case | Store | Corruption | Expected recovery | Result |
| --- | --- | --- | --- | --- |
| `memory-truncated` | `memory.json` | truncated canonical JSON | exact `.corrupt` backup and regenerated canonical file | PASS |
| `memory2-empty` | `memory2.json` | zero-byte canonical file | exact `.corrupt` backup and regenerated canonical file | PASS |
| `semantic-wrong-root` | `semantic-memory.json` | JSON array instead of the required root object | exact `.corrupt` backup and regenerated canonical file | PASS |
| `relationships-incompatible-schema` | `relationships.json` | incompatible version/schema | exact `.corrupt` backup and regenerated canonical file | PASS |
| `voices-stale-temp` | `voices.json` | valid stale `.tmp` beside a valid canonical file | canonical bytes preserved and stale temp removed without backup | PASS |
| `operator-lore-invalid-orphan-temp` | `operator-lore.json` | invalid orphan `.tmp` without canonical file | exact `.tmp.corrupt` backup and regenerated canonical file | PASS |

## Required invariants

For every destructive case the machine-readable report proves:

- the injected payload SHA-256 and size are recorded;
- the expected backup preserves the exact injected bytes;
- the recovered canonical file is valid JSON with the expected root type;
- the other five canonical stores retain their baseline paths and SHA-256 values;
- no `.tmp` file remains after recovery;
- the second isolated JVM exits cleanly and produces identical canonical store state;
- recovery backup bytes do not change on the second startup;
- no production lifecycle fixture is accidentally executed in recovery mode;
- strict fixture-ready and crash-signature checks remain active.

## Machine-readable evidence

Artifact:

```text
persistence-recovery-231
artifact id: 8943164115
digest: sha256:3e287684e6068ce7961698a31f1152ac75e50147acd383bbeeeb4ffe7fc2d739
```

Report terminal fields:

```json
{
  "schema": 1,
  "scenario": "VAI-PERSIST-003",
  "status": "PASS",
  "minecraftVersion": "1.21.1",
  "loaderVersion": "0.19.3",
  "candidateSha256": "1036fa1e9aebdba6b6b8f0ab9a96596e9f6b8855f664e5edf90255a02535924c"
}
```

## Runtime implementation

All six stores use the shared `JsonStoreRecovery` contract with their existing schema codecs:

```text
memory.json
memory2.json
semantic-memory.json
relationships.json
voices.json
operator-lore.json
```

The shared layer owns atomic replacement, deterministic corrupt backup and orphan temporary-file recovery. Individual stores retain ownership of schema validation, sanitization and default state.

## Security boundary

- no real operator world is read or modified;
- no provider key or public provider request is used;
- no reflection or `setAccessible` exception was added;
- the recovery helper remains an imported CI library and is classified accordingly in the deterministic script inventory;
- PR and release workflow parity is protected by source-policy tests;
- the distributable package contains no acceptance fixture classes.

## Decision

`VAI-PERSIST-003` is now `AUTOMATED`. Recovery of malformed auxiliary persistence is no longer a manual release-regression responsibility. Physical/client canaries remain separate and unchanged.
