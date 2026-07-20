# LivingWorld Foundation Design

## Goal

Turn the MCA Reborn fork into a stable LivingWorld base without replacing working MCA systems. For the MVP, the server owner should normally configure only one secret (`apiKey`) and get AI villager chat with sensible defaults.

## Decisions

- Keep MCA's existing `common`, `fabric`, and `neoforge` structure.
- Reuse MCA's existing ChatAI context, personality, relationship, village, environment, player, memory, and tool plumbing.
- Add a small LivingWorld configuration/provider layer instead of creating a parallel AI stack.
- MVP provider: OpenAI Chat Completions-compatible API only.
- Keep provider boundaries explicit so STT/TTS/alternative LLM providers can be added later without rewriting villager logic.
- Keep all AI credentials server-side. Never synchronize the API key to clients.
- Preserve legacy MCA ChatAI configuration as a fallback when LivingWorld is not configured.
- If LivingWorld is configured, AI chat is enabled automatically; the owner should not also need to edit `mca.json`.
- Prefer `OPENAI_API_KEY` over the JSON value when present, so production servers can keep secrets out of files.
- Do not add SQLite, vector databases, factions, autonomous agents, or voice dependencies in this foundation PR.

## Configuration

Create `config/livingworld.json` automatically with safe defaults:

```json
{
  "version": 1,
  "enabled": true,
  "apiKey": "",
  "provider": "openai",
  "endpoint": "https://api.openai.com/v1/chat/completions",
  "model": "gpt-4.1-mini",
  "connectTimeoutSeconds": 10,
  "readTimeoutSeconds": 60
}
```

The normal setup path is: start server once, put the API key into `apiKey`, restart. `OPENAI_API_KEY` may be used instead.

## Runtime flow

1. Minecraft server receives player chat.
2. LivingWorld decides whether AI chat is enabled.
3. MCA selects the target villager and builds its existing context.
4. LivingWorld resolves provider endpoint/model/token/timeouts.
5. Existing MCA ChatAI sends the request and applies its existing response/tool handling.
6. If LivingWorld has no key or is disabled, legacy MCA behavior remains available through `mca.json`.

## Boundaries

`LivingWorldConfig` owns persistence and secret resolution.

`AiProviderSettings` is an immutable runtime provider contract.

`LivingWorldAI` decides enablement and resolves LivingWorld-vs-legacy settings.

`OpenAIChatAI` remains responsible for request construction, response parsing, conversation memory, and MCA tool execution, but consumes resolved provider settings rather than reading provider fields directly from MCA config.

## Error handling

- Missing LivingWorld key does not break MCA; legacy MCA configuration is used.
- Invalid LivingWorld JSON logs a clear error and recreates safe defaults.
- HTTP connections have explicit connect/read timeouts.
- Provider error payloads are converted to readable messages where possible.
- API secrets must never be logged.

## Testing

Add focused unit tests for configuration defaults and provider resolution that do not require launching Minecraft. Existing build/loader checks remain the integration safety net.

## Next milestones

After this foundation is stable:

1. Simple Voice Chat server integration and explicit NPC conversation targeting.
2. Server-side STT provider.
3. TTS plus entity-position spatial playback.
4. Persistent world memory/knowledge only after voice MVP proves the interaction loop.
