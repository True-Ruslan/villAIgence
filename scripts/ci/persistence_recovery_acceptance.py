#!/usr/bin/env python3
"""Destructive production-JAR recovery matrix for VillAIgence JSON stores."""

from __future__ import annotations

import argparse
from dataclasses import asdict, dataclass
import hashlib
import json
from pathlib import Path
import shutil
import sys
from typing import Any, Mapping, Sequence

import production_server_acceptance as base
import production_server_acceptance_strict as strict

RECOVERY_MODE_PROPERTY = "-Dvillaigence.acceptance.mode=recovery"
RECOVERY_READY_MARKER = "VAI-PERSIST-003-RECOVERY-READY"
STORE_DIRECTORY = Path("world/livingworld")


@dataclass(frozen=True)
class RecoveryCase:
    case_id: str
    store: str
    variant: str
    payload: bytes
    mutation_target: str
    expected_backup: str | None
    canonical_must_match_baseline: bool = False


@dataclass(frozen=True)
class CorruptionEvidence:
    mutation_path: str
    payload_sha256: str
    payload_size: int
    expected_backup_path: str | None


RECOVERY_CASES: tuple[RecoveryCase, ...] = (
    RecoveryCase(
        "memory-truncated",
        "memory.json",
        "TRUNCATED_CANONICAL",
        b'{"version":1,"conversations":',
        "canonical",
        ".corrupt",
    ),
    RecoveryCase(
        "memory2-empty",
        "memory2.json",
        "EMPTY_CANONICAL",
        b"",
        "canonical",
        ".corrupt",
    ),
    RecoveryCase(
        "semantic-wrong-root",
        "semantic-memory.json",
        "WRONG_ROOT_CANONICAL",
        b"[]",
        "canonical",
        ".corrupt",
    ),
    RecoveryCase(
        "relationships-incompatible-schema",
        "relationships.json",
        "INCOMPATIBLE_SCHEMA_CANONICAL",
        b'{"version":2,"relationships":{}}',
        "canonical",
        ".corrupt",
    ),
    RecoveryCase(
        "voices-stale-temp",
        "voices.json",
        "STALE_VALID_TEMP_WITH_CANONICAL",
        b'{"version":1,"profiles":{}}',
        "temporary",
        None,
        canonical_must_match_baseline=True,
    ),
    RecoveryCase(
        "operator-lore-invalid-orphan-temp",
        "operator-lore.json",
        "INVALID_ORPHAN_TEMP_WITHOUT_CANONICAL",
        b"{broken",
        "orphan-temporary",
        ".tmp.corrupt",
    ),
)


class RecoveryAcceptanceError(RuntimeError):
    """Raised when a recovery case violates a hard acceptance invariant."""


def _sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def _sha256_file(path: Path) -> str:
    return _sha256_bytes(path.read_bytes())


def recovery_server_command(command: Sequence[str]) -> list[str]:
    result = list(command)
    try:
        jar_index = result.index("-jar")
    except ValueError as exception:
        raise RecoveryAcceptanceError("server command is missing -jar") from exception
    result.insert(jar_index, RECOVERY_MODE_PROPERTY)
    return result


def _store_path(server_root: Path, store: str) -> Path:
    root = server_root.resolve(strict=True)
    candidate = (root / STORE_DIRECTORY / store).resolve()
    try:
        candidate.relative_to(root)
    except ValueError as exception:
        raise RecoveryAcceptanceError("store path escaped the server root") from exception
    return candidate


def _cleanup_recovery_files(canonical: Path) -> None:
    for suffix in (".tmp", ".corrupt", ".tmp.corrupt"):
        path = canonical.resolveSibling(canonical.name + suffix)
        if path.exists():
            if path.is_dir() or path.is_symlink():
                raise RecoveryAcceptanceError(
                    f"unexpected recovery path type: {path}"
                )
            path.unlink()


