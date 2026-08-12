#!/usr/bin/env python3
from __future__ import annotations

from copy import deepcopy
import os
from pathlib import Path
import tempfile
import unittest

from release_convergence import (
    ConvergenceContractError,
    EXPECTED_DEFERRED_INSTALLED_CASES,
    EXPECTED_MANUAL_CANARY_CASES,
    collect_feature_prs,
    extract_unreleased_section,
    load_contract,
    resolve_history_ref,
    validate_contract,
    validate_repository_contract,
)

REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
CONTRACT_PATH = Path("docs/releases/0.3.0-convergence.json")


class ReleaseConvergenceValidatorTest(unittest.TestCase):
    def _write_validation_root(
        self,
        root: Path,
        *,
        changelog: str,
        request: str,
    ) -> None:
        (root / "gradle.properties").write_text(
            "minecraft_version=1.21.1\n",
            encoding="utf-8",
        )
        (root / "CHANGELOG.md").write_text(changelog, encoding="utf-8")
        releases = root / "docs/releases"
        releases.mkdir(parents=True)
        (releases / "NEXT_RELEASE.txt").write_text(
            request + "\n",
            encoding="utf-8",
        )

    @staticmethod
    def _capability_inventory(contract: dict | object) -> str:
        assert isinstance(contract, dict)
        return "\n".join(
            f"- Capability from PR #{pr}."
            for pr in contract["capabilityPullRequests"]
        )

    def test_repository_contract_matches_current_release_boundary(self) -> None:
        release_workflow = os.environ.get("GITHUB_WORKFLOW") == "VillAIgence GitHub Release"
        history_ref = resolve_history_ref(
            event_name=os.environ.get("GITHUB_EVENT_NAME", ""),
            base_ref=os.environ.get("GITHUB_BASE_REF", ""),
        )
        errors = validate_repository_contract(
            REPOSITORY_ROOT,
            contract_path=CONTRACT_PATH,
            requested_tag=os.environ.get("RELEASE_VERSION", ""),
            check_history=release_workflow,
            history_ref=history_ref,
        )
        self.assertEqual((), errors)

    def test_exact_candidate_and_previous_release_are_explicit(self) -> None:
        contract = load_contract(REPOSITORY_ROOT / CONTRACT_PATH)
        self.assertEqual("0.3.0+1.21.1", contract["candidateTag"])
        self.assertEqual("1.21.1", contract["minecraftVersion"])
        self.assertEqual("0.2.0+1.21.1", contract["previousRelease"]["tag"])
        self.assertEqual(
            "e426f588efefa6aa48a6e536c4a998421bbda241",
            contract["previousRelease"]["commit"],
        )
        self.assertEqual(
            "docs/releases/NEXT_RELEASE.txt",
            contract["publicationTrigger"],
        )

    def test_post_release_capability_inventory_starts_at_123_not_127(self) -> None:
        contract = load_contract(REPOSITORY_ROOT / CONTRACT_PATH)
        self.assertEqual(
            (
                123, 125, 127, 129, 131, 133, 135, 137, 139,
                141, 143, 145, 147, 149, 151, 153, 155, 158,
            ),
            tuple(contract["capabilityPullRequests"]),
        )
        self.assertEqual((121, 122), tuple(contract["releaseInfrastructurePullRequests"]))

    def test_feature_history_parser_ignores_docs_and_keeps_feature_prs(self) -> None:
        messages = (
            "docs: reconcile release state (#124)",
            "feat: add controlled belief admission (#123)",
            "feat: add bounded player claims (#125)",
            "chore: release prep (#160)",
        )
        self.assertEqual((123, 125), collect_feature_prs(messages))

    def test_pull_request_release_history_uses_base_branch_not_synthetic_merge_head(self) -> None:
        self.assertEqual(
            "refs/remotes/origin/1.21.1",
            resolve_history_ref(event_name="pull_request", base_ref="1.21.1"),
        )

    def test_push_and_tag_release_history_use_exact_head(self) -> None:
        self.assertEqual("HEAD", resolve_history_ref(event_name="push", base_ref=""))
        self.assertEqual("HEAD", resolve_history_ref(event_name="workflow_dispatch", base_ref=""))

    def test_pull_request_history_without_base_ref_fails_closed(self) -> None:
        with self.assertRaises(ConvergenceContractError):
            resolve_history_ref(event_name="pull_request", base_ref="")

    def test_unreleased_parser_excludes_policy_header_and_released_sections(self) -> None:
        changelog = """# Changelog

## Changelog policy
PR #999 is not release inventory.

## [Unreleased]
- Capability from PR #123.
- Capability from PR #151.

## [0.2.0+1.21.1] - 2026-08-07
- Historical PR #42.
"""
        self.assertEqual(
            "- Capability from PR #123.\n- Capability from PR #151.",
            extract_unreleased_section(changelog).strip(),
        )

    def test_pre_request_stage_requires_capability_inventory_in_unreleased(self) -> None:
        contract = dict(load_contract(REPOSITORY_ROOT / CONTRACT_PATH))
        inventory = self._capability_inventory(contract)
        changelog = f"""# Changelog

## [Unreleased]
{inventory}

## [0.2.0+1.21.1] — 2026-08-07
- Previous release.
"""
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self._write_validation_root(
                root,
                changelog=changelog,
                request="0.2.0+1.21.1",
            )
            errors = validate_contract(contract, repository_root=root)
        self.assertEqual((), errors)

    def test_exact_candidate_request_moves_inventory_to_exact_release_section(self) -> None:
        contract = dict(load_contract(REPOSITORY_ROOT / CONTRACT_PATH))
        inventory = self._capability_inventory(contract)
        changelog = f"""# Changelog

## [Unreleased]
_No entries._

## [0.3.0+1.21.1] — 2026-08-12
{inventory}

## [0.2.0+1.21.1] — 2026-08-07
- Previous release.
"""
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self._write_validation_root(
                root,
                changelog=changelog,
                request="0.3.0+1.21.1",
            )
            errors = validate_contract(
                contract,
                repository_root=root,
                requested_tag="0.3.0+1.21.1",
            )
        self.assertEqual((), errors)

    def test_exact_candidate_request_rejects_missing_release_inventory(self) -> None:
        contract = dict(load_contract(REPOSITORY_ROOT / CONTRACT_PATH))
        inventory = "\n".join(
            f"- Capability from PR #{pr}."
            for pr in contract["capabilityPullRequests"][:-1]
        )
        changelog = f"""# Changelog

## [Unreleased]
_No entries._

## [0.3.0+1.21.1] — 2026-08-12
{inventory}
"""
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self._write_validation_root(
                root,
                changelog=changelog,
                request="0.3.0+1.21.1",
            )
            errors = validate_contract(
                contract,
                repository_root=root,
                requested_tag="0.3.0+1.21.1",
            )
        self.assertTrue(
            any(
                "release section 0.3.0+1.21.1 does not reference capability PR #158"
                in error
                for error in errors
            )
        )

    def test_exact_candidate_request_rejects_shipped_inventory_duplication(self) -> None:
        contract = dict(load_contract(REPOSITORY_ROOT / CONTRACT_PATH))
        inventory = self._capability_inventory(contract)
        changelog = f"""# Changelog

## [Unreleased]
- Duplicate shipped capability PR #123.

## [0.3.0+1.21.1] — 2026-08-12
{inventory}
"""
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self._write_validation_root(
                root,
                changelog=changelog,
                request="0.3.0+1.21.1",
            )
            errors = validate_contract(
                contract,
                repository_root=root,
                requested_tag="0.3.0+1.21.1",
            )
        self.assertTrue(
            any("[Unreleased] duplicates shipped capability PR #123" in error for error in errors)
        )

    def test_requested_candidate_requires_armed_candidate_trigger(self) -> None:
        contract = dict(load_contract(REPOSITORY_ROOT / CONTRACT_PATH))
        inventory = self._capability_inventory(contract)
        changelog = f"""# Changelog

## [Unreleased]
{inventory}
"""
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self._write_validation_root(
                root,
                changelog=changelog,
                request="0.2.0+1.21.1",
            )
            errors = validate_contract(
                contract,
                repository_root=root,
                requested_tag="0.3.0+1.21.1",
            )
        self.assertTrue(
            any("requested candidate requires publication trigger 0.3.0+1.21.1" in error for error in errors)
        )

    def test_unknown_publication_trigger_fails_closed(self) -> None:
        contract = dict(load_contract(REPOSITORY_ROOT / CONTRACT_PATH))
        inventory = self._capability_inventory(contract)
        changelog = f"""# Changelog

## [Unreleased]
{inventory}
"""
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self._write_validation_root(
                root,
                changelog=changelog,
                request="0.2.9+1.21.1",
            )
            errors = validate_contract(contract, repository_root=root)
        self.assertTrue(any("publication trigger must be previous release or exact candidate" in error for error in errors))

    def test_requested_tag_must_match_candidate(self) -> None:
        contract = load_contract(REPOSITORY_ROOT / CONTRACT_PATH)
        errors = validate_contract(
            contract,
            repository_root=REPOSITORY_ROOT,
            requested_tag="0.3.1+1.21.1",
        )
        self.assertTrue(any("does not match convergence candidate" in error for error in errors))

    def test_missing_recovery_store_fails_closed(self) -> None:
        contract = deepcopy(load_contract(REPOSITORY_ROOT / CONTRACT_PATH))
        contract["recoveryStores"] = contract["recoveryStores"][:-1]
        errors = validate_contract(contract, repository_root=REPOSITORY_ROOT)
        self.assertTrue(any("recoveryStores" in error for error in errors))

    def test_duplicate_capability_pr_fails_closed(self) -> None:
        contract = deepcopy(load_contract(REPOSITORY_ROOT / CONTRACT_PATH))
        contract["capabilityPullRequests"] = [123, 123]
        errors = validate_contract(contract, repository_root=REPOSITORY_ROOT)
        self.assertTrue(any("unique and sorted" in error for error in errors))

    def test_manual_and_deferred_boundaries_are_exact(self) -> None:
        contract = load_contract(REPOSITORY_ROOT / CONTRACT_PATH)
        self.assertEqual(
            EXPECTED_MANUAL_CANARY_CASES,
            tuple(contract["manualCanaryCases"]),
        )
        self.assertEqual(
            EXPECTED_DEFERRED_INSTALLED_CASES,
            tuple(contract["deferredInstalledCases"]),
        )

    def test_publication_trigger_is_previous_release_or_exact_candidate(self) -> None:
        contract = load_contract(REPOSITORY_ROOT / CONTRACT_PATH)
        request = (REPOSITORY_ROOT / "docs/releases/NEXT_RELEASE.txt").read_text(
            encoding="utf-8"
        ).strip()
        self.assertIn(
            request,
            (contract["previousRelease"]["tag"], contract["candidateTag"]),
        )


if __name__ == "__main__":
    unittest.main()
