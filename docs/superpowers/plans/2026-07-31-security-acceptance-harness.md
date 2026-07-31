# Security Acceptance Harness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a loopback hostile-provider server and exact-JAR verification/PCM probes that prepare deterministic closure testing for SEC-003, SEC-004 and SEC-007.

**Architecture:** A standard-library Python HTTP server generates bounded hostile response patterns and sanitized JSONL evidence. Two JDK-only Java main classes exercise the production verification transport and PCM budget directly from the built VillAIgence JAR. Normal Minecraft runtime remains unchanged because every tool requires explicit operator invocation.

**Tech Stack:** Python 3 standard library, Java 21, JUnit 5, Gradle, `HttpURLConnection`, `ThreadingHTTPServer`.

## Global Constraints

- Bind network test services only to literal loopback addresses.
- Do not contact OpenRouter, OpenAI or any production provider from tests.
- Add no third-party dependency.
- Never log credential values, Authorization values, query values, prompts, transcripts or request bodies.
- Preserve configuration version 2 and every persistent world format.
- Do not add an in-game command or automatic startup hook.
- Keep SEC-003, SEC-004 and SEC-007 open until a later real-server acceptance record exists.

---

### Task 1: RED acceptance contracts

**Files:**
- Create: `scripts/ci/test_provider_acceptance_harness.py`
- Create: `common/src/test/java/net/conczin/mca/livingworld/ai/AccountVerificationAcceptanceProbeTest.java`
- Create: `common/src/test/java/net/conczin/mca/livingworld/voice/VoicePcmBudgetAcceptanceProbeTest.java`
- Modify: `.github/workflows/security-policy.yml`
- Modify: `docs/security/APPROVED_SCRIPT_INVENTORY.json`

**Interfaces:**
- Consumes: existing `AccountVerificationClient`, `VoicePcmBudget`, `VoiceCaptureLimits`.
- Produces: failing contracts for `provider_acceptance_harness`, `AccountVerificationAcceptanceProbe` and `VoicePcmBudgetAcceptanceProbe`.

- [ ] **Step 1: Write Python tests for loopback enforcement, declared/chunked/error/redirect/slow-drip behavior and sanitized evidence.**

- [ ] **Step 2: Write Java tests requiring package-private `AccountVerificationClient.execute(URI, int, int)` and safe probe URI validation.**

- [ ] **Step 3: Write Java tests requiring `VoicePcmBudgetAcceptanceProbe.run(long, int, long)` and exact recovery evidence.**

- [ ] **Step 4: Run the Python and Gradle tests.**

Expected: Python import failure and Java compilation failure because production APIs do not exist.

- [ ] **Step 5: Commit RED evidence.**

```bash
git add scripts/ci/test_provider_acceptance_harness.py \
  common/src/test/java/net/conczin/mca/livingworld/ai/AccountVerificationAcceptanceProbeTest.java \
  common/src/test/java/net/conczin/mca/livingworld/voice/VoicePcmBudgetAcceptanceProbeTest.java \
  .github/workflows/security-policy.yml \
  docs/security/APPROVED_SCRIPT_INVENTORY.json
git commit -m "test: define security acceptance harness contracts"
```

### Task 2: Loopback provider harness

**Files:**
- Create: `scripts/security/provider_acceptance_harness.py`
- Modify: `scripts/ci/test_provider_acceptance_harness.py`
- Modify: `docs/security/APPROVED_SCRIPT_INVENTORY.json`

**Interfaces:**
- Produces: `HarnessConfig`, `ProviderAcceptanceServer`, `validate_loopback_bind`, `summarize_evidence`, and CLI subcommands `serve` and `summarize`.

- [ ] **Step 1: Implement literal-loopback validation using `ipaddress.ip_address`.**

- [ ] **Step 2: Implement deterministic route parsing for Chat, STT, TTS, provider-error and verification scenarios.**

- [ ] **Step 3: Implement declared-length, chunked, error, redirect and slow-drip response writers without full-payload allocation.**

- [ ] **Step 4: Implement sanitized JSONL request evidence and startup manifest output.**

- [ ] **Step 5: Run Python tests.**

```bash
python3 scripts/ci/test_provider_acceptance_harness.py
```

Expected: PASS.

- [ ] **Step 6: Commit.**

```bash
git add scripts/security/provider_acceptance_harness.py \
  scripts/ci/test_provider_acceptance_harness.py \
  docs/security/APPROVED_SCRIPT_INVENTORY.json
git commit -m "feat: add loopback provider acceptance harness"
```

