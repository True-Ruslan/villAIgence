#!/usr/bin/env python3
"""Loopback-only hostile provider acceptance harness for VillAIgence.

The server uses only Python's standard library, never binds to a non-loopback
address, streams hostile bodies without allocating them in full, and records
only sanitized request metadata.
"""

from __future__ import annotations

import argparse
from collections import Counter
import http.server
import ipaddress
import json
from pathlib import Path
import socket
import threading
import time
from typing import Any
from urllib.parse import parse_qsl, urlsplit

MEBIBYTE = 1024 * 1024


class ResponseLimits:
    def __init__(
        self,
        *,
        chat_json_bytes: int = 8 * MEBIBYTE,
        stt_json_bytes: int = 4 * MEBIBYTE,
        tts_audio_bytes: int = 64 * MEBIBYTE,
        error_body_bytes: int = 256 * 1024,
        verification_json_bytes: int = 64 * 1024,
    ) -> None:
        values = (
            chat_json_bytes,
            stt_json_bytes,
            tts_audio_bytes,
            error_body_bytes,
            verification_json_bytes,
        )
        if any(value <= 0 for value in values):
            raise ValueError("Every response limit must be positive")
        self.chat_json_bytes = chat_json_bytes
        self.stt_json_bytes = stt_json_bytes
        self.tts_audio_bytes = tts_audio_bytes
        self.error_body_bytes = error_body_bytes
        self.verification_json_bytes = verification_json_bytes

    def as_dict(self) -> dict[str, int]:
        return {
            "chat_json_bytes": self.chat_json_bytes,
            "stt_json_bytes": self.stt_json_bytes,
            "tts_audio_bytes": self.tts_audio_bytes,
            "error_body_bytes": self.error_body_bytes,
            "verification_json_bytes": self.verification_json_bytes,
        }


class HarnessConfig:
    def __init__(
        self,
        *,
        bind: str = "127.0.0.1",
        port: int = 18080,
        evidence_dir: Path | str = Path("build/security-acceptance/provider"),
        slow_duration_seconds: float = 660.0,
        slow_interval_seconds: float = 5.0,
        limits: ResponseLimits | None = None,
    ) -> None:
        self.bind = validate_loopback_bind(bind)
        if port < 0 or port > 65535:
            raise ValueError("port must be between 0 and 65535")
        if slow_duration_seconds <= 0.0:
            raise ValueError("slow_duration_seconds must be positive")
        if slow_interval_seconds <= 0.0:
            raise ValueError("slow_interval_seconds must be positive")
        self.port = port
        self.evidence_dir = Path(evidence_dir)
        self.slow_duration_seconds = slow_duration_seconds
        self.slow_interval_seconds = slow_interval_seconds
        self.limits = limits or ResponseLimits()


def validate_loopback_bind(value: str) -> str:
    """Require a literal loopback IP; hostnames and wildcard addresses are rejected."""
    try:
        address = ipaddress.ip_address(value)
    except ValueError as exc:
        raise ValueError("Harness bind address must be a literal loopback IP") from exc
    if not address.is_loopback:
        raise ValueError("Harness bind address must be loopback")
    return address.compressed


class _ThreadingHttpServer(http.server.ThreadingHTTPServer):
    daemon_threads = True
    allow_reuse_address = False


class _ThreadingHttpServerV6(_ThreadingHttpServer):
    address_family = socket.AF_INET6


