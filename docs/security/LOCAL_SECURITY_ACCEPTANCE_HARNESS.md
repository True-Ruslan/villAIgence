# Local Security Acceptance Harness

## Purpose

This procedure exercises the runtime-sensitive Step 1 boundaries without sending hostile responses to OpenRouter, OpenAI or any other production provider:

```text
SEC-003 bounded provider responses
SEC-004 constrained account verification and redirect handling
SEC-007 voice duration and aggregate PCM budget
```

The harness is intentionally split into:

```text
Python loopback provider server
+ real Minecraft server using temporary loopback endpoints
+ exact-release-JAR verification probe
+ exact-release-JAR voice/PCM probe
```

Every component requires an explicit operator command. Nothing in this procedure runs during Minecraft startup.

## Normative persistence rule

Persistence expectations depend on the stage that fails.

### Failure before a dialogue exists

For Chat rejection and STT rejection before successful Chat:

```text
no legitimate response exists
→ no DIALOGUE should be persisted
→ persistent-file hashes should remain unchanged
```

### TTS failure after successful Chat

TTS is optional and occurs after visible text and the authoritative dialogue are produced. Therefore:

```text
successful STT + Chat
→ visible text published
→ DIALOGUE persisted
→ TTS attempted
→ TTS failure must not roll back the text or DIALOGUE
```

For TTS-negative cases, an empty diff for `memory.json` and `memory2.json` is not required and would contradict fail-soft behavior. Instead verify:

- the visible text remains available;
- the successful dialogue is persisted exactly once;
- no duplicate DIALOGUE, source event or side effect is created;
- redirect targets are not contacted;
- no mutation is caused solely by consuming excess TTS bytes;
- the server remains healthy.

Files unrelated to a legitimate upstream Chat result must remain unchanged. Use a controlled prompt and inspect the resulting event identifiers when exact attribution matters.

This rule supersedes the earlier generic statement that every rejected TTS operation must produce an empty six-file diff.

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
10. Keep a finding open until its required release-JAR acceptance evidence is recorded.

The harness rejects hostnames, wildcard binds, LAN addresses and internet addresses.

## Required candidate metadata

Use a release built from a commit containing the acceptance harness and any follow-up fixes required by the current test cycle.

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

Generated evidence:

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
- stage, case and response status.

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

Diagnostics:

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

The placeholder values must not be real credentials. Custom loopback stages require dedicated explicit placeholder keys so that the stage remains configured while production environment keys stay endpoint-bound.

Examples:

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

Expected transport behavior:

- operation fails before the advertised body is consumed;
- a controlled size-limit diagnostic is produced;
- no server crash or `OutOfMemoryError` occurs;
- no provider body is copied to logs.

Persistence expectation:

- Chat/STT failure before a dialogue exists: empty persistent diff;
- TTS failure after successful Chat: preserve exactly one legitimate dialogue according to the normative persistence rule.

### Chunked or unknown length

Run for Chat, STT and TTS:

```text
<stage>/chunked-oversize
```

Expected:

- the client aborts on the first byte above the stage limit;
- the harness may observe a broken connection after the client aborts;
- no unbounded allocation occurs;
- persistence follows the stage-specific rule above.

The TTS route streams 64 MiB plus one byte. Run it only on loopback during maintenance.

### Oversized provider error

Run for at least Chat and one audio stage:

```text
<stage>/error-oversize
```

Expected:

- HTTP failure remains controlled;
- response processing stops at 256 KiB plus the first excess byte;
- logs contain neither the complete body nor credentials;
- persistence follows the stage-specific rule.

### Redirect

Run for Chat, STT and TTS:

```text
<stage>/redirect
```

Expected:

- `307` is treated as provider failure;
- the redirect target is not requested;
- credentials and provider metadata are not forwarded;
- Chat/STT rejection before a dialogue exists leaves persistence unchanged;
- a TTS redirect after successful Chat preserves the legitimate dialogue once and does not create additional mutation.

### Slow-drip total deadline

Use one chunk every five seconds for eleven minutes. Keep `readTimeoutSeconds` above the chunk interval so the socket idle timeout does not end the test first.

Run at least Chat:

```text
/v1/chat/completions/slow-drip
```

Expected:

- the operation terminates at the VillAIgence ten-minute total body deadline;
- it does not continue until the harness finishes eleven minutes;
- the server stays responsive;
- no dialogue is persisted because Chat never completed.

Record exact UTC start and end timestamps.

## SEC-004 verification probe

The exact release JAR contains a JDK-only probe using the same bounded, no-redirect HTTP transport as production account verification.

The acceptance harness is plaintext loopback. Therefore the probe must accept only:

```text
scheme: http
host: literal 127.0.0.1 or literal IPv6 loopback
no user-info
no fragment
```

