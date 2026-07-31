#!/usr/bin/env python3
from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    target = Path(path)
    text = target.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected one replacement, found {count}: {old[:100]!r}")
    target.write_text(text.replace(old, new, 1), encoding="utf-8")


def insert_before_once(path: str, marker: str, addition: str) -> None:
    replace_once(path, marker, addition + marker)


project = "docs/PROJECT_STATE.md"
replace_once(
    project,
    "> Last major state update: **2026-07-30**, after merged PR #56.",
    "> Last major state update: **2026-07-31**, after merged Step 1 security PRs #59–#63.",
)
replace_once(
    project,
    """latest implementation:
PR #56 — deterministic Semantic Memory forgetting and decay
merge: 73145dd0925d403af7ef343521eb3ae27f68804d
exact verified feature head: c08b47431b6a121deae4be8410be1e4fe4c5126a

exact-head CI:
VillAIgence CI #764 / 30573965448 — SUCCESS
Java Pull Request CI #307 / 30573965439 — SUCCESS""",
    """latest merged engineering program:
Step 1 Security and supply-chain hardening — H1 through H5
PRs: #59, #60, #61, #62, #63
closing merge: 6d82b4e4650294a4a42b9ea2113e64d990e08811
final H5 validated code head: ae26a9445b646c02e53b9fe8a557204fd703c7ff

final H5 exact-head CI:
VillAIgence CI #922 / 30636167806 — SUCCESS
Java Pull Request CI #458 / 30636168112 — SUCCESS
Repository security policy #79 / 30636168870 — SUCCESS

latest gameplay/memory implementation:
PR #56 — deterministic Semantic Memory forgetting and decay
merge: 73145dd0925d403af7ef343521eb3ae27f68804d
exact verified feature head: c08b47431b6a121deae4be8410be1e4fe4c5126a""",
)
replace_once(
    project,
    "**Status boundary:** deterministic forgetting/decay is merged and automated-CI validated, but not yet validated on a real server. `0.1.13+1.21.1` remains the latest live-server checkpoint.",
    "**Status boundary:** repository-side Step 1 H1–H5 is merged and automated-CI validated. SEC-005, SEC-006, SEC-008 and SEC-009 are Closed. Runtime-sensitive SEC-001, SEC-002, SEC-003, SEC-004 and SEC-007 still require the controlled H1/H2 server scenario. Deterministic forgetting/decay from PR #56 also remains pending real-server validation. `0.1.13+1.21.1` remains the latest live-server checkpoint.",
)
insert_before_once(
    project,
    "## Release metadata status\n",
    """## Step 1 security hardening — repository implementation complete

Merged sequence:

```text
H1 provider endpoint and credential policy     PR #59 → 787f1a781b5970d4bafb851bfb3c7cba7c21fc0a
H2 bounded network and voice resources         PR #60 → 15c56526417ac7dfb76567d51d1aa107f522cda7
H3 immutable verified build inputs             PR #61 → 4cf9aef2e5c31a5682a7cad8544219154330e056
H4 primary CI and repository security policy   PR #62 → 05d105c1f558d5643b8190a88cc744b4d7cbe129
H5 legacy utility and whole-tree closure       PR #63 → 6d82b4e4650294a4a42b9ea2113e64d990e08811
```

Implemented controls include:

- normalized endpoint validation, endpoint-family credential binding and blocked authenticated redirects;
- bounded Chat/STT/TTS/error/verification bodies plus a hard total response deadline;
- voice capture clamp and aggregate PCM memory budget;
- stable Fabric Loom, verified Gradle wrapper, dependency checksums/locks and immutable GitHub Actions;
- required common/Fabric/NeoForge CI and deterministic secret/source/workflow policy;
- exact-head tracked-tree manifests and a five-launcher approved script inventory;
- removal of every inherited non-CI network/generation utility and its tool-only resources.

Closed findings:

```text
SEC-005
SEC-006
SEC-008
SEC-009
```

Pending controlled runtime validation:

```text
SEC-001
SEC-002
SEC-003
SEC-004
SEC-007
```

Canonical security evidence starts at `docs/security/README.md`. The required runtime scenario is `docs/security/H1_H2_CONTROLLED_SERVER_VALIDATION.md`.

""",
)
replace_once(
    project,
    "- diagnostics without secrets, prompts, transcripts or hidden reasoning.",
    """- diagnostics without secrets, prompts, transcripts or hidden reasoning;
- validated provider destinations and endpoint-aware credential selection;
- no authenticated provider redirects;
- byte-bounded Chat/STT/TTS/error/verification responses and a ten-minute total body-read deadline;
- `voiceMaxSeconds` clamped to `1..120` and global active PCM bounded to 128 MiB;
- stable verified Gradle/dependency/action inputs;
- required common, Fabric and NeoForge CI;
- deterministic secret/source/workflow/script policy and exact-head whole-tree evidence;
- only five approved Gradle/CI/security launchers remain in the repository.""",
)
replace_once(
    project,
    """## Next sequence

```text
1. Live-validate PR #56 forgetting/decay
2. Calibrate only if live evidence exposes a concrete retention defect
3. Design legacy memory.json migration after semantic layers stabilize
4. Run long-horizon Memory 2.0 exit-criterion validation
5. Begin 0.3 Personality + NPC↔NPC social graph
```""",
    """## Next sequence

```text
1. Build and install candidate 0.1.15+1.21.1 from current 1.21.1
2. Run docs/security/H1_H2_CONTROLLED_SERVER_VALIDATION.md
3. Live-validate PR #56 forgetting/decay in the candidate or a dedicated controlled build
4. Calibrate only if live evidence exposes a concrete defect
5. Design legacy memory.json migration after semantic layers stabilize
6. Run long-horizon Memory 2.0 exit-criterion validation
7. Begin 0.3 Personality + NPC↔NPC social graph
```""",
)
insert_before_once(
    project,
    "# Immediate live-test scenario\n",
    """# Immediate validation sequence

1. Run the complete H1/H2 security and resource-bound scenario in `docs/security/H1_H2_CONTROLLED_SERVER_VALIDATION.md` against candidate `0.1.15+1.21.1`.
2. Preserve the candidate JAR SHA-256, dependency manifest, redacted logs and pre/post-restart hashes.
3. Then run the deterministic forgetting/decay scenario below.
4. Promote the candidate only after both applicable validation records are complete.

""",
)