def apply_corruption(server_root: Path | str, case: RecoveryCase) -> CorruptionEvidence:
    server = Path(server_root).resolve(strict=True)
    canonical = _store_path(server, case.store)
    if not canonical.is_file() or canonical.is_symlink():
        raise RecoveryAcceptanceError(
            f"baseline canonical store is missing: {case.store}"
        )
    _cleanup_recovery_files(canonical)

    if case.mutation_target == "canonical":
        mutation = canonical
    elif case.mutation_target == "temporary":
        mutation = canonical.resolveSibling(canonical.name + ".tmp")
    elif case.mutation_target == "orphan-temporary":
        canonical.unlink()
        mutation = canonical.resolveSibling(canonical.name + ".tmp")
    else:
        raise RecoveryAcceptanceError(
            f"unsupported mutation target: {case.mutation_target}"
        )

    mutation.write_bytes(case.payload)
    expected_backup = (
        canonical.resolveSibling(canonical.name + case.expected_backup)
        if case.expected_backup is not None
        else None
    )
    return CorruptionEvidence(
        mutation_path=mutation.relative_to(server).as_posix(),
        payload_sha256=_sha256_bytes(case.payload),
        payload_size=len(case.payload),
        expected_backup_path=(
            expected_backup.relative_to(server).as_posix()
            if expected_backup is not None
            else None
        ),
    )


def compare_unaffected_stores(
    baseline: Mapping[str, base.PersistentFileEvidence],
    recovered: Mapping[str, base.PersistentFileEvidence],
    target_store: str,
) -> tuple[str, ...]:
    errors: list[str] = []
    for store in base.CANONICAL_PERSISTENT_STORES:
        if store == target_store:
            continue
        before = baseline.get(store)
        after = recovered.get(store)
        if before is None or after is None:
            errors.append(f"unaffected store evidence is missing: {store}")
            continue
        if before.sha256 != after.sha256:
            errors.append(
                f"unaffected store changed during {target_store} recovery: "
                f"{store} {before.sha256} -> {after.sha256}"
            )
        if before.relative_path != after.relative_path:
            errors.append(
                f"unaffected store moved during {target_store} recovery: {store}"
            )
    return tuple(errors)


def _run_recovery_server(
    manifest: base.StageManifest,
    command: Sequence[str],
    *,
    server: Path,
    log_path: Path,
    startup_timeout_seconds: float,
    shutdown_timeout_seconds: float,
) -> base.ProcessRunEvidence:
    evidence = strict.run_server_process(
        command,
        cwd=server,
        log_path=log_path,
        startup_timeout_seconds=startup_timeout_seconds,
        shutdown_timeout_seconds=shutdown_timeout_seconds,
    )
    log = evidence.log_path.read_text(encoding="utf-8")
    oracle = strict.evaluate_server_log(
        log,
        minecraft_version=manifest.minecraft_version,
        candidate_version=manifest.candidate.version,
        require_shutdown=True,
    )
    errors = list(oracle.errors)
    if RECOVERY_READY_MARKER not in log:
        errors.append("server log does not contain the recovery ready marker")
    if errors:
        raise RecoveryAcceptanceError("; ".join(errors))
    return evidence


def _verify_case_after_first_run(
    server: Path,
    case: RecoveryCase,
    corruption: CorruptionEvidence,
    baseline: Mapping[str, base.PersistentFileEvidence],
    recovered: Mapping[str, base.PersistentFileEvidence],
) -> dict[str, Any]:
    canonical = _store_path(server, case.store)
    temporary = canonical.resolveSibling(canonical.name + ".tmp")
    if temporary.exists():
        raise RecoveryAcceptanceError(
            f"temporary file remains after recovery: {temporary.name}"
        )

    unaffected_errors = compare_unaffected_stores(
        baseline,
        recovered,
        case.store,
    )
    if unaffected_errors:
        raise RecoveryAcceptanceError("; ".join(unaffected_errors))

    if case.canonical_must_match_baseline:
        expected = baseline[case.store].sha256
        actual = recovered[case.store].sha256
        if expected != actual:
            raise RecoveryAcceptanceError(
                f"canonical store changed because of stale temp: {expected} -> {actual}"
            )

    backup: dict[str, Any] | None = None
    if corruption.expected_backup_path is not None:
        backup_path = (server / corruption.expected_backup_path).resolve()
        if not backup_path.is_file() or backup_path.is_symlink():
            raise RecoveryAcceptanceError(
                f"expected recovery backup is missing: {corruption.expected_backup_path}"
            )
        raw = backup_path.read_bytes()
        if raw != case.payload:
            raise RecoveryAcceptanceError(
                f"recovery backup does not preserve exact bytes: {case.case_id}"
            )
        backup = {
            "path": corruption.expected_backup_path,
            "sha256": _sha256_bytes(raw),
            "size": len(raw),
        }
    else:
        for suffix in (".corrupt", ".tmp.corrupt"):
            unexpected = canonical.resolveSibling(canonical.name + suffix)
            if unexpected.exists():
                raise RecoveryAcceptanceError(
                    f"stale temp case created unexpected backup: {unexpected.name}"
                )

    return {
        "backup": backup,
        "recoveredStore": asdict(recovered[case.store]),
    }


