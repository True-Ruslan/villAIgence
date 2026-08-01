# VillAIgence 0.1.16+1.21.1 Security Acceptance

**Validation date:** 2026-08-01  
**Minecraft:** 1.21.1  
**Release tag:** `0.1.16+1.21.1`  
**Release commit:** `521568f903078b91dd5817cdc9a551bd2392e663`  
**JAR:** `villaigence-fabric-0.1.16+1.21.1.jar`  
**JAR SHA-256:** `036cbacc657ceb676813f41ee293024690b981e971e7c6037fc5d3ecbe3ee062`

## Verdict

```text
SEC-003 bounded provider responses            PASS
SEC-004 constrained verification probe        PARTIAL / OPEN
SEC-007 voice clamp and aggregate PCM budget  PASS
```

The release remains suitable for normal production use. The only unresolved acceptance item is the acceptance-only verification probe accepting HTTPS loopback input and reaching a TLS attempt instead of rejecting it before connection.

## SEC-003 results

Confirmed limits:

```text
Chat JSON                 8 MiB
STT JSON                  4 MiB
TTS audio                64 MiB
provider error body     256 KiB
body-read total deadline 10 minutes
```

Observed:

- declared-length oversized Chat, STT and TTS responses were rejected at their configured limits;
- chunked/unknown-length responses were stopped on the first byte beyond the limit;
- oversized provider error bodies were bounded at 256 KiB;
- the Chat slow-drip stream was interrupted after `600.026 s`, before the harness completed its 660-second stream;
- Chat, STT and TTS redirect scenarios returned `307` and were not followed;
- the harness reported `redirect_target_hits=0`;
- no server crash, `OutOfMemoryError`, credential disclosure or unbounded provider-body logging occurred.

### TTS fail-soft persistence interpretation

All four TTS-negative scenarios completed STT and Chat before synthesis failed. VillAIgence intentionally publishes visible text and persists the corresponding dialogue before starting TTS, so `memory.json` and `memory2.json` changed.

This is expected fail-soft behavior and is consistent with the previously validated requirement:

```text
TTS failure must not discard visible text or Memory 2.0 DIALOGUE
```

An empty persistence diff is therefore required only when the rejected stage occurs before a legitimate dialogue is produced. For TTS-negative scenarios, acceptance instead requires:

- the visible text response remains available;
- the successful dialogue is persisted once;
- TTS failure does not create a duplicate dialogue or duplicate side effect;
- no mutation is attributable solely to following a redirect or consuming excess response bytes;
- the server remains healthy.

The original generic empty-diff wording was an acceptance-document defect, not a runtime persistence defect.

## SEC-004 results

Confirmed:

- verification success route worked through the exact release JAR probe;
- declared and chunked verification responses above 64 KiB were classified as too large;
- verification redirect returned `307` and the redirect target received zero requests;
- hostnames, LAN/internet addresses, URI user-info and URI fragments were rejected before connection.

Unresolved behavior in `0.1.16+1.21.1`:

```text
https://127.0.0.1:...
→ accepted by probe input validation
→ TLS attempted against the HTTP-only harness
→ IO_ERROR / SSLException
```

The acceptance probe exists only for the local plaintext loopback harness. It must accept only `http` with a literal loopback IP. Production `/mca verify` remains independently constrained to its fixed trusted HTTPS provider origin.

SEC-004 remains Open until a later JAR rejects HTTPS loopback before connection and the focused probe retest passes.

## SEC-007 results

The exact-release-JAR PCM probe completed successfully:

```text
maxBytes                           134217728
clampedLowSeconds                  1
clampedHighSeconds                 120
accepted                           128
rejected                           128
peakBytes                          134217728
finalBytes                         0
recoveryReservationSucceeded       true
passed                             true
```

This confirms:

- voice duration is clamped to exactly `1..120` seconds;
- concurrent reservations never exceed 128 MiB;
- reservations above the budget are rejected;
- all accepted reservations are released;
- a full-budget reservation succeeds after recovery.

After the probe, production Chat, STT, TTS, Simple Voice Chat and Opus were successfully exercised.

## Persistence, restoration and operations

Confirmed after the acceptance run:

- production `config/livingworld.json` was restored byte-for-byte;
- a final clean restart produced an empty diff for all six persistent files when no new interaction occurred;
- the harness was stopped;
- the server returned to `STARTED`;
- Minecraft TCP 25565, Simple Voice Chat UDP 24454 and the monitor were healthy;
- no corruption/recovery path or critical VillAIgence error occurred.

A brief Mojang authentication outage occurred during the window, recovered independently and was unrelated to VillAIgence.

## Finding effect

This evidence is sufficient to close:

```text
SEC-003 bounded provider responses
SEC-007 bounded voice capture and aggregate PCM
```

The following remains open:

```text
SEC-004 constrained account verification
```

Required focused retest after the probe fix:

```text
http://127.0.0.1:<port>/v1/mca/verify/success    → SUCCESS
http://127.0.0.1:<port>/v1/mca/verify/redirect   → HTTP_ERROR / 307, target hits 0
https://127.0.0.1:<port>/...                      → rejected before connection, exit 2
https://[::1]:<port>/...                          → rejected before connection, exit 2
```

No full Chat/STT/TTS/PCM acceptance rerun is required unless the fix changes shared runtime transport behavior.
