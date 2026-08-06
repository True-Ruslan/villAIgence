#!/usr/bin/env python3
"""Run bounded repeated VillAIgence production restarts under constrained heap."""

from __future__ import annotations

import argparse
from dataclasses import asdict
import json
from pathlib import Path
from typing import Any, Mapping, Sequence

import production_lifecycle_acceptance as lifecycle
import production_server_acceptance as base
import production_server_acceptance_strict as strict

MIN_SOAK_CYCLES = 3
MAX_SOAK_CYCLES = 12
MIN_SOAK_HEAP_MIB = 384
MAX_SOAK_HEAP_MIB = 768
CREATED_MARKER = "VAI-LIFE-002-CREATED"
RESTART_MARKER = "VAI-LIFE-002-RESTART-VERIFIED"


class SoakAcceptanceError(base.AcceptanceError):
    """Raised when a bounded production soak invariant fails."""


def validate_soak_parameters(cycles: int, max_heap_mib: int) -> None:
    if not MIN_SOAK_CYCLES <= cycles <= MAX_SOAK_CYCLES:
        raise SoakAcceptanceError(
            f"cycles must be between {MIN_SOAK_CYCLES} and {MAX_SOAK_CYCLES}, "
            f"found {cycles}"
        )
    if not MIN_SOAK_HEAP_MIB <= max_heap_mib <= MAX_SOAK_HEAP_MIB:
        raise SoakAcceptanceError(
            f"heap must be between {MIN_SOAK_HEAP_MIB} and "
            f"{MAX_SOAK_HEAP_MIB} MiB, found {max_heap_mib}"
        )


def verify_stable_persistent_state(
    baseline: Mapping[str, base.PersistentFileEvidence],
    current: Mapping[str, base.PersistentFileEvidence],
    *,
    cycle: int,
) -> None:
    errors = base.compare_persistent_states(baseline, current)
    if errors:
        raise SoakAcceptanceError(
            f"persistent state changed during soak cycle {cycle}: "
            + "; ".join(errors)
        )


def _persistent_json(
    evidence: Mapping[str, base.PersistentFileEvidence],
) -> dict[str, dict[str, Any]]:
    return {
        name: asdict(value)
        for name, value in sorted(evidence.items())
    }


