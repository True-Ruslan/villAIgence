#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path
import tempfile
import unittest

from production_server_acceptance import (
    AcceptanceError,
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
        with tempfile.TemporaryDirectory() as directory:
            server = Path(directory)
            self.write_evidence(server, "CREATED")
            first = collect_lifecycle_state(server)
            self.write_evidence(
                server,
                "RESTART_VERIFIED",
                npc_uuid="11111111-1111-1111-1111-111111111111",
                live_entity_count=2,
            )
            second = collect_lifecycle_state(server)

            errors = compare_lifecycle_states(first, second)

            self.assertTrue(any("npcUuid" in error for error in errors))
            self.assertTrue(any("liveEntityCount" in error for error in errors))

    def test_rejects_invalid_phase_progression(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            server = Path(directory)
            self.write_evidence(server, "RESTART_VERIFIED")
            first = collect_lifecycle_state(server)
            self.write_evidence(server, "RESTART_VERIFIED")
            second = collect_lifecycle_state(server)

            errors = compare_lifecycle_states(first, second)

            self.assertTrue(any("phase" in error for error in errors))

    @staticmethod
    def write_evidence(
        server: Path,
        phase: str,
        *,
        schema: int = 1,
        npc_uuid: str = FIXTURE_UUID,
        inventory: dict[str, int] | None = None,
        non_empty_stacks: int = 3,
        live_entity_count: int = 1,
    ) -> None:
        path = server / "world/livingworld/acceptance-lifecycle.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(
            json.dumps(
                {
                    "schema": schema,
                    "phase": phase,
                    "npcUuid": npc_uuid,
                    "npcName": FIXTURE_NAME,
                    "inventory": inventory or FIXTURE_INVENTORY,
                    "nonEmptyStacks": non_empty_stacks,
                    "liveEntityCount": live_entity_count,
                    "portableGraveConsumed": True,
                },
                sort_keys=True,
            ),
            encoding="utf-8",
        )


if __name__ == "__main__":
    unittest.main()