def _state_json(
    state: Mapping[str, base.PersistentFileEvidence],
) -> dict[str, dict[str, Any]]:
    return {name: asdict(value) for name, value in sorted(state.items())}


def execute_recovery_matrix(
    manifest: base.StageManifest,
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
        raise RecoveryAcceptanceError("recovery work directory must be empty")
    work.mkdir(parents=True, exist_ok=True)
    reports.mkdir(parents=True, exist_ok=True)

    baseline_server = work / "baseline-server"
    baseline_server.mkdir()
    base.run_bounded_command(
        base.build_installer_command(manifest, baseline_server, java_command),
        cwd=baseline_server,
        log_path=reports / "installer.log",
        timeout_seconds=installer_timeout_seconds,
    )
    launcher = baseline_server / "fabric-server-launch.jar"
    if not launcher.is_file() or launcher.is_symlink():
        raise RecoveryAcceptanceError(
            "Fabric Installer did not create a regular fabric-server-launch.jar"
        )
    base.prepare_server_directory(manifest, baseline_server)
    server_command = recovery_server_command(
        base.build_server_command(
            baseline_server,
            java_command,
            max_heap_mib=max_heap_mib,
        )
    )
    baseline_run = _run_recovery_server(
        manifest,
        server_command,
        server=baseline_server,
        log_path=reports / "baseline-server.log",
        startup_timeout_seconds=startup_timeout_seconds,
        shutdown_timeout_seconds=shutdown_timeout_seconds,
    )
    baseline_state = base.collect_persistent_state(baseline_server)
    lifecycle_evidence = baseline_server / STORE_DIRECTORY / "acceptance-lifecycle.json"
    if lifecycle_evidence.exists():
        raise RecoveryAcceptanceError(
            "recovery mode must not execute the unrelated NPC lifecycle fixture"
        )

    case_results: list[dict[str, Any]] = []
    for case in RECOVERY_CASES:
        case_server = work / "cases" / case.case_id
        case_server.parent.mkdir(parents=True, exist_ok=True)
        shutil.copytree(baseline_server, case_server)
        corruption = apply_corruption(case_server, case)
        case_command = recovery_server_command(
            base.build_server_command(
                case_server,
                java_command,
                max_heap_mib=max_heap_mib,
            )
        )

        first_run = _run_recovery_server(
            manifest,
            case_command,
            server=case_server,
            log_path=reports / f"{case.case_id}-run-1.log",
            startup_timeout_seconds=startup_timeout_seconds,
            shutdown_timeout_seconds=shutdown_timeout_seconds,
        )
        first_state = base.collect_persistent_state(case_server)
        first_evidence = _verify_case_after_first_run(
            case_server,
            case,
            corruption,
            baseline_state,
            first_state,
        )
        backup_before_second = (
            None
            if corruption.expected_backup_path is None
            else _sha256_file(case_server / corruption.expected_backup_path)
        )

        second_run = _run_recovery_server(
            manifest,
            case_command,
            server=case_server,
            log_path=reports / f"{case.case_id}-run-2.log",
            startup_timeout_seconds=startup_timeout_seconds,
            shutdown_timeout_seconds=shutdown_timeout_seconds,
        )
        second_state = base.collect_persistent_state(case_server)
        idempotence_errors = base.compare_persistent_states(
            first_state,
            second_state,
        )
        if idempotence_errors:
            raise RecoveryAcceptanceError(
                f"{case.case_id} second startup was not idempotent: "
                + "; ".join(idempotence_errors)
            )
        if corruption.expected_backup_path is not None:
            backup_after_second = _sha256_file(
                case_server / corruption.expected_backup_path
            )
            if backup_before_second != backup_after_second:
                raise RecoveryAcceptanceError(
                    f"{case.case_id} recovery backup changed on second startup"
                )

        case_results.append(
            {
                "id": case.case_id,
                "store": case.store,
                "variant": case.variant,
                "corruption": asdict(corruption),
                "firstRun": {
                    "durationMillis": first_run.duration_millis,
                    "exitCode": first_run.exit_code,
                    "log": first_run.log_path.name,
                },
                "secondRun": {
                    "durationMillis": second_run.duration_millis,
                    "exitCode": second_run.exit_code,
                    "log": second_run.log_path.name,
                },
                "recovery": first_evidence,
                "persistentStateAfterRecovery": _state_json(first_state),
                "persistentStateAfterSecondStartup": _state_json(second_state),
                "status": "PASS",
            }
        )

    result: dict[str, Any] = {
        "schema": 1,
        "scenario": "VAI-PERSIST-003",
        "status": "PASS",
        "minecraftVersion": manifest.minecraft_version,
        "loaderVersion": manifest.loader_version,
        "candidateVersion": manifest.candidate.version,
        "candidateSha256": manifest.candidate.sha256,
        "baselineRun": {
            "durationMillis": baseline_run.duration_millis,
            "exitCode": baseline_run.exit_code,
            "log": baseline_run.log_path.name,
        },
        "baselinePersistentState": _state_json(baseline_state),
        "cases": case_results,
    }
    base._write_json(reports / "persistence-recovery-report.json", result)
    return result


def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--stage-dir", type=Path, required=True)
    parser.add_argument("--execute", action="store_true")
    parser.add_argument("--work-dir", type=Path)
    parser.add_argument("--report-dir", type=Path)
    parser.add_argument("--java", default="java")
    parser.add_argument("--installer-timeout-seconds", type=float, default=300.0)
    parser.add_argument("--startup-timeout-seconds", type=float, default=180.0)
    parser.add_argument("--shutdown-timeout-seconds", type=float, default=60.0)
    parser.add_argument("--max-heap-mib", type=int, default=768)
    return parser.parse_args()


def main() -> int:
    args = _parse_args()
    manifest = base.load_stage_manifest(args.stage_dir)
    if not args.execute:
        print(
            json.dumps(
                {
                    "schema": 1,
                    "scenario": "VAI-PERSIST-003",
                    "status": "STAGING_VERIFIED",
                    "candidateVersion": manifest.candidate.version,
                    "candidateSha256": manifest.candidate.sha256,
                    "caseCount": len(RECOVERY_CASES),
                },
                indent=2,
                sort_keys=True,
            )
        )
        return 0
    if args.work_dir is None or args.report_dir is None:
        raise SystemExit("--execute requires --work-dir and --report-dir")

    report_path = args.report_dir.resolve() / "persistence-recovery-report.json"
    try:
        result = execute_recovery_matrix(
            manifest,
            work_dir=args.work_dir,
            report_dir=args.report_dir,
            java_command=args.java,
            installer_timeout_seconds=args.installer_timeout_seconds,
            startup_timeout_seconds=args.startup_timeout_seconds,
            shutdown_timeout_seconds=args.shutdown_timeout_seconds,
            max_heap_mib=args.max_heap_mib,
        )
    except (RecoveryAcceptanceError, base.AcceptanceError, OSError) as exception:
        failure = {
            "schema": 1,
            "scenario": "VAI-PERSIST-003",
            "status": "FAIL",
            "candidateVersion": manifest.candidate.version,
            "candidateSha256": manifest.candidate.sha256,
            "error": str(exception),
        }
        base._write_json(report_path, failure)
        print(json.dumps(failure, indent=2, sort_keys=True))
        return 1

    print(json.dumps(result, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    sys.exit(main())
