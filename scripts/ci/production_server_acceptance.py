#!/usr/bin/env python3
"""Production-JAR startup/restart acceptance harness for VillAIgence.

The exact remapped candidate is installed into an isolated production Fabric
server, started in a separate JVM, stopped through stdin, restarted against the
same world, and evaluated with deterministic log and persistence oracles.
"""

from __future__ import annotations

import argparse
from dataclasses import asdict, dataclass
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import threading
import time
from typing import Any, Mapping, Sequence

CANONICAL_PERSISTENT_STORES: tuple[str, ...] = (
    "memory.json",
    "memory2.json",
    "semantic-memory.json",
    "relationships.json",
    "voices.json",
    "operator-lore.json",
)

FORBIDDEN_LOG_SIGNATURES: tuple[str, ...] = (
    "InvalidInjectionException",
    "MixinApplyError",
    "MixinTransformerError",
    "No refMap loaded",
    "MixinTombstoneBlock",
    "MixinTombstoneData",
    "MixinGroundPathNavigation failed",
    "Mod resolution encountered an incompatible mod set",
    "Could not find required mod",
    "Failed to start the minecraft server",
    "OutOfMemoryError",
)

SERVER_PROPERTIES: tuple[tuple[str, str], ...] = (
    ("allow-flight", "true"),
    ("difficulty", "peaceful"),
    ("enable-command-block", "false"),
    ("enable-query", "false"),
    ("enable-rcon", "false"),
    ("enforce-secure-profile", "false"),
    ("force-gamemode", "true"),
    ("gamemode", "creative"),
    ("generate-structures", "false"),
    ("level-name", "world"),
    ("max-players", "1"),
    ("motd", "VillAIgence production acceptance"),
    ("network-compression-threshold", "-1"),
    ("online-mode", "false"),
    ("prevent-proxy-connections", "false"),
    ("server-port", "0"),
    ("simulation-distance", "2"),
    ("spawn-animals", "false"),
    ("spawn-monsters", "false"),
    ("spawn-npcs", "true"),
    ("spawn-protection", "0"),
    ("sync-chunk-writes", "true"),
    ("view-distance", "2"),
)

_SHA256 = re.compile(r"^[0-9a-f]{64}$")


class AcceptanceError(RuntimeError):
    """Raised when acceptance input or evidence violates a hard invariant."""


@dataclass(frozen=True)
class VerifiedArtifact:
    path: Path
    sha256: str
    size: int


@dataclass(frozen=True)
class CandidateArtifact(VerifiedArtifact):
    version: str


@dataclass(frozen=True)
class StageManifest:
    root: Path
    minecraft_version: str
    loader_version: str
    installer_version: str
    installer: VerifiedArtifact
    candidate: CandidateArtifact
    runtime_mods: tuple[VerifiedArtifact, ...]


@dataclass(frozen=True)
class ProcessRunEvidence:
    exit_code: int
    duration_millis: int
    ready: bool
    stop_sent: bool
    log_path: Path


@dataclass(frozen=True)
class ServerLogResult:
    errors: tuple[str, ...]
    ready: bool
    clean_shutdown: bool


