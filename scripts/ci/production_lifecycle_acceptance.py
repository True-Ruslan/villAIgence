#!/usr/bin/env python3
"""Validate exact NPC lifecycle and real voice transport evidence across production JVM runs."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys
from typing import Any, Mapping
from uuid import UUID

LIFECYCLE_RELATIVE_PATH = Path("world/livingworld/acceptance-lifecycle.json")
VOICE_TRANSPORT_RELATIVE_PATH = Path(
    "world/livingworld/acceptance-voice-transport.json"
)
EXPECTED_UUID = "5cf53206-ec2c-4c88-ad11-a8bbc56f514e"
EXPECTED_NAME = "Production Lifecycle Acceptance"
EXPECTED_INVENTORY: dict[str, int] = {
    "minecraft:bread": 11,
    "minecraft:emerald": 3,
    "minecraft:iron_sword": 1,
}
EXPECTED_NON_EMPTY_STACKS = 3
EXPECTED_LIVE_ENTITY_COUNT = 1
ALLOWED_PHASES = ("CREATED", "RESTART_VERIFIED")
VOICE_SCENARIO = "VAI-AI-005"
VOICE_SAMPLE_RATE = 48_000
VOICE_FRAME_SAMPLES = 960
VOICE_ENCODED_FRAMES = 4
VOICE_ACCEPTED_PACKETS = 3
VOICE_DECODED_FRAMES = 4
VOICE_DECODED_SAMPLES = VOICE_FRAME_SAMPLES * VOICE_DECODED_FRAMES
VOICE_LOST_PACKETS = 1
VOICE_PLC_SAMPLES = VOICE_FRAME_SAMPLES
VOICE_PCM_BYTES = VOICE_DECODED_SAMPLES * 2


class AcceptanceError(RuntimeError):
    """Raised when production evidence violates a hard acceptance invariant."""


def _required_mapping(value: Any, field: str) -> Mapping[str, Any]:
    if not isinstance(value, Mapping):
        raise AcceptanceError(f"{field} must be a JSON object")
    return value


def _required_string(value: Mapping[str, Any], field: str) -> str:
    candidate = value.get(field)
    if not isinstance(candidate, str) or not candidate.strip():
        raise AcceptanceError(f"{field} must be a non-empty string")
    return candidate.strip()


def _required_positive_int(value: Mapping[str, Any], field: str) -> int:
    candidate = value.get(field)
    if not isinstance(candidate, int) or isinstance(candidate, bool) or candidate <= 0:
        raise AcceptanceError(f"{field} must be a positive integer")
    return candidate


def _required_non_negative_int(value: Mapping[str, Any], field: str) -> int:
    candidate = value.get(field)
    if not isinstance(candidate, int) or isinstance(candidate, bool) or candidate < 0:
        raise AcceptanceError(f"{field} must be a non-negative integer")
    return candidate


def _required_true(value: Mapping[str, Any], field: str) -> bool:
    if value.get(field) is not True:
        raise AcceptanceError(f"{field} must be true")
    return True


def _load_json_evidence(
    server_root: Path | str,
    relative_path: Path,
    label: str,
) -> Mapping[str, Any]:
    root = Path(server_root).resolve(strict=True)
    if not root.is_dir():
        raise AcceptanceError(f"server root is not a directory: {root}")
    path = (root / relative_path).resolve()
    try:
        path.relative_to(root)
    except ValueError as exception:
        raise AcceptanceError(f"{label} escaped the server root") from exception
    if not path.is_file() or path.is_symlink():
        raise AcceptanceError(f"{label} is missing: {relative_path}")
    try:
        parsed = json.loads(path.read_text(encoding="utf-8"))
    except UnicodeDecodeError as exception:
        raise AcceptanceError(f"{label} must be valid UTF-8") from exception
    except json.JSONDecodeError as exception:
        raise AcceptanceError(f"{label} must be valid JSON") from exception
    return _required_mapping(parsed, label)


def _load_lifecycle_evidence(server_root: Path | str) -> Mapping[str, Any]:
    return _load_json_evidence(
        server_root,
        LIFECYCLE_RELATIVE_PATH,
        "lifecycle evidence",
    )


def _normalize_state(value: Mapping[str, Any], field: str) -> dict[str, Any]:
    if value.get("schema") != 1:
        raise AcceptanceError(f"{field} schema must equal 1")

    phase = _required_string(value, "phase")
    if phase not in ALLOWED_PHASES:
        raise AcceptanceError(f"{field} phase must be one of {ALLOWED_PHASES}")

    npc_uuid = _required_string(value, "npcUuid")
    try:
        UUID(npc_uuid)
    except ValueError as exception:
        raise AcceptanceError(f"{field} npcUuid must be a valid UUID") from exception
    if npc_uuid != EXPECTED_UUID:
        raise AcceptanceError(
            f"{field} npcUuid mismatch: expected {EXPECTED_UUID}, found {npc_uuid}"
        )

    npc_name = _required_string(value, "npcName")
    if npc_name != EXPECTED_NAME:
        raise AcceptanceError(
            f"{field} npcName mismatch: expected {EXPECTED_NAME!r}, found {npc_name!r}"
        )

    inventory_raw = _required_mapping(value.get("inventory"), "inventory")
    inventory: dict[str, int] = {}
    for item_id, count in inventory_raw.items():
        if not isinstance(item_id, str) or not item_id.strip():
            raise AcceptanceError(f"{field} inventory keys must be non-empty strings")
        if not isinstance(count, int) or isinstance(count, bool) or count <= 0:
            raise AcceptanceError(
                f"{field} inventory count for {item_id!r} must be a positive integer"
            )
        inventory[item_id.strip()] = count
    inventory = dict(sorted(inventory.items()))
    if inventory != EXPECTED_INVENTORY:
        raise AcceptanceError(
            f"{field} inventory mismatch: expected {EXPECTED_INVENTORY}, found {inventory}"
        )

    non_empty_stacks = _required_positive_int(value, "nonEmptyStacks")
    if non_empty_stacks != EXPECTED_NON_EMPTY_STACKS:
        raise AcceptanceError(
            f"{field} nonEmptyStacks mismatch: expected {EXPECTED_NON_EMPTY_STACKS}, "
            f"found {non_empty_stacks}"
        )

    live_entity_count = _required_positive_int(value, "liveEntityCount")
    if live_entity_count != EXPECTED_LIVE_ENTITY_COUNT:
        raise AcceptanceError(
            f"{field} liveEntityCount mismatch: expected {EXPECTED_LIVE_ENTITY_COUNT}, "
            f"found {live_entity_count}"
        )

    portable_consumed = value.get("portableGraveConsumed")
    if portable_consumed is not True:
        raise AcceptanceError(f"{field} portableGraveConsumed must be true")

    return {
        "schema": 1,
        "phase": phase,
        "npcUuid": npc_uuid,
        "npcName": npc_name,
        "inventory": inventory,
        "nonEmptyStacks": non_empty_stacks,
        "liveEntityCount": live_entity_count,
        "portableGraveConsumed": True,
    }


def collect_lifecycle_state(server_root: Path | str) -> dict[str, Any]:
    """Read and validate the current lifecycle state from an isolated server root."""
    return _normalize_state(_load_lifecycle_evidence(server_root), "lifecycle evidence")


def compare_lifecycle_states(
    first: Mapping[str, Any],
    second: Mapping[str, Any],
) -> tuple[str, ...]:
    """Compare immutable lifecycle fields and the mandatory phase transition."""
    errors: list[str] = []
    if first.get("phase") != "CREATED" or second.get("phase") != "RESTART_VERIFIED":
        errors.append(
            "lifecycle phase progression must be CREATED -> RESTART_VERIFIED: "
            f"{first.get('phase')} -> {second.get('phase')}"
        )

    stable_fields = (
        "npcUuid",
        "npcName",
        "inventory",
        "nonEmptyStacks",
        "liveEntityCount",
        "portableGraveConsumed",
    )
    for field in stable_fields:
        if first.get(field) != second.get(field):
            errors.append(
                f"lifecycle {field} changed across restart: "
                f"{first.get(field)!r} -> {second.get(field)!r}"
            )
    return tuple(errors)


def collect_lifecycle_history(server_root: Path | str) -> tuple[dict[str, Any], dict[str, Any]]:
    """Validate the durable two-snapshot history emitted by the production fixture."""
    evidence = _load_lifecycle_evidence(server_root)
    history = evidence.get("history")
    if not isinstance(history, list) or len(history) != 2:
        raise AcceptanceError("lifecycle history must contain exactly two snapshots")
    first = _normalize_state(_required_mapping(history[0], "history[0]"), "history[0]")
    second = _normalize_state(_required_mapping(history[1], "history[1]"), "history[1]")
    current = _normalize_state(evidence, "lifecycle evidence")
    if current != second:
        raise AcceptanceError("top-level lifecycle evidence must equal history[1]")
    errors = compare_lifecycle_states(first, second)
    if errors:
        raise AcceptanceError("restart lifecycle failed: " + "; ".join(errors))
    return first, second


def collect_voice_transport_state(server_root: Path | str) -> dict[str, Any]:
    """Validate real Simple Voice Chat Opus loopback evidence from the fixture."""
    value = _load_json_evidence(
        server_root,
        VOICE_TRANSPORT_RELATIVE_PATH,
        "voice transport evidence",
    )
    if value.get("schema") != 1:
        raise AcceptanceError("voice transport evidence schema must equal 1")
    scenario = _required_string(value, "scenario")
    if scenario != VOICE_SCENARIO:
        raise AcceptanceError(
            f"voice transport scenario must equal {VOICE_SCENARIO}, found {scenario}"
        )
    status = _required_string(value, "status")
    if status != "PASS":
        raise AcceptanceError(f"voice transport status must equal PASS, found {status}")

    exact_values = {
        "sampleRate": VOICE_SAMPLE_RATE,
        "frameSamples": VOICE_FRAME_SAMPLES,
        "encodedFrames": VOICE_ENCODED_FRAMES,
        "acceptedPackets": VOICE_ACCEPTED_PACKETS,
        "decodedFrames": VOICE_DECODED_FRAMES,
        "decodedSamples": VOICE_DECODED_SAMPLES,
        "lostPackets": VOICE_LOST_PACKETS,
        "plcSamples": VOICE_PLC_SAMPLES,
        "pcmBudgetMaxBytes": VOICE_PCM_BYTES,
    }
    normalized: dict[str, Any] = {
        "schema": 1,
        "scenario": scenario,
        "status": status,
    }
    for field, expected in exact_values.items():
        actual = _required_positive_int(value, field)
        if actual != expected:
            raise AcceptanceError(
                f"voice transport {field} mismatch: expected {expected}, found {actual}"
            )
        normalized[field] = actual

    encoded_bytes = _required_positive_int(value, "encodedBytes")
    normalized["encodedBytes"] = encoded_bytes

    peak_pcm = _required_positive_int(value, "peakPcmBytes")
    max_pcm = normalized["pcmBudgetMaxBytes"]
    if peak_pcm > max_pcm:
        raise AcceptanceError(
            f"voice transport peak PCM exceeds budget: {peak_pcm} > {max_pcm}"
        )
    if peak_pcm != VOICE_PCM_BYTES:
        raise AcceptanceError(
            f"voice transport peakPcmBytes mismatch: expected {VOICE_PCM_BYTES}, "
            f"found {peak_pcm}"
        )
    normalized["peakPcmBytes"] = peak_pcm

    for field in ("pcmBytesAfterCancel", "pcmBytesAfterDisconnect"):
        actual = _required_non_negative_int(value, field)
        if actual != 0:
            raise AcceptanceError(f"voice transport {field} must equal 0, found {actual}")
        normalized[field] = actual

    required_true_fields = (
        "duplicateRejected",
        "outOfOrderRejected",
        "budgetExhaustionRejected",
        "postCancelRejected",
        "postDisconnectRejected",
        "encoderClosed",
        "primaryDecoderClosed",
        "disconnectDecoderClosed",
    )
    for field in required_true_fields:
        normalized[field] = _required_true(value, field)

    return normalized


def _write_json(path: Path, value: Mapping[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(
        json.dumps(value, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    temporary.replace(path)


def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-root", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    return parser.parse_args()


def main() -> int:
    args = _parse_args()
    report_path = args.report.resolve()
    try:
        first, second = collect_lifecycle_history(args.server_root)
        voice_transport = collect_voice_transport_state(args.server_root)
        result: dict[str, Any] = {
            "schema": 1,
            "status": "PASS",
            "scenario": "VAI-LIFE-002",
            "firstRun": first,
            "secondRun": second,
            "voiceTransport": voice_transport,
        }
    except (AcceptanceError, OSError) as exception:
        result = {
            "schema": 1,
            "status": "FAIL",
            "scenario": "VAI-LIFE-002",
            "error": str(exception),
        }
        _write_json(report_path, result)
        print(json.dumps(result, indent=2, sort_keys=True))
        return 1

    _write_json(report_path, result)
    print(json.dumps(result, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    sys.exit(main())
