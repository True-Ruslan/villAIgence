# Local Security Acceptance Harness

## Purpose

This procedure exercises the remaining runtime-sensitive Step 1 boundaries without sending hostile responses to OpenRouter, OpenAI or any other production provider:

```text
SEC-003 bounded provider responses
SEC-004 account verification and redirect handling
SEC-007 voice duration and aggregate PCM budget
```

The harness is intentionally split into:

```text
Python loopback provider server
+ real Minecraft server using temporary loopback endpoints
+ exact-release-JAR verification probe
+ exact-release-JAR voice/PCM probe
```

The tooling does not run during Minecraft startup. Every component requires an explicit operator command.

## Safety rules

1. Run only during a maintenance window.
2. Stop the Minecraft server before changing `config/livingworld.json`.
3. Back up the complete world and configuration first.
4. Keep the previous live-validated VillAIgence JAR available for rollback.
5. Bind the harness only to literal `127.0.0.1` or `::1`.
6. Never expose the harness port through Docker, a reverse proxy, firewall forwarding or a public listener.
7. Never use production OpenRouter/OpenAI credentials in the temporary loopback configuration.
8. Do not run oversized or slow-drip scenarios against any external provider.
9. Restore the production configuration byte-for-byte after the test.
10. Keep SEC-003, SEC-004 and SEC-007 open until the full controlled run and dated evidence follow-up are complete.

The harness itself rejects hostnames, wildcard binds, LAN addresses and internet addresses.

## Required candidate

Use a release built from a commit containing the acceptance harness PR.

Record:

```text
release tag:
release commit:
JAR filename:
JAR SHA-256:
Java version:
Minecraft version:
Fabric Loader version:
Simple Voice Chat version:
```

Confirm that the release JAR contains:

```bash
jar tf villaigence-fabric-<version>.jar | grep -E \
'AccountVerificationAcceptanceProbe|AccountVerificationTransport|VoicePcmBudgetAcceptanceProbe'
```

All three classes are mandatory package smoke-check entries.

## Backup and baseline hashes

Stop the server cleanly and preserve:

```text
config/livingworld.json
<world>/livingworld/memory.json
<world>/livingworld/memory2.json
<world>/livingworld/semantic-memory.json
<world>/livingworld/relationships.json
<world>/livingworld/voices.json
<world>/livingworld/events.json
```

Example:

```bash
set -euo pipefail
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
backup="$HOME/villaigence-security-acceptance-$stamp"
mkdir -p "$backup"
cp -a config/livingworld.json "$backup/livingworld.json.production"
cp -a <world>/livingworld "$backup/livingworld"
sha256sum config/livingworld.json > "$backup/config-before.sha256"
find <world>/livingworld -maxdepth 1 -type f -print0 \
  | sort -z \
  | xargs -0 sha256sum \
  > "$backup/world-before.sha256"
printf '%s\n' "$backup"
```

Replace `<world>` with the real world directory.

## Start the provider harness

From a checkout matching the candidate source:

```bash
python3 scripts/security/provider_acceptance_harness.py serve \
  --bind 127.0.0.1 \
  --port 18080 \
  --evidence-dir build/security-acceptance/provider \
  --slow-duration-seconds 660 \
  --slow-interval-seconds 5
```

Expected startup marker:

```text
VILLAIGENCE_PROVIDER_HARNESS_READY
```

Generated files:

```text
build/security-acceptance/provider/manifest.json
build/security-acceptance/provider/requests.jsonl
```

`requests.jsonl` stores only:

- HTTP method;
- path without query values;
- query key names;
- request body byte count;
- whether Authorization and X-Title headers were present;
- stage/case/status.

It never stores header values, API keys, email values, prompts, transcripts or request bodies.

## Harness routes

Stages:

```text
Chat:         /v1/chat/completions/<case>
STT:          /v1/audio/transcriptions/<case>
TTS:          /v1/audio/speech/<case>
Verification: /v1/mca/verify/<case>
```

Cases:

```text
ok
success                verification only
failed                 verification only
declared-oversize
chunked-oversize
error-oversize
redirect
slow-drip
```

Harness diagnostics:

```text
/__harness__/status
/__harness__/redirect-target
```

## Temporary Minecraft configuration

