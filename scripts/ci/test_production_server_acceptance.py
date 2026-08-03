#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import tempfile
import unittest

from production_server_acceptance import (
    AcceptanceError,
    CANONICAL_PERSISTENT_STORES,
    build_installer_command,
    build_server_command,
    collect_persistent_state,
    compare_persistent_states,
    evaluate_server_log,
    load_stage_manifest,
    prepare_server_directory,
)


class StageFixture:
    @staticmethod
    def create(stage: Path):
        installer = StageFixture.write(
            stage / "installer/fabric-installer.jar", b"installer"
        )
        candidate = StageFixture.write(
            stage / "mods/villaigence-under-test.jar", b"candidate"
        )
        fabric_api = StageFixture.write(
            stage / "mods/fabric-api.jar", b"fabric-api"
        )
        voicechat = StageFixture.write(
            stage / "mods/simple-voice-chat.jar", b"voicechat"
        )
        StageFixture.manifest(
            stage,
            installer,
            candidate,
            [fabric_api, voicechat],
        )
        return load_stage_manifest(stage)

    @staticmethod
    def manifest(
        stage: Path,
        installer: Path,
        candidate: Path,
        runtime_mods: list[Path],
    ) -> Path:
        value = {
            "schema": 1,
            "minecraftVersion": "1.21.1",
            "loaderVersion": "0.19.3",
            "installerVersion": "1.1.1",
            "installer": StageFixture.artifact(stage, installer),
            "candidate": {
                **StageFixture.artifact(stage, candidate),
                "version": "1.21.1-SNAPSHOT",
            },
            "runtimeMods": [
                StageFixture.artifact(stage, path) for path in runtime_mods
            ],
        }
        path = stage / "manifest.json"
        path.write_text(json.dumps(value), encoding="utf-8")
        return path

    @staticmethod
    def artifact(stage: Path, path: Path) -> dict[str, object]:
        return {
            "path": path.relative_to(stage).as_posix(),
            "sha256": StageFixture.sha(path),
            "size": path.stat().st_size,
        }

    @staticmethod
    def write(path: Path, value: bytes) -> Path:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(value)
        return path

    @staticmethod
    def sha(path: Path) -> str:
        return hashlib.sha256(path.read_bytes()).hexdigest()


