# VillAIgence 0.1.26 installed canary evidence

Date: 2026-08-06

Candidate:

```text
version:       0.1.26+1.21.1
workflow:      VillAIgence GitHub Release #338 / run 31086618176
PR head:       cd5ae877582e1aa13d870b5ab66eaa27ec48e727
artifact:      villaigence-fabric-package / 8961845829
candidate JAR: villaigence-fabric-dry-run-338.jar
JAR SHA-256:   5728f0f1a57b4c268df9b73603539f09ca30945a2ba251e72a5169ab45ae0a53
```

## Operator result

```text
VAI-BOOT-002    PASS
VAI-NAV-001     PASS
VAI-GAME-001    PASS
VAI-GAME-003    PASS
VAI-AI-006      PASS after switching Chat model to google/gemini-2.5-flash-lite
VAI-CONCUR-004  NOT TESTED — no second graphical client was available

Total: 5 PASS / 0 FAIL / 1 NOT TESTED
```

## Evidence interpretation

The five executed installed canaries passed on the exact candidate identified above. No executed canary failed.

`VAI-CONCUR-004` is explicitly **not** represented as PASS. It remains an installed graphical two-client validation gap. Automated authenticated two-session acceptance already proves server authority, revision conflicts, owner-bound responses, retained drafts and explicit retry at the logical-session layer, but that evidence does not prove graphical presentation or real two-client interaction.

The operator accepted deferring `VAI-CONCUR-004` to a future release because a second graphical client was unavailable. This is a documented release-risk exception, not a reclassification of the scenario and not evidence that the scenario passed.

## Deferred scenario

A future installed validation must use two graphical clients and confirm:

1. both clients load the same Operator Lore revision;
2. the first client writes successfully;
3. the second client receives a visible stale-revision conflict;
4. the second client's draft is retained;
5. reload/review exposes the canonical value and revision;
6. an explicit retry succeeds exactly once;
7. neither client receives the other session's response;
8. no blind overwrite or silent draft loss occurs.

Until that test is executed, reports must state:

```text
VAI-CONCUR-004: DEFERRED / NOT TESTED
```

## Release decision

Release `0.1.26+1.21.1` may proceed with the accepted limitation above because:

- the complete exact-candidate automated release matrix passed;
- five available installed canaries passed;
- there were zero installed failures;
- the unexecuted boundary is narrowly graphical/two-client;
- equivalent server-side concurrency and authority contracts are automated;
- the missing graphical evidence remains visible and must be retried in a future version.

This document does not weaken the acceptance catalog or convert `VAI-CONCUR-004` to automated or passed status.
