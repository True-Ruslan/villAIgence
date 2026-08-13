# VillAIgence 0.3.0 Clean-World Installed Validation

Status: **INSTALLED / OPERATIONAL / NOT INSTALLED-ACCEPTED — ONE REQUIRED MANUAL CANARY FAILED**

## Validation boundary

- Release: `0.3.0+1.21.1`
- Artifact: `villaigence-fabric-0.3.0+1.21.1.jar`
- SHA-256: `854bc5d7252a9e25cdcfbcbac3ceea27695e0f4db5d3617422f77b62ccb76c27`
- Release commit: `d42141511c0c61f10256fd06576f977f2a784d1c`
- GitHub Release workflow: run `31594937796` / run number `854`
- Release asset id: `511533950`
- Validation date: `2026-08-12` (`Europe/Moscow`)
- Server: private LinuxGSM Fabric server
- Minecraft: `1.21.1`
- Fabric Loader: `0.19.3`
- Java: Temurin `21.0.11`
- Fabric API: `0.116.14+1.21.1`
- Simple Voice Chat: `1.21.1-2.6.20`

This report covers only installed, clean-world evidence that cannot be established reliably by CI. It does not repeat automated GameTests, package verification, destructive persistence recovery, production soak, or byte-identity testing.

## Installation evidence

The official GitHub Release asset was downloaded and verified locally before upload. The active server JAR produced the expected SHA-256 after installation.

The previous state was preserved through a reversible cutover:

- Recovery backup: `/home/server/minecraft/backups/pre-villaigence-0.3.0-20260812-153253/`
- Previous LivingWorld state: `world/livingworld.pre-0.3.20260812-153253`
- Previous `0.2.0+1.21.1` JAR retained in the recovery backup

Only these active mod JARs were present after cutover:

- `fabric-api-0.116.14+1.21.1.jar`
- `villaigence-fabric-0.3.0+1.21.1.jar`
- `voicechat-fabric-1.21.1-2.6.20.jar`

The original MCA Reborn JAR was not installed separately.

## Gate results

### Gate A — clean startup: PASS

- Fabric loaded Minecraft `1.21.1` and VillAIgence internal mod ID `mca` version `0.3.0+1.21.1`.
- The server reached `Done` without a crash.
- Java 21 was confirmed from the active process.
- Minecraft TCP/UDP `25565` and Simple Voice Chat UDP `24454` were active.
- The VillAIgence Simple Voice Chat plugin loaded and registered as `mca_livingworld`.
- No blocking Mixin, refmap, injection, VillAIgence startup, or crash errors were found.
- LinuxGSM monitor returned `OK` after startup.
- No legacy `world/livingworld/memory.json` was created.

LinuxGSM continued to print its stale missing-`openjdk-17-jre` dependency warning. This was non-blocking because the active process used Temurin Java 21 and the server reached `Done`.

### Gate B — base AI chain: PASS

The player sent this text message to Muammer:

```text
Привет! Запомни: мой тестовый маркер — cobalt-lantern-731. Повтори его и скажи, как тебя зовут.
```

The NPC replied once, identified himself as Muammer, and repeated `Cobalt-lantern-731`. The dialogue was stored once in valid `memory2.json` under NPC UUID `34e2a220-7e85-4edc-8c93-52b068b97608`.

`/villaigence ai status` reported Chat `SUCCESS` with one provider attempt, `reasoningPresent=false`, Chat admission `active=0/4` after completion, and no rejected request or provider cooldown.

No API key, `Authorization` header, prompt, reasoning text, or raw provider payload appeared in player-visible output or the inspected logs.

## Manual canaries

The six canonical manual IDs remain the acceptance boundary:

```text
VAI-PCM-E2E-001
VAI-PCM-MULTI-001
VAI-PROX-MULTI-001
VAI-SEC-001
VAI-RESET-001
VAI-STT-001
```

### `VAI-PCM-E2E-001` — physical voice E2E: PASS

The player used a real microphone and Simple Voice Chat near Muammer. The spoken marker was `crimson-orbit-527`.

Observed and server-confirmed results:

- STT produced a meaningful transcript containing `crimson-orbit-527`;
- exactly one NPC text response appeared;
- exactly one audible TTS response was heard;
- voice was spatially attached to Muammer;
- the response was persisted once under Muammer's UUID;
- `memory2.json` and `voices.json` were valid;
- Chat, STT, and TTS admission counts returned to zero;
- no relevant provider, STT, TTS, duplicate, or security error was found.

### `VAI-STT-001` — real STT: PASS

Real STT preserved the unique marker `crimson-orbit-527`. Later voice requests also preserved `Amber-pine-314` and `violet-river-926`, although ordinary words and NPC names were sometimes normalized or misrecognized.

The normalization did not create duplicate STT events or leave NPC/server state busy.

### `VAI-PCM-MULTI-001` — multiple NPCs: FAIL

Two NPC identities were used:

- Muammer: `34e2a220-7e85-4edc-8c93-52b068b97608`
- Nurey: `4adf884f-81ad-4a88-bdf9-46eda1ce237a`

The private markers were stored under separate owner UUIDs:

- Muammer: `amber-pine-314`
- Nurey: `violet-river-926`

