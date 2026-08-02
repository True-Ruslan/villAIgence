#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
configured_minecraft_version="$(grep '^minecraft_version=' "${repo_root}/gradle.properties" | head -n 1 | cut -d= -f2-)"
test_version="9.8.7+${configured_minecraft_version}"

actual_version="$(
  cd "${repo_root}"
  ./gradlew -q properties -Prelease_version="${test_version}" --no-daemon \
    | sed -n 's/^version: //p' \
    | head -n 1
)"

if [[ "${actual_version}" != "${test_version}" ]]; then
  echo "::error title=Explicit release version ignored::Expected ${test_version}, found ${actual_version:-<empty>}."
  exit 1
fi

tmp_dir="$(mktemp -d)"
trap 'rm -rf "${tmp_dir}"' EXIT

create_fixture_jar() {
  local embedded_version="$1"
  local jar_path="$2"
  local payload_dir="${tmp_dir}/payload"
  local manifest_file="${tmp_dir}/MANIFEST.MF"

  rm -rf "${payload_dir}"
  mkdir -p \
    "${payload_dir}/net/conczin/mca/livingworld" \
    "${payload_dir}/net/conczin/mca/fabric/livingworld/voice" \
    "${payload_dir}/net/conczin/mca/livingworld/ai" \
    "${payload_dir}/net/conczin/mca/livingworld/voice"

  cat > "${payload_dir}/fabric.mod.json" <<JSON
{
  "schemaVersion": 1,
  "id": "mca",
  "version": "${embedded_version}",
  "name": "VillAIgence"
}
JSON

  : > "${payload_dir}/net/conczin/mca/livingworld/LivingWorldConfig.class"
  : > "${payload_dir}/net/conczin/mca/fabric/livingworld/voice/VoiceConversationService.class"
  : > "${payload_dir}/net/conczin/mca/livingworld/ai/AccountVerificationTransport.class"
  : > "${payload_dir}/net/conczin/mca/livingworld/ai/AccountVerificationAcceptanceProbe.class"
  : > "${payload_dir}/net/conczin/mca/livingworld/voice/VoicePcmBudgetAcceptanceProbe.class"

  printf '%s\n' \
    'Manifest-Version: 1.0' \
    "Implementation-Version: ${embedded_version}" \
    '' \
    > "${manifest_file}"

  mkdir -p "$(dirname "${jar_path}")"
  (
    cd "${payload_dir}"
    jar --create --file "${jar_path}" --manifest "${manifest_file}" .
  )
}

fixture_root="${tmp_dir}/fixture"
fixture_jar="${fixture_root}/fabric/build/libs/mca-fabric-${test_version}.jar"

create_fixture_jar "${configured_minecraft_version}-SNAPSHOT" "${fixture_jar}"

set +e
invalid_output="$(
  cd "${fixture_root}"
  bash "${repo_root}/scripts/ci/package-livingworld-release.sh" \
    "${test_version}" true dist-invalid 2>&1
)"
invalid_status=$?
set -e

if [[ "${invalid_status}" -eq 0 ]]; then
  echo "::error title=Invalid release identity accepted::Packaging accepted a release-named JAR with snapshot metadata."
  printf '%s\n' "${invalid_output}"
  exit 1
fi

if ! grep -Fq 'Embedded release version mismatch' <<< "${invalid_output}"; then
  echo "::error title=Unexpected invalid-version diagnostic::Packaging failed without the expected embedded-version mismatch message."
  printf '%s\n' "${invalid_output}"
  exit 1
fi

rm -f "${fixture_jar}"
create_fixture_jar "${test_version}" "${fixture_jar}"

(
  cd "${fixture_root}"
  bash "${repo_root}/scripts/ci/package-livingworld-release.sh" \
    "${test_version}" true dist-valid
)

output_jar="${fixture_root}/dist-valid/villaigence-fabric-${test_version}.jar"
embedded_fabric_version="$(
  unzip -p "${output_jar}" fabric.mod.json \
    | python3 -c 'import json, sys; print(json.load(sys.stdin)["version"])'
)"
embedded_manifest_version="$(
  unzip -p "${output_jar}" META-INF/MANIFEST.MF \
    | tr -d '\r' \
    | sed -n 's/^Implementation-Version: //p' \
    | head -n 1
)"

if [[ "${embedded_fabric_version}" != "${test_version}" ]]; then
  echo "::error title=Fabric metadata version changed::Expected ${test_version}, found ${embedded_fabric_version:-<empty>}."
  exit 1
fi

if [[ "${embedded_manifest_version}" != "${test_version}" ]]; then
  echo "::error title=Manifest version changed::Expected ${test_version}, found ${embedded_manifest_version:-<empty>}."
  exit 1
fi

printf 'Release identity contract passed for %s\n' "${test_version}"
