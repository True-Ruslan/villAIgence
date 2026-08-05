#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path
import tempfile
import unittest

from production_lifecycle_acceptance import (
    AcceptanceError,
    collect_lifecycle_history,
    collect_lifecycle_state,
    compare_lifecycle_states,
)


FIXTURE_UUID = "5cf53206-ec2c-4c88-ad11-a8bbc56f514e"
FIXTURE_NAME = "Production Lifecycle Acceptance"
FIXTURE_INVENTORY = {
    "minecraft:bread": 11,
    "minecraft:emerald": 3,
    "minecraft:iron_sword": 1,
}


class ProductionLifecycleEvidenceTest(unittest.TestCase):
    def test_accepts_created_then_restart_verified_evidence(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            server = Path(directory)
            self.write_evidence(server, "CREATED")
            first = collect_lifecycle_state(server)
            self.write_evidence(server, "RESTART_VERIFIED")
            second = collect_lifecycle_state(server)

            self.assertEqual((), compare_lifecycle_states(first, second))
            self.assertEqual(FIXTURE_UUID, second["npcUuid"])
            self.assertEqual(FIXTURE_INVENTORY, second["inventory"])

    def test_accepts_exact_two_snapshot_history(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            server = Path(directory)
            created = self.state("CREATED")
            restarted = self.state("RESTART_VERIFIED")
            self.write_raw(server, restarted | {"history": [created, restarted]})

            first, second = collect_lifecycle_history(server)

            self.assertEqual("CREATED", first["phase"])
            self.assertEqual("RESTART_VERIFIED", second["phase"])

    def test_rejects_missing_lifecycle_evidence(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaisesRegex(AcceptanceError, "lifecycle evidence"):
                collect_lifecycle_state(Path(directory))

    def test_rejects_invalid_lifecycle_schema(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            server = Path(directory)
            self.write_evidence(server, "CREATED", schema=2)

            with self.assertRaisesRegex(AcceptanceError, "schema"):
                collect_lifecycle_state(server)

    def test_rejects_duplicate_or_missing_inventory(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            server = Path(directory)
            self.write_evidence(
                server,
                "CREATED",
                inventory={"minecraft:emerald": 6},
                non_empty_stacks=2,
            )

            with self.assertRaisesRegex(AcceptanceError, "inventory"):
                collect_lifecycle_state(server)

    def test_reports_identity_or_count_mutation_across_restart(self) -> None:
        first = self.state("CREATED")
        second = self.state("RESTART_VERIFIED")
        second["npcUuid"] = "11111111-1111-1111-1111-111111111111"
        second["liveEntityCount"] = 2

        errors = compare_lifecycle_states(first, second)

        self.assertTrue(any("npcUuid" in error for error in errors))
        self.assertTrue(any("liveEntityCount" in error for error in errors))

    def test_rejects_invalid_phase_progression(self) -> None:
        first = self.state("RESTART_VERIFIED")
        second = self.state("RESTART_VERIFIED")

        errors = compare_lifecycle_states(first, second)

        self.assertTrue(any("phase" in error for error in errors))

    def test_rejects_history_whose_top_level_differs_from_second_snapshot(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            server = Path(directory)
            created = self.state("CREATED")
            restarted = self.state("RESTART_VERIFIED")
            top_level = dict(restarted)
            top_level["npcName"] = "Wrong top-level name"
            self.write_raw(server, top_level | {"history": [created, restarted]})

            with self.assertRaisesRegex(AcceptanceError, "npcName|top-level"):
                collect_lifecycle_history(server)

    @classmethod
    def state(
        cls,
        phase: str,
        *,
        schema: int = 1,
        npc_uuid: str = FIXTURE_UUID,
        inventory: dict[str, int] | None = None,
        non_empty_stacks: int = 3,
        live_entity_count: int = 1,
    ) -> dict[str, object]:
        return {
            "schema": schema,
            "phase": phase,
            "npcUuid": npc_uuid,
            "npcName": FIXTURE_NAME,
            "inventory": inventory or FIXTURE_INVENTORY,
            "nonEmptyStacks": non_empty_stacks,
            "liveEntityCount": live_entity_count,
            "portableGraveConsumed": True,
        }

    @classmethod
    def write_evidence(
        cls,
        server: Path,
        phase: str,
        **kwargs: object,
    ) -> None:
        cls.write_raw(server, cls.state(phase, **kwargs))

    @staticmethod
    def write_raw(server: Path, value: dict[str, object]) -> None:
        path = server / "world/livingworld/acceptance-lifecycle.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(
            json.dumps(value, sort_keys=True),
            encoding="utf-8",
        )


if __name__ == "__main__":
    unittest.main()
