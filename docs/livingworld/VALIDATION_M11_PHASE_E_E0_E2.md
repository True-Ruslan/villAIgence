# M11 Phase E — E0 to E2 validation

## Status

PASS on 2026-08-05.

This document records the exact evidence for the first three Phase E workstreams. It does not request or publish a release.

## Exact validated head

```text
498572428047784ef9e4b4b3e62dbe459cf0cbe2
```

The later catalog-only reconciliation commit does not change runtime behavior.

## Mandatory exact-head workflows

| Gate | Run | Result |
| --- | ---: | --- |
| VillAIgence CI | 1575 / `31031943534` | PASS |
| Java Pull Request CI with Gradle | 961 / `31031941807` | PASS |
| VillAIgence GitHub Release dry-run | 187 / `31031941485` | PASS |
| Repository security policy | 1055 / `31031941342` | PASS |

The release job ran in dry-run mode. `github-release` was intentionally skipped and no tag or GitHub release was created.

## E0 — production acceptance infrastructure

Validated properties:

- `stageProductionAcceptanceRuntime` no longer resolves `Project` state during task execution;
- configuration-time providers supply Minecraft, loader, installer and mod identity values;
- the exact remapped candidate and production acceptance fixture remain separate artifacts;
- production staging completes without the previous `Task.project` configuration-cache diagnostics;
- Fabric and NeoForge builds remain green.

## E1 — duplicate identity replay

Automated scenario:

```text
VAI-LIFE-005
```

Validated properties:

- a consuming restoration is rejected while the same UUID is live;
- rejected replay does not clear the authoritative grave data;
- after the conflicting live identity is removed, one restoration succeeds;
- UUID, name and complete inventory multiset remain unchanged;
- the GameTest is explicitly registered and CI requires `VAI-LIFE-005-AUTOMATED-PASS`.

## E2 — real lifecycle across two JVMs

The production acceptance fixture executes this sequence against the exact remapped candidate:

```text
real MCA villager
-> fatal MCA death path
-> tombstone capture
-> portable grave serialization
-> original UUID removal
-> production TombstoneBlock resurrection pipeline
-> exact identity and inventory verification
-> clean stop
-> second isolated JVM
-> exact identity and inventory verification after restart
```

The headless fixture force-loads only its generated test chunks, removes the force-load during shutdown and never touches an operator world.

### Machine-readable lifecycle evidence

```json
{
  "scenario": "VAI-LIFE-002",
  "status": "PASS",
  "firstRun": {
    "phase": "CREATED",
    "npcUuid": "5cf53206-ec2c-4c88-ad11-a8bbc56f514e",
    "npcName": "Production Lifecycle Acceptance",
    "inventory": {
      "minecraft:bread": 11,
      "minecraft:emerald": 3,
      "minecraft:iron_sword": 1
    },
    "nonEmptyStacks": 3,
    "liveEntityCount": 1,
    "portableGraveConsumed": true
  },
  "secondRun": {
    "phase": "RESTART_VERIFIED",
    "npcUuid": "5cf53206-ec2c-4c88-ad11-a8bbc56f514e",
    "npcName": "Production Lifecycle Acceptance",
    "inventory": {
      "minecraft:bread": 11,
      "minecraft:emerald": 3,
      "minecraft:iron_sword": 1
    },
    "nonEmptyStacks": 3,
    "liveEntityCount": 1,
    "portableGraveConsumed": true
  }
}
```

### Process evidence

- first JVM duration: `53194 ms`;
- second JVM duration: `15617 ms`;
- both processes received `stop`, saved all dimensions and exited with code `0`;
- first log contains `VAI-LIFE-002-CREATED` and `VILLAIGENCE_PRODUCTION_FIXTURE_READY`;
- second log contains `VAI-LIFE-002-RESTART-VERIFIED` and `VILLAIGENCE_PRODUCTION_FIXTURE_READY`;
- no unexpected-exception or crash-report signature is allowed after fixture readiness;
- all six canonical persistent-store SHA-256 values remained identical across restart.

## False-positive startup defect closed

During E2 implementation the old harness was proven capable of accepting a server that printed Minecraft's vanilla `Done` marker and then crashed inside the fixture. Phase E now requires the later VillAIgence fixture-ready marker and rejects:

```text
Encountered an unexpected exception
This crash report has been saved to:
```

The process tests also verify pipe cleanup; the earlier unclosed-pipe `ResourceWarning` diagnostics are absent from the passing exact-head run.

## Release-gate parity

Both pull-request and release gates now require:

- strict process readiness;
- lifecycle contract tests;
- two-JVM lifecycle execution;
- `lifecycle-report.json` with `status=PASS`;
- exact accepted-JAR versus packaged-JAR byte identity.

`ProductionAcceptanceGateParityPolicyTest` prevents the two workflows from silently diverging.

## Remaining manual boundary

`VAI-LIFE-002` is automated. `VAI-GAME-003` remains a focused installed canary because a real player's Silk Touch break, pickup, placement and visual interaction are outside the headless production fixture. The deterministic death, portable data, resurrection and restart mechanisms are no longer manual-only.