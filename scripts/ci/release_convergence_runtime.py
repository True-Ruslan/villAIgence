#!/usr/bin/env python3
"""Patch-aware VillAIgence 0.3.x release convergence facade."""
from __future__ import annotations

import argparse
from pathlib import Path
import tempfile
from typing import Any, Mapping, Sequence

import release_convergence_baseline as baseline

ConvergenceContractError = baseline.ConvergenceContractError
EXPECTED_DEFERRED_INSTALLED_CASES = baseline.EXPECTED_DEFERRED_INSTALLED_CASES
EXPECTED_MANUAL_CANARY_CASES = baseline.EXPECTED_MANUAL_CANARY_CASES
collect_feature_prs = baseline.collect_feature_prs
extract_unreleased_section = baseline.extract_unreleased_section
load_contract = baseline.load_contract
resolve_history_ref = baseline.resolve_history_ref


def _patch_releases(contract: Mapping[str, Any]) -> tuple[tuple[str, tuple[int, ...]], ...]:
    raw = contract.get("patchReleases")
    if not isinstance(raw, list):
        raise ConvergenceContractError("patchReleases must be a list")
    result: list[tuple[str, tuple[int, ...]]] = []
    for index, item in enumerate(raw):
        if not isinstance(item, Mapping):
            raise ConvergenceContractError(f"patchReleases[{index}] must be a JSON object")
        tag = baseline._required_string(item, "tag")
        prs = baseline._required_pr_list(item, "pullRequests")
        if not prs:
            raise ConvergenceContractError(f"patchReleases[{index}].pullRequests must not be empty")
        result.append((tag, prs))
    tags = tuple(tag for tag, _ in result)
    if len(tags) != len(set(tags)):
        raise ConvergenceContractError("patchReleases must not contain duplicate tags")
    return tuple(result)


def _validate_patch_declarations(
    contract: Mapping[str, Any],
    patches: tuple[tuple[str, tuple[int, ...]], ...],
) -> tuple[int, ...]:
    candidate = baseline._required_string(contract, "candidateTag")
    minecraft = baseline._required_string(contract, "minecraftVersion")
    candidate_version = baseline._semantic_version(candidate)
    last_version = candidate_version
    patch_prs: list[int] = []
    for tag, prs in patches:
        version = baseline._semantic_version(tag)
        if version[:2] != candidate_version[:2]:
            raise ConvergenceContractError(
                f"declared patch release must remain on candidate major/minor line: {tag}"
            )
        if version <= last_version:
            raise ConvergenceContractError("patchReleases must be strictly ordered after the candidate")
        if baseline._minecraft_from_tag(tag) != minecraft:
            raise ConvergenceContractError(
                f"declared patch release Minecraft suffix does not match minecraftVersion: {tag}"
            )
        patch_prs.extend(prs)
        last_version = version
    if len(patch_prs) != len(set(patch_prs)):
        raise ConvergenceContractError("patchReleases must not reuse PRs across patch releases")
    baseline_prs = set(baseline._required_pr_list(contract, "capabilityPullRequests")) | set(
        baseline._required_pr_list(contract, "releaseInfrastructurePullRequests")
    )
    if baseline_prs & set(patch_prs):
        raise ConvergenceContractError(
            "patch release PR inventory must be disjoint from baseline capability/infrastructure PRs"
        )
    return tuple(patch_prs)


def _validate_frozen_candidate(contract: Mapping[str, Any], root: Path) -> tuple[str, ...]:
    with tempfile.TemporaryDirectory() as directory:
        shadow = Path(directory)
        (shadow / "gradle.properties").write_text(
            (root / "gradle.properties").read_text(encoding="utf-8"), encoding="utf-8"
        )
        (shadow / "CHANGELOG.md").write_text(
            (root / "CHANGELOG.md").read_text(encoding="utf-8"), encoding="utf-8"
        )
        releases = shadow / "docs/releases"
        releases.mkdir(parents=True)
        (releases / "NEXT_RELEASE.txt").write_text(
            baseline._required_string(contract, "candidateTag") + "\n", encoding="utf-8"
        )
        return baseline.validate_contract(contract, repository_root=shadow)


