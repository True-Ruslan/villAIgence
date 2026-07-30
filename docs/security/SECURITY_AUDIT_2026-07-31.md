# VillAIgence Security and Supply-Chain Audit

**Audit date:** 2026-07-31  
**Repository:** `True-Ruslan/villAIgence`  
**Target branch:** `1.21.1`  
**Target commit:** `c45aea45dd915b24ba236344feef30559c7171bb`  
**Status:** review complete for the inspected boundaries; hardening work remains open

## Executive conclusion

The inspected VillAIgence runtime, persistence, command-execution, provider and release paths contain **no direct evidence of an intentional backdoor, credential harvester, arbitrary command launcher or malicious payload**.

The project does, however, have several real hardening gaps:

- configurable provider endpoints can receive credentials without a strong endpoint trust policy;
- one host classification uses substring matching rather than exact URI-host validation;
- provider responses are read without consistent byte limits;
- an apparently unused URL-verification helper can perform an arbitrary outbound GET;
- Gradle and GitHub Actions supply-chain inputs are not fully pinned and verified;
- the primary CI workflow does not build NeoForge even though compile compatibility is required;
- voice capture duration is not capped to a safe upper bound;
- the repository contains inherited development scripts, including a Python script that sends localization text to a third-party service when manually executed.

These findings justify a dedicated security and supply-chain hardening workstream before broad feature expansion.

## Important correction

An earlier audit statement claimed that the repository contained no Python scripts. That conclusion was incorrect because the review concentrated on the custom fork delta and did not fully account for inherited upstream files.

At minimum, the repository contains:

```text
scripts/pirate_translator.py
```

This correction is part of the permanent audit record. Future reviews must inventory the complete repository tree, not only files changed relative to upstream.

## Scope inspected

The review covered the following security-relevant areas:

- canonical project state, roadmap and release history;
- VillAIgence-specific changes relative to the MCA-derived base;
- OpenAI-compatible Chat, STT and TTS provider configuration and clients;
- provider error handling and structured-response parsing;
- safe action proposal, validation, revalidation and server-thread execution;
- world-local persistent stores and atomic-write behavior;
- voice capture/session resource behavior;
- Gradle settings, dependency repositories, wrapper configuration and plugin versions;
- GitHub Actions CI and release workflows;
- release packaging shell script;
- `scripts/pirate_translator.py`;
- call-site searches for Python execution, shell execution and Java process-launch APIs where repository search was available.

## Coverage limitations

This audit is a source and configuration review, not a penetration test or a guarantee that every inherited byte is safe.

Known limitations:

1. A complete independent recursive checkout was not available during the initial review.
2. GitHub code search for this repository was not consistently indexed.
3. The complete inherited upstream tree was not reviewed file by file.
4. Only directly identified scripts were inspected; a machine-generated recursive inventory remains required.
5. No hostile provider, proxy or network-fault integration test was executed as part of this documentation-only step.
6. No binary reproducibility comparison or dependency SBOM was generated.

Claims in this document are intentionally limited to the inspected evidence. The first hardening work package closes these coverage gaps with a reproducible inventory and automated checks.

## Threat model and protected assets

### Protected assets

- Chat, STT and TTS API credentials;
- Minecraft world seed and server-owned world state;
- player UUIDs, NPC UUIDs and persistent NPC identity;
- conversation, relationship, voice and Memory 2.0 data;
- server availability, heap and tick stability;
- release artifacts and their provenance;
- repository and GitHub Actions write authority.

### Main trust boundaries

```text
server configuration
→ provider endpoint selection
→ outbound HTTP request with credentials/data
→ untrusted provider response
→ parser and policy boundary
→ server-authoritative mutation

source repository
→ Gradle/plugins/dependencies/actions
→ CI runner
→ release JAR
→ server operator
```

### Relevant attacker or failure models

- malicious or compromised provider endpoint;
- lookalike hostname or operator misconfiguration;
- oversized or never-ending HTTP response;
- compromised build dependency, plugin, action tag or wrapper distribution;
- inherited development utility executed without understanding its network behavior;
- accidental regression that bypasses compatibility or security checks.

## Finding summary

