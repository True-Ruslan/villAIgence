# Security Acceptance Harness Design

## Purpose

Prepare a safe, repeatable acceptance environment for the three remaining Step 1 findings without sending hostile traffic to OpenRouter, OpenAI or any production provider:

```text
SEC-003 bounded provider responses
SEC-004 constrained account verification and redirect handling
SEC-007 voice duration and aggregate PCM budget
```

The harness must exercise the exact production limits and transports from the exact VillAIgence JAR while remaining inert during normal Minecraft startup.

## Chosen approach

Use three isolated components:

1. a Python standard-library loopback HTTP server for controlled provider responses;
2. a JAR-contained account-verification probe that calls the same bounded, no-redirect transport as production verification;
3. a JAR-contained PCM budget probe that exercises the real `VoicePcmBudget` implementation and exact 128 MiB constant.

The Python server is an operator tool. The Java probes are ordinary classes with `main` methods but are never invoked by Minecraft, Fabric, NeoForge or normal server startup.

## Alternatives rejected

### Production-provider fault injection

Rejected because oversized, chunked and slow-drip payloads must never be generated against OpenRouter/OpenAI. It would also provide weak evidence because the operator cannot control exact response boundaries.

### OP-only in-game security commands

Rejected because adding runtime commands or configuration flags expands the production attack surface and requires command-permission lifecycle maintenance.

### Test-only Java mocks

Retained as regression support but insufficient alone. They cannot provide a real loopback endpoint for the deployed server or produce an operator evidence bundle from the exact release JAR.

## Component boundaries

### `scripts/security/provider_acceptance_harness.py`

A dependency-free `ThreadingHTTPServer` bound only to literal loopback addresses. It exposes deterministic paths for:

```text
normal response
declared Content-Length above the stage limit
chunked/unknown-length response one byte above the stage limit
oversized non-2xx error body
307 redirect to a local capture endpoint
slow-drip chunked response beyond the ten-minute body deadline
verification success / failed / oversized / redirect
```

Supported production limits are imported as constants copied from `ProviderResponseLimits` and documented next to the Java source anchors:

```text
Chat JSON                 8 MiB
STT JSON                  4 MiB
TTS audio                64 MiB
provider error body     256 KiB
account verification     64 KiB
body read deadline       10 minutes
```

The server streams large bodies in bounded chunks and never allocates the complete hostile payload. It writes JSONL evidence containing only method, path, query-key names, body byte count and header-presence booleans. Header values, API keys, email values and request bodies are never persisted.

### `AccountVerificationClient` transport split

Production verification retains its current flow:

```text
configured Chat endpoint
→ ProviderEndpointPolicy exact trusted Conczin HTTPS validation
→ fixed /v1/mca/verify URI
→ bounded no-redirect transport
```

The HTTP execution body becomes a package-private reusable method. The public production API still accepts only the configured Chat endpoint and still cannot accept an arbitrary URL.

### `AccountVerificationAcceptanceProbe`

A standalone main class in the same package calls the reusable verification transport. It accepts only literal loopback `http` or `https` URIs with no user-info or fragment. It prints a single JSON result marker and exits nonzero when arguments are unsafe or malformed.

This allows redirect and response-bound acceptance against the local harness without weakening production origin validation.

### `VoicePcmBudgetAcceptanceProbe`

A standalone JDK-only main class exercises `VoicePcmBudget` using `VoiceCaptureLimits.MAX_ACTIVE_PCM_BYTES`.

The probe starts concurrent reservations, verifies the budget never exceeds 128 MiB, records accepted and rejected workers, releases every successful reservation and proves that a full-budget reservation succeeds afterward. It prints one JSON result with marker `VILLAIGENCE_PCM_PROBE_PASS`.

It does not allocate PCM arrays; it tests the same atomic reservation accounting used by active capture sessions.

## Safety invariants

- harness binding is rejected unless the address is a literal loopback IP;
- probes reject non-loopback targets before opening a connection;
- no probe reads environment API keys or configuration credentials;
- no Authorization value, query value, prompt, transcript or request body is written to evidence;
- redirects are not followed by the verification transport;
- hostile bodies are generated locally and streamed with bounded server memory;
- normal Minecraft startup performs no harness or probe work;
- no persistent world or configuration schema changes;
- no new third-party dependency;
- Python tests use only the standard library;
- all new scripts are declared in the reviewed script inventory.

## TDD and validation

The first implementation commit contains failing Python and Java contract tests:

- loopback harness module is absent;
- verification transport/probe APIs are absent;
- PCM probe API is absent.

The implementation then makes those tests pass. Final required checks:

```text
python3 scripts/ci/test_provider_acceptance_harness.py
./gradlew --no-daemon :common:test
./gradlew --no-daemon :fabric:build :neoforge:build
python3 scripts/ci/repository_security_policy.py --check
scripts/ci/package-livingworld-release.sh
```

## Acceptance outcome

This PR prepares deterministic tooling and automated coverage. It does not close SEC-003, SEC-004 or SEC-007 by itself. Closure requires running the probes and real-server provider scenarios against a release containing this harness, preserving the generated evidence, confirming persistence/log safety and recording a dated validation follow-up.
