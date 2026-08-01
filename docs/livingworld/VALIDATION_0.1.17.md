# VillAIgence 0.1.17+1.21.1 SEC-004 Artifact Validation

**Validation date:** 2026-08-01  
**Minecraft target:** 1.21.1  
**Release tag:** `0.1.17+1.21.1`  
**Release commit:** `88a20d86e8b08e4b5eaf60da943a63e750f2b545`  
**JAR:** `villaigence-fabric-0.1.17+1.21.1.jar`  
**JAR SHA-256:** `b33af40f7a2696dc679c49e0fc544f6b5df99e0aa600ea5c767bc5a9747da1ab`  
**Validation marker:** `V0117_SEC004_ARTIFACT_AND_EVIDENCE_PASS`

## Scope

This was a focused autonomous validation of the exact official `0.1.17+1.21.1` release JAR. It closes the only acceptance item left open after the full `0.1.16+1.21.1` controlled server run:

```text
SEC-004 constrained account verification
```

The test exercised the explicit JDK-only `AccountVerificationAcceptanceProbe` against the literal-loopback HTTP harness. It did not install the candidate into Minecraft and did not modify the server, `mods`, world data or VillAIgence configuration.

A full Chat/STT/TTS/slow-drip/PCM rerun was intentionally not repeated because PR #70 changed only acceptance-probe URI validation and documentation. The production verification origin policy, shared bounded transport, provider clients, voice path and persistence code were unchanged.

## Artifact identity

The tested file was downloaded from the official GitHub release:

```text
0.1.17+1.21.1
```

Recorded identity:

```text
commit    88a20d86e8b08e4b5eaf60da943a63e750f2b545
SHA-256   b33af40f7a2696dc679c49e0fc544f6b5df99e0aa600ea5c767bc5a9747da1ab
```

The SHA-256 above identifies the exact artifact whose probe behavior was accepted.

## Verification transport results

### HTTP success

```text
route       /v1/mca/verify/success
outcome     SUCCESS
status      200
```

The exact release-JAR probe connected to the local HTTP harness and successfully parsed the normal verification response.

### HTTP redirect

```text
route                  /v1/mca/verify/redirect
outcome                HTTP_ERROR
status                 307
redirect followed      no
redirect_target_hits   0
```

The probe preserved production-equivalent no-redirect behavior. The redirect target received no request.

### Declared oversize

```text
route       /v1/mca/verify/declared-oversize
outcome     TOO_LARGE
errorType   ResponseTooLargeException
```

### Chunked oversize

```text
route       /v1/mca/verify/chunked-oversize
outcome     TOO_LARGE
errorType   ResponseTooLargeException
```

Both size-bound paths retained the exact 64 KiB verification response limit and controlled classification.

## HTTPS loopback rejection

The `0.1.16` acceptance-only probe accepted HTTPS loopback input and reached a TLS attempt against the HTTP-only harness, producing `IO_ERROR / SSLException`.

The `0.1.17` release corrected that discrepancy.

Tested HTTPS loopback input was rejected during argument validation:

```text
connection attempted   no
exit code              2
SSLException           none
IO_ERROR               none
```

This proves the acceptance probe is now restricted to the transport the local harness actually provides:

```text
scheme   http only
host     literal loopback IP only
```

Production `/mca verify` remains separate and stricter: it continues to construct its fixed trusted HTTPS provider endpoint and does not accept an operator-supplied arbitrary URI.

## Harness evidence

The sanitized evidence summary contained:

```text
HTTP requests          4
redirect_target_hits   0
```

The four HTTP requests corresponded to:

```text
success
redirect
declared-oversize
chunked-oversize
```

The rejected HTTPS input did not reach the harness and therefore did not appear as an HTTP request.

After validation:

```text
harness stopped        yes
TCP port 18080 free    yes
```

No production credential, prompt, transcript, header value or private payload was required or retained by the harness evidence.

## Local source-build note

A separate local source build stopped before compilation because Gradle dependency verification lacked checksum records for transitive Fabric dependencies resolved in that environment.

This did not cause an unverified build to proceed. Dependency verification failed closed, and the local output was not used for acceptance.

The focused test therefore followed the documented alternate path and used the official CI/release JAR with an explicitly recorded SHA-256. This is sufficient for SEC-004 because the finding concerns the behavior of the exact distributed artifact.

The local checksum-resolution gap remains a non-blocking build-maintenance follow-up. Any metadata refresh must use the controlled dependency update procedure; verification must not be disabled or bypassed.

## Server impact

The production Minecraft server was intentionally untouched:

```text
installed server release   0.1.16+1.21.1
mods changed               no
server restart             no
world data changed         no
configuration changed      no
```

The earlier `0.1.16` server acceptance remains authoritative for Chat/STT/TTS, response limits, redirects, slow-drip deadline, voice clamp, PCM budget, production restoration and restart durability.

## Verdict

```text
SEC-004 constrained account verification   PASS / Closed
```

Together with the accepted `0.1.16` evidence:

```text
SEC-003 bounded provider responses                  Closed
SEC-004 constrained account verification            Closed
SEC-007 bounded voice capture and aggregate PCM     Closed
```

All findings from the Step 1 security and supply-chain hardening audit now have implementation, automated validation and the applicable runtime or exact-release-artifact acceptance evidence.
