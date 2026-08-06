from __future__ import annotations

import unittest

from select_acceptance_suites import (
    ALL_SUITES,
    FAST_SUITE,
    PACKAGE_SUITE,
    PRODUCTION_SUITE,
    RECOVERY_SUITE,
    SERVER_SUITE,
    SelectionMode,
    select_suites,
)


class AcceptanceSuiteSelectionTest(unittest.TestCase):
    def test_release_mode_is_always_all(self) -> None:
        selection = select_suites(["docs/README.md"], mode=SelectionMode.RELEASE)

        self.assertEqual(ALL_SUITES, selection.suites)
        self.assertTrue(selection.fail_closed)
        self.assertEqual("release-mode", selection.reason)

    def test_empty_change_set_fails_closed_to_all(self) -> None:
        selection = select_suites([], mode=SelectionMode.PR)

        self.assertEqual(ALL_SUITES, selection.suites)
        self.assertTrue(selection.fail_closed)
        self.assertEqual("empty-change-set", selection.reason)

    def test_unknown_path_fails_closed_to_all(self) -> None:
        selection = select_suites(
            ["experimental/unclassified/new-runtime.txt"],
            mode=SelectionMode.PR,
        )

        self.assertEqual(ALL_SUITES, selection.suites)
        self.assertTrue(selection.fail_closed)
        self.assertIn("experimental/unclassified/new-runtime.txt", selection.reason)

    def test_documentation_only_change_runs_fast_contracts(self) -> None:
        selection = select_suites(
            ["docs/livingworld/VALIDATION_EXAMPLE.md", "docs/ROADMAP.md"],
            mode=SelectionMode.PR,
        )

        self.assertEqual(frozenset({FAST_SUITE}), selection.suites)
        self.assertFalse(selection.fail_closed)
        self.assertEqual("classified", selection.reason)

    def test_persistence_change_selects_every_mandatory_suite(self) -> None:
        selection = select_suites(
            [
                "common/src/main/java/net/conczin/mca/livingworld/persistence/JsonStoreRecovery.java",
            ],
            mode=SelectionMode.PR,
        )

        self.assertEqual(ALL_SUITES, selection.suites)
        self.assertFalse(selection.fail_closed)

    def test_voice_change_selects_real_server_without_recovery_matrix(self) -> None:
        selection = select_suites(
            [
                "common/src/main/java/net/conczin/mca/livingworld/voice/VoiceTurnCoordinator.java",
            ],
            mode=SelectionMode.PR,
        )

        self.assertEqual(
            frozenset(
                {
                    FAST_SUITE,
                    SERVER_SUITE,
                    PRODUCTION_SUITE,
                    PACKAGE_SUITE,
                }
            ),
            selection.suites,
        )
        self.assertNotIn(RECOVERY_SUITE, selection.suites)
        self.assertFalse(selection.fail_closed)

    def test_navigation_change_selects_server_terminal_state_suites(self) -> None:
        selection = select_suites(
            [
                "common/src/main/java/net/conczin/mca/entity/ai/navigation/MCAGroundPathNavigation.java",
            ],
            mode=SelectionMode.PR,
        )

        self.assertEqual(
            frozenset(
                {
                    FAST_SUITE,
                    SERVER_SUITE,
                    PRODUCTION_SUITE,
                    PACKAGE_SUITE,
                }
            ),
            selection.suites,
        )
        self.assertFalse(selection.fail_closed)

    def test_workflow_or_build_change_fails_closed_to_all(self) -> None:
        for changed_path in (
            ".github/workflows/livingworld-ci.yml",
            "fabric/build.gradle",
            "gradle.properties",
            "scripts/ci/package-livingworld-release.sh",
        ):
            with self.subTest(changed_path=changed_path):
                selection = select_suites([changed_path], mode=SelectionMode.PR)
                self.assertEqual(ALL_SUITES, selection.suites)
                self.assertTrue(selection.fail_closed)


if __name__ == "__main__":
    unittest.main()
