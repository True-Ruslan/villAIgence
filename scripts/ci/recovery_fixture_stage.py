#!/usr/bin/env python3
"""Derive a recovery-only production acceptance fixture inside a staged runtime."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
import tempfile
from typing import Any, Mapping
import zipfile

FIXTURE_ID = "mca-production-acceptance-fixture"
RECOVERY_ENTRYPOINT = (
    "net.conczin.mca.acceptancefixture.ProductionAcceptanceRecoveryMode"
)
METADATA_PATH = "fabric.mod.json"


class RecoveryFixtureStageError(RuntimeError):
    """Raised when the staged fixture cannot be safely derived."""


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _load_json(path: Path) -> Mapping[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exception:
        raise RecoveryFixtureStageError(f"invalid JSON file: {path}") from exception
    if not isinstance(value, Mapping):
        raise RecoveryFixtureStageError(f"JSON root must be an object: {path}")
    return value


def _read_mod_metadata(path: Path) -> dict[str, Any]:
    try:
        with zipfile.ZipFile(path) as archive:
            raw = archive.read(METADATA_PATH)
    except (OSError, KeyError, zipfile.BadZipFile) as exception:
        raise RecoveryFixtureStageError(
            f"invalid Fabric fixture JAR: {path}"
        ) from exception
    try:
        metadata = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exception:
        raise RecoveryFixtureStageError(
            f"invalid Fabric metadata in fixture JAR: {path}"
        ) from exception
    if not isinstance(metadata, dict):
        raise RecoveryFixtureStageError("Fabric metadata root must be an object")
    return metadata


def _find_fixture(stage: Path, manifest: Mapping[str, Any]) -> tuple[Path, int]:
    runtime = manifest.get("runtimeMods")
    if not isinstance(runtime, list):
        raise RecoveryFixtureStageError("manifest runtimeMods must be an array")
    matches: list[tuple[Path, int]] = []
    for index, entry in enumerate(runtime):
        if not isinstance(entry, Mapping) or entry.get("id") != FIXTURE_ID:
            continue
        relative = entry.get("path")
        if not isinstance(relative, str) or not relative:
            raise RecoveryFixtureStageError("fixture manifest path is invalid")
        path = (stage / relative).resolve()
        try:
            path.relative_to(stage)
        except ValueError as exception:
            raise RecoveryFixtureStageError(
                "fixture path escaped the staged runtime"
            ) from exception
        matches.append((path, index))
    if len(matches) != 1:
        raise RecoveryFixtureStageError(
            f"expected exactly one {FIXTURE_ID} runtime mod, found {len(matches)}"
        )
    path, index = matches[0]
    if not path.is_file() or path.is_symlink():
        raise RecoveryFixtureStageError("fixture runtime mod is not a regular file")
    return path, index


def _rewrite_fixture(path: Path) -> tuple[str, str, list[str]]:
    original_sha = _sha256(path)
    metadata = _read_mod_metadata(path)
    if metadata.get("id") != FIXTURE_ID:
        raise RecoveryFixtureStageError("fixture metadata id mismatch")
    entrypoints = metadata.get("entrypoints")
    if not isinstance(entrypoints, dict):
        entrypoints = {}
        metadata["entrypoints"] = entrypoints
    previous_main = entrypoints.get("main")
    previous = (
        [value for value in previous_main if isinstance(value, str)]
        if isinstance(previous_main, list)
        else []
    )
    entrypoints["main"] = [RECOVERY_ENTRYPOINT]

    with tempfile.NamedTemporaryFile(
        prefix=path.name + ".",
        suffix=".tmp",
        dir=path.parent,
        delete=False,
    ) as temporary_handle:
        temporary = Path(temporary_handle.name)
    try:
        with zipfile.ZipFile(path, "r") as source, zipfile.ZipFile(
            temporary,
            "w",
        ) as target:
            for info in source.infolist():
                data = source.read(info.filename)
                if info.filename == METADATA_PATH:
                    data = (
                        json.dumps(
                            metadata,
                            indent=2,
                            ensure_ascii=False,
                            sort_keys=True,
                        )
                        + "\n"
                    ).encode("utf-8")
                target.writestr(info, data)
        temporary.replace(path)
    finally:
        if temporary.exists():
            temporary.unlink()

    with zipfile.ZipFile(path) as archive:
        names = set(archive.namelist())
    class_path = RECOVERY_ENTRYPOINT.replace(".", "/") + ".class"
    if class_path not in names:
        raise RecoveryFixtureStageError(
            f"recovery fixture class is missing from staged JAR: {class_path}"
        )
    accepted = _read_mod_metadata(path)
    accepted_main = accepted.get("entrypoints", {}).get("main")
    if accepted_main != [RECOVERY_ENTRYPOINT]:
        raise RecoveryFixtureStageError(
            "derived fixture did not retain the recovery-only entrypoint"
        )
    return original_sha, _sha256(path), previous


def prepare_recovery_fixture_stage(stage_dir: Path | str) -> dict[str, Any]:
    stage = Path(stage_dir).resolve(strict=True)
    if not stage.is_dir():
        raise RecoveryFixtureStageError("stage directory is invalid")
    manifest_path = stage / "manifest.json"
    manifest = dict(_load_json(manifest_path))
    fixture, runtime_index = _find_fixture(stage, manifest)
    original_sha, derived_sha, previous = _rewrite_fixture(fixture)

    runtime = manifest["runtimeMods"]
    fixture_entry = dict(runtime[runtime_index])
    fixture_entry["sha256"] = derived_sha
    fixture_entry["size"] = fixture.stat().st_size
    runtime[runtime_index] = fixture_entry
    manifest["runtimeMods"] = runtime
    manifest_path.write_text(
        json.dumps(manifest, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )

    return {
        "id": FIXTURE_ID,
        "path": fixture.relative_to(stage).as_posix(),
        "originalSha256": original_sha,
        "derivedSha256": derived_sha,
        "size": fixture.stat().st_size,
        "previousMainEntrypoints": previous,
        "recoveryMainEntrypoints": [RECOVERY_ENTRYPOINT],
    }
