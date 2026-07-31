#!/usr/bin/env python3
"""Standard-library contract tests for the local provider acceptance harness."""

from __future__ import annotations

import http.client
import importlib.util
import json
from pathlib import Path
import tempfile
import time
import unittest

ROOT = Path(__file__).resolve().parents[2]
HARNESS_PATH = ROOT / "scripts" / "security" / "provider_acceptance_harness.py"
SPEC = importlib.util.spec_from_file_location("provider_acceptance_harness", HARNESS_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError(f"Cannot load harness module from {HARNESS_PATH}")
HARNESS = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(HARNESS)


class ProviderAcceptanceHarnessTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        limits = HARNESS.ResponseLimits(
            chat_json_bytes=16,
            stt_json_bytes=12,
            tts_audio_bytes=20,
            error_body_bytes=8,
            verification_json_bytes=10,
        )
        config = HARNESS.HarnessConfig(
            bind="127.0.0.1",
            port=0,
            evidence_dir=Path(self.temp.name),
            slow_duration_seconds=0.08,
            slow_interval_seconds=0.01,
            limits=limits,
        )
        self.server = HARNESS.ProviderAcceptanceServer(config)
        self.server.start()

    def tearDown(self) -> None:
        self.server.close()
        self.temp.cleanup()

    def request(
        self,
        method: str,
        path: str,
        *,
        headers: dict[str, str] | None = None,
        body: bytes = b"{}",
    ) -> tuple[int, dict[str, str], bytes]:
        connection = http.client.HTTPConnection(
            "127.0.0.1",
            self.server.port,
            timeout=3,
        )
        try:
            connection.request(method, path, body=body, headers=headers or {})
            response = connection.getresponse()
            response_headers = {key.lower(): value for key, value in response.getheaders()}
            try:
                response_body = response.read()
            except http.client.IncompleteRead as error:
                response_body = error.partial
            return response.status, response_headers, response_body
        finally:
            connection.close()

    def test_rejects_non_loopback_bind_addresses(self) -> None:
        self.assertEqual("127.0.0.1", HARNESS.validate_loopback_bind("127.0.0.1"))
        self.assertEqual("::1", HARNESS.validate_loopback_bind("::1"))
        for unsafe in ("0.0.0.0", "192.168.1.10", "8.8.8.8", "localhost"):
            with self.subTest(unsafe=unsafe):
                with self.assertRaises(ValueError):
                    HARNESS.validate_loopback_bind(unsafe)

    def test_declared_oversize_advertises_limit_plus_one(self) -> None:
        status, headers, body = self.request(
            "POST",
            "/v1/chat/completions/declared-oversize",
        )

        self.assertEqual(200, status)
        self.assertEqual("17", headers["content-length"])
        self.assertLessEqual(len(body), 1)

    def test_chunked_and_error_routes_cross_exact_limits(self) -> None:
        status, headers, body = self.request(
            "POST",
            "/v1/chat/completions/chunked-oversize",
        )
        self.assertEqual(200, status)
        self.assertEqual("chunked", headers["transfer-encoding"].lower())
        self.assertEqual(17, len(body))

        status, headers, body = self.request(
            "POST",
            "/v1/chat/completions/error-oversize",
        )
        self.assertEqual(500, status)
        self.assertEqual("chunked", headers["transfer-encoding"].lower())
        self.assertEqual(9, len(body))

    def test_redirect_is_not_followed_by_basic_client(self) -> None:
        status, headers, _ = self.request(
            "POST",
            "/v1/chat/completions/redirect",
        )

        self.assertEqual(307, status)
        self.assertEqual("/__harness__/redirect-target", headers["location"])
        self.assertEqual(0, self.server.snapshot()["redirect_target_hits"])

    def test_slow_drip_streams_for_configured_duration(self) -> None:
        started = time.monotonic()
        status, headers, body = self.request(
            "POST",
            "/v1/chat/completions/slow-drip",
        )
        elapsed = time.monotonic() - started

        self.assertEqual(200, status)
        self.assertEqual("chunked", headers["transfer-encoding"].lower())
        self.assertGreaterEqual(elapsed, 0.05)
        self.assertGreaterEqual(len(body), 4)

    def test_evidence_redacts_header_and_query_values(self) -> None:
        secret = "Bearer NEVER_WRITE_THIS_VALUE"
        email = "private-address@example.invalid"
        status, _, _ = self.request(
            "GET",
            "/v1/mca/verify/success?email=" + email + "&player=Ruslan",
            headers={"Authorization": secret, "X-Title": "private title"},
            body=b"",
        )
        self.assertEqual(200, status)

        records = [
            json.loads(line)
            for line in self.server.evidence_path.read_text(encoding="utf-8").splitlines()
            if line.strip()
        ]
        serialized = json.dumps(records, ensure_ascii=False)
        self.assertNotIn(secret, serialized)
        self.assertNotIn(email, serialized)
        self.assertNotIn("private title", serialized)
        self.assertTrue(records[-1]["authorization_present"])
        self.assertTrue(records[-1]["title_header_present"])
        self.assertEqual(["email", "player"], records[-1]["query_keys"])

    def test_manifest_and_summary_are_machine_readable(self) -> None:
        self.request("POST", "/v1/audio/transcriptions/ok")
        self.request("POST", "/v1/audio/speech/redirect")

        manifest = json.loads(self.server.manifest_path.read_text(encoding="utf-8"))
        summary = HARNESS.summarize_evidence(Path(self.temp.name))

        self.assertEqual(1, manifest["schema"])
        self.assertEqual("127.0.0.1", manifest["bind"])
        self.assertEqual(self.server.port, manifest["port"])
        self.assertEqual(2, summary["request_count"])
        self.assertEqual(1, summary["redirect_response_count"])
        self.assertEqual(0, summary["redirect_target_hits"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