class ProviderAcceptanceServer:
    ROUTES = (
        "/v1/chat/completions/{ok,declared-oversize,chunked-oversize,error-oversize,redirect,slow-drip}",
        "/v1/audio/transcriptions/{ok,declared-oversize,chunked-oversize,error-oversize,redirect,slow-drip}",
        "/v1/audio/speech/{ok,declared-oversize,chunked-oversize,error-oversize,redirect,slow-drip}",
        "/v1/mca/verify/{success,failed,declared-oversize,chunked-oversize,error-oversize,redirect,slow-drip}",
        "/__harness__/status",
        "/__harness__/redirect-target",
    )

    def __init__(self, config: HarnessConfig) -> None:
        self.config = config
        self.evidence_path = config.evidence_dir / "requests.jsonl"
        self.manifest_path = config.evidence_dir / "manifest.json"
        self._lock = threading.Lock()
        self._request_count = 0
        self._redirect_response_count = 0
        self._redirect_target_hits = 0
        self._server: http.server.ThreadingHTTPServer | None = None
        self._thread: threading.Thread | None = None

    @property
    def port(self) -> int:
        if self._server is None:
            raise RuntimeError("Harness server has not been started")
        return int(self._server.server_address[1])

    @property
    def base_url(self) -> str:
        host = f"[{self.config.bind}]" if ":" in self.config.bind else self.config.bind
        return f"http://{host}:{self.port}"

    def start(self) -> None:
        if self._server is not None:
            raise RuntimeError("Harness server is already started")
        self.config.evidence_dir.mkdir(parents=True, exist_ok=True)
        self.evidence_path.write_text("", encoding="utf-8")
        server_type = _ThreadingHttpServerV6 if ":" in self.config.bind else _ThreadingHttpServer
        owner = self

        class Handler(http.server.BaseHTTPRequestHandler):
            protocol_version = "HTTP/1.1"
            server_version = "VillAIgenceAcceptanceHarness/1"
            sys_version = ""

            def do_GET(self) -> None:  # noqa: N802 - inherited HTTP API
                owner._handle(self)

            def do_POST(self) -> None:  # noqa: N802 - inherited HTTP API
                owner._handle(self)

            def log_message(self, _format: str, *args: object) -> None:
                del args

        self._server = server_type((self.config.bind, self.config.port), Handler)
        self._write_manifest()
        self._thread = threading.Thread(
            target=self._server.serve_forever,
            name="villaigence-provider-acceptance-harness",
            daemon=True,
        )
        self._thread.start()

    def close(self) -> None:
        server = self._server
        if server is None:
            return
        server.shutdown()
        server.server_close()
        if self._thread is not None:
            self._thread.join(timeout=5.0)
        self._server = None
        self._thread = None

    def snapshot(self) -> dict[str, int]:
        with self._lock:
            return {
                "request_count": self._request_count,
                "redirect_response_count": self._redirect_response_count,
                "redirect_target_hits": self._redirect_target_hits,
            }

    def _write_manifest(self) -> None:
        manifest = {
            "schema": 1,
            "bind": self.config.bind,
            "port": self.port,
            "base_url": self.base_url,
            "slow_duration_seconds": self.config.slow_duration_seconds,
            "slow_interval_seconds": self.config.slow_interval_seconds,
            "limits": self.config.limits.as_dict(),
            "routes": list(self.ROUTES),
            "evidence": self.evidence_path.name,
        }
        self.manifest_path.write_text(
            json.dumps(manifest, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )

    def _handle(self, handler: http.server.BaseHTTPRequestHandler) -> None:
        parsed = urlsplit(handler.path)
        path = parsed.path
        body_bytes = self._discard_request_body(handler)
        stage, case = self._classify(path)
        response_status = 404

        try:
            if path == "/__harness__/redirect-target":
                with self._lock:
                    self._redirect_target_hits += 1
                response_status = 204
                self._send_empty(handler, response_status)
            elif path == "/__harness__/status":
                response_status = 200
                self._send_json(handler, response_status, self.snapshot())
            elif stage is None or case is None:
                self._send_json(handler, response_status, {"error": "unknown_route"})
            else:
                response_status = self._respond(handler, stage, case)
        except (BrokenPipeError, ConnectionResetError, TimeoutError):
            # Expected when the VillAIgence client aborts an oversized or deadline-bound response.
            pass
        finally:
            self._record(
                handler=handler,
                path=path,
                query_keys=sorted({key for key, _ in parse_qsl(parsed.query, keep_blank_values=True)}),
                body_bytes=body_bytes,
                stage=stage or "harness",
                case=case or path.rsplit("/", 1)[-1],
                response_status=response_status,
            )

    @staticmethod
    def _discard_request_body(handler: http.server.BaseHTTPRequestHandler) -> int:
        raw = handler.headers.get("Content-Length")
        if raw is None:
            return 0
        try:
            remaining = max(0, int(raw))
        except ValueError:
            return 0
        observed = 0
        while remaining > 0:
            chunk = handler.rfile.read(min(64 * 1024, remaining))
            if not chunk:
                break
            observed += len(chunk)
            remaining -= len(chunk)
        return observed

    @staticmethod
    def _classify(path: str) -> tuple[str | None, str | None]:
        prefixes = (
            ("/v1/chat/completions/", "chat"),
            ("/v1/audio/transcriptions/", "stt"),
            ("/v1/audio/speech/", "tts"),
            ("/v1/mca/verify/", "verification"),
        )
        for prefix, stage in prefixes:
            if path.startswith(prefix):
                case = path[len(prefix):]
                return stage, case or None
        return None, None

    def _respond(self, handler: http.server.BaseHTTPRequestHandler, stage: str, case: str) -> int:
        if case in {"ok", "success", "failed"}:
            return self._respond_normal(handler, stage, case)
        if case == "declared-oversize":
            self._send_declared_oversize(handler, self._stage_limit(stage))
            return 200
        if case == "chunked-oversize":
            self._send_chunked(handler, 200, self._stage_limit(stage) + 1, self._content_type(stage))
            return 200
        if case == "error-oversize":
            self._send_chunked(
                handler,
                500,
                self.config.limits.error_body_bytes + 1,
                "application/json",
            )
            return 500
        if case == "redirect":
            with self._lock:
                self._redirect_response_count += 1
            handler.send_response(307)
            handler.send_header("Location", "/__harness__/redirect-target")
            handler.send_header("Content-Length", "0")
            handler.send_header("Connection", "close")
            handler.end_headers()
            return 307
        if case == "slow-drip":
            self._send_slow_drip(handler, stage)
            return 200
        self._send_json(handler, 404, {"error": "unknown_case"})
        return 404

    def _respond_normal(self, handler: http.server.BaseHTTPRequestHandler, stage: str, case: str) -> int:
        if stage == "chat":
            body = {
                "choices": [{
                    "message": {"content": '{"message":"Harness OK"}'},
                    "finish_reason": "stop",
                }]
            }
            self._send_json(handler, 200, body)
        elif stage == "stt":
            self._send_json(handler, 200, {"text": "harness speech"})
        elif stage == "tts":
            self._send_fixed(handler, 200, b"\x00\x00\x01\x00", "audio/pcm;rate=24000;channels=1")
        elif stage == "verification":
            answer = "failed" if case == "failed" else "success"
            self._send_json(handler, 200, {"answer": answer})
        else:
            self._send_json(handler, 404, {"error": "unknown_stage"})
            return 404
        return 200

    def _stage_limit(self, stage: str) -> int:
        return {
            "chat": self.config.limits.chat_json_bytes,
            "stt": self.config.limits.stt_json_bytes,
            "tts": self.config.limits.tts_audio_bytes,
            "verification": self.config.limits.verification_json_bytes,
        }[stage]

    @staticmethod
    def _content_type(stage: str) -> str:
        return "audio/pcm;rate=24000;channels=1" if stage == "tts" else "application/json"

    def _send_declared_oversize(self, handler: http.server.BaseHTTPRequestHandler, limit: int) -> None:
        handler.send_response(200)
        handler.send_header("Content-Type", "application/json")
        handler.send_header("Content-Length", str(limit + 1))
        handler.send_header("Connection", "close")
        handler.end_headers()
        handler.wfile.write(b"x")
        handler.wfile.flush()
        handler.close_connection = True

    @staticmethod
    def _send_fixed(
        handler: http.server.BaseHTTPRequestHandler,
        status: int,
        body: bytes,
        content_type: str,
    ) -> None:
        handler.send_response(status)
        handler.send_header("Content-Type", content_type)
        handler.send_header("Content-Length", str(len(body)))
        handler.send_header("Connection", "close")
        handler.end_headers()
        handler.wfile.write(body)
        handler.wfile.flush()
        handler.close_connection = True

    def _send_json(self, handler: http.server.BaseHTTPRequestHandler, status: int, value: Any) -> None:
        body = json.dumps(value, ensure_ascii=False, sort_keys=True).encode("utf-8")
        self._send_fixed(handler, status, body, "application/json")

    @staticmethod
    def _send_empty(handler: http.server.BaseHTTPRequestHandler, status: int) -> None:
        handler.send_response(status)
        handler.send_header("Content-Length", "0")
        handler.send_header("Connection", "close")
        handler.end_headers()
        handler.close_connection = True

    @staticmethod
    def _write_chunk(handler: http.server.BaseHTTPRequestHandler, data: bytes) -> None:
        handler.wfile.write(f"{len(data):X}\r\n".encode("ascii"))
        handler.wfile.write(data)
        handler.wfile.write(b"\r\n")

    def _send_chunked(
        self,
        handler: http.server.BaseHTTPRequestHandler,
        status: int,
        byte_count: int,
        content_type: str,
    ) -> None:
        handler.send_response(status)
        handler.send_header("Content-Type", content_type)
        handler.send_header("Transfer-Encoding", "chunked")
        handler.send_header("Connection", "close")
        handler.end_headers()
        remaining = byte_count
        block = b"x" * (64 * 1024)
        while remaining > 0:
            size = min(len(block), remaining)
            self._write_chunk(handler, block[:size])
            remaining -= size
        handler.wfile.write(b"0\r\n\r\n")
        handler.wfile.flush()
        handler.close_connection = True

    def _send_slow_drip(self, handler: http.server.BaseHTTPRequestHandler, stage: str) -> None:
        handler.send_response(200)
        handler.send_header("Content-Type", self._content_type(stage))
        handler.send_header("Transfer-Encoding", "chunked")
        handler.send_header("Connection", "close")
        handler.end_headers()
        deadline = time.monotonic() + self.config.slow_duration_seconds
        while time.monotonic() < deadline:
            self._write_chunk(handler, b"x")
            handler.wfile.flush()
            time.sleep(self.config.slow_interval_seconds)
        handler.wfile.write(b"0\r\n\r\n")
        handler.wfile.flush()
        handler.close_connection = True

    def _record(
        self,
        *,
        handler: http.server.BaseHTTPRequestHandler,
        path: str,
        query_keys: list[str],
        body_bytes: int,
        stage: str,
        case: str,
        response_status: int,
    ) -> None:
        record = {
            "schema": 1,
            "timestamp_epoch_millis": int(time.time() * 1000),
            "method": handler.command,
            "path": path,
            "query_keys": query_keys,
            "request_body_bytes": body_bytes,
            "authorization_present": "Authorization" in handler.headers,
            "title_header_present": "X-Title" in handler.headers,
            "stage": stage,
            "case": case,
            "response_status": response_status,
        }
        line = json.dumps(record, ensure_ascii=False, sort_keys=True) + "\n"
        with self._lock:
            self._request_count += 1
            with self.evidence_path.open("a", encoding="utf-8") as handle:
                handle.write(line)


def summarize_evidence(evidence_dir: Path | str) -> dict[str, Any]:
    directory = Path(evidence_dir)
    manifest_path = directory / "manifest.json"
    evidence_path = directory / "requests.jsonl"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    records = [
        json.loads(line)
        for line in evidence_path.read_text(encoding="utf-8").splitlines()
        if line.strip()
    ]
    stage_counts = Counter(record.get("stage", "unknown") for record in records)
    case_counts = Counter(record.get("case", "unknown") for record in records)
    return {
        "schema": 1,
        "bind": manifest["bind"],
        "port": manifest["port"],
        "request_count": len(records),
        "redirect_response_count": sum(
            1 for record in records if record.get("response_status") in (301, 302, 303, 307, 308)
        ),
        "redirect_target_hits": sum(
            1 for record in records if record.get("path") == "/__harness__/redirect-target"
        ),
        "authorization_present_count": sum(
            1 for record in records if record.get("authorization_present") is True
        ),
        "stage_counts": dict(sorted(stage_counts.items())),
        "case_counts": dict(sorted(case_counts.items())),
    }


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    subcommands = parser.add_subparsers(dest="command", required=True)

    serve = subcommands.add_parser("serve", help="start the loopback provider harness")
    serve.add_argument("--bind", default="127.0.0.1")
    serve.add_argument("--port", type=int, default=18080)
    serve.add_argument(
        "--evidence-dir",
        type=Path,
        default=Path("build/security-acceptance/provider"),
    )
    serve.add_argument("--slow-duration-seconds", type=float, default=660.0)
    serve.add_argument("--slow-interval-seconds", type=float, default=5.0)

    summarize = subcommands.add_parser("summarize", help="summarize sanitized JSONL evidence")
    summarize.add_argument(
        "--evidence-dir",
        type=Path,
        default=Path("build/security-acceptance/provider"),
    )
    return parser


def main() -> int:
    args = _build_parser().parse_args()
    if args.command == "summarize":
        print(json.dumps(summarize_evidence(args.evidence_dir), indent=2, sort_keys=True))
        return 0

    config = HarnessConfig(
        bind=args.bind,
        port=args.port,
        evidence_dir=args.evidence_dir,
        slow_duration_seconds=args.slow_duration_seconds,
        slow_interval_seconds=args.slow_interval_seconds,
    )
    server = ProviderAcceptanceServer(config)
    server.start()
    print(json.dumps({
        "marker": "VILLAIGENCE_PROVIDER_HARNESS_READY",
        "base_url": server.base_url,
        "manifest": str(server.manifest_path),
        "evidence": str(server.evidence_path),
    }, sort_keys=True), flush=True)
    try:
        while True:
            time.sleep(3600.0)
    except KeyboardInterrupt:
        return 0
    finally:
        server.close()


if __name__ == "__main__":
    raise SystemExit(main())
