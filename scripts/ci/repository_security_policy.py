#!/usr/bin/env python3
"""Deterministic repository security policy for VillAIgence.

Uses only Python's standard library and tracked Git metadata. It performs:
- high-confidence secret scanning;
- dangerous Java/Gradle API scanning with exact documented exceptions;
- recursive tracked script/executable inventory verification;
- workflow write-permission isolation checks;
- deterministic inventory report generation.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys
from dataclasses import dataclass
from typing import Iterable

SCRIPT_SUFFIXES = {
    ".sh", ".bash", ".zsh", ".fish", ".py", ".ps1", ".bat", ".cmd",
    ".js", ".mjs", ".cjs", ".ts",
}
SCRIPT_NAMES = {"gradlew"}
TEXT_SCAN_LIMIT_BYTES = 2 * 1024 * 1024

SECRET_PATTERNS: tuple[tuple[str, re.Pattern[str]], ...] = (
    ("PRIVATE_KEY", re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----")),
    ("GITHUB_TOKEN", re.compile(r"\b(?:gh[pousr]_[A-Za-z0-9]{30,255}|github_pat_[A-Za-z0-9_]{50,255})\b")),
    ("AWS_ACCESS_KEY", re.compile(r"\b(?:AKIA|ASIA)[0-9A-Z]{16}\b")),
    ("GOOGLE_API_KEY", re.compile(r"\bAIza[0-9A-Za-z_-]{35}\b")),
    ("OPENAI_STYLE_KEY", re.compile(r"\bsk-(?:proj-|svcacct-)?[A-Za-z0-9_-]{32,}\b")),
    ("GITLAB_TOKEN", re.compile(r"\bglpat-[A-Za-z0-9_-]{20,}\b")),
    ("SLACK_TOKEN", re.compile(r"\bxox[baprs]-[A-Za-z0-9-]{20,}\b")),
    ("STRIPE_LIVE_SECRET", re.compile(r"\bsk_live_[A-Za-z0-9]{20,}\b")),
)

SOURCE_PATTERNS: tuple[tuple[str, tuple[str, ...], re.Pattern[str]], ...] = (
    ("JAVA_RUNTIME_EXEC", (".java",), re.compile(r"Runtime\s*\.\s*getRuntime\s*\(\s*\)\s*\.\s*exec\s*\(")),
    ("JAVA_PROCESS_BUILDER", (".java",), re.compile(r"\bnew\s+ProcessBuilder\s*\(")),
    ("JAVA_NATIVE_LOAD", (".java",), re.compile(r"\bSystem\s*\.\s*load(?:Library)?\s*\(")),
    ("JAVA_SCRIPT_ENGINE", (".java",), re.compile(r"\bnew\s+ScriptEngineManager\s*\(")),
    ("JAVA_OBJECT_DESERIALIZATION", (".java",), re.compile(r"\bnew\s+ObjectInputStream\s*\(")),
    ("JAVA_URL_CLASSLOADER", (".java",), re.compile(r"\bnew\s+URLClassLoader\s*\(")),
    ("JAVA_REFLECTION_ACCESS", (".java",), re.compile(r"\.setAccessible\s*\(\s*true\s*\)")),
    ("JAVA_DEFINE_CLASS", (".java",), re.compile(r"\bdefineClass\s*\(")),
    ("GRADLE_PROCESS_EXEC", (".gradle", ".groovy"), re.compile(r"\b(?:providers\s*\.\s*)?exec\s*\{")),
    ("GRADLE_COMMAND_LINE", (".gradle", ".groovy"), re.compile(r"\bcommandLine\b")),
)

NETWORK_INDICATORS: tuple[re.Pattern[str], ...] = (
    re.compile(r"https?://", re.IGNORECASE),
    re.compile(r"\b(?:urllib|requests|http\.client|aiohttp|socket)\b"),
    re.compile(r"\b(?:curl|wget)\b"),
    re.compile(r"\bInvoke-(?:WebRequest|RestMethod)\b", re.IGNORECASE),
    re.compile(r"\b(?:fetch|axios)\s*\("),
)


@dataclass(frozen=True)
class Finding:
    rule: str
    path: str
    line: int
    excerpt: str


def run_git(root: Path, *args: str) -> bytes:
    completed = subprocess.run(
        ["git", *args],
        cwd=root,
        check=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    return completed.stdout


def repository_root() -> Path:
    completed = subprocess.run(
        ["git", "rev-parse", "--show-toplevel"],
        check=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
    )
    return Path(completed.stdout.strip()).resolve()


def tracked_paths(root: Path) -> list[str]:
    raw = run_git(root, "ls-files", "-z")
    return sorted(item.decode("utf-8") for item in raw.split(b"\0") if item)


def executable_paths(root: Path) -> set[str]:
    raw = run_git(root, "ls-files", "--stage", "-z")
    result: set[str] = set()
    for item in raw.split(b"\0"):
        if not item:
            continue
        metadata, path = item.decode("utf-8").split("\t", 1)
        mode = metadata.split(" ", 1)[0]
        if mode == "100755":
            result.add(path)
    return result


def read_text(path: Path) -> str | None:
    try:
        size = path.stat().st_size
    except OSError:
        return None
    if size > TEXT_SCAN_LIMIT_BYTES:
        return None
    try:
        data = path.read_bytes()
    except OSError:
        return None
    if b"\0" in data:
        return None
    return data.decode("utf-8", errors="replace")


def load_json(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as handle:
        value = json.load(handle)
    if not isinstance(value, dict):
        raise ValueError(f"{path} must contain a JSON object")
    return value


def candidate_scripts(paths: Iterable[str], executable: set[str]) -> list[str]:
    discovered: set[str] = set(executable)
    for path in paths:
        name = Path(path).name
        suffix = Path(path).suffix.lower()
        if name in SCRIPT_NAMES or suffix in SCRIPT_SUFFIXES:
            discovered.add(path)
    return sorted(discovered)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(128 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def has_network_indicator(text: str) -> bool:
    return any(pattern.search(text) for pattern in NETWORK_INDICATORS)


def workflow_references(root: Path) -> str:
    parts: list[str] = []
    workflow_dir = root / ".github" / "workflows"
    for path in sorted(workflow_dir.glob("*.y*ml")):
        text = read_text(path)
        if text is not None:
            parts.append(text)
    return "\n".join(parts)


def verify_script_inventory(root: Path, paths: list[str], errors: list[str]) -> None:
    manifest_path = root / "docs/security/APPROVED_SCRIPT_INVENTORY.json"
    if not manifest_path.is_file():
        errors.append("approved script inventory is missing: docs/security/APPROVED_SCRIPT_INVENTORY.json")
        return

    try:
        manifest = load_json(manifest_path)
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        errors.append(f"cannot read approved script inventory: {exc}")
        return

    entries = manifest.get("scripts")
    if manifest.get("schema") != 1 or not isinstance(entries, list):
        errors.append("approved script inventory must use schema=1 and a scripts array")
        return

    approved: dict[str, dict] = {}
    for entry in entries:
        if not isinstance(entry, dict) or not isinstance(entry.get("path"), str):
            errors.append(f"invalid script inventory entry: {entry!r}")
            continue
        path = entry["path"]
        if path in approved:
            errors.append(f"duplicate script inventory path: {path}")
            continue
        if not isinstance(entry.get("purpose"), str) or not entry["purpose"].strip():
            errors.append(f"script inventory purpose is missing: {path}")
        if not isinstance(entry.get("network"), bool):
            errors.append(f"script inventory network flag must be boolean: {path}")
        if not isinstance(entry.get("ci"), bool):
            errors.append(f"script inventory ci flag must be boolean: {path}")
        approved[path] = entry

    executable = executable_paths(root)
    discovered = candidate_scripts(paths, executable)
    missing = sorted(set(discovered) - set(approved))
    stale = sorted(set(approved) - set(discovered))
    if missing:
        errors.append("undocumented tracked scripts/executables:\n  " + "\n  ".join(missing))
    if stale:
        errors.append("stale approved script entries:\n  " + "\n  ".join(stale))

    workflows = workflow_references(root)
    report: list[dict] = []
    for relative in discovered:
        file_path = root / relative
        text = read_text(file_path) or ""
        detected_network = has_network_indicator(text)
        referenced_by_ci = relative in workflows or f"./{relative}" in workflows
        entry = approved.get(relative)
        if entry is not None:
            if detected_network and not entry.get("network"):
                errors.append(f"network-capable script is not classified as network=true: {relative}")
            if referenced_by_ci != entry.get("ci"):
                errors.append(
                    f"CI classification mismatch for {relative}: "
                    f"manifest={entry.get('ci')} detected={referenced_by_ci}"
                )
        report.append({
            "path": relative,
            "mode": "100755" if relative in executable else "tracked",
            "sha256": sha256(file_path),
            "network_detected": detected_network,
            "ci_referenced": referenced_by_ci,
        })

    output = root / "build/security/script-inventory.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(
        json.dumps({"schema": 1, "scripts": report}, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )


def scan_secrets(root: Path, paths: list[str], errors: list[str]) -> None:
    findings: list[Finding] = []
    for relative in paths:
        text = read_text(root / relative)
        if text is None:
            continue
        for line_number, line in enumerate(text.splitlines(), start=1):
            if "security-scan: allow" in line:
                continue
            for rule, pattern in SECRET_PATTERNS:
                if pattern.search(line):
                    findings.append(Finding(rule, relative, line_number, "<redacted>"))
    if findings:
        errors.append(
            "high-confidence secret patterns found:\n  "
            + "\n  ".join(f"{finding.path}:{finding.line} [{finding.rule}]" for finding in findings)
        )


def approved_source_exceptions(root: Path, errors: list[str]) -> list[dict]:
    path = root / "docs/security/APPROVED_SOURCE_SECURITY_EXCEPTIONS.json"
    if not path.is_file():
        errors.append("source security exception registry is missing")
        return []
    try:
        value = load_json(path)
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        errors.append(f"cannot read source security exceptions: {exc}")
        return []
    entries = value.get("exceptions")
    if value.get("schema") != 1 or not isinstance(entries, list):
        errors.append("source security exceptions must use schema=1 and an exceptions array")
        return []
    return entries


def scan_source_security(root: Path, paths: list[str], errors: list[str]) -> None:
    exceptions = approved_source_exceptions(root, errors)
    used: set[int] = set()
    findings: list[Finding] = []

    for relative in paths:
        suffix = Path(relative).suffix.lower()
        text = read_text(root / relative)
        if text is None:
            continue
        for rule, suffixes, pattern in SOURCE_PATTERNS:
            if suffix not in suffixes:
                continue
            for line_number, line in enumerate(text.splitlines(), start=1):
                if not pattern.search(line):
                    continue
                allowed = False
                for index, exception in enumerate(exceptions):
                    if not isinstance(exception, dict):
                        continue
                    if (
                        exception.get("path") == relative
                        and exception.get("rule") == rule
                        and isinstance(exception.get("contains"), str)
                        and exception["contains"] in line
                        and isinstance(exception.get("reason"), str)
                        and exception["reason"].strip()
                    ):
                        used.add(index)
                        allowed = True
                        break
                if not allowed:
                    findings.append(Finding(rule, relative, line_number, line.strip()[:160]))

    if findings:
        errors.append(
            "unapproved dangerous source APIs found:\n  "
            + "\n  ".join(
                f"{finding.path}:{finding.line} [{finding.rule}] {finding.excerpt}"
                for finding in findings
            )
        )

    unused = [entry for index, entry in enumerate(exceptions) if index not in used]
    if unused:
        errors.append("unused/stale source security exceptions:\n  " + "\n  ".join(map(str, unused)))


def verify_workflow_permissions(root: Path, errors: list[str]) -> None:
    workflow_dir = root / ".github" / "workflows"
    for path in sorted(workflow_dir.glob("*.y*ml")):
        text = read_text(path) or ""
        relative = path.relative_to(root).as_posix()
        write_count = len(re.findall(r"(?m)^\s+contents:\s*write\s*$", text))
        if path.name == "livingworld-release.yml":
            if not re.search(r"(?m)^permissions:\s*\n\s{2}contents:\s*read\s*$", text):
                errors.append("release workflow must default to contents: read")
            if write_count != 1:
                errors.append(
                    f"release workflow must contain exactly one job-scoped contents: write, found {write_count}"
                )
            release_block = re.search(
                r"(?ms)^  github-release:\n.*?^    permissions:\n      contents: write\s*$",
                text,
            )
            if release_block is None:
                errors.append("release contents: write must remain inside github-release job")
        elif write_count:
            errors.append(f"non-release workflow grants contents: write: {relative}")
        if "permissions:" not in text:
            errors.append(f"workflow lacks an explicit permissions boundary: {relative}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="verify repository policy")
    args = parser.parse_args()
    if not args.check:
        parser.error("--check is required")

    root = repository_root()
    paths = tracked_paths(root)
    errors: list[str] = []

    verify_script_inventory(root, paths, errors)
    scan_secrets(root, paths, errors)
    scan_source_security(root, paths, errors)
    verify_workflow_permissions(root, errors)

    if errors:
        print("Repository security policy failed:", file=sys.stderr)
        for error in errors:
            print(f"\n- {error}", file=sys.stderr)
        return 1

    print(f"Repository security policy passed for {len(paths)} tracked files.")
    print("Deterministic script inventory: build/security/script-inventory.json")
    return 0


if __name__ == "__main__":
    sys.exit(main())