Create a separate acceptance copy of the production configuration. Change only the fields required for the current case.

Base loopback settings:

```json
{
  "provider": "openai",
  "allowInsecureLoopbackAiEndpoints": true,
  "apiKey": "acceptance-chat-placeholder",
  "sttApiKey": "acceptance-stt-placeholder",
  "ttsApiKey": "acceptance-tts-placeholder",
  "connectTimeoutSeconds": 10,
  "readTimeoutSeconds": 60
}
```

The placeholder values must not be real credentials. Custom loopback stages require dedicated explicit keys so that the stage remains configured while production environment keys stay endpoint-bound.

For each case, configure only the endpoint being tested. Examples:

```json
{
  "endpoint": "http://127.0.0.1:18080/v1/chat/completions/declared-oversize"
}
```

```json
{
  "sttEndpoint": "http://127.0.0.1:18080/v1/audio/transcriptions/chunked-oversize"
}
```

```json
{
  "ttsEndpoint": "http://127.0.0.1:18080/v1/audio/speech/error-oversize",
  "ttsResponseFormat": "pcm"
}
```

Restart after every configuration change. Preserve the production copy unchanged.

## SEC-003 response-bound matrix

Production limits:

```text
Chat JSON                 8 MiB
STT JSON                  4 MiB
TTS audio                64 MiB
provider error body     256 KiB
account verification     64 KiB
body-read total deadline 10 minutes
```

### Declared length

Run for Chat, STT and TTS:

```text
<stage>/declared-oversize
```

Expected:

- operation fails before the advertised body is read;
- controlled size-limit diagnostic;
- no server crash or OutOfMemory;
- no provider body copied to logs;
- rejected operation does not mutate persistent files.

### Chunked/unknown length

Run for Chat, STT and TTS:

```text
<stage>/chunked-oversize
```

Expected:

- the client aborts on the first byte above the stage limit;
- the harness may observe a broken connection after the client aborts;
- no unbounded allocation or persistent mutation.

The TTS route streams 64 MiB plus one byte. Run it only on loopback during maintenance.

### Oversized provider error

Run for at least Chat and one audio stage:

```text
<stage>/error-oversize
```

Expected:

- HTTP failure remains controlled;
- response processing stops at 256 KiB plus the first excess byte;
- logs contain neither the complete body nor credentials.

### Redirect

Run for Chat, STT and TTS:

```text
<stage>/redirect
```

Expected:

- `307` is treated as provider failure;
- the target is not requested;
- credentials and provider metadata are not forwarded;
- persistent files remain unchanged.

### Slow-drip total deadline

Use the harness defaults shown above: one chunk every five seconds for eleven minutes. Keep `readTimeoutSeconds` above the chunk interval so the socket idle timeout does not end the test first.

Run at least Chat:

```text
/v1/chat/completions/slow-drip
```

Expected:

- the operation terminates at the VillAIgence ten-minute total body deadline;
- it does not continue until the harness finishes eleven minutes;
- the server stays responsive;
- no persistent mutation occurs.

Record exact UTC start and end timestamps.

## SEC-004 verification probe

The exact release JAR contains a JDK-only probe using the same bounded, no-redirect HTTP transport as production account verification.

Set:

```bash
jar=/absolute/path/to/villaigence-fabric-<version>.jar
probe=net.conczin.mca.livingworld.ai.AccountVerificationAcceptanceProbe
```

Normal transport:

```bash
java -cp "$jar" "$probe" \
  http://127.0.0.1:18080/v1/mca/verify/success
```

Expected:

```json
{"marker":"VILLAIGENCE_VERIFICATION_PROBE","outcome":"SUCCESS","status":200}
```

Redirect:

```bash
java -cp "$jar" "$probe" \
  http://127.0.0.1:18080/v1/mca/verify/redirect
```

Expected:

```text
outcome = HTTP_ERROR
status = 307
redirect_target_hits = 0
```

Declared oversize:

```bash
java -cp "$jar" "$probe" \
  http://127.0.0.1:18080/v1/mca/verify/declared-oversize
```

Expected:

```text
outcome = TOO_LARGE
errorType = ResponseTooLargeException
```

Chunked oversize:

```bash
java -cp "$jar" "$probe" \
  http://127.0.0.1:18080/v1/mca/verify/chunked-oversize
```

