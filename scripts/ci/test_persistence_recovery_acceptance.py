#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

import production_server_acceptance as base
from persistence_recovery_acceptance import (
    RECOVERY_CASES,
    RECOVERY_MODE_PROPERTY,
    RecoveryAcceptanceError,
    apply_corruption,
    compare_unaffected_stores,
    recovery_server_command,
)
from recovery_fixture_stage import (
    RECOVERY_ENTRYPOINT,
    prepare_recovery_fixture_stage,
)


class RecoveryMatrixContractTest(unittest.TestCase):
    def test_matrix_covers_each_canonical_store_once(self) -> None:
        stores = [case.store for case in RECOVERY_CASES]

        self.assertEqual(list(base.CANONICAL_PERSISTENT_STORES), stores)
        self.assertEqual(len(stores), len(set(stores)))
        self.assertTrue(all(case.payload is not None for case in RECOVERY_CASES))

    def test_matrix_includes_npc_social_graph_as_sixth_canonical_store(self) -> None:
        self.assertEqual(6, len(base.CANONICAL_PERSISTENT_STORES))
        self.assertEqual("npc-social-graph.json", base.CANONICAL_PERSISTENT_STORES[-1])
        self.assertEqual(6, len(RECOVERY_CASES))
        graph_case = RECOVERY_CASES[-1]
        self.assertEqual("npc-social-graph.json", graph_case.store)
        self.assertEqual("INCOMPATIBLE_SCHEMA_CANONICAL", graph_case.variant)
        self.assertEqual("canonical", graph_case.mutation_target)
        self.assertEqual(".corrupt", graph_case.expected_backup)

    def test_recovery_mode_property_is_inserted_before_jar(self) -> None:
        command = ["java", "-Xmx768M", "-jar", "/server/launcher.jar", "nogui"]

        result = recovery_server_command(command)

        self.assertEqual(
            [
                "java",
                "-Xmx768M",
                RECOVERY_MODE_PROPERTY,
                "-jar",
                "/server/launcher.jar",
                "nogui",
            ],
            result,
        )

    def test_rejects_command_without_jar_boundary(self) -> None:
        with self.assertRaisesRegex(RecoveryAcceptanceError, "missing -jar"):
            recovery_server_command(["java", "launcher.jar"])

    def test_canonical_corruption_replaces_exact_store_bytes(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            server = self.server(Path(directory))
            case = RECOVERY_CASES[0]

            evidence = apply_corruption(server, case)

            canonical = server / "world/livingworld" / case.store
            self.assertEqual(case.payload, canonical.read_bytes())
            self.assertEqual(
                f"world/livingworld/{case.store}", evidence.mutation_path
            )
            self.assertEqual(
                f"world/livingworld/{case.store}.corrupt",
                evidence.expected_backup_path,
            )

    def test_stale_temp_preserves_canonical_before_server_recovery(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            server = self.server(Path(directory))
            case = next(
                value for value in RECOVERY_CASES
                if value.variant == "STALE_VALID_TEMP_WITH_CANONICAL"
            )
            canonical = server / "world/livingworld" / case.store
            canonical_before = canonical.read_bytes()

            evidence = apply_corruption(server, case)

            self.assertEqual(canonical_before, canonical.read_bytes())
            self.assertEqual(
                case.payload,
                (server / evidence.mutation_path).read_bytes(),
            )
            self.assertIsNone(evidence.expected_backup_path)

    def test_orphan_temp_removes_canonical_and_targets_temp_backup(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            server = self.server(Path(directory))
            case = next(
                value for value in RECOVERY_CASES
                if value.variant == "INVALID_ORPHAN_TEMP_WITHOUT_CANONICAL"
            )

            evidence = apply_corruption(server, case)

            canonical = server / "world/livingworld" / case.store
            self.assertFalse(canonical.exists())
            self.assertEqual(case.payload, (server / evidence.mutation_path).read_bytes())
            self.assertEqual(
                f"world/livingworld/{case.store}.tmp.corrupt",
                evidence.expected_backup_path,
            )

    def test_unaffected_store_comparison_reports_only_sibling_mutation(self) -> None:
        baseline = self.state()
        recovered = dict(baseline)
        recovered["relationships.json"] = base.PersistentFileEvidence(
            relative_path="world/livingworld/relationships.json",
            sha256="f" * 64,
            size=9,
            root_type="object",
        )

        errors = compare_unaffected_stores(
            baseline,
            recovered,
            "memory2.json",
        )

        self.assertEqual(1, len(errors))
        self.assertIn("relationships.json", errors[0])

    def test_recovery_stage_rewrites_only_fixture_entrypoint_and_manifest_hash(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            stage = self.stage(Path(directory))
            candidate = stage / "mods/villaigence-under-test.jar"
            candidate_before = candidate.read_bytes()

            evidence = prepare_recovery_fixture_stage(stage)

            fixture = stage / evidence["path"]
            with zipfile.ZipFile(fixture) as archive:
                metadata = json.loads(archive.read("fabric.mod.json"))
            manifest = json.loads((stage / "manifest.json").read_text())
            fixture_entry = next(
                entry for entry in manifest["runtimeMods"]
                if entry["id"] == "mca-production-acceptance-fixture"
            )
            self.assertEqual(
                [RECOVERY_ENTRYPOINT],
                metadata["entrypoints"]["main"],
            )
            self.assertEqual(self.sha(fixture), fixture_entry["sha256"])
            self.assertEqual(fixture.stat().st_size, fixture_entry["size"])
            self.assertEqual(candidate_before, candidate.read_bytes())
            self.assertNotEqual(
                evidence["originalSha256"], evidence["derivedSha256"]
            )

    def test_recovery_stage_derivation_is_idempotent(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            stage = self.stage(Path(directory))

            first = prepare_recovery_fixture_stage(stage)
            first_sha = self.sha(stage / first["path"])
            second = prepare_recovery_fixture_stage(stage)

            self.assertEqual(first_sha, self.sha(stage / second["path"]))
            self.assertEqual(
                [RECOVERY_ENTRYPOINT], second["recoveryMainEntrypoints"]
            )

    @staticmethod
    def server(root: Path) -> Path:
        living_world = root / "world/livingworld"
        living_world.mkdir(parents=True)
        for store in base.CANONICAL_PERSISTENT_STORES:
            (living_world / store).write_text(
                '{"version":1}',
                encoding="utf-8",
            )
        return root

    @classmethod
    def stage(cls, root: Path) -> Path:
        stage = root / "stage"
        mods = stage / "mods"
        mods.mkdir(parents=True)
        candidate = mods / "villaigence-under-test.jar"
        candidate.write_bytes(b"candidate")
        fixture = mods / "mca-production-acceptance-fixture-1.0.0.jar"
        metadata = {
            "schemaVersion": 1,
            "id": "mca-production-acceptance-fixture",
            "version": "1.0.0",
            "entrypoints": {
                "main": [
                    "net.conczin.mca.acceptancefixture.ProductionAcceptanceFixture",
                    RECOVERY_ENTRYPOINT,
                ]
            },
        }
        with zipfile.ZipFile(fixture, "w") as archive:
            archive.writestr(
                "fabric.mod.json",
                json.dumps(metadata, sort_keys=True),
            )
            archive.writestr(
                RECOVERY_ENTRYPOINT.replace(".", "/") + ".class",
                b"class",
            )
        manifest = {
            "schema": 1,
            "runtimeMods": [
                {
                    "id": "mca-production-acceptance-fixture",
                    "version": "1.0.0",
                    "path": fixture.relative_to(stage).as_posix(),
                    "sha256": cls.sha(fixture),
                    "size": fixture.stat().st_size,
                }
            ],
        }
        (stage / "manifest.json").write_text(
            json.dumps(manifest, sort_keys=True),
            encoding="utf-8",
        )
        return stage

    @staticmethod
    def state() -> dict[str, base.PersistentFileEvidence]:
        return {
            store: base.PersistentFileEvidence(
                relative_path=f"world/livingworld/{store}",
                sha256=f"{index:064x}"[-64:],
                size=13,
                root_type="object",
            )
            for index, store in enumerate(base.CANONICAL_PERSISTENT_STORES, start=1)
        }

    @staticmethod
    def sha(path: Path) -> str:
        return hashlib.sha256(path.read_bytes()).hexdigest()


if __name__ == "__main__":
    unittest.main()
