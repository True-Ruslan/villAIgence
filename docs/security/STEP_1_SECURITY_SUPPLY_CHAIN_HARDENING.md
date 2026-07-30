# Step 1 — Security and Supply-Chain Hardening

**Status:** planned  
**Priority:** immediate reliability/security interruption before broad feature expansion  
**Audit source:** `docs/security/SECURITY_AUDIT_2026-07-31.md`  
**Planning baseline:** branch `1.21.1`, commit `c45aea45dd915b24ba236344feef30559c7171bb`

## Goal

Strengthen the boundaries that protect credentials, server availability, release integrity and supported build compatibility without changing VillAIgence gameplay semantics or persistent Memory 2.0 schemas.

The work should leave the project with:

- explicit provider endpoint trust rules;
- bounded outbound-network response handling;
- no latent arbitrary-URL helper;
- immutable and verified build inputs;
- complete Fabric and NeoForge CI coverage;
- automated whole-tree script, secret and dependency inventory;
- no unexplained legacy network utilities in normal project scope.

## Why this is Step 1

Security, reliability and data-integrity fixes are allowed to interrupt any product milestone. The current foundation is already substantial enough that provider credentials, persistent world data and release artifacts deserve stronger preventive controls before Personality, NPC-to-NPC social state or larger autonomous systems expand the attack surface.

This work does **not** replace the pending live validation of deterministic Semantic Memory forgetting/decay. That remains a separate release-validation obligation. Security hardening should be completed before the next major product milestone and followed by its own runtime smoke validation.

## Security invariants

The implementation must make the following properties testable:

1. A credential is never sent to a host that has not been explicitly selected for that credential.
2. Authenticated provider traffic uses HTTPS except for an explicit loopback-only development mode.
3. Trusted-host behavior is based on normalized URI host identity, never substring matching.
4. Redirects cannot silently move authenticated traffic or trusted-context data to another origin.
5. Every provider response has a deterministic byte limit independent of `Content-Length`.
6. No production helper can fetch an arbitrary URL without an explicit allowlist and private-address protection.
7. Voice capture has both per-session and aggregate memory limits.
8. CI and release dependencies are pinned or cryptographically verified.
9. The supported Fabric and NeoForge compilation targets are enforced by CI.
10. The complete repository tree is inventoried; inherited scripts are never omitted from audit conclusions.
11. No Python or shell utility is executed by build, CI or release flows unless it is documented, reviewed and intentionally required.
12. Hardening failures fail closed for credentials and unsafe network destinations, but fail soft for gameplay availability where no mutation has occurred.

## Non-goals

This workstream will not:

- redesign Memory 2.0 or change persistent JSON schemas;
- add embeddings, a vector database or LLM-based trust classification;
- redesign voice identity or TTS profiles;
- add Personality or NPC-to-NPC social simulation;
- replace all OpenAI-compatible provider support with a single vendor;
- introduce a general plugin sandbox;
- claim binary reproducibility before it is actually measured.

## Delivery strategy

The work is intentionally split into focused pull requests. Each PR must be independently reviewable, have targeted tests and preserve rollback options.

```text
Planning PR
    ↓
H1 Provider endpoint and credential policy
    ↓
H2 Bounded network I/O and SSRF removal
    ↓
H3 Supply-chain verification and dependency controls
    ↓
H4 CI security coverage and complete build matrix
    ↓
H5 Legacy tools cleanup and audit closure
    ↓
Controlled server smoke validation
    ↓
Update PROJECT_STATE / CHANGELOG / security audit status
```

## Planning PR — Audit and implementation contract

### Scope

- add the dated security audit;
- record the correction concerning inherited Python scripts;
- define findings, limitations and closure rules;
- define this ordered implementation plan;
- create a GitHub tracking issue for the implementation work.

### Acceptance criteria

- documents are internally consistent;
- target branch and commit are explicit;
- observed, inferred and proposed claims remain distinguishable;
- no finding is marked fixed;
- the next implementation PR is unambiguous.

## H1 — Provider endpoint and credential policy

### Purpose

Centralize endpoint parsing, origin trust and credential selection so every Chat, STT and TTS request uses the same enforceable security boundary.

### Proposed design

Create a small server-owned endpoint policy abstraction, provisionally:

```text
ProviderEndpointPolicy
ProviderEndpoint
ProviderCredentialBinding
```

`ProviderEndpoint` should contain a validated normalized URI and derived origin identity rather than passing raw strings through request code.

### Required behavior

- parse with `java.net.URI`;
- reject missing scheme, host, fragments and user info;
- allow only `https` for non-loopback destinations;
- allow `http` only for explicit loopback development destinations such as `127.0.0.1`, `::1` and a carefully validated `localhost`;
- treat Chat, STT and TTS credentials as separate bindings;
- do not fall back from a custom endpoint to an unrelated main API key;
- replace `contains("conczin.net")` with exact normalized-host rules;
- disable redirects for authenticated provider calls, or revalidate every redirect target and strip authorization on origin change;
- avoid logging query strings, credentials or authorization headers.