Expected: the same `TOO_LARGE` classification.

The probe rejects before connection:

```text
localhost hostnames
LAN/internet addresses
URI user-info
URI fragments
non-HTTP schemes
```

Production `/mca verify` remains stricter than the probe: it still constructs a fixed `/v1/mca/verify` path only from the exact trusted Conczin HTTPS provider family. The probe's arbitrary path is limited to literal loopback and is not reachable through Minecraft commands.

## SEC-007 voice and PCM probe

Run directly from the same release JAR:

```bash
java -cp "$jar" \
  net.conczin.mca.livingworld.voice.VoicePcmBudgetAcceptanceProbe \
  256 1048576
```

This starts 256 synchronized reservation workers at 1 MiB each against the exact production maximum of 128 MiB. It allocates reservation counters, not 256 MiB of PCM data.

Expected core fields:

```json
{
  "marker": "VILLAIGENCE_PCM_PROBE_PASS",
  "maxBytes": 134217728,
  "clampedLowSeconds": 1,
  "clampedHighSeconds": 120,
  "accepted": 128,
  "rejected": 128,
  "peakBytes": 134217728,
  "finalBytes": 0,
  "recoveryReservationSucceeded": true,
  "passed": true
}
```

This proves:

- low duration values clamp to one second;
- extreme duration values clamp to 120 seconds;
- concurrent reservations never exceed 128 MiB;
- excess reservations fail without mutating the budget;
- every accepted reservation is released;
- a full-budget reservation succeeds after recovery.

A final real microphone smoke still remains required after the probe: one normal voice interaction must succeed, proving the operational capture path is healthy after the maintenance test.

## Persistence checks

Before every rejected provider case, hash the six persistent files. Hash them again after the operation and before introducing a legitimate interaction.

Example:

```bash
find <world>/livingworld -maxdepth 1 -type f -print0 \
  | sort -z \
  | xargs -0 sha256sum \
  > "$backup/case-before.sha256"

# Run one rejected case.

find <world>/livingworld -maxdepth 1 -type f -print0 \
  | sort -z \
  | xargs -0 sha256sum \
  > "$backup/case-after.sha256"

diff -u "$backup/case-before.sha256" "$backup/case-after.sha256"
```

Expected: empty diff.

## Evidence summary

After all routes:

```bash
python3 scripts/security/provider_acceptance_harness.py summarize \
  --evidence-dir build/security-acceptance/provider \
  | tee build/security-acceptance/provider/summary.json
```

Confirm:

```text
redirect_response_count > 0
redirect_target_hits = 0
authorization_present_count may be > 0
no credential/header/query values exist in requests.jsonl
```

Search all evidence and server logs for accidental secrets before publication. Do not paste a real key into a search command that is preserved in shell history; use known non-secret prefixes or review locally.

## Restore production

1. Stop the Minecraft server.
2. Replace `config/livingworld.json` with the preserved production copy.
3. Compare it byte-for-byte:

```bash
cmp -s "$backup/livingworld.json.production" config/livingworld.json
```

4. Start the server.
5. Confirm `villaigence ai status` reports production Chat/STT/TTS as configured.
6. Run one text dialogue and one voice dialogue.
7. Confirm ports 25565/TCP and 24454/UDP plus the service monitor.
8. Stop and restart once more, then compare persistent hashes without new interactions.
9. Stop the Python harness.

## Pass criteria

The controlled run passes only if:

- every declared, chunked and error response is bounded;
- slow-drip ends at the ten-minute total deadline;
- Chat/STT/TTS redirects are not followed;
- verification redirect target receives zero requests;
- verification responses are bounded at 64 KiB;
- probe input cannot leave literal loopback;
- voice duration clamps are exactly 1 and 120 seconds;
- concurrent PCM use never exceeds 128 MiB;
- excess reservations fail and later full recovery succeeds;
- rejected operations do not mutate world files;
- logs and evidence contain no secrets or private payloads;
- production configuration is restored byte-for-byte;
- normal Text/STT/TTS/Voice behavior works afterward;
- restart persistence remains stable.

After a complete pass, create a dated validation record, close SEC-003/SEC-004/SEC-007, update canonical state and mark Step 1 fully complete.