@dataclass(frozen=True)
class PersistentFileEvidence:
    relative_path: str
    sha256: str
    size: int
    root_type: str


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(128 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


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


def _confined_path(root: Path, raw: str, field: str) -> Path:
    relative = Path(raw)
    if relative.is_absolute():
        raise AcceptanceError(f"{field} must be relative and confined to the staging root")
    try:
        resolved = (root / relative).resolve(strict=True)
    except OSError as exception:
        raise AcceptanceError(
            f"{field} does not resolve to an existing file: {raw}"
        ) from exception
    try:
        resolved.relative_to(root)
    except ValueError as exception:
        raise AcceptanceError(
            f"{field} must remain confined to the staging root: {raw}"
        ) from exception
    if not resolved.is_file() or resolved.is_symlink():
        raise AcceptanceError(
            f"{field} must resolve to a regular non-symlink file: {raw}"
        )
    return resolved


def _load_artifact(root: Path, raw: Any, field: str) -> VerifiedArtifact:
    value = _required_mapping(raw, field)
    relative_path = _required_string(value, "path")
    expected_sha = _required_string(value, "sha256").lower()
    if not _SHA256.fullmatch(expected_sha):
        raise AcceptanceError(f"{field}.sha256 must be a lowercase SHA-256 digest")
    expected_size = _required_positive_int(value, "size")
    path = _confined_path(root, relative_path, f"{field}.path")
    actual_size = path.stat().st_size
    if actual_size != expected_size:
        raise AcceptanceError(
            f"{field} size mismatch: expected {expected_size}, found {actual_size}"
        )
    actual_sha = _sha256(path)
    if actual_sha != expected_sha:
        raise AcceptanceError(
            f"{field} checksum mismatch: expected {expected_sha}, found {actual_sha}"
        )
    return VerifiedArtifact(path=path, sha256=actual_sha, size=actual_size)


def load_stage_manifest(stage_dir: Path | str) -> StageManifest:
    root = Path(stage_dir).resolve(strict=True)
    if not root.is_dir():
        raise AcceptanceError(f"staging root is not a directory: {root}")
    manifest_path = root / "manifest.json"
    try:
        raw = json.loads(manifest_path.read_text(encoding="utf-8"))
    except FileNotFoundError as exception:
        raise AcceptanceError(f"staging manifest is missing: {manifest_path}") from exception
    except UnicodeDecodeError as exception:
        raise AcceptanceError("staging manifest must be valid UTF-8") from exception
    except json.JSONDecodeError as exception:
        raise AcceptanceError("staging manifest must be valid JSON") from exception

    value = _required_mapping(raw, "manifest")
    if value.get("schema") != 1:
        raise AcceptanceError("staging manifest must use schema=1")

    installer = _load_artifact(root, value.get("installer"), "installer")
    candidate_raw = _required_mapping(value.get("candidate"), "candidate")
    candidate_base = _load_artifact(root, candidate_raw, "candidate")
    candidate_version = _required_string(candidate_raw, "version")

    runtime_raw = value.get("runtimeMods")
    if not isinstance(runtime_raw, list) or not runtime_raw:
        raise AcceptanceError("runtimeMods must be a non-empty array")
    runtime_mods = tuple(
        _load_artifact(root, artifact, f"runtimeMods[{index}]")
        for index, artifact in enumerate(runtime_raw)
    )

    all_paths = [
        installer.path,
        candidate_base.path,
        *(item.path for item in runtime_mods),
    ]
    if len(set(all_paths)) != len(all_paths):
        raise AcceptanceError("staging manifest contains duplicate artifact paths")

    return StageManifest(
        root=root,
        minecraft_version=_required_string(value, "minecraftVersion"),
        loader_version=_required_string(value, "loaderVersion"),
        installer_version=_required_string(value, "installerVersion"),
        installer=installer,
        candidate=CandidateArtifact(
            path=candidate_base.path,
            sha256=candidate_base.sha256,
            size=candidate_base.size,
            version=candidate_version,
        ),
        runtime_mods=runtime_mods,
    )


def build_installer_command(
    manifest: StageManifest,
    server_dir: Path | str,
    java_command: str,
) -> list[str]:
    server = Path(server_dir).resolve()
    return [
        java_command,
        "-jar",
        str(manifest.installer.path),
        "server",
        "-dir",
        str(server),
        "-mcversion",
        manifest.minecraft_version,
        "-loader",
        manifest.loader_version,
        "-downloadMinecraft",
    ]


def build_server_command(
    server_dir: Path | str,
    java_command: str,
    *,
    max_heap_mib: int,
) -> list[str]:
    if max_heap_mib < 256:
        raise AcceptanceError("server max heap must be at least 256 MiB")
    server = Path(server_dir).resolve()
    return [
        java_command,
        "-Xms256M",
        f"-Xmx{max_heap_mib}M",
        "-Djava.awt.headless=true",
        "-jar",
        str(server / "fabric-server-launch.jar"),
        "nogui",
    ]


def prepare_server_directory(
    manifest: StageManifest,
    server_dir: Path | str,
) -> None:
    server = Path(server_dir).resolve()
    server.mkdir(parents=True, exist_ok=True)
    mods_dir = server / "mods"
    if mods_dir.exists() and any(mods_dir.iterdir()):
        raise AcceptanceError("server mods directory must be empty before staging")
    mods_dir.mkdir(parents=True, exist_ok=True)

    artifacts = (manifest.candidate, *manifest.runtime_mods)
    target_names = [artifact.path.name for artifact in artifacts]
    if len(set(target_names)) != len(target_names):
        raise AcceptanceError("staged production mods must have unique filenames")

    for artifact in artifacts:
        target = mods_dir / artifact.path.name
        shutil.copy2(artifact.path, target)
        if _sha256(target) != artifact.sha256:
            raise AcceptanceError(
                f"copied production mod checksum mismatch: {target.name}"
            )

    (server / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    properties = "".join(f"{key}={value}\n" for key, value in SERVER_PROPERTIES)
    (server / "server.properties").write_text(properties, encoding="utf-8")


def _terminate_process(process: subprocess.Popen[str], timeout_seconds: float) -> None:
    if process.poll() is not None:
        return
    process.terminate()
    try:
        process.wait(timeout=max(timeout_seconds, 0.1))
    except subprocess.TimeoutExpired:
        process.kill()
        process.wait(timeout=max(timeout_seconds, 0.1))


def run_server_process(
    command: Sequence[str],
    *,
    cwd: Path | str,
    log_path: Path | str,
    startup_timeout_seconds: float,
    shutdown_timeout_seconds: float,
) -> ProcessRunEvidence:
    if not command or any(not isinstance(value, str) or not value for value in command):
        raise AcceptanceError("server command must be a non-empty argument vector")
    if startup_timeout_seconds <= 0 or shutdown_timeout_seconds <= 0:
        raise AcceptanceError("server process timeouts must be positive")

    working_directory = Path(cwd).resolve(strict=True)
    if not working_directory.is_dir():
        raise AcceptanceError(f"server working directory is invalid: {working_directory}")
    output_path = Path(log_path).resolve()
    output_path.parent.mkdir(parents=True, exist_ok=True)

    started_at = time.monotonic()
    ready_event = threading.Event()
    reader_failure: list[BaseException] = []

    with output_path.open("w", encoding="utf-8", newline="") as log_handle:
        process = subprocess.Popen(
            list(command),
            cwd=working_directory,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            encoding="utf-8",
            errors="replace",
            bufsize=1,
            shell=False,
        )
        if process.stdout is None or process.stdin is None:
            _terminate_process(process, shutdown_timeout_seconds)
            raise AcceptanceError("server process pipes were not created")

        def read_output() -> None:
            try:
                for line in process.stdout:
                    log_handle.write(line)
                    log_handle.flush()
                    if "Done (" in line and 'For help, type "help"' in line:
                        ready_event.set()
            except BaseException as exception:  # pragma: no cover
                reader_failure.append(exception)

        reader = threading.Thread(
            target=read_output,
            name="production-server-log-reader",
            daemon=True,
        )
        reader.start()

        startup_deadline = time.monotonic() + startup_timeout_seconds
        while not ready_event.wait(timeout=0.05):
            if reader_failure:
                _terminate_process(process, shutdown_timeout_seconds)
                reader.join(timeout=1.0)
                raise AcceptanceError(
                    f"server log reader failed: {reader_failure[0]}"
                )
            exit_code = process.poll()
            if exit_code is not None:
                reader.join(timeout=1.0)
                raise AcceptanceError(
                    f"server process exited before ready with code {exit_code}"
                )
            if time.monotonic() >= startup_deadline:
                _terminate_process(process, shutdown_timeout_seconds)
                reader.join(timeout=1.0)
                raise AcceptanceError(
                    f"server startup timeout after {startup_timeout_seconds:.3f}s"
                )

        try:
            process.stdin.write("stop\n")
            process.stdin.flush()
            process.stdin.close()
        except (BrokenPipeError, OSError) as exception:
            _terminate_process(process, shutdown_timeout_seconds)
            reader.join(timeout=1.0)
            raise AcceptanceError("failed to send stop to ready server") from exception

        try:
            exit_code = process.wait(timeout=shutdown_timeout_seconds)
        except subprocess.TimeoutExpired as exception:
            _terminate_process(process, shutdown_timeout_seconds)
            reader.join(timeout=1.0)
            raise AcceptanceError(
                f"server shutdown timeout after {shutdown_timeout_seconds:.3f}s"
            ) from exception

        reader.join(timeout=max(shutdown_timeout_seconds, 1.0))
        if reader.is_alive():
            raise AcceptanceError("server log reader did not terminate")
        if reader_failure:
            raise AcceptanceError(f"server log reader failed: {reader_failure[0]}")
        if exit_code != 0:
            raise AcceptanceError(
                f"server process exited with non-zero code {exit_code} after ready"
            )

    duration_millis = max(0, round((time.monotonic() - started_at) * 1000))
    return ProcessRunEvidence(
        exit_code=exit_code,
        duration_millis=duration_millis,
        ready=True,
        stop_sent=True,
        log_path=output_path,
    )


def run_bounded_command(
    command: Sequence[str],
    *,
    cwd: Path | str,
    log_path: Path | str,
    timeout_seconds: float,
) -> int:
    if timeout_seconds <= 0:
        raise AcceptanceError("command timeout must be positive")
    working_directory = Path(cwd).resolve(strict=True)
    output_path = Path(log_path).resolve()
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8", newline="") as log_handle:
        try:
            completed = subprocess.run(
                list(command),
                cwd=working_directory,
                stdin=subprocess.DEVNULL,
                stdout=log_handle,
                stderr=subprocess.STDOUT,
                text=True,
                encoding="utf-8",
                errors="replace",
                shell=False,
                timeout=timeout_seconds,
                check=False,
            )
        except subprocess.TimeoutExpired as exception:
            raise AcceptanceError(
                f"bounded command timeout after {timeout_seconds:.3f}s"
            ) from exception
    if completed.returncode != 0:
        raise AcceptanceError(
            f"bounded command exited with non-zero code {completed.returncode}"
        )
    return completed.returncode


def evaluate_server_log(
    log: str,
    *,
    minecraft_version: str,
    candidate_version: str,
    require_shutdown: bool,
) -> ServerLogResult:
    errors: list[str] = []
    loader_pattern = re.compile(
        rf"Loading Minecraft\s+{re.escape(minecraft_version)}\s+with Fabric Loader\b"
    )
    candidate_pattern = re.compile(
        rf"(?m)^\s*-\s+mca\s+{re.escape(candidate_version)}(?:\s|$)"
    )
    ready = "Done (" in log and 'For help, type "help"' in log
    clean_shutdown = (
        "Stopping server" in log
        and "Saving worlds" in log
        and "All dimensions are saved" in log
    )

    if loader_pattern.search(log) is None:
        errors.append(
            f"server log does not prove Minecraft {minecraft_version} "
            "production Fabric startup"
        )
    if candidate_pattern.search(log) is None:
        errors.append(
            "server log does not contain the expected mca candidate version "
            f"{candidate_version}"
        )
    if not ready:
        errors.append("server log does not contain the Minecraft ready marker")
    if require_shutdown and not clean_shutdown:
        errors.append("server log does not contain a complete clean-shutdown/save marker")

    for signature in FORBIDDEN_LOG_SIGNATURES:
        if signature in log:
            errors.append(f"forbidden startup signature detected: {signature}")

    return ServerLogResult(
        errors=tuple(errors),
        ready=ready,
        clean_shutdown=clean_shutdown,
    )


def _json_root_type(value: Any) -> str:
    if isinstance(value, dict):
        return "object"
    if isinstance(value, list):
        return "array"
    raise AcceptanceError("persistent store JSON root must be an object or array")


def collect_persistent_state(
    server_root: Path | str,
    stores: Sequence[str] = CANONICAL_PERSISTENT_STORES,
) -> dict[str, PersistentFileEvidence]:
    root = Path(server_root).resolve(strict=True)
    if not root.is_dir():
        raise AcceptanceError(f"server root is not a directory: {root}")

    evidence: dict[str, PersistentFileEvidence] = {}
    for basename in stores:
        candidates = sorted(
            path
            for path in root.rglob(basename)
            if path.is_file() and not path.is_symlink()
        )
        if not candidates:
            raise AcceptanceError(f"missing canonical persistent store: {basename}")
        if len(candidates) != 1:
            relative = ", ".join(
                path.relative_to(root).as_posix() for path in candidates
            )
            raise AcceptanceError(
                f"duplicate canonical persistent store {basename}: {relative}"
            )

        path = candidates[0].resolve(strict=True)
        try:
            path.relative_to(root)
        except ValueError as exception:
            raise AcceptanceError(
                f"persistent store escaped the server root: {basename}"
            ) from exception
        raw = path.read_bytes()
        try:
            parsed = json.loads(raw.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError) as exception:
            raise AcceptanceError(
                f"persistent store must contain valid JSON: {basename}"
            ) from exception
        try:
            root_type = _json_root_type(parsed)
        except AcceptanceError as exception:
            raise AcceptanceError(
                f"persistent store must contain valid JSON object or array: {basename}"
            ) from exception

        evidence[basename] = PersistentFileEvidence(
            relative_path=path.relative_to(root).as_posix(),
            sha256=hashlib.sha256(raw).hexdigest(),
            size=len(raw),
            root_type=root_type,
        )

    return evidence


def compare_persistent_states(
    first: Mapping[str, PersistentFileEvidence],
    second: Mapping[str, PersistentFileEvidence],
) -> tuple[str, ...]:
    errors: list[str] = []
    if set(first) != set(second):
        missing_after = sorted(set(first) - set(second))
        added_after = sorted(set(second) - set(first))
        errors.append(
            "persistent store set changed across restart: "
            f"missing={missing_after}, added={added_after}"
        )

    for basename in sorted(set(first) & set(second)):
        before = first[basename]
        after = second[basename]
        if before.relative_path != after.relative_path:
            errors.append(
                f"{basename} moved across restart: "
                f"{before.relative_path} -> {after.relative_path}"
            )
        if before.sha256 != after.sha256:
            errors.append(
                f"{basename} changed across no-op restart: "
                f"{before.sha256} -> {after.sha256}"
            )
        if before.root_type != after.root_type:
            errors.append(
                f"{basename} JSON root type changed: "
                f"{before.root_type} -> {after.root_type}"
            )

    return tuple(errors)


def _persistent_json(
    evidence: Mapping[str, PersistentFileEvidence],
) -> dict[str, dict[str, Any]]:
    return {name: asdict(value) for name, value in sorted(evidence.items())}


def _write_json(path: Path, value: Mapping[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(
        json.dumps(value, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    temporary.replace(path)


def execute_production_acceptance(
    manifest: StageManifest,
    *,
    work_dir: Path | str,
    report_dir: Path | str,
    java_command: str,
    installer_timeout_seconds: float,
    startup_timeout_seconds: float,
    shutdown_timeout_seconds: float,
    max_heap_mib: int,
) -> dict[str, Any]:
    work = Path(work_dir).resolve()
    reports = Path(report_dir).resolve()
    if work.exists() and any(work.iterdir()):
        raise AcceptanceError("production acceptance work directory must be empty")
    work.mkdir(parents=True, exist_ok=True)
    reports.mkdir(parents=True, exist_ok=True)
    server = work / "server"
    server.mkdir()

    installer_log = reports / "installer.log"
    run_bounded_command(
        build_installer_command(manifest, server, java_command),
        cwd=server,
        log_path=installer_log,
        timeout_seconds=installer_timeout_seconds,
    )
    launcher = server / "fabric-server-launch.jar"
    if not launcher.is_file() or launcher.is_symlink():
        raise AcceptanceError(
            "Fabric Installer did not create a regular fabric-server-launch.jar"
        )

    prepare_server_directory(manifest, server)
    server_command = build_server_command(
        server,
        java_command,
        max_heap_mib=max_heap_mib,
    )

    first_run = run_server_process(
        server_command,
        cwd=server,
        log_path=reports / "server-run-1.log",
        startup_timeout_seconds=startup_timeout_seconds,
        shutdown_timeout_seconds=shutdown_timeout_seconds,
    )
    first_log = first_run.log_path.read_text(encoding="utf-8")
    first_oracle = evaluate_server_log(
        first_log,
        minecraft_version=manifest.minecraft_version,
        candidate_version=manifest.candidate.version,
        require_shutdown=True,
    )
    if first_oracle.errors:
        raise AcceptanceError("first production startup failed: " + "; ".join(first_oracle.errors))
    first_state = collect_persistent_state(server)

    second_run = run_server_process(
        server_command,
        cwd=server,
        log_path=reports / "server-run-2.log",
        startup_timeout_seconds=startup_timeout_seconds,
        shutdown_timeout_seconds=shutdown_timeout_seconds,
    )
    second_log = second_run.log_path.read_text(encoding="utf-8")
    second_oracle = evaluate_server_log(
        second_log,
        minecraft_version=manifest.minecraft_version,
        candidate_version=manifest.candidate.version,
        require_shutdown=True,
    )
    if second_oracle.errors:
        raise AcceptanceError(
            "second production startup failed: " + "; ".join(second_oracle.errors)
        )
    second_state = collect_persistent_state(server)
    persistence_errors = compare_persistent_states(first_state, second_state)
    if persistence_errors:
        raise AcceptanceError("restart persistence failed: " + "; ".join(persistence_errors))

    result: dict[str, Any] = {
        "schema": 1,
        "status": "PASS",
        "minecraftVersion": manifest.minecraft_version,
        "loaderVersion": manifest.loader_version,
        "installerVersion": manifest.installer_version,
        "candidateVersion": manifest.candidate.version,
        "candidateSha256": manifest.candidate.sha256,
        "runtimeMods": [
            {
                "filename": artifact.path.name,
                "sha256": artifact.sha256,
                "size": artifact.size,
            }
            for artifact in manifest.runtime_mods
        ],
        "firstRun": {
            "exitCode": first_run.exit_code,
            "durationMillis": first_run.duration_millis,
            "ready": first_run.ready,
            "stopSent": first_run.stop_sent,
            "log": first_run.log_path.name,
        },
        "secondRun": {
            "exitCode": second_run.exit_code,
            "durationMillis": second_run.duration_millis,
            "ready": second_run.ready,
            "stopSent": second_run.stop_sent,
            "log": second_run.log_path.name,
        },
        "persistentStateBeforeRestart": _persistent_json(first_state),
        "persistentStateAfterRestart": _persistent_json(second_state),
    }
    _write_json(reports / "acceptance-report.json", result)
    return result


def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--stage-dir", type=Path, required=True)
    parser.add_argument("--execute", action="store_true")
    parser.add_argument("--work-dir", type=Path)
    parser.add_argument("--report-dir", type=Path)
    parser.add_argument("--java", default="java")
    parser.add_argument("--installer-timeout-seconds", type=float, default=300.0)
    parser.add_argument("--startup-timeout-seconds", type=float, default=240.0)
    parser.add_argument("--shutdown-timeout-seconds", type=float, default=60.0)
    parser.add_argument("--max-heap-mib", type=int, default=768)
    return parser.parse_args()


def main() -> int:
    args = _parse_args()
    manifest = load_stage_manifest(args.stage_dir)
    if not args.execute:
        print(
            json.dumps(
                {
                    "schema": 1,
                    "minecraftVersion": manifest.minecraft_version,
                    "loaderVersion": manifest.loader_version,
                    "candidateVersion": manifest.candidate.version,
                    "candidateSha256": manifest.candidate.sha256,
                    "runtimeModCount": len(manifest.runtime_mods),
                    "status": "STAGING_VERIFIED",
                },
                indent=2,
                sort_keys=True,
            )
        )
        return 0

    if args.work_dir is None or args.report_dir is None:
        raise SystemExit("--execute requires --work-dir and --report-dir")

    report_path = args.report_dir.resolve() / "acceptance-report.json"
    try:
        result = execute_production_acceptance(
            manifest,
            work_dir=args.work_dir,
            report_dir=args.report_dir,
            java_command=args.java,
            installer_timeout_seconds=args.installer_timeout_seconds,
            startup_timeout_seconds=args.startup_timeout_seconds,
            shutdown_timeout_seconds=args.shutdown_timeout_seconds,
            max_heap_mib=args.max_heap_mib,
        )
    except AcceptanceError as exception:
        failure = {
            "schema": 1,
            "status": "FAIL",
            "minecraftVersion": manifest.minecraft_version,
            "loaderVersion": manifest.loader_version,
            "installerVersion": manifest.installer_version,
            "candidateVersion": manifest.candidate.version,
            "candidateSha256": manifest.candidate.sha256,
            "error": str(exception),
        }
        _write_json(report_path, failure)
        print(json.dumps(failure, indent=2, sort_keys=True))
        return 1

    print(json.dumps(result, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
