#!/usr/bin/env python3
from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding='utf-8')
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{path}: expected one match, found {count}: {old[:100]!r}')
    p.write_text(text.replace(old, new, 1), encoding='utf-8')


def insert_before(path: str, marker: str, block: str) -> None:
    replace_once(path, marker, block + marker)


p = 'docs/PROJECT_STATE.md'
replace_once(p, '> Last major state update: **2026-07-31**, after live validation of `0.1.14+1.21.1` and repository-side completion of Step 1 security hardening.', '> Last major state update: **2026-07-31**, after live validation of `0.1.15+1.21.1`; `0.1.14+1.21.1` remains the forgetting/decay checkpoint.')
replace_once(p, '''latest live-validated release checkpoint:
0.1.14+1.21.1 — PASS
validation date: 2026-07-31
tested release commit: c45aea45dd915b24ba236344feef30559c7171bb
validation marker: V0114_FINAL_RESTART_VERIFICATION_PASS''', '''latest live-validated release checkpoint:
0.1.15+1.21.1 — PASS within executed production/security scope
validation date: 2026-07-31
tested release commit: 26070c37b806897e37cc3dabe2e4b27af458ac20
JAR: villaigence-fabric-0.1.15+1.21.1.jar
JAR SHA-256: af142be94885541bb4840d0effff73627afe3f0e245dec8307ed665701cc94fb''')
replace_once(p, '**Status boundary:** deterministic forgetting/decay, source durability, existing-entry eviction, persistence and NPC isolation are live-proven by `0.1.14+1.21.1`. The rejected-**new**-append no-rewrite branch remains automated-test proven but was not reached through the live Chat model. Repository-side Step 1 H1–H5 is merged and automated-CI validated, but runtime-sensitive SEC-001, SEC-002, SEC-003, SEC-004 and SEC-007 still require the controlled H1/H2 server scenario in a later security candidate.', '**Status boundary:** `0.1.14+1.21.1` live-proves forgetting/decay, source durability, existing-entry eviction, persistence and NPC isolation; the rejected-new-append branch remains automated-test proven only. `0.1.15+1.21.1` live-proves production Chat/STT/TTS, endpoint rejection, fail-soft TTS and six-file restart durability. SEC-001 and SEC-002 are Closed. SEC-003, SEC-004 and SEC-007 remain open only for isolated mock-provider, verification/redirect and concurrent PCM acceptance.')
replace_once(p, '''docs/livingworld/VALIDATION_0.1.14.md
docs/livingworld/VALIDATION_0.1.13.md''', '''docs/livingworld/VALIDATION_0.1.15.md
docs/security/SECURITY_AUDIT_FOLLOW_UP_2026-07-31_RUNTIME_0.1.15.md
docs/livingworld/VALIDATION_0.1.14.md
docs/livingworld/VALIDATION_0.1.13.md''')
replace_once(p, '''0.1.14+1.21.1 → c45aea45dd915b24ba236344feef30559c7171bb
```''', '''0.1.14+1.21.1 → c45aea45dd915b24ba236344feef30559c7171bb
0.1.15+1.21.1 → 26070c37b806897e37cc3dabe2e4b27af458ac20
```''')
replace_once(p, '`0.1.14` identifies the exact forgetting/decay payload tested on the real server. Current `1.21.1` is a descendant of that tag and subsequently advanced through H1–H5 security and supply-chain hardening. Those later commits are not part of the `0.1.14` live checkpoint.', '`0.1.14` remains the exact forgetting/decay payload tested under retention pressure. `0.1.15` identifies the later H1–H5 security payload and is the latest production/security live checkpoint.')
replace_once(p, '''Closed:
SEC-005
SEC-006
SEC-008
SEC-009

Pending controlled runtime validation:
SEC-001
SEC-002
SEC-003
SEC-004
SEC-007''', '''Closed:
SEC-001
SEC-002
SEC-005
SEC-006
SEC-008
SEC-009

Pending isolated acceptance:
SEC-003
SEC-004
SEC-007''')
replace_once(p, '`0.1.14` does not contain H1–H5 and cannot close these runtime-sensitive findings.', '`0.1.15` contains H1–H5 and closes SEC-001/SEC-002 with live evidence. SEC-003/SEC-004/SEC-007 remain pending isolated acceptance.')
insert_before(p, '## 0.1.14+1.21.1 — PASS\n', '''## 0.1.15+1.21.1 — PASS within executed scope

```text
commit: 26070c37b806897e37cc3dabe2e4b27af458ac20
JAR: villaigence-fabric-0.1.15+1.21.1.jar
SHA-256: af142be94885541bb4840d0effff73627afe3f0e245dec8307ed665701cc94fb
```

Live-proven:

```text
Text / STT / TTS                                      PASS
Pio / Justino isolation                               PASS
Pio name and favorite-color recall                    PASS
TTS io_error preserved text and DIALOGUE              PASS
OpenRouter HTTP 429 remained controlled               PASS
six persistent files hash-identical after restart    PASS
LAN HTTP / lookalike / user-info / fragment rejected PASS
production config restored byte-for-byte              PASS
keys or Authorization leaked                          no
server / UDP 24454 / TCP 25565 / monitor              healthy
```

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.15.md
docs/security/SECURITY_AUDIT_FOLLOW_UP_2026-07-31_RUNTIME_0.1.15.md
```

''')
replace_once(p, 'Gameplay and Memory 2.0 behavior is live-validated through `0.1.14+1.21.1`.\n\nRepository-side security hardening is merged after that release and awaits a controlled security candidate validation.', 'Gameplay and Memory 2.0 retention behavior is live-validated by `0.1.14+1.21.1`. Production Chat/STT/TTS and endpoint-policy behavior is live-validated by `0.1.15+1.21.1`.')
replace_once(p, '''1. Build and install a security candidate containing H1–H5, expected 0.1.15+1.21.1
2. Run docs/security/H1_H2_CONTROLLED_SERVER_VALIDATION.md
3. Preserve candidate JAR SHA-256, dependency manifest, redacted logs and restart hashes
4. Promote the candidate only after applicable security/runtime evidence is complete
5. Exercise rejected-new-append live only if a deterministic test path becomes available; do not block other work on model randomness
6. Design legacy memory.json migration
7. Run long-horizon Memory 2.0 exit-criterion validation
8. Begin 0.3 Personality + NPC↔NPC social graph''', '''1. Run isolated hostile mock-provider acceptance for SEC-003
2. Run controlled /mca verify and redirect acceptance for SEC-004
3. Run voice clamp and concurrent PCM exhaustion/recovery acceptance for SEC-007
4. Close the remaining Step 1 findings if evidence passes
5. Exercise rejected-new-append live only if a deterministic test path becomes available
6. Design legacy memory.json migration
7. Run long-horizon Memory 2.0 exit-criterion validation
8. Begin 0.3 Personality + NPC↔NPC social graph''')
replace_once(p, '''Run the complete H1/H2 security and resource-bound scenario:

```text
docs/security/H1_H2_CONTROLLED_SERVER_VALIDATION.md
```

Required candidate characteristics:

- contains PRs #59–#63;
- common, Fabric, NeoForge and repository security workflows green;
- release artifact and dependency manifest retained;
- provider endpoint and credential behavior exercised;
- bounded Chat/STT/TTS/error responses exercised;
- voice duration and aggregate PCM limits exercised;
- normal Chat/STT/TTS/Voice Chat behavior preserved;
- five persistent files stable across restart;
- no secrets or unredacted provider bodies stored in evidence.

After successful security validation, the next implementation design target is legacy `memory.json` migration unless live evidence exposes a concrete defect.''', '''Preserve `0.1.15+1.21.1` as the latest production/security live checkpoint.

Execute only the remaining isolated sections from:

```text
docs/security/H1_H2_CONTROLLED_SERVER_VALIDATION.md
```

Required remaining evidence:

- hostile oversized/chunked/error/slow-drip mock-provider behavior;
- `/mca verify` trusted-origin and redirect behavior;
- voice duration clamp and aggregate PCM exhaustion/recovery;
- no secrets or unredacted provider bodies in evidence.

After SEC-003/SEC-004/SEC-007 closure, continue with legacy `memory.json` migration unless live evidence exposes a concrete defect.''')
replace_once(p, '`docs/livingworld/VALIDATION_0.1.14.md` and `docs/security/README.md`', '`docs/livingworld/VALIDATION_0.1.15.md`, `docs/livingworld/VALIDATION_0.1.14.md` and `docs/security/README.md`')

