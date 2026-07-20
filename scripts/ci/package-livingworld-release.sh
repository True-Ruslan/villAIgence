#!/usr/bin/env bash
set -euo pipefail

artifact_label="${1:?artifact label is required}"
is_release="${2:-false}"
dist_dir="${3:-dist}"

mkdir -p "${dist_dir}"

if [[ "${is_release}" == "true" ]]; then
  source_jar="fabric/build/libs/mca-fabric-${artifact_label}.jar"
  if [[ ! -f "${source_jar}" ]]; then
    echo "::error title=Expected release JAR missing::${source_jar} was not produced."
    find fabric/build/libs -maxdepth 1 -type f -name '*.jar' -print || true
    exit 1
  fi
else
  mapfile -t candidates < <(find fabric/build/libs -maxdepth 1 -type f -name 'mca-fabric-*.jar' \
    ! -name '*-sources.jar' \
    ! -name '*-dev.jar' \
    ! -name '*-dev-shadow.jar' \
    ! -name '*-shadow.jar' | sort)

  if [[ "${#candidates[@]}" -ne 1 ]]; then
    echo "::error title=Unexpected Fabric JAR set::Expected exactly one distributable Fabric JAR, found ${#candidates[@]}."
    printf '%s\n' "${candidates[@]}"
    exit 1
  fi
  source_jar="${candidates[0]}"
fi

output_jar="${dist_dir}/mca-livingworld-fabric-${artifact_label}.jar"
cp "${source_jar}" "${output_jar}"

contents_file="${dist_dir}/jar-contents.txt"
jar tf "${output_jar}" > "${contents_file}"
grep -qx 'fabric.mod.json' "${contents_file}"
grep -qx 'net/conczin/mca/livingworld/LivingWorldConfig.class' "${contents_file}"
grep -qx 'net/conczin/mca/fabric/livingworld/voice/VoiceConversationService.class' "${contents_file}"
rm "${contents_file}"

sha256sum "${output_jar}" > "${output_jar}.sha256"

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  echo "jar=${output_jar}" >> "${GITHUB_OUTPUT}"
  echo "checksum=${output_jar}.sha256" >> "${GITHUB_OUTPUT}"
fi

printf 'Verified release package: %s\n' "${output_jar}"
printf 'Checksum: '
cat "${output_jar}.sha256"
