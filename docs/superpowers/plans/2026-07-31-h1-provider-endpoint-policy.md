# H1 Provider Endpoint and Credential Policy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Introduce one validated endpoint/origin boundary for Chat, STT and TTS so credentials and trusted world/session metadata cannot be sent to unsafe, malformed or lookalike destinations.

**Architecture:** Add an immutable `ProviderEndpoint` value and a single `ProviderEndpointPolicy` parser under `livingworld.ai`. Add `ProviderCredentialBinding` to select keys by the validated endpoint family instead of raw provider strings. Route Chat and Audio HTTP construction through these values, disable automatic redirects, and keep persistence/gameplay schemas unchanged.

**Tech Stack:** Java 21, JUnit 5, Gradle multi-project build, `HttpURLConnection`, `java.net.URI`, `java.net.IDN`.

## Global Constraints

- Minecraft target remains `1.21.1` and Java remains `21`.
- Fabric is the primary runtime; NeoForge compile compatibility must not regress.
- No persistent Memory 2.0, relationship, voice-profile or world-data schema changes.
- API credentials remain server-side and must never appear in diagnostics.
- Remote authenticated endpoints require HTTPS.
- Plain HTTP is permitted only for explicit loopback development mode.
- Host trust is derived from normalized URI host identity, never substring matching.
- Automatic redirects are disabled for authenticated Chat, STT and TTS requests.
- Custom STT/TTS endpoints require their dedicated key; they cannot inherit an unrelated main Chat key.
- Existing standard OpenAI and OpenRouter configurations remain compatible.

---

## File Map

**Create**

- `common/src/main/java/net/conczin/mca/livingworld/ai/ProviderEndpoint.java` — immutable validated URI/origin/provider-family value.
- `common/src/main/java/net/conczin/mca/livingworld/ai/ProviderEndpointPolicy.java` — parser and security rules for schemes, hosts and loopback development mode.
- `common/src/main/java/net/conczin/mca/livingworld/ai/ProviderCredentialBinding.java` — endpoint-family-aware Chat/STT/TTS key selection.
- `common/src/test/java/net/conczin/mca/livingworld/ai/ProviderEndpointPolicyTest.java` — URI, hostname, loopback and trusted-host regression tests.
- `common/src/test/java/net/conczin/mca/livingworld/ai/ProviderCredentialBindingTest.java` — key-binding and custom-endpoint fail-closed tests.

**Modify**

- `common/src/main/java/net/conczin/mca/livingworld/LivingWorldConfig.java` — add explicit loopback-development flag and expose validated Chat/STT/TTS endpoint/key bindings.
- `common/src/main/java/net/conczin/mca/livingworld/ai/AiProviderSettings.java` — carry a validated `ProviderEndpoint` rather than an untrusted raw endpoint string.
- `common/src/main/java/net/conczin/mca/livingworld/ai/LivingWorldAI.java` — resolve both LivingWorld and legacy Chat settings through endpoint policy.
- `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java` — use validated endpoint URI, trusted-host property and disabled redirects.
- `common/src/main/java/net/conczin/mca/livingworld/voice/OpenAIAudioProvider.java` — open STT/TTS requests from validated bindings and disable redirects.
- `common/src/main/java/net/conczin/mca/livingworld/voice/SttRequestFormat.java` — delegate OpenRouter classification to normalized endpoint policy and remove substring fallback.
- `common/src/main/java/net/conczin/mca/livingworld/voice/TtsResponseFormat.java` — delegate OpenRouter classification to normalized endpoint policy.
- `common/src/test/java/net/conczin/mca/livingworld/LivingWorldConfigTest.java` — update credential-resolution expectations and configuration compatibility tests.
- `common/src/test/java/net/conczin/mca/livingworld/ai/LivingWorldAITest.java` — update settings construction and trusted-host behavior.
- `common/src/test/java/net/conczin/mca/livingworld/voice/OpenAIAudioProviderTest.java` — enable explicit loopback mode for local test servers and verify redirects are not followed.
- `common/src/test/java/net/conczin/mca/livingworld/voice/SttRequestFormatTest.java` — add malformed/lookalike endpoint assertions.
- `common/src/test/java/net/conczin/mca/livingworld/voice/TtsResponseFormatTest.java` — add malformed/lookalike endpoint assertions.
- `docs/livingworld/CONFIGURATION.md` — document HTTPS policy, loopback flag and dedicated custom audio keys.
- `docs/security/STEP_1_TRACKER.md` — record H1 implementation/CI evidence without marking live validation complete.