changelog = "docs/CHANGELOG.md"
insert_before_once(
    changelog,
    "## Post-0.1.13 — Deterministic Semantic Memory forgetting and decay\n",
    """## 2026-07-31 — Step 1 security and supply-chain hardening

**Status:** repository-side H1–H5 merged and automated-CI validated; controlled H1/H2 real-server validation remains.

### Merge sequence

```text
PR #59 H1 endpoint/credential policy       787f1a781b5970d4bafb851bfb3c7cba7c21fc0a
PR #60 H2 bounded provider/voice resources 15c56526417ac7dfb76567d51d1aa107f522cda7
PR #61 H3 verified supply chain            4cf9aef2e5c31a5682a7cad8544219154330e056
PR #62 H4 CI security coverage             05d105c1f558d5643b8190a88cc744b4d7cbe129
PR #63 H5 legacy-tool closure              6d82b4e4650294a4a42b9ea2113e64d990e08811
```

### Provider and runtime hardening

- provider URLs are parsed and normalized before credentials are selected;
- remote plaintext endpoints are rejected; HTTP is limited to explicit lexical loopback development mode;
- OpenAI/OpenRouter/custom Chat, STT and TTS credentials cannot cross endpoint-family boundaries;
- authenticated redirects are not followed;
- Chat, STT, TTS, provider-error and verification bodies are byte-bounded;
- slow-drip responses have a hard total deadline;
- arbitrary-URL account verification was replaced with a fixed trusted-origin client;
- voice capture is clamped to `1..120` seconds and aggregate active PCM is bounded to 128 MiB.

### Supply-chain and CI hardening

- Fabric Loom moved from snapshot to stable `1.17.17`;
- Gradle wrapper distribution checksum and wrapper validation are enforced;
- external GitHub Actions are pinned to immutable commit SHAs;
- dependency verification metadata and per-project lockfiles are committed;
- third-party Maven content is restricted;
- release packages contain a deterministic lockfile-based dependency manifest;
- primary CI requires common tests, Fabric and NeoForge;
- a dependency-free policy scans high-confidence secrets, dangerous Java/Gradle APIs, workflow permissions and tracked scripts;
- exact-head script and whole-tree SHA-256 artifacts are retained.

### Whole-tree cleanup

- all inherited non-CI utilities were semantically reviewed;
- deprecated Google/AWS TTS, Crowdin/patron fetch, external-LLM localization, pirate translation, name generation and skin generation utilities were removed;
- the unmanaged Python requirements and 4.3 MiB raw name dataset were removed;
- generated game resources remain unchanged;
- the approved executable/script surface is now exactly five Gradle/CI/security launchers.

### Finding status

```text
Closed: SEC-005, SEC-006, SEC-008, SEC-009
Pending controlled server validation: SEC-001, SEC-002, SEC-003, SEC-004, SEC-007
```

`0.1.13+1.21.1` remains the latest live-validated checkpoint. The expected validation candidate is `0.1.15+1.21.1`; its test plan is `docs/security/H1_H2_CONTROLLED_SERVER_VALIDATION.md`.

""",
)

