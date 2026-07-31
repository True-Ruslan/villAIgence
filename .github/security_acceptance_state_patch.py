#!/usr/bin/env python3
from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


def write(path: str, transform) -> None:
    target = Path(path)
    original = target.read_text(encoding="utf-8")
    updated = transform(original)
    if updated == original:
        raise RuntimeError(f"{path}: patch made no changes")
    target.write_text(updated, encoding="utf-8")


def patch_state(text: str) -> str:
    text = replace_once(
        text,
        "> Last major state update: **2026-07-31**, after live validation of `0.1.15+1.21.1`; `0.1.14+1.21.1` remains the forgetting/decay checkpoint.",
        "> Last major state update: **2026-07-31**, after preparing the residual Step 1 security acceptance harness on top of live-validated `0.1.15+1.21.1`; `0.1.14+1.21.1` remains the forgetting/decay checkpoint.",
        "state update line",
    )
    text = replace_once(
        text,
        "**Status boundary:** `0.1.14+1.21.1` live-proves forgetting/decay, source durability, existing-entry eviction, persistence and NPC isolation; the rejected-new-append branch remains automated-test proven only. `0.1.15+1.21.1` live-proves production Chat/STT/TTS, endpoint rejection, fail-soft TTS and six-file restart durability. SEC-001 and SEC-002 are Closed. SEC-003, SEC-004 and SEC-007 remain open only for isolated mock-provider, verification/redirect and concurrent PCM acceptance.",
        "**Status boundary:** `0.1.14+1.21.1` live-proves forgetting/decay, source durability, existing-entry eviction, persistence and NPC isolation; the rejected-new-append branch remains automated-test proven only. `0.1.15+1.21.1` live-proves production Chat/STT/TTS, endpoint rejection, fail-soft TTS and six-file restart durability. SEC-001 and SEC-002 are Closed. Deterministic literal-loopback provider tooling and exact-release-JAR verification/PCM probes are prepared for SEC-003, SEC-004 and SEC-007; those findings remain open until the controlled candidate run passes.",
        "status boundary",
    )
    text = replace_once(
        text,
        "docs/security/H1_H2_CONTROLLED_SERVER_VALIDATION.md\n```",
        "docs/security/H1_H2_CONTROLLED_SERVER_VALIDATION.md\ndocs/security/LOCAL_SECURITY_ACCEPTANCE_HARNESS.md\n```",
        "canonical evidence",
    )
    text = replace_once(
        text,
        "`0.1.15` contains H1–H5 and closes SEC-001/SEC-002 with live evidence. SEC-003/SEC-004/SEC-007 remain pending isolated acceptance.\n\n---\n\n# Identity and compatibility",
        """`0.1.15` contains H1–H5 and closes SEC-001/SEC-002 with live evidence. SEC-003/SEC-004/SEC-007 remain pending isolated acceptance.

## Residual acceptance tooling — prepared

PR #68 adds the controlled execution surface required for the remaining findings:

```text
scripts/security/provider_acceptance_harness.py
→ literal loopback bind only
→ normal / declared / chunked / error / redirect / slow-drip routes
→ streamed hostile payloads without whole-body allocation
→ sanitized manifest and JSONL evidence

AccountVerificationAcceptanceProbe
→ explicit java -cp invocation only
→ literal loopback target validation
→ shared JDK-only bounded/no-redirect production transport

VoicePcmBudgetAcceptanceProbe
→ explicit java -cp invocation only
→ exact 1..120 second clamp
→ exact 128 MiB budget contention, rejection, release and recovery
```

The distributable Fabric package is required to contain both probes and the shared verification transport. Security CI runs the Python harness contract tests and deterministic seven-script inventory. No probe or harness has an in-game command, startup hook, production-key lookup or persistent schema effect.

Tooling does not close SEC-003, SEC-004 or SEC-007. A release containing PR #68 must still complete `docs/security/LOCAL_SECURITY_ACCEPTANCE_HARNESS.md` on the controlled server.

---

# Identity and compatibility""",
        "residual tooling section",
    )
    text = replace_once(
        text,
        """```text
1. Run isolated hostile mock-provider acceptance for SEC-003
2. Run controlled /mca verify and redirect acceptance for SEC-004
3. Run voice clamp and concurrent PCM exhaustion/recovery acceptance for SEC-007
4. Close the remaining Step 1 findings if evidence passes
5. Exercise rejected-new-append live only if a deterministic test path becomes available
6. Design legacy memory.json migration
7. Run long-horizon Memory 2.0 exit-criterion validation
8. Begin 0.3 Personality + NPC↔NPC social graph
```""",
        """```text
1. Merge PR #68 and build the first release containing the acceptance harness, expected 0.1.16+1.21.1
2. Run docs/security/LOCAL_SECURITY_ACCEPTANCE_HARNESS.md for SEC-003
3. Run the exact-JAR verification probe and redirect checks for SEC-004
4. Run the exact-JAR voice clamp/PCM probe plus final microphone smoke for SEC-007
5. Close the remaining Step 1 findings only if persistence, redaction and recovery evidence passes
6. Exercise rejected-new-append live only if a deterministic test path becomes available
7. Design legacy memory.json migration
8. Run long-horizon Memory 2.0 exit-criterion validation
9. Begin 0.3 Personality + NPC↔NPC social graph
```""",
        "next sequence",
    )
    text = replace_once(
        text,
        """Execute only the remaining isolated sections from:

```text
docs/security/H1_H2_CONTROLLED_SERVER_VALIDATION.md
```

Required remaining evidence:

- hostile oversized/chunked/error/slow-drip mock-provider behavior;
- `/mca verify` trusted-origin and redirect behavior;
- voice duration clamp and aggregate PCM exhaustion/recovery;
- no secrets or unredacted provider bodies in evidence.

After SEC-003/SEC-004/SEC-007 closure, continue with legacy `memory.json` migration unless live evidence exposes a concrete defect.""",
        """Build the first candidate containing PR #68, expected `0.1.16+1.21.1`, then execute:

```text
docs/security/LOCAL_SECURITY_ACCEPTANCE_HARNESS.md
```

Required remaining evidence:

- declared, chunked, error and slow-drip Chat/STT/TTS behavior from literal loopback;
- zero redirect-target hits for provider and verification redirects;
- 64 KiB verification response bound through the exact-JAR transport probe;
- exact 1/120 second clamp and 128 MiB PCM rejection/recovery output;
- normal microphone operation after the PCM probe;
- byte-identical rejected-operation persistence and production configuration restoration;
- no secrets or unredacted provider bodies in evidence.

After SEC-003/SEC-004/SEC-007 closure, continue with legacy `memory.json` migration unless live evidence exposes a concrete defect.""",
        "immediate target",
    )
    text = replace_once(
        text,
        "`docs/livingworld/VALIDATION_0.1.14.md` and `docs/security/README.md`",
        "`docs/livingworld/VALIDATION_0.1.14.md`, `docs/security/README.md` and `docs/security/LOCAL_SECURITY_ACCEPTANCE_HARNESS.md`",
        "resume prompt",
    )
    return text