---

### Task 1: Endpoint policy RED

**Files:**
- Create: `common/src/test/java/net/conczin/mca/livingworld/ai/ProviderEndpointPolicyTest.java`

**Interfaces:**
- Consumes: planned `ProviderEndpointPolicy.parse(String endpoint, boolean allowInsecureLoopback)`.
- Produces: executable security contract for `ProviderEndpoint`.

- [ ] **Step 1: Add failing tests**

Tests must assert:

```java
ProviderEndpoint openAi = ProviderEndpointPolicy.parse(
        "https://API.OPENAI.COM./v1/chat/completions",
        false
);
assertEquals("api.openai.com", openAi.host());
assertEquals(ProviderEndpoint.Family.OPENAI, openAi.family());

assertThrows(IllegalArgumentException.class, () ->
        ProviderEndpointPolicy.parse("http://example.com/v1/chat", true));
assertThrows(IllegalArgumentException.class, () ->
        ProviderEndpointPolicy.parse("http://127.0.0.1:8080/v1/chat", false));
assertTrue(ProviderEndpointPolicy.parse(
        "http://127.0.0.1:8080/v1/chat",
        true
).loopback());

assertTrue(ProviderEndpointPolicy.parse(
        "https://api.conczin.net/v1/chat",
        false
).trustedConczin());
assertFalse(ProviderEndpointPolicy.parse(
        "https://conczin.net.example.invalid/v1/chat",
        false
).trustedConczin());

assertThrows(IllegalArgumentException.class, () ->
        ProviderEndpointPolicy.parse("https://user:secret@example.com/v1/chat", false));
assertThrows(IllegalArgumentException.class, () ->
        ProviderEndpointPolicy.parse("https://example.com/v1/chat#fragment", false));
assertThrows(IllegalArgumentException.class, () ->
        ProviderEndpointPolicy.parse("ftp://example.com/resource", false));
```

Include `localhost`, `127.0.0.1`, `127.255.255.255`, `::1`, malformed IPv4, mixed case, trailing dot, Unicode/IDN lookalike and OpenRouter exact/lookalike cases.

- [ ] **Step 2: Run RED verification**

Run:

```bash
./gradlew :common:test --tests net.conczin.mca.livingworld.ai.ProviderEndpointPolicyTest --no-daemon
```

Expected: compilation failure because `ProviderEndpoint` and `ProviderEndpointPolicy` do not exist.

- [ ] **Step 3: Record the expected failing CI run**

Record exact commit and CI run IDs in the PR description and tracker as RED evidence.

---

