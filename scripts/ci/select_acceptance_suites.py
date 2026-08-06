#!/usr/bin/env python3
from __future__ import annotations

from dataclasses import dataclass
from enum import Enum
from pathlib import PurePosixPath
from typing import Iterable

FAST_SUITE = "fast"
SERVER_SUITE = "server"
PRODUCTION_SUITE = "production"
RECOVERY_SUITE = "recovery"
PACKAGE_SUITE = "package"

ALL_SUITES = frozenset(
    {
        FAST_SUITE,
        SERVER_SUITE,
        PRODUCTION_SUITE,
        RECOVERY_SUITE,
        PACKAGE_SUITE,
    }
)
RUNTIME_SUITES = frozenset(
    {
        FAST_SUITE,
        SERVER_SUITE,
        PRODUCTION_SUITE,
        PACKAGE_SUITE,
    }
)


class SelectionMode(str, Enum):
    PR = "pr"
    PUSH = "push"
    RELEASE = "release"


@dataclass(frozen=True)
class Selection:
    suites: frozenset[str]
    fail_closed: bool
    reason: str


def _normalize_path(raw_path: str) -> str | None:
    value = raw_path.strip().replace("\\", "/")
    if not value:
        return None
    path = PurePosixPath(value)
    if path.is_absolute() or ".." in path.parts:
        raise ValueError(value)
    normalized = path.as_posix()
    if normalized in {"", "."}:
        return None
    return normalized


def _is_documentation(path: str) -> bool:
    return (
        path.startswith("docs/")
        or path in {"README.md", "LICENSE", "CODE_OF_CONDUCT.md"}
    )


def _is_persistence_runtime(path: str) -> bool:
    return (
        path.startswith(
            "common/src/main/java/net/conczin/mca/livingworld/persistence/"
        )
        or path.startswith(
            "common/src/test/java/net/conczin/mca/livingworld/persistence/"
        )
    )


def _is_voice_runtime(path: str) -> bool:
    prefixes = (
        "common/src/main/java/net/conczin/mca/livingworld/voice/",
        "common/src/main/java/net/conczin/mca/livingworld/audio/",
        "common/src/main/java/net/conczin/mca/voice/",
        "common/src/test/java/net/conczin/mca/livingworld/voice/",
        "common/src/test/java/net/conczin/mca/livingworld/audio/",
    )
    return path.startswith(prefixes)


def _is_navigation_or_gameplay(path: str) -> bool:
    prefixes = (
        "common/src/main/java/net/conczin/mca/entity/ai/navigation/",
        "common/src/main/java/net/conczin/mca/entity/ai/brain/tasks/",
        "fabric/src/gametest/java/net/conczin/mca/gametest/Navigation",
        "fabric/src/gametest/java/net/conczin/mca/gametest/Gameplay",
        "fabric/src/gametest/java/net/conczin/mca/gametest/Mounted",
        "fabric/src/gametest/java/net/conczin/mca/entity/ai/brain/tasks/",
    )
    return path.startswith(prefixes)


def _is_generic_runtime(path: str) -> bool:
    prefixes = (
        "common/src/main/",
        "common/src/test/",
        "fabric/src/main/",
        "fabric/src/test/",
        "fabric/src/gametest/",
        "neoforge/src/main/",
        "neoforge/src/test/",
    )
    return path.startswith(prefixes)


def _must_fail_closed(path: str) -> bool:
    prefixes = (
        ".github/",
        "buildSrc/",
        "gradle/",
        "scripts/ci/",
        "fabric/src/productionAcceptanceFixture/",
    )
    return (
        path.startswith(prefixes)
        or path.endswith(".gradle")
        or path.endswith(".gradle.kts")
        or path in {
            "gradle.properties",
            "settings.gradle",
            "settings.gradle.kts",
            "gradlew",
            "gradlew.bat",
        }
    )


def select_suites(
    changed_paths: Iterable[str],
    *,
    mode: SelectionMode,
) -> Selection:
    if mode is SelectionMode.RELEASE:
        return Selection(ALL_SUITES, True, "release-mode")

    normalized_paths: list[str] = []
    for raw_path in changed_paths:
        try:
            normalized = _normalize_path(raw_path)
        except ValueError:
            return Selection(ALL_SUITES, True, f"unsafe-path:{raw_path}")
        if normalized is not None:
            normalized_paths.append(normalized)

    if not normalized_paths:
        return Selection(ALL_SUITES, True, "empty-change-set")

    suites: set[str] = set()
    for path in sorted(set(normalized_paths)):
        if _must_fail_closed(path):
            return Selection(ALL_SUITES, True, f"protected-path:{path}")
        if _is_persistence_runtime(path):
            suites.update(ALL_SUITES)
            continue
        if _is_voice_runtime(path) or _is_navigation_or_gameplay(path):
            suites.update(RUNTIME_SUITES)
            continue
        if _is_generic_runtime(path):
            suites.update(RUNTIME_SUITES)
            continue
        if _is_documentation(path):
            suites.add(FAST_SUITE)
            continue
        return Selection(ALL_SUITES, True, f"unclassified-path:{path}")

    return Selection(frozenset(suites), False, "classified")