def patch_changelog(text: str) -> str:
    marker = "> Human-readable implementation and validation history. For exact current state and next priority, read `docs/PROJECT_STATE.md`. For long-term direction, read `docs/ROADMAP.md`.\n\n"
    section = """## 2026-07-31 — Residual Step 1 security acceptance harness

**Status:** implementation and automated validation prepared in PR #68; controlled release/server execution remains pending.

### Added

- dependency-free Python provider harness restricted to literal loopback;
- deterministic normal, declared-oversize, chunked-oversize, oversized-error, redirect and slow-drip routes for Chat/STT/TTS/verification;
- streamed hostile payloads without whole-body allocation;
- sanitized JSONL evidence containing header-presence booleans and query-key names, never values;
- shared JDK-only account-verification transport preserving fixed production origin policy and disabled redirects;
- exact-release-JAR verification probe restricted to literal loopback;
- exact-release-JAR voice probe covering the 1..120 second clamp, 128 MiB concurrent budget, overflow rejection, release and recovery;
- package smoke checks requiring every probe/transport class in the distributable Fabric JAR;
- standard-library harness tests in read-only repository security CI;
- current approved script inventory expanded from the historical H5 five launchers to seven reviewed scripts.

### Safety boundary

```text
no in-game command
no Minecraft startup hook
no production-provider traffic
no production credential lookup by probes
no config or persistence schema change
```

SEC-003, SEC-004 and SEC-007 remain Open. Tooling alone is not closure evidence. The next candidate, expected `0.1.16+1.21.1`, must complete `docs/security/LOCAL_SECURITY_ACCEPTANCE_HARNESS.md` on the controlled server.

---

"""
    if section.strip() in text:
        raise RuntimeError("changelog section already present")
    text = replace_once(text, marker, marker + section, "changelog insertion")
    text = replace_once(
        text,
        "- the approved executable/script surface is reduced to five Gradle/CI/security launchers.",
        "- H5 reduced the executable/script surface to five Gradle/CI/security launchers; the later reviewed acceptance harness and its CI test intentionally bring the current approved inventory to seven scripts.",
        "script inventory history",
    )
    text = replace_once(
        text,
        "remaining isolated acceptance uses `docs/security/H1_H2_CONTROLLED_SERVER_VALIDATION.md`.",
        "remaining isolated acceptance uses `docs/security/LOCAL_SECURITY_ACCEPTANCE_HARNESS.md` in a release containing PR #68.",
        "remaining acceptance link",
    )
    return text


def patch_h1_h2(text: str) -> str:
    text = replace_once(
        text,
        "Repository-side implementation H1–H5 is merged. This document covers only real Minecraft 1.21.1 server behavior.",
        "Repository-side implementation H1–H5 is merged. Release `0.1.15+1.21.1` completed production validation and closed SEC-001/SEC-002. This historical full scenario remains canonical; execute the residual SEC-003/SEC-004/SEC-007 phases through `LOCAL_SECURITY_ACCEPTANCE_HARNESS.md`, which provides the literal-loopback server and exact-JAR probes.",
        "H1/H2 purpose",
    )
    old = """## Candidate boundary

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

Do not promote the candidate to the latest live-validated checkpoint until every required positive test passes and every negative test fails safely."""
    new = """## Candidate boundary

Historical production/endpoint-policy candidate:

```text
0.1.15+1.21.1
commit 26070c37b806897e37cc3dabe2e4b27af458ac20
SEC-001 / SEC-002 Closed
```

Residual acceptance must use a later release built from default branch `1.21.1` after PR #68 is merged, expected:

```text
0.1.16+1.21.1
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

Use `LOCAL_SECURITY_ACCEPTANCE_HARNESS.md` for exact commands. Do not close SEC-003/SEC-004/SEC-007 until every required negative path fails safely, the probes pass and normal production behavior is restored."""
    return replace_once(text, old, new, "H1/H2 candidate boundary")


write("docs/PROJECT_STATE.md", patch_state)
write("docs/CHANGELOG.md", patch_changelog)
write("docs/security/H1_H2_CONTROLLED_SERVER_VALIDATION.md", patch_h1_h2)