| ID | Severity | Finding | Status |
|---|---|---|---|
| SEC-001 | Medium | Custom provider endpoints can receive credentials without a strong endpoint trust policy | Open |
| SEC-002 | Medium | `conczin.net` trust classification uses substring matching | Open |
| SEC-003 | Medium | Chat/STT/TTS response bodies are not consistently size-bounded | Open |
| SEC-004 | Medium-Low | Latent arbitrary-URL GET helper may create an SSRF boundary if activated | Open; no active call site confirmed |
| SEC-005 | Medium | Build and workflow supply-chain inputs are not fully immutable or verified | Open |
| SEC-006 | Medium | Primary CI omits NeoForge build despite compatibility requirement | Open |
| SEC-007 | Low | `voiceMaxSeconds` has no safe upper bound | Open |
| SEC-008 | Low | Legacy Python translator sends localization text to an external service | Open; manual utility only |
| SEC-009 | Process | Previous delta-only review missed inherited scripts | Open until recursive inventory is automated |

## Detailed findings

### SEC-001 — Provider endpoint credential boundary

**Observed**

VillAIgence supports configurable OpenAI-compatible Chat, STT and TTS endpoints. The current configuration permits arbitrary endpoint strings. Custom STT/TTS configuration can also fall back to the main provider key.

The Chat HTTP path attaches an `Authorization: Bearer ...` header to the configured provider request.

**Risk**

A mistaken, copied, compromised or lookalike endpoint can receive an API key and request data. This is principally an operator-misconfiguration and trust-boundary risk rather than a remote exploit against an otherwise protected server configuration.

**Required control**

- parse endpoints with `URI`;
- require HTTPS for non-loopback hosts;
- permit explicit loopback HTTP only through a documented development option;
- never reuse the main provider key for an unrelated custom host by default;
- make dedicated Chat/STT/TTS keys explicit;
- redact user info, query strings and credentials from diagnostics;
- add tests for malformed URIs, user-info injection, redirects and lookalike domains.

### SEC-002 — Substring hostname trust check

**Observed**

`OpenAIChatAI` identifies the in-house endpoint using a string condition equivalent to:

```java
provider.endpoint().contains("conczin.net")
```

That classification controls whether additional identifiers and world context are attached.

**Risk**

A hostname such as `conczin.net.example.invalid` or another URL containing the substring can be misclassified and receive data intended only for the trusted host.

**Required control**

- use `URI.getHost()`;
- normalize the host with IDN-safe handling;
- accept only the exact trusted hostname or an explicitly documented subdomain boundary;
- require HTTPS;
- revalidate the final destination after redirects, or disable redirects for authenticated requests.

### SEC-003 — Unbounded provider response bodies

**Observed**

The inspected Chat response path converts the input stream to a string without a local byte cap. Audio response handling transfers the full stream into memory without a maximum size.

**Risk**

A faulty or malicious endpoint can return a very large or endless response and cause excessive heap usage, long stalls or server termination.

**Required control**

Introduce a shared bounded-response reader with separate limits, for example:

```text
Chat JSON response:       1 MiB maximum
STT JSON response:        512 KiB maximum
TTS audio response:       32 MiB maximum
error response snippet:   64 KiB maximum
```

Exact limits must be justified by supported providers and tests. Enforce both declared `Content-Length` and streaming limits for chunked responses.

### SEC-004 — Latent arbitrary URL verification helper

**Observed**

`OpenAIChatAI.verify(String encodedURL)` can issue a GET to a supplied URL. No active production call site was confirmed during the review.

**Risk**

If this helper becomes reachable with attacker-controlled input, it can create an SSRF primitive against loopback, private networks or cloud metadata endpoints. Its response is also not clearly bounded.

**Required control**

Preferred action: delete the unused helper.

If it is genuinely required:

- accept only explicit HTTPS origins;
- block loopback, link-local, private, multicast and metadata address ranges;
- resolve and revalidate DNS safely;
- disable or revalidate redirects;
- set strict connect/read timeouts;
- apply a small response limit;
- add SSRF regression tests.

### SEC-005 — Supply-chain verification gaps

**Observed**

- Fabric Loom is configured with a snapshot version.
- Gradle wrapper configuration does not record `distributionSha256Sum`.
- Gradle dependency verification metadata is absent.
- dependency locking is not established as a repository-wide invariant;
- several GitHub Actions are referenced by mutable major-version tags rather than immutable commit SHAs;
- some third-party Maven repositories are broader than the minimum required content scope.

**Risk**

Build inputs can drift or be replaced without a reviewable source change. A compromised action tag, plugin snapshot, dependency repository or wrapper distribution can affect CI or release artifacts.

**Required control**

- move to a stable compatible Loom version;
- add the official Gradle wrapper distribution checksum;
- enable and commit Gradle dependency verification metadata;
- enable dependency locking where compatible;
- pin third-party GitHub Actions to full commit SHAs with version comments;
- restrict Maven repositories with content filters;
- generate a dependency inventory or SBOM for release builds;
- validate the wrapper in CI;
- document the controlled procedure for dependency updates.

