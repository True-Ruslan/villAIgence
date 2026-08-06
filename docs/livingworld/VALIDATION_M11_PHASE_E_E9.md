# M11 Phase E — E9 risk selection and production soak validation

## Status

PASS on 2026-08-06.

This document records exact implementation and independent-review evidence for E9. It does not request a version, create a tag, merge PR #114 or publish a release.

## Exact validated implementation head

```text
78d7961632501b038d233dd662c62384d81a7c3b
```

## Mandatory workflow evidence

| Gate | Run | Result |
| --- | ---: | --- |
| VillAIgence CI | 1721 / `31083451312` | PASS |
| Java Pull Request CI with Gradle | 1107 / `31083451053` | PASS |
| Repository security policy | 1347 / `31083451124` | PASS |
| Supply-chain verification | 167 / `31083451252` | PASS |
| VillAIgence Production Soak | 14 / `31083451193` | PASS |
| VillAIgence GitHub Release dry-run | 333 / `31083451015` | PASS |

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
- persistence infrastructure changes select all five suites;
- every production `*Store.java` under LivingWorld selects all five suites;
- voice, navigation and generic runtime changes select fast, server, production and package;
- documentation-only changes select the fast contract suite.

This is fail-closed optimization: classification may remove work only for explicitly reviewed paths. Failure to classify never removes a mandatory gate.

### TDD evidence

The selector was developed and reviewed through independently observed RED states:

1. missing module produced `ModuleNotFoundError: select_acceptance_suites`;
2. missing CLI entrypoint produced an import failure for `main`;
3. missing workflow wiring produced seven focused policy failures;
4. missing release/soak parity produced four focused policy failures;
5. independent review found that the six canonical store implementations selected runtime suites but omitted `recovery`;
6. the review regression test produced seven focused failures: six current stores and one future LivingWorld `*Store.java`;
7. the matcher was corrected and the complete selector suite passed before any expensive gate ran.

The final implementation passes API, CLI, store-classification and workflow-policy contracts in normal CI, release validation and the soak workflow.

### Independent change review

The complete PR #114 diff was reviewed after E9 implementation, with focused inspection of:

- changed-path collection and fail-closed classification;
- release-mode `all=true` enforcement;
- workflow permissions and publication conditions;
- Gradle fork-heap compatibility;
- production soak failure/report paths;
- corrupt-store backup and atomic replacement;
- authenticated text and Operator Lore authority boundaries;
- tombstone replay identity protection;
- exact-package and no-release side effects.

One P2 finding was found and fixed: canonical store-class changes did not directly select the destructive recovery matrix in normal CI. Release dry-run already protected those paths, so published-release safety was not bypassed, but E9's main-CI fail-closed contract was incomplete. The final matcher treats any production LivingWorld `*Store.java` as persistence-sensitive while leaving ordinary voice/runtime classes on the narrower runtime suite.

After the fix and full re-review, no open P0, P1, P2 or P3 findings remained.

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

PR #114 modifies protected workflow, build and CI-script paths, therefore the selector correctly chose the complete five-suite matrix rather than an optimized subset. The fresh main-CI run executed and passed the selected recovery matrix.

### Release integration

The release workflow runs the selector with `--mode release`, then fails unless `all=true`. It does not conditionally skip any release acceptance stage.

Changes to the soak workflow, soak harness or soak tests are release-triggering paths. The release contract suite executes selector and soak-harness tests before exact production acceptance.

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
artifact: production-soak-14
artifact id: 8960538615
digest: sha256:6567b4a8cad895945a204a8a64b64e7a9078ab989b6d6e7db3c242a56fbd1c83
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
production-server-acceptance-333
artifact id: 8960504586
digest: sha256:00985742a59aaf8db9617a57c2cca586a2a7184ffec26cf006c2b6650fbab2e0

persistence-recovery-333
artifact id: 8960600598
digest: sha256:85051aaef1fef9b0d0e04055437aa4ebdf20cebd02737b75b4c1efd5c474a4c3

villaigence-fabric-package
artifact id: 8960636786
digest: sha256:d8e9574874414e28e7f583319f4a3386367f2cb37ddc26597d355053da129558
```

The production-accepted JAR and packaged JAR passed byte-identity verification.

## Manual boundary after Phase E

Phase E removes routine manual regression for deterministic server behavior. It does not make physical or visual claims that CI cannot observe.

The six remaining catalog canaries are:

1. exact released/candidate JAR startup in the operator environment;
2. two ordinary MCA NPC brains visibly escape reachable water;
3. an installed client visibly addresses the selected NPC and renders one response;
4. a real player performs Silk Touch grave pickup, placement and restart without loss or duplication;
5. one physical microphone turn traverses OS permission, client capture and UDP routing and yields an audible spatial response;
6. two installed graphical clients visibly render Operator Lore conflict, keep/reload the draft and complete an explicit retry.

All other catalog scenarios are automated. There are no remaining `PLANNED` scenarios in `common/src/test/resources/acceptance/scenarios.tsv`.

## Decision

M11 Phase E implementation and independent code review are complete at the automation layer.

PR #114 remains draft and unmerged. No release should be requested until the final documentation-head workflows complete and the six installed canaries are prepared as the next delivery boundary.