def _write_json(path: Path, value: Mapping[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(
        json.dumps(value, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    temporary.replace(path)


def _validate_cycle_evidence(
    server: Path,
    *,
    cycle: int,
) -> tuple[dict[str, Any], dict[str, Any]]:
    try:
        lifecycle_state = lifecycle.collect_lifecycle_state(server)
        voice_state = lifecycle.collect_voice_transport_state(server)
        if cycle == 1:
            if lifecycle_state.get("phase") != "CREATED":
                raise SoakAcceptanceError(
                    "first soak cycle lifecycle phase must equal CREATED"
                )
        else:
            if lifecycle_state.get("phase") != "RESTART_VERIFIED":
                raise SoakAcceptanceError(
                    f"soak cycle {cycle} lifecycle phase must equal RESTART_VERIFIED"
                )
            lifecycle.collect_lifecycle_history(server)
        return lifecycle_state, voice_state
    except lifecycle.AcceptanceError as exception:
        raise SoakAcceptanceError(
            f"production evidence failed during soak cycle {cycle}: {exception}"
        ) from exception


def execute_production_soak(
    manifest: base.StageManifest,
    *,
    cycles: int,
    work_dir: Path | str,
    report_dir: Path | str,
    java_command: str,
    installer_timeout_seconds: float,
    startup_timeout_seconds: float,
    shutdown_timeout_seconds: float,
    max_heap_mib: int,
) -> dict[str, Any]:
    validate_soak_parameters(cycles, max_heap_mib)

    work = Path(work_dir).resolve()
    reports = Path(report_dir).resolve()
    if work.exists() and any(work.iterdir()):
        raise SoakAcceptanceError("production soak work directory must be empty")
    work.mkdir(parents=True, exist_ok=True)
    reports.mkdir(parents=True, exist_ok=True)
    server = work / "server"
    server.mkdir()

    base.run_bounded_command(
        base.build_installer_command(manifest, server, java_command),
        cwd=server,
        log_path=reports / "installer.log",
        timeout_seconds=installer_timeout_seconds,
    )
    launcher = server / "fabric-server-launch.jar"
    if not launcher.is_file() or launcher.is_symlink():
        raise SoakAcceptanceError(
            "Fabric Installer did not create a regular fabric-server-launch.jar"
        )

    base.prepare_server_directory(manifest, server)
    server_command = base.build_server_command(
        server,
        java_command,
        max_heap_mib=max_heap_mib,
    )

    baseline_state: Mapping[str, base.PersistentFileEvidence] | None = None
    cycle_reports: list[dict[str, Any]] = []
    for cycle in range(1, cycles + 1):
        run = strict.run_server_process(
            server_command,
            cwd=server,
            log_path=reports / f"server-cycle-{cycle}.log",
            startup_timeout_seconds=startup_timeout_seconds,
            shutdown_timeout_seconds=shutdown_timeout_seconds,
        )
        log = run.log_path.read_text(encoding="utf-8")
        oracle = strict.evaluate_server_log(
            log,
            minecraft_version=manifest.minecraft_version,
            candidate_version=manifest.candidate.version,
            require_shutdown=True,
        )
        if oracle.errors:
            raise SoakAcceptanceError(
                f"production soak cycle {cycle} failed: "
                + "; ".join(oracle.errors)
            )

        expected_marker = CREATED_MARKER if cycle == 1 else RESTART_MARKER
        if expected_marker not in log:
            raise SoakAcceptanceError(
                f"production soak cycle {cycle} is missing {expected_marker}"
            )

        persistent_state = base.collect_persistent_state(server)
        if baseline_state is None:
            baseline_state = persistent_state
        else:
            verify_stable_persistent_state(
                baseline_state,
                persistent_state,
                cycle=cycle,
            )

        lifecycle_state, voice_state = _validate_cycle_evidence(
            server,
            cycle=cycle,
        )
        cycle_reports.append(
            {
                "cycle": cycle,
                "exitCode": run.exit_code,
                "durationMillis": run.duration_millis,
                "ready": run.ready,
                "stopSent": run.stop_sent,
                "log": run.log_path.name,
                "lifecyclePhase": lifecycle_state["phase"],
                "liveEntityCount": lifecycle_state["liveEntityCount"],
                "voiceStatus": voice_state["status"],
                "peakPcmBytes": voice_state["peakPcmBytes"],
                "persistentState": _persistent_json(persistent_state),
            }
        )

    if baseline_state is None:
        raise SoakAcceptanceError("production soak did not execute any cycles")

    result: dict[str, Any] = {
        "schema": 1,
        "status": "PASS",
        "cycles": cycles,
        "maxHeapMiB": max_heap_mib,
        "minecraftVersion": manifest.minecraft_version,
        "loaderVersion": manifest.loader_version,
        "installerVersion": manifest.installer_version,
        "candidateVersion": manifest.candidate.version,
        "candidateSha256": manifest.candidate.sha256,
        "baselinePersistentState": _persistent_json(baseline_state),
        "cycleResults": cycle_reports,
    }
    _write_json(reports / "production-soak-report.json", result)
    return result


def _parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--stage-dir", type=Path, required=True)
    parser.add_argument("--execute", action="store_true")
    parser.add_argument("--cycles", type=int, default=5)
    parser.add_argument("--work-dir", type=Path)
    parser.add_argument("--report-dir", type=Path)
    parser.add_argument("--java", default="java")
    parser.add_argument("--installer-timeout-seconds", type=float, default=300.0)
    parser.add_argument("--startup-timeout-seconds", type=float, default=240.0)
    parser.add_argument("--shutdown-timeout-seconds", type=float, default=60.0)
    parser.add_argument("--max-heap-mib", type=int, default=512)
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = _parse_args(argv)
    manifest = base.load_stage_manifest(args.stage_dir)
    validate_soak_parameters(args.cycles, args.max_heap_mib)
    if not args.execute:
        print(
            json.dumps(
                {
                    "schema": 1,
                    "status": "STAGING_VERIFIED",
                    "cycles": args.cycles,
                    "maxHeapMiB": args.max_heap_mib,
                    "candidateVersion": manifest.candidate.version,
                    "candidateSha256": manifest.candidate.sha256,
                },
                indent=2,
                sort_keys=True,
            )
        )
        return 0

    if args.work_dir is None or args.report_dir is None:
        raise SystemExit("--execute requires --work-dir and --report-dir")

    report_path = args.report_dir.resolve() / "production-soak-report.json"
    try:
        result = execute_production_soak(
            manifest,
            cycles=args.cycles,
            work_dir=args.work_dir,
            report_dir=args.report_dir,
            java_command=args.java,
            installer_timeout_seconds=args.installer_timeout_seconds,
            startup_timeout_seconds=args.startup_timeout_seconds,
            shutdown_timeout_seconds=args.shutdown_timeout_seconds,
            max_heap_mib=args.max_heap_mib,
        )
    except (base.AcceptanceError, lifecycle.AcceptanceError) as exception:
        failure = {
            "schema": 1,
            "status": "FAIL",
            "cycles": args.cycles,
            "maxHeapMiB": args.max_heap_mib,
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