It must reject HTTPS loopback before any TLS connection attempt. Production `/mca verify` remains separate and stricter: it constructs a fixed path only for the exact trusted HTTPS provider family.

Set:

```bash
jar=/absolute/path/to/villaigence-fabric-<version>.jar
probe=net.conczin.mca.livingworld.ai.AccountVerificationAcceptanceProbe
```

### Normal transport

```bash
java -cp "$jar" "$probe" \
  http://127.0.0.1:18080/v1/mca/verify/success
```

Expected:

```json
{"marker":"VILLAIGENCE_VERIFICATION_PROBE","outcome":"SUCCESS","status":200}
```

### Redirect

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

### Declared and chunked oversize

```bash
java -cp "$jar" "$probe" \
  http://127.0.0.1:18080/v1/mca/verify/declared-oversize

java -cp "$jar" "$probe" \
  http://127.0.0.1:18080/v1/mca/verify/chunked-oversize
```

Expected:

```text
outcome = TOO_LARGE
errorType = ResponseTooLargeException
```

### Rejected before connection

The following must exit with code `2` and print the controlled rejection message:

```bash
java -cp "$jar" "$probe" \
  https://127.0.0.1:18080/v1/mca/verify/success

java -cp "$jar" "$probe" \
  https://[::1]:18080/v1/mca/verify/success

java -cp "$jar" "$probe" \
  http://localhost:18080/v1/mca/verify/success

java -cp "$jar" "$probe" \
  http://192.168.1.10:18080/v1/mca/verify/success

java -cp "$jar" "$probe" \
  'http://user:password@127.0.0.1:18080/v1/mca/verify/success'

java -cp "$jar" "$probe" \
  'http://127.0.0.1:18080/v1/mca/verify/success#fragment'
```

None of these rejected inputs may appear in harness request evidence.

## SEC-007 voice and PCM probe

Run directly from the release JAR:

```bash
java -cp "$jar" \
  net.conczin.mca.livingworld.voice.VoicePcmBudgetAcceptanceProbe \
  256 1048576
```

This starts 256 synchronized reservation workers at 1 MiB each against the production maximum of 128 MiB. It allocates reservation counters, not 256 MiB of PCM data.

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

After the probe, run one normal real microphone interaction through:

```text
microphone → Voice Chat / Opus → STT → Chat → visible text → TTS
```

## Persistence evidence

### Chat and STT negative cases

Before each case:

```bash
find <world>/livingworld -maxdepth 1 -type f -print0 \
  | sort -z \
  | xargs -0 sha256sum \
  > "$backup/case-before.sha256"
```

After the rejected operation:

```bash
find <world>/livingworld -maxdepth 1 -type f -print0 \
  | sort -z \
  | xargs -0 sha256sum \
  > "$backup/case-after.sha256"

diff -u "$backup/case-before.sha256" "$backup/case-after.sha256"
```

Expected: empty diff when no legitimate dialogue was produced.

### TTS negative cases

Do not use an empty-diff requirement for `memory.json` and `memory2.json` after successful Chat. Instead record:

- hashes before and after;
- the visible response text;
- the new DIALOGUE/event identifier;
- the number of matching new entries;
- whether any duplicate source event or unrelated side effect appeared.

Expected: exactly one legitimate persisted dialogue and no duplicate caused by TTS failure.

### Final restart

After restoring production configuration and completing all intentional interactions, take a new baseline. Restart without new interaction and compare all six files. Expected: empty diff.

## Evidence summary

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

Search evidence and server logs for accidental secrets before publication. Do not paste a real key into a search command that is retained in shell history.

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
7. Confirm TCP 25565, UDP 24454 and the service monitor.
8. Take a new six-file baseline.
9. Stop and restart once without new interactions.
10. Confirm an empty six-file diff.
11. Stop the Python harness.

## Pass criteria

The controlled run passes only if:

- every declared, chunked and error response is bounded;
- slow-drip ends at the ten-minute total deadline;
- Chat/STT/TTS redirects are not followed;
- verification redirect target receives zero requests;
- verification responses are bounded at 64 KiB;
- verification probe accepts only HTTP literal loopback and rejects HTTPS before connection;
- voice duration clamps are exactly 1 and 120 seconds;
- concurrent PCM use never exceeds 128 MiB;
- excess reservations fail and later full recovery succeeds;
- Chat/STT rejection before dialogue creation leaves persistence unchanged;
- TTS failure preserves exactly one legitimate upstream dialogue without duplication;
- logs and evidence contain no secrets or private payloads;
- production configuration is restored byte-for-byte;
- normal Text/STT/TTS/Voice behavior works afterward;
- restart persistence remains stable.

After a complete pass, create a dated validation record, update the finding follow-up, reconcile canonical project state and mark Step 1 fully complete.
