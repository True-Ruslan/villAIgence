# Security Audit Follow-Up — 0.1.17 SEC-004 Closure

**Date:** 2026-08-01  
**Repository:** `True-Ruslan/villAIgence`  
**Branch:** `1.21.1`  
**Implementation merge:** PR #70 — `88a20d86e8b08e4b5eaf60da943a63e750f2b545`  
**Release:** `0.1.17+1.21.1`  
**Release JAR SHA-256:** `b33af40f7a2696dc679c49e0fc544f6b5df99e0aa600ea5c767bc5a9747da1ab`  
**Validation marker:** `V0117_SEC004_ARTIFACT_AND_EVIDENCE_PASS`

## Purpose

Close the final open finding from the Step 1 security and supply-chain audit after validating the exact official release artifact:

```text
SEC-004 constrained account verification
```

SEC-003 and SEC-007 were closed by the controlled `0.1.16+1.21.1` server acceptance. SEC-004 remained open only because the acceptance-only probe permitted HTTPS loopback input and attempted TLS against the HTTP-only local harness.

## Implementation reviewed

PR #70 restricted `AccountVerificationAcceptanceProbe` to:

```text
scheme = http
host   = literal loopback IP
```

The change did not alter:

- production `/mca verify` trusted-origin construction;
- `AccountVerificationTransport` size limits or no-redirect behavior;
- Chat/STT/TTS provider clients;
- endpoint-family credential binding;
- Minecraft startup or commands;
- persistent formats or runtime side effects.

Regression tests cover rejection of both HTTPS IPv4 and HTTPS IPv6 loopback targets.

## Automated evidence

The implementation head passed the required matrix before merge:

```text
VillAIgence CI #988 / 30688283177             SUCCESS
Java Pull Request CI #511 / 30688283172       SUCCESS
Repository security policy #182 / 30688283175 SUCCESS
Fabric package smoke-check                    SUCCESS
```

The prior RED run was retained as TDD evidence:

```text
VillAIgence CI #976 / 30688049936
AccountVerificationAcceptanceProbeTest
→ acceptsOnlyLiteralHttpLoopbackUrisWithoutUserInfoOrFragment() FAILED
```

That failure demonstrated that the test rejected HTTPS loopback while the old implementation still accepted it.

## Exact-release-artifact evidence

Tested artifact:

```text
release     0.1.17+1.21.1
commit      88a20d86e8b08e4b5eaf60da943a63e750f2b545
SHA-256     b33af40f7a2696dc679c49e0fc544f6b5df99e0aa600ea5c767bc5a9747da1ab
```

Focused results:

```text
HTTP success              SUCCESS / 200
HTTP redirect             HTTP_ERROR / 307
redirect followed         no
redirect_target_hits      0
declared oversize         TOO_LARGE / ResponseTooLargeException
chunked oversize          TOO_LARGE / ResponseTooLargeException
HTTPS loopback            rejected before connection
HTTPS rejection exit      2
SSLException              none
```

Harness evidence contained exactly four HTTP requests: success, redirect, declared oversize and chunked oversize. HTTPS input was rejected before any network request.

The harness was stopped and port `18080` was released after the run.

## Source-build checksum note

A local source build in the validation environment stopped before compilation because dependency verification did not contain checksum records for transitive Fabric artifacts resolved there.

Security interpretation:

```text
dependency verification bypassed    no
unverified dependency accepted       no
local candidate used                 no
official release JAR used            yes
artifact SHA-256 recorded            yes
```

The build failed closed as intended. The exact official CI/release JAR was used under the documented alternate acceptance path, so this does not invalidate SEC-004 artifact evidence.

The checksum-resolution difference is retained as a build-maintenance follow-up. If local cold-build support for that dependency graph is required, metadata must be refreshed only through `docs/security/DEPENDENCY_UPDATE_PROCEDURE.md`; verification must remain enabled.

## Finding disposition

### SEC-004 — Closed

Closure conditions:

```text
implementation merged                         PASS
focused regression tests                      PASS
common/Fabric/NeoForge and package CI          PASS
repository security policy                    PASS
HTTP success retained                         PASS
64 KiB declared/chunked bounds retained        PASS
redirect not followed                         PASS
redirect target hits                          0
HTTPS loopback rejected before connection     PASS
exact official JAR identity recorded           PASS
```

Residual risk:

- the acceptance probe is an explicit operator tool and not a production Minecraft path;
- production verification still depends on availability and correctness of the fixed trusted HTTPS provider;
- future dependency graph changes may require controlled verification-metadata refreshes for new local build environments.

No open Step 1 audit finding remains.

## Step 1 final status

```text
SEC-001 Closed
SEC-002 Closed
SEC-003 Closed
SEC-004 Closed
SEC-005 Closed
SEC-006 Closed
SEC-007 Closed
SEC-008 Closed
SEC-009 Closed
```

Step 1 Security and supply-chain hardening is complete within its defined implementation and acceptance scope.

Canonical runtime evidence:

```text
docs/livingworld/VALIDATION_0.1.15.md
docs/livingworld/VALIDATION_0.1.16.md
docs/livingworld/VALIDATION_0.1.17.md
```
