# Provider endpoint and credential security

VillAIgence validates Chat, STT and TTS destinations before an API credential or provider payload is attached to an HTTP request.

This policy protects server-side credentials and trusted world/session metadata from malformed, remote plaintext and lookalike endpoints.

## Secure defaults

```json
{
  "allowInsecureLoopbackAiEndpoints": false
}
```

The configuration version remains `2`. Existing version-2 files require no data migration. A missing `allowInsecureLoopbackAiEndpoints` field is interpreted as `false`.

## Allowed endpoint schemes

Remote provider endpoints must use HTTPS:

```text
https://api.openai.com/...
https://openrouter.ai/...
https://provider.example/...
```

Plain HTTP is rejected for every remote hostname, including private-LAN addresses.

Plain HTTP can be enabled only for explicit loopback development endpoints:

```json
{
  "allowInsecureLoopbackAiEndpoints": true,
  "endpoint": "http://127.0.0.1:11434/v1/chat/completions"
}
```

Accepted lexical loopback forms are:

```text
localhost
127.0.0.0/8 with valid decimal IPv4 octets
::1
0:0:0:0:0:0:0:1
```

The flag does not permit remote HTTP, private-LAN HTTP or hostname lookalikes.

## URI validation

Before use, VillAIgence:

- parses the endpoint with `java.net.URI`;
- accepts only HTTPS, or explicit loopback HTTP development mode;
- requires a host;
- rejects URI user information such as `https://user:password@host/...`;
- rejects fragments;
- normalizes host case, trailing dots and IDN representation;
- classifies providers by normalized host boundaries rather than raw-string substring matching.

A hostname such as:

```text
conczin.net.example.invalid
openrouter.ai.example.invalid
```

is treated as a custom provider and never inherits trusted-host behavior.

## Credential binding

Credentials are selected only after endpoint validation and provider-family classification.

### Chat

For an OpenAI Chat endpoint:

1. `OPENAI_API_KEY`;
2. `apiKey`.

For an OpenRouter Chat endpoint:

1. `OPENROUTER_API_KEY`;
2. `apiKey`.

For another custom Chat endpoint:

1. `apiKey` only.

Provider environment keys are not sent to an unrelated custom Chat host.

### STT

For an OpenAI STT endpoint:

1. `OPENAI_API_KEY`;
2. `sttApiKey`;
3. the main Chat key only when the validated main Chat endpoint is also OpenAI.

For an OpenRouter STT endpoint:

1. `OPENROUTER_API_KEY`;
2. `sttApiKey`;
3. the main Chat key only when the validated main Chat endpoint is also OpenRouter.

For another custom STT endpoint:

1. `sttApiKey` only.

### TTS

For an OpenAI TTS endpoint:

1. `OPENAI_API_KEY`;
2. `ttsApiKey`;
3. the main Chat key only when the validated main Chat endpoint is also OpenAI.

For an OpenRouter TTS endpoint:

1. `OPENROUTER_API_KEY`;
2. `ttsApiKey`;
3. the main Chat key only when the validated main Chat endpoint is also OpenRouter.

For another custom TTS endpoint:

1. `ttsApiKey` only.

Custom audio endpoints therefore require their own dedicated credentials. They cannot silently inherit an unrelated main Chat key.

## Redirect behavior

Authenticated Chat, STT and TTS requests do not automatically follow HTTP redirects.

A `3xx` response is treated as a provider failure. VillAIgence does not forward the `Authorization` header or trusted provider metadata to the redirect target.

Operators must configure the final provider endpoint directly.

## Trusted Conczin metadata boundary

World seed, player UUID, NPC UUID and legacy in-house session metadata are enabled automatically only for the exact normalized `conczin.net` host boundary and its real subdomains.

Raw URL text containing the words `conczin.net` is not sufficient.

The general operator option `villagerChatAIIncludeSessionInformation` remains an explicit separate choice.

## Failure behavior

An invalid or unsafe endpoint fails closed before provider credentials are attached.

Expected operator-visible outcomes include:

- Chat configuration reported as `MISCONFIGURED`;
- STT or TTS stage reported as unavailable when its required dedicated key is absent;
- concise diagnostics without API keys, authorization headers or URI user information;
- no persistent or gameplay mutation caused by the rejected provider request.

## Local provider example

```json
{
  "provider": "openai",
  "allowInsecureLoopbackAiEndpoints": true,
  "endpoint": "http://127.0.0.1:11434/v1/chat/completions",
  "model": "local-model",
  "apiKey": "local-placeholder",

  "sttEndpoint": "http://127.0.0.1:9000/v1/audio/transcriptions",
  "sttApiKey": "local-stt-placeholder",

  "ttsEndpoint": "http://127.0.0.1:9001/v1/audio/speech",
  "ttsApiKey": "local-tts-placeholder"
}
```

Use loopback HTTP only for software running on the same server host. Prefer HTTPS even for internal network services that are not loopback.

## Scope boundary

This hardening step validates destinations, binds credentials and blocks authenticated redirects.

Response-body byte limits, aggregate PCM budgets and removal or restriction of the legacy arbitrary-URL verification helper belong to Step 1 H2 and are tracked separately.