### Tests

At minimum:

- valid OpenRouter/OpenAI HTTPS endpoint;
- valid explicit loopback HTTP endpoint;
- remote HTTP rejected;
- `conczin.net` accepted only under the documented exact/subdomain rule;
- `conczin.net.example.invalid` rejected as trusted;
- mixed-case and trailing-dot normalization behavior;
- Unicode/IDN lookalike behavior;
- URI with user info rejected;
- malformed URI rejected;
- custom STT/TTS endpoint without dedicated key fails closed;
- redirect to another origin cannot receive authorization or trusted metadata.

### Compatibility

Existing standard OpenRouter/OpenAI configuration should continue to work without changes. Custom local endpoints may require an explicit development flag or documented migration.

### Rollback

The policy should be isolated behind one provider-construction boundary so the PR can be reverted without touching persistence or gameplay schemas.

## H2 — Bounded network I/O and SSRF removal

### Purpose

Prevent untrusted or faulty providers from consuming unbounded heap/time and remove dormant arbitrary-network authority.

### Required changes

- introduce a shared bounded byte/string response reader;
- enforce limits while streaming, not only through `Content-Length`;
- define separate Chat, STT, TTS and error-body limits;
- reject oversized payloads with controlled provider diagnostics;
- preserve text fallback when TTS is rejected;
- ensure streams and connections are always closed;
- delete `OpenAIChatAI.verify(String encodedURL)` if unused;
- otherwise replace it with a narrowly scoped allowlisted client that blocks private, loopback, link-local, multicast and metadata targets;
- set explicit connect, response and total-operation timeouts;
- clamp `voiceMaxSeconds` to a documented safe range;
- introduce an aggregate active PCM budget across players.

### Initial limit proposal

These values are starting points and must be validated against supported providers:

```text
Chat JSON response:       1 MiB
STT JSON response:        512 KiB
TTS audio response:       32 MiB
error response snippet:   64 KiB
voice capture:            1..60 seconds per session
```

### Tests

- declared oversized `Content-Length` rejected before full read;
- chunked response crossing limit rejected during streaming;
- exact-limit response accepted;
- endless/slow response terminates by timeout;
- oversized TTS preserves valid text reply;
- oversized Chat/STT produces controlled fallback without persistence side effects;
- SSRF helper has no call site or private-address targets are rejected;
- extreme `voiceMaxSeconds` is clamped or rejected;
- aggregate voice budget prevents unbounded concurrent buffering;
- retries do not repeat accepted actions or persistence mutations.

### Rollback

Limits and timeouts must be constants/configuration with conservative defaults. Rollback should not require data migration.

## H3 — Supply-chain verification and dependency controls

### Purpose

Make build inputs reviewable, reproducible enough for controlled releases and resistant to mutable upstream references.

### Required changes

1. Replace the Loom snapshot with a stable compatible release.
2. Add the official `distributionSha256Sum` to `gradle-wrapper.properties`.
3. Add Gradle wrapper validation to CI.
4. Generate and commit Gradle dependency verification metadata.
5. Enable dependency locking where compatible with the multi-loader build.
6. Restrict third-party Maven repositories using exclusive content or content filters.
7. Pin every third-party GitHub Action to a full commit SHA and retain a version comment.
8. Generate a dependency manifest or SBOM for release artifacts.
9. Document the dependency-update procedure and expected metadata refresh.
10. Preserve the existing release JAR content checks and SHA-256 output.

### Tests and verification

- clean build from a fresh dependency cache;
- dependency verification rejects an altered artifact;
- Gradle wrapper checksum validation passes;
- dependency locks are stable across repeated builds;
- Fabric and NeoForge both resolve using the restricted repositories;
- workflow action references contain full immutable SHAs;
- release artifact and SBOM are produced together;
- existing JAR smoke checks still pass.

### Migration risk

Dependency verification and repository filters can initially reveal undocumented transitive sources or metadata differences. Introduce them in a dedicated PR and resolve each exception explicitly rather than disabling verification broadly.

### Rollback

Keep the previous stable build configuration visible in PR history. Do not combine this PR with runtime provider changes.

## H4 — CI security coverage and complete build matrix

### Purpose

Turn the security invariants into required gates and enforce every supported compilation target.

### Required changes

- add NeoForge build coverage to the primary CI matrix;
- retain common tests and Fabric build/package checks;
- run Gradle wrapper validation;
- run dependency verification/locking checks;
- add secret scanning for the complete repository history or at least every PR diff plus current tree;
- add static analysis appropriate for Java 21 and Gradle;
- add a repository script inventory check covering `.py`, `.sh`, `.ps1`, `.bat`, executable files and workflow invocations;
- ensure CI permissions remain least-privilege;
- keep release write permission isolated to the release job;
- fail when undocumented executable/network utilities are newly added;
- publish security scan output as non-secret CI artifacts where useful.

