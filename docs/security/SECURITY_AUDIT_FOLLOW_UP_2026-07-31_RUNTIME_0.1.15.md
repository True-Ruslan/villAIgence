# Security Audit Follow-up — 0.1.15 Runtime Validation

**Date:** 2026-07-31  
**Release:** `0.1.15+1.21.1`  
**Release commit:** `26070c37b806897e37cc3dabe2e4b27af458ac20`  
**Validation record:** [`../livingworld/VALIDATION_0.1.15.md`](../livingworld/VALIDATION_0.1.15.md)

## Purpose

This follow-up reconciles the original security audit with the real-server validation of the merged H1/H2 implementation.

The validation used the release artifact:

```text
villaigence-fabric-0.1.15+1.21.1.jar
sha256:af142be94885541bb4840d0effff73627afe3f0e245dec8307ed665701cc94fb
```

## Runtime evidence summary

Confirmed on a real Minecraft 1.21.1 server:

- production OpenRouter Chat, STT and TTS operation;
- Pio/Justino NPC isolation;
- persistent player-name and favorite-color memory;
- controlled TTS `io_error` with text and `DIALOGUE` preserved;
- clean recovery from one external OpenRouter `HTTP 429` on a later request;
- identical restart hashes for all six persistent files;
- rejection of LAN HTTP, OpenRouter-lookalike, URI user-info and URI fragment endpoints;
- no rejected-operation memory mutation;
- byte-identical restoration of production configuration;
- no credential/header leakage, corruption/recovery or critical server failure.

## Finding reconciliation

### SEC-001 — Closed

**Original risk:** configurable custom endpoints could receive unrelated provider credentials.

**Implemented controls:** endpoint-family credential binding, dedicated custom audio keys, HTTPS/non-loopback rules and no authenticated redirects.

**Live evidence:** LAN HTTP and provider-lookalike configurations became `MISCONFIGURED`; no provider request, credential transmission or persistent mutation occurred. Normal matching OpenRouter Chat/STT/TTS worked after byte-identical configuration restoration.

**Status:** **Closed** at release `0.1.15+1.21.1`, commit `26070c37b806897e37cc3dabe2e4b27af458ac20`.

### SEC-002 — Closed

**Original risk:** substring-based host trust could classify lookalike hosts as trusted and disclose metadata.

**Implemented controls:** parsed/normalized URI host boundaries, user-info/fragment rejection and exact provider-family classification.

**Live evidence:** OpenRouter lookalike, URI user-info and URI fragment endpoints were rejected as `MISCONFIGURED`; no keys were sent and persistence remained unchanged.

**Status:** **Closed** at release `0.1.15+1.21.1`, commit `26070c37b806897e37cc3dabe2e4b27af458ac20`.

### SEC-003 — Remains open

The normal production Chat/STT/TTS path and fail-soft behavior passed. Automated tests cover declared, chunked/unknown-length, oversized error and slow-drip boundaries.

However, the dedicated real-server mock-provider scenarios for oversized Chat/STT/TTS bodies, error bodies and the ten-minute total deadline were intentionally not executed against OpenRouter/OpenAI.

**Status:** implemented and automated-CI validated; dedicated mock-provider acceptance remains required.

### SEC-004 — Remains open

The generic arbitrary-URL helper is removed and automated coverage constrains account verification. The live run did not execute the dedicated `/mca verify` trusted-origin, lookalike and redirect scenario.

**Status:** implemented and automated-CI validated; controlled verification/redirect acceptance remains required.

### SEC-007 — Remains open

Normal voice operation and TTS failure recovery passed without heap or server failure. The live run did not execute explicit `voiceMaxSeconds` clamp endpoints or aggregate 128 MiB PCM exhaustion/release under concurrent clients.

**Status:** implemented and automated-CI validated; controlled clamp/concurrency/PCM acceptance remains required.

## Current finding status

```text
Closed:
SEC-001
SEC-002
SEC-005
SEC-006
SEC-008
SEC-009

Open pending isolated acceptance:
SEC-003
SEC-004
SEC-007
```

## Release status

`0.1.15+1.21.1` becomes the latest live-validated checkpoint for:

- production text and voice AI operation;
- Chat/STT/TTS provider integration;
- NPC memory isolation;
- TTS fail-soft behavior;
- endpoint validation and credential non-disclosure;
- persistence and restart durability.

This does not represent full closure of Step 1 until the isolated hostile mock-provider and concurrent PCM scenarios are completed.