### Task 2: Endpoint policy GREEN

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/ai/ProviderEndpoint.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/ai/ProviderEndpointPolicy.java`

**Interfaces:**
- Produces:
  - `ProviderEndpointPolicy.parse(String, boolean): ProviderEndpoint`
  - `ProviderEndpoint.uri(): URI`
  - `ProviderEndpoint.externalForm(): String`
  - `ProviderEndpoint.host(): String`
  - `ProviderEndpoint.family(): ProviderEndpoint.Family`
  - `ProviderEndpoint.loopback(): boolean`
  - `ProviderEndpoint.trustedConczin(): boolean`

- [ ] **Step 1: Implement immutable endpoint value**

`ProviderEndpoint.Family` must be exactly:

```java
public enum Family {
    OPENAI,
    OPENROUTER,
    CONCZIN,
    CUSTOM
}
```

The canonical URI must use the normalized ASCII lowercase host and preserve scheme, explicit port, raw path and raw query. User info and fragments are forbidden.

- [ ] **Step 2: Implement policy parser**

Rules:

```text
https + valid host                         → accepted
http + explicit loopback + flag true      → accepted
http + any remote host                     → rejected
http + loopback + flag false              → rejected
other schemes                              → rejected
missing host / user info / fragment        → rejected
Unicode host that cannot normalize safely  → rejected
```

Loopback recognition is lexical and DNS-free:

```text
localhost
127.0.0.0/8 with four valid decimal octets
::1
0:0:0:0:0:0:0:1
```

Trusted host classification:

```text
api.openai.com or subdomain boundary       → OPENAI
openrouter.ai or subdomain boundary        → OPENROUTER
conczin.net or subdomain boundary           → CONCZIN + trustedConczin
anything else                              → CUSTOM
```

- [ ] **Step 3: Run targeted GREEN verification**

```bash
./gradlew :common:test --tests net.conczin.mca.livingworld.ai.ProviderEndpointPolicyTest --no-daemon
```

Expected: PASS.

---

### Task 3: Credential binding RED

**Files:**
- Create: `common/src/test/java/net/conczin/mca/livingworld/ai/ProviderCredentialBindingTest.java`
- Modify: `common/src/test/java/net/conczin/mca/livingworld/LivingWorldConfigTest.java`

**Interfaces:**
- Consumes planned endpoint-family-aware binding functions.

- [ ] **Step 1: Add failing key-selection tests**

Required behaviors:

```text
Chat OpenRouter endpoint  → OPENROUTER_API_KEY, then configured apiKey
Chat OpenAI endpoint      → OPENAI_API_KEY, then configured apiKey
Chat custom endpoint      → configured apiKey only; ignore provider environment keys
STT/TTS OpenRouter        → endpoint-specific environment key, dedicated key, compatible main key
STT/TTS OpenAI            → endpoint-specific environment key, dedicated key, compatible main key
STT/TTS custom endpoint   → dedicated key only; never main Chat key
```

Add negative assertions proving:

```java
assertEquals("", ProviderCredentialBinding.resolveAudioKey(
        customEndpoint,
        "",
        "sk-main",
        standardChatEndpoint,
        "sk-openai-env",
        "sk-openrouter-env"
));
```

- [ ] **Step 2: Run RED verification**

```bash
./gradlew :common:test \
  --tests net.conczin.mca.livingworld.ai.ProviderCredentialBindingTest \
  --tests net.conczin.mca.livingworld.LivingWorldConfigTest \
  --no-daemon
```

Expected: compilation/test failure because endpoint-aware credential binding is absent and existing custom audio fallback remains permissive.

---

### Task 4: Credential binding and configuration GREEN

**Files:**
- Create: `common/src/main/java/net/conczin/mca/livingworld/ai/ProviderCredentialBinding.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/LivingWorldConfig.java`

**Interfaces:**
- Produces:
  - `ProviderCredentialBinding.resolveChatKey(...)`
  - `ProviderCredentialBinding.resolveAudioKey(...)`
  - `LivingWorldConfig.chatEndpoint()`
  - `LivingWorldConfig.sttEndpointBinding()`
  - `LivingWorldConfig.ttsEndpointBinding()`

- [ ] **Step 1: Add configuration flag**

Add:

```java
public boolean allowInsecureLoopbackAiEndpoints = false;
```

Do not bump the JSON version. Missing fields remain secure by default.

- [ ] **Step 2: Resolve endpoint and key together**

Expose a small immutable binding record in `ProviderCredentialBinding`:

```java
public record BoundEndpoint(ProviderEndpoint endpoint, String apiKey) {
    public BoundEndpoint {
        Objects.requireNonNull(endpoint, "endpoint");
        apiKey = apiKey == null ? "" : apiKey.trim();
    }
}
```

`LivingWorldConfig.isConfigured()` and voice configured checks must return `false` when endpoint parsing fails or a required key is blank. They must not log secrets or full query strings.

- [ ] **Step 3: Preserve standard provider compatibility**

Standard OpenAI/OpenRouter endpoint families may reuse the compatible main key. Custom STT/TTS endpoints require `sttApiKey`/`ttsApiKey` respectively.

- [ ] **Step 4: Run targeted GREEN verification**

```bash
./gradlew :common:test \
  --tests net.conczin.mca.livingworld.ai.ProviderCredentialBindingTest \
  --tests net.conczin.mca.livingworld.LivingWorldConfigTest \
  --no-daemon