c = 'docs/CHANGELOG.md'
insert_before(c, '## 2026-07-31 — Step 1 security and supply-chain hardening\n', '''## 2026-07-31 — 0.1.15 production and endpoint-policy validation

**Status:** PASS within executed real-server scope; latest production/security live checkpoint.

```text
tag: 0.1.15+1.21.1
commit: 26070c37b806897e37cc3dabe2e4b27af458ac20
JAR: villaigence-fabric-0.1.15+1.21.1.jar
SHA-256: af142be94885541bb4840d0effff73627afe3f0e245dec8307ed665701cc94fb
```

Validated production Text/STT/TTS, Pio/Justino isolation, Pio memory recall, TTS fail-soft behavior, controlled OpenRouter 429 handling, six-file restart hashes, endpoint rejection, byte-identical configuration restoration and log redaction.

```text
Closed: SEC-001, SEC-002
Pending isolated acceptance: SEC-003, SEC-004, SEC-007
```

Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.15.md
docs/security/SECURITY_AUDIT_FOLLOW_UP_2026-07-31_RUNTIME_0.1.15.md
```

---

''')
replace_once(c, '**Status:** repository-side H1–H5 merged and automated-CI validated; controlled H1/H2 real-server validation remains.', '**Status:** repository-side H1–H5 merged and automated-CI validated; production endpoint-policy validation passed in `0.1.15+1.21.1`; isolated SEC-003/SEC-004/SEC-007 acceptance remains.')
replace_once(c, '''Closed: SEC-005, SEC-006, SEC-008, SEC-009
Pending controlled server validation: SEC-001, SEC-002, SEC-003, SEC-004, SEC-007''', '''Closed: SEC-001, SEC-002, SEC-005, SEC-006, SEC-008, SEC-009
Pending isolated acceptance: SEC-003, SEC-004, SEC-007''')
replace_once(c, '`0.1.14+1.21.1` is now the latest live-validated Memory 2.0 checkpoint, but it predates H1–H5. The expected security-validation candidate is `0.1.15+1.21.1`; its test plan is `docs/security/H1_H2_CONTROLLED_SERVER_VALIDATION.md`.', '`0.1.14+1.21.1` remains the forgetting/decay checkpoint. `0.1.15+1.21.1` is the latest production/security live checkpoint; remaining isolated acceptance uses `docs/security/H1_H2_CONTROLLED_SERVER_VALIDATION.md`.')