### Task 3: Verification transport and exact-JAR probe

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/ai/AccountVerificationClient.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/ai/AccountVerificationAcceptanceProbe.java`
- Modify: `common/src/test/java/net/conczin/mca/livingworld/ai/AccountVerificationAcceptanceProbeTest.java`
- Modify: `common/src/test/java/net/conczin/mca/livingworld/ai/AccountVerificationClientTest.java`

**Interfaces:**
- Produces: package-private `AccountVerificationClient.execute(URI, int, int)` and `AccountVerificationAcceptanceProbe.validateLoopbackUri(String)`.

- [ ] **Step 1: Extract the existing HTTP execution body into `execute`, preserving disabled redirects, timeouts, limits and result parsing.**

- [ ] **Step 2: Keep public `verify` responsible for exact Conczin HTTPS policy and fixed-path construction.**

- [ ] **Step 3: Implement a main probe that accepts only literal loopback HTTP/HTTPS URI without user-info or fragment.**

- [ ] **Step 4: Print one deterministic JSON result and use nonzero exit for unsafe arguments.**

- [ ] **Step 5: Run targeted tests.**

```bash
./gradlew --no-daemon :common:test --tests '*AccountVerification*'
```

Expected: PASS.

- [ ] **Step 6: Commit.**

```bash
git add common/src/main/java/net/conczin/mca/livingworld/ai/AccountVerificationClient.java \
  common/src/main/java/net/conczin/mca/livingworld/ai/AccountVerificationAcceptanceProbe.java \
  common/src/test/java/net/conczin/mca/livingworld/ai/AccountVerificationAcceptanceProbeTest.java \
  common/src/test/java/net/conczin/mca/livingworld/ai/AccountVerificationClientTest.java
git commit -m "feat: add verification acceptance probe"
```

### Task 4: Exact-JAR PCM budget probe

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/voice/VoicePcmBudgetAcceptanceProbe.java`
- Modify: `common/src/test/java/net/conczin/mca/livingworld/voice/VoicePcmBudgetAcceptanceProbeTest.java`

**Interfaces:**
- Produces: `VoicePcmBudgetAcceptanceProbe.Result` and `run(long maxBytes, int workers, long reservationBytes)`.

- [ ] **Step 1: Implement synchronized concurrent start, reservation counting and peak observation.**

- [ ] **Step 2: Release every accepted reservation in `finally` and verify final usage is zero.**

- [ ] **Step 3: Prove a full-budget reservation succeeds after the concurrent phase.**

- [ ] **Step 4: Print deterministic JSON marker `VILLAIGENCE_PCM_PROBE_PASS`.**

- [ ] **Step 5: Run targeted tests.**

```bash
./gradlew --no-daemon :common:test --tests '*VoicePcmBudgetAcceptanceProbeTest'
```

Expected: PASS.

- [ ] **Step 6: Commit.**

```bash
git add common/src/main/java/net/conczin/mca/livingworld/voice/VoicePcmBudgetAcceptanceProbe.java \
  common/src/test/java/net/conczin/mca/livingworld/voice/VoicePcmBudgetAcceptanceProbeTest.java
git commit -m "feat: add PCM budget acceptance probe"
```

### Task 5: Operator documentation and policy reconciliation

**Files:**
- Create: `docs/security/LOCAL_SECURITY_ACCEPTANCE_HARNESS.md`
- Modify: `docs/security/H1_H2_CONTROLLED_SERVER_VALIDATION.md`
- Modify: `docs/security/STEP_1_TRACKER.md`
- Modify: `docs/security/README.md`
- Modify: `docs/PROJECT_STATE.md`
- Modify: `docs/CHANGELOG.md`
- Modify: `.github/workflows/security-policy.yml`
- Modify: `docs/security/APPROVED_SCRIPT_INVENTORY.json`

**Interfaces:**
- Produces exact commands for server setup, config backup/restore, hostile route selection, JAR probes, hash comparison and evidence retention.

- [ ] **Step 1: Document build/JAR identity and rollback requirements.**

- [ ] **Step 2: Document loopback server startup and every endpoint path.**

- [ ] **Step 3: Document exact `java -cp` verification and PCM probe commands.**

- [ ] **Step 4: Document that production provider credentials must be removed from the temporary custom loopback stages and restored byte-for-byte afterward.**

- [ ] **Step 5: Update the script inventory from five historical H5 launchers to seven currently approved scripts, preserving H5 historical evidence unchanged.**

- [ ] **Step 6: Keep all three findings open and mark tooling prepared.**

- [ ] **Step 7: Commit.**

```bash
git add docs .github/workflows/security-policy.yml
git commit -m "docs: add local security acceptance procedure"
```

### Task 6: Full validation and PR

**Files:**
- Verify all changed files.

- [ ] **Step 1: Run harness tests and repository policy.**

```bash
python3 scripts/ci/test_provider_acceptance_harness.py
python3 scripts/ci/repository_security_policy.py --check
```

- [ ] **Step 2: Run common tests and both loaders.**

```bash
./gradlew --no-daemon :common:test :fabric:build :neoforge:build
```

- [ ] **Step 3: Run release package verification.**

```bash
scripts/ci/package-livingworld-release.sh
```

- [ ] **Step 4: Confirm no test or probe runs automatically during Minecraft startup.**

- [ ] **Step 5: Open a draft PR with RED/GREEN anchors, exact head, CI runs, script inventory and residual live-validation boundary.**
