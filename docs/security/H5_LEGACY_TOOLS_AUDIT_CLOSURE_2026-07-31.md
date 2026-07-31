# H5 Legacy Tools Audit and Closure Evidence — 2026-07-31

## Scope

This record covers Step 1 H5: semantic review and removal of inherited repository utilities, exact-head whole-tree evidence and the implementation boundary for SEC-008/SEC-009 closure.

Implementation branch:

```text
agent/h5-legacy-tools-audit-closure
```

Final automated-validation code head:

```text
ae26a9445b646c02e53b9fe8a557204fd703c7ff
```

Merged through PR #63 as squash commit:

```text
6d82b4e4650294a4a42b9ea2113e64d990e08811
```

No gameplay, provider, Memory 2.0, generated game-resource or persistent world format was changed.

## Pre-cleanup evidence

Exact source head:

```text
2bcd6be28a3a69c50ab31237d23844b95052a2a0
```

Tracked-tree artifact:

```text
artifact id: 8794902496
digest: sha256:0f1ebb58d010ae30439bc54053150615215f128266e06f194bde91cfa5a43f91
tracked files: 3477
tracked files under scripts/: 22
```

The pre-cleanup tree contained 17 tracked scripts/executable launchers in the approved inventory and additional tool-only resources under `scripts/`.

## Semantic review decisions

### Deprecated static TTS bundle — removed

Removed:

```text
scripts/TTS/googole.py
scripts/TTS/main.py
scripts/TTS/polly.py
```

Reasons:

- the repository documentation already marked this TTS path deprecated;
- Google Cloud and AWS clients depended on local credentials;
- the bundle was unrelated to VillAIgence's current server-side STT/TTS pipeline;
- AWS audio used an unbounded stream read;
- `ffmpeg` and `wavegain` were invoked through `shell=True` command construction;
- no build, CI or release workflow invoked the bundle.

### Contributor fetcher — removed

Removed:

```text
scripts/fetch_contributors.py
```

Reasons:

- required a Crowdin credential from the local environment;
- called Crowdin and patron APIs outside VillAIgence runtime requirements;
- direct `requests.get` operations had no explicit timeouts or response bounds;
- report polling had no total deadline;
- generated upstream supporter metadata rather than VillAIgence product state;
- no build, CI or release workflow invoked it.

### LLM localization generator — removed

Removed:

```text
scripts/lang_pre_generation.py
```

Reasons:

- sent localization content to an external LLM;
- targeted the legacy `gpt-3.5-turbo` generation path;
- parsed free-form model text into resource JSON through brittle line/index assumptions;
- rewrote committed localization resources directly;
- upstream documentation described the utility as mostly dead code;
- no build, CI or release workflow invoked it.

### Pirate translator — removed

Removed:

```text
scripts/pirate_translator.py
```

Reasons:

- sent every source localization string to an unrelated third-party translation service;
- had no explicit request timeout or response-size bound;
- used concurrent external requests;
- was manual-only and unrelated to VillAIgence runtime;
- removal closes the concrete legacy network-tool finding SEC-008 after merge.

### Name database generator — removed

Removed:

```text
scripts/names/convert_names.py
scripts/names/countries.py
scripts/names/raw_names.txt
scripts/names/unicodes.py
```

Reasons:

- the 4.3 MiB raw upstream dataset and conversion tables were only generation inputs;
- generated name resources remain committed under the normal game-resource tree;
- the utility was not used by build, CI or release paths;
- retaining an untested generator and large raw dataset added maintenance surface without serving VillAIgence development.

### Skin generators and masks — removed

Removed:

```text
scripts/skins/clothing_generator.py
scripts/skins/face_generator.py
scripts/skins/res/burnt.png
scripts/skins/res/moss.png
scripts/skins/res/torn.png
```

Reasons:

- these were upstream offline asset-generation tools requiring OpenCV, NumPy and SciPy;
- generated clothing and face assets remain committed in the game-resource tree;
- no build, CI or release workflow invoked the tools;
- VillAIgence does not maintain an independent skin-generation pipeline.

### Umbrella launcher, tool metadata and dependency bundle — removed

Removed:

```text
scripts/all.sh
scripts/.gitignore
scripts/README.md
scripts/requirements.txt
```

The umbrella launcher activated an implicit local virtual environment and invoked unrelated maintenance utilities. The requirements file introduced a separate unmanaged Python environment with cloud/LLM/Crowdin/scientific dependencies and duplicate OpenCV declarations. After removing the utilities, these files had no valid purpose.

## Retained executable/script surface

The approved inventory now contains exactly five reviewed launchers:

```text
gradlew
gradlew.bat
scripts/ci/package-livingworld-release.sh
scripts/ci/repository_security_policy.py
scripts/ci/test_repository_security_policy.py
```

Only the three VillAIgence-owned CI/security files remain under `scripts/`.

Permanent regression controls require:

- all removed legacy paths to remain absent;
- exactly five approved launchers;
- no build, buildSrc source, CI, release or remaining CI-script reference to removed utilities;
- exact-head deterministic script and whole-tree artifacts;
- secret, dangerous-source and workflow-permission policy success.

## Final exact-head evidence

Final code head:

```text
ae26a9445b646c02e53b9fe8a557204fd703c7ff
```

Successful workflows:

```text
VillAIgence CI #922 / 30636167806 — SUCCESS
Java Pull Request CI with Gradle #458 / 30636168112 — SUCCESS
Repository security policy #79 / 30636168870 — SUCCESS
```

Final artifacts:

```text
script inventory
artifact id: 8795396094
digest: sha256:f92b9dffb43da32cf6be4b39506c1502dbcb20f85dac8c1a00d7fa4e8d54a54b
items: 5

tracked-tree manifest
artifact id: 8795396369
digest: sha256:fa0868462479b85c16027f989ab44693dbd0e39a0d3d90fbe6b48cde77d40175
tracked files: 3458
```

The whole-tree manifest records the exact commit, stable path order, Git mode, Git blob SHA, file SHA-256 and byte size for every tracked file.

## TDD evidence

Initial H5 contract:

```text
c746c289b79b08273869fc44cc515c3bd14edaa5
VillAIgence CI #898 / 30634910035 — expected FAILURE
```

The failure required removal of the legacy utility paths and whole-tree manifest retention before the implementation existed.

The first invocation guard run exposed a test-environment mistake: scanning all of `buildSrc` also traversed Gradle-generated binary outputs during `:common:test`. The guard was corrected to scan only tracked source surfaces (`buildSrc/build.gradle` and `buildSrc/src`) while an independent exact-head workflow guard verified the same removed reference tokens with path/line diagnostics.

## Finding status

### SEC-008

**Closed.** The network-capable pirate translator and all related unmanaged Python dependency metadata were removed and squash-merged through PR #63 at `6d82b4e4650294a4a42b9ea2113e64d990e08811`. Exact closing reconciliation is recorded in `SECURITY_AUDIT_FOLLOW_UP_2026-07-31_H5.md`.

### SEC-009

Implementation and automated validation are complete:

- the full tracked tree was inventoried before and after cleanup;
- every inherited utility received a semantic retain/remove decision;
- all obsolete non-CI utilities and tool resources were removed;
- only five approved launchers remain;
- undocumented additions and removed utility references are blocked;
- secret, source, permission, Fabric, NeoForge and package checks pass.

**SEC-009 is Closed.** PR #63 was squash-merged at `6d82b4e4650294a4a42b9ea2113e64d990e08811`; the dated closing record is `SECURITY_AUDIT_FOLLOW_UP_2026-07-31_H5.md`.