def validate_contract(
    contract: Mapping[str, Any],
    *,
    repository_root: Path | str,
    requested_tag: str = "",
    check_history: bool = False,
    history_ref: str = "HEAD",
) -> tuple[str, ...]:
    root = Path(repository_root).resolve()
    try:
        patches = _patch_releases(contract)
        patch_prs = _validate_patch_declarations(contract, patches)
        previous = baseline._required_mapping(contract, "previousRelease")
        previous_tag = baseline._required_string(previous, "tag")
        candidate_tag = baseline._required_string(contract, "candidateTag")
        trigger = baseline._required_string(contract, "publicationTrigger")
        publication = baseline._read_publication_request(root, trigger)
        patch_by_tag = dict(patches)

        if publication in (previous_tag, candidate_tag):
            return baseline.validate_contract(
                contract,
                repository_root=root,
                requested_tag=requested_tag,
                check_history=check_history,
                history_ref=history_ref,
            )
        if publication not in patch_by_tag:
            return ("publication trigger must be previous release, exact candidate, or declared patch release",)
        if requested_tag and requested_tag not in (candidate_tag, *patch_by_tag.keys()):
            return (
                f"requested release {requested_tag} does not match convergence candidate or declared patch release",
            )
        if requested_tag and requested_tag != publication:
            return (f"requested patch release requires publication trigger {requested_tag}",)

        baseline_errors = _validate_frozen_candidate(contract, root)
        if baseline_errors:
            return baseline_errors

        changelog = (root / "CHANGELOG.md").read_text(encoding="utf-8")
        unreleased = baseline.extract_unreleased_section(changelog)
        publication_version = baseline._semantic_version(publication)
        for patch_tag, prs in patches:
            if baseline._semantic_version(patch_tag) > publication_version:
                break
            section = baseline.extract_release_section(changelog, patch_tag)
            for pr in prs:
                if f"PR #{pr}" not in section:
                    return (f"CHANGELOG patch release section {patch_tag} does not reference PR #{pr}",)
                if f"PR #{pr}" in unreleased:
                    return (f"CHANGELOG [Unreleased] duplicates shipped patch PR #{pr}",)

        if check_history:
            previous_commit = baseline._required_string(previous, "commit").lower()
            messages = baseline._history_messages(root, previous_commit, history_ref)
            capability_prs = baseline._required_pr_list(contract, "capabilityPullRequests")
            observed_features = baseline.collect_feature_prs(messages)
            if observed_features != capability_prs:
                return (
                    "capabilityPullRequests do not match actual post-release feat: history: "
                    f"contract={capability_prs!r}, history={observed_features!r}",
                )
            observed_prs = {
                int(match.group("pr"))
                for message in messages
                if (match := baseline.PR_SUFFIX_PATTERN.search(message.splitlines()[0].strip()))
            }
            infrastructure = baseline._required_pr_list(contract, "releaseInfrastructurePullRequests")
            missing_infra = [pr for pr in infrastructure if pr not in observed_prs]
            if missing_infra:
                return (
                    "release infrastructure PRs are outside the post-release range: "
                    + ", ".join(str(pr) for pr in missing_infra),
                )
            missing_patch = [pr for pr in patch_prs if pr not in observed_prs]
            if missing_patch:
                return (
                    "patch release PRs are outside the post-release range: "
                    + ", ".join(str(pr) for pr in missing_patch),
                )
        return ()
    except (ConvergenceContractError, OSError) as exception:
        return (str(exception),)


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


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Validate VillAIgence patch-aware release convergence.")
    parser.add_argument("--check", required=True, dest="contract")
    parser.add_argument("--repository-root", default=".")
    parser.add_argument("--requested-tag", default="")
    parser.add_argument("--check-history", action="store_true")
    parser.add_argument("--history-ref", default="HEAD")
    args = parser.parse_args(argv)
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