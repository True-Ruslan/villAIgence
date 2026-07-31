# H2 Bounded Network and Voice Resource Hardening

**Date:** 2026-07-31  
**Branch:** `agent/h2-bounded-network-and-ssrf`  
**Pull request:** #60  
**Base:** H1 merge commit `787f1a781b5970d4bafb851bfb3c7cba7c21fc0a`

## Scope

This record reconciles the original security audit with the implementation evidence produced during Step 1 H2.

H2 addresses:

- SEC-003 — unbounded or indefinitely streamed Chat, STT and TTS provider responses;
- SEC-004 — the legacy generic URL verification helper;
- SEC-007 — unbounded microphone capture duration and aggregate PCM buffering.

## Audit correction: active verification call site

The original audit stated that no active production call site for `OpenAIChatAI.verify(String encodedURL)` had been confirmed. Compilation after removing the helper proved that statement incomplete.

The helper was called by the legacy `/mca verify <email>` command in `Command.java`. The command constructed its verification URL by modifying the configurable `villagerChatAIEndpoint` and then passed the complete URL to the generic GET helper.

This path was reachable by a player command and therefore was not merely latent. The relevant risk required remediation.

## Implemented controls

### Bounded provider responses

All inspected provider response paths now use the shared streaming `BoundedResponseReader`.

Final limits:

```text
Chat JSON:                 8 MiB
STT JSON:                  4 MiB
TTS audio:                64 MiB
error body:              256 KiB
verification JSON:        64 KiB
provider body-read time:  10 minutes
```

The reader:

- rejects an excessive declared `Content-Length` before reading the body;
- enforces the same bound for chunked or unknown-length streams;
- aborts immediately after the first byte beyond the limit;
- enforces a hard total body-read deadline so slow-drip responses cannot continue indefinitely;
- never includes provider payload data in its limit exceptions;
- replaces unbounded `IOUtils.toString` and `InputStream.transferTo` response handling.

Connect and socket-read timeouts remain active. The total body-read deadline is an additional invariant and is deliberately generous at ten minutes.

### Constrained account verification

`OpenAIChatAI.verify(String encodedURL)` was removed.

The replacement `AccountVerificationClient`:

- accepts a validated Chat endpoint rather than an arbitrary complete URL;
- permits verification only for the trusted Conczin HTTPS provider family;
- constructs the fixed `/v1/mca/verify` path internally;
- disables automatic redirects;
- applies connect/read timeouts;
- applies the shared total body-read deadline;
- applies the 64 KiB verification response limit;
- returns only the expected `success`, `failed` or controlled error state.

### Voice capture bounds

The existing default remains `voiceMaxSeconds=20`.

The runtime now applies non-disableable safety boundaries:

```text
per-session continuous capture: 1..120 seconds
aggregate active microphone PCM: 128 MiB
```

PCM bytes are atomically reserved before buffering. Exhausted budget causes the affected capture to be dropped cleanly without exceeding the global bound.

Reservations are released when a capture finishes, is dropped, fails or the manager shuts down. The release occurs in a `finally` block so decoder cleanup failure cannot leak the reservation.

## TDD evidence

```text
bounded-reader RED
85a88b9fceb7553c3a04f9c1e54f19ad020c3c2d
VillAIgence CI #801 / 30593290408 — expected FAILURE
reason: BoundedResponseReader did not exist

bounded-reader GREEN
94fe1c03a05c7c85fa0a112b963b4cfe96754496
VillAIgence CI #803 / 30593479219 — SUCCESS
Java Pull Request CI #342 / 30593479217 — SUCCESS

widened-limit GREEN checkpoint
8a2116191b484810b8b20ef0a476ecaede1c0dc7
VillAIgence CI #821 / 30618378645 — SUCCESS
Java Pull Request CI #360 / 30618378582 — SUCCESS

defensive PCM release
cb5ed05c66b1015abc2b951c4fd1987742d651cd
regression source guard
19c4a667a47fba63694410e8c0cdb6899228c615

total body-read deadline RED
5d49462fa81a2c12c7e8d2894d18eeedb9c9331c
VillAIgence CI #825 / 30625004367 — expected FAILURE
reason: deadline API and exception did not exist

total body-read deadline GREEN
d7291d277e9bfe9745974abdcdc69569567e3a96
VillAIgence CI #826 / 30625132186 — SUCCESS
Java Pull Request CI #365 / 30625131764 — SUCCESS
```

## Automated coverage

- declared-length rejection before stream reads;
- exact-limit acceptance;
- chunked/unknown-length rejection on the first excess byte;
- total deadline rejection of deterministic slow-drip streams;
- fragmented-stream reconstruction;
- UTF-8 byte limits rather than character limits;
- safe exceptions that do not include provider payloads;
- oversized STT and TTS integration responses from local HTTP servers;
- source guards against unbounded Chat/Audio response helpers;
- trusted-origin-only account verification and disabled redirects;
- atomic PCM reservation, exhaustion, release and concurrent access;
- runtime voice-duration clamp;
- release of PCM reservations even when decoder cleanup fails;
- common tests, Fabric packaging, Fabric compilation and NeoForge compilation.

## Compatibility boundary

- no Memory 2.0 schema change;
- no relationship schema change;
- no voice-profile or world-data schema change;
- configuration version remains `2`;
- no persistent migration is required;
- normal OpenAI/OpenRouter responses remain far below the limits;
- default voice capture duration remains 20 seconds.

## Remaining validation

SEC-003, SEC-004 and SEC-007 are **implemented and automated-CI validated**, but are not marked Closed until:

1. PR #60 is merged;
2. a controlled Minecraft 1.21.1 server test confirms Chat, STT and TTS operation;
3. hostile oversized declared and chunked responses fail safely;
4. a controlled slow-drip provider response is terminated;
5. `/mca verify` works only against the trusted service boundary;
6. concurrent voice capture remains stable;
7. logs contain no credentials, authorization headers, prompts or transcripts;
8. restart validation confirms persistent files remain stable where no mutation is expected.

This status follows the closure rule in `SECURITY_AUDIT_2026-07-31.md`: code and green CI alone are not sufficient for final closure of runtime-sensitive findings.
