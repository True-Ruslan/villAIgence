# VillAIgence 0.3.2 Corrective Installed Validation

Status: **INSTALLED / OPERATIONAL / CORRECTIVE CANARY PASS**

## Release boundary

- Release: `0.3.2+1.21.1`
- Artifact: `villaigence-fabric-0.3.2+1.21.1.jar`
- SHA-256: `b51cfcf3f46718fac9620586cf8b5aae53356c600d5ac375ca3280050befe015`
- Release commit: `3bb39e7ed126163efcdf971e85c89a4a5efd3111`
- GitHub Release workflow: `VillAIgence GitHub Release #897` / run `31879227075` / `SUCCESS`
- Release id: `371021968`, asset id: `515590903`
- Installed validation date: `2026-09-04` (operator-reported; exact local time not recorded in this session)
- Result: **INSTALLED CORRECTIVE CANARY PASS**

This validation repeats only the corrective `VAI-PCM-MULTI-001` boundary from `docs/livingworld/TEST_PLAN_0.3.2_CORRECTIVE_INSTALLED.md`. Previously accepted voice, STT, security, restart and other manual canaries were not repeated.

## Operator-reported result

The operator ran the full test plan on the retained private test-server world (same Muammer/Nurey markers used for the `0.3.0`/`0.3.1` canaries, not re-taught) and reported every required acceptance-matrix row as PASS:

```text
Official release SHA         PASS
Embedded version              PASS
Active installed JAR SHA      PASS (matches b51cfcf3f46718fac9620586cf8b5aae53356c600d5ac375ca3280050befe015)
Startup gate                  PASS
Pre-dialogue persistence      unchanged
Muammer retained source       present
Muammer exact recall          PASS (amber-pine-314)
Nurey exact recall            PASS (violet-river-926)
Cross-NPC isolation           PASS
Duplicate event check         PASS
memory2.json validity         PASS
LinuxGSM monitor              OK

VAI-PCM-MULTI-001             PASS
```

**Evidence-capture note:** the exact request/response transcripts, new persisted event IDs, pre/post-install persistence hashes and backup path called for by the test plan's "Evidence to retain" section were not transcribed into this repository in this session. This record reflects the operator's direct report of a full-matrix PASS on the exact official `0.3.2+1.21.1` asset; it does not fabricate transcript-level detail that was not provided. If the raw evidence (logs, hashes, exact reply text, event UUIDs) exists in the operator's own backup, it should be attached to this file or referenced from it for future audit.

## Explicit deferred installed evidence

These boundaries remain unchanged and must not be promoted by inference:

```text
VAI-M2-INST-005  NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004   NOT TESTED / DEFERRED
```

## Final disposition

```text
Official SHA            PASS
Startup gate            PASS
Muammer recall          PASS
Nurey recall            PASS
Cross-NPC isolation     PASS
Duplicate check         PASS
Persistence validity    PASS
VAI-PCM-MULTI-001       PASS
LinuxGSM monitor        OK
VAI-M2-INST-005         NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004          NOT TESTED / DEFERRED
```

`0.3.2+1.21.1` is installed, operational, and the corrective installed acceptance gate `VAI-PCM-MULTI-001` is **PASS**. Under the delivery contract, this result satisfies the sole remaining blocker documented in `docs/livingworld/TEST_PLAN_0.3.2_CORRECTIVE_INSTALLED.md` and authorizes transition to the `0.4` Knowledge ecosystem milestone.

`0.3` is now fully released and installed-accepted at `0.3.2+1.21.1`. `VAI-M2-INST-005` and `VAI-CONCUR-004` remain explicit unrelated deferrals and are not promoted by this result.