Nurey recalled `Violet-river-926`. Her history did not contain Muammer's marker. Muammer's history did not contain Nurey's marker.

Muammer did not recall `amber-pine-314`. The first voice recall question was distorted by STT, so the recall was repeated through an exact text request:

```text
Муаммер, назови личный маркер, который я ранее сообщил именно тебе.
```

Muammer still replied that he did not remember any marker. The original marker event remained present and valid under his UUID. Therefore storage ownership/isolation passed, while functional retrieval/recall failed.

This is the sole confirmed release-blocking installed defect from this validation run and is tracked for a narrow `0.3.1+1.21.1` corrective patch.

### `VAI-PROX-MULTI-001` — explicit voice target switching with nearby NPCs: PASS

The original test wording incorrectly assumed proximity-only NPC selection. VillAIgence does not provide that interaction contract: voice routing becomes available only after the player explicitly interacts with an NPC, and that RMB interaction selects the active voice target.

The corrected acceptance contract is:

1. multiple NPCs may be nearby;
2. RMB interaction with NPC A sets A as the active voice target;
3. the next voice request is delivered only to A;
4. merely moving closer to NPC B does not implicitly retarget the conversation;
5. RMB interaction with B explicitly changes the active target to B;
6. the next voice request is delivered only to B;
7. no double delivery, stale delivery to A, or cross-NPC busy state occurs.

Installed evidence:

1. The player interacted with Muammer and spoke `Контроль цели один. Назови своё имя.`
2. Only Muammer received the event and identified himself.
3. The player interacted with Nurey, moved closer to her, and spoke `Контроль цели два. Назови своё имя.`
4. Only Nurey received the event and identified herself.
5. No double delivery, cross-NPC busy state, or stale delivery to the previous NPC was observed.

**Result: PASS.** Proximity-only selection is `N/A / unsupported by design`, not a missing acceptance result and not a release defect.

### `VAI-SEC-001` — installed security sanity: PASS

- `/villaigence ai status` exposed configuration state but not credentials.
- No API key or `Authorization` header appeared in the inspected startup/runtime logs.
- No raw provider payload, prompt, or reasoning text was shown.
- Normal Chat, STT, and TTS requests completed without unsafe error output.
- No provider failure was forced because this check was optional and did not justify changing the working provider configuration.

### `VAI-RESET-001` — restart/reset boundary: PASS

A graceful LinuxGSM restart was performed on the same world after real text and voice state had been created.

Before and immediately after restart, the following hashes were identical:

```text
memory2.json  780fd65136b9d8722193566deb13b26939e5afbeed1dcc0a41ef1dec53a8d19b
voices.json   8f2237532996ad974b2f60647b8c8a66ad2b02cb866202528021f75dbef32cb1
```

After restart:

- the server reached `Done` again;
- Voice Chat restarted on UDP `24454`;
- Nurey's UUID remained `4adf884f-81ad-4a88-bdf9-46eda1ce237a`;
- Nurey recalled the pre-restart marker `Violet-river-926`;
- her voice remained subjectively the same and the persisted voice assignment remained `eve`;
- Muammer's persisted voice assignment remained `sal`;
- no duplicate restart event or persistence corruption was observed;
- legacy `memory.json` did not reappear;
- `npc-social-graph.json` had not been created by the exercised scenarios, so its restart loading was not applicable.

Restart evidence directory:

```text
/home/server/minecraft/restart-villaigence-0.3.0-20260812-235155
```

## Exploratory personality smoke: PASS

Muammer and Nurey produced observably different response styles over several interactions. This is subjective installed smoke evidence only; it does not prove a specific MCA Personality mapping or promote model output to authoritative fact.

## Explicit deferred installed evidence

These boundaries remain unchanged and must not be promoted by inference:

```text
VAI-M2-INST-005  NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004   NOT TESTED / DEFERRED
```

`VAI-M2-INST-005` must not become installed PASS without the missing real second-player installed isolation evidence.

`VAI-CONCUR-004` must not become PASS without two real graphical clients exercising the deferred boundary.

## Final disposition

```text
Gate A             PASS
Gate B             PASS
VAI-PCM-E2E-001    PASS
VAI-PCM-MULTI-001  FAIL
VAI-PROX-MULTI-001 PASS
VAI-SEC-001        PASS
VAI-RESET-001      PASS
VAI-STT-001        PASS
Personality smoke  PASS (exploratory)
VAI-M2-INST-005    NOT TESTED / AUTOMATED EVIDENCE ONLY
VAI-CONCUR-004     NOT TESTED / DEFERRED
```

The official `0.3.0+1.21.1` release remains installed, healthy and operational. It is **not installed-accepted** because the executed required canary `VAI-PCM-MULTI-001` has a confirmed FAIL.

No rollback is required merely to continue diagnostic playtesting because this run found no crash, corruption, credential leak, restart failure, persistence loss, or identity cross-contamination. The preserved rollback backup remains the recovery boundary if a later blocker requires it.

Because runtime bytes must change to correct the recall defect, the immutable `0.3.0+1.21.1` release is not modified. The corrective development track must use a new patch version, expected `0.3.1+1.21.1`.

Installed acceptance can close only after the corrective official asset is published, installed, its SHA verified, and the affected recall canary is rerun with real installed evidence.
