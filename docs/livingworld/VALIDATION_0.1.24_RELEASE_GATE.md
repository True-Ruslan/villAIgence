# VillAIgence `0.1.24+1.21.1` Historical Release Boundary

## Correction

`0.1.24+1.21.1` was already published before PR #106 merged the complete exact-production release gate.

Canonical tag identity:

```text
tag:     0.1.24+1.21.1
commit:  42d8cb3408c53770abe63ced130727c805bc9e8a
status:  published and immutable
```

The tag contains the PR #105 tombstone-inventory ownership correction, but it was not created by the stronger release workflow introduced afterward at:

```text
PR:      #106
merge:   193f1a0ed3882f0c8e925c5ae16d59f5bacb489c
```

Therefore:

- `0.1.24+1.21.1` must not be overwritten, moved or reused;
- PR #106 dry-run evidence must not be retroactively represented as the official `0.1.24` tag run;
- the first artifact promoted through the complete release gate uses the next version, `0.1.25+1.21.1`;
- exact release and installed-canary evidence continue in `VALIDATION_0.1.25_RELEASE_GATE.md`.

## Runtime scope retained in `0.1.24`

PR #105 corrected the death-path ordering so that a valid tombstone serializes an MCA NPC while the custom inventory is intact. Successful capture owns the inventory without loose duplicates; failed or absent capture retains the legacy single loose-drop path.

Automated PR #105 evidence remains valid for the code behavior, but the official `0.1.24` JAR identity and any installed acceptance must be recorded separately rather than inferred from a later workflow run.

## Result

```text
0.1.24 tag consumed:                 yes
0.1.24 tag rewrite permitted:        no
complete exact-production gate:      merged after publication
correct next release:                0.1.25+1.21.1
canonical continuing document:       VALIDATION_0.1.25_RELEASE_GATE.md
```
