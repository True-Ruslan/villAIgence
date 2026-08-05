# M11 Phase E — E6 real voice transport validation

## Status

PASS on 2026-08-05.

This document records exact-head evidence for `VAI-AI-005`. It does not request a release version, create a tag or publish a release.

## Exact validated head

```text
abc1cbd276dcb67d3d51efe2234b3f36baee5bd8
```

## Mandatory workflows

| Gate | Run | Result |
| --- | ---: | --- |
| VillAIgence CI | 1646 / `31043585002` | PASS |
| Java Pull Request CI with Gradle | 1032 / `31043584966` | PASS |
| Repository security policy | 1197 / `31043584992` | PASS |
| VillAIgence GitHub Release dry-run | 258 / `31043584991` | PASS |

The release workflow ran in dry-run mode. The `github-release` job was intentionally skipped.

## Tested runtime

The normal exact-candidate production acceptance staged these runtime artifacts:

```text
Minecraft: 1.21.1
Fabric Loader: 0.19.3
Simple Voice Chat: 1.21.1-2.6.20
candidate SHA-256: 666a7da363fcd217dd0600eb62f47c2cae11fc183d682ee84dcf72c74b421621
```

Simple Voice Chat was not mocked. A test-only `VoicechatPlugin` inside the independently staged acceptance fixture obtained the real `VoicechatApi` and called:

```text
VoicechatApi.createEncoder()
VoicechatApi.createDecoder()
OpusEncoder.encode(short[])
OpusDecoder.decode(byte[])
OpusDecoder.decode(null)
```

The acceptance fixture remains outside the distributable candidate JAR. Package verification confirmed that no acceptance class or metadata leaked into the published artifact candidate.

## Real codec matrix

The plugin generated four deterministic mono PCM frames:

```text
sample rate: 48,000 Hz
frame size: 960 samples
frame duration: 20 ms
encoded frames: 4
```

It encoded all four frames through the real Opus encoder, then submitted sequences `0`, `1` and `3` to a bounded loopback session. Sequence `2` was intentionally omitted.

The session:

- decoded accepted packets through the real Opus decoder;
- called `decode(null)` once for packet-loss concealment;
- rejected a duplicate sequence;
- rejected an out-of-order sequence;
- reserved every decoded PCM frame through the production `VoicePcmBudget`;
- rejected an additional reservation after reaching the exact budget;
- released all reserved bytes on cancellation;
- rejected input after cancellation;
- repeated an independent one-frame session and released all bytes on disconnect;
- rejected input after disconnect;
- closed the encoder and both decoders.

## Machine-readable result

Artifact:

```text
production-server-acceptance-258
artifact id: 8945669123
digest: sha256:083221aa84c9bf67771a9bed244b62ac4f52b6fcefc29780d499f601e13641ed
```

Validated report fragment:

```json
{
  "scenario": "VAI-AI-005",
  "status": "PASS",
  "sampleRate": 48000,
  "frameSamples": 960,
  "encodedFrames": 4,
  "encodedBytes": 535,
  "acceptedPackets": 3,
  "decodedFrames": 4,
  "decodedSamples": 3840,
  "lostPackets": 1,
  "plcSamples": 960,
  "duplicateRejected": true,
  "outOfOrderRejected": true,
  "budgetExhaustionRejected": true,
  "pcmBudgetMaxBytes": 7680,
  "peakPcmBytes": 7680,
  "pcmBytesAfterCancel": 0,
  "pcmBytesAfterDisconnect": 0,
  "postCancelRejected": true,
  "postDisconnectRejected": true,
  "encoderClosed": true,
  "primaryDecoderClosed": true,
  "disconnectDecoderClosed": true
}
```

Both isolated JVM logs contain exactly one marker:

```text
VAI-AI-005-VOICE-TRANSPORT-PASS
```

The first JVM also completed real death, portable tombstone and 500-tick MCA resurrection. The second JVM verified the restored NPC after restart. All six canonical persistent-store SHA-256 values remained identical across the restart.

## Failure behavior

The fixture fails closed when:

- Simple Voice Chat does not initialize the plugin;
- the codec matrix reports an exception;
- the matrix does not complete within 200 server ticks;
- an encoded packet is empty or exceeds the accepted packet bound;
- the decoder returns an unexpected frame size;
- decoded PCM exceeds the budget;
- cancellation or disconnect leaves reserved bytes;
- codec resources remain open;
- the machine-readable report is absent, malformed or incomplete.

The release and PR production gates call the same strict report verifier.

## Recovery isolation

The destructive persistence recovery matrix sets:

```text
-Dvillaigence.acceptance.mode=recovery
```

The voice codec fixture and evidence writer explicitly skip this mode. The six recovery scenarios therefore remain isolated from unrelated voice workload while the normal two-JVM production acceptance always runs the real codec matrix.

## Catalog decision

`VAI-AI-005` is `AUTOMATED`.

A separate `VAI-AI-006` installed canary remains because CI cannot prove physical microphone permissions, real client UDP routing or audible playback through a user's speakers. Manual voice acceptance is therefore reduced to one short hardware/client smoke rather than rechecking codec, ordering, loss, cancellation, provider deadlines or resource cleanup.
