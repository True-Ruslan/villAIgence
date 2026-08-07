#!/usr/bin/env python3

import importlib.util
from pathlib import Path
import sys
import tempfile
import unittest

MODULE_PATH = Path(__file__).with_name("repository_security_policy.py")
SPEC = importlib.util.spec_from_file_location("repository_security_policy", MODULE_PATH)
POLICY = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
sys.modules[SPEC.name] = POLICY
SPEC.loader.exec_module(POLICY)


class RepositorySecurityPolicyTest(unittest.TestCase):
    def test_high_confidence_secret_signatures(self):
        samples = {
            "PRIVATE_KEY": "-----BEGIN " + "PRIVATE KEY-----",
            "GITHUB_TOKEN": "ghp_" + "A" * 36,
            "AWS_ACCESS_KEY": "AKIA" + "A" * 16,
            "GOOGLE_API_KEY": "AIza" + "A" * 35,
            "OPENAI_STYLE_KEY": "sk-" + "A" * 40,
            "GITLAB_TOKEN": "glpat-" + "A" * 24,
            "SLACK_TOKEN": "xoxb-" + "A" * 24,
            "STRIPE_LIVE_SECRET": "sk_" + "live_" + "A" * 24,
        }
        patterns = dict(POLICY.SECRET_PATTERNS)
        for rule, sample in samples.items():
            with self.subTest(rule=rule):
                self.assertIsNotNone(patterns[rule].search(sample))

    def test_secret_signatures_ignore_placeholders(self):
        placeholders = [
            "sk-your-key",
            "AKIA...",
            "ghp_example",
            "-----BEGIN PUBLIC KEY-----",
        ]
        for placeholder in placeholders:
            with self.subTest(placeholder=placeholder):
                self.assertFalse(any(pattern.search(placeholder) for _, pattern in POLICY.SECRET_PATTERNS))

    def test_dangerous_java_and_gradle_signatures(self):
        samples = {
            "JAVA_RUNTIME_EXEC": "Runtime.getRuntime().exec(command);",
            "JAVA_PROCESS_BUILDER": "new ProcessBuilder(command);",
            "JAVA_OBJECT_DESERIALIZATION": "new ObjectInputStream(stream);",
            "JAVA_REFLECTION_ACCESS": "constructor.setAccessible(true);",
            "GRADLE_PROCESS_EXEC": "def tag = providers.exec {",
            "GRADLE_COMMAND_LINE": "commandLine 'git', 'tag'",
        }
        patterns = {rule: pattern for rule, _, pattern in POLICY.SOURCE_PATTERNS}
        for rule, sample in samples.items():
            with self.subTest(rule=rule):
                self.assertIsNotNone(patterns[rule].search(sample))

    def test_script_candidate_inventory_is_extension_and_mode_based(self):
        paths = [
            "scripts/tool.py",
            "scripts/tool.sh",
            "src/Main.java",
            "gradlew",
            "assets/data.json",
        ]
        result = POLICY.candidate_scripts(paths, {"bin/manual-tool"})
        self.assertEqual(
            result,
            ["bin/manual-tool", "gradlew", "scripts/tool.py", "scripts/tool.sh"],
        )

    def test_network_indicators_are_conservative(self):
        self.assertTrue(POLICY.has_network_indicator("requests.get(endpoint)"))
        self.assertTrue(POLICY.has_network_indicator("curl https://example.invalid"))
        self.assertFalse(POLICY.has_network_indicator("Path('local.json').read_text()"))

    def test_release_critical_workflows_allow_one_named_write_job(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            workflow_dir = root / ".github" / "workflows"
            workflow_dir.mkdir(parents=True)
            for workflow_name, write_job in POLICY.RELEASE_WRITE_JOBS.items():
                (workflow_dir / workflow_name).write_text(
                    "permissions:\n"
                    "  contents: read\n"
                    "jobs:\n"
                    f"  {write_job}:\n"
                    "    permissions:\n"
                    "      contents: write\n",
                    encoding="utf-8",
                )

            errors = []
            POLICY.verify_workflow_permissions(root, errors)
            self.assertEqual([], errors)

    def test_release_recovery_write_permission_is_bound_to_restore_job(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            workflow_dir = root / ".github" / "workflows"
            workflow_dir.mkdir(parents=True)
            (workflow_dir / "livingworld-release-recovery.yml").write_text(
                "permissions:\n"
                "  contents: read\n"
                "jobs:\n"
                "  build-and-package:\n"
                "    permissions:\n"
                "      contents: write\n",
                encoding="utf-8",
            )

            errors = []
            POLICY.verify_workflow_permissions(root, errors)
            self.assertIn(
                "livingworld-release-recovery.yml contents: write must remain inside restore-github-release job",
                errors,
            )

    def test_non_release_workflow_cannot_grant_contents_write(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            workflow_dir = root / ".github" / "workflows"
            workflow_dir.mkdir(parents=True)
            (workflow_dir / "ordinary.yml").write_text(
                "permissions:\n"
                "  contents: read\n"
                "jobs:\n"
                "  publish:\n"
                "    permissions:\n"
                "      contents: write\n",
                encoding="utf-8",
            )

            errors = []
            POLICY.verify_workflow_permissions(root, errors)
            self.assertIn(
                "non-release workflow grants contents: write: .github/workflows/ordinary.yml",
                errors,
            )


if __name__ == "__main__":
    unittest.main()