r = 'docs/security/README.md'
insert_before(r, '- [`APPROVED_SCRIPT_INVENTORY.json`](APPROVED_SCRIPT_INVENTORY.json)', '- [`SECURITY_AUDIT_FOLLOW_UP_2026-07-31_RUNTIME_0.1.15.md`](SECURITY_AUDIT_FOLLOW_UP_2026-07-31_RUNTIME_0.1.15.md) — closes SEC-001 and SEC-002 using real-server evidence.\n- [`../livingworld/VALIDATION_0.1.15.md`](../livingworld/VALIDATION_0.1.15.md) — production Chat/STT/TTS, endpoint-policy, persistence and restart validation.\n')
replace_once(r, '- H1 provider endpoint and credential policy: merged and automated-CI validated; real-server smoke remains required before SEC-001/SEC-002 closure.', '- H1 provider endpoint and credential policy: merged and live-validated in `0.1.15+1.21.1`; SEC-001 and SEC-002 are Closed.')
replace_once(r, '- H2 bounded network I/O and voice resource controls: merged as `15c56526417ac7dfb76567d51d1aa107f522cda7`; real-server smoke remains required before SEC-003/SEC-004/SEC-007 closure.', '- H2 bounded network I/O and voice resource controls: normal production and TTS fail-soft behavior passed in `0.1.15+1.21.1`; SEC-003, SEC-004 and SEC-007 remain open for isolated acceptance.')

