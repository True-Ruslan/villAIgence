from __future__ import annotations

from pathlib import Path
import tempfile
import unittest

import production_server_acceptance as base
from production_soak_acceptance import (
    MAX_SOAK_CYCLES,
    MAX_SOAK_HEAP_MIB,
    MIN_SOAK_CYCLES,
    MIN_SOAK_HEAP_MIB,
    SoakAcceptanceError,
    validate_soak_parameters,
    verify_stable_persistent_state,
)


class ProductionSoakContractTest(unittest.TestCase):
    def test_bounds_accept_five_cycles_at_512_mib(self) -> None:
        validate_soak_parameters(5, 512)

    def test_cycles_are_bounded(self) -> None:
        for cycles in (MIN_SOAK_CYCLES - 1, MAX_SOAK_CYCLES + 1):
            with self.subTest(cycles=cycles):
                with self.assertRaisesRegex(SoakAcceptanceError, "cycles"):
                    validate_soak_parameters(cycles, 512)

    def test_heap_is_bounded_to_memory_pressure_range(self) -> None:
        for heap in (MIN_SOAK_HEAP_MIB - 1, MAX_SOAK_HEAP_MIB + 1):
            with self.subTest(heap=heap):
                with self.assertRaisesRegex(SoakAcceptanceError, "heap"):
                    validate_soak_parameters(5, heap)

    def test_persistent_store_mutation_fails_soak(self) -> None:
        baseline = self.state()
        changed = dict(baseline)
        changed["memory2.json"] = base.PersistentFileEvidence(
            relative_path="world/livingworld/memory2.json",
            sha256="f" * 64,
            size=17,
            root_type="object",
        )

        with self.assertRaisesRegex(SoakAcceptanceError, "memory2.json"):
            verify_stable_persistent_state(baseline, changed, cycle=4)

    def test_stable_persistent_state_is_accepted(self) -> None:
        state = self.state()
        verify_stable_persistent_state(state, dict(state), cycle=5)

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


class ProductionSoakWorkflowPolicyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        root = Path(__file__).resolve().parents[2]
        cls.workflow = (
            root / ".github/workflows/livingworld-soak.yml"
        ).read_text(encoding="utf-8")
        cls.build = (root / "build.gradle").read_text(encoding="utf-8")

    def test_workflow_uses_bounded_restart_and_heap_parameters(self) -> None:
        self.assertIn("--cycles 5", self.workflow)
        self.assertIn("--max-heap-mib 512", self.workflow)
        self.assertIn("-Pfork_max_heap=512M", self.workflow)
        self.assertIn("timeout-minutes: 90", self.workflow)

    def test_workflow_repeats_authenticated_concurrency_scenarios(self) -> None:
        self.assertIn("AuthenticatedTextTurnAcceptanceTest", self.workflow)
        self.assertIn("OperatorLoreNetworkSessionAcceptanceTest", self.workflow)
        self.assertIn("for iteration in 1 2 3", self.workflow)
        self.assertIn("--rerun-tasks", self.workflow)

    def test_workflow_uploads_machine_readable_soak_evidence(self) -> None:
        self.assertIn("production-soak-report.json", self.workflow)
        self.assertIn("production-soak-${{ github.run_number }}", self.workflow)
        self.assertIn("if-no-files-found: error", self.workflow)

    def test_gradle_supports_explicit_fork_heap_override(self) -> None:
        self.assertIn("fork_max_heap", self.build)
        self.assertIn("task.maxHeapSize", self.build)


if __name__ == "__main__":
    unittest.main()
