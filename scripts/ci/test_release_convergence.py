#!/usr/bin/env python3
from __future__ import annotations

from copy import deepcopy
import os
from pathlib import Path
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

    def test_convergence_does_not_arm_publication_request(self) -> None:
        request = (REPOSITORY_ROOT / "docs/releases/NEXT_RELEASE.txt").read_text(
            encoding="utf-8"
        ).strip()
        self.assertEqual("0.2.0+1.21.1", request)


if __name__ == "__main__":
    unittest.main()