s = 'docs/security/STEP_1_TRACKER.md'
replace_once(s, '**Status:** H1–H5 merged; repository-side hardening is complete; controlled H1/H2 real-server validation remains', '**Status:** H1–H5 merged; `0.1.15+1.21.1` production validation passed; SEC-003/SEC-004/SEC-007 isolated acceptance remains')
replace_once(s, '''- [ ] Complete controlled real-server smoke validation.
- [ ] Close SEC-001 and SEC-002 in a dated audit follow-up.''', '''- [x] Complete production real-server endpoint and credential validation in `0.1.15+1.21.1`.
- [x] Close SEC-001 and SEC-002 in `SECURITY_AUDIT_FOLLOW_UP_2026-07-31_RUNTIME_0.1.15.md`.''')
replace_once(s, '''- [ ] Complete controlled real-server validation.
- [ ] Close SEC-003, SEC-004 and SEC-007 in a dated follow-up.''', '''- [x] Complete production Chat/STT/TTS, TTS fail-soft and restart validation in `0.1.15+1.21.1`.
- [ ] Complete isolated mock-provider acceptance for SEC-003.
- [ ] Complete controlled `/mca verify` and redirect acceptance for SEC-004.
- [ ] Complete voice clamp and concurrent PCM acceptance for SEC-007.
- [ ] Close SEC-003, SEC-004 and SEC-007 after those scenarios pass.''')
replace_once(s, '''- [ ] Standard OpenRouter/OpenAI configuration works with merged H1/H2.
- [ ] Invalid HTTP/lookalike endpoints fail safely.
- [ ] Explicit loopback development mode works only with opt-in.
- [ ] Oversized declared/chunked responses fail safely.
- [ ] Slow-drip responses terminate at the total deadline.
- [ ] Text Chat persists exactly once.
- [ ] Voice STT/TTS remains operational.
- [ ] TTS failure preserves text output.
- [ ] Concurrent voice capture remains stable under the PCM budget.
- [ ] Logs contain no credentials, authorization headers, prompts or transcripts.
- [ ] Persistent world files remain stable across restart when no mutation is expected.
- [ ] Release JAR, checksum and dependency manifest are retained as evidence.''', '''- [x] Standard OpenRouter configuration works with merged H1/H2 in `0.1.15+1.21.1`.
- [x] LAN HTTP, lookalike, user-info and fragment endpoints fail safely without persistence mutation.
- [ ] Explicit loopback development mode works only with opt-in.
- [ ] Oversized declared/chunked responses fail safely in the mock-provider harness.
- [ ] Slow-drip responses terminate at the total deadline in the mock-provider harness.
- [x] Text Chat and Memory 2.0 DIALOGUE persistence remain operational.
- [x] Voice STT/TTS remains operational.
- [x] TTS failure preserves text output and DIALOGUE.
- [ ] Concurrent voice capture remains stable under the PCM budget.
- [x] Logs contain no credentials or authorization headers in the reviewed run.
- [x] All six persistent world files remain hash-identical across restart.
- [x] Release JAR filename, tag, commit and SHA-256 are retained as evidence.''')
replace_once(s, '- [ ] Mark Step 1 fully complete only after applicable live validation exists.', '- [ ] Mark Step 1 fully complete only after SEC-003, SEC-004 and SEC-007 isolated acceptance exists.')
