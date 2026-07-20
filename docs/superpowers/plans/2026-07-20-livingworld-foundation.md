# LivingWorld Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a server-side LivingWorld AI configuration layer that makes an OpenAI API key the only required MVP setting while preserving legacy MCA ChatAI behavior.

**Architecture:** Reuse MCA's existing `OpenAIChatAI` and context pipeline. Add a small immutable provider-settings contract, a LivingWorld config loader, and a resolver that selects LivingWorld settings when configured and otherwise falls back to legacy MCA settings.

**Tech Stack:** Java 21, Minecraft 1.21.1, MCA common module, Gson, JUnit 5, Gradle.

## Global Constraints

- Keep the existing `common`, `fabric`, and `neoforge` module structure.
- API credentials remain server-side and are never synchronized to clients.
- MVP provider is OpenAI-compatible Chat Completions only.
- `config/livingworld.json` requires only `apiKey` for normal setup; other fields have defaults.
- `OPENAI_API_KEY` overrides the file secret when present.
- Legacy `mca.json` ChatAI settings remain a fallback.
- No voice, SQLite, factions, vector database, or autonomous agent scope in this PR.

---

### Task 1: Configuration contract and tests

**Files:**
- Modify: `common/build.gradle`
- Create: `common/src/test/java/net/conczin/mca/livingworld/LivingWorldConfigTest.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/LivingWorldConfig.java`

**Interfaces:**
- Produces: `LivingWorldConfig#getInstance()`, `isConfigured()`, `resolvedApiKey()` and public default provider fields.

- [ ] Add JUnit Jupiter test dependencies and `useJUnitPlatform()`.
- [ ] Write failing tests asserting OpenAI-compatible defaults and that a nonblank API key makes the config configured.
- [ ] Implement JSON load/create, validation, atomic-enough save via normal file replacement, and environment key override.
- [ ] Run `./gradlew :common:test` and verify green.

### Task 2: Provider settings resolver

**Files:**
- Create: `common/src/test/java/net/conczin/mca/livingworld/ai/LivingWorldAITest.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/ai/AiProviderSettings.java`
- Create: `common/src/main/java/net/conczin/mca/livingworld/ai/LivingWorldAI.java`

**Interfaces:**
- Produces: `AiProviderSettings(endpoint, model, apiKey, connectTimeoutMillis, readTimeoutMillis)`.
- Produces: `LivingWorldAI.isChatEnabled()` and `LivingWorldAI.resolveChatProviderSettings()`.

- [ ] Write failing tests for LivingWorld-first selection and legacy fallback selection using pure resolver inputs.
- [ ] Implement immutable provider settings with validated timeout conversion.
- [ ] Implement resolver logic: configured LivingWorld wins; otherwise legacy MCA fields are returned.
- [ ] Ensure no method logs or exposes the API key.
- [ ] Run focused tests and full common tests.

### Task 3: Wire ChatAI to LivingWorld provider settings

**Files:**
- Modify: `common/src/main/java/net/conczin/mca/entity/ai/chatAI/OpenAIChatAI.java`
- Modify: `common/src/main/java/net/conczin/mca/mixin/MixinServerGamePacketListenerImpl.java`

**Interfaces:**
- Consumes: `LivingWorldAI.isChatEnabled()` and `LivingWorldAI.resolveChatProviderSettings()`.

- [ ] Add a failing resolver/integration-facing test where practical without Minecraft bootstrap.
- [ ] Replace the chat gate with `LivingWorldAI.isChatEnabled()`.
- [ ] Resolve endpoint/model/token/timeouts once per answer request and use those settings for HTTP.
- [ ] Add explicit connection and read timeouts.
- [ ] Preserve conczin.net-specific legacy behavior only when the legacy fallback is active.
- [ ] Improve provider error parsing without leaking request credentials.
- [ ] Run common tests and compile tasks.

### Task 4: Operator documentation and verification

**Files:**
- Create: `docs/livingworld/CONFIGURATION.md`

- [ ] Document first-run setup: start once, edit only `apiKey`, restart.
- [ ] Document `OPENAI_API_KEY` as the preferred secret override for managed servers.
- [ ] Document fallback behavior and that players need no API keys.
- [ ] Run `./gradlew :common:test :common:compileJava` plus Fabric compile/build checks available in CI.
- [ ] Review diff for secret leakage, upstream compatibility, and unrelated changes.
- [ ] Open PR against `1.21.1` with validation status and follow-up voice milestone explicitly out of scope.
