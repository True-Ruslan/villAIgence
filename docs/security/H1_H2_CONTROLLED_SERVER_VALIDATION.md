# H1/H2 Controlled Server Validation

## Purpose

This scenario is the final runtime-sensitive validation required before closing:

```text
SEC-001 provider credential binding
SEC-002 normalized endpoint trust
SEC-003 bounded provider responses
SEC-004 constrained account verification
SEC-007 bounded voice capture and aggregate PCM
```

Repository-side implementation H1–H5 is merged. This document covers only real Minecraft 1.21.1 server behavior.

## Candidate boundary

Run this scenario only on a release built from default branch `1.21.1` after merge commit:

```text
6d82b4e4650294a4a42b9ea2113e64d990e08811
```

The expected candidate version is:

```text
0.1.15+1.21.1
```

Before testing, record:

```text
release tag:
release commit:
JAR filename:
JAR SHA-256:
dependency manifest filename:
server start timestamp:
```

Do not promote the candidate to the latest live-validated checkpoint until every required positive test passes and every negative test fails safely.

## Safety and backup preparation

1. Stop the server cleanly.
2. Back up the complete world.
3. Independently copy:

```text
<world>/livingworld/memory.json
<world>/livingworld/memory2.json
<world>/livingworld/semantic-memory.json
<world>/livingworld/relationships.json
<world>/livingworld/voices.json
<world>/livingworld/events.json
config/livingworld.json
```

4. Record SHA-256 for every copied file.
5. Preserve the previous live-validated `0.1.13+1.21.1` JAR for rollback.
6. Install only the candidate VillAIgence JAR; do not mix duplicate versions.
7. Confirm Simple Voice Chat and all required loader dependencies match the candidate environment.

## Baseline configuration

Use the existing working OpenRouter/OpenAI configuration first.

Secure default must remain:

```json
{
  "allowInsecureLoopbackAiEndpoints": false
}
```

Record configured Chat, STT and TTS provider families without copying credentials into the validation document.

Never paste:

- API keys;
- Authorization headers;
- complete prompts or transcripts containing private content;
- environment variables containing credentials.

## Phase A — startup and diagnostics

1. Start the server.
2. Confirm VillAIgence loads once with the expected candidate version.
3. Confirm no configuration migration error.
4. Confirm all persistent stores load successfully.
5. Run the available AI diagnostics/status commands, including the project-standard equivalents of:

```text
/livingworld ai status
/livingworld ai test
```

6. Record stage states, durations, attempt counts and controlled provider errors.
7. Confirm logs do not contain API keys, Authorization headers, URI user information, full prompts, transcripts or hidden reasoning.
8. Confirm ports and operations:

```text
Minecraft TCP 25565
Simple Voice Chat UDP 24454
LinuxGSM/service monitor
```

Expected result: server remains started and diagnostics are usable without secrets.

## Phase B — normal Chat, STT and TTS

Use one known NPC A and one separate NPC B.

1. Send a normal text message to NPC A.
2. Confirm one visible response.
3. Confirm one legacy dialogue append and one Memory 2.0 `DIALOGUE` event according to the current dual-write contract.
4. Repeat through voice input to NPC A.
5. Confirm:

```text
microphone → Opus/PCM → STT → NPC A → Chat → text → optional TTS
```

6. Send a separate text or voice interaction to NPC B.
7. Confirm NPC A/B owner and related-entity isolation.
8. Confirm retry does not duplicate authoritative actions, relationship changes or memory events.
9. Temporarily make TTS unavailable while leaving Chat available.
10. Confirm text output remains visible and only audio fails softly.

Expected result: standard working provider behavior is unchanged by H1/H2.

## Phase C — credential-to-endpoint binding

Perform these tests with disposable test credentials or redacted placeholders. Restart after each configuration change.

### C1 — custom Chat endpoint cannot inherit provider environment key

1. Configure a custom HTTPS Chat hostname not belonging to OpenAI or OpenRouter.
2. Leave only `OPENAI_API_KEY` or `OPENROUTER_API_KEY` available.
3. Leave custom `apiKey` empty.
4. Run diagnostics or Chat.

Expected result:

```text
MISCONFIGURED or controlled unavailable state
no request with unrelated provider credential
no persistent/gameplay mutation
```

### C2 — custom STT/TTS require dedicated keys

1. Configure custom HTTPS STT/TTS endpoints.
2. Leave only the main Chat key available.
3. Leave `sttApiKey` and `ttsApiKey` empty.
4. Run voice input and TTS.

Expected result: custom audio stages fail before sending the unrelated main Chat key.

### C3 — normal OpenAI/OpenRouter family fallback

Restore a normal matching provider endpoint and its matching environment/config key.

Expected result: the stage works normally.

## Phase D — endpoint trust and transport rejection

Restart after each configuration change.

### D1 — remote HTTP rejected

Configure a non-loopback remote or private-LAN HTTP endpoint while:

```json
{
  "allowInsecureLoopbackAiEndpoints": true
}
```

Expected result: remote HTTP remains rejected. The flag must not authorize LAN or internet plaintext endpoints.

### D2 — lookalike hosts rejected as trusted providers

Test controlled invalid endpoints such as:

```text
https://openrouter.ai.example.invalid/...
https://conczin.net.example.invalid/...
https://user:password@example.invalid/...
https://example.invalid/path#fragment
```

Expected result:

- user-info and fragment URIs fail validation;
- lookalike hosts receive no trusted-provider behavior or metadata;
- credentials are not attached before validation;
- no persistent/gameplay mutation occurs.

