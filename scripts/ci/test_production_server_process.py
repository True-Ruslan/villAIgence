#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import sys
import tempfile
import unittest

from production_server_acceptance import AcceptanceError, run_server_process


SUCCESS_SCRIPT = r'''
import sys
print('[main/INFO]: Loading Minecraft 1.21.1 with Fabric Loader 0.19.3', flush=True)
print('\t- mca 1.21.1-SNAPSHOT', flush=True)
print('[Server thread/INFO]: Done (0.100s)! For help, type "help"', flush=True)
command = sys.stdin.readline().strip()
if command != 'stop':
    print('unexpected command=' + command, flush=True)
    raise SystemExit(9)
print('[Server thread/INFO]: Stopping server', flush=True)
print('[Server thread/INFO]: Saving worlds', flush=True)
print('[Server thread/INFO]: ThreadedAnvilChunkStorage: All dimensions are saved', flush=True)
'''

EARLY_EXIT_SCRIPT = r'''
print('fatal before ready', flush=True)
raise SystemExit(7)
'''


class ServerProcessTest(unittest.TestCase):
    def test_ready_process_receives_stop_and_exits_cleanly(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            log_path = root / 'server.log'

            evidence = run_server_process(
                [sys.executable, '-u', '-c', SUCCESS_SCRIPT],
                cwd=root,
                log_path=log_path,
                startup_timeout_seconds=5.0,
                shutdown_timeout_seconds=5.0,
            )

            self.assertEqual(0, evidence.exit_code)
            self.assertTrue(evidence.ready)
            self.assertTrue(evidence.stop_sent)
            self.assertGreaterEqual(evidence.duration_millis, 0)
            log = log_path.read_text(encoding='utf-8')
            self.assertIn('Done (0.100s)!', log)
            self.assertIn('All dimensions are saved', log)

    def test_process_exit_before_ready_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)

            with self.assertRaisesRegex(AcceptanceError, 'before ready'):
                run_server_process(
                    [sys.executable, '-u', '-c', EARLY_EXIT_SCRIPT],
                    cwd=root,
                    log_path=root / 'early.log',
                    startup_timeout_seconds=5.0,
                    shutdown_timeout_seconds=5.0,
                )

    def test_startup_timeout_terminates_process(self) -> None:
        script = "import time; print('not ready', flush=True); time.sleep(30)"
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)

            with self.assertRaisesRegex(AcceptanceError, 'startup timeout'):
                run_server_process(
                    [sys.executable, '-u', '-c', script],
                    cwd=root,
                    log_path=root / 'timeout.log',
                    startup_timeout_seconds=0.2,
                    shutdown_timeout_seconds=0.2,
                )


if __name__ == '__main__':
    unittest.main()
