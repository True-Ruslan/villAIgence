# VillAIgence roadmap and project state

The canonical planning documents are:

- [`docs/PROJECT_STATE.md`](docs/PROJECT_STATE.md) — **where the project is now**: implemented systems, current release state, known gaps and immediate next priorities.
- [`docs/ROADMAP.md`](docs/ROADMAP.md) — **where the project is going**: the full `0.1.x → 1.0` product roadmap, architecture principles and milestone exit criteria.

## Resume development in a new session

Use this prompt:

> **Open `docs/PROJECT_STATE.md` and `docs/ROADMAP.md` in `True-Ruslan/villAIgence`. Check recent PRs/releases/CI, then tell me how VillAIgence development is going, what is complete, what changed since the state file, and what we should build next.**

The repository, recent PRs/releases and CI are the source of truth. `PROJECT_STATE.md` must be updated after material project/release progress so a new session can reconstruct context without relying on an old chat thread.
