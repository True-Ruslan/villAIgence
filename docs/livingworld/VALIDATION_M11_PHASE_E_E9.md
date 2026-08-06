# M11 Phase E — E9 risk selection and production soak validation

## Status

PASS on 2026-08-06.

This document records exact-head evidence for E9. It does not request a version, create a tag, merge PR #114 or publish a release.

## Exact validated implementation head

```text
20fb7fd741916ffcb3f7f4630fd6d0bb046efba7
```

## Mandatory workflow evidence

| Gate | Run | Result |
| --- | ---: | --- |
| VillAIgence CI | 1715 / `31081408589` | PASS |
| Java Pull Request CI with Gradle | 1101 / `31081408606` | PASS |
| Repository security policy | 1335 / `31081408597` | PASS |
| Supply-chain verification | 161 / `31081408667` | PASS |
| VillAIgence Production Soak | 8 / `31081408703` | PASS |
| VillAIgence GitHub Release dry-run | 327 / `31081408638` | PASS |

The release workflow ran in dry-run mode. Its `github-release` job was intentionally skipped, so no tag or release was created.

## Fail-closed acceptance selector

`scripts/ci/select_acceptance_suites.py` maps changed repository paths to five explicit suites:

```text
fast
server
production
recovery
package
```

The selector is deterministic and has no GitHub API or network dependency. It receives a newline-delimited changed-path file and emits sorted GitHub Actions outputs.

### Selection policy

- release mode always selects all five suites;
- an empty change set selects all five suites;
- absolute, parent-traversal, unknown or unclassified paths select all five suites;
- workflow, build, Gradle, production-fixture and CI-script changes select all five suites;
- persistence runtime changes select all five suites;
- voice, navigation and generic runtime changes select fast, server, production and package;
- documentation-only changes select the fast contract suite.

This is fail-closed optimization: classification may remove work only for explicitly reviewed paths. Failure to classify never removes a mandatory gate.

### TDD evidence

The selector was developed through independently observed RED states:

1. missing module produced `ModuleNotFoundError: select_acceptance_suites`;
2. missing CLI entrypoint produced an import failure for `main`;
3. missing workflow wiring produced seven focused policy failures;
4. missing release/soak parity produced four focused policy failures.

The final implementation passes the API, CLI and workflow-policy contract tests in normal CI, release validation and the soak workflow.

### Main CI integration

The PR/push workflow collects changed paths against the authoritative base/before SHA and exposes selector outputs through `id: acceptance`.

Expensive steps are guarded independently:

```text
fast       → common and Python acceptance contracts
server     → risk catalog, 16 Fabric GameTests, Fabric and NeoForge builds
production → exact staged candidate startup/restart and lifecycle evidence
recovery   → six-case destructive persistent-store recovery matrix
package    → distributable package smoke
```

PR #114 modifies protected workflow, build and CI-script paths, therefore the selector correctly chose the complete five-suite matrix rather than an optimized subset.

### Release integration

The release workflow runs the selector with `--mode release`, then fails unless `all=true`. It does not conditionally skip any release acceptance stage.

Changes to the soak workflow, soak harness or soak tests are release-triggering paths. The release contract suite executes the soak harness tests before exact production acceptance.

## Bounded production soak

`.github/workflows/livingworld-soak.yml` runs weekly, on relevant pull-request changes and by manual dispatch.

The workflow is bounded by:

```text
timeout:                 90 minutes
common test JVM heap:    512 MiB
Gradle max workers:      2
production server heap:  512 MiB
concurrency repetitions: 3
production JVM cycles:   5
```

No external provider, paid request or repository secret is required.

### Repeated authenticated concurrency

Three clean Gradle executions run with `--rerun-tasks` and constrained fork heap:

- `AuthenticatedTextTurnAcceptanceTest`;
- `OperatorLoreNetworkSessionAcceptanceTest`.

This prevents a cached test result from being reported as repeated concurrency evidence.

### Production restart harness

`scripts/ci/production_soak_acceptance.py` installs and stages the exact remapped candidate once, then starts and cleanly stops the same generated world five times.