```

Expected: PASS.

---

### Task 5: HTTP and trusted-context RED

**Files:**
- Modify: `common/src/test/java/net/conczin/mca/livingworld/ai/LivingWorldAITest.java`
- Modify: `common/src/test/java/net/conczin/mca/livingworld/voice/OpenAIAudioProviderTest.java`
- Modify: `common/src/test/java/net/conczin/mca/livingworld/voice/SttRequestFormatTest.java`
- Modify: `common/src/test/java/net/conczin/mca/livingworld/voice/TtsResponseFormatTest.java`

**Interfaces:**
- Tests that HTTP callers consume validated endpoint values and do not follow redirects.

- [ ] **Step 1: Add failing tests**

Required tests:

- `AiProviderSettings` accepts `ProviderEndpoint`, not a raw string.
- trusted Conczin context is true only for exact/subdomain boundary.
- legacy `conczin.net.example.invalid` never activates player-name token or trusted metadata behavior.
- local audio tests require `allowInsecureLoopbackAiEndpoints = true`.
- a local 302 endpoint pointing to a second local server is not followed and the second server receives no `Authorization` header.
- malformed OpenRouter text no longer falls back to substring classification.

- [ ] **Step 2: Run RED verification**

```bash
./gradlew :common:test \
  --tests net.conczin.mca.livingworld.ai.LivingWorldAITest \
  --tests net.conczin.mca.livingworld.voice.OpenAIAudioProviderTest \
  --tests net.conczin.mca.livingworld.voice.SttRequestFormatTest \
  --tests net.conczin.mca.livingworld.voice.TtsResponseFormatTest \
  --no-daemon
```

Expected: failure because callers still use raw URLs, substring trust and default redirect behavior.

---

### Task 6: HTTP and trusted-context GREEN

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/livingworld/ai/AiProviderSettings.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/ai/LivingWorldAI.java`
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/voice/OpenAIAudioProvider.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/voice/SttRequestFormat.java`
- Modify: `common/src/main/java/net/conczin/mca/livingworld/voice/TtsResponseFormat.java`

**Interfaces:**
- `AiProviderSettings.endpoint()` returns `ProviderEndpoint`.
- Chat and Audio open connections through `endpoint.uri().toURL()`.

- [ ] **Step 1: Replace raw Chat endpoint usage**

Use:

```java
HttpURLConnection connection = (HttpURLConnection) endpoint.uri().toURL().openConnection();
connection.setInstanceFollowRedirects(false);
```

Use `provider.endpoint().trustedConczin()` for trusted session/world metadata and legacy token behavior.

- [ ] **Step 2: Replace raw Audio endpoint usage**

Resolve `BoundEndpoint` before request construction. Use its endpoint URI and key. Set `setInstanceFollowRedirects(false)` before writing credentials.

- [ ] **Step 3: Remove substring fallback classification**

`SttRequestFormat` and `TtsResponseFormat` must classify only validated/parseable URI hosts. Invalid endpoints return `false` rather than searching raw text.

- [ ] **Step 4: Run targeted GREEN verification**

Run the Task 5 command. Expected: PASS.

---

### Task 7: Full verification and documentation

**Files:**
- Modify: `docs/livingworld/CONFIGURATION.md`
- Modify: `docs/security/STEP_1_TRACKER.md`

- [ ] **Step 1: Run all common tests**

```bash
./gradlew :common:test --no-daemon
```

Expected: PASS with zero test failures.

- [ ] **Step 2: Run supported builds**

```bash
./gradlew :fabric:build :neoforge:build --no-daemon
```

Expected: both builds PASS. If NeoForge is not yet part of normal CI, retain the exact local/CI evidence for H4 planning.

- [ ] **Step 3: Run packaging smoke check**

```bash
./gradlew :fabric:build --no-daemon
./scripts/ci/package-livingworld-release.sh h1-security false dist
```

Expected: distributable JAR and SHA-256 file produced; required JAR entries present.

- [ ] **Step 4: Document operator migration**

Document:

```text
remote providers: HTTPS required
local HTTP providers: set allowInsecureLoopbackAiEndpoints=true
custom STT: sttApiKey required
custom TTS: ttsApiKey required
redirects: not followed
```

- [ ] **Step 5: Update security tracker**

Mark only code/automated-test items supported by exact evidence. Keep H1 live-server validation open until a real server smoke test is performed.

- [ ] **Step 6: Open draft PR**

PR title:

```text
fix: enforce provider endpoint and credential policy
```

PR description must include RED commit/run, GREEN commit/run, compatibility impact, rollback and remaining live-validation boundary.