### D3 — loopback HTTP requires explicit opt-in

1. Set a local mock endpoint such as:

```text
http://127.0.0.1:<port>/v1/chat/completions
```

2. With `allowInsecureLoopbackAiEndpoints=false`, confirm rejection.
3. Set the flag to `true` and restart.
4. Confirm the lexical loopback endpoint can be used.
5. Repeat with a non-loopback hostname and confirm it remains rejected.

## Phase E — redirect boundary

Use a local HTTPS-capable controlled mock or a disposable provider endpoint that returns `302`/`307` to another origin.

Test Chat, STT and TTS separately.

Expected result:

```text
3xx treated as provider failure
redirect not followed
Authorization not forwarded
trusted metadata not forwarded
text/persistence side-effect boundary preserved
```

## Phase F — bounded response tests

Use a local controlled mock provider. Keep it on loopback and explicitly enable loopback development mode only for this phase.

Limits:

```text
Chat JSON:                 8 MiB
STT JSON:                  4 MiB
TTS audio:                64 MiB
provider error body:     256 KiB
account verification:     64 KiB
provider body-read time:  10 minutes
```

### F1 — oversized declared length

Return a `Content-Length` greater than the applicable limit without streaming the full body.

Expected result: rejection occurs before full body read.

### F2 — oversized chunked/unknown length

Stream one byte beyond the applicable limit without `Content-Length`.

Expected result: rejection occurs on the first excess byte.

### F3 — oversized provider error body

Return a non-2xx status with an error body above 256 KiB.

Expected result: bounded sanitized diagnostics; provider payload is not copied to logs or exceptions.

### F4 — slow-drip body

Send a response slowly enough to remain below socket idle timeout while continuing beyond the ten-minute total body-read deadline.

Expected result: the operation terminates at the total deadline instead of continuing indefinitely.

For practical validation, this test may run in an isolated maintenance window. Record start/end timestamps and do not reduce the production deadline solely to make the test shorter.

## Phase G — `/mca verify` boundary

The legacy generic arbitrary-URL helper was removed. The command now uses `AccountVerificationClient`.

1. With a normal non-Conczin provider endpoint, run the available `/mca verify <email>` command using a disposable test address.
2. Confirm it does not construct or fetch an arbitrary URL from the configured endpoint.
3. Confirm verification is allowed only for the exact trusted Conczin HTTPS provider family.
4. Test a lookalike Conczin hostname and a redirecting endpoint.

Expected result:

```text
fixed internal verification path only
trusted exact HTTPS family only
64 KiB response bound
no redirects
controlled success/failed/error state
```

Do not include the disposable email address in public evidence.

## Phase H — voice capture and PCM budget

Runtime boundaries:

```text
voiceMaxSeconds effective range: 1..120 seconds
aggregate active microphone PCM: 128 MiB
```

1. Set `voiceMaxSeconds` below 1, restart and confirm effective clamp to 1 second.
2. Set it above 120, restart and confirm effective clamp to 120 seconds.
3. Restore the operational value, normally 20 seconds.
4. Exercise overlapping voice captures from multiple clients/NPC interactions.
5. Increase concurrency until the aggregate budget is approached in a controlled environment.
6. Confirm excess capture is dropped cleanly rather than exceeding the budget.
7. Confirm later captures work after previous sessions finish or fail, proving reservation release.
8. Force a decoder/capture failure and confirm no permanent budget leak.
9. Confirm server memory remains stable and no OutOfMemory error occurs.

## Phase I — persistence and exactly-once validation

1. Record hashes of all world-local VillAIgence files.
2. Run one text dialogue and one voice dialogue.
3. Confirm expected files change exactly once.
4. Trigger a provider retry/failure path.
5. Confirm no duplicate authoritative ACTION, RELATIONSHIP_CHANGE or DIALOGUE event.
6. Run negative endpoint/oversized-response tests that should not mutate state.
7. Confirm world files remain byte-identical across those rejected operations.
8. Stop the server cleanly.
9. Hash all files.
10. Restart.
11. Hash again without new interactions.

Expected result: pre/post-restart hashes are byte-identical where no mutation is expected, and all legitimate changes survive.

## Required evidence bundle

Create a dated validation document containing:

```text
candidate version/tag/commit
JAR filename and SHA-256
dependency manifest
server/loader/Java versions
redacted configuration families
positive Chat/STT/TTS results
negative endpoint results
redirect results
bounded declared/chunked/error/slow-drip results
verification boundary result
voice clamp/concurrency/PCM result
exactly-once observations
pre/post restart hashes
relevant redacted log excerpts
rollback result if used
```

Never publish credentials, headers, private transcripts or disposable verification addresses.

## Pass criteria

The candidate passes only when:

- every required positive path works;
- every unsafe endpoint or response fails closed/soft as designed;
- no credential crosses an endpoint-family boundary;
- no redirect receives credentials or trusted metadata;
- no rejected request mutates gameplay or persistence;
- no retry duplicates effects;
- voice resources remain bounded and recover after failures;
- logs remain sanitized;
- persistence survives restart;
- normal Chat/Voice operations remain healthy.

After a complete pass:

1. add a dated `VALIDATION_0.1.15.md` record;
2. close SEC-001, SEC-002, SEC-003, SEC-004 and SEC-007 in a final audit follow-up;
3. update `docs/PROJECT_STATE.md` and `docs/CHANGELOG.md`;
4. promote `0.1.15+1.21.1` to the latest live-validated checkpoint.