Every cycle requires:

- strict candidate version/startup markers;
- fixture-ready terminal state;
- controlled `stop` command;
- save and exit code zero;
- absence of forbidden Mixin, refmap, mod-resolution and JVM crash signatures;
- valid lifecycle evidence;
- exactly one live fixture NPC;
- real Simple Voice Chat transport status `PASS`;
- bounded PCM evidence;
- exactly one valid instance of every canonical persistent store;
- SHA-256 equality with the first completed cycle.

Cycle one must report `CREATED`. Cycles two through five must report `RESTART_VERIFIED`.

## Inspected soak artifact

```text
artifact: production-soak-8
artifact id: 8959713111
digest: sha256:a623cd6e662a1b4e6759ad4ba15a1fe1c646b87d374442435f2cc8bc1ef78c9f
```

The downloaded `production-soak-report.json` was inspected after the workflow completed.

```text
status:            PASS
cycles:            5
maxHeapMiB:        512
candidateVersion:  1.21.1-SNAPSHOT
candidateSha256:   a18bddd8333a73939adbff22ad2b4ff33f382ee9bff42db0797778815e6c2d46
```

Observed cycle evidence:

| Cycle | Exit | Lifecycle | Live NPCs | Voice | Peak PCM bytes |
| ---: | ---: | --- | ---: | --- | ---: |
| 1 | 0 | CREATED | 1 | PASS | 7680 |
| 2 | 0 | RESTART_VERIFIED | 1 | PASS | 7680 |
| 3 | 0 | RESTART_VERIFIED | 1 | PASS | 7680 |
| 4 | 0 | RESTART_VERIFIED | 1 | PASS | 7680 |
| 5 | 0 | RESTART_VERIFIED | 1 | PASS | 7680 |

All six hashes were identical in all five cycles:

```text
memory.json          56108e4ce630802d19b22d29ebd66777b8e8437b229e456b66a61e6543139920
memory2.json         54fc94ea83f6b83c5eaec8dce4ae5e5fb65cc60dd0c9da82076ba5c64f506aa9
operator-lore.json   acbb545ff9ec1aef86f53041e94b44025897322da3365c6c9809399f23497c04
relationships.json   9c3debc80ec56dd85ab3eba640e3db1e22bd7bf8c4c09ba84bc9eadd130612f3
semantic-memory.json c932e23d6940df02617823e954af875feec4022f0dd667c9aa3aacc8392ea313
voices.json          4211e9e5d493f46975514e252f52d1c3bf379648b34325d94ac9673c7e180158
```

## Exact release dry-run artifacts

```text
production-server-acceptance-327
artifact id: 8959844583
digest: sha256:c631cd41d925121e72994c2b1457b480b88f0d9ddba71aeef3a1181fa8615dac

persistence-recovery-327
artifact id: 8959920387
digest: sha256:b67ff4b3d965aab1d6e15b2c57e5b0881450325f33a894fa82a30da04283d92f

villaigence-fabric-package
artifact id: 8959947498
digest: sha256:2cec6fb4588cb40db7557eaae4a48e47311964f9132167a1f75ed4187bfbcb35
```

The production-accepted JAR and packaged JAR passed byte-identity verification.

## Manual boundary after Phase E

Phase E removes routine manual regression for deterministic server behavior. It does not make physical or visual claims that CI cannot observe.

The remaining release canaries are limited to:

1. the exact released JAR starts and connects in the operator environment;
2. one installed player performs the visible Silk Touch grave interaction;
3. two installed graphical clients visibly render Operator Lore conflict/reload/keep-draft behavior;
4. one real microphone input traverses OS permission, client capture and UDP routing;
5. one spatial NPC reply is audibly correct and has no obvious visual/audio defect.

All other catalog scenarios are automated. There are no remaining `PLANNED` scenarios in `common/src/test/resources/acceptance/scenarios.tsv`.

## Decision

M11 Phase E implementation is complete at the automation layer.

PR #114 must remain unmerged and no release should be requested until final exact-head documentation CI and change review are complete. The next product delivery boundary is the focused installed canary package followed by the next free exact release version.