class StageManifestTest(unittest.TestCase):
    def test_accepts_confined_verified_files(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            stage = Path(directory)
            manifest = StageFixture.create(stage)

            self.assertEqual("1.21.1", manifest.minecraft_version)
            self.assertEqual("0.19.3", manifest.loader_version)
            self.assertEqual("1.1.1", manifest.installer_version)
            self.assertEqual(
                (stage / "mods/villaigence-under-test.jar").resolve(),
                manifest.candidate.path,
            )
            self.assertEqual(2, len(manifest.runtime_mods))

    def test_rejects_path_traversal_even_when_target_exists(self) -> None:
        with tempfile.TemporaryDirectory() as parent:
            parent_path = Path(parent)
            stage = parent_path / "stage"
            stage.mkdir()
            outside = StageFixture.write(parent_path / "outside.jar", b"outside")
            installer = StageFixture.write(
                stage / "installer/fabric-installer.jar", b"installer"
            )
            candidate = StageFixture.write(
                stage / "mods/villaigence-under-test.jar", b"candidate"
            )
            runtime_mod = StageFixture.write(
                stage / "mods/fabric-api.jar", b"fabric-api"
            )
            manifest_path = StageFixture.manifest(
                stage, installer, candidate, [runtime_mod]
            )
            value = json.loads(manifest_path.read_text(encoding="utf-8"))
            value["candidate"]["path"] = "../outside.jar"
            value["candidate"]["sha256"] = StageFixture.sha(outside)
            manifest_path.write_text(json.dumps(value), encoding="utf-8")

            with self.assertRaisesRegex(AcceptanceError, "confined"):
                load_stage_manifest(stage)

    def test_rejects_checksum_mismatch(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            stage = Path(directory)
            StageFixture.create(stage)
            manifest_path = stage / "manifest.json"
            value = json.loads(manifest_path.read_text(encoding="utf-8"))
            value["candidate"]["sha256"] = "0" * 64
            manifest_path.write_text(json.dumps(value), encoding="utf-8")

            with self.assertRaisesRegex(AcceptanceError, "checksum"):
                load_stage_manifest(stage)


class ProcessBoundaryTest(unittest.TestCase):
    def test_installer_command_is_explicit_argument_vector(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest = StageFixture.create(root / "stage")
            server = (root / "server").resolve()

            command = build_installer_command(manifest, server, "java")

            self.assertEqual(
                [
                    "java",
                    "-jar",
                    str(manifest.installer.path),
                    "server",
                    "-dir",
                    str(server),
                    "-mcversion",
                    "1.21.1",
                    "-loader",
                    "0.19.3",
                    "-downloadMinecraft",
                ],
                command,
            )

    def test_server_command_is_bounded_and_uses_installed_launcher(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            server = Path(directory).resolve()

            command = build_server_command(server, "java", max_heap_mib=768)

            self.assertEqual(
                [
                    "java",
                    "-Xms256M",
                    "-Xmx768M",
                    "-Djava.awt.headless=true",
                    "-jar",
                    str(server / "fabric-server-launch.jar"),
                    "nogui",
                ],
                command,
            )

    def test_prepares_exact_mod_set_and_server_configuration(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest = StageFixture.create(root / "stage")
            server = root / "server"
            server.mkdir()

            prepare_server_directory(manifest, server)

            self.assertEqual("eula=true\n", (server / "eula.txt").read_text())
            properties = (server / "server.properties").read_text(encoding="utf-8")
            self.assertIn("online-mode=false\n", properties)
            self.assertIn("server-port=0\n", properties)
            self.assertIn("level-name=world\n", properties)
            self.assertIn("view-distance=2\n", properties)
            self.assertIn("simulation-distance=2\n", properties)
            self.assertEqual(
                {
                    "villaigence-under-test.jar",
                    "fabric-api.jar",
                    "simple-voice-chat.jar",
                },
                {path.name for path in (server / "mods").iterdir()},
            )
            self.assertEqual(
                manifest.candidate.sha256,
                StageFixture.sha(server / "mods/villaigence-under-test.jar"),
            )

    def test_rejects_nonempty_mod_directory(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest = StageFixture.create(root / "stage")
            server = root / "server"
            (server / "mods").mkdir(parents=True)
            (server / "mods/unexpected.jar").write_bytes(b"unexpected")

            with self.assertRaisesRegex(AcceptanceError, "mods directory"):
                prepare_server_directory(manifest, server)


class ServerLogOracleTest(unittest.TestCase):
    def test_accepts_production_ready_and_clean_shutdown_log(self) -> None:
        log = """
[main/INFO]: Loading Minecraft 1.21.1 with Fabric Loader 0.19.3
[main/INFO]: Loading 47 mods:
\t- mca 1.21.1-SNAPSHOT
[Server thread/INFO]: Done (4.123s)! For help, type \"help\"
[Server thread/INFO]: Stopping server
[Server thread/INFO]: Saving worlds
[Server thread/INFO]: ThreadedAnvilChunkStorage: All dimensions are saved
"""

        result = evaluate_server_log(
            log,
            minecraft_version="1.21.1",
            candidate_version="1.21.1-SNAPSHOT",
            require_shutdown=True,
        )

        self.assertEqual((), result.errors)
        self.assertTrue(result.ready)
        self.assertTrue(result.clean_shutdown)

    def test_rejects_mixin_failure_even_when_ready_marker_exists(self) -> None:
        log = """
Loading Minecraft 1.21.1 with Fabric Loader 0.19.3
\t- mca 1.21.1-SNAPSHOT
InvalidInjectionException: could not find any targets matching 'getDrops'
Done (1.000s)! For help, type \"help\"
Stopping server
ThreadedAnvilChunkStorage: All dimensions are saved
"""

        result = evaluate_server_log(
            log,
            minecraft_version="1.21.1",
            candidate_version="1.21.1-SNAPSHOT",
            require_shutdown=True,
        )

        self.assertTrue(any("InvalidInjectionException" in error for error in result.errors))

    def test_rejects_wrong_candidate_version(self) -> None:
        log = """
Loading Minecraft 1.21.1 with Fabric Loader 0.19.3
\t- mca 0.0.0-wrong
Done (1.000s)! For help, type \"help\"
Stopping server
ThreadedAnvilChunkStorage: All dimensions are saved
"""

        result = evaluate_server_log(
            log,
            minecraft_version="1.21.1",
            candidate_version="1.21.1-SNAPSHOT",
            require_shutdown=True,
        )

        self.assertTrue(any("candidate version" in error for error in result.errors))


class PersistenceEvidenceTest(unittest.TestCase):
    def test_collects_and_compares_all_canonical_stores(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            server = Path(directory)
            for index, basename in enumerate(CANONICAL_PERSISTENT_STORES):
                path = server / "world/data/villaigence" / basename
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(json.dumps({"index": index}), encoding="utf-8")

            first = collect_persistent_state(server)
            second = collect_persistent_state(server)

            self.assertEqual(set(CANONICAL_PERSISTENT_STORES), set(first))
            self.assertEqual((), compare_persistent_states(first, second))

    def test_rejects_missing_store(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            server = Path(directory)
            for basename in CANONICAL_PERSISTENT_STORES[:-1]:
                path = server / "world/data" / basename
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text("{}", encoding="utf-8")

            with self.assertRaisesRegex(AcceptanceError, "missing"):
                collect_persistent_state(server)

    def test_rejects_duplicate_basename(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            server = Path(directory)
            for basename in CANONICAL_PERSISTENT_STORES:
                path = server / "world/data" / basename
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text("{}", encoding="utf-8")
            duplicate = server / "backup" / CANONICAL_PERSISTENT_STORES[0]
            duplicate.parent.mkdir(parents=True, exist_ok=True)
            duplicate.write_text("{}", encoding="utf-8")

            with self.assertRaisesRegex(AcceptanceError, "duplicate"):
                collect_persistent_state(server)

    def test_rejects_invalid_json(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            server = Path(directory)
            for basename in CANONICAL_PERSISTENT_STORES:
                path = server / "world/data" / basename
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text("{}", encoding="utf-8")
            (server / "world/data" / CANONICAL_PERSISTENT_STORES[2]).write_text(
                "{broken",
                encoding="utf-8",
            )

            with self.assertRaisesRegex(AcceptanceError, "valid JSON"):
                collect_persistent_state(server)

    def test_reports_restart_mutation(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            server = Path(directory)
            for basename in CANONICAL_PERSISTENT_STORES:
                path = server / "world/data" / basename
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text("{}", encoding="utf-8")
            first = collect_persistent_state(server)
            (server / "world/data" / "relationships.json").write_text(
                '{"changed":true}',
                encoding="utf-8",
            )
            second = collect_persistent_state(server)

            errors = compare_persistent_states(first, second)

            self.assertTrue(any("relationships.json" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