h5 = "docs/security/H5_LEGACY_TOOLS_AUDIT_CLOSURE_2026-07-31.md"
insert_before_once(
    h5,
    "No gameplay, provider, Memory 2.0, generated game-resource or persistent world format was changed.\n",
    """Merged through PR #63 as squash commit:

```text
6d82b4e4650294a4a42b9ea2113e64d990e08811
```

""",
)
replace_once(
    h5,
    "Implementation and automated validation are complete. The network-capable pirate translator and all related unmanaged Python dependency metadata are removed. SEC-008 remains open only until PR #63 is merged and the exact squash commit is recorded.",
    "**Closed.** The network-capable pirate translator and all related unmanaged Python dependency metadata were removed and squash-merged through PR #63 at `6d82b4e4650294a4a42b9ea2113e64d990e08811`. Exact closing reconciliation is recorded in `SECURITY_AUDIT_FOLLOW_UP_2026-07-31_H5.md`.",
)
replace_once(
    h5,
    "SEC-009 remains open only until PR #63 is merged and the exact closing commit is reconciled in a dated audit follow-up.",
    "**SEC-009 is Closed.** PR #63 was squash-merged at `6d82b4e4650294a4a42b9ea2113e64d990e08811`; the dated closing record is `SECURITY_AUDIT_FOLLOW_UP_2026-07-31_H5.md`.",
)

readme = "docs/security/README.md"
insert_before_once(
    readme,
    "- [`APPROVED_SCRIPT_INVENTORY.json`](APPROVED_SCRIPT_INVENTORY.json)",
    "- [`SECURITY_AUDIT_FOLLOW_UP_2026-07-31_H5.md`](SECURITY_AUDIT_FOLLOW_UP_2026-07-31_H5.md) — closes SEC-008 and SEC-009 at H5 merge `6d82b4e4650294a4a42b9ea2113e64d990e08811`.\n",
)
replace_once(
    readme,
    "- H5 legacy-tool cleanup: implementation and exact-head automated validation are complete in PR #63; SEC-008/SEC-009 remain open only until merge evidence is recorded.",
    "- H5 legacy-tool cleanup: merged as `6d82b4e4650294a4a42b9ea2113e64d990e08811`; SEC-008 and SEC-009 are Closed.",
)
replace_once(
    readme,
    "All inherited non-CI utilities, external-maintenance scripts, raw generator inputs and tool-only masks have now been removed from the H5 branch.",
    "All inherited non-CI utilities, external-maintenance scripts, raw generator inputs and tool-only masks have been removed from the merged default branch.",
)

tracker = "docs/security/STEP_1_TRACKER.md"
replace_once(
    tracker,
    "**Status:** H1–H4 merged; H5 implementation and automated validation complete in PR #63; H1/H2 controlled real-server validation remains",
    "**Status:** H1–H5 merged; repository-side hardening is complete; controlled H1/H2 real-server validation remains",
)
replace_once(
    tracker,
    "**PR:** #63  ",
    "**Merge:** PR #63 — `6d82b4e4650294a4a42b9ea2113e64d990e08811`  ",
)
replace_once(
    tracker,
    "- [ ] Merge PR #63 and record exact squash commit.",
    "- [x] Merge PR #63 and record exact squash commit.",
)
replace_once(
    tracker,
    "- [ ] Close SEC-008 and SEC-009 in a dated audit follow-up.",
    "- [x] Close SEC-008 and SEC-009 in `SECURITY_AUDIT_FOLLOW_UP_2026-07-31_H5.md`.",
)
replace_once(
    tracker,
    "- [ ] Record H5 merge and close SEC-008/SEC-009.",
    "- [x] Record H5 merge and close SEC-008/SEC-009.",
)
replace_once(
    tracker,
    "- [ ] Add combined H1/H2 real-server validation document.",
    "- [x] Add `H1_H2_CONTROLLED_SERVER_VALIDATION.md`; execution evidence remains pending.",
)
replace_once(
    tracker,
    "- [ ] Reconcile `docs/PROJECT_STATE.md` and `docs/CHANGELOG.md` after H5 merge.",
    "- [x] Reconcile `docs/PROJECT_STATE.md` and `docs/CHANGELOG.md` after H5 merge.",
)