### Candidate checks

Selection should favor maintained, pinned and low-noise tools. Candidates include:

- CodeQL for Java/Kotlin and GitHub Actions;
- Gitleaks or equivalent secret scanning;
- Gradle wrapper validation;
- dependency verification and lock consistency;
- custom deterministic script/workflow inventory;
- optional dependency-review action for pull requests.

Every selected GitHub Action must itself be pinned by full SHA.

### Tests

- intentional NeoForge compilation break fails CI;
- altered wrapper checksum fails CI;
- dependency metadata drift fails CI;
- synthetic secret fixture in a test-only branch demonstrates scanner detection without committing a real secret;
- new undocumented Python script or workflow invocation fails the inventory policy;
- normal documentation-only PR remains reasonably fast.

### Performance constraint

Security checks must not make the normal pull-request loop unusable. Independent checks should run in parallel, while release-only SBOM/provenance work may remain in release jobs.

## H5 — Legacy tools cleanup and audit closure

### Purpose

Eliminate unexplained inherited utilities and close the original audit coverage gap.

### Required changes

- generate a recursive manifest for the exact repository commit;
- classify every executable or network-capable script;
- determine whether `scripts/pirate_translator.py` is required by VillAIgence maintenance;
- preferred result: remove the translator and its unused Python dependency surface;
- if retained, move it to an explicit developer-tools/legacy location and harden it with timeout, response limits, bounded concurrency, UTF-8 handling and a third-party disclosure warning;
- confirm no build, CI or release path invokes Python unexpectedly;
- document every retained script in a script inventory;
- rerun secret and dependency scans against the complete tree;
- update the dated audit with exact closing commits and residual risks.

### Tests

- recursive inventory is deterministic;
- workflow/build invocation graph contains only approved scripts;
- removed utility has no references;
- retained utility has isolated tests with mocked network access;
- CI blocks reintroduction of an undocumented executable utility.

## Cross-PR testing matrix

| Area | Unit | Integration | CI/build | Live server |
|---|---:|---:|---:|---:|
| URI and host policy | Required | Required with local test server | Required | Smoke |
| Credential binding | Required | Required | Required | Smoke without exposing secrets |
| Response byte limits | Required | Required with hostile responses | Required | Smoke |
| Redirect handling | Required | Required | Required | Optional smoke |
| Voice memory limits | Required | Required | Required | Multiplayer smoke |
| Wrapper/dependency verification | N/A | Fresh-cache build | Required | N/A |
| Fabric/NeoForge compatibility | N/A | N/A | Required | Fabric server smoke |
| Script inventory and secret scan | Required for scripts | N/A | Required | N/A |

## Controlled live validation

After H1–H5 merge, prepare a release candidate and validate on a real Minecraft 1.21.1 server.

### Required checks

1. Existing OpenRouter/OpenAI configuration starts successfully.
2. Invalid remote HTTP and lookalike endpoints fail with concise redacted diagnostics.
3. Explicit loopback development endpoint works only when enabled.
4. Text Chat succeeds and persists exactly once.
5. STT and TTS succeed with supported payload sizes.
6. TTS failure still preserves the text reply.
7. Voice capture limits behave correctly under concurrent sessions.
8. No secrets, prompts, transcripts or authorization data appear in logs.
9. Memory, relationships and voices remain byte-stable across restart when no semantic change is expected.
10. Fabric runtime remains healthy; NeoForge compile gate is green.
11. CI-produced release JAR, checksum and dependency inventory match the tested artifact.
12. No VillAIgence persistence, security-policy or OutOfMemory errors occur.

The validation must record the exact tag, commit, CI runs, configuration mode and artifact checksum in a dated document.

## Definition of done

Step 1 is complete only when:

- SEC-001 through SEC-009 have explicit final states;
- all selected implementation PRs are merged;
- targeted negative tests exist and pass;
- common, Fabric and NeoForge build gates are green;
- actions and wrapper inputs are immutable or checksum-verified;
- dependency verification and inventory are committed and enforced;
- provider response bodies and voice buffers are bounded;
- the arbitrary-URL helper is removed or strongly constrained;
- no unexplained Python/network utility remains;
- a controlled server smoke validation passes;
- `docs/PROJECT_STATE.md`, `docs/CHANGELOG.md` and the security audit are updated with exact evidence.

## Recommended immediate implementation

Begin with **H1 — Provider endpoint and credential policy**.

It addresses the highest-value trust boundary, provides the URI/origin model needed by redirect and SSRF controls, and can be implemented without touching persistent data or gameplay behavior.

H2 should follow immediately because the same provider-client boundary is the correct ownership point for response limits and timeout behavior.

Do not combine H1 with supply-chain changes. Keeping runtime security and build-system security in separate PRs makes review, regression diagnosis and rollback substantially safer.
