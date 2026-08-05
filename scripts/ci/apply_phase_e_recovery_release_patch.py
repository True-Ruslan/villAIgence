#!/usr/bin/env python3
"""Apply the reviewed E3 release-gate patch with exact source anchors."""

from pathlib import Path

TARGET = Path(".github/workflows/livingworld-release.yml")

PATH_ANCHOR = """      - 'scripts/ci/package-livingworld-release.sh'
      - 'scripts/ci/production_lifecycle_acceptance.py'
"""
PATH_REPLACEMENT = """      - 'common/src/main/java/net/conczin/mca/livingworld/**'
      - 'common/src/test/java/net/conczin/mca/livingworld/**'
      - 'scripts/ci/package-livingworld-release.sh'
      - 'scripts/ci/persistence_recovery_acceptance.py'
      - 'scripts/ci/production_lifecycle_acceptance.py'
"""

TEST_ANCHOR = """          python3 scripts/ci/test_production_server_process.py
          python3 scripts/ci/test_production_lifecycle_acceptance.py

      - name: Stage and execute exact production server acceptance
"""
TEST_REPLACEMENT = """          python3 scripts/ci/test_production_server_process.py
          python3 scripts/ci/test_production_lifecycle_acceptance.py
          python3 scripts/ci/test_persistence_recovery_acceptance.py

      - name: Stage and execute exact production server acceptance
"""

STEP_ANCHOR = """      - name: Run risk catalog, server GameTests and supported loader builds
"""
STEP_REPLACEMENT = """      - name: Execute exact persistence recovery matrix
        shell: bash
        run: |
          set -euo pipefail
          rm -rf build/persistence-recovery-acceptance
          mkdir -p build/persistence-recovery-acceptance/evidence
          python3 scripts/ci/persistence_recovery_acceptance.py \\
            --stage-dir fabric/build/production-acceptance/stage \\
            --execute \\
            --work-dir build/persistence-recovery-acceptance/work \\
            --report-dir build/persistence-recovery-acceptance/evidence \\
            --java "${JAVA_HOME}/bin/java" \\
            --installer-timeout-seconds 300 \\
            --startup-timeout-seconds 180 \\
            --shutdown-timeout-seconds 60 \\
            --max-heap-mib 768
          python3 - <<'PY'
          import json
          from pathlib import Path

          report = json.loads(
              Path('build/persistence-recovery-acceptance/evidence/persistence-recovery-report.json')
              .read_text(encoding='utf-8')
          )
          if report.get('status') != 'PASS':
              raise SystemExit('persistence recovery report is not PASS')
          cases = report.get('cases')
          if not isinstance(cases, list) or len(cases) != 6:
              raise SystemExit('persistence recovery report must contain six cases')
          if any(case.get('status') != 'PASS' for case in cases):
              raise SystemExit('at least one persistence recovery case is not PASS')
          PY

      - name: Upload persistence recovery evidence
        if: always()
        uses: actions/upload-artifact@ea165f8d65b6e75b540449e92b4886f43607fa02 # v4
        with:
          name: persistence-recovery-${{ github.run_number }}
          if-no-files-found: warn
          retention-days: 30
          path: build/persistence-recovery-acceptance/evidence

      - name: Run risk catalog, server GameTests and supported loader builds
"""

TEST_PATH_ANCHOR = """      - 'scripts/ci/test_production_lifecycle_acceptance.py'
"""
TEST_PATH_REPLACEMENT = """      - 'scripts/ci/test_persistence_recovery_acceptance.py'
      - 'scripts/ci/test_production_lifecycle_acceptance.py'
"""


def replace_exact(source: str, old: str, new: str, label: str) -> str:
    count = source.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one {label} anchor, found {count}")
    return source.replace(old, new, 1)


def main() -> None:
    source = TARGET.read_text(encoding="utf-8")
    source = replace_exact(source, PATH_ANCHOR, PATH_REPLACEMENT, "path filter")
    source = replace_exact(source, TEST_PATH_ANCHOR, TEST_PATH_REPLACEMENT, "test path filter")
    source = replace_exact(source, TEST_ANCHOR, TEST_REPLACEMENT, "contract test")
    source = replace_exact(source, STEP_ANCHOR, STEP_REPLACEMENT, "recovery step")
    TARGET.write_text(source, encoding="utf-8")


if __name__ == "__main__":
    main()