### SEC-006 — NeoForge compatibility not enforced by primary CI

**Observed**

The project state requires NeoForge compile compatibility, while the primary VillAIgence workflow builds and tests the common/Fabric path but does not enforce a NeoForge build.

**Risk**

Security or compatibility changes can merge while silently breaking a supported compilation target. This weakens release confidence and can encourage emergency unreviewed fixes later.

**Required control**

Add the appropriate NeoForge build task to the required CI matrix and retain Java 21 consistency.

### SEC-007 — Voice capture duration upper bound

**Observed**

Invalid non-positive `voiceMaxSeconds` values are normalized, but no conservative upper bound is enforced.

**Risk**

An extreme operator value can permit excessive per-session PCM buffering and increase aggregate heap pressure under multiplayer use.

**Required control**

- clamp the setting to a documented range, provisionally `1..60` seconds;
- enforce a total active PCM budget across players;
- reject new capture sessions cleanly when the budget is exhausted;
- expose bounded diagnostics without audio content.

### SEC-008 — Legacy Python translation utility

**Observed**

`scripts/pirate_translator.py`:

- reads English localization JSON files;
- sends each localization string to `https://pirate.monkeyness.com/api/translate`;
- performs parallel requests through `tqdm.contrib.concurrent.thread_map`;
- writes translated output locally;
- does not execute shell commands, inspect credentials or install software;
- is not invoked by the inspected Gradle build, release packaging script or GitHub Actions workflows.

The forked copy also lacks robust timeout and response-size controls present in a newer upstream variant.

**Risk**

The script is not a runtime backdoor, but it is an unnecessary external-data transfer and maintenance surface. A developer running it can disclose localization content and depend on an untrusted service response.

**Required control**

Preferred action: remove the script if pirate localization generation is not part of VillAIgence maintenance.

Alternative: move it under an explicitly documented legacy/developer-tools directory and add:

- fixed timeout;
- response byte cap;
- bounded worker count;
- UTF-8-safe file handling;
- explicit dependency lock;
- warning that source text is sent to a third party;
- no CI or release invocation.

### SEC-009 — Audit coverage process gap

**Observed**

The initial review used the VillAIgence fork delta as a strong evidence source. That method missed unchanged inherited files such as `scripts/pirate_translator.py`.

**Risk**

Security conclusions can be overly broad when repository inheritance is not reflected in the audit inventory.

**Required control**

- generate a recursive file manifest for the exact audited commit;
- classify executable/network-capable files by language and purpose;
- record inherited versus VillAIgence-owned origin where practical;
- run secret scanning and dependency inventory against the entire tree;
- store audit scope and limitations with the report.

## Existing controls that reduce risk

The audit also confirmed several sound design decisions:

- the LLM is non-authoritative;
- gameplay actions are selected from a fixed whitelist;
- commands are validated, revalidated and executed on the server thread;
- arbitrary command strings are not accepted from the model;
- structured response parsing is defensive and does not execute returned code;
- retries are designed not to duplicate persistent or gameplay side effects;
- persistent JSON writes use atomic replacement patterns;
- world-local data formats are explicit and inspectable;
- malformed provider or auxiliary responses generally fail soft;
- the primary CI workflow uses read-only repository permissions;
- the release workflow limits write permission to the release job and verifies release-tag conditions;
- the release packaging script verifies expected JAR entries and writes SHA-256 output.

These controls should be preserved while the open findings are fixed.

## Priority decision

### Immediate priority

1. SEC-001 — provider endpoint and credential policy;
2. SEC-002 — exact trusted-host validation;
3. SEC-003 — bounded response bodies;
4. SEC-005 — immutable and verified build inputs;
5. SEC-006 — complete supported build matrix.

### Follow immediately

6. SEC-004 — remove or constrain URL verifier;
7. SEC-007 — voice resource caps;
8. SEC-008/SEC-009 — remove legacy utility where unnecessary and automate complete-tree inventory.

## Closure rule

No finding in this document is considered fixed by documentation, code review or a green unit test alone.

A finding can move to **Closed** only after:

- the implementation is merged;
- targeted negative and regression tests pass;
- full CI passes, including Fabric and NeoForge build coverage;
- build/security verification checks pass;
- runtime-sensitive changes pass a controlled server smoke test;
- this audit or a dated follow-up records the exact closing commit and evidence.

## Next document

Implementation order, tests, rollout and acceptance criteria are defined in:

```text
docs/security/STEP_1_SECURITY_SUPPLY_CHAIN_HARDENING.md
```
