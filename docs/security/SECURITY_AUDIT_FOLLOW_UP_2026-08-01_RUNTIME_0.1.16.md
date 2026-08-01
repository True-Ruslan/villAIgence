# Security Audit Follow-Up — 0.1.16 Runtime Acceptance

**Date:** 2026-08-01  
**Release:** `0.1.16+1.21.1`  
**Commit:** `521568f903078b91dd5817cdc9a551bd2392e663`  
**JAR SHA-256:** `036cbacc657ceb676813f41ee293024690b981e971e7c6037fc5d3ecbe3ee062`

## Scope

This follow-up reconciles the isolated real-server acceptance evidence for:

```text
SEC-003 bounded provider responses
SEC-004 constrained account verification
SEC-007 bounded voice capture and aggregate PCM
```

Canonical detailed evidence is recorded in:

```text
docs/livingworld/VALIDATION_0.1.16.md
```

## SEC-003 — Closed

### Evidence

- Chat JSON stopped at 8 MiB;
- STT JSON stopped at 4 MiB;
- TTS audio stopped at 64 MiB;
- provider error bodies stopped at 256 KiB;
- declared and chunked/unknown-length paths were exercised;
- slow-drip ended at `600.026 s` under the ten-minute total deadline;
- Chat/STT/TTS redirects returned `307` and were not followed;
- `redirect_target_hits=0`;
- no `OutOfMemoryError`, server crash, body disclosure or credential disclosure occurred.

### Persistence reconciliation

The original acceptance procedure applied one generic empty-diff requirement to every negative stage. That was incorrect for TTS failures.

VillAIgence persists visible text and the authoritative DIALOGUE after successful STT+Chat and before optional TTS. A TTS size, error or redirect failure must preserve that dialogue. Requiring byte-identical `memory.json` and `memory2.json` would contradict the established fail-soft requirement and would encourage data loss.

The corrected acceptance boundary is:

```text
Chat/STT rejection before a dialogue exists
→ no dialogue persistence mutation

TTS rejection after successful Chat
→ one legitimate dialogue remains persisted
→ no duplicate dialogue or duplicate side effect
→ no mutation caused solely by redirect following or excess-body consumption
```

The observed `memory.json` and `memory2.json` updates in the TTS-negative scenarios are therefore expected and are not a SEC-003 failure.

**Status:** Closed by the `0.1.16+1.21.1` controlled acceptance evidence and the corrected persistence interpretation.

## SEC-004 — Open

### Passed evidence

- bounded 64 KiB verification responses;
- declared and chunked oversize rejection;
- redirect returned `307`;
- redirect target received zero requests;
- literal-loopback, user-info, fragment and non-loopback restrictions otherwise worked.

### Remaining defect

The acceptance-only probe allowed `https://127.0.0.1` and `https://[::1]`. Against the HTTP-only local harness this reached a TLS attempt and returned `IO_ERROR / SSLException` instead of being rejected before connection.

The probe must be narrower than general production transport configuration:

```text
acceptance probe: literal loopback + HTTP only
production /mca verify: fixed trusted provider family + HTTPS only
```

A focused code fix and release-JAR retest are required. The production `/mca verify` trust boundary is not weakened by this acceptance-probe defect.

**Status:** Open.

## SEC-007 — Closed

### Evidence

```text
voice duration clamp low             1 second
voice duration clamp high            120 seconds
aggregate PCM maximum                134217728 bytes
accepted 1 MiB reservations          128
rejected 1 MiB reservations          128
observed peak                        134217728 bytes
final reserved bytes                 0
full-budget recovery reservation     success
```

Production microphone, STT, Chat, TTS, Simple Voice Chat and Opus worked after the probe. The final server restart and six-file persistence comparison were clean.

**Status:** Closed by the `0.1.16+1.21.1` controlled acceptance evidence.

## Residual risk and next action

Only SEC-004 remains open. The required fix is isolated to `AccountVerificationAcceptanceProbe.validateLoopbackUri` and must not alter:

- `AccountVerificationTransport` bounded/no-redirect behavior;
- production trusted-origin construction;
- Chat/STT/TTS provider endpoint policy;
- persistence or voice behavior.

After the fixed JAR rejects both HTTPS loopback forms before connection and the HTTP success/redirect cases still pass, Step 1 security hardening can be marked fully complete.
