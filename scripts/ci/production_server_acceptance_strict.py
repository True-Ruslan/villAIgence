#!/usr/bin/env python3
"""Strict entrypoint for the production server acceptance harness.

The base harness owns staging, installer execution, persistence comparison and
report generation. This module replaces only the process and log oracles so a
vanilla Minecraft ``Done`` line cannot mask a failing VillAIgence fixture.
"""

from __future__ import annotations

from pathlib import Path
import subprocess
import threading
import time
from typing import Sequence

import production_server_acceptance as base

FIXTURE_READY_MARKER = "VILLAIGENCE_PRODUCTION_FIXTURE_READY"
STRICT_FORBIDDEN_LOG_SIGNATURES: tuple[str, ...] = (
    "Encountered an unexpected exception",
    "This crash report has been saved to:",
)

AcceptanceError = base.AcceptanceError
ProcessRunEvidence = base.ProcessRunEvidence
ServerLogResult = base.ServerLogResult


def _close_pipe(pipe: object | None) -> None:
    if pipe is None:
        return
    closed = getattr(pipe, "closed", True)
    if closed:
        return
    try:
        pipe.close()
    except OSError:
        pass


def _finish_reader(
    process: subprocess.Popen[str],
    reader: threading.Thread | None,
    timeout_seconds: float,
) -> None:
    if process.poll() is None:
        base._terminate_process(process, timeout_seconds)
    if reader is not None:
        reader.join(timeout=max(timeout_seconds, 1.0))
    _close_pipe(process.stdin)
    _close_pipe(process.stdout)


def run_server_process(
    command: Sequence[str],
    *,
    cwd: Path | str,
    log_path: Path | str,
    startup_timeout_seconds: float,
    shutdown_timeout_seconds: float,
) -> ProcessRunEvidence:
    """Run one server JVM and wait for VillAIgence fixture readiness.

    Minecraft's own ready line is intentionally insufficient. The fixture
    marker is emitted only after all acceptance assertions and evidence writes
    have completed on the server thread.
    """
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
    reader: threading.Thread | None = None

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
            _finish_reader(process, None, shutdown_timeout_seconds)
            raise AcceptanceError("server process pipes were not created")

        def read_output() -> None:
            try:
                for line in process.stdout:
                    log_handle.write(line)
                    log_handle.flush()
                    if FIXTURE_READY_MARKER in line:
                        ready_event.set()
            except BaseException as exception:  # pragma: no cover
                reader_failure.append(exception)

        reader = threading.Thread(
            target=read_output,
            name="production-server-log-reader",
            daemon=True,
        )
        reader.start()

        try:
            startup_deadline = time.monotonic() + startup_timeout_seconds
            while not ready_event.wait(timeout=0.05):
                if reader_failure:
                    raise AcceptanceError(
                        f"server log reader failed: {reader_failure[0]}"
                    )
                exit_code = process.poll()
                if exit_code is not None:
                    raise AcceptanceError(
                        f"server process exited before ready with code {exit_code}"
                    )
                if time.monotonic() >= startup_deadline:
                    raise AcceptanceError(
                        f"server startup timeout after {startup_timeout_seconds:.3f}s"
                    )

            try:
                process.stdin.write("stop\n")
                process.stdin.flush()
                process.stdin.close()
            except (BrokenPipeError, OSError) as exception:
                raise AcceptanceError("failed to send stop to ready server") from exception

            try:
                exit_code = process.wait(timeout=shutdown_timeout_seconds)
            except subprocess.TimeoutExpired as exception:
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
        except BaseException:
            _finish_reader(process, reader, shutdown_timeout_seconds)
            raise
        finally:
            _close_pipe(process.stdin)
            if reader is not None and not reader.is_alive():
                _close_pipe(process.stdout)

    duration_millis = max(0, round((time.monotonic() - started_at) * 1000))
    return ProcessRunEvidence(
        exit_code=exit_code,
        duration_millis=duration_millis,
        ready=True,
        stop_sent=True,
        log_path=output_path,
    )


def evaluate_server_log(
    log: str,
    *,
    minecraft_version: str,
    candidate_version: str,
    require_shutdown: bool,
) -> ServerLogResult:
    """Extend the base log oracle with fixture and crash requirements."""
    baseline = base.evaluate_server_log(
        log,
        minecraft_version=minecraft_version,
        candidate_version=candidate_version,
        require_shutdown=require_shutdown,
    )
    errors = list(baseline.errors)
    fixture_ready = FIXTURE_READY_MARKER in log
    if not fixture_ready:
        errors.append("server log does not contain the VillAIgence fixture ready marker")
    for signature in STRICT_FORBIDDEN_LOG_SIGNATURES:
        if signature in log:
            errors.append(f"forbidden startup signature detected: {signature}")
    return ServerLogResult(
        errors=tuple(errors),
        ready=baseline.ready and fixture_ready,
        clean_shutdown=baseline.clean_shutdown,
    )


def install_strict_oracles() -> None:
    """Install strict oracles into the base harness before orchestration."""
    base.run_server_process = run_server_process
    base.evaluate_server_log = evaluate_server_log


def main() -> int:
    install_strict_oracles()
    return base.main()


if __name__ == "__main__":
    raise SystemExit(main())
