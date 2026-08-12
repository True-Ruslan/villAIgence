#!/usr/bin/env python3
"""Validate the bounded VillAIgence 0.3 release-convergence contract.

This is deliberately not a release trigger. It validates the planned candidate
identity, post-release capability inventory, persistence/recovery boundary and
installed-acceptance boundary. Actual publication remains owned exclusively by
`docs/releases/NEXT_RELEASE.txt` and the existing release workflow.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import re
import subprocess
from typing import Any, Mapping, Sequence

import persistence_recovery_acceptance as recovery
import production_server_acceptance as production

CONTRACT_SCHEMA = 1
TAG_PATTERN = re.compile(
    r"^(?P<version>[0-9]+[.][0-9]+[.][0-9]+)"
    r"(?P<prerelease>-[0-9A-Za-z]+(?:[.-][0-9A-Za-z]+)*)?"
    r"[+](?P<minecraft>[0-9]+(?:[.][0-9]+){1,2})$"
)
SHA40_PATTERN = re.compile(r"^[0-9a-f]{40}$")
PR_SUFFIX_PATTERN = re.compile(r"\(#(?P<pr>[0-9]+)\)$")
SAFE_BASE_REF_PATTERN = re.compile(r"^[0-9A-Za-z._/-]+$")

EXPECTED_WORLD_STORES: tuple[str, ...] = (
    "memory2.json",
    "semantic-memory.json",
    "events.json",
    "relationships.json",
    "voices.json",
    "operator-lore.json",
    "npc-social-graph.json",
)
EXPECTED_DELIVERY_GATES: tuple[str, ...] = (
    "repository-security",
    "full-ci",
    "production-soak",
    "release-dry-run",
    "independent-review-p0-p1-p2-zero",
)
EXPECTED_MANUAL_CANARY_CASES: tuple[str, ...] = (
    "VAI-PCM-E2E-001",
    "VAI-PCM-MULTI-001",
    "VAI-PROX-MULTI-001",
    "VAI-SEC-001",
    "VAI-RESET-001",
    "VAI-STT-001",
)
EXPECTED_DEFERRED_INSTALLED_CASES: tuple[str, ...] = (
    "VAI-M2-INST-005",
    "VAI-CONCUR-004",
)


class ConvergenceContractError(RuntimeError):
    """Raised when the release-convergence contract violates a hard invariant."""


def load_contract(path: Path | str) -> Mapping[str, Any]:
    contract_path = Path(path)
    try:
        value = json.loads(contract_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exception:
        raise ConvergenceContractError(
            f"unable to read convergence contract: {contract_path}"
        ) from exception
    if not isinstance(value, Mapping):
        raise ConvergenceContractError("convergence contract root must be a JSON object")
    return value


def extract_unreleased_section(changelog: str) -> str:
    marker = "## [Unreleased]"
    start = changelog.find(marker)
    if start < 0:
        raise ConvergenceContractError("CHANGELOG is missing ## [Unreleased]")
    section_start = start + len(marker)
    next_release = changelog.find("\n## [", section_start)
    section_end = len(changelog) if next_release < 0 else next_release
    return changelog[section_start:section_end]


def resolve_history_ref(*, event_name: str, base_ref: str) -> str:
    event = event_name.strip()
    if event != "pull_request":
        return "HEAD"

    base = base_ref.strip()
    if not base:
        raise ConvergenceContractError(
            "pull_request release history requires GITHUB_BASE_REF"
        )
    if (
        base.startswith("/")
        or base.endswith("/")
        or ".." in base
        or SAFE_BASE_REF_PATTERN.fullmatch(base) is None
    ):
        raise ConvergenceContractError(
            f"pull_request base ref is unsafe or invalid: {base!r}"
        )
    return f"refs/remotes/origin/{base}"


def _required_string(value: Mapping[str, Any], field: str) -> str:
    raw = value.get(field)
    if not isinstance(raw, str) or not raw.strip():
        raise ConvergenceContractError(f"{field} must be a non-empty string")
    return raw.strip()


def _required_mapping(value: Mapping[str, Any], field: str) -> Mapping[str, Any]:
    raw = value.get(field)
    if not isinstance(raw, Mapping):
        raise ConvergenceContractError(f"{field} must be a JSON object")
    return raw


def _required_string_list(value: Mapping[str, Any], field: str) -> tuple[str, ...]:
    raw = value.get(field)
    if not isinstance(raw, list) or any(
        not isinstance(item, str) or not item.strip() for item in raw
    ):
        raise ConvergenceContractError(f"{field} must be a list of non-empty strings")
    result = tuple(item.strip() for item in raw)
    if len(result) != len(set(result)):
        raise ConvergenceContractError(f"{field} must not contain duplicates")
    return result


def _required_pr_list(value: Mapping[str, Any], field: str) -> tuple[int, ...]:
    raw = value.get(field)
    if not isinstance(raw, list) or any(
        not isinstance(item, int) or isinstance(item, bool) or item <= 0 for item in raw
    ):
        raise ConvergenceContractError(f"{field} must be a list of positive PR numbers")
    result = tuple(raw)
    if result != tuple(sorted(set(result))):
        raise ConvergenceContractError(f"{field} must be unique and sorted")
    return result


def _configured_minecraft_version(root: Path) -> str:
    properties = root / "gradle.properties"
    try:
        for line in properties.read_text(encoding="utf-8").splitlines():
            if line.startswith("minecraft_version="):
                value = line.split("=", 1)[1].strip()
                if value:
                    return value
    except OSError as exception:
        raise ConvergenceContractError("unable to read gradle.properties") from exception
    raise ConvergenceContractError("gradle.properties does not define minecraft_version")


def _semantic_version(tag: str) -> tuple[int, int, int]:
    match = TAG_PATTERN.fullmatch(tag)
    if match is None:
        raise ConvergenceContractError(f"invalid release tag: {tag}")
    return tuple(int(part) for part in match.group("version").split("."))


def _minecraft_from_tag(tag: str) -> str:
    match = TAG_PATTERN.fullmatch(tag)
    if match is None:
        raise ConvergenceContractError(f"invalid release tag: {tag}")
    return match.group("minecraft")


def _recovery_stores() -> tuple[str, ...]:
    stores = tuple(case.store for case in recovery.RECOVERY_CASES)
    if len(stores) != len(set(stores)):
        raise ConvergenceContractError("recovery matrix contains duplicate stores")
    return stores


def collect_feature_prs(messages: Sequence[str]) -> tuple[int, ...]:
    result: list[int] = []
    for message in messages:
        subject = message.splitlines()[0].strip()
        if not subject.startswith("feat:"):
            continue
        match = PR_SUFFIX_PATTERN.search(subject)
        if match is None:
            raise ConvergenceContractError(
                f"post-release feature commit has no PR suffix: {subject}"
            )
        result.append(int(match.group("pr")))
    return tuple(sorted(set(result)))


def _history_messages(
    root: Path,
    previous_release_commit: str,
    history_ref: str,
) -> tuple[str, ...]:
    try:
        verify = subprocess.run(
            ["git", "rev-parse", "--verify", history_ref],
            cwd=root,
            check=False,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
        if verify.returncode != 0:
            raise ConvergenceContractError(
                f"release history ref is unavailable: {history_ref}"
            )
        ancestor = subprocess.run(
            ["git", "merge-base", "--is-ancestor", previous_release_commit, history_ref],
            cwd=root,
            check=False,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
    except OSError as exception:
        raise ConvergenceContractError("unable to execute git") from exception
    if ancestor.returncode != 0:
        raise ConvergenceContractError(
            "previous release commit is unavailable or is not an ancestor of "
            + history_ref
        )
    completed = subprocess.run(
        ["git", "log", "--format=%s", f"{previous_release_commit}..{history_ref}"],
        cwd=root,
        check=False,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
    )
    if completed.returncode != 0:
        raise ConvergenceContractError(
            "unable to inspect post-release git history: " + completed.stderr.strip()
        )
    return tuple(line for line in completed.stdout.splitlines() if line.strip())


def validate_contract(
    contract: Mapping[str, Any],
    *,
    repository_root: Path | str,
    requested_tag: str = "",
    check_history: bool = False,
    history_ref: str = "HEAD",
) -> tuple[str, ...]:
    root = Path(repository_root).resolve()
    errors: list[str] = []

    try:
        schema = contract.get("schema")
        if schema != CONTRACT_SCHEMA:
            raise ConvergenceContractError(
                f"schema must be {CONTRACT_SCHEMA}, got {schema!r}"
            )

        candidate_tag = _required_string(contract, "candidateTag")
        minecraft_version = _required_string(contract, "minecraftVersion")
        previous = _required_mapping(contract, "previousRelease")
        previous_tag = _required_string(previous, "tag")
        previous_commit = _required_string(previous, "commit").lower()
        publication_trigger = _required_string(contract, "publicationTrigger")
        capability_prs = _required_pr_list(contract, "capabilityPullRequests")
        infrastructure_prs = _required_pr_list(
            contract, "releaseInfrastructurePullRequests"
        )
        world_stores = _required_string_list(contract, "worldStores")
        recovery_stores = _required_string_list(contract, "recoveryStores")
        delivery_gates = _required_string_list(contract, "deliveryGates")
        manual_cases = _required_string_list(contract, "manualCanaryCases")
        deferred_cases = _required_string_list(contract, "deferredInstalledCases")
        excluded_scope = _required_string_list(contract, "excludedScope")
        migration = _required_mapping(contract, "migrationPolicy")

        if TAG_PATTERN.fullmatch(candidate_tag) is None:
            raise ConvergenceContractError("candidateTag has invalid release-tag format")
        if TAG_PATTERN.fullmatch(previous_tag) is None:
            raise ConvergenceContractError("previousRelease.tag has invalid release-tag format")
        if not SHA40_PATTERN.fullmatch(previous_commit):
            raise ConvergenceContractError("previousRelease.commit must be a lowercase 40-hex SHA")
        if minecraft_version != _configured_minecraft_version(root):
            raise ConvergenceContractError(
                "minecraftVersion does not match gradle.properties"
            )
        if _minecraft_from_tag(candidate_tag) != minecraft_version:
            raise ConvergenceContractError(
                "candidateTag Minecraft suffix does not match minecraftVersion"
            )
        if _minecraft_from_tag(previous_tag) != minecraft_version:
            raise ConvergenceContractError(
                "previous release Minecraft suffix does not match minecraftVersion"
            )
        if _semantic_version(candidate_tag) <= _semantic_version(previous_tag):
            raise ConvergenceContractError(
                "candidate semantic version must be newer than previous release"
            )
        if publication_trigger != "docs/releases/NEXT_RELEASE.txt":
            raise ConvergenceContractError(
                "publicationTrigger must remain docs/releases/NEXT_RELEASE.txt"
            )
        if requested_tag and requested_tag != candidate_tag:
            raise ConvergenceContractError(
                f"requested release {requested_tag} does not match convergence candidate {candidate_tag}"
            )
        if world_stores != EXPECTED_WORLD_STORES:
            raise ConvergenceContractError(
                f"worldStores must exactly match {EXPECTED_WORLD_STORES!r}"
            )
        canonical_recovery = tuple(production.CANONICAL_PERSISTENT_STORES)
        if recovery_stores != canonical_recovery:
            raise ConvergenceContractError(
                "recoveryStores do not match production canonical persistent stores"
            )
        if set(recovery_stores) != set(_recovery_stores()):
            raise ConvergenceContractError(
                "recoveryStores do not match destructive recovery matrix cases"
            )
        if delivery_gates != EXPECTED_DELIVERY_GATES:
            raise ConvergenceContractError(
                f"deliveryGates must exactly match {EXPECTED_DELIVERY_GATES!r}"
            )
        if manual_cases != EXPECTED_MANUAL_CANARY_CASES:
            raise ConvergenceContractError(
                "manualCanaryCases do not match the current unavoidable manual catalog"
            )
        if deferred_cases != EXPECTED_DEFERRED_INSTALLED_CASES:
            raise ConvergenceContractError(
                "deferredInstalledCases must keep the two explicit installed deferrals"
            )
        if not excluded_scope:
            raise ConvergenceContractError("excludedScope must not be empty")

        if migration.get("cleanStateBoundary") is not True:
            raise ConvergenceContractError("cleanStateBoundary must remain true")
        if migration.get("migrationRequired") is not False:
            raise ConvergenceContractError("migrationRequired must remain false")
        if migration.get("legacyConversationMigration") != "cancelled-by-design":
            raise ConvergenceContractError(
                "legacyConversationMigration must remain cancelled-by-design"
            )

        if set(capability_prs) & set(infrastructure_prs):
            raise ConvergenceContractError(
                "capability and release infrastructure PR inventories must be disjoint"
            )

        changelog = (root / "CHANGELOG.md").read_text(encoding="utf-8")
        unreleased = extract_unreleased_section(changelog)
        for pr in capability_prs:
            if f"PR #{pr}" not in unreleased:
                raise ConvergenceContractError(
                    f"CHANGELOG [Unreleased] does not reference capability PR #{pr}"
                )

        if check_history:
            messages = _history_messages(root, previous_commit, history_ref)
            observed_features = collect_feature_prs(messages)
            if observed_features != capability_prs:
                raise ConvergenceContractError(
                    "capabilityPullRequests do not match actual post-release feat: history: "
                    f"contract={capability_prs!r}, history={observed_features!r}"
                )
            observed_prs = {
                int(match.group("pr"))
                for message in messages
                if (match := PR_SUFFIX_PATTERN.search(message.splitlines()[0].strip()))
            }
            missing_infrastructure = [
                pr for pr in infrastructure_prs if pr not in observed_prs
            ]
            if missing_infrastructure:
                raise ConvergenceContractError(
                    "release infrastructure PRs are outside the post-release range: "
                    + ", ".join(str(pr) for pr in missing_infrastructure)
                )
    except (ConvergenceContractError, OSError) as exception:
        errors.append(str(exception))

    return tuple(errors)


def validate_repository_contract(
    repository_root: Path | str,
    *,
    contract_path: Path | str,
    requested_tag: str = "",
    check_history: bool = False,
    history_ref: str = "HEAD",
) -> tuple[str, ...]:
    root = Path(repository_root).resolve()
    path = Path(contract_path)
    if not path.is_absolute():
        path = root / path
    try:
        contract = load_contract(path)
    except ConvergenceContractError as exception:
        return (str(exception),)
    return validate_contract(
        contract,
        repository_root=root,
        requested_tag=requested_tag.strip(),
        check_history=check_history,
        history_ref=history_ref,
    )


def _parse_args(argv: Sequence[str] | None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Validate VillAIgence release-convergence state without publishing."
    )
    parser.add_argument("--check", required=True, dest="contract")
    parser.add_argument("--repository-root", default=".")
    parser.add_argument("--requested-tag", default="")
    parser.add_argument("--check-history", action="store_true")
    parser.add_argument("--history-ref", default="HEAD")
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = _parse_args(argv)
    errors = validate_repository_contract(
        args.repository_root,
        contract_path=args.contract,
        requested_tag=args.requested_tag,
        check_history=args.check_history,
        history_ref=args.history_ref,
    )
    if errors:
        for error in errors:
            print(f"release convergence error: {error}")
        return 1
    print(f"Release convergence contract passed: {args.contract}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
