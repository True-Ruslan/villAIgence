# VillAIgence 0.1.15+1.21.1 Live Validation

**Validation date:** 2026-07-31  
**Minecraft:** 1.21.1  
**Release tag:** `0.1.15+1.21.1`  
**Release commit:** `26070c37b806897e37cc3dabe2e4b27af458ac20`  
**JAR:** `villaigence-fabric-0.1.15+1.21.1.jar`  
**JAR SHA-256:** `af142be94885541bb4840d0effff73627afe3f0e245dec8307ed665701cc94fb`

## Validation scope

This run validates the normal production Chat/STT/TTS path, NPC isolation, memory persistence, TTS fail-soft behavior, secure endpoint rejection, log redaction and restart durability on a real Minecraft server.

The release tag and default branch resolved to the same commit at validation time.

## Environment and operations

Confirmed operational:

```text
server state                         STARTED
Minecraft TCP 25565                  healthy
Simple Voice Chat UDP 24454          healthy
service/LinuxGSM monitor             healthy
Chat endpoint                        CONFIGURED
STT endpoint                         CONFIGURED
TTS endpoint                         CONFIGURED
provider family                      OpenRouter
```

The release does not expose a `villaigence ai test` command. Runtime diagnostics used the available:

```text
villaigence ai status
```

This command confirmed the final production Chat/STT/TTS configuration after all negative tests were complete.

## Positive-path results

### Text and memory

NPCs used:

```text
NPC A: Pio
NPC B: Justino
```

Results:

- text dialogue worked;
- Pio retained the player name `Руслан`;
- Pio retained the player's favorite color;
- recalled information remained associated with Pio;
- Pio and Justino remained isolated;
- no cross-NPC memory mixing was observed.

### Voice pipeline

The complete production path worked:

```text
microphone
→ Simple Voice Chat / Opus
→ PCM
→ STT
→ selected NPC
→ Chat
→ visible text
→ TTS
```

Text, STT and TTS all completed successfully in the restored production configuration.

### TTS fail-soft behavior

TTS was made unavailable while Chat remained usable.

Observed result:

```text
visible text response                 preserved
Memory 2.0 DIALOGUE                  preserved
TTS result                            controlled io_error
server process                        remained healthy
```

The first voice request during this phase did not produce text because Chat received `HTTP 429` from OpenRouter. A second request succeeded. This was treated as an external provider rate-limit event rather than a TTS or persistence defect.

## Persistence and restart

The following six world-local stores were checked:

```text
memory.json
memory2.json
semantic-memory.json
relationships.json
voices.json
events.json
```

All six files had identical SHA-256 hashes before and after a clean restart when no new interactions occurred.

Confirmed:

- legitimate dialogue and memory survived restart;
- Pio's remembered name and favorite color survived;
- no unexpected rewrite occurred during restart;
- no corruption or recovery path was triggered.

## Endpoint-policy negative tests

Each test was performed with a controlled configuration change followed by restart. The production configuration was restored byte-for-byte afterward.

### Remote/LAN HTTP

A non-loopback LAN HTTP endpoint was rejected even with insecure loopback development mode enabled.

Result:

```text
configuration state                  MISCONFIGURED
provider request                     not sent
credential transmission              none
persistent mutation                  none
```

### OpenRouter lookalike

A hostname shaped like an OpenRouter lookalike was rejected and did not inherit OpenRouter trust or credentials.

Result:

```text
configuration state                  MISCONFIGURED
OpenRouter credential transmission   none
persistent mutation                  none
```

### URI user-info

An endpoint containing URI user-info was rejected as `MISCONFIGURED` before provider use.

### URI fragment

An endpoint containing a fragment was rejected as `MISCONFIGURED` before provider use.

### Production restoration

After all endpoint tests:

- `config/livingworld.json` was restored byte-for-byte;
- Chat/STT/TTS returned to `CONFIGURED`;
- the working OpenRouter endpoints were restored;
- normal text and voice operation resumed.

## Log and failure review

Confirmed absent:

```text
API key leakage
Authorization header leakage
credential-bearing URI output
corruption/recovery errors
critical VillAIgence server errors
OutOfMemory failure
```

The observed OpenRouter `HTTP 429` was controlled, transient and followed by a successful request.

## Pass matrix

```text
release artifact identity                    PASS
server startup and operations                PASS
text dialogue                                PASS
STT                                          PASS
TTS                                          PASS
Pio/Justino isolation                        PASS
Pio memory recall                            PASS
TTS fail-soft text preservation              PASS
DIALOGUE preservation on TTS failure         PASS
provider 429 controlled handling             PASS
six-file restart hash stability              PASS
LAN HTTP rejection                           PASS
OpenRouter lookalike rejection               PASS
URI user-info rejection                      PASS
URI fragment rejection                       PASS
no rejected-operation memory mutation        PASS
production config byte restoration           PASS
log credential redaction                     PASS
```

## Security finding effect

This live evidence completes the required runtime confirmation for:

```text
SEC-001 provider endpoint credential boundary
SEC-002 normalized endpoint trust boundary
```

The following remain open because their dedicated hostile mock-provider or concurrency acceptance scenarios were not executed on the production provider:

```text
SEC-003 oversized/chunked/error/slow-drip response behavior
SEC-004 constrained /mca verify and redirect acceptance
SEC-007 voice clamp and aggregate PCM exhaustion/recovery
```

These boundaries already have automated regression coverage but require a separate controlled mock-provider/PCM run before final closure under the project's evidence rules.

## Conclusion

`0.1.15+1.21.1` is the latest live-validated VillAIgence checkpoint for normal production Chat/STT/TTS, NPC isolation, memory persistence, endpoint rejection and restart durability.

No release-blocking regression was found in the executed scope. The remaining mock-provider and high-concurrency acceptance work is isolated from normal OpenRouter operation and must not be tested against the production provider.
