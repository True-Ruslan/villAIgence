from __future__ import annotations

from pathlib import Path
import tempfile
import unittest

from select_acceptance_suites import (
    ALL_SUITES,
    FAST_SUITE,
    PACKAGE_SUITE,
    PRODUCTION_SUITE,
    RECOVERY_SUITE,
    SERVER_SUITE,
    SelectionMode,
    main,
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

    def test_unsafe_path_fails_closed_to_all(self) -> None:
        for changed_path in ("/runtime.java", "../runtime.java"):
            with self.subTest(changed_path=changed_path):
                selection = select_suites([changed_path], mode=SelectionMode.PR)
                self.assertEqual(ALL_SUITES, selection.suites)
                self.assertTrue(selection.fail_closed)
                self.assertIn("unsafe-path", selection.reason)

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

    def test_cli_writes_deterministic_github_outputs(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            changed_paths = root / "changed-paths.txt"
            output = root / "github-output.txt"
            changed_paths.write_text(
                "docs/PROJECT_STATE.md\ndocs/ROADMAP.md\n",
                encoding="utf-8",
            )

            exit_code = main(
                [
                    "--mode",
                    "pr",
                    "--changed-paths-file",
                    str(changed_paths),
                    "--github-output",
                    str(output),
                ]
            )

            self.assertEqual(0, exit_code)
            self.assertEqual(
                [
                    "all=false",
                    "fail_closed=false",
                    "fast=true",
                    "package=false",
                    "production=false",
                    "reason=classified",
                    "recovery=false",
                    "server=false",
                    "suites=fast",
                ],
                output.read_text(encoding="utf-8").splitlines(),
            )

    def test_cli_release_mode_ignores_narrow_changed_paths(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            changed_paths = root / "changed-paths.txt"
            output = root / "github-output.txt"
            changed_paths.write_text("docs/README.md\n", encoding="utf-8")

            exit_code = main(
                [
                    "--mode",
                    "release",
                    "--changed-paths-file",
                    str(changed_paths),
                    "--github-output",
                    str(output),
                ]
            )

            self.assertEqual(0, exit_code)
            values = dict(
                line.split("=", 1)
                for line in output.read_text(encoding="utf-8").splitlines()
            )
            self.assertEqual("true", values["all"])
            self.assertEqual("true", values["fail_closed"])
            self.assertEqual("release-mode", values["reason"])
            self.assertEqual(
                "fast,package,production,recovery,server",
                values["suites"],
            )


if __name__ == "__main__":
    unittest.main()
