#!/usr/bin/env python3
from pathlib import Path

path = Path('.github/security_acceptance_state_patch.py')
text = path.read_text(encoding='utf-8')
old = '''        "docs/security/H1_H2_CONTROLLED_SERVER_VALIDATION.md\\n```",
        "docs/security/H1_H2_CONTROLLED_SERVER_VALIDATION.md\\ndocs/security/LOCAL_SECURITY_ACCEPTANCE_HARNESS.md\\n```",
        "canonical evidence",
'''
new = '''        """Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.15.md
docs/security/SECURITY_AUDIT_FOLLOW_UP_2026-07-31_RUNTIME_0.1.15.md
docs/livingworld/VALIDATION_0.1.14.md
docs/livingworld/VALIDATION_0.1.13.md
docs/livingworld/SEMANTIC_FORGETTING_DECAY.md
docs/security/H1_H2_CONTROLLED_SERVER_VALIDATION.md
```""",
        """Canonical evidence:

```text
docs/livingworld/VALIDATION_0.1.15.md
docs/security/SECURITY_AUDIT_FOLLOW_UP_2026-07-31_RUNTIME_0.1.15.md
docs/livingworld/VALIDATION_0.1.14.md
docs/livingworld/VALIDATION_0.1.13.md
docs/livingworld/SEMANTIC_FORGETTING_DECAY.md
docs/security/H1_H2_CONTROLLED_SERVER_VALIDATION.md
docs/security/LOCAL_SECURITY_ACCEPTANCE_HARNESS.md
```""",
        "canonical evidence",
'''
if text.count(old) != 1:
    raise RuntimeError(f'expected one source anchor, found {text.count(old)}')
path.write_text(text.replace(old, new, 1), encoding='utf-8')
