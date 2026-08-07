# VillAIgence 0.1.26+1.21.1 — completed release evidence

Date: 2026-08-07

This document is the canonical final release/publication record for `0.1.26+1.21.1`.

## Immutable release identity

```text
tag:             0.1.26+1.21.1
release commit:  40ce7cb77e9b9178fd96fd91025cee22ba686dc0
release PR:      #115
```

The tag resolves to exactly `40ce7cb77e9b9178fd96fd91025cee22ba686dc0` and was not moved during recovery.

## Installed acceptance

The exact candidate installed on the operator server/client produced:

```text
VAI-BOOT-002    PASS
VAI-NAV-001     PASS
VAI-GAME-001    PASS
VAI-GAME-003    PASS
VAI-AI-006      PASS after Chat model was switched to google/gemini-2.5-flash-lite
VAI-CONCUR-004  NOT TESTED — no second graphical client was available

Total: 5 PASS / 0 FAIL / 1 NOT TESTED
```

`VAI-CONCUR-004` remains explicitly deferred. It is not represented as PASS. Automated authenticated two-session acceptance covers server authority, optimistic revision, response ownership, retained draft and reviewed retry, but does not claim real two-client graphical presentation.

Canonical installed detail is also recorded in:

```text
docs/livingworld/VALIDATION_0.1.26_INSTALLED_CANARIES.md
```

## Accepted and published artifact identity

```text
JAR:
villaigence-fabric-0.1.26+1.21.1.jar

JAR SHA-256:
5728f0f1a57b4c268df9b73603539f09ca30945a2ba251e72a5169ab45ae0a53

Dependency manifest:
villaigence-dependencies-0.1.26+1.21.1.txt

Dependency manifest SHA-256:
b16a7b842776d44ed21cad1b56cee63aadc782ada457c108c5107c483aab5816
```

The final recovery build reproduced the same JAR SHA-256 as the installed canary candidate.

## GitHub Actions outage and recovery

The initial merge-triggered release flow was interrupted by a GitHub Actions service outage after the immutable tag and GitHub Release record existed but before release assets were complete. The original run became stuck and could not be cleanly rerun.

Recovery was implemented through PR #116 without creating, deleting or moving the release tag.

```text
recovery PR:             #116
recovery control commit: ae551b81d221ce88ceebfce96b1038afa718da50
recovery workflow:       VillAIgence Release Recovery #4
recovery run id:         31154864224
result:                  PASS
```

The recovery workflow is fail-closed:

- resolves an already-existing release tag;
- requires its commit to belong to `1.21.1` history;
- checks out and validates the immutable tag commit rather than the recovery-control commit;
- runs the complete release acceptance suite;
- never creates, deletes or moves a release tag;
- keeps repository permissions read-only except for one explicitly policy-approved asset-restore job;
- publishes only on a `push` event after PR validation;
- re-downloads the published assets and compares them byte-for-byte with the verified recovery package.

## Post-merge recovery proof

`VillAIgence Release Recovery #4` passed:

```text
immutable target resolution                         PASS
exact tag-commit checkout                           PASS
complete release-suite selection                    PASS
exact release identity                              PASS
repository security                                 PASS
production acceptance contracts                     PASS
exact production startup/restart + lifecycle        PASS
six-case persistence recovery                       PASS
risk catalog + Fabric GameTests                     PASS
Fabric build                                        PASS
NeoForge build                                      PASS
dependency manifest                                 PASS
package smoke                                       PASS
production-accepted/package JAR byte identity       PASS
verified recovery artifact upload                   PASS
immutable tag re-verification                       PASS
GitHub Release metadata/assets restore              PASS
published asset re-download                         PASS
published/local byte-for-byte comparison            PASS
```

Final published assets:

```text
villaigence-fabric-0.1.26+1.21.1.jar
villaigence-fabric-0.1.26+1.21.1.jar.sha256
villaigence-dependencies-0.1.26+1.21.1.txt
```

## Restart claim

The published JAR is byte-identical to the exact installed candidate that passed startup/restart and the installed grave/restart/resurrection canary. No separate temporal claim is made that the operator performed another manual restart only after the GitHub Release assets became visible.

## Release verdict

`0.1.26+1.21.1` is published and accepted with one explicitly deferred graphical boundary:

```text
release publication: COMPLETE
runtime/candidate failures: 0
installed acceptance: 5 PASS / 0 FAIL / 1 NOT TESTED
VAI-CONCUR-004: DEFERRED, NOT PASS
published artifact identity: VERIFIED
immutable tag identity: VERIFIED
```

The next development package is the additive, deterministic and reversible legacy `memory.json` migration. The release boundary no longer blocks that work.
